package ch.epfl.bio410.tracking;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackSpeedStatisticsAnalyzer;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.tracking.sparselap.SparseLAPTrackerFactory;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.tracking.LAPUtils;
import ij.ImagePlus;

public class TrackMateRunner {

    public Model createTracker(ImagePlus imp) {
        // Instantiate model object
        Model model = new Model();

        // Prepare settings object
        Settings settings = new Settings();
        settings.setFrom(imp);

        // Configure detector
        settings.detectorFactory = new LogDetectorFactory();
        settings.detectorSettings.put(DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION, true);
        settings.detectorSettings.put(DetectorKeys.KEY_RADIUS, 2.5d);
        settings.detectorSettings.put(DetectorKeys.KEY_TARGET_CHANNEL, 1);
        settings.detectorSettings.put(DetectorKeys.KEY_THRESHOLD, 5.0d);
        settings.detectorSettings.put(DetectorKeys.KEY_DO_MEDIAN_FILTERING, false);

        // Configure tracker
        settings.trackerFactory = new SparseLAPTrackerFactory();
        settings.trackerSettings = LAPUtils.getDefaultLAPSettingsMap();
        settings.trackerSettings.put("LINKING_MAX_DISTANCE", 10.0d);
        settings.trackerSettings.put("GAP_CLOSING_MAX_DISTANCE", 10.0d);
        settings.trackerSettings.put("MAX_FRAME_GAP", 3);

        // Add the analyzers for some spot features
        settings.addSpotAnalyzerFactory(new SpotIntensityAnalyzerFactory());
        settings.addSpotAnalyzerFactory(new SpotContrastAndSNRAnalyzerFactory());

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
        HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, imp);
        displayer.render();
        displayer.refresh();

        return model;
    }
}
