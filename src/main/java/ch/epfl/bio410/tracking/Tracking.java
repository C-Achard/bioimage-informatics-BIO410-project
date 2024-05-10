package ch.epfl.bio410.tracking;

import fiji.plugin.trackmate.*;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.tracking.jaqaman.SparseLAPTrackerFactory;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImagePlus;



public class Tracking {
     // Default config
     public static final double DETECTOR_RADIUS = 0.31d;
    public static final double DETECTOR_THRESHOLD = 30.0d;
    public static final boolean DETECTOR_MEDIAN_FILTER = true;
    public static final double TRACKER_LINKING_MAX_DISTANCE = 1.0d;
    public static final double TRACKER_GAP_CLOSING_MAX_DISTANCE = 1.0d;
    public static final int TRACKER_MAX_FRAME_GAP = 4;
    public static final double TRACK_DURATION_MIN = 8.0d;
    private TrackingConfig trackingConfig;

    public String trackingConfigName;
    public String trackingConfigPath;
    /** Returns the default configuration parameters. */
    public TrackingConfig useDefaultConfig() {
        this.trackingConfig = new TrackingConfig(
                DETECTOR_RADIUS,
                DETECTOR_THRESHOLD,
                DETECTOR_MEDIAN_FILTER,
                TRACKER_LINKING_MAX_DISTANCE,
                TRACKER_GAP_CLOSING_MAX_DISTANCE,
                TRACKER_MAX_FRAME_GAP,
                TRACK_DURATION_MIN
        );
        return this.trackingConfig;
    }

    /**
    * Load configuration parameters from a properties file
    * @param filename Name of the file to load from resources.
    * @return TrackingConfig object with the loaded parameters.
    */
    public TrackingConfig loadConfig(String filename) {
        this.trackingConfig = TrackingConfig.createFromPropertiesFile(filename);
        return this.trackingConfig;
    }

    public TrackingConfig setConfig(
            double detector_radius,
            double detector_threshold,
            boolean detector_median_filter,
            double tracker_linking_max_distance,
            double tracker_gap_closing_max_distance,
            int tracker_max_frame_gap,
            double track_duration_min
    ) {
        this.trackingConfig = new TrackingConfig(
                detector_radius,
                detector_threshold,
                detector_median_filter,
                tracker_linking_max_distance,
                tracker_gap_closing_max_distance,
                tracker_max_frame_gap,
                track_duration_min
        );
        return this.trackingConfig;
    }

    /**
     * Creates a TrackMate tracker from the specified configuration parameters, in order to track replisomes in the GFP channel.
     * @return TrackMate model object.
     */
    public Model runTracking(ImagePlus imp) {
        IJ.log("------------------ TRACKMATE ------------------");
        this.trackingConfig.printConfig(); // show parameters
        IJ.log("Tracking started");
        // Instantiate model object and logger
        Model model = new Model();
        model.setLogger(Logger.IJ_LOGGER);
        // Prepare settings object
        Settings settings = new Settings(imp);

        // if config is not set, use default config
        if (this.trackingConfig == null) {
            this.useDefaultConfig();
        }

        // Configure detector
        settings.detectorFactory = new LogDetectorFactory();
        settings.detectorSettings.put(DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION, true);
        settings.detectorSettings.put(DetectorKeys.KEY_RADIUS, this.trackingConfig.detector_radius);
        settings.detectorSettings.put(DetectorKeys.KEY_TARGET_CHANNEL, 1);
        settings.detectorSettings.put(DetectorKeys.KEY_THRESHOLD, this.trackingConfig.detector_threshold);
        settings.detectorSettings.put(DetectorKeys.KEY_DO_MEDIAN_FILTERING, this.trackingConfig.detector_median_filter);

        // Filter results of detection
        FeatureFilter detect_filter_quality = new FeatureFilter("QUALITY", 30, true);
        settings.addSpotFilter(detect_filter_quality);

        // Configure tracker
        settings.trackerFactory = new SparseLAPTrackerFactory();
        settings.trackerSettings = settings.trackerFactory.getDefaultSettings();
        settings.trackerSettings.put("LINKING_MAX_DISTANCE", this.trackingConfig.tracker_linking_max_distance);
        settings.trackerSettings.put("GAP_CLOSING_MAX_DISTANCE", this.trackingConfig.tracker_gap_closing_max_distance);
        settings.trackerSettings.put("MAX_FRAME_GAP", this.trackingConfig.tracker_max_frame_gap);
        // Prevent track splitting and merging
        settings.trackerSettings.put("ALLOW_TRACK_SPLITTING", false);
        settings.trackerSettings.put("ALLOW_TRACK_MERGING", false);

        // Add the analyzers for all features
        settings.addAllAnalyzers();


        // Configure track filter
        FeatureFilter track_duration_filter = new FeatureFilter(
                "TRACK_DURATION",
                this.trackingConfig.track_duration_min,
                true);
        settings.addTrackFilter(track_duration_filter);

        // Instantiate and run trackmate
        TrackMate trackmate = new TrackMate(model, settings);
        boolean ok = trackmate.checkInput();
        if (!ok) {
            System.out.println(trackmate.getErrorMessage());
            return null;
        }

        ok = trackmate.process();
        if (!ok) {
            System.out.println(trackmate.getErrorMessage());
            return null;
        }

        // Display the results on top of the image
        SelectionModel selectionModel = new SelectionModel(model);
        DisplaySettings displaySettings = DisplaySettingsIO.readUserDefault();
        displaySettings.setTrackColorBy(DisplaySettings.TrackMateObject.TRACKS, "TRACK_DURATION");
        displaySettings.setSpotColorBy(DisplaySettings.TrackMateObject.SPOTS, "SPOT_QUALITY");
//        PerTrackFeatureColorGenerator trackColor = PerTrackFeatureColorGenerator(model, "TRACK_DURATION");
        HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, imp, displaySettings);
        displayer.render();
        displayer.refresh();

        // Echo results with the logger we set at start:
        model.getLogger().log(model.toString());
        IJ.log("------------------ TRACKMATE FINISHED ------------------\n");
        return model;
    }
}
