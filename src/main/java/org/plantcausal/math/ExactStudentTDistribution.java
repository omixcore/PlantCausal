package org.plantcausal.math;

public class ExactStudentTDistribution {
    public static double twoSidedP(double t,int df){
        if(df<=0) throw new IllegalArgumentException("df");
        if(Double.isNaN(t)) throw new IllegalArgumentException("NaN t statistic");
        if(t==0) return 1.0;
        double x=df/(df+t*t);
        return RegularizedIncompleteBeta.evaluate(x,df/2.0,0.5);
    }
    public static double criticalValue95(int df) {
        if(df<=0) throw new IllegalArgumentException("df");
        double low=0, high=1;
        while(twoSidedP(high,df)>0.05) high*=2;
        for(int i=0;i<100;i++) {
            double mid=(low+high)/2;
            if(twoSidedP(mid,df)>0.05) low=mid; else high=mid;
        }
        return (low+high)/2;
    }
}
