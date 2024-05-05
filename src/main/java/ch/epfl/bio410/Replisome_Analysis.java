package ch.epfl.bio410;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

import ch.epfl.bio410.tracking.cost.DistanceAndIntensityCost;
import ch.epfl.bio410.tracking.graph.PartitionedGraph;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import java.io.File;

import ch.epfl.bio410.tracking.Tracking;


@Plugin(type = Command.class, menuPath = "Plugins>BII>Project Template")
public class Replisome_Analysis implements Command {

		// Default path is 5 folders above the current folder, in DATA
		private final static String path = "../../../../../DATA";

		// Constants below are set for homework.tif, except distmax
		private final double sigma = 5.0;  // Detection parameters, sigma of the DoG, 6 for easy.tif
		private final double threshold = 160.0;  // Detection parameters, threshold of the localmax
		private final double costmax = 0.045;	// Cost parameters DistanceAndIntensityCost
		private final double lambda = 0.95; 	// Cost parameters DistanceAndIntensityCost
	public void run() {
		// resolve path

		GenericDialog dlg = new GenericDialog("Replisome Analysis");
		try {
			Path realPath = Paths.get(path).toRealPath();
			dlg.addDirectoryField("Image folder", realPath.toString());;
		} catch (IOException e) {
			IJ.log("Path " + path + " does not exist");
			return;
		}
		try {
			dlg.addChoice("Image", new File(path).list(), new File(path).list()[0]);
		} catch (Exception e) {
			IJ.log("No images found in folder " + path + ", please enter the path to the image");
			return;
		}
		dlg.addMessage("Detection parameters");
		dlg.addNumericField("Sigma", sigma, 1);
		dlg.addNumericField("Threshold", threshold, 0);
		// cost function params
		dlg.addMessage("Distance and Intensity parameters");
		dlg.addNumericField("Cost max", costmax, 5);
		dlg.addNumericField("Lambda", lambda, 3);
		dlg.showDialog();
		if (dlg.wasCanceled()) return;

		double trackingSigma = dlg.getNextNumber();
		double trackingThreshold = dlg.getNextNumber();
		double trackingCostMax = dlg.getNextNumber();
		double trackingLambda = dlg.getNextNumber();

		// show the image
		String imagePath = dlg.getNextString() + FileSystems.getDefault().getSeparator() + dlg.getNextChoice();
		ImagePlus imp = new ImagePlus(new File(imagePath).getAbsolutePath());
		imp.show();

		// Run tracking on the second channel of the image
		Tracking tracker = new Tracking();
		// Detect spots
		PartitionedGraph frames = tracker.detect(imp, trackingSigma, trackingThreshold);
		frames.drawSpots(imp);
		DistanceAndIntensityCost costFunc = new DistanceAndIntensityCost(imp, trackingCostMax, trackingLambda);
		// compute trajectories
		PartitionedGraph trajs = null;
		trajs = tracker.trackToNearestTrajectory(frames, costFunc);
		trajs.drawLines(imp);

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
