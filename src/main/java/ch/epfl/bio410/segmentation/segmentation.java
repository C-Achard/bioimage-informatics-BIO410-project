package ch.epfl.bio410.segmentation;

import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.plugin.ImageCalculator;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;

/**
 * This class implements the "DistanceAndIntensityCost" algorithm for tracking particles.
 * It implements the "AbstractCost" interface to benefit from the generic methods "evaluate" and "validate"
 */

/*
public class DistanceAndIntensityCost implements  {

    private double lambda = 0;

    public DistanceAndIntensityCost(ImagePlus imp, double costMax, double lambda) {
        this.lambda = lambda;
        this.costMax = costMax;
        int height = imp.getHeight();
        int width = imp.getWidth();
        this.normalizationDistance = Math.sqrt(height * height + width * width);
        this.normalizationIntensity = ZProjector.run(imp,"max").getStatistics().max;
    }

    @Override
    public double segment(Spot a, Spot b) {
        // TODO: Add your code here
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double Ia = a.value;
        double Ib = b.value;
        double Imax = normalizationIntensity;
        double dist = Math.sqrt(dx * dx + dy * dy);
        //cost_function = lambda * dist(xa, xb) / distmax + (1 - lambda) * abs(I(xa) - I(xb)) / Imax
        return lambda * dist / normalizationDistance + (1-lambda) * Math.abs(Ia-Ib) / Imax;
    }

    @Override
    public boolean denoise(Spot a, Spot b) {
        if (a == null) return false;
        if (b == null) return false;
        return evaluate(a, b) < costMax;
    }

    private ImageProcessor dog(ImageProcessor ip, double sigma) {
        ImagePlus g1 = new ImagePlus("g1", ip.duplicate());
        ImagePlus g2 = new ImagePlus("g2", ip.duplicate());
        double sigma2 = (Math.sqrt(2) * sigma);
        GaussianBlur3D.blur(g1, sigma, sigma, 0);
        GaussianBlur3D.blur(g2, sigma2, sigma2, 0);
        ImagePlus dog = ImageCalculator.run(g1, g2, "Subtract create stack");
        return dog.getProcessor();
    }
}
*/