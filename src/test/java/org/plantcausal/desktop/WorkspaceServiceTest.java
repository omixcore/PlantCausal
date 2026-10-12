package org.plantcausal.desktop;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.plantcausal.core.PackageReader;
import java.nio.file.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceServiceTest {
    @TempDir Path temp;
    private final WorkspaceService service=new WorkspaceService();
    @Test void builtinsWorkOutsideRepositoryWorkingDirectory() throws Exception {
        for(String id:new String[]{"synthetic","PC_B001"}) {
            Path directory=service.builtin(id);var report=service.validate(directory).report();
            assertTrue(report.schemaValid());assertEquals(id.equals("synthetic")?4:280,report.unitCount());
        }
    }
    @Test void saveValidProtocolPreservesExtensionsAndUnknownFieldsAreRejected() throws Exception {
        Path directory=service.builtin("PC_B001");var node=(com.fasterxml.jackson.databind.node.ObjectNode)service.protocol(directory);var extension=node.get("extensions").deepCopy();
        node.put("title","Edited title");service.saveProtocol(directory,node.toString());assertEquals(extension,service.protocol(directory).get("extensions"));
        String before=Files.readString(directory.resolve("protocol.json"));node.put("not_allowed",true);
        assertThrows(java.io.IOException.class,()->service.saveProtocol(directory,node.toString()));assertEquals(before,Files.readString(directory.resolve("protocol.json")));
    }
    @Test void reportsRejectChangesToValidatedInput() throws Exception {
        Path directory=service.builtin("synthetic");var snapshot=service.validate(directory);Path output=temp.resolve("report.json");
        service.exportReport(snapshot,output);assertTrue(PackageReader.JSON.readTree(output.toFile()).has("input_sha256"));
        Files.writeString(directory.resolve("units.tsv"),"changed");assertThrows(java.io.IOException.class,()->service.exportReport(snapshot,output));
    }
    @Test void projectCopyNeverOverwritesExistingContents() throws Exception {
        Path source=service.builtin("synthetic"),dest=temp.resolve("saved");service.copyProject(source,dest);
        assertTrue(service.validate(dest).report().schemaValid());assertThrows(java.io.IOException.class,()->service.copyProject(source,dest));
        assertThrows(java.io.IOException.class,()->service.copyProject(source,source.resolve("nested")));
    }
    @Test void previewsHaveExplicitLimitAndKeepMissingValues() throws Exception {
        Path source=service.builtin("PC_B001");var preview=service.preview(source.resolve("observations.tsv"),25);
        assertEquals(25,preview.rows().size());assertEquals(1400,preview.totalRows());assertTrue(preview.columns().contains("missing_reason"));
    }
    @Test void invalidDraftDoesNotCreateProjectCopy() throws Exception {
        Path source=service.builtin("synthetic"),dest=temp.resolve("invalid");
        assertThrows(Exception.class,()->service.saveProjectAs(source,dest,"{}"));assertFalse(Files.exists(dest));
    }
    @Test void guiLoadsPublicDemoAndUsesCoreValidation() throws Exception {
        DesktopApp[] app=new DesktopApp[1];SwingUtilities.invokeAndWait(()->{app[0]=new DesktopApp();app[0].loadBuiltin("PC_B001");});
        await(()->all(app[0]).stream().anyMatch(c->c instanceof JTable t&&t.getColumnCount()==13&&t.getRowCount()==280));
        SwingUtilities.invokeAndWait(()->all(app[0]).stream().filter(c->c instanceof JButton b&&b.getText().equals("Validate")).map(c->(JButton)c).findFirst().orElseThrow().doClick());
        await(()->all(app[0]).stream().anyMatch(c->c instanceof JTable t&&t.getColumnCount()==4&&t.getRowCount()==841));
    }
    private static java.util.List<Component> all(Container root){java.util.List<Component> result=new java.util.ArrayList<>();for(Component c:root.getComponents()){result.add(c);if(c instanceof Container)result.addAll(all((Container)c));}return result;}
    private static void await(java.util.function.BooleanSupplier condition) throws Exception {
        long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);boolean[] ok={false};
        while(System.nanoTime()<end){SwingUtilities.invokeAndWait(()->ok[0]=condition.getAsBoolean());if(ok[0])return;Thread.sleep(50);}
        fail("GUI background operation did not complete");
    }
    @Test void desktopCanRenderInHeadlessTests() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            DesktopApp panel=new DesktopApp();panel.setSize(1220,800);layout(panel);
            BufferedImage image=new BufferedImage(1220,800,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
            assertNotEquals(image.getRGB(5,5),image.getRGB(1100,200));
        });
    }
    static void layout(Container container){container.doLayout();for(Component child:container.getComponents())if(child instanceof Container)layout((Container)child);}
}
