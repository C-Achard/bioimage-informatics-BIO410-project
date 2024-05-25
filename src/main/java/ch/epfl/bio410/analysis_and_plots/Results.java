package ch.epfl.bio410.analysis_and_plots;

import ch.epfl.bio410.segmentation.Colonies;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements functions to analyze the results of the tracking
 * And specifically to link colonies and tracks
 */

public class Results {


    /**
     * This method assigns labels to tracks based on the position of the colonies in the first frame of the track.
     * Assign each track to a colony label (unique color).
     * Also checks label != 0 and assigns to closest, or zero label if it is.
     * @param tracks List of tracks from the tracking CSV file
     * @param colonyLabels ImagePlus object containing the image with colony labels
     */
    public static void assignTracksToColonies(
            List<CSVRecord> tracks , ImagePlus colonyLabels, String imageNameWithoutExtension){
        ImageStack stack = colonyLabels.getImageStack();
        int[] labelsArray = new int[tracks.size()];
        int index = 0;
        for (CSVRecord track : tracks) {
            int frame = (int) Double.parseDouble(track.get("TRACK_START")); // for each track get start_frame
            double x_micron = Double.parseDouble(track.get("TRACK_X_LOCATION"));
            double y_micron = Double.parseDouble(track.get("TRACK_Y_LOCATION"));

            // get the pixel size in microns
            double pixelWidth = colonyLabels.getCalibration().pixelWidth;
            double pixelHeight = colonyLabels.getCalibration().pixelHeight;
            // convert the micron coordinates to pixel coordinates
            int x_pixel = (int) (x_micron / pixelWidth);
            int y_pixel = (int) (y_micron / pixelHeight);

            // get the label of the colony at the position of the track
            ImageProcessor ip = stack.getProcessor(frame+1); // frame 0 in csv but frames start at 1 in imageJ
            int label = ip.getPixel(x_pixel, y_pixel); // getInterpolatedPixel
            // if the label is 0, get the label of the closest non-zero pixel
            if (label == 0) {
                label = getClosestNonZeroLabel(ip, x_pixel, y_pixel);
            }
            labelsArray[index] = label;
            index++;
        }

        // add labelsarray as new feature of tracks
        // and save to new csv in results folder
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(System.getProperty("user.dir"), "DATA", "results", "tracks_with_colonylabels_" + imageNameWithoutExtension + ".csv").toString()));
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

            // Write header
            List<String> header = new ArrayList<>();
            for (String head : tracks.get(0).toMap().keySet()) {
                header.add(head);
            }
            header.add("COLONY_LABEL");
            csvPrinter.printRecord(header);

            // Write records
            for (int i = 0; i < tracks.size(); i++) {
                CSVRecord record = tracks.get(i);
                List<String> newRecord = new ArrayList<>();
                for (String value : record) {
                    newRecord.add(value);
                }
                newRecord.add(String.valueOf(labelsArray[i]));
                csvPrinter.printRecord(newRecord);
            }

            csvPrinter.flush();
            csvPrinter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * When the label zero is assigned, this function finds closest non-zero colony label, if any, and returns it
     * By default this will search in a 5x5 pixel neighborhood.
     * @param ip ImageProcessor of the image
     * @param x track position x
     * @param y track position y
     * @return non-zero label of the closest colony
     */
    public static int getClosestNonZeroLabel(ImageProcessor ip, int x, int y) {
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                // skip the center pixel
                if (i == 0 && j == 0) continue;
                if (x + i >= 0 && x + i < ip.getWidth() && y + j >= 0 && y + j < ip.getHeight()) {
                    try {
                        // get the label of the neighboring pixel
                        int label = ip.getPixel(x + i, y + j);
                        // if the label is non-zero, return it
                        if (label != 0) return label;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // skip if the pixel is out of bounds
                        e.printStackTrace();
                    }
                }
            }
        }
        // If no non-zero label was found within the 3 pixel neighborhood, return 0
        return 0;
    }



    public List<double[][]> getColonyFeatures(String track_ID, List<CSVRecord> tracks, ImagePlus labels, ImagePlus imageDIC) {
        //ImagePlus labels = colony.colonyLabels;
        Colonies colony = new Colonies(labels);
        List<double[][]> colonyFeatures = new ArrayList<>();
        for (CSVRecord track : tracks) {
            if (track.get("TRACK_ID").equals(track_ID)) {
                int start_frame = (int)Double.parseDouble(track.get("TRACK_START"));
                int end_frame = (int)Double.parseDouble(track.get("TRACK_STOP"));
                for(int i = start_frame; i <= end_frame; i++){
                    // Get the label statistics for the colony in the current frame
                    ImageProcessor ip_labels = labels.getImageStack().getProcessor(i+1); // get frame i of labels
                    ImagePlus label = new ImagePlus("Frame", ip_labels.duplicate()); //get imageplus of frame i
                    ImageProcessor ip_DIC = imageDIC.getImageStack().getProcessor(i+1); // get frame i of dic
                    ImagePlus DICframe = new ImagePlus("Frame", ip_DIC.duplicate()); //get imageplus of frame i for dic too
                    double[][] stats = colony.getLabelStats(label, DICframe);
                    colonyFeatures.add(stats);
                }
            }
        }
        return colonyFeatures;
    }


    /* Get the colony label of a track */
    public int getLabel(String track_ID, List<CSVRecord> tracks_with_labels) {
        for (CSVRecord track_with_label : tracks_with_labels) {
            if (track_with_label.get("TRACK_ID").equals(track_ID)) {
                return (int)Double.parseDouble(track_with_label.get("COLONY_LABEL"));
            }
        }
        return -1;
    }




}
