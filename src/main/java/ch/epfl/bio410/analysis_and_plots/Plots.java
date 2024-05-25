// TODO : find suitable library for plotting and implement plots
package ch.epfl.bio410.analysis_and_plots;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import ij.ImagePlus;
import ij.gui.NewImage;
// import io.scif.DefaultParser;
// import net.imagej.updater.CommandLine;

import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
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

@Command(name = "Plots", mixinStandardHelpOptions = true, version = "Plots 1.0",
        description = "Processes a CSV file and generates plots.")
public class Plots implements Runnable {

    @Parameters(index = "0", description = "Path to the CSV file")
    private String csvFilePath;

    @Option(names = {"-o", "--output"}, description = "Output directory for the plots")
    private String outputDirectory;

    @Option(names = {"-x"}, description = "Column name 1 for the histogram")
    private String hist1;

    @Option(names = {"-y"}, description = "Column name 2 for the histogram")
    private String hist2;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Plots()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        if (outputDirectory == null) {
            File csvFile = new File(csvFilePath);
            String baseName = csvFile.getName().substring(0, csvFile.getName().lastIndexOf('.'));
            outputDirectory = csvFile.getParent() + File.separator + baseName;
        }

        System.out.println("CSV File Path: " + csvFilePath);
        System.out.println("Output Directory: " + outputDirectory);
        System.out.println("Histogram Column 1: " + (hist1 != null ? hist1 : "Not Provided"));
        System.out.println("Histogram Column 2: " + (hist2 != null ? hist2 : "Not Provided"));

        // Create the output directory if it doesn't exist
        File outputDir = new File(outputDirectory);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // String[] cols = {
        //     "NUMBER_SPOTS", "NUMBER_GAPS", "LONGEST_GAP", "TRACK_DURATION", "TRACK_START", "TRACK_STOP", "TRACK_DISPLACEMENT", "TRACK_X_LOCATION", "TRACK_Y_LOCATION", "TRACK_MEAN_SPEED", "TRACK_MAX_SPEED", "TRACK_MIN_SPEED", "TRACK_MEDIAN_SPEED", "TRACK_STD_SPEED", "TRACK_MEAN_QUALITY", "TOTAL_DISTANCE_TRAVELED", "MAX_DISTANCE_TRAVELED", "CONFINEMENT_RATIO", "MEAN_STRAIGHT_LINE_SPEED", "LINEARITY_OF_FORWARD_PROGRESSION", "MEAN_DIRECTIONAL_CHANGE_RATE"
        // };

