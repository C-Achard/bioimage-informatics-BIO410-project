package ch.epfl.bio410.tracking;

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
     * Access a file from the resources folder.
     * @param path Path to the file, relative to resources.
     * @return Path to the file.
     */
    private String accessConfigFromResources(String path) {
        try {
            URL resourceUrl = TrackingConfig.class.getClassLoader().getResource(path);
            if (resourceUrl != null) {
                String real_path = resourceUrl.getPath();
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
            String path = accessConfigFromResources("configs");
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
    public void loadFromPropertiesFile(String filePath) {
        try {
            Properties properties = new Properties();
            properties.load(Files.newInputStream(Paths.get(filePath)));

            this.detector_radius = Double.parseDouble(properties.getProperty("detector_radius"));
            this.detector_threshold = Double.parseDouble(properties.getProperty("detector_threshold"));
            this.detector_median_filter = Boolean.parseBoolean(properties.getProperty("detector_median_filter"));
            this.tracker_linking_max_distance = Double.parseDouble(properties.getProperty("tracker_linking_max_distance"));
            this.tracker_gap_closing_max_distance = Double.parseDouble(properties.getProperty("tracker_gap_closing_max_distance"));
            this.tracker_max_frame_gap = Integer.parseInt(properties.getProperty("tracker_max_frame_gap"));
            this.track_duration_min = Double.parseDouble(properties.getProperty("track_duration_min"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
