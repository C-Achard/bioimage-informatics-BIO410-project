package ch.epfl.bio410.analysis_and_plots;

import ch.epfl.bio410.segmentation.Colonies;
import ij.ImagePlus;
import ij.gui.NewImage;

import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.knowm.xchart.style.lines.SeriesLines;
import org.apache.commons.csv.CSVRecord;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.*;
import static ij.IJ.openImage;

/**
 * Class for generation of plots, from CSV files or existing dataframes.
 */

public class Plots{
    /**
     * Empty. Not meant to be run on its own.
     */

    public static void main(String[] args) {}


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

    /**
     * Groups the data rows by a given column name
     * @param dataRows List of CSV records
     * @param colName Name of column to group by. Must contain integers as IDs
     * @return Map where key is the ID of the group, and value is list of records in that group
     */
    public static Map<Integer, List<CSVRecord>> groupBy(List<CSVRecord> dataRows, String colName) {
        return dataRows.stream().collect(Collectors.groupingBy(row -> Integer.parseInt(row.get(colName))));
    }

    /**
     * Groups the data rows by TRACK_ID
     * @param dataRows List of CSV records
     * @return Map where key is TRACK_ID and value is list of records with that TRACK_ID
     */
    public static Map<Integer, List<CSVRecord>> groupByTrackId(List<CSVRecord> dataRows) {
        return groupBy(dataRows, "TRACK_ID");
    }

    /**
     * Gets the data in a list of CSVRecords corresponding to a selected column name
     * @param rows List of CSV records
     * @param colName Name of column in header
     * @return The values of the column
     */
    public static List<Double> getColumn(List<CSVRecord> rows, String colName) {
        return rows.stream().map(row -> Double.parseDouble(row.get(colName))).collect(Collectors.toList());
    }

        /**
     * Plot the area of each track over time.
     * @param tracksStats Map of track ID to map of frame to statistics
     * @param feature The feature to plot (see the CSV header for available features)
     * @param limitOptional Optional limit on the number of tracks to plot
     * @return JPanel containing the chart
     */
    public static JPanel plotColonyFeaturePerTrack(Map<Integer, Map<Integer, double[]>> tracksStats, String feature, Optional<Integer> limitOptional){
        // VALID ENTRIES IN THE STATS //
        //    IDENTIFIER	BOUNDING_BOX_X	BOUNDING_BOX_Y	BOUNDING_BOX_Z	BOUNDING_BOX_END_X	BOUNDING_BOX_END_Y
        //    BOUNDING_BOX_END_Z	BOUNDING_BOX_WIDTH	BOUNDING_BOX_HEIGHT	BOUNDING_BOX_DEPTH	MINIMUM_INTENSITY
        //    MAXIMUM_INTENSITY	MEAN_INTENSITY	SUM_INTENSITY	STANDARD_DEVIATION_INTENSITY
        //    PIXEL_COUNT	SUM_INTENSITY_TIMES_X	SUM_INTENSITY_TIMES_Y	SUM_INTENSITY_TIMES_Z	MASS_CENTER_X
        //    MASS_CENTER_Y	MASS_CENTER_Z	SUM_X	SUM_Y	SUM_Z	CENTROID_X	CENTROID_Y	CENTROID_Z
        //    SUM_DISTANCE_TO_MASS_CENTER	MEAN_DISTANCE_TO_MASS_CENTER
        //    MAX_DISTANCE_TO_MASS_CENTER	MAX_MEAN_DISTANCE_TO_MASS_CENTER_RATIO	SUM_DISTANCE_TO_CENTROID
        //    MEAN_DISTANCE_TO_CENTROID	MAX_DISTANCE_TO_CENTROID	MAX_MEAN_DISTANCE_TO_CENTROID_RATIO
        //////////////////////
        // Create the XYChart
        int limit = limitOptional.orElse(tracksStats.size());
        XYChart chart = new XYChartBuilder().width(1600).height(800).title("Area of each track over time").xAxisTitle("Frame").yAxisTitle("Area").build();
        // For each track, extract the frame as x and the area as y
        int limiter = 0;
        for (Map.Entry<Integer, Map<Integer, double[]>> entry : tracksStats.entrySet()) {
            if (limit != 0 && limiter >= limit) break;
            limiter++;
            Integer trackId = entry.getKey();
            Map<Integer, double[]> stats = entry.getValue();
            List<Integer> xData = new ArrayList<>();
            List<Double> yData = new ArrayList<>();
            // sort stats by frame (key)
            SortedMap<Integer, double[]> sortedStats = new TreeMap<>(stats);
            for (Map.Entry<Integer, double[]> frameStats : sortedStats.entrySet()) {
                xData.add(frameStats.getKey());
                yData.add(frameStats.getValue()[Colonies.getColumnMapping().get(feature)]);
            }
            // Add the series to the chart
            try {
                XYSeries series = chart.addSeries("Track " + trackId, xData, yData);
                series.setMarker(SeriesMarkers.NONE);
                series.setLineStyle(SeriesLines.SOLID);
            } catch (IllegalArgumentException e) {
                System.out.println("Track " + trackId + " has no data for feature " + feature);
            }
        }
        // Put chart on a panel for easier manipulation
        return new XChartPanel<>(chart);
    }
    /**
     * Plot the area of each track over time.
     * @param tracksStats Map of track ID to map of frame to statistics
     * @return JPanel containing the chart
     */
    public static JPanel plotAreaPerTrack(Map<Integer, Map<Integer, double[]>> tracksStats){
        return plotColonyFeaturePerTrack(tracksStats, "PIXEL_COUNT", Optional.empty());
    }

