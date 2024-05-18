package ch.epfl.bio410.utils;

import com.opencsv.CSVReader;
import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.plugin.ImageCalculator;
import ij.IJ;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class implements utils functions
 */


public class utils {

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
/*
    public static List<List<Double>> read_csv(){
        String fileName = ".../DATA/analyze_particle_results/Results.csv";
        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            List<String[]> r = reader.readAll();
            List<List<Double>> myList = new ArrayList<>();

            r.forEach(lineArr -> {
                List<Double> line = new ArrayList<>();
                for (String str : lineArr) {
                    line.add(Double.parseDouble(str));
                }
                myList.add(line);
            });
        }
        return myList;
    }*/

    public static List<List<String>> read_csv(String path){
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return records;
    }

}