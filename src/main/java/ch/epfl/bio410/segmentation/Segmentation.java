package ch.epfl.bio410.segmentation;

import ij.IJ;
import ij.ImagePlus;

/**
 * This class implements Segmentation.
 */

public class Segmentation {

    public static void segment(ImagePlus imp) {
        // Otsu, Default and IsoData work well
        IJ.run(imp, "Convert to Mask", "method=Otsu background=Light calculate black");
    }

}
