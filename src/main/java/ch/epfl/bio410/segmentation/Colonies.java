package ch.epfl.bio410.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import ch.epfl.bio410.utils.utils;


import java.nio.file.FileSystems;
import java.util.*;

public class Colonies {
    /** The goal of this class is to group clusters of bacteria into colonies.
     * We first use connected components labeling to segment the image into touching bacteria with a unique label per colony.
     * Then, we expand these labels in the first frame with a Voronoi diagram to obtain a "region" for each colony.
     * Finally, over each frame, we assign a given group of bacteria to a colony if they are mainly in the same region of the Voronoi diagram.
     */
    private ImagePlus imageDIC; // holds the original DIC image
    public ImagePlus voronoiDiagrams; // contains the Voronoi diagrams for each frame, if kept
    private ImageStack voronoiDiagramStack; // holds the Voronoi diagrams for each frame
    public ImagePlus colonyLabels; // contains the colony labels, with consistent values between frames
    public Map<Integer, double[][]> colonyStats = new HashMap<>(); // holds the statistics for each frame
    private CLIJ2 clij2; // the CLIJ2 instance used for image processing
    private final LUT glasbeyLUT = utils.getGlasbeyLUT();
    public final Map<String, Integer> columnMapping = new HashMap<>();

    /**
     * Constructor for Colonies.
     * After instantiation, call runColoniesComputation to compute the colonies.
     * One can then access the colony labels and statistics : this.colonyLabels, this.colonyStats
     * @param imageDIC ImagePlus object containing the DIC channel
     */
    public Colonies(ImagePlus imageDIC) {
        this.clij2 = CLIJ2.getInstance();
        this.imageDIC = imageDIC;
        setColumnMapping();
    }



    /**
     * This method sets the column mapping for the statistics table from CLIJ2.
     * Usage : stats[i][columnMapping.get("IDENTIFIER")];
     */
    private void setColumnMapping() {
        columnMapping.put("IDENTIFIER", 0);
        columnMapping.put("BOUNDING_BOX_X", 1);
        columnMapping.put("BOUNDING_BOX_Y", 2);
        columnMapping.put("BOUNDING_BOX_Z", 3);
        columnMapping.put("BOUNDING_BOX_END_X", 4);
        columnMapping.put("BOUNDING_BOX_END_Y", 5);
        columnMapping.put("BOUNDING_BOX_END_Z", 6);
        columnMapping.put("BOUNDING_BOX_WIDTH", 7);
        columnMapping.put("BOUNDING_BOX_HEIGHT", 8);
        columnMapping.put("BOUNDING_BOX_DEPTH", 9);
        columnMapping.put("MINIMUM_INTENSITY", 10);
        columnMapping.put("MAXIMUM_INTENSITY", 11);
        columnMapping.put("MEAN_INTENSITY", 12);
        columnMapping.put("SUM_INTENSITY", 13);
        columnMapping.put("STANDARD_DEVIATION_INTENSITY", 14);
        columnMapping.put("PIXEL_COUNT", 15);
        columnMapping.put("SUM_INTENSITY_TIMES_X", 16);
        columnMapping.put("SUM_INTENSITY_TIMES_Y", 17);
        columnMapping.put("SUM_INTENSITY_TIMES_Z", 18);
        columnMapping.put("MASS_CENTER_X", 19);
        columnMapping.put("MASS_CENTER_Y", 20);
        columnMapping.put("MASS_CENTER_Z", 21);
        columnMapping.put("SUM_X", 22);
        columnMapping.put("SUM_Y", 23);
        columnMapping.put("SUM_Z", 24);
        columnMapping.put("CENTROID_X", 25);
        columnMapping.put("CENTROID_Y", 26);
        columnMapping.put("CENTROID_Z", 27);
        columnMapping.put("SUM_DISTANCE_TO_MASS_CENTER", 28);
        columnMapping.put("MEAN_DISTANCE_TO_MASS_CENTER", 29);
        columnMapping.put("MAX_DISTANCE_TO_MASS_CENTER", 30);
        columnMapping.put("MAX_MEAN_DISTANCE_TO_MASS_CENTER_RATIO", 31);
        columnMapping.put("SUM_DISTANCE_TO_CENTROID", 32);
        columnMapping.put("MEAN_DISTANCE_TO_CENTROID", 33);
        columnMapping.put("MAX_DISTANCE_TO_CENTROID", 34);
        columnMapping.put("MAX_MEAN_DISTANCE_TO_CENTROID_RATIO", 35);
    }

