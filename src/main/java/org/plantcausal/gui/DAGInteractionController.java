package org.plantcausal.gui;

public class DAGInteractionController {
    private double zoom = 1.0;

    public void zoomIn() {
        zoom *= 1.1;
    }

    public void zoomOut() {
        zoom /= 1.1;
    }

    public double getZoom() {
        return zoom;
    }
}
