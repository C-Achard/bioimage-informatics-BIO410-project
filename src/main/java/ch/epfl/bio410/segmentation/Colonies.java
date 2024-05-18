package ch.epfl.bio410.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import ch.epfl.bio410.utils.utils;

import java.util.*;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.IntStream;

public class Colonies {
    /** The goal of this class is to group clusters of bacteria into colonies.
     * We first use connected components labeling to segment the image into touching bacteria with a unique label per colony.
     * Then, we expand these labels in the first frame with a Voronoi diagram to obtain a "region" for each colony.
     * Finally, over each frame, we assign a given group of bacteria to a colony if they are mainly in the same region of the Voronoi diagram.
     */
    public ImagePlus imageDIC; // holds the original DIC image
    public ImagePlus bacteriaLabelsNoColonies; // contains the instance labels, with disjoint values between frames
    public ImagePlus voronoiDiagram; // contains the Voronoi diagram of the first frame, used to define the colony regions
    public ImagePlus colonyLabels; // contains the colony labels, with consistent values between frames
    private CLIJ2 clij2; // the CLIJ2 instance used for image processing
    private final LUT glasbeyLUT = utils.getGlasbeyLUT();

    public Colonies(ImagePlus imageDIC) {
        this.clij2 = CLIJ2.getInstance();
        this.imageDIC = imageDIC;
    }
private ImagePlus getColonyFromDiagram() {
    // In python, this would be :
    // result = np.zeros_like(bacteriaLabelsNoColonies)
    // for i in range(imageDIC.shape[0]):
    //     for label in np.unique(bacteriaLabelsNoColonies[i]):
    //        mask = np.where(bacteriaLabelsNoColonies[i] == label, 1, 0)
    //        voronoi_regions = np.unique(np.where(mask != 0, voronoiDiagram, 0), return_counts=True)
    //        colony_label = voronoi_regions[0][np.argmax(voronoi_regions[1])]
    //        result[i] += np.where(bacteriaLabelsNoColonies[i] == label, colony_label, result[i])
    IJ.log("Assigning colonies to bacteria");
    // Create a new ImageStack to hold the result
    ImageStack result = new ImageStack(bacteriaLabelsNoColonies.getWidth(), bacteriaLabelsNoColonies.getHeight());

    // Loop over each slice in the stack
    for (int i = 1; i <= bacteriaLabelsNoColonies.getStackSize(); i++) {
        IJ.log("Computing colonies for frame " + i + "/" + bacteriaLabelsNoColonies.getStackSize());
        // Get the bacteria labels for the current frame
        ImageProcessor bacteriaLabels = bacteriaLabelsNoColonies.getStack().getProcessor(i);

        // Create a new ImageProcessor to hold the result for the current frame
        ImageProcessor resultFrame = new ByteProcessor(bacteriaLabelsNoColonies.getWidth(), bacteriaLabelsNoColonies.getHeight());

        // Loop over each unique label in the bacteria labels
        for (int label : getUniqueValues(bacteriaLabels)) {
            // Create a binary mask where the pixels with the current label are set to 1 and all other pixels are set to 0
             ImageProcessor mask = createBinaryMask(bacteriaLabels, label);
//            ImageProcessor mask = new ByteProcessor(bacteriaLabels.getWidth(), bacteriaLabels.getHeight());
//            addLabelToResultAndCreateBinaryMask(bacteriaLabels, mask, label, 1);

            // Get the unique values in the Voronoi diagram that overlap with the mask, along with their counts
//            Map<Integer, Integer> voronoiRegions = getUniqueValuesWithCounts(voronoiDiagram.getProcessor(), mask);
            int[] voronoiRegions = getUniqueValuesWithCounts(voronoiDiagram.getProcessor(), mask);
            IJ.log("Voronoi regions : " + Arrays.toString(voronoiRegions));
            // Find the label with the maximum count
            int colonyLabel = getMaxCountLabel(voronoiRegions);

            // Add the colony label to the result for the current frame
            addLabelToResult(bacteriaLabels, resultFrame, label, colonyLabel);
            IJ.log("Frame " + i + " - Label " + label + " assigned to colony " + colonyLabel);
        }

        // Add the result for the current frame to the result stack
        result.addSlice(resultFrame);
    }
    return new ImagePlus("Colony labels", result);
}

private ImagePlus voronoiOtsuLabeling(ImagePlus slice) {
    ClearCLBuffer input = clij2.push(slice);
    ClearCLBuffer destination = clij2.create(input);
    clij2.voronoiOtsuLabeling(input, destination, 5.0f, 1.0f);

    // Pull the result and add it to the processed stack
    ImagePlus destinationImagePlus = clij2.pull(destination);

    // Cleanup memory on GPU
    clij2.release(input);
    clij2.release(destination);
    return destinationImagePlus;
}

private ImagePlus connectedComponentsLabeling(ImagePlus slice) {
    ClearCLBuffer input = clij2.push(slice);
    ClearCLBuffer destination = clij2.create(input);
    clij2.connectedComponentsLabelingBox(input, destination);

    // Pull the result and add it to the processed stack
    ImagePlus destinationImagePlus = clij2.pull(destination);

    // Cleanup memory on GPU
    clij2.release(input);
    clij2.release(destination);
    return destinationImagePlus;
}

private ImagePlus voronoiDiagram(ImagePlus slice) {
    ClearCLBuffer input = clij2.push(slice);
    ClearCLBuffer destination = clij2.create(input);
    clij2.extendLabelingViaVoronoi(input, destination);

    // Pull the result and add it to the processed stack
    ImagePlus destinationImagePlus = clij2.pull(destination);

    // Cleanup memory on GPU
    clij2.release(input);
    clij2.release(destination);
    return destinationImagePlus;
}

/**
 * This method performs Voronoi-Otsu labeling on the DIC image stack.
 * @return The processed image stack. Colonies are labeled with a unique color, inconsistent between frames.
 */
public void runColoniesComputation() {
    // Create a stack to hold the processed frames
    ImageStack processedStack = new ImageStack(this.imageDIC.getWidth(), this.imageDIC.getHeight());
    //        ImageStack regionDiagramStack = new ImageStack(this.imageDIC.getWidth(), this.imageDIC.getHeight());
    IJ.log("Computing labels for bacteria");
    IJ.log("Processing " + this.imageDIC.getStackSize() + " frames");
    // Loop through each slice in the stack
    for (int i = 1; i <= this.imageDIC.getStackSize(); i++) {
        // Extract and copy the slice using substack
        ImageProcessor frame = this.imageDIC.getStack().getProcessor(i);
        ImagePlus slice = new ImagePlus("Slice", frame.duplicate());

        // Process the slice
        ImagePlus destinationImagePlus = connectedComponentsLabeling(slice);
        // ImagePlus destinationImagePlus = voronoiOtsuLabeling(slice);

        // TODO : add filtering based on area
        IJ.log("Filtering frame " + i + "/" + this.imageDIC.getStackSize());
        ImageProcessor filteredImage = filterLabelsByPixelCount(destinationImagePlus.getProcessor(), 1, 99);
        destinationImagePlus = new ImagePlus("Filtered", filteredImage);

        processedStack.addSlice(destinationImagePlus.getProcessor());

        // Compute the Voronoi diagram for the first frame only
        // Assumptions :
        // - The bacteria are initially not too close to each other
        // - The bacteria do not move too much over all frames
        if (i == 1) {
            ImagePlus voronoiImagePlus = voronoiDiagram(destinationImagePlus);
            this.voronoiDiagram = voronoiImagePlus;
            // regionDiagramStack.addSlice(voronoiImagePlus.getProcessor());
        }

        // Delete slice
        slice.close();
        destinationImagePlus.close();
        IJ.log("Finished labeling frame " + i + "/" + this.imageDIC.getStackSize());
        //////////////
        // only first two frames for testing
//        if (i == 2) {
//            break;
//        }
    }

    // Create a new ImagePlus from the processed stack
    this.bacteriaLabelsNoColonies  = new ImagePlus("Bacteria labels", processedStack);

    // Set Glasbey LUT
    this.bacteriaLabelsNoColonies.setLut(this.glasbeyLUT);
    this.voronoiDiagram.setLut(this.glasbeyLUT);

    // Show the result
    this.bacteriaLabelsNoColonies.show();
    this.voronoiDiagram.show();

    // Assign colonies
    this.colonyLabels = getColonyFromDiagram();
    this.colonyLabels.setLut(this.glasbeyLUT);
    this.colonyLabels.show();
    }

