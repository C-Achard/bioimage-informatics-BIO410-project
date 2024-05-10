package ch.epfl.bio410.tracking;

import ij.IJ;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TrackingConfig {
    public double detector_radius;
    public double detector_threshold;
    public boolean detector_median_filter;
    public double tracker_linking_max_distance;
    public double tracker_gap_closing_max_distance;
    public int tracker_max_frame_gap;
    public double track_duration_min;

    public String configPath = null;
    public String configName = null;
    public TrackingConfig(
            double detector_radius,
            double detector_threshold,
            boolean detector_median_filter,
            double tracker_linking_max_distance,
            double tracker_gap_closing_max_distance,
            int tracker_max_frame_gap,
            double track_duration_min
    ) {
        this.detector_radius = detector_radius;
        this.detector_threshold = detector_threshold;
        this.detector_median_filter = detector_median_filter;
        this.tracker_linking_max_distance = tracker_linking_max_distance;
        this.tracker_gap_closing_max_distance = tracker_gap_closing_max_distance;
        this.tracker_max_frame_gap = tracker_max_frame_gap;
        this.track_duration_min = track_duration_min;
    }
    /**
     * Create a TrackingConfig object from a properties file.
     * @param filename Name of the file to load from resources.
     * @return TrackingConfig object with the loaded parameters.
     */
    public static TrackingConfig createFromPropertiesFile(String filename) {
        String config_path = accessConfigPathFromResources(filename);
        TrackingConfig config = new TrackingConfig(0, 0, false, 0, 0, 0, 0);
        if (config_path != null) {
            config.loadFromPropertiesFile(config_path);
        }
        config.configName = filename;
        config.configPath = config_path;
        return config;
    }

    // Prints the current configuration
    public void printConfig() {
        IJ.log("----- Tracking config :");
        if (this.configPath != null) {
            IJ.log("Config loaded from " + this.configName);
        }
        IJ.log("- Detector radius : " + this.detector_radius + "um");
        IJ.log("- Detector quality threshold : " + this.detector_threshold + "um");
        IJ.log("- Detector using median filter : " + this.detector_median_filter);
        IJ.log("- Tracker max distance for linking : " + this.tracker_linking_max_distance + "um");
        IJ.log("- Tracker gap closing max distance : " + this.tracker_gap_closing_max_distance + "um");
        IJ.log("- Tracker max frame gap for closing : " + this.tracker_max_frame_gap);
        IJ.log("- Track minimum duration filter : " + this.track_duration_min + "frames");
        IJ.log("----- End of tracking config");
    }

    /**
     * Access a file from the resources folder.
     * @param filename Name of the config file to load from resources.
     * @return Path to the file.
     */
    private static String accessConfigPathFromResources(String filename) {
        try {
            String path = "configs/" + filename; // system file separator is not applicable here
            URL resourceUrl = TrackingConfig.class.getClassLoader().getResource(path);
            if (resourceUrl != null) {
                String real_path = resourceUrl.getPath();
                // if the first character of real path is "/", remove it
                if (real_path.charAt(0) == '/') {
                    real_path = real_path.substring(1);
                }
                return real_path;
            } else {
                System.out.println("Resource not found: " + path);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public List<String> listAvailableConfigs() {
        try {
            String path = accessConfigPathFromResources("configs");
            File folder = new File(path);
            File[] listOfFiles = folder.listFiles();
            List<String> files = new ArrayList<>();
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    files.add(file.getName());
                }
            }
            return files;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private void loadFromPropertiesFile(String filePath) {
        try {
            Properties properties = new Properties();
            properties.load(Files.newInputStream(Paths.get(filePath)));

            this.detector_radius = Double.parseDouble(properties.getProperty("DETECTOR_RADIUS"));
            this.detector_threshold = Double.parseDouble(properties.getProperty("DETECTOR_THRESHOLD"));
            this.detector_median_filter = Boolean.parseBoolean(properties.getProperty("DETECTOR_MEDIAN_FILTER"));
            this.tracker_linking_max_distance = Double.parseDouble(properties.getProperty("TRACKER_LINKING_MAX_DISTANCE"));
            this.tracker_gap_closing_max_distance = Double.parseDouble(properties.getProperty("TRACKER_GAP_CLOSING_MAX_DISTANCE"));
            this.tracker_max_frame_gap = Integer.parseInt(properties.getProperty("TRACKER_MAX_FRAME_GAP"));
            this.track_duration_min = Double.parseDouble(properties.getProperty("TRACK_DURATION_MIN"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
