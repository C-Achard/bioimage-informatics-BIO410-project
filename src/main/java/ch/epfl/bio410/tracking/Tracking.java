// TODO : define core tracking algorithms and add to interface
package ch.epfl.bio410.tracking;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.plugin.ImageCalculator;
import ij.process.ImageProcessor;
import ch.epfl.bio410.tracking.graph.PartitionedGraph;
import ch.epfl.bio410.tracking.graph.Spot;
import ch.epfl.bio410.tracking.graph.Spots;
import ch.epfl.bio410.tracking.cost.AbstractCost;

/**
 * This class implements the tracking algorithm for particles.
 * It uses a Difference of Gaussian (DoG) filter to detect the spots, then the local maxima are detected
 * using localMax function. The tracking is done by extending the current trajectories by
 * appending the nearest valid spot of the next frame.
 */
public class Tracking {
    /**
     * This method allows to track single spots across frames.
     * The algorithm is working by extending the current trajectories by
     * appending the nearest valid spot of the next frame.
     *
     * @param frames Graph organized by partition of spots belonging to the same frame
     * @param cost   Cost function for the connection of spots
     * @return Graph organized by partition of spots belonging to the same trajectory
     */
    public PartitionedGraph trackToNearestTrajectory(PartitionedGraph frames, AbstractCost cost) {
        PartitionedGraph trajectories = new PartitionedGraph();
        for (Spots frame : frames) { // iterate over spots in all frames
            for (Spot spot : frame) {
                Spots trajectory = trajectories.getPartitionOf(spot); // get existing trajectory ending on spot, if one exists
                if (trajectory == null)
                    trajectory = trajectories.createPartition(spot); // if no trajectory exists, make a new from the current spot
                if (spot.equals(trajectory.last())) { // if trajectory ends on spot, find next one
                    Spot nearest = null;
                    double minCost = Double.MAX_VALUE; // max so that we can find min
                    int currentFrameIndex = spot.t;
                    if (currentFrameIndex < frames.size() - 1) { // check if we are not at the last frame
                        for (Spot next : frames.get(currentFrameIndex + 1)) {
                            // compute the nearest spot (the one with min cost value in frame t+1)
                            if (cost.validate(next, spot)) { // check if there is a valid spot cost-wise
                                double c = cost.evaluate(next, spot); // compute cost
                                if (c < minCost) {
                                    // if a spot with lower cost is found, update the minCost and the nearest spot
                                    minCost = c;
                                    nearest = next;
                                }
                            }
                        }
                    }
                    if (nearest != null) { // if a nearest spot was found
                        IJ.log("#" + trajectories.size() + " spot " + nearest + " with a cost:" + minCost);
                        // add nearest found spot to the trajectory
                        trajectory.add(nearest);
                    }
                }
            }
        }
        return trajectories;
    }

    /**
     * This function detects the spots in the image and returns a partitioned graph.
     * It uses a Difference of Gaussian (DoG) filter to detect the spots, then the local maxima are detected
     * using localMax function.
     *
     * @param imp       The ImagePlus from which we extract the data as ip (ImageProcessor)
     * @param sigma     The sigma for the DoG filter (the other is computed automatically)
     * @param threshold The threshold to detect the local maxima
     * @return The partitioned graph containing the detected spots
     */
    public PartitionedGraph detect(ImagePlus imp, double sigma, double threshold) {
        int nt = imp.getNFrames();
        new ImagePlus("DoG", dog(imp.getProcessor(), sigma)).show();
        PartitionedGraph graph = new PartitionedGraph();
        for (int t = 0; t < nt; t++) {
            imp.setPosition(1, 1, 1 + t);
            ImageProcessor ip = imp.getProcessor();
            ImageProcessor dog = dog(ip, sigma); // DoG filtering
            Spots spots = localMax(dog, ip, t, threshold); // Local maxima
            IJ.log("Frame t:" + t + " #localmax:" + spots.size());
            graph.add(spots);
        }
        return graph;
    }

    /**
     * Filters the image with a Difference of Gaussian filter, as seen in Week 4.
     *
     * @param ip    The ImageProcessor to filter
     * @param sigma The sigma1 of the Gaussian filter. The sigma2 is calculated as sqrt(2) * sigma
     * @return The ImageProcessor filtered with the Difference of Gaussian filter
     */
    public ImageProcessor dog(ImageProcessor ip, double sigma) {
        ImagePlus g1 = new ImagePlus("g1", ip.duplicate());
        ImagePlus g2 = new ImagePlus("g2", ip.duplicate());
        double sigma2 = (Math.sqrt(2) * sigma);
        GaussianBlur3D.blur(g1, sigma, sigma, 0);
        GaussianBlur3D.blur(g2, sigma2, sigma2, 0);
        ImagePlus dog = ImageCalculator.run(g1, g2, "Subtract create stack");
        return dog.getProcessor();
    }

    /**
     * This function detects local maxima in the image, after DoG filtering.
     *
     * @param dog       An image filtered with a Difference of Gaussian filter
     * @param image     The original image
     * @param t         The current frame (time point)
     * @param threshold The threshold to detect the local maxima
     * @return A Spots object containing the detected local maxima
     */
    public Spots localMax(ImageProcessor dog, ImageProcessor image, int t, double threshold) {
        Spots spots = new Spots();
        for (int x = 1; x < dog.getWidth() - 1; x++) {
            for (int y = 1; y < dog.getHeight() - 1; y++) {
                double valueImage = image.getPixelValue(x, y);
                if (valueImage >= threshold) {
                    double v = dog.getPixelValue(x, y);
                    double max = -1;
                    for (int k = -1; k <= 1; k++)
                        for (int l = -1; l <= 1; l++)
                            max = Math.max(max, dog.getPixelValue(x + k, y + l));
                    if (v == max) spots.add(new Spot(x, y, t, valueImage));
                }
            }
        }
        return spots;
    }
}
