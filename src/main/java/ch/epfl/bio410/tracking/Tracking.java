package ch.epfl.bio410.tracking;

import fiji.plugin.trackmate.*;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.features.track.TrackSpeedStatisticsAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.tracking.jaqaman.SparseLAPTrackerFactory;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImagePlus;

public class Tracking {

    public Model createTracker(ImagePlus imp) {
        IJ.log("------------------ TRACKMATE ------------------");
        // Instantiate model object
        Model model = new Model();
        model.setLogger(Logger.IJ_LOGGER);
        // Prepare settings object
        Settings settings = new Settings(imp);


        // Configure detector
        settings.detectorFactory = new LogDetectorFactory();
        settings.detectorSettings.put(DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION, true);
        settings.detectorSettings.put(DetectorKeys.KEY_RADIUS, 0.31d);
        settings.detectorSettings.put(DetectorKeys.KEY_TARGET_CHANNEL, 1);
        settings.detectorSettings.put(DetectorKeys.KEY_THRESHOLD, 80d);
        settings.detectorSettings.put(DetectorKeys.KEY_DO_MEDIAN_FILTERING, true);

        // Filter results of detection
        FeatureFilter detect_filter_quality = new FeatureFilter("QUALITY", 30, true);
        settings.addSpotFilter(detect_filter_quality);

        // Configure tracker
        settings.trackerFactory = new SparseLAPTrackerFactory();
        settings.trackerSettings = settings.trackerFactory.getDefaultSettings();
        settings.trackerSettings.put("LINKING_MAX_DISTANCE", 1.0d);
        settings.trackerSettings.put("GAP_CLOSING_MAX_DISTANCE", 1.0d);
        settings.trackerSettings.put("MAX_FRAME_GAP", 4);
        // Prevent track splitting and merging
        settings.trackerSettings.put("ALLOW_TRACK_SPLITTING", false);
        settings.trackerSettings.put("ALLOW_TRACK_MERGING", false);

        // Add the analyzers for all features
        settings.addAllAnalyzers();


        // Configure track filter
        double minDuration = 8.0d;
        FeatureFilter track_duration_filter = new FeatureFilter("TRACK_DURATION", minDuration, true);
        settings.addTrackFilter(track_duration_filter);


        // Add an analyzer for some track features, such as the track mean speed.
        settings.addTrackAnalyzer(new TrackSpeedStatisticsAnalyzer());
        settings.initialSpotFilterValue = 1.0;

        // Instantiate trackmate
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
        displaySettings.setTrackColorBy(DisplaySettings.TrackMateObject.TRACKS, TrackIndexAnalyzer.TRACK_INDEX);
        displaySettings.setSpotColorBy(DisplaySettings.TrackMateObject.TRACKS, TrackIndexAnalyzer.TRACK_INDEX);

        HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, imp, displaySettings);
        displayer.render();
        displayer.refresh();

        // Echo results with the logger we set at start:
        model.getLogger().log(model.toString());
        IJ.log("------------------ TRACKMATE RESULTS ------------------\n");
        return model;
    }
}