    /**
     * Creates a chart panel with XY plots for the specified spot.
     * @param trackId The ID of the spot
     * @param rows List of CSV records for the spots
     * @return JPanel containing the chart
     */
    public static JPanel plotSpotTrack(Integer trackId, List<CSVRecord> rows) {
        // Dimensions
        int width = 1600;
        int height = 1600;

        // Combine the charts into a single panel
        JPanel chartPanel = new JPanel(new GridLayout(2, 1));
        // First chart (POSITION_X vs POSITION_Y)
        chartPanel.add(plotFeatures(trackId, rows, "POSITION_X", "POSITION_Y", width, height/2));
        // Second chart (POSITION_T vs MEDIAN_INTENSITY_CH1)
        chartPanel.add(plotFeatures(trackId, rows, "POSITION_T", Arrays.asList("MEAN_INTENSITY_CH1", "MEDIAN_INTENSITY_CH1", "MIN_INTENSITY_CH1", "MAX_INTENSITY_CH1"), width, height/2));

        return chartPanel;
    }

    /**
     * Plot the specified features for a single track.
     * @param trackId The ID of the track to plot
     * @param rows List of CSV records containing the data (sorted by frame)
     * @param xFeature The feature to plot on the x-axis
     * @param yFeature The feature to plot on the y-axis
     * @param width Plot width
     * @param height Plot height
     * @return JPanel containing the chart
     */
    public static JPanel plotFeatures(Integer trackId, List<CSVRecord> rows, String xFeature, String yFeature, int width, int height) {
        return plotFeatures(trackId, rows, xFeature, Arrays.asList(yFeature), width, height);
    }
    public static JPanel plotFeatures(Integer trackId, List<CSVRecord> rows, String xFeature, String yFeature){
        return plotFeatures(trackId, rows, xFeature, yFeature, 1600, 800);
    }

