package ch.epfl.bio410;

import java.io.FilenameFilter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

import ch.epfl.bio410.tracking.TrackingConfig;
import fiji.plugin.trackmate.Model;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import java.io.File;
import java.util.List;

// import tracking from local package
import ch.epfl.bio410.tracking.Tracking;


@Plugin(type = Command.class, menuPath = "Plugins>BII>Replisome Analysis")
public class Replisome_Analysis implements Command {

		/////////// EXAMPLE, PLEASE ADAPT TO YOUR NEEDS ///////////

		// Default path is 5 folders above the current folder, in DATA
		private String path = Paths.get(System.getProperty("user.home"), "Desktop", "Code", "bioimage-informatics-BIO410-project", "DATA").toString();

		// Constants below are set for homework.tif, except distmax
		private final double radius = 0.31; 	// Detection parameters, radius of the object in um
		private final double threshold = 80.0;  // Detection parameters, quality threshold
		private final boolean median_filter = true; // Detection parameters, median filter

		private final double max_link_distance = 1.0; // Tracking parameters, max linking distance between objects
		private final double max_gap_distance = 1.0; // Tracking parameters, max gap distance to close a track across frames
		private final int max_frame_gap = 4; // Tracking parameters, max frame gap allowed for tracking
		private final double duration_filter = 8.0; // Tracking parameters, duration filter (min duration of a track)
	public void run() {
		GenericDialog dlg = new GenericDialog("Replisome Analysis");
		dlg.addDirectoryField("Path to the image", path);
		File directory = new File(path);
		String[] fileList = directory.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".tif");
			}
		});

		if (fileList == null || fileList.length == 0) {
			fileList = new String[]{};
			IJ.log("No images found in folder " + path + ", please enter the path to the folder with images");
		}
		dlg.addChoice("Image", fileList, fileList.length > 0 ? fileList[0] : "");
		dlg.addMessage("__________________________");
		dlg.addMessage("Use existing config, or set new parameters :");
		dlg.addCheckbox("Use existing config", true);
		// add config choices
		List<String> config_list = TrackingConfig.listAvailableConfigs();
		if (config_list.size() == 0) {
			IJ.log("No config files found in folder " + path + ", please set the parameters");
		}
		dlg.addChoice("Config", config_list.toArray(new String[0]), config_list.size() > 0 ? config_list.get(0) : "");
		//////// PARAMETERS (if not using existing config) ///////////
		dlg.addMessage("__________________________");
		dlg.addMessage("OR set new parameters :");
		// detection parameters
		dlg.addMessage("Detection parameters");
		dlg.addNumericField("Radius (um)", radius, 2);
		dlg.addNumericField("Quality threshold", threshold, 0);
		dlg.addCheckbox("Median filter", median_filter);
		// tracking parameters
		dlg.addMessage("Tracking parameters");
		dlg.addNumericField("Max linking distance", max_link_distance, 2);
		dlg.addNumericField("Max gap distance", max_gap_distance, 2);
		dlg.addNumericField("Max frame gap", max_frame_gap, 0);
		dlg.addNumericField("Duration filter", duration_filter, 2);
		dlg.showDialog();
		if (dlg.wasCanceled()) return;

		// Get all the parameters
		String path = dlg.getNextString();
		String image = dlg.getNextChoice();
		boolean use_existing_config = dlg.getNextBoolean();
		String config_name = dlg.getNextChoice();
		double radius = dlg.getNextNumber();
		double threshold = dlg.getNextNumber();
		boolean median_filter = dlg.getNextBoolean();
		double max_link_distance = dlg.getNextNumber();
		double max_gap_distance = dlg.getNextNumber();
		int max_frame_gap = (int) dlg.getNextNumber();
		double duration_filter = dlg.getNextNumber();

		// show the image
		String imagePath = Paths.get(path, image).toString();
		ImagePlus imp = IJ.openImage(imagePath);
		imp.show();

		// split the channels in DIC and GFP. DIC is used for segmentation, GFP for tracking)
		IJ.run(imp, "Split Channels", "");
		ImagePlus imageDIC = WindowManager.getImage("C1-" + imp.getTitle());
		ImagePlus imageGFP = WindowManager.getImage("C2-" + imp.getTitle());
		// show the results
		imageDIC.show();
		imageGFP.show();

		Tracking tracker = new Tracking();
		// Note : model and config are exposed for later if needed
		if (!use_existing_config) {
			TrackingConfig config = tracker.setConfig(
					radius,
					threshold,
					median_filter,
					max_link_distance,
					max_gap_distance,
					max_frame_gap,
					duration_filter
			);
		}
		// load the config if it exists
		if (use_existing_config && config_name != null) {
			TrackingConfig config = tracker.loadConfig(config_name);
		}
		Model model = tracker.runTracking(imageGFP);

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
