package ch.epfl.bio410.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.frame.RoiManager;
import java.lang.Object;
import ij.measure.ResultsTable;
import java.util.Arrays;
import static ij.IJ.selectWindow;
import static ij.plugin.frame.RoiManager.multiMeasure;
import java.lang.Cloneable;

import java.util.ArrayList;
import java.util.List;
import static ch.epfl.bio410.utils.utils.read_csv;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;


/**
 * This class implements the segmentation of the bacteria
 */

public class segmentation {

    /**
     * This method segments the bacteria in the image
     * AND finds the closest bacteria on next frame
     * by computing euclidian distance between centroid
     * of current and all bacteria on next frame
     * @param imp the image to segment
     */
    public static void segment(ImagePlus imp) {
        IJ.run(imp, "Maximum...", "radius=1.5 stack"); // for the watershed to work better
        // Otsu, Default and IsoData work well
        IJ.setAutoThreshold(imp, "Yen dark");
        Prefs.blackBackground = true;
        IJ.run(imp, "Convert to Mask", "method=Yen background=Light calculate black");
        //IJ.run(imp, "Invert", "stack");
        IJ.run(imp, "Watershed", "stack"); //try adjustable watershed instead (installable plugin)

        // Add labels:
        find_centroids(imp); // saves results in csv files
        List<List<String>> results = read_csv("C:/Users/mathi/OneDrive/Documents/EPFL/MA4/BioimageAnalysis/Project/bioimage-informatics-BIO410-project/DATA/analyze_particle_results/Results.csv");
        List<List<String>> summary = read_csv("C:/Users/mathi/OneDrive/Documents/EPFL/MA4/BioimageAnalysis/Project/bioimage-informatics-BIO410-project/DATA/analyze_particle_results/Summary.csv");

        // Stuff to check and print out
        System.out.print(summary.size());
        System.out.print(System.getProperty("user.dir"));

        // initialize a 2D array to store the new bacteria labels, its size should be X= number of bact in first frame, Y= number of frames

        double[][][] new_bacteria_labels = new double[results.size()][summary.size()][2];

        // loop over all frames
        for (int frame = 1; frame < summary.size(); frame++) {
            // for each bacteria in current frame, find the closest bacteria in next frame
            int bact_number_current_frame = get_bact_count(summary, frame);
            List<String> bact_ids_current_frame = get_bacteria_ids_of_frame(frame, results);
            for (String row : bact_ids_current_frame) {
            //for (int i = 0; i <= bact_ids_current_frame.size(); i++) {
                double[] returned = label_bacteria(Integer.parseInt(row), frame+1, results, summary);
                // store the new bacteria label in the 2D array
                new_bacteria_labels[(int)returned[0]][frame][0] = returned[1];
                new_bacteria_labels[(int)returned[0]][frame][1] = returned[2];
            }
        }

        // create a new image with the new bacteria labels
        // for each frame, for each bacteria, draw a circle around the centroid
        System.out.print(new_bacteria_labels);

    }


    // find centroid data in DATA
    public static void find_centroids(ImagePlus imp) {
        IJ.run("Set Measurements...", "area centroid stack limit add redirect=None decimal=3");
        IJ.run(imp, "Analyze Particles...", "size=25-Infinity pixel display exclude summarize overlay add composite stack");

        selectWindow("Results");
        IJ.saveAs("Results", "C:/Users/mathi/OneDrive/Documents/EPFL/MA4/BioimageAnalysis/Project/bioimage-informatics-BIO410-project/DATA/analyze_particle_results/Results.csv");
        //IJ.saveAs("Results", "./../../../../../../../DATA/analyze_particle_results/Results.csv");

        selectWindow("Summary of C1-Merged-2.tif");
        IJ.saveAs("Results", "C:/Users/mathi/OneDrive/Documents/EPFL/MA4/BioimageAnalysis/Project/bioimage-informatics-BIO410-project/DATA/analyze_particle_results/Summary.csv");
        //IJ.saveAs("Results", "./../../../../../../../DATA/analyze_particle_results/Summary.csv"); // size of bact gets bigger so make adjustable watershed
        // TODO: demander à Cyril comment faire pour que les fichiers soient sauvegardés dans le bon dossier (quel est le bon path)
    }


    /**
     * This method calculates the euclidian distance between two centroids
     * @param centroidA
     * @param centroidB
     * @return the euclidian distance between the two centroids
     */
    public static double euclidian_distance(double[] centroidA, double[] centroidB) {
        return sqrt(pow(centroidA[0] - centroidB[0], 2) + pow(centroidA[1] - centroidB[1], 2));
    }


    /**
     * This method labels the bacteria in the next frame
     * @param bacteria_label
     * @param frame2
     * @param results
     * @return the label of the closest bacteria
     */
    public static double[] label_bacteria(int bacteria_label, int frame2, List<List<String>> results, List<List<String>> summary){
        double[] current_centroid = get_centroid(bacteria_label, results);
        double min_distance = Double.MAX_VALUE;
        int closest_bacteria_label = -1;

        for (int i = 0; i <= get_bact_count(summary, frame2); i++) {
            double[] next_centroid = get_centroid(i+1, results);
            double distance = euclidian_distance(current_centroid, next_centroid);

            if (distance < min_distance) {
                min_distance = distance;
                closest_bacteria_label = i+1;
            }
        }
        double[] returned = new double[3];
        returned[0] = closest_bacteria_label;
        returned[1] = current_centroid[0];
        returned[2] = current_centroid[1];

        return returned;

    }


    /** This method iterates over each row in the results list.
     * If the frame number of the row (which is now the last element of the row) matches the given frame number,
     * it adds the bacteria ID of that row (which is the first element of the row) to the bacteria_ids list.
     * The method returns the bacteria_ids list, which contains the IDs of all bacteria in the given frame.
     * @param frame
     * @param results
     * @return the list of bacteria IDs in the given frame
     */
    public static List<String> get_bacteria_ids_of_frame(int frame, List<List<String>> results) {
        List<String> bacteria_ids = new ArrayList<>();

        for (List<String> row : results.subList(1, results.size())) {
            if (Integer.parseInt(row.get(row.size() - 1)) == frame) { // get the last element of the row for the frame number
                bacteria_ids.add(row.get(0)); // get the first element of the row for the bacteria id
            }
        }
        return bacteria_ids;
    }


    /**
     * This method returns the centroid of a bacteria
     * @param bacteria_label
     * @param results
     * @return the centroid of the bacteria
     */
    public static double[] get_centroid(int bacteria_label, List<List<String>> results) {
        double X = Double.parseDouble(results.get(bacteria_label).get(2));
        double Y = Double.parseDouble(results.get(bacteria_label).get(3));
        return new double[] {X,Y};
    }


    /**
     * This method returns the number of bacteria in a frame i
     * @param summary
     * @param i the frame number
     * @return the number of bacteria in the frame
     */
    public static int get_bact_count(List<List<String>> summary, int i) {
        int total = Integer.parseInt(summary.get(i).get(1));
        return total;
    }

}



// code for voronoi colony segmentation
//IJ.run("Set Measurements...", "area centroid limit redirect=None decimal=3");
//IJ.run(imp, "Analyze Particles...", "size=25-Infinity pixel show=[Count Masks] display exclude summarize overlay add composite stack");
//IJ.run(" ROIs to Label image");
//imp = imp.duplicate();
//IJ.run(imp, "glasbey", "");
//IJ.run(imp, "Analyze Particles...", "size=25-Infinity pixel show=Masks display exclude summarize overlay add stack");
