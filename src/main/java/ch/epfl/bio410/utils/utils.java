package ch.epfl.bio410.utils;

import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.plugin.ImageCalculator;
import ij.IJ;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.plugin.LutLoader;

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This class implements utils functions
 */


public class utils {

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

    /**
     * This method removes noise from the image by applying a median filter
     * @param imp contains the pixel data of the image and some basic methods to manipulate it.
     * @return the processed image as an ImagePlus
     */
    public static ImagePlus remove_noise(ImagePlus imp) {
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

    /**
     * This method lists all files in a folder in the resources folder.
     *
     * @param folder The folder to list files from
     * @return A list of file paths
     */
    public static java.util.List<String> listFilesInResourceFolder(String folder) {
        URL url = TrackingConfig.class.getClassLoader().getResource(folder);
        System.out.println(url);
        if (url == null) {
            return null;
        }

        List<String> files = new ArrayList<>();
        try {
            URI uri = url.toURI();
            if (uri.getScheme().equals("file")) {
                // Running from IDE - read files directly from the filesystem
                File[] fileList = new File(uri).listFiles();
                if (fileList != null) {
                    for (File file : fileList) {
                        if (file.isFile()) {
                            files.add(file.getAbsolutePath());
                        }
                    }
                }
            } else if (uri.getScheme().equals("jar")) {
                // Running from JAR - read entries in the JAR file
                String[] parts = uri.toString().split("!");
                // if jar name contains -sources, remove it
                if (parts[0].contains("-sources")) {
                    parts[0] = parts[0].replace("-sources", "");
                }
                try (JarFile jarFile = new JarFile(parts[0].substring(5))) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith(folder + "/") && !name.equals(folder + "/")) {
                            files.add(name);
                        }
                    }
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            return null;
        }
        return files;
    }

    /**
     * Reads a CSV file and returns a list of CSV records (columns).
     * @param csvFilePath Path to the CSV file
     * @return List of CSV records
     * @throws IOException If an error occurs while reading the file
     */
    public static List<CSVRecord> readCsv(String csvFilePath, int skipped_lines) throws IOException {
        return readCsv(new File(csvFilePath), skipped_lines);
    }

    /**
     * Reads a CSV file and returns a list of CSV records (columns).
     * @param csvFile CSV file to read
     * @return List of CSV records
     * @throws IOException If an error occurs while reading the file
     */
    public static List<CSVRecord> readCsv(File csvFile, int skipped_lines) throws IOException {
        try (FileReader reader = new FileReader(csvFile);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            // Skip the first n-1 lines (header) and collect the remaining records
            return csvParser.getRecords().stream().skip(skipped_lines).collect(Collectors.toList());
        }
    }

    /**
     * This method adds the pixel size for original image (width and height) to the image properties
     * @param colonyLabels is the image to which we want to add the pixel size
     * @param imageDIC is the image from which we want to get the pixel size
     */
    public static void add_pixel_size(ImagePlus colonyLabels, ImagePlus imageDIC){
        int[] dimensions = colonyLabels.getDimensions();
        int channels = dimensions[2];
        int slices = dimensions[3];
        int frames = dimensions[4];
        colonyLabels.getCalibration().setXUnit("µm");
        double pixelWidth = imageDIC.getCalibration().pixelWidth;
        double pixelHeight = imageDIC.getCalibration().pixelHeight;
        IJ.run(colonyLabels, "Properties...", "channels="+channels+" slices="+slices+" frames="+frames+" pixel_width="+pixelWidth+" pixel_height="+pixelHeight+" voxel_depth=1.0");
    }

    public static boolean FileExists(String path, String fileName){
        return new File (Paths.get(path, "results", fileName).toString()).exists();
    }

    /**
     * Calculates descriptive statistics of the provided dataset, like the .describe() method in pandas
     * @param data Dataframe containing numerical values
     * @return Dataframe with descriptive statistics for each column
     * @throws IOException If and error occurs while printing and parsing CSV data
     */
    public static List<CSVRecord> describe(List<CSVRecord> data, int skipCols, String filePath) throws IOException {
        if (data.isEmpty()) {
            return Collections.emptyList();
        }

        // Get column names from the first record
        List<String> headers = new ArrayList<>(data.get(0).toMap().keySet());
        headers = headers.subList(skipCols, headers.size());

        // Data for each column
        Map<String, List<Double>> columnData = headers.stream()
                .collect(Collectors.toMap(
                        header -> header,
                        header -> data.stream().map(row -> Double.parseDouble(row.get(header))).collect(Collectors.toList())
                ));

        // Map to store all the data
        String[] statTypes = {"count", "mean", "std", "min", "25%", "50%", "75%", "max"};
        Map<String, Object[]> stats = new HashMap<String, Object[]>();
        for (String stat : statTypes) {
            stats.put(stat, new Object[headers.size()]);
        }

        // Calculate statistics for each column
        for (int i = 0; i < headers.size(); i++) {
            List<Double> values = columnData.get(headers.get(i));
            double[] valuesArray = values.stream().mapToDouble(Double::doubleValue).toArray();

            // Store each value
            stats.get("count")[i] = values.size();
            stats.get("mean")[i] = mean(valuesArray);
            stats.get("std")[i] = std(valuesArray);
            stats.get("min")[i] = Collections.min(values);
            stats.get("25%")[i] = percentile(valuesArray, 25);
            stats.get("50%")[i] = percentile(valuesArray, 50);
            stats.get("75%")[i] = percentile(valuesArray, 75);
            stats.get("max")[i] = Collections.max(values);
        }

        // Use CSVPrinter to create a CSV file
        FileWriter out = new FileWriter(filePath);
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT)) {
            printer.printRecord(makeTitledRow("statistic", headers.toArray()));
            for (String stat : statTypes) {
                printer.printRecord(makeTitledRow(stat, stats.get(stat)));
            }
        }

        // Parse the output back into CSVRecords
        return readCsv(filePath, 0);
    }

    private static Object[] makeTitledRow(String title, Object[] data) {
        List<Object> row = new ArrayList<>();
        row.add(title);

        // Add the double values to the row
        for (Object value : data) {
            row.add(value);
        }

        return row.toArray();
    }

    public static double mean(double[] values) {
        return DoubleStream.of(values).average().orElse(Double.NaN);
    }

    public static double std(double[] values) {

        double variance = DoubleStream.of(values).map(v -> Math.pow(v - mean(values), 2)).sum() / (values.length - 1);
        return Math.sqrt(variance);
    }

    public static double percentile(double[] values, double percentile) {
        Arrays.sort(values);
        int index = (int) Math.ceil((percentile / 100.0) * values.length);
        return values[index - 1];
    }
}