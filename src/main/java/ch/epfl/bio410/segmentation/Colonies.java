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
private void getColonyFromDiagram() {
    // In python, this would be :
    // for i in range(imageDIC.shape[0]):
    //     for label in np.unique(bacteriaLabelsNoColonies[i]):
    //        mask = np.where(bacteriaLabelsNoColonies[i] == label, 1, 0)
    //        voronoi_regions = np.unique(np.where(mask != 0, voronoiDiagram, 0))
    IJ.log("Assigning colonies to bacteria");
    ImageStack resultColonyLabels = new ImageStack(this.imageDIC.getWidth(), this.imageDIC.getHeight());
    for (int i = 1; i <= this.imageDIC.getStackSize(); i++) {
        IJ.log("Processing frame " + i + "/" + this.imageDIC.getStackSize());
        // Create an ImageProcessor to hold the colony labels
        ImageProcessor sliceColonyLabels = new ByteProcessor(this.imageDIC.getWidth(), this.imageDIC.getHeight());
        // Get bacteria labels stack for the current frame
        ImageProcessor bacteriaLabelsStack = bacteriaLabelsNoColonies.getStack().getProcessor(i);
        // Get voronoi diagram
        ImageProcessor voronoi = voronoiDiagram.getStack().getProcessor(1);
        Set<Float> uniqueLabels = new HashSet<>();
        for (int j = 0; j < voronoi.getPixelCount(); j++) {
            float value = voronoi.getf(j);
            if (value != 0) {
                uniqueLabels.add(value);
            }
        }

        for (Float label : uniqueLabels) {
            // Get all regions in the voronoi diagram that overlap with the current label
            ImageProcessor mask = bacteriaLabelsStack.duplicate();
            mask.setThreshold(label, label, ImageProcessor.NO_LUT_UPDATE);
            ImageProcessor binaryMask = mask.createMask();
            ImageProcessor regions = voronoi.duplicate();
            regions.copyBits(binaryMask, 0, 0, Blitter.MULTIPLY);
            Map<Float, Integer> counts = new HashMap<>();
            for (int j = 0; j < regions.getPixelCount(); j++) {
                float value = regions.getf(j);
                if (value != 0) {  // Ignore zero values
                    counts.put(value, counts.getOrDefault(value, 0) + 1);
                }
            }

            // Find the value with the maximum count
            float maxValue = 0;
            int maxCount = 0;
            for (Map.Entry<Float, Integer> entry : counts.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxValue = entry.getKey();
                    maxCount = entry.getValue();
                }
            }

            // Assign the colony label to the bacteria
            ImageProcessor colonyLabel = bacteriaLabelsStack.duplicate();
            colonyLabel.setThreshold(maxValue, maxValue, ImageProcessor.NO_LUT_UPDATE);
            ImageProcessor binaryColonyLabel = colonyLabel.createMask();
            // Add to sliceColonyLabels
            sliceColonyLabels.copyBits(binaryColonyLabel, 0, 0, Blitter.ADD);
        }
        resultColonyLabels.addSlice(sliceColonyLabels);
    }
    // Convert resultColonyLabels to ImagePlus
    this.colonyLabels = new ImagePlus("Colony labels", resultColonyLabels);
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
//        getColonyFromDiagram();
//        this.colonyLabels.show();
    }

}
