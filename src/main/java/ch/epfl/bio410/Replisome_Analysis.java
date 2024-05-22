package ch.epfl.bio410;

import java.awt.*;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;

import ch.epfl.bio410.segmentation.Colonies;
import ch.epfl.bio410.utils.TrackingConfig;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;

import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import java.io.File;
import java.util.List;

// import tracking from local package
import ch.epfl.bio410.utils.utils;
import ch.epfl.bio410.segmentation.segmentation;
import ch.epfl.bio410.tracking.Tracking;


@Plugin(type = Command.class, menuPath = "Plugins>BII>Replisome Analysis")
public class Replisome_Analysis implements Command {
		// Default path is 5 folders above the current folder, in DATA
		private String path = Paths.get(System.getProperty("user.home"), "Desktop", "Code", "bioimage-informatics-BIO410-project", "DATA").toString();
		private String[] fileList = new String[]{};
		// private String path = utils.getFolderPathInResources("DATA"); does not work this way, as it means including several Gbs of data in the jar. We will have to load from our specific paths each time.
		private final boolean runColonies = true;
		private final boolean runTracking = true;
		private final int colony_min_area = 50; // Colony assignment parameters, minimum colony area
		private final double radius = 0.31; 	// Detection parameters, radius of the object in um
		private final double threshold = 80.0;  // Detection parameters, quality threshold
		private final boolean medianFilter = true; // Detection parameters, do median filter (GFP channel only, before detection in TrackMate)
    	private final double sigma = 10;  // Detection parameters, sigma of the DoG

		private final double maxLinkDistance = 1.0; // Tracking parameters, max linking distance between objects
		private final double maxGapDistance = 1.0; // Tracking parameters, max gap distance to close a track across frames
		private final int maxFrameGap = 4; // Tracking parameters, max frame gap allowed for tracking
		private final double durationFilter = 8.0; // Tracking parameters, duration filter (min duration of a track)

		private TrackingConfig config;
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
		dlg.addMessage("__________________________");
		// Config
		dlg.addMessage("Use existing config, or set new parameters :");
		dlg.addCheckbox("Use existing config", true);
		List<String> configList = TrackingConfig.listAvailableConfigs();
		if (configList.size() == 0) {
			IJ.log("No config files found in folder " + path + ", please set the parameters");
		}
		dlg.addChoice("Config", configList.toArray(new String[0]), configList.size() > 0 ? configList.get(0) : "");
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
		//// CONFIG (Existing)
		boolean useExistingConfig = dlg.getNextBoolean();
		String configName = dlg.getNextChoice();
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

		// Set the config if needed
		if (!useExistingConfig) {
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
			this.config = TrackingConfig.createFromPropertiesFile(configName);
		}

		// show the image
		String imagePath = Paths.get(path, image).toString();
		// Results
		// Save the results to CSV
		String imageNameWithoutExtension = image.substring(0, image.lastIndexOf('.'));
		// create "results" folder if it doesn't exist
		File resultsFolder = Paths.get(path, "results").toFile();
    	//pour mathilde
    	//String imagePath = "C:/Users/mathi/OneDrive/Documents/EPFL/MA4/BioimageAnalysis/Project/DATA/Merged-1.tif"
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
			ImagePlus denoised = utils.remove_noise(imageDIC, sigma);
			denoised.show();

			// Segmentation
			IJ.log("Segmentation of DIC channel");
			segmentation.segment(denoised);
			denoised.show();

			// Assign colonies
			Colonies colonies = new Colonies(imageDIC);
			colonies.runColoniesComputation(this.config.colony_min_area, showColonyVoronoi);
			colonies.colonyLabels.show();
			if (showColonyVoronoi) {
				colonies.voronoiDiagrams.show();
			}
			IJ.run("Tile");
			try {
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