// TODO : add analysis to the package
package ch.epfl.bio410.analysis_and_plots;


import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class Analysis {
    /**
     * Creates an iterable with all track IDs in the model
     * @param model The TrackMate model used for tracking
     * @return An iterable with all track IDs
     */
    public static Iterable<Integer> getTrackIDs(Model model) {
        return model.getTrackModel().trackIDs(true);
    }

    /**
     * Get the features of all the spots in a track. Results are stored in a map with the track ID as key.
     * @param model The TrackMate model used for tracking
     * @return A map with the track ID as key and a map of features as value
     */
    public static Map<Integer, Map<String, Double>> getSpotFeatures(Model model) {
        // this stores the results : trackID -> feature -> value
        Map<Integer, Map<String, Double>> results = new HashMap<>();

        for (Integer id : model.getTrackModel().trackIDs(true)) {
            Set<Spot> track = model.getTrackModel().trackSpots(id);
            Map<String, Double> features = new HashMap<>();
            for (Spot spot : track) {
                features.put("POSITION_X", spot.getFeature("POSITION_X"));
                features.put("POSITION_Y", spot.getFeature("POSITION_Y"));
                features.put("FRAME", spot.getFeature("FRAME"));
                features.put("QUALITY", spot.getFeature("QUALITY"));
                features.put("MEAN_INTENSITY_CH1", spot.getFeature("MEAN_INTENSITY_CH1"));
            }
            results.put(id, features);
        }

        return results;
    }

    /**
     * Get the first spot position of a track. Used to check which object the track starts with.
     * @param model The TrackMate model used for tracking
     * @param trackID The ID of the track to get the first spot position from
     * @return A double array with the x and y position of the first spot in the track
     */
    public static double[] getFirstSpotPosition(Model model, Integer trackID) {
        Set<Spot> track = model.getTrackModel().trackSpots(trackID);
        double[] position = new double[2];
        for (Spot spot : track) {
            position[0] = spot.getFeature("POSITION_X");
            position[1] = spot.getFeature("POSITION_Y");
            break;
        }
        return position;
    }
}