    /**
     * This method finds colony in a thresholded DIC channel.
     * First, we label the connected components in the first frame.
     * Then, we assign labels to bacteria in the following frames based on the Voronoi diagram of the previous frame.
     * @param minLabelArea minimum area of a label to be considered a colony
     * If you want to keep the Voronoi diagrams, set keepVoronoi as a second argument to true.
     */
    public void runColoniesComputation(double minLabelArea) {
        runColoniesComputation(minLabelArea, false);
    }

    /**
     * This method finds colony in a thresholded DIC channel.
     * First, we label the connected components in the first frame.
     * Then, we assign labels to bacteria in the following frames based on the Voronoi diagram of the previous frame.
     * @param minLabelArea minimum area of a label to be considered a colony
     * @param keepVoronoi boolean to keep the Voronoi diagrams
     */
    public void runColoniesComputation(double minLabelArea, boolean keepVoronoi) {
        //////////////////////////////////////////////////
        // Improved method :
        // - Get conn comp labels for the first frame
        // - Compute the area of labels for that frame
        // - Discard labels that are too small based on area (given pixel thresh)
        // - Compute the Voronoi diagram for that frame
        // - On the next frame, *assign labels purely based on Voronoi diag from before*
        // - Begin again w/o connected comp labeling
        ///////////////////////////////////////////////

        // Create a stack to hold the processed frames
        ImageStack processedStack = new ImageStack(this.imageDIC.getWidth(), this.imageDIC.getHeight());
        ImagePlus prevFrameVoronoi = null; // used to store the Voronoi diagram of the previous frame
        if (keepVoronoi) {
            this.voronoiDiagramStack = new ImageStack(this.imageDIC.getWidth(), this.imageDIC.getHeight());
        }
        // Below is if we want to get diagram for all frames
        // ImageStack regionDiagramStack = new ImageStack(this.imageDIC.getWidth(), this.imageDIC.getHeight());
        IJ.log("Computing labels for bacteria");
        IJ.log("Processing " + this.imageDIC.getStackSize() + " frames");
        // Loop through each slice in the stack
        for (int i = 1; i <= this.imageDIC.getStackSize(); i++) {
            // Extract and copy the slice using substack
            ImageProcessor frame = this.imageDIC.getStack().getProcessor(i);
            ImagePlus slice = new ImagePlus("Slice", frame.duplicate());

            // Process the first frame with connected components labeling
            // Subsequent frames are processed with Voronoi diagram assignment
            // Connected components labeling is still used for filtering (see below)
            ImagePlus destinationImagePlus = null;
            // NOTE : the filtering has to be based on connected components labeling of the CURRENT frame
            // If we filter based on the assignment, from the Voronoi diagram, small labels will be assigned labels
            // from colonies, and will then evade the filtering as they will be considered part of a colony.
            // Therefore, we need to get the CC labels for the current frame, and filter the labels based on that.
            // It's a large slowdown, but it's the only way to ensure that we don't assign small labels to colonies.
            destinationImagePlus = connectedComponentsLabeling(slice);
            double[][] connCompStats = getLabelStats(destinationImagePlus, slice); // these stats are only used for filtering
            // Filter labels by area
            destinationImagePlus = filterLabelsByArea(destinationImagePlus, minLabelArea, connCompStats);
            // use the prev. frame's Voronoi diagram to assign labels
            if (i != 1) {
                // Binarize the labels after filtering (for assignment from Voronoi diagram)
                destinationImagePlus = binarize(destinationImagePlus);
                destinationImagePlus = assignLabelsFromVoronoi(prevFrameVoronoi, destinationImagePlus);
            }

            // get statistics from clij
            double[][] stats = getLabelStats(destinationImagePlus, slice); // these are the stats we want to keep
            this.colonyStats.put(i, stats);


            // Get Voronoi diagram of this frame and save it for next frame
            prevFrameVoronoi = voronoiDiagram(destinationImagePlus);
            if (keepVoronoi) {
                this.voronoiDiagramStack.addSlice(prevFrameVoronoi.getProcessor());
            }

            // Record the processed frame
            processedStack.addSlice(destinationImagePlus.getProcessor());

            // Remove any intermediate images
            slice.close();
            destinationImagePlus.close();
            IJ.log("Finished labeling frame " + i + "/" + this.imageDIC.getStackSize());
            //////////////
            // only first ten frames (for testing)
//            if (i == 10) {
//                break;
//            }
        }

    this.colonyLabels = new ImagePlus("Colony labels", processedStack);
    // Set Glasbey LUT
    this.colonyLabels.setLut(this.glasbeyLUT);

    // save pixel width "metadata" to ColonyLabels image too
    this.colonyLabels.getCalibration().setXUnit("µm");
    double pixelWidth = this.imageDIC.getCalibration().pixelWidth;
    double pixelHeight = this.imageDIC.getCalibration().pixelHeight;
    IJ.run(this.colonyLabels, "Properties...", "channels=1 slices=120 frames=1 pixel_width="+pixelWidth+" pixel_height="+pixelHeight+" voxel_depth=1.0");

        if (keepVoronoi) {
        this.voronoiDiagrams = new ImagePlus("Voronoi Diagrams", this.voronoiDiagramStack);
        this.voronoiDiagramStack = null;
        this.voronoiDiagrams.setLut(this.glasbeyLUT);
        }
    }
    /**
     * This method binarizes an image by setting all non-zero pixels to 1.
     * @param slice ImagePlus object to binarize
     * @return ImagePlus object with binarized pixels
     */
    private ImagePlus binarize(ImagePlus slice) {
        ImageProcessor processor = slice.getProcessor();
        for (int y = 0; y < processor.getHeight(); y++) {
            for (int x = 0; x < processor.getWidth(); x++) {
                if (processor.get(x, y) != 0) {
                    processor.set(x, y, 1);
                }
            }
        }
    return new ImagePlus("Binarized", processor);
    }
    /**
     * This method assigns labels to bacteria based on the Voronoi diagram of the previous frame.
     * @param prevFrameVoronoi ImagePlus object containing the Voronoi diagram of the previous frame
     * @param frameToLabel ImagePlus object containing the frame to label
     * @return ImagePlus object with assigned labels
     */
    private ImagePlus assignLabelsFromVoronoi(ImagePlus prevFrameVoronoi, ImagePlus frameToLabel) {
        // For the current frame, loop over the pixels in Voronoi.
        // If the frameToLabel pixel is not 0, assign the Voronoi pixel value to it
        ImageProcessor voronoi = prevFrameVoronoi.getProcessor();
        ImageProcessor frame = frameToLabel.getProcessor();
        ImageProcessor result = new ByteProcessor(frame.getWidth(), frame.getHeight());
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                if (frame.get(x, y) != 0) {
                    result.set(x, y, voronoi.get(x, y));
                }
            }
        }
    return new ImagePlus("Assigned labels", result);
    }

    /**
     * Computes stats using CLIJ2 for the given labels and DIC frame.
     * Note : the DIC frame is thresholded and binarized before computing the stats.
     * Therefore, the stats are not really related to the actual raw DIC frame intensity.
     * This is done this way only because we need the area of the labels to filter them.
     * @param labels ImagePlus object containing the instance labels
     * @param DICFrame ImagePlus object containing the DIC frame
     * @return double[][] containing the statistics for each label (see this.columnMapping)
     */
    public double[][] getLabelStats(ImagePlus labels, ImagePlus DICFrame) {
        ClearCLBuffer input = clij2.push(DICFrame);
        ClearCLBuffer labelmap = clij2.push(labels);
        double[][] stats = clij2.statisticsOfBackgroundAndLabelledPixels(input, labelmap);
        //    IDENTIFIER	BOUNDING_BOX_X	BOUNDING_BOX_Y	BOUNDING_BOX_Z	BOUNDING_BOX_END_X	BOUNDING_BOX_END_Y
        //    BOUNDING_BOX_END_Z	BOUNDING_BOX_WIDTH	BOUNDING_BOX_HEIGHT	BOUNDING_BOX_DEPTH	MINIMUM_INTENSITY
        //    MAXIMUM_INTENSITY	MEAN_INTENSITY	SUM_INTENSITY	STANDARD_DEVIATION_INTENSITY
        //    PIXEL_COUNT	SUM_INTENSITY_TIMES_X	SUM_INTENSITY_TIMES_Y	SUM_INTENSITY_TIMES_Z	MASS_CENTER_X
        //    MASS_CENTER_Y	MASS_CENTER_Z	SUM_X	SUM_Y	SUM_Z	CENTROID_X	CENTROID_Y	CENTROID_Z
        //    SUM_DISTANCE_TO_MASS_CENTER	MEAN_DISTANCE_TO_MASS_CENTER
        //    MAX_DISTANCE_TO_MASS_CENTER	MAX_MEAN_DISTANCE_TO_MASS_CENTER_RATIO	SUM_DISTANCE_TO_CENTROID
        //    MEAN_DISTANCE_TO_CENTROID	MAX_DISTANCE_TO_CENTROID	MAX_MEAN_DISTANCE_TO_CENTROID_RATIO
        clij2.release(input);
        clij2.release(labelmap);
        return stats;
    }
    /**
     * This method saves the colony labels and Voronoi diagrams to the specified path.
     * @param path String containing the path to save the results
     * @param filename String containing the filename of the source image
     */
    public void saveResults(String path, String filename) {
        // Save the colony labels
        String coloniesPath = path + FileSystems.getDefault().getSeparator() + filename + "_colony_labels.tif";
        IJ.log("Saving colony labels to " + coloniesPath);
        IJ.saveAsTiff(this.colonyLabels, coloniesPath);
        // Save the Voronoi diagrams
        if (this.voronoiDiagrams != null) {
            String voronoiPath = path + FileSystems.getDefault().getSeparator() + filename + "_voronoi_diagrams.tif";
            IJ.log("Saving Voronoi diagrams to " + voronoiPath);
            IJ.saveAsTiff(this.voronoiDiagrams, voronoiPath);
        }
    }
    /**
     * This method filters the labels based on their area.
     * @param labels ImagePlus object containing the labels
     * @param minLabelArea double containing the minimum area of a label to be considered a colony
     * @param stats double[][] containing the statistics for each label
     * @return ImagePlus object containing the filtered labels
     */
    private ImagePlus filterLabelsByArea(ImagePlus labels, double minLabelArea, double[][] stats) {
        // Create a new image processor to store the filtered labels
        ImageProcessor result = labels.getProcessor().duplicate();
        // Get the IDENTIFIER column
        int[] identifiers = Arrays.stream(stats).mapToInt(row -> (int) row[columnMapping.get("IDENTIFIER")]).toArray();
        // Loop over the labels
        for (int i = 0; i < identifiers.length; i++) {
            // Check if the area of the label is smaller than the minimum area
            double area = stats[i][columnMapping.get("PIXEL_COUNT")];
            if (area< minLabelArea) {
                // If it is, set the label to 0
                int label = identifiers[i];
                IJ.log("Removing label " + label + " with area " + area);
                for (int y = 0; y < result.getHeight(); y++) {
                    for (int x = 0; x < result.getWidth(); x++) {
                        float pixel = result.getf(x, y);
                        if (pixel == label && pixel != 0) {
                            result.set(x, y, 0);
                        }
                    }
                }
            }
        }
        return new ImagePlus("Filtered labels", result);
    }

    /**
     * This method computes the connected components labeling of an image.
     * @param slice ImagePlus object containing the slice to process. Should be binarized (mask from DIC channel)
     * @return ImagePlus object containing the connected components labeling
     */
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
    /**
     * This method computes the Voronoi diagram of an image,
     * by expanding instance labels using extendLabelingViaVoronoi from CLIJ2.
     * @param slice ImagePlus object containing the slice to process
     * @return ImagePlus object containing the Voronoi diagram
     */
    private ImagePlus voronoiDiagram(ImagePlus slice) {
        ClearCLBuffer input = clij2.push(slice);
        ClearCLBuffer destination = clij2.create(input);
        clij2.extendLabelingViaVoronoi(input, destination);

        ImagePlus destinationImagePlus = clij2.pull(destination);
        destinationImagePlus.setTitle("Voronoi Diagram");

        clij2.release(input);
        clij2.release(destination);
        return destinationImagePlus;
    }


}


