package ch.epfl.bio410.utils;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.GaussianBlur3D;
import ij.plugin.ImageCalculator;
import ij.IJ;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.plugin.LutLoader;

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;

/**
 * This class implements utils functions
 */


public class utils {

    public static String getFolderPathInResources(String folderName) {
        URL resourceURL = utils.class.getClassLoader().getResource(folderName);
        return Paths.get(resourceURL.getPath()).toString();
    }

    public static LUT getGlasbeyLUT() {
        InputStream lutStream = utils.class.getClassLoader().getResourceAsStream("glasbey.lut");
        IndexColorModel glasbeyLut = null;
        try {
            glasbeyLut = new LutLoader().open(lutStream);
        } catch (IOException e) {
            IJ.log("Failed to load Glasbey LUT");
            e.printStackTrace();
        }
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        glasbeyLut.getBlues(b);
        glasbeyLut.getGreens(g);
        glasbeyLut.getReds(r);

        // Set first color to black
        r[0] = (byte) Color.BLACK.getRed();
        g[0] = (byte) Color.BLACK.getGreen();
        b[0] = (byte) Color.BLACK.getBlue();

        return new LUT(r, g, b);
    }

    public static ImagePlus remove_noise(ImagePlus imp, double sigma) {
        //imp.show();
        /* Subtract Background but segmentation actually works best without subtracting background
            - Background subtraction plugin causes halo in background
            - Manual BS (using image calculator) causes for black background to be same color as bacteria
        Integer rolling = 100;
        IJ.run(imp, "Subtract Background...", "rolling="+rolling+" light sliding stack"); */
        // Despeckle
        IJ.run(imp, "Median...", "radius=2 stack");
        //IJ.run(imp,"Despeckle", "stack"); same but with radius 1

        return imp; //dog(imp, sigma); It seems that dog isn't useful either
    }


    /**
     * This method performs a difference of Gaussian, removing high-frequency spatial detail (random noise).
     * Here enhancing edges allows for spot detection later in the detect() function.
     * Dog works with subtraction a blurred version of the image to a less blurry one (different sigma)
     *
     * @param imp contains the pixel data of the image and some basic methods to manipulate it.
     * @param sigma is the standard deviation of the gaussian we want to apply to the image to blur it
     * @return the processed image as an ImagePlus
     */
    public static ImagePlus dog(ImagePlus imp, double sigma) {
        ImagePlus g1 = imp.duplicate(); // Duplicate the input image
        ImagePlus g2 = g1.duplicate(); // Duplicate the image again
        double sigma2 = Math.sqrt(2) * sigma;
        GaussianBlur3D.blur(g1, sigma, sigma, 0);
        GaussianBlur3D.blur(g2, sigma2, sigma2, 0);
        ImagePlus dog = ImageCalculator.run(g1, g2, "Subtract create stack");
        return dog;

    }

}