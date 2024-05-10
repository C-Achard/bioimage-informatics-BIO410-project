package ch.epfl.bio410;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>BII>Replisome Analysis")
public class Replisome_Analysis implements Command {

		/////////// EXAMPLE, PLEASE ADAPT TO YOUR NEEDS ///////////

		// Default path is 5 folders above the current folder, in DATA
		private String path = Paths.get(System.getProperty("user.home"), "Desktop", "Code", "bioimage-informatics-BIO410-project", "DATA").toString();

		// Constants below are set for homework.tif, except distmax
		private final double sigma = 5.0;  // Detection parameters, sigma of the DoG, 6 for easy.tif
		private final double threshold = 160.0;  // Detection parameters, threshold of the localmax
		private final double costmax = 0.045;	// Cost parameters DistanceAndIntensityCost
		private final double lambda = 0.95; 	// Cost parameters DistanceAndIntensityCost
	public void run() {
		// resolve path
		GenericDialog dlg = new GenericDialog("Replisome Analysis");
		//print
		dlg.addDirectoryField("Path to the image", path);
		File directory = new File(path);
		String[] fileList = directory.list();
		if (fileList == null || fileList.length == 0) {
			fileList = new String[]{};
			IJ.log("No images found in folder " + path + ", please enter the path to the image");
		}
		dlg.addChoice("Image", fileList, fileList.length > 0 ? fileList[0] : "");
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
		ImagePlus imp = IJ.openImage(imagePath);
		imp.show();

		// split the channels in DIC and GFP. DIC is used for segmentation, GFP for tracking)
		IJ.run(imp, "Split Channels", "");
		ImagePlus imageDIC = WindowManager.getImage("C1-" + imp.getTitle());
		ImagePlus imageGFP = WindowManager.getImage("C2-" + imp.getTitle());
		// show the results
		imageDIC.show();
		imageGFP.show();
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
