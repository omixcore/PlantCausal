package org.plantcausal.math;

/** Continued fraction evaluation for incomplete beta. */
public class BetaContinuedFraction {
    public static double evaluate(double a,double b,double x){
        if(!Double.isFinite(a)||!Double.isFinite(b)||!Double.isFinite(x)||a<=0||b<=0||x<0||x>1) throw new IllegalArgumentException("Invalid beta fraction parameters");
        final int max=10000;
        final double eps=1e-14;
        double qab=a+b, qap=a+1, qam=a-1;
        double c=1, d=1-qab*x/qap;
        if(Math.abs(d)<1e-30)d=1e-30;
        d=1/d;
        double h=d;
        for(int m=1;m<=max;m++){
            int m2=2*m;
            double aa=m*(b-m)*x/((qam+m2)*(a+m2));
            d=1+aa*d; if(Math.abs(d)<1e-30)d=1e-30;
            c=1+aa/c; if(Math.abs(c)<1e-30)c=1e-30;
            d=1/d; h*=d*c;
            aa=-(a+m)*(qab+m)*x/((a+m2)*(qap+m2));
            d=1+aa*d; if(Math.abs(d)<1e-30)d=1e-30;
            c=1+aa/c; if(Math.abs(c)<1e-30)c=1e-30;
            d=1/d;
            double del=d*c; h*=del;
            if(Math.abs(del-1)<eps) return h;
        }
        throw new ArithmeticException("Incomplete beta fraction did not converge");
    }
}