    /**
     * Plot a collection of features against a single x-feature for a single track.
     * @param trackId The ID of the track to plot
     * @param rows List of CSV records containing the data (sorted by frame)
     * @param xFeature The feature to plot on the x-axis
     * @param yFeatures The collection of features to plot on the y-axis
     * @param width Plot width
     * @param height Plot height
     * @return JPanel containing the chart
     */
    public static JPanel plotFeatures(Integer trackId, List<CSVRecord> rows, String xFeature, List<String> yFeatures, int width, int height) {
        // Sort by frame
        rows.sort(Comparator.comparingInt(row -> Integer.parseInt(row.get("FRAME"))));

        // Prepare x-axis
        List<Double> xData = getColumn(rows, xFeature);
        XYChart chart = new XYChartBuilder().width(width).height(height).title(
                "Track ID: " + trackId + " (" + xFeature + " as independent variable)"
                ).xAxisTitle(xFeature).yAxisTitle("others").build();

        // Add features one by one
        for (String yFeature : yFeatures) {
            // Get y-feature
            List<Double> yData = getColumn(rows, yFeature);

            // Add series to plot
            XYSeries series = chart.addSeries(yFeature, xData, yData);
            chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
            series.setMarker(SeriesMarkers.NONE);
            series.setLineStyle(SeriesLines.SOLID);
        }

        // Put chart on a panel for easier manipulation
        return new XChartPanel<>(chart);
    }
    public static JPanel plotFeatures(Integer trackId, List<CSVRecord> rows, String xFeature, List<String> yFeatures){
        return plotFeatures(trackId, rows, xFeature, yFeatures, 1600, 800);
    }

    /**
     * Compute particle speed from provided time and position data
     * @param tData List of times when measurements were performed
     * @param dimData Lists of coordinates. Each list should hold the data for its own dimension
     * @return List of speed data computed using finite differences
     */
    @SafeVarargs  // Not a good idea, but otherwise can't have an array of List<Double> in dimData
    public static List<Double> getSpeed(List<Double> tData, List<Double>... dimData) {
        List<Double> speeds = new ArrayList<Double>();

        // For each point in time, get the displacement
        for (int i = 1; i < tData.size(); i++) {
            Double rSquared = 0.0;
            for (List<Double> data : dimData) {
                rSquared += Math.pow(data.get(i) - data.get(i-1), 2.0);
            }
            // Divide displacement by timestep
            speeds.add(Math.sqrt(rSquared)/(tData.get(i) - tData.get(i-1)));
        }

        return speeds;
    }

    /**
     * Compute particle speed from provided dataframe and selected columns
     * @param rows Dataframe containing CSVRecord rows with the position of a point at a given time
     * @param tName Name of the time variable in the header of the CSVRecords
     * @param dimNames Names of the position variables
     * @return List of speed data computed from the given columns, using finite differences
     */
    @SuppressWarnings("unchecked")  // Not a good idea, but otherwise can't create List<Double>[] for generic getSpeed.
    public static List<Double> getSpeed(List<CSVRecord> rows, String tName, String... dimNames) {
        // Get the corresponding columns
        List<Double> tData = getColumn(rows, tName);
        List<Double>[] dimData = new ArrayList[dimNames.length];
        for (int i = 0; i < dimNames.length; i++) {
            dimData[i] = getColumn(rows, dimNames[i]);
        }

        // Feed the data into generic getSpeed
        return getSpeed(tData, dimData);
    }

    /**
     * Plot "instantaneous" speed over time for a given collection of tracks.
     * @param trackIds List of IDs of the tracks to plot
     * @param rows List of CSV records containing the data (sorted by frame)
     * @param width Plot width
     * @param height Plot height
     * @return JPanel containing the chart
     */
    public static JPanel plotSpeed(List<Integer> trackIds, List<CSVRecord> rows, int width, int height) {
        // Sort by frame
        rows.sort(Comparator.comparingInt(row -> Integer.parseInt(row.get("FRAME"))));
        // Separate rows per track
        Map<Integer, List<CSVRecord>> groupedRows = groupByTrackId(rows);

        // Prepare chart for adding series in
        XYChart chart = new XYChartBuilder().width(width).height(height).title(
                "POSITION_T vs SPOT_SPEED for selected tracks"
            ).xAxisTitle("POSITION_T").yAxisTitle("SPOT_SPEED").build();

        for (Integer trackId : trackIds) {
            List<CSVRecord> trackData = groupedRows.get(trackId);

            // Get data
            List<Double> tData = getColumn(trackData, "POSITION_T");
            List<Double> trackSpeed = getSpeed(trackData, "POSITION_T", "POSITION_X", "POSITION_Y", "POSITION_Z");
            tData.remove(0);  // to get array of the same size

            // Add series to the chart
            try {
                XYSeries series = chart.addSeries("Track " + trackId, tData, trackSpeed);
                series.setMarker(SeriesMarkers.NONE);
                series.setLineStyle(SeriesLines.SOLID);
            } catch (IllegalArgumentException e) {
                System.out.println("Unable to calculate speed for track " + trackId);
            }
        }

        return new XChartPanel<>(chart);
    }
    public static JPanel plotSpeed(List<Integer> trackIds, List<CSVRecord> rows) {
        return plotSpeed(trackIds, rows, 1600, 800);
    }
    public static JPanel plotSpeed(Integer trackId, List<CSVRecord> rows, int width, int height) {
        return plotSpeed(Arrays.asList(trackId), rows, width, height);
    }
    public static JPanel plotSpeed(Integer trackId, List<CSVRecord> rows) {
        return plotSpeed(trackId, rows, 1600, 800);
    }

