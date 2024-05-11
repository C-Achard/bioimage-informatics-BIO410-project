package ch.epfl.bio410;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ChannelSplitter;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import ch.epfl.bio410.utils.utils;
import ch.epfl.bio410.segmentation.segmentation;


@Plugin(type = Command.class, menuPath = "Plugins>BII>Replisome_Analysis")
public class Replisome_Analysis implements Command {

	private final double sigma = 10;  // Detection parameters, sigma of the DoG


	@Override
	public void run() {

		ImagePlus imp0 = IJ.openImage("C:/Users/mathi/OneDrive/Documents/EPFL/MA4/Bioimage Analysis/Project/Project - SPB - Spot in bacteria/Bacteria/Merged-2.tif");
		imp0.show();
		// because I don't have enough memory
		IJ.run("Duplicate...", "duplicate frames=1-10");
		ImagePlus imp = IJ.getImage();
		imp.show();

		//changing memory
		//IJ.run("Memory & Threads...", "maximum=6000 parallel=8 run");

		// Split channels
		ImagePlus[] channels = ChannelSplitter.split(imp);
		System.out.println(channels.getClass());
		ImagePlus DIC = channels[0];
		ImagePlus dots = channels[1];

		// Removing noise
		ImagePlus denoised = utils.remove_noise(DIC,sigma);
		denoised.show();

		// Segmentation
		segmentation.segment(denoised);
		denoised.show();
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