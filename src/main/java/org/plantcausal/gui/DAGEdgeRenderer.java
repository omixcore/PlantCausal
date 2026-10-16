package org.plantcausal.gui;

public class DAGEdgeRenderer {

    public double[] arrowHead(double x1, double y1, double x2, double y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double size = 8.0;
        return new double[]{
            x2 - size * Math.cos(angle - Math.PI / 6),
            y2 - size * Math.sin(angle - Math.PI / 6),
            x2 - size * Math.cos(angle + Math.PI / 6),
            y2 - size * Math.sin(angle + Math.PI / 6)
        };
    }
}