    /**
     * Plot the specified feature for each track in the list of track IDs.
     * @param trackIds List of track IDs to plot
     * @param rows List of CSV records containing the data (sorted by track ID)
     * @param feature The feature to plot (see the CSV header for available features)
     * @return JPanel containing the chart
     */
    public static JPanel plotTracksFeatures(List<Integer> trackIds, List<CSVRecord> rows, String feature) {
        // Dimensions
        int width = 1600;
        int height = 1000;

        // Plot the feature for each track (track id is x, feature is y)
        Map<Integer, List<CSVRecord>> groupedData = groupByTrackId(rows);
        List<Double> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        for (Integer trackId : trackIds) {
            List<CSVRecord> trackData = groupedData.get(trackId);
            if (trackData != null) {
                xData.add((double) trackId);
                yData.add(Double.parseDouble(trackData.get(0).get(feature)));
            }
            System.out.println(trackData);
        }

        // Create the CategoryChart (discrete features) with the specified feature
        CategoryChart chart = new CategoryChartBuilder().width(width).height(height).title(
                "Track IDs vs " + feature
        ).xAxisTitle("Track ID").yAxisTitle(feature).build();
        chart.getStyler().setXAxisLabelRotation(90);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.getStyler().setDefaultSeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Stick);
        chart.getStyler().setXAxisTickMarkSpacingHint(10);

        CategorySeries series = chart.addSeries(feature, xData, yData);
        series.setMarker(SeriesMarkers.CIRCLE);