        try {
            List<CSVRecord> dataRows = readCsv(csvFilePath);
            if (hist1 != null) {
                // for (int i = 0; i < cols.length; i++) {
                //     createAndSaveHistogram(dataRows, cols[i], outputDirectory + File.separator + "hist_" + cols[i]);
                //     for (int j = i+1; j < cols.length; j++) {
                //         if (hist2 != null) {
                //             // createAndSaveHistogram(dataRows, hist2, outputDirectory + File.separator + "hist_" + hist2);
                //             if (cols[i] != cols[j]) createAndSaveHeatmap(dataRows, cols[i], cols[j], outputDirectory + File.separator + "heat_" + cols[i] + "_" + cols[j]);
                //         }
                //     }
                // }
                createAndSaveHistogram(dataRows, hist1, outputDirectory + File.separator + "hist_" + hist1);
                if (hist2 != null) {
                    if (hist1 != hist2) createAndSaveHistogram(dataRows, hist2, outputDirectory + File.separator + "hist_" + hist2);
                    createAndSaveHeatmap(dataRows, hist1, hist2, outputDirectory + File.separator + "heat_" + hist1 + "_" + hist2);
                }
            } else {
                Map<Integer, List<CSVRecord>> groupedData = groupByTrackId(dataRows);
                for (Map.Entry<Integer, List<CSVRecord>> entry : groupedData.entrySet()) {
                    Integer trackId = entry.getKey();
                    List<CSVRecord> rows = entry.getValue();
                    JPanel chartPanel = createChartPanel(trackId, rows);
                    saveChartPanelAsPNG(chartPanel, outputDirectory + File.separator + "plot_track_" + trackId);
                }
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
        XYChart chart1 = new XYChartBuilder().width(1600).height(800).title("Track ID: " + trackId + " (POSITION_X vs POSITION_Y)")
                .xAxisTitle("POSITION_X").yAxisTitle("POSITION_Y").build();
        XYSeries series1 = chart1.addSeries("Track " + trackId, xData, yData);
        chart1.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        series1.setMarker(SeriesMarkers.NONE);
        series1.setLineStyle(SeriesLines.SOLID);

        // Create the second chart (POSITION_T vs MEDIAN_INTENSITY_CH1)
        XYChart chart2 = new XYChartBuilder().width(1600).height(800).title("Track ID: " + trackId + " (POSITION_T vs MEDIAN_INTENSITY_CH1)")
                .xAxisTitle("POSITION_T").yAxisTitle("MEDIAN_INTENSITY_CH1").build();
        XYSeries series2 = chart2.addSeries("Track " + trackId, timeData, intensityData);
        chart2.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
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
        XYChart chart1 = new XYChartBuilder().width(1600).height(800).title(
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
        CategoryChart chart1 = new CategoryChartBuilder().width(1600).height(800).title(
                "Track IDs vs " + feature
        ).xAxisTitle("Track ID").yAxisTitle(feature).build();
        chart1.getStyler().setXAxisLabelRotation(90);
        chart1.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart1.getStyler().setDefaultSeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Stick);
        chart1.getStyler().setXAxisTickMarkSpacingHint(10);

        CategorySeries series1 = chart1.addSeries(feature, xData, yData);
        series1.setMarker(SeriesMarkers.CIRCLE);
        // rotate the x-axis labels

        JPanel chartPanel = new JPanel(new GridLayout(1, 1));
        chartPanel.add(new XChartPanel<>(chart1));
        return chartPanel;
    }
    /**
     * Save the chart panel as a PNG file.
     * @param chartPanel The chart panel to save
     * @param filePath The path to save the PNG file
     * @throws IOException If an error occurs while saving the file
     */
    public static void saveChartPanelAsPNG(JPanel chartPanel, String filePath) throws IOException {
        int width = (int) chartPanel.getPreferredSize().getWidth();
        int height = (int) chartPanel.getPreferredSize().getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        // Ensure the panel is fully rendered before capturing
        chartPanel.setSize(width, height);
        chartPanel.doLayout();
        chartPanel.print(g2);

        g2.dispose();

        ImageIO.write(image, "png", new File(filePath + ".png"));
    }
    public static ImagePlus showSavedPlot(String filePath) {
        ImagePlus imp = openImage(filePath);
        imp.show();
        return imp;
    }

    public static void createAndSaveHistogram(List<CSVRecord> dataRows, String column, String filePath) throws IOException {
        double[] values = dataRows.stream().mapToDouble(row -> Double.parseDouble(row.get(column))).toArray();

        Histogram histogram = new Histogram(values, 50);

        // Create the histogram chart
        CategoryChart chart = new CategoryChartBuilder().width(1610).height(1000).title("Histogram of " + column)
                .xAxisTitle(column).yAxisTitle("Frequency").build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setXAxisLabelRotation(90);
        chart.addSeries(column, histogram.getx(), Arrays.stream(histogram.gety()).asDoubleStream().toArray());

        // Save the chart as a TIFF file
        BufferedImage image = new BufferedImage(chart.getWidth(), chart.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        chart.paint(g2, 1610, 1000);
        g2.dispose();
        ImageIO.write(image, "png", new File(filePath + ".png"));
    }

    static class Histogram {
        private final double[] xAxisData;
        private final int[] yAxisData;

        public Histogram(double[] values, int numBins) {
            double min = Arrays.stream(values).min().getAsDouble();
            double max = Arrays.stream(values).max().getAsDouble();
            double binWidth = max != min ? (max - min) / numBins : 1 / numBins;

            xAxisData = new double[numBins];
            yAxisData = new int[numBins];

            for (int i = 0; i < numBins; i++) {
                xAxisData[i] = min + i * binWidth;
            }

            for (double value : values) {
                int bin = (int) ((value - min) / binWidth);
                // Edge case: include maximum
                if (bin == numBins) {
                    bin--;
                }
                yAxisData[bin]++;
            }
        }

        public double[] getx() {
            return xAxisData;
        }

        public int[] gety() {
            return yAxisData;
        }
    }

    public static void createAndSaveHeatmap(List<CSVRecord> dataRows, String columnX, String columnY, String filePath) throws IOException {
        // Number of bins for the histogram
        int numBinsX = 50;
        int numBinsY = 50;
    
        // Extract the data for the specified columns
        List<Double> xData = dataRows.stream().map(row -> Double.parseDouble(row.get(columnX))).collect(Collectors.toList());
        List<Double> yData = dataRows.stream().map(row -> Double.parseDouble(row.get(columnY))).collect(Collectors.toList());
    
        // Find min and max values for the columns
        Double minX = xData.stream().min(Double::compareTo).orElse(0.0);
        Double maxX = xData.stream().max(Double::compareTo).orElse(1.0);
        Double minY = yData.stream().min(Double::compareTo).orElse(0.0);
        Double maxY = yData.stream().max(Double::compareTo).orElse(1.0);
    
        // Define bin width
        Double binWidthX = (maxX - minX) / numBinsX;
        Double binWidthY = (maxY - minY) / numBinsY;
    
        // Initialize the 2D array to count occurrences
        Integer[][] zDataSparse = new Integer[numBinsY][numBinsX];
        for (Integer[] row: zDataSparse) {
            Arrays.fill(row, 0);
        }
    
        // Count occurrences within bins
        for (int i = 0; i < xData.size(); i++) {
            int xBin = (int) ((xData.get(i) - minX) / binWidthX);
            int yBin = (int) ((yData.get(i) - minY) / binWidthY);
            
            // Edge case: include maximum
            if (xBin == numBinsX) xBin--;
            if (yBin == numBinsY) yBin--;
    
            zDataSparse[yBin][xBin]++;
        }
    
        // Create the bin edges
        List<Double> xBins = new ArrayList<>();
        List<Double> yBins = new ArrayList<>();
        for (int i = 0; i <= numBinsX; i++) {
            xBins.add(minX + i * binWidthX);
        }
        for (int i = 0; i <= numBinsY; i++) {
            yBins.add(minY + i * binWidthY);
        }

        List<Number[]> zData = new ArrayList<Number[]>();
        for (Integer j = 0; j < numBinsY; j++) {
            for (Integer i = 0; i < numBinsX; i++) {
                if (zDataSparse[j][i] != 0) {
                    zData.add(new Number[]{i, j, zDataSparse[j][i]});
                }
            } 
        }
    
        // Create and configure the heatmap chart
        HeatMapChart chart = new HeatMapChartBuilder().width(1610).height(1000).title("Heatmap of " + columnX + " vs " + columnY).xAxisTitle(columnX).yAxisTitle(columnY).build();
        chart.addSeries("heatmap", xBins, yBins, zData);
        chart.getStyler().setXAxisLabelRotation(90);
    
        // Save the heatmap as a PNG file
        BufferedImage image = new BufferedImage(chart.getWidth(), chart.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        chart.paint(g2, chart.getWidth(), chart.getHeight());
        g2.dispose();
        ImageIO.write(image, "png", new File(filePath + ".png"));
    }
}