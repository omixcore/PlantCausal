package org.plantcausal.desktop;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DesktopAnalysisWorkflowTest {
 @Test void loadDemoThenAnalyzeThroughActualButtons() throws Exception {
  AtomicReference<DesktopApp> app=new AtomicReference<>();
  SwingUtilities.invokeAndWait(()->app.set(new DesktopApp()));
  SwingUtilities.invokeAndWait(()->button(app.get(),"Load estimator demo").doClick());
  waitFor(()->hasLabel(app.get(),"units.tsv loaded"));
  SwingUtilities.invokeAndWait(()->button(app.get(),"Run analysis").doClick());
  waitFor(()->hasLabel(app.get(),"Analysis complete; identification assumptions require review"));
  SwingUtilities.invokeAndWait(()->{
   JTable table=analysisTable(app.get());assertEquals(3,table.getRowCount());assertEquals(2,((Number)table.getValueAt(1,1)).doubleValue(),1e-10);
   JTextField field=baselineField(app.get());field.setText("");assertEquals(0,table.getRowCount(),"Parameter changes must clear stale estimates");
  });
 }
 @Test void baselineCausalDemoRunsThroughActualUi() throws Exception {
  AtomicReference<DesktopApp> app=new AtomicReference<>();SwingUtilities.invokeAndWait(()->app.set(new DesktopApp()));
  SwingUtilities.invokeAndWait(()->button(app.get(),"Load causal demo").doClick());waitFor(()->hasLabel(app.get(),"units.tsv loaded"));
  SwingUtilities.invokeAndWait(()->button(app.get(),"Run analysis").doClick());waitFor(()->hasLabel(app.get(),"Analysis complete; identification assumptions require review"));
  SwingUtilities.invokeAndWait(()->{var table=analysisTable(app.get());assertEquals(1,table.getRowCount());assertEquals(1.994271317868655,((Number)table.getValueAt(0,1)).doubleValue(),1e-8);});
 }
 @Test void longitudinalDemoRunsThroughActualUi() throws Exception {
  AtomicReference<DesktopApp> app=new AtomicReference<>();SwingUtilities.invokeAndWait(()->app.set(new DesktopApp()));
  SwingUtilities.invokeAndWait(()->button(app.get(),"Load longitudinal demo").doClick());waitFor(()->hasLabel(app.get(),"units.tsv loaded"));
  SwingUtilities.invokeAndWait(()->button(app.get(),"Run analysis").doClick());waitFor(()->hasLabel(app.get(),"Analysis complete; identification assumptions require review"));
  SwingUtilities.invokeAndWait(()->{var table=analysisTable(app.get());assertEquals(1,table.getRowCount());assertEquals(2.954201370544272,((Number)table.getValueAt(0,1)).doubleValue(),1e-8);});
 }
 private static JButton button(Container root,String text) {
  for(Component c:root.getComponents()) {if(c instanceof JButton b&&b.getText().equals(text))return b;if(c instanceof Container child){JButton b=button(child,text);if(b!=null)return b;}}
  return null;
 }
 private static JTable analysisTable(Container root) {
  for(Component c:root.getComponents()) {if(c instanceof JTable t&&t.getColumnCount()==7&&t.getColumnName(0).equals("Term"))return t;if(c instanceof Container child){var t=analysisTable(child);if(t!=null)return t;}}return null;
 }
 private static JTextField baselineField(Container root) {
  for(Component c:root.getComponents()) {if(c instanceof JTextField f&&f.getText().equals("baseline_x"))return f;if(c instanceof Container child){var f=baselineField(child);if(f!=null)return f;}}return null;
 }
 private static boolean hasLabel(Container root,String text) {
  for(Component c:root.getComponents()) {if(c instanceof JLabel l&&l.getText().equals(text))return true;if(c instanceof Container child&&hasLabel(child,text))return true;}return false;
 }
 private static void waitFor(java.util.function.BooleanSupplier condition) throws Exception {
  long deadline=System.nanoTime()+20_000_000_000L;
  while(System.nanoTime()<deadline) {
   AtomicReference<Boolean> ready=new AtomicReference<>(false);SwingUtilities.invokeAndWait(()->ready.set(condition.getAsBoolean()));
   if(ready.get())return;Thread.sleep(50);
  }
  fail("Desktop workflow did not complete within 20 seconds");
 }
}