        // Put chart on a panel for easier manipulation
        return new XChartPanel<>(chart);
    }

    /**
     * Save the chart panel as a PNG file.
     * @param chartPanel The chart panel to save
     * @param filePath Path to the output file
     * @throws IOException If an error occurs while saving the file
     */
    public static void saveChartPanelAsPNG(JPanel chartPanel, String filePath) throws IOException {
        // getPreferredSize() takes into account the contents of chartPanel to compute a minimum size
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

    /**
     * Display the file in ImageJ.
     * Not used in this file, but useful method for integration with ImageJ.
     * @param filePath Path to the image file
     * @return ImagePlus object for further manipulation with ImageJ
     */
    public static ImagePlus showSavedPlot(String filePath, String fileExtension) {
        ImagePlus imp = openImage(filePath + "." + fileExtension);
        imp.show();
        return imp;
    }
    public static ImagePlus showSavedPlot(String filePath) {
        return showSavedPlot(filePath, "png");
    }

    /**
     * Creates a histogram for the specified column and saves it as a PNG file.
     * @param dataRows List of CSV records
     * @param columnName Name of the column
     * @param nBins Number of bins in the histogram
     * @param width Width of output figure
     * @param height Height of output figure
     * @param visible Whether or not to display other stuff than the data
     * @return JPanel containing the chart
     */
    public static JPanel plotHistogram(List<CSVRecord> dataRows, String columnName, int nBins, int width, int height, boolean visible) {
        // Extract data for the histogram
        List<Double> columnData = dataRows.stream().map(row -> Double.parseDouble(row.get(columnName))).collect(Collectors.toList());

        // Create the histogram chart
        Histogram histogram = new Histogram(columnData, nBins);
        CategoryChart chart = new CategoryChartBuilder().width(width).height(height).title("Histogram of " + columnName).xAxisTitle(columnName).yAxisTitle("Frequency").build();
        chart.addSeries(columnName, histogram.getxAxisData(), histogram.getyAxisData());
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setXAxisLabelRotation(90);
        if (!visible) {
            chart.getStyler().setChartTitleVisible(false);
            chart.getStyler().setAxisTitlesVisible(false);
            chart.getStyler().setAxisTicksVisible(false);
            chart.getStyler().setPlotGridLinesVisible(false);
            chart.getStyler().setLegendPadding(2);
            chart.getStyler().setChartPadding(2);
        }

        // Put chart on a panel for easier manipulation
        return new XChartPanel<>(chart);
    }
    public static JPanel plotHistogram(List<CSVRecord> dataRows, String columnName, int nBins, int width, int height) {
        return plotHistogram(dataRows, columnName, nBins, width, height, true);
    }
    public static JPanel plotHistogram(List<CSVRecord> dataRows, String columnName, int nBins, boolean visible) {
        return plotHistogram(dataRows, columnName, nBins, 1600, 1000, visible);
    }
    public static JPanel plotHistogram(List<CSVRecord> dataRows, String columnName, int nBins) {
        return plotHistogram(dataRows, columnName, nBins, 1600, 1000, true);
    }

    /**
     * Creates a heatmap for the specified columns and saves it as a PNG file.
     * @param dataRows List of CSV records
     * @param columnX Name of the X column
     * @param columnY Name of the Y column
     * @param nBinsX Number of bins in the x-direction
     * @param nBinsX Number of bins in the y-direction
     * @param width Width of output figure
     * @param height Height of output figure
     * @param visible Whether or not to display other stuff than the data
     * @return JPanel containing the chart
     */
    public static JPanel plotHeatmap(List<CSVRecord> dataRows, String columnX, String columnY, int nBinsX, int nBinsY, int width, int height, boolean visible) {
        // Extract data for the heatmap
        List<Double> xData = dataRows.stream().map(row -> Double.parseDouble(row.get(columnX))).collect(Collectors.toList());
        List<Double> yData = dataRows.stream().map(row -> Double.parseDouble(row.get(columnY))).collect(Collectors.toList());

        // Create a 2D histogram for the heatmap
        int[][] bins = new int[nBinsY][nBinsX];
        double xMin = Collections.min(xData);
        double xMax = Collections.max(xData);
        double yMin = Collections.min(yData);
        double yMax = Collections.max(yData);
        double xBinSize = (xMax - xMin) / nBinsX;
        double yBinSize = (yMax - yMin) / nBinsY;

        // Sort values into bins
        for (int i = 0; i < xData.size(); i++) {
            int xBin = (int) ((xData.get(i) - xMin) / xBinSize);
            int yBin = (int) ((yData.get(i) - yMin) / yBinSize);

            // Edge case: include maximum
            if (xBin == nBinsX) xBin--;
            if (yBin == nBinsY) yBin--;

            bins[yBin][xBin]++;
        }

        // Compute bin edges for display
        List<Double> xBins = new ArrayList<>();
        List<Double> yBins = new ArrayList<>();
        for (int i = 0; i < nBinsX; i++) xBins.add(xMin + i * xBinSize);
        for (int i = 0; i < nBinsY; i++) yBins.add(yMin + i * yBinSize);

        // Change bins (sparse matrix) into list of (coordinates + value) for display
        List<Number[]> zData = new ArrayList<Number[]>();
        for (Integer j = 0; j < nBinsY; j++) {
            for (Integer i = 0; i < nBinsX; i++) {
                if (bins[j][i] != 0) zData.add(new Number[]{i, j, bins[j][i]});
            } 
        }

        // Create and configure the heatmap chart
        HeatMapChart chart = new HeatMapChartBuilder().width(width).height(height).title("Heatmap of " + columnX + " vs " + columnY)
                .xAxisTitle(columnX).yAxisTitle(columnY).build();
        chart.addSeries("heatmap", xBins, yBins, zData);
        chart.getStyler().setXAxisLabelRotation(90);
        if (!visible) {
            chart.getStyler().setChartTitleVisible(false);
            chart.getStyler().setAxisTitlesVisible(false);
            chart.getStyler().setAxisTicksVisible(false);
            chart.getStyler().setPlotGridLinesVisible(false);
            chart.getStyler().setLegendVisible(false);
            chart.getStyler().setChartPadding(2);
        }

        // Put chart on a panel for easier manipulation
        return new XChartPanel<>(chart);
    }
    public static JPanel plotHeatmap(List<CSVRecord> dataRows, String columnX, String columnY, int nBinsX, int nBinsY, int width, int height) {
        return plotHeatmap(dataRows, columnX, columnY, nBinsX, nBinsY, width, height, true);
    }
    public static JPanel plotHeatmap(List<CSVRecord> dataRows, String columnX, String columnY, int nBinsX, int nBinsY, boolean visible) {
        return plotHeatmap(dataRows, columnX, columnY, nBinsX, nBinsY, 1600, 1000, visible);
    }
    public static JPanel plotHeatmap(List<CSVRecord> dataRows, String columnX, String columnY, int nBinsX, int nBinsY) {
        return plotHeatmap(dataRows, columnX, columnY, nBinsX, nBinsY, 1600, 1000, true);
    }

    /**
     * Creates a histogram/heatmap jointplot from selected columns of a dataframe.
     * @param dataRows List of CSV records
     * @param columns List of columns to be used in the jointplot
     * @param panelSize Size of each individual square panel
     * @param render Which side of the diagonal gets heatmaps
     * @return JPanel containing the chart
     */
    public static JPanel plotJointPanel(List<CSVRecord> dataRows, List<String> columns, int panelSize, int render) {
        // Prepare the panel on which we draw the jointplot
        JPanel chartPanel = new JPanel(new GridLayout(columns.size(), columns.size()));
        boolean show;

        for (int j = 0; j < columns.size(); j++) {
            for (int i = 0; i < columns.size(); i++) {
                // Option for heatmap layout
                if (render > 0) {  // Display heatmaps above diagonal
                    show = i > j;
                } else if (render < 0) {  // Display below diagonal
                    show = i < j;
                } else show = i != j;  // Both above and below

                // Tile the charts!
                if (i == j) {  // Histogram on the diagonal
                    chartPanel.add(plotHistogram(dataRows, columns.get(j), 20, panelSize, panelSize, false));
                } else if (show) {  // Heatmaps according to the previous conditions
                    chartPanel.add(plotHeatmap(dataRows, columns.get(i), columns.get(j), 20, 20, panelSize, panelSize, false));
                } else {  // Blank everywhere else
                    JPanel blankPanel = new JPanel();
                    blankPanel.setSize(panelSize, panelSize);
                    chartPanel.add(blankPanel);
                }
            }
        }

        return chartPanel;
    }
    public static JPanel plotJointPanel(List<CSVRecord> dataRows, List<String> columns) {
        return plotJointPanel(dataRows, columns, 250, -1);
    }

    /**
     * Plot a boxplot to compare a given feature between bacteria colonies
     * @param dataRows Dataframe containing track data
     * @param feature Feature to plot and compare
     * @param width Width of output plot
     * @param height Height of output plot
     * @return JPanel containing the chart
     */
    public static JPanel plotColonyStats(List<CSVRecord> dataRows, String feature, int width, int height) {
        SortedMap<Integer, List<CSVRecord>> colonyData = new TreeMap<Integer, List<CSVRecord>>(groupBy(dataRows, "COLONY_LABEL"));

        BoxChart chart = new BoxChartBuilder().width(width).height(height).build();

        for (Integer colonyID : colonyData.keySet()) {
            List<CSVRecord> data = colonyData.get(colonyID);
            chart.addSeries(String.valueOf(colonyID) + " (" + data.size() + " tracks)", getColumn(data, feature));
        }

        return new XChartPanel<>(chart);
    }
    public static JPanel plotColonyStats(List<CSVRecord> dataRows, String feature) {
        return plotColonyStats(dataRows, feature, 1600, 1000);
    }
}
