package ch.epfl.bio410;

import java.awt.*;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import javax.swing.*;

import net.imagej.ImageJ;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;

// import tracking from local package
import ch.epfl.bio410.utils.utils;
import ch.epfl.bio410.utils.TrackingConfig;
import ch.epfl.bio410.segmentation.Segmentation;
import ch.epfl.bio410.segmentation.Colonies;
import ch.epfl.bio410.tracking.Tracking;
import ch.epfl.bio410.analysis_and_plots.Plots;
import ch.epfl.bio410.analysis_and_plots.Results;
import static ch.epfl.bio410.analysis_and_plots.Results.assignTracksToColonies;


@Plugin(type = Command.class, menuPath = "Plugins>BII>Replisome Analysis")
public class Replisome_Analysis implements Command {
		// Default path is 5 folders above the current folder, in DATA
		private boolean isConfigAvailable = false;
		private String path = Paths.get(System.getProperty("user.home"), "Desktop", "Code", "bioimage-informatics-BIO410-project", "DATA").toString();
		private String[] fileList = new String[]{};
		private final boolean runColonies = true;
		private final boolean runTracking = true;
		private final boolean runAnalysis = true;
		// Colony assignment parameters
		private final int colony_min_area = 50; // Colony assignment parameters, minimum colony area
		// Detection parameters
		private final double radius = 0.31; 	// Detection parameters, radius of the object in um
		private final double threshold = 80.0;  // Detection parameters, quality threshold
		private final boolean medianFilter = true; // Detection parameters, do median filter (GFP channel only, before detection in TrackMate)
		// Tracking parameters
		private final double maxLinkDistance = 1.0; // Tracking parameters, max linking distance between objects
		private final double maxGapDistance = 1.0; // Tracking parameters, max gap distance to close a track across frames
		private final int maxFrameGap = 4; // Tracking parameters, max frame gap allowed for tracking
		private final double durationFilter = 8.0; // Tracking parameters, duration filter (min duration of a track)
		// Config
		private TrackingConfig config;

		// Misc utils
		private ImagePlus colonyLabels;
		public Map<Integer, double[][]> colonyStats;
		public Map<Integer, Map<Integer, double[]>> trackStats;
	/**
	 * This method is called when the command is run.
	 */
	public void run() {
		GenericDialog dlg = new GenericDialog("Replisome Analysis");
		dlg.addDirectoryField("Path to the image", path);
		dlg.addChoice("Image", fileList, fileList.length > 0 ? fileList[0] : "");

		// Get the TextField for the directory field
		TextField directoryField = (TextField) dlg.getStringFields().lastElement();

		// Get the Choice for the image choice dropdown menu
		Choice imageChoice = (Choice) dlg.getChoices().get(0);

		// Add a TextListener to the directory field
		directoryField.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				// Update the path
				path = directoryField.getText();

				// Update the fileList with the new list of images from the updated directory
				File directory = new File(path);
				fileList = directory.list(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(".tif");
					}
				});

				if (fileList == null || fileList.length == 0) {
					fileList = new String[]{};
					IJ.log("No images found in folder " + path + ", please enter the path to the folder with images");
				}

				// Update the Choice for the image choice dropdown menu with the updated fileList
				imageChoice.removeAll();
				for (String file : fileList) {
					imageChoice.add(file);
				}
			}
		});
		// When created, check if there are images in the folder
		directoryField.dispatchEvent(new TextEvent(directoryField, TextEvent.TEXT_VALUE_CHANGED));

		///////////// DIALOG /////////////
		// Image choice

		dlg.addMessage("__________________________");
		// Choose what to run
		dlg.addCheckbox("Run colony detection on DIC channel", runColonies);
		dlg.addCheckbox("Run tracking on GFP channel", runTracking);
		dlg.addCheckbox("Run analysis", runAnalysis);
		dlg.addMessage("Note : If you want to run analysis, please make sure that both colony detection and tracking have been run.");
		dlg.addMessage("If the previous steps have been run, make sure to select the folder containing the results.");
		dlg.addMessage("__________________________");
		// Config
		dlg.addMessage("Use existing config, or set new parameters :");
		try {
			List<String> configList = TrackingConfig.listAvailableConfigs();
			if (configList == null || configList.isEmpty()) {
				IJ.log("No config files found in folder, please set the parameters manually");
				isConfigAvailable = false;
			} else {
				isConfigAvailable = true;
			}
			dlg.addChoice("Config", configList.toArray(new String[0]), configList.size() > 0 ? configList.get(0) : "");
		} catch (NullPointerException e) {
			// try to load by copying the file to Downloads (backup if loading from resources fails)
			// NOTE : to avoid additional issues, this does not delete the copied files once it's done
			// However they are extremely lightweight and should not be an issue
			List<String> configList = Arrays.asList("configs/Merged1_config.properties", "configs/Merged2_config.properties", "configs/Merged3_config.properties");
			List<String> copiedFiles = new ArrayList<>();
			String copiedFile;
			for (String config : configList) {
				try {
					copiedFile = TrackingConfig.copyFromResources(config);
				} catch (NullPointerException e2) {
					copiedFile = null;
				}
				if (copiedFile != null) {
					copiedFiles.add(copiedFile);
				}
			}
			// Add the paths of the copied files to the list
			if (copiedFiles.size() > 0) {
				configList = copiedFiles;
				isConfigAvailable = true;
				IJ.log("Using backup configs : copying to Downloads folder");
				dlg.addChoice("Config", configList.toArray(new String[0]), configList.size() > 0 ? configList.get(0) : "");
			} else {
				IJ.log("No config files found in folder, please set the parameters manually");
				isConfigAvailable = false;
				// Add None as a choice
				dlg.addChoice("Config", new String[]{"None"}, "None");
			}
		}
		dlg.addCheckbox("Use existing config", isConfigAvailable);
		//////// PARAMETERS (if not using existing config) ///////////
