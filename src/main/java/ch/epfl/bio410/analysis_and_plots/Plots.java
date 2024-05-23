// TODO : find suitable library for plotting and implement plots
package ch.epfl.bio410.analysis_and_plots;

import ij.ImagePlus;
import ij.gui.NewImage;
import org.knowm.xchart.*;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.knowm.xchart.style.lines.SeriesLines;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
// import javax.imageio.ImageWriter;
// import javax.imageio.spi.IIORegistry;
// import javax.imageio.spi.ImageWriterSpi;
import javax.swing.*;

import static ij.IJ.openImage;
// import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriterSpi;

public class Plots {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please provide the path to the CSV file.");
            return;
        }

        String csvFilePath = args[0];
        String outputDirectory;

        if (args.length >= 2) {
            outputDirectory = args[1];
        } else {
            File csvFile = new File(csvFilePath);
            String baseName = csvFile.getName().substring(0, csvFile.getName().lastIndexOf('.'));
            outputDirectory = csvFile.getParent() + File.separator + baseName;
        }

        // Create the output directory if it doesn't exist
        File outputDir = new File(outputDirectory);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // // Register the TIFF image writer SPI
        // IIORegistry registry = IIORegistry.getDefaultInstance();
        // registry.registerServiceProvider(new TIFFImageWriterSpi());

        try {
            List<CSVRecord> dataRows = readCsv(csvFilePath);
            Map<Integer, List<CSVRecord>> groupedData = groupByTrackId(dataRows);
            for (Map.Entry<Integer, List<CSVRecord>> entry : groupedData.entrySet()) {
                Integer trackId = entry.getKey();
                List<CSVRecord> rows = entry.getValue();
                JPanel chartPanel = createChartPanel(trackId, rows);
                saveChartPanelAsPNG(chartPanel, outputDirectory + File.separator + "plot_track_" + trackId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Display the chart panel as an ImagePlus window.
     * @param chartPanel The chart panel to display
     */
    public static void displayChartAsImagePlus(JPanel chartPanel) {
        // Create an ImagePlus window
        ImagePlus imagePlus = NewImage.createRGBImage("Chart", chartPanel.getWidth(), chartPanel.getHeight(), 1, NewImage.FILL_WHITE);
        imagePlus.show();

        // Get the Graphics object of the ImagePlus
        Graphics g = imagePlus.getProcessor().getBufferedImage().getGraphics();

        // Paint the chart panel onto the ImagePlus
        chartPanel.paint(g);

        // Update the ImagePlus window
        imagePlus.updateAndDraw();
    }

    /**
     * Display the chart as an ImagePlus window.
     * @param chart The chart to display
     */
    public static void displayChartAsImagePlus(XYChart chart) {
        // Convert the chart to a BufferedImage
        BufferedImage chartImage = new BufferedImage(chart.getWidth(), chart.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = chartImage.createGraphics();
        chart.paint(g2, chart.getWidth(), chart.getHeight());
        g2.dispose();

        // Convert the BufferedImage to an ImagePlus
        ImagePlus imagePlus = new ImagePlus("Chart", chartImage);

        // Display the ImagePlus
        imagePlus.show();
    }

    public static List<CSVRecord> readCsv(String csvFilePath) throws IOException {
        try (FileReader reader = new FileReader(csvFilePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            return csvParser.getRecords().stream().skip(3).collect(Collectors.toList());
        }
    }
    public static List<CSVRecord> readCsv(File csvFile) throws IOException {
        try (FileReader reader = new FileReader(csvFile);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            return csvParser.getRecords().stream().skip(3).collect(Collectors.toList());
        }
    }

    public static Map<Integer, List<CSVRecord>> groupByTrackId(List<CSVRecord> dataRows) {
        return dataRows.stream().collect(Collectors.groupingBy(row -> Integer.parseInt(row.get("TRACK_ID"))));
    }

    public static JPanel createChartPanel(Integer trackId, List<CSVRecord> rows) {
        rows.sort(Comparator.comparingInt(row -> Integer.parseInt(row.get("FRAME"))));

        List<Double> xData = rows.stream().map(row -> Double.parseDouble(row.get("POSITION_X"))).collect(Collectors.toList());
        List<Double> yData = rows.stream().map(row -> Double.parseDouble(row.get("POSITION_Y"))).collect(Collectors.toList());
        List<Double> timeData = rows.stream().map(row -> Double.parseDouble(row.get("POSITION_T"))).collect(Collectors.toList());
        List<Double> intensityData = rows.stream().map(row -> Double.parseDouble(row.get("MEDIAN_INTENSITY_CH1"))).collect(Collectors.toList());

        // Create the first chart (POSITION_X vs POSITION_Y)
        XYChart chart1 = new XYChartBuilder().width(800).height(400).title("Track ID: " + trackId + " (POSITION_X vs POSITION_Y)")
                .xAxisTitle("POSITION_X").yAxisTitle("POSITION_Y").build();
        XYSeries series1 = chart1.addSeries("Track " + trackId, xData, yData);
        series1.setMarker(SeriesMarkers.NONE);
        series1.setLineStyle(SeriesLines.SOLID);

        // Create the second chart (POSITION_T vs MEDIAN_INTENSITY_CH1)
        XYChart chart2 = new XYChartBuilder().width(800).height(400).title("Track ID: " + trackId + " (POSITION_T vs MEDIAN_INTENSITY_CH1)")
                .xAxisTitle("POSITION_T").yAxisTitle("MEDIAN_INTENSITY_CH1").build();
        XYSeries series2 = chart2.addSeries("Track " + trackId, timeData, intensityData);
        series2.setMarker(SeriesMarkers.NONE);
        series2.setLineStyle(SeriesLines.SOLID);

//        displayChartAsImagePlus(chart1);
//        displayChartAsImagePlus(chart2);

        // Combine the charts into a single panel
        JPanel chartPanel = new JPanel(new GridLayout(2, 1));
        chartPanel.add(new XChartPanel<>(chart1));
        chartPanel.add(new XChartPanel<>(chart2));

        return chartPanel;
    }

    /**
     * Plot the specified features for a single track.
     * @param trackId The ID of the track to plot
     * @param rows List of CSV records containing the data (sorted by frame)
     * @param xFeature The feature to plot on the x-axis
     * @param yFeature The feature to plot on the y-axis
     * @return JPanel containing the chart
     */
    public static JPanel plotFeatures(Integer trackId , List<CSVRecord> rows, String xFeature, String yFeature) {
        rows.sort(Comparator.comparingInt(row -> Integer.parseInt(row.get("FRAME"))));

        List<Double> xData = rows.stream().map(row -> Double.parseDouble(row.get(xFeature))).collect(Collectors.toList());
        List<Double> yData = rows.stream().map(row -> Double.parseDouble(row.get(yFeature))).collect(Collectors.toList());

        // Create the chart with the specified features
        XYChart chart1 = new XYChartBuilder().width(800).height(400).title(
                "Track ID: " + trackId + " (" + xFeature + " vs " + yFeature + ")"
                ).xAxisTitle(xFeature).yAxisTitle(yFeature).build();
        XYSeries series1 = chart1.addSeries("Track " + trackId, xData, yData);
        series1.setMarker(SeriesMarkers.NONE);
        series1.setLineStyle(SeriesLines.SOLID);

        // Combine the charts into a single panel
        JPanel chartPanel = new JPanel(new GridLayout(1, 1));
        chartPanel.add(new XChartPanel<>(chart1));
        return chartPanel;
    }

    /**
     * Plot the specified feature for each track in the list of track IDs.
     * @param trackIds List of track IDs to plot
     * @param rows List of CSV records containing the data (sorted by track ID)
     * @param feature The feature to plot (see the CSV header for available features)
     * @return JPanel containing the chart
     */
    public static JPanel plotTracksFeatures(List<Integer> trackIds, List<CSVRecord> rows, String feature) {
        // Plot the feature for each track (track id is x, feature is y)
        // Use CategoryChart for discrete x-axis values
        Map<Integer, List<CSVRecord>> groupedData = groupByTrackId(rows);
        List<Double> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        for (Integer trackId : trackIds) {
            List<CSVRecord> trackData = groupedData.get(trackId);
            if (trackData != null) {
                xData.add((double) trackId);
                yData.add(Double.parseDouble(trackData.get(0).get(feature)));
            }
        }

        // Create the chart with the specified feature
        CategoryChart chart1 = new CategoryChartBuilder().width(800).height(400).title(
                "Track IDs vs " + feature
        ).xAxisTitle("Track ID").yAxisTitle(feature).build();
        CategorySeries series1 = chart1.addSeries(feature, xData, yData);
        series1.setMarker(SeriesMarkers.CIRCLE);

        JPanel chartPanel = new JPanel(new GridLayout(1, 1));
        chartPanel.add(new XChartPanel<>(chart1));
        return chartPanel;
    }

    public static void saveChartPanelAsPNG(JPanel chartPanel, String filePath) throws IOException {
        int width = 800;
        int height = 800;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        // Ensure the panel is fully rendered before capturing
        chartPanel.setSize(width, height);
        chartPanel.doLayout();
        chartPanel.print(g2);

        g2.dispose();

        ImageIO.write(image, "png", new File(filePath + ".png"));
        // Write the image as a TIFF file
        // ImageWriterSpi writerSpi = new TIFFImageWriterSpi();
        // ImageWriter writer = writerSpi.createWriterInstance();
        // writer.setOutput(ImageIO.createImageOutputStream(new File(filePath + ".tif")));
        // writer.write(image);
    }
    public static ImagePlus showSavedPlot(String filePath) {
        ImagePlus imp = openImage(filePath + ".png");
        imp.show();
        return imp;
    }
}