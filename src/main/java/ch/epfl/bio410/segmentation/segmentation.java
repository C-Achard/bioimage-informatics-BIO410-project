package ch.epfl.bio410.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.frame.RoiManager;
//import java.lang.Object;
import ij.measure.ResultsTable;

import static ij.plugin.frame.RoiManager.multiMeasure;
//import java.lang.Cloneable;


/**
 * This class implements Segmentation.
 */

public class segmentation {

    public static void segment(ImagePlus imp) {
        // Otsu, Default and IsoData work well
        IJ.setAutoThreshold(imp, "Otsu dark");
        Prefs.blackBackground = true;
        IJ.run(imp, "Convert to Mask", "method=Otsu background=Dark calculate black");
        IJ.run(imp, "Invert", "stack");
        IJ.run(imp, "Watershed", "stack"); //try adjustable watershed instead (installable plugin)
    }

    // find centroid data in DATA
    public static void getcentroid(ImagePlus imp){
        IJ.run("Set Measurements...", "area centroid limit add redirect=None decimal=3");
        IJ.run(imp, "Analyze Particles...", "size=25-Infinity pixel display exclude summarize overlay add composite stack");

        IJ.saveAs("Results", "C:/Users/mathi/OneDrive/Documents/EPFL/MA4/BioimageAnalysis/Project/bioimage-informatics-BIO410-project/DATA/analyze_particle_results/Results.csv");
        IJ.saveAs("Summary", "C:/Users/mathi/OneDrive/Documents/EPFL/MA4/BioimageAnalysis/Project/bioimage-informatics-BIO410-project/DATA/analyze_particle_results/Summary.csv");
        // size of bact gets bigger so make adjustable watershed
    }

}
