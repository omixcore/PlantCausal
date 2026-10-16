package org.plantcausal.causal;

/**
 * Numerical Gamma/log-Gamma utilities for statistical distributions.
 * This module provides the foundation for high precision Student-t inference.
 */
public final class LanczosGamma {
    private LanczosGamma() {}

    public static double logGamma(double x) {
        if (x <= 0) throw new IllegalArgumentException("Gamma domain error");
        double[] c = {76.18009172947146,-86.50532032941677,24.01409824083091,
                -1.231739572450155,0.001208650973866179,-5.395239384953E-6};
        double y=x;
        double tmp=x+5.5;
        tmp-=(x+0.5)*Math.log(tmp);
        double ser=1.000000000190015;
        for(double v:c){y+=1;ser+=v/y;}
        return -tmp+Math.log(2.5066282746310005*ser/x);
    }
}
