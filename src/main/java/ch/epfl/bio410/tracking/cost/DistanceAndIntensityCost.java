package ch.epfl.bio410.tracking.cost;

import ch.epfl.bio410.tracking.graph.Spot;
import ij.ImagePlus;
import ij.plugin.ZProjector;

/**
 * This class implements the "DistanceAndIntensityCost" algorithm for tracking particles.
 * It implements the "AbstractCost" interface to benefit from the generic methods "evaluate" and "validate"
 */
public class DistanceAndIntensityCost implements AbstractCost {

    private double lambda = 0;
    private double costMax = 0;
    private double normalizationDistance = 1;
    private double normalizationIntensity = 1;

    public DistanceAndIntensityCost(ImagePlus imp, double costMax, double lambda) {
        this.lambda = lambda;
        this.costMax = costMax;
//        this.normalizationDistance = imp.getWidth() + imp.getHeight();
        this.normalizationDistance = Math.sqrt(imp.getWidth() * imp.getWidth() + imp.getHeight() * imp.getHeight());
//        this.normalizationIntensity = imp.getProcessor().getStatistics().max;
        this.normalizationIntensity = ZProjector.run(imp, "max").getStatistics().max;
    }

    @Override
    public double evaluate(Spot a, Spot b) {
    /**
     * cost_function = lambda * dist(xa, xb) / distmax + (1 - lambda) * abs(I(xa) - I(xb)) / Imax
     * lambda is the hyperparameter to balance the 2 terms.
     * distmax is the maximum distance between 2 spots
     * Imax is the maximum intensity value
     * xa and xb are the spots to evaluate.
     */
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        // compute normalized distance
        double distance = Math.sqrt(dx * dx + dy * dy) / normalizationDistance;
        // compute normalized intensity difference
        double intensity = Math.abs(a.value - b.value) / normalizationIntensity;
        return (lambda * distance) + (1 - lambda) * intensity;
    }

    @Override
    public boolean validate(Spot a, Spot b) {
        if (a == null) return false;
        if (b == null) return false;
        return evaluate(a, b) < costMax;
    }
}