    private ImageProcessor filterLabelsByPixelCount(ImageProcessor labels, double lowerPercentile, double upperPercentile) {
        // Get the counts of each label
        int[] counts = getUniqueValuesWithCounts(labels);
        IJ.log("Counts : " + Arrays.toString(counts));
        // Calculate the total sum of all counts
        int totalSum = Arrays.stream(counts).sum();

        // Calculate the cumulative sum of the counts array
        int[] cumulativeSum = new int[counts.length];
        int sum = 0;
        // Always skip the first element, as it is the background
        for (int i = 1; i < counts.length; i++) {
            sum += counts[i];
            cumulativeSum[i] = sum;
        }
        IJ.log("Total sum : " + totalSum);
        IJ.log("Cumulative sum : " + Arrays.toString(cumulativeSum));

        // Find the indices where the cumulative sum is just above the lower and upper percentiles of the total sum
        int lowerThreshold = findThresholdIndex(cumulativeSum, totalSum * lowerPercentile / 100);
        int upperThreshold = findThresholdIndex(cumulativeSum, totalSum * upperPercentile / 100);

        IJ.log("Lower threshold : " + lowerThreshold);
        IJ.log("Upper threshold : " + upperThreshold);

        // Create a new image where only the labels with counts within the percentile range are kept
        ImageProcessor filteredImage = new ByteProcessor(labels.getWidth(), labels.getHeight());
        for (int y = 0; y < labels.getHeight(); y++) {
            for (int x = 0; x < labels.getWidth(); x++) {
                int label = labels.get(x, y);
                if (cumulativeSum[label] >= lowerThreshold && cumulativeSum[label] <= upperThreshold) {
                    filteredImage.set(x, y, label);
                }
            }
        }
        return filteredImage;
    }
    private int findThresholdIndex(int[] cumulativeSum, double threshold) {
        for (int i = 0; i < cumulativeSum.length; i++) {
            if (cumulativeSum[i] > threshold) {
                return i;
            }
        }
        return cumulativeSum.length - 1;
    }

