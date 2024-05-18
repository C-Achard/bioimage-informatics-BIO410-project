package ch.epfl.bio410.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.frame.RoiManager;
//import java.lang.Object;
import ij.measure.ResultsTable;

import java.util.Arrays;
import java.util.List;

import static ch.epfl.bio410.utils.utils.read_csv;
import static ij.IJ.selectWindow;
import static ij.plugin.frame.RoiManager.multiMeasure;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
//import java.lang.Cloneable;


/**
 * This class implements Segmentation.
 */

public class segmentation {

    public static void segment(ImagePlus imp) {
        IJ.run(imp, "Maximum...", "radius=1.5 stack"); // for the watershed to work better
        // Otsu, Default and IsoData work well
        IJ.setAutoThreshold(imp, "Yen dark");
        Prefs.blackBackground = true;
        IJ.run(imp, "Convert to Mask", "method=Yen background=Light calculate black");
        //IJ.run(imp, "Invert", "stack");
        IJ.run(imp, "Watershed", "stack"); //try adjustable watershed instead (installable plugin)

        // Add labels:
        find_centroids(imp);


    }

    // find centroid data in DATA
    public static void find_centroids(ImagePlus imp) {
        IJ.run("Set Measurements...", "area centroid limit add redirect=None decimal=3");
        IJ.run(imp, "Analyze Particles...", "size=25-Infinity pixel display exclude summarize overlay add composite stack");

        IJ.selectWindow("Results");
        IJ.saveAs("Results", "C:/Users/mathi/OneDrive/Documents/EPFL/MA4/BioimageAnalysis/Project/bioimage-informatics-BIO410-project/DATA/analyze_particle_results/Results.csv");
        //IJ.saveAs("Results", "./../../../../../../../DATA/analyze_particle_results/Results.csv");
        //selectWindow("Results");
        //IJ.run("Close");

        IJ.selectWindow("Summary of C1-Merged-2.tif");
        IJ.saveAs("Results", "C:/Users/mathi/OneDrive/Documents/EPFL/MA4/BioimageAnalysis/Project/bioimage-informatics-BIO410-project/DATA/analyze_particle_results/Summary.csv");
        //IJ.saveAs("Results", "./../../../../../../../DATA/analyze_particle_results/Summary.csv"); // size of bact gets bigger so make adjustable watershed

        List<List<String>> results = read_csv("C:/Users/mathi/OneDrive/Documents/EPFL/MA4/BioimageAnalysis/Project/bioimage-informatics-BIO410-project/DATA/analyze_particle_results/Results.csv");
        List<List<String>> summary = read_csv("C:/Users/mathi/OneDrive/Documents/EPFL/MA4/BioimageAnalysis/Project/bioimage-informatics-BIO410-project/DATA/analyze_particle_results/Summary.csv");

        double[] centroid = get_centroid(1, results);
        System.out.print(Arrays.toString(centroid));
        int bact_number_frame_1 = get_initial_bact_number(summary);

    }


    public static double euclidian_distance(double[] centroidA, double[] centroidB) {
        return sqrt(pow(centroidA[1] - centroidB[1], 2) + pow(centroidA[2] - centroidB[2], 2));
    }


    public static void get_next_bact() {

    }

    public static double[] get_centroid(int bacteria_label, List<List<String>> results) {
        double X = Double.parseDouble(results.get(bacteria_label).get(2));
        double Y = Double.parseDouble(results.get(bacteria_label).get(3));
        return new double[] {X,Y};
    }

    public static int get_initial_bact_number(List<List<String>> summary) {
        int total = Integer.parseInt((summary.get(1).get(1)));
        return total;
    }

}


//IJ.run("Set Measurements...", "area centroid limit redirect=None decimal=3");
//IJ.run(imp, "Analyze Particles...", "size=25-Infinity pixel show=[Count Masks] display exclude summarize overlay add composite stack");
//IJ.run(" ROIs to Label image");
//imp = imp.duplicate();
//IJ.run(imp, "glasbey", "");
//IJ.run(imp, "Analyze Particles...", "size=25-Infinity pixel show=Masks display exclude summarize overlay add stack");