//		dlg.addMessage("__________________________");
		dlg.addMessage("OR set new parameters :");
		// Colony assignment parameters
		dlg.addMessage("Colony assignment parameters");
		dlg.addNumericField("Minimum colony area", colony_min_area, 0);
		// detection parameters
		dlg.addMessage("Detection parameters");
		dlg.addNumericField("Radius (um)", radius, 2);
		dlg.addNumericField("Quality threshold", threshold, 0);
		dlg.addCheckbox("Median filter", medianFilter);
		// tracking parameters
		dlg.addMessage("Tracking parameters");
		dlg.addNumericField("Max linking distance", maxLinkDistance, 2);
		dlg.addNumericField("Max gap distance", maxGapDistance, 2);
		dlg.addNumericField("Max frame gap", maxFrameGap, 0);
		dlg.addNumericField("Duration filter", durationFilter, 2);
		dlg.addMessage("__________________________");
		// Display options
		dlg.addMessage("Display options :\n(WARNING : May cause memory issues for large images)");
		dlg.addCheckbox("Show colony regions (Voronoi diagram for each frame)", false);
		dlg.showDialog();
		if (dlg.wasCanceled()) return;

		// Get all the parameters
		//// PATH
		String path = dlg.getNextString();
		String image = dlg.getNextChoice();
		//// CHOICES OF COMPUTATION
		boolean computeColonies = dlg.getNextBoolean();
		boolean computeTracking = dlg.getNextBoolean();
		boolean computeAnalysis = dlg.getNextBoolean();
		//// CONFIG (Existing)
		String configName = dlg.getNextChoice();
		boolean useExistingConfig = dlg.getNextBoolean();
		// Colony detection parameters
		int colony_min_area = (int) dlg.getNextNumber();
		// Detection parameters
		double radius = dlg.getNextNumber();
		double threshold = dlg.getNextNumber();
		boolean medianFilter = dlg.getNextBoolean();
		// Tracking parameters
		double maxLinkDistance = dlg.getNextNumber();
		double maxGapDistance = dlg.getNextNumber();
		int maxFrameGap = (int) dlg.getNextNumber();
		double durationFilter = dlg.getNextNumber();
		//// DISPLAY
		boolean showColonyVoronoi = dlg.getNextBoolean();

		// Set the config if needed (use existing if set or no config available)
		if (!useExistingConfig || !isConfigAvailable) {
			this.config = new TrackingConfig(
					colony_min_area,
					radius,
					threshold,
					medianFilter,
					maxLinkDistance,
					maxGapDistance,
					maxFrameGap,
					durationFilter
			);
		} else {
			// if the configName contains :, it is a path and should be loaded as such
			// this only occurs if the config had to be copied from the resources to Downloads
			if (configName.contains(":")) {
				File configFile = new File(configName);
				// we use the overloaded constructor that takes a File
				this.config = TrackingConfig.createFromPropertiesFile(configFile);
			} else {
				// otherwise, we load it from the resources as String
				this.config = TrackingConfig.createFromPropertiesFile(configName);
			}
		}

		// show the image
		String imagePath = Paths.get(path, image).toString();
		// Results
		// Save the results to CSV
		String imageNameWithoutExtension = image.substring(0, image.lastIndexOf('.'));
		// create "results" folder if it doesn't exist
		File resultsFolder = Paths.get(path, "results").toFile();



		ImagePlus imp = IJ.openImage(imagePath);
		imp.show();

		// split the channels in DIC and GFP. DIC is used for segmentation, GFP for tracking)
		IJ.run(imp, "Split Channels", "");
		ImagePlus imageDIC = WindowManager.getImage("C1-" + imp.getTitle());
		ImagePlus imageGFP = WindowManager.getImage("C2-" + imp.getTitle());
		// show the results
		imageDIC.show();
		imageGFP.show();
		// Tile
		IJ.run("Tile");

		if (computeColonies) {
			IJ.log("------------------ COLONIES ------------------");
			// Print the configuration
			this.config.printColonyConfig();
			// Removing noise
			IJ.log("Removing noise in DIC channel");
			ImagePlus denoised = utils.remove_noise(imageDIC);
			denoised.show();

			// Segmentation
			IJ.log("Segmentation of DIC channel");
			Segmentation.segment(denoised);
			denoised.show();

			// Assign colonies
			Colonies colonies = new Colonies(imageDIC);
			colonies.runColoniesComputation(this.config.colony_min_area, showColonyVoronoi);
			this.colonyLabels = colonies.colonyLabels;
			this.colonyLabels.show();
			this.colonyStats = colonies.colonyStats;

			if (showColonyVoronoi) {
				colonies.voronoiDiagrams.show();
			}
			IJ.run("Tile");
			try {
				// If results folder does not exist, create it
				if (!resultsFolder.exists()) {
					if (resultsFolder.mkdir()) {
						IJ.log("Directory is created!");
					} else {
						IJ.log("Failed to create directory!");
						throw new RuntimeException("Failed to create results directory. Aborting.");
					}
				}
				colonies.saveResults(Paths.get(path, "results").toString(), imageNameWithoutExtension);
			} catch (Exception e) {
				IJ.log("ERROR : Failed to save colonies results.");
				throw new RuntimeException(e);
			}
		}

		if (computeTracking) {

			Tracking tracker = new Tracking();
			tracker.setConfig(config);
			// Note : model and config are exposed for later if needed
			Model model = tracker.runTracking(imageGFP);
			FeatureModel featureModel = model.getFeatureModel();
			// see https://imagej.net/plugins/trackmate/scripting/scripting#display-spot-edge-and-track-numerical-features-after-tracking for ways to get the features

			if (!resultsFolder.exists()) {
				if (resultsFolder.mkdir()) {
					IJ.log("Directory is created!");
				} else {
					IJ.log("Failed to create directory!");
					throw new RuntimeException("Failed to create results directory. Aborting.");
				}
			}
			String spotsCSVName = "/results/spots_" + imageNameWithoutExtension + ".csv";
			String tracksCSVName = "/results/tracks_" + imageNameWithoutExtension + ".csv";
			File csvSpotsPath = Paths.get(path, spotsCSVName).toFile();
			File csvTracksPath = Paths.get(path, tracksCSVName).toFile();
			try {
				tracker.saveFeaturesToCSV(model, csvSpotsPath, csvTracksPath);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		if(computeAnalysis){
			IJ.log("------------------ ANALYSIS ------------------");
			String colonyFileName = imageNameWithoutExtension + "_colony_labels.tif";
			String tracksFileName = "tracks_" + imageNameWithoutExtension + ".csv";
			if((computeColonies || utils.FileExists(path, colonyFileName)) &&
					(computeTracking || utils.FileExists(path, tracksFileName)))
			{

				List<CSVRecord> tracks = null;
				// Load the tracks
				try {
					tracks = utils.readCsv(Paths.get(path,  "results", "tracks_" + imageNameWithoutExtension + ".csv").toString(), 3);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				// Check if an ImaagePlus called colonylabels is open
				if (WindowManager.getImage(imageNameWithoutExtension+"_colony_labels.tif") != null) {
					IJ.log("Assigning tracks to colony labels");
					// Assign tracks to colonies and save the results
					assignTracksToColonies(tracks, WindowManager.getImage(imageNameWithoutExtension+"_colony_labels.tif"), imageNameWithoutExtension, path);
				}
				// Or open a new one
				else{
					this.colonyLabels = IJ.openImage(Paths.get(path, "results", imageNameWithoutExtension + "_colony_labels.tif").toString());
					IJ.run("Tile");
					this.colonyLabels.hide();
					utils.add_pixel_size(this.colonyLabels, imageDIC);
					IJ.log("Assigning tracks to colony labels");
					// Assign tracks to colonies and save the results
					assignTracksToColonies(tracks, this.colonyLabels, imageNameWithoutExtension, path); //not sure if this works
					this.colonyLabels.show();
				}

				// Analysis : plot area per track //

				// if colonies is not null, access the stats from there, otherwise recompute them
				try {
					IJ.log("Fetching stats for tracks and colonies");
					if (this.colonyStats == null) {
						IJ.log("Computing stats for colonies");
						this.colonyLabels.hide();
						this.colonyStats = Colonies.computeStats(this.colonyLabels, imageDIC);
						this.colonyLabels.show();
					}
					List<CSVRecord> tracks_with_labels = null;
					Results results = new Results();
					try {
					// Load the tracks features with colony labels
						tracks_with_labels = utils.readCsv(Paths.get(path, "results", "tracks_with_colonylabels_" + imageNameWithoutExtension + ".csv").toString(), 0);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					// Hide colonyLabels while processing
					IJ.log("Starting stats processing...");
					this.colonyLabels.hide();
					// Loop over tracks, and assign colony stats to each of them
					// This is a mapping of mapping of double[][]
					// First ID is the track ID, second ID is the frame, and the double[] is the stats
					this.trackStats = new HashMap<>();
					// Group by track ID
					Map<Integer, List<CSVRecord>> groupedData = Plots.groupByTrackId(tracks_with_labels);
					for (Map.Entry<Integer, List<CSVRecord>> entry : groupedData.entrySet()) {
						IJ.log("Processing track " + entry.getKey());
						Integer trackId = entry.getKey();
						String trackIdString = Integer.toString(trackId);
						List<CSVRecord> rows = entry.getValue();
						// Get the colony label for the track
						Map<Integer, double[]> statsforTrack = results.getColonyFeatures(trackIdString, rows, this.colonyStats);
						this.trackStats.put(trackId, statsforTrack);
					}
					IJ.log("Finished processing stats");
					// Show the colonyLabels again
					this.colonyLabels.show();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// Analysis : plot area per track //
				JPanel areaPerTrackPlot = Plots.plotAreaPerTrack(this.trackStats);
				try {
					String areaTracksPlotPath = Paths.get(path, "results", "area_per_track_" + imageNameWithoutExtension).toString();
					Plots.saveChartPanelAsPNG(areaPerTrackPlot, areaTracksPlotPath);
					Plots.showSavedPlot(areaTracksPlotPath);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				// Additional analysis //
				// Goal 1 : show position of tracks : mean displacement, directionality
				// Goal 2 : show mobility : Speed, duration
				// Goal 3 : compare mobility across images (and also colonies ?)

				IJ.log("All done!");

			} else{
				IJ.log("ERROR : Cannot run analysis without both colonies and tracking results.");
			}
		}
    }


	/**
	 * This main function serves for development purposes.
	 * It allows you to run the plugin immediately out of
	 * your integrated development environment (IDE).
	 *
	 * @param args whatever, it's ignored
	 * @throws Exception
	 */
	public static void main(final String... args) throws Exception {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
	}
}