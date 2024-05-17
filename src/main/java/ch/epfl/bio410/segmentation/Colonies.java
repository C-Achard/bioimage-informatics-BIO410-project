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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    for (int i = 1; i <= imageDIC.getStackSize(); i++) {
        IJ.log("Processing frame " + i + "/" + imageDIC.getStackSize());
        // Get the bacteria labels for the current frame
        ImageProcessor bacteriaLabels = bacteriaLabelsNoColonies.getStack().getProcessor(i);

        // Create a new ImageProcessor to hold the result for the current frame
        ImageProcessor resultFrame = new ByteProcessor(bacteriaLabelsNoColonies.getWidth(), bacteriaLabelsNoColonies.getHeight());

        // Loop over each unique label in the bacteria labels
        for (int label : getUniqueValues(bacteriaLabels)) {
            // Create a binary mask where the pixels with the current label are set to 1 and all other pixels are set to 0
            ImageProcessor mask = createBinaryMask(bacteriaLabels, label);

            // Get the unique values in the Voronoi diagram that overlap with the mask, along with their counts
            Map<Integer, Integer> voronoiRegions = getUniqueValuesWithCounts(voronoiDiagram.getProcessor(), mask);

            // Find the label with the maximum count
            int colonyLabel = getMaxCountLabel(voronoiRegions);

            // Add the colony label to the result for the current frame
            addLabelToResult(bacteriaLabels, resultFrame, label, colonyLabel);
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

        IJ.log("Processing " + this.imageDIC.getStackSize() + " frames");
        // Loop through each slice in the stack
        for (int i = 1; i <= this.imageDIC.getStackSize(); i++) {
            // Extract and copy the slice using substack
            ImageProcessor frame = this.imageDIC.getStack().getProcessor(i);
            ImagePlus slice = new ImagePlus("Slice", frame.duplicate());

            // Process the slice
            ImagePlus destinationImagePlus = connectedComponentsLabeling(slice);
//            ImagePlus destinationImagePlus = voronoiOtsuLabeling(slice);
            processedStack.addSlice(destinationImagePlus.getProcessor());

            // Compute the Voronoi diagram for the first frame only
            if (i == 1) {
                ImagePlus voronoiImagePlus = voronoiDiagram(destinationImagePlus);
                this.voronoiDiagram = voronoiImagePlus;
//                regionDiagramStack.addSlice(voronoiImagePlus.getProcessor());
            }

            // Delete slice
            slice.close();
            destinationImagePlus.close();
            IJ.log("Processed frame " + i + "/" + this.imageDIC.getStackSize());
        }

        // Create a new ImagePlus from the processed stack
        ImagePlus processedImage = new ImagePlus("Bacteria labels", processedStack);
//        ImagePlus regionDiagram = new ImagePlus("Region diagrams", regionDiagramStack);
        // Set Glasbey LUT
        processedImage.setLut(this.glasbeyLUT);
        voronoiDiagram.setLut(this.glasbeyLUT);

        // Show the result
        processedImage.show();
        voronoiDiagram.show();
        this.bacteriaLabelsNoColonies = processedImage;
//        this.voronoiDiagram = voronoiDiagram;

        // Assign colonies
        this.colonyLabels = getColonyFromDiagram();
        this.colonyLabels.setLut(this.glasbeyLUT);
        this.colonyLabels.show();
    }
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

    private ImageProcessor createBinaryMask(ImageProcessor image, float value) {
        // This method returns a binary ImageProcessor where the pixels with the given value are set to 1 and all other pixels are set to 0
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

    private Map<Integer, Integer> getUniqueValuesWithCounts(ImageProcessor image, ImageProcessor mask) {
        // This method returns a Map where the keys are the unique values in the given ImageProcessor that overlap with the mask, and the values are their counts
        Map<Integer, Integer> counts = new HashMap<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (mask.get(x, y) == 1) {
                    int value = image.get(x, y);
                    counts.put(value, counts.getOrDefault(value, 0) + 1);
                }
            }
        }
        return counts;
    }

    private int getMaxCountLabel(Map<Integer, Integer> counts) {
        // This method returns the key with the maximum value in the given Map
        int maxLabel = 0;
        int maxCount = 0;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxLabel = entry.getKey();
                maxCount = entry.getValue();
            }
        }
        return maxLabel;
    }

    private void addLabelToResult(ImageProcessor source, ImageProcessor destination, int sourceLabel, int destinationLabel) {
        // This method adds the destination label to the destination ImageProcessor at the positions where the source ImageProcessor has the source label
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if (source.getf(x, y) == sourceLabel) {
                    destination.set(x, y, destinationLabel);
                }
            }
        }
    }

}