    /**
     * This method returns a Set of the unique values in the given ImageProcessor.
     * Sort of equivalent to np.unique in Python.
     * @param image The ImageProcessor to analyze.
     * @return A Set containing the unique values in the ImageProcessor.
     */
    private Set<Integer> getUniqueValues(ImageProcessor image) {
        // This method returns a Set of the unique values in the given ImageProcessor
        Set<Integer> uniqueValues = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int value = image.get(x, y);
                if (value != 0) {
                    uniqueValues.add(value);
                }
            }
        }
        return uniqueValues;
    }

    /**
     * This method returns a binary ImageProcessor where the pixels with the given value are set to 1 and all other pixels are set to 0.
     * In Python, this would be : mask = np.where(image == value, 1, 0)
     * @param image The ImageProcessor to analyze.
     * @param value The value to use for the binary mask.
     * @return A binary ImageProcessor where the pixels with the given value are set to 1 and all other pixels are set to 0.
     */
    private ImageProcessor createBinaryMask(ImageProcessor image, float value) {
        ImageProcessor mask = new ByteProcessor(image.getWidth(), image.getHeight());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getf(x, y) == value) {
                    mask.set(x, y, 1);
                }
            }
        }
        return mask;
    }
    /**
     * This method adds the destination label to the destination ImageProcessor at the positions where the source ImageProcessor has the source label.
     * It's a replacement for np.where in Python.
     * @param source The ImageProcessor to analyze.
     * @param destination The ImageProcessor to modify.
     * @param sourceLabel The value to look for in the source ImageProcessor.
     * @param destinationLabel The value to add to the destination ImageProcessor.
     */
    private void addLabelToResult(ImageProcessor source, ImageProcessor destination, int sourceLabel, int destinationLabel) {
        // This method adds the destination label to the destination ImageProcessor at the positions where the source ImageProcessor has the source label
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if (source.getf(x, y) == sourceLabel) { // perhaps use getPixel instead ?
                    destination.set(x, y, destinationLabel);
                }
            }
        }
    }
    /**
     * This method returns an array where the index is the unique value in the given ImageProcessor that overlap with the mask, and the value is their count.
     * Similar to np.unique with return_counts=True in Python.
     * @param image The ImageProcessor to analyze.
     * @param mask The binary mask to use for the analysis.
     * @return An array where the index is the unique value in the ImageProcessor that overlap with the mask, and the value is their count.
     */
    // non-parallel version
    private int[] getUniqueValuesWithCounts(ImageProcessor image, ImageProcessor mask) {
        int[] counts = new int[256]; // Assuming 8-bit image, adjust size for different bit depths

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (mask.get(x, y) == 1) {
                    int value = image.get(x, y);
                    counts[value]++;
                }
            }
        }

        return counts;
    }
    // version that does not take masks into account
    private int[] getUniqueValuesWithCounts(ImageProcessor image) {
        int[] counts = new int[256]; // Assuming 8-bit image, adjust size for different bit depths

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int value = image.get(x, y);
                counts[value]++;
            }
        }

        return counts;
    }
    /**
     * This method returns the key with the maximum value in the given Map.
     * @param counts The Array to analyze.
     * @return The value with the maximum count in the Array.
     */
    private int getMaxCountLabel(int[] counts) {
    int maxLabel = 0;
    int maxCount = 0;
    for (int i = 0; i < counts.length; i++) {
        if (counts[i] > maxCount) {
            maxLabel = i;
            maxCount = counts[i];
        }
    }
    return maxLabel;
}


}
