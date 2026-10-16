package org.plantcausal.math;

/** Lanczos approximation for log Gamma. */
public class GammaFunction {
    private static final double[] COEFF = {76.18009172947146,-86.50532032941677,24.01409824083091,-1.231739572450155,0.001208650973866179,-5.395239384953E-6};

    public static double logGamma(double x) {
        if(!Double.isFinite(x)||x<=0) throw new IllegalArgumentException("Gamma requires finite positive x");
        double y=x;
        double tmp=x+5.5;
        tmp-=(x+0.5)*Math.log(tmp);
        double ser=1.000000000190015;
        for(int j=0;j<COEFF.length;j++) ser+=COEFF[j]/++y;
        return -tmp+Math.log(2.5066282746310005*ser/x);
    }

    public static double gamma(double x){ return Math.exp(logGamma(x)); }
}
