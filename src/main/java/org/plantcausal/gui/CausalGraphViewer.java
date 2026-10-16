package org.plantcausal.gui;
import javax.swing.JPanel;
import java.awt.*;
import java.util.*;
/** Swing DAG view, matching the desktop application's existing UI runtime. */
public class CausalGraphViewer extends JPanel {
 private JavaFxDAGViewerModel model;
 public CausalGraphViewer(double width,double height){setPreferredSize(new Dimension((int)width,(int)height));}
 public void render(JavaFxDAGViewerModel value){model=value; repaint();}
 @Override protected void paintComponent(Graphics g) {
  super.paintComponent(g); if(model==null) return;
  Map<String,Integer> positions=new LinkedHashMap<>(); int i=0;
  for(String node:model.getNodes()) positions.put(node,60+100*i++);
  g.setColor(Color.GRAY);
  for(String[] edge:model.getEdges()) {
   Integer a=positions.get(edge[0]),b=positions.get(edge[1]);
   if(a!=null && b!=null) {
    g.drawLine(a,80,b,80); int d=b>a?1:-1;
    g.drawLine(b,80,b-8*d,75); g.drawLine(b,80,b-8*d,85);
   }
  }
  for(var entry:positions.entrySet()) {
   int x=entry.getValue(); g.setColor(new Color(210,233,248)); g.fillOval(x-15,65,30,30);
   g.setColor(Color.DARK_GRAY); g.drawString(entry.getKey(),x-20,55);
  }
 }
}
