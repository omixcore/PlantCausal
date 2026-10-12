package org.plantcausal.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.plantcausal.core.PackageReader;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Native Java desktop UI; expensive work is performed outside the Swing event thread. */
public final class DesktopApp extends JPanel {
    private static final long serialVersionUID=1L;
    private static final Color BG=new Color(244,248,252),BLUE=new Color(40,115,184),INK=new Color(34,53,72),MUTED=new Color(106,124,142);
    private final WorkspaceService service=new WorkspaceService();
    private Path directory;private boolean loading,busy;private String savedText="";private WorkspaceService.Snapshot snapshot;
    private final JLabel project=new JLabel("No project open"),status=new JLabel("Ready — open a project or load a built-in example"),summary=new JLabel("Open a project to view its data and validation status.");
    private final CardLayout layout=new CardLayout();private final JPanel pages=new JPanel(layout);
    private final JTextArea json=new JTextArea(),benchmarkDetails=new JTextArea();
    private final Map<String,JTextArea> fields=new LinkedHashMap<>();
    private final DefaultTableModel dataModel=readonly(),issueModel=readonly("Severity","Rule","Location","Message");
    private final JTable dataTable=new JTable(dataModel),issueTable=new JTable(issueModel);
    private final JComboBox<String> dataFiles=new JComboBox<>(new String[]{"units.tsv","variables.tsv","observations.tsv","treatments.tsv","events.tsv"});
    private final JLabel rowCount=new JLabel("No table loaded"),reportHeading=new JLabel("No validation report yet");
    private final JTextField search=new JTextField(20);private final List<JButton> actions=new ArrayList<>();
    private final JTabbedPane protocolTabs=new JTabbedPane();
    private JsonNode formBase;

    public static void launch() {
        if(GraphicsEnvironment.isHeadless()) {System.err.println("Desktop requires a graphical session. Use validate <directory> for CLI mode.");return;}
        SwingUtilities.invokeLater(()->{
            UIManager.put("Label.font",new Font("SansSerif",Font.PLAIN,13));UIManager.put("Button.font",new Font("SansSerif",Font.PLAIN,13));
            JFrame frame=new JFrame("PlantCausal | Desktop 0.2.0");DesktopApp app=new DesktopApp();
            frame.setContentPane(app);frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){if(!app.busy&&app.discardAllowed())frame.dispose();}});
            frame.setMinimumSize(new Dimension(960,650));frame.setSize(1220,800);frame.setLocationRelativeTo(null);frame.setVisible(true);
        });
    }
    public DesktopApp() {
        super(new BorderLayout());setBackground(BG);
        JPanel sidebar=new JPanel();sidebar.setLayout(new BoxLayout(sidebar,BoxLayout.Y_AXIS));sidebar.setBackground(new Color(228,240,250));sidebar.setBorder(new EmptyBorder(30,20,20,20));sidebar.setPreferredSize(new Dimension(205,0));
        sidebar.add(label("PlantCausal",24,true));sidebar.add(Box.createVerticalStrut(7));sidebar.add(label("TARGET TRIAL WORKSPACE",10,false));sidebar.add(Box.createVerticalStrut(35));
        for(String page:List.of("Overview","Protocol","Data preview","Validation","Benchmarks")) {
            JButton nav=button(page,()->layout.show(pages,page));nav.setMaximumSize(new Dimension(165,42));nav.setAlignmentX(LEFT_ALIGNMENT);sidebar.add(nav);sidebar.add(Box.createVerticalStrut(10));
        }
        sidebar.add(Box.createVerticalGlue());sidebar.add(label("Desktop v0.2.0",12,false));sidebar.add(Box.createVerticalStrut(8));sidebar.add(label("Causal estimation: planned",11,false));add(sidebar,BorderLayout.WEST);
        JPanel content=new JPanel(new BorderLayout(0,18));content.setBackground(BG);content.setBorder(new EmptyBorder(24,24,12,24));
        JPanel header=new JPanel(new BorderLayout(15,8));header.setOpaque(false);JPanel titles=new JPanel(new GridLayout(2,1,0,5));titles.setOpaque(false);titles.add(label("Plant causal data workspace",23,true));project.setForeground(MUTED);titles.add(project);header.add(titles,BorderLayout.CENTER);
        JPanel toolbar=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));toolbar.setOpaque(false);toolbar.add(button("Open project",this::chooseProject));toolbar.add(button("Save project as",this::saveAs));toolbar.add(button("Validate",this::validateProject));header.add(toolbar,BorderLayout.SOUTH);content.add(header,BorderLayout.NORTH);
        pages.setOpaque(false);pages.add(overview(),"Overview");pages.add(protocolPage(),"Protocol");pages.add(dataPage(),"Data preview");pages.add(reportPage(),"Validation");pages.add(benchmarkPage(),"Benchmarks");content.add(pages,BorderLayout.CENTER);
        status.setForeground(MUTED);status.setBorder(new EmptyBorder(9,0,0,0));content.add(status,BorderLayout.SOUTH);add(content,BorderLayout.CENTER);
        json.setFont(new Font("Monospaced",Font.PLAIN,12));json.getDocument().addDocumentListener(listener(()->{if(!loading){snapshot=null;reportHeading.setText("Protocol edited — save and validate again");status.setText("Unsaved protocol edits");}}));
        dataTable.setAutoCreateRowSorter(true);dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);dataTable.setRowHeight(26);issueTable.setRowHeight(26);issueTable.setAutoCreateRowSorter(true);
        search.getDocument().addDocumentListener(listener(this::filter));dataFiles.addActionListener(e->{if(directory!=null&&!loading&&!busy)loadTable();});
    }
    private static DefaultTableModel readonly(String... columns) {return new DefaultTableModel(columns,0){private static final long serialVersionUID=1L;@Override public boolean isCellEditable(int r,int c){return false;}};}
    private static DocumentListener listener(Runnable action) {return new DocumentListener(){public void insertUpdate(DocumentEvent e){action.run();}public void removeUpdate(DocumentEvent e){action.run();}public void changedUpdate(DocumentEvent e){action.run();}};}
    private JLabel label(String text,int size,boolean bold){JLabel l=new JLabel(text);l.setFont(new Font("SansSerif",bold?Font.BOLD:Font.PLAIN,size));l.setForeground(INK);return l;}
    private JButton button(String text,Runnable action) {JButton b=new JButton(text);b.setFocusPainted(false);b.setBackground(Color.WHITE);b.setForeground(BLUE);b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(195,216,234)),new EmptyBorder(9,14,9,14)));b.addActionListener(e->{if(!busy)action.run();});actions.add(b);return b;}
    private JPanel page(String title,String description) {JPanel p=new JPanel(new BorderLayout(0,14));p.setBackground(Color.WHITE);p.setBorder(new EmptyBorder(22,22,22,22));JPanel top=new JPanel(new GridLayout(2,1,0,6));top.setOpaque(false);top.add(label(title,20,true));top.add(label(description,12,false));p.add(top,BorderLayout.NORTH);return p;}
    private JPanel overview() {
        JPanel p=page("Prepare a defensible target trial","Define the protocol, inspect your observations, and audit data quality.");
        JPanel center=new JPanel();center.setOpaque(false);center.setLayout(new BoxLayout(center,BoxLayout.Y_AXIS));
        summary.setFont(new Font("SansSerif",Font.BOLD,16));center.add(summary);center.add(Box.createVerticalStrut(25));
        center.add(label("01  Open data     02  Define protocol     03  Validate & export",15,true));center.add(Box.createVerticalStrut(25));
        JPanel cards=new JPanel(new GridLayout(1,2,18,0));cards.setOpaque(false);
        cards.add(infoCard("Synthetic example","4 units · 8 observations","For learning the format and validation workflow.","Load demo",()->loadBuiltin("synthetic")));
        cards.add(infoCard("Public pine benchmark","280 units · 1,400 observations","Source-backed data with explicit design limitations.","Load public data",()->loadBuiltin("PC_B001")));
        cards.setMaximumSize(new Dimension(1000,230));center.add(cards);center.add(Box.createVerticalStrut(24));
        center.add(label("A format pass is separate from causal analysis readiness.",13,false));center.add(Box.createVerticalStrut(10));center.add(label("Causal effect algorithms will be added in a later release.",13,false));center.add(Box.createVerticalGlue());for(Component child:center.getComponents())if(child instanceof JComponent)((JComponent)child).setAlignmentX(LEFT_ALIGNMENT);p.add(center,BorderLayout.CENTER);return p;
    }
    private JPanel infoCard(String title,String count,String note,String action,Runnable task) {
        JPanel card=new JPanel();card.setBackground(BG);card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));card.setBorder(new EmptyBorder(20,20,20,20));
        card.add(label(title,17,true));card.add(Box.createVerticalStrut(13));card.add(label(count,14,false));card.add(Box.createVerticalStrut(12));
        JTextArea text=new JTextArea(note);text.setOpaque(false);text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);text.setForeground(MUTED);text.setMaximumSize(new Dimension(500,55));card.add(text);card.add(Box.createVerticalStrut(16));card.add(button(action,task));return card;
    }
    private JPanel protocolPage() {
        JPanel p=page("Target trial protocol","Edit trial definitions; the full JSON preserves source mappings and extensions.");
        JPanel form=new JPanel();form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));form.setBackground(Color.WHITE);
        String[][] definitions={{"/title","Title"},{"/trial_id","Trial ID"},{"/target_trial/population","Target population"},{"/target_trial/eligibility","Eligibility criteria"},{"/target_trial/assignment","Treatment assignment"},{"/target_trial/assignment_unit","Assignment unit (plant / pot / plot / chamber / site)"},{"/target_trial/analysis_unit","Analysis unit"},{"/target_trial/strategies/0/id","Strategy 1 ID"},{"/target_trial/strategies/0/description","Strategy 1 definition"},{"/target_trial/strategies/1/id","Strategy 2 ID"},{"/target_trial/strategies/1/description","Strategy 2 definition"},{"/target_trial/time_zero","Time zero"},{"/target_trial/time_unit","Time unit (h / d / week)"},{"/target_trial/followup_end","Follow-up end (relative time)"},{"/target_trial/outcome/variable_id","Outcome variable ID"},{"/target_trial/outcome/time","Primary outcome time"},{"/target_trial/outcome/unit","Outcome unit"},{"/target_trial/outcome/definition","Primary outcome definition"},{"/target_trial/estimand/population","Estimand population"},{"/target_trial/estimand/contrast","Contrast (A-B)"},{"/target_trial/estimand/effect","Effect scale"},{"/target_trial/estimand/strategy_effect","Strategy effect"},{"/target_trial/estimand/intercurrent_events","Intercurrent events"},{"/target_trial/analysis_plan","Analysis plan"},{"/missing_data_plan","Missing data plan"}};
        for(String[] def:definitions) {
            JPanel row=new JPanel(new BorderLayout(0,7));row.setBackground(Color.WHITE);row.setBorder(new EmptyBorder(7,8,10,8));row.add(label(def[1],13,true),BorderLayout.NORTH);
            JTextArea field=new JTextArea(3,60);field.setLineWrap(true);field.setWrapStyleWord(true);field.setBorder(BorderFactory.createLineBorder(new Color(205,218,229)));fields.put(def[0],field);field.getDocument().addDocumentListener(listener(()->{if(!loading){snapshot=null;reportHeading.setText("Protocol edited — save and validate again");status.setText("Unsaved protocol edits");}}));row.add(field,BorderLayout.CENTER);row.setMaximumSize(new Dimension(1200,115));form.add(row);
        }
        protocolTabs.addTab("Protocol form",new JScrollPane(form));protocolTabs.addTab("Full JSON",new JScrollPane(json));
        protocolTabs.addChangeListener(e->{if(loading||formBase==null)return;try {if(protocolTabs.getSelectedIndex()==1)applyForm();else fillForm(PackageReader.JSON.readTree(json.getText()));}catch(Exception ex){error(ex);loading=true;protocolTabs.setSelectedIndex(1);loading=false;}});
        p.add(protocolTabs,BorderLayout.CENTER);JPanel bar=new JPanel(new FlowLayout(FlowLayout.LEFT));bar.setOpaque(false);bar.add(button("Save protocol",this::saveProtocol));bar.add(label("Tables are previewed as read-only; edit TSV files in your preferred editor.",12,false));p.add(bar,BorderLayout.SOUTH);return p;
    }
    private void fillForm(JsonNode node) {boolean previous=loading;loading=true;try{formBase=node.deepCopy();fields.forEach((key,field)->field.setText(node.at(key).asText()));}finally{loading=previous;}}
    private void applyForm() throws Exception {
        JsonNode node=formBase.deepCopy();
        for(var entry:fields.entrySet()) {String key=entry.getKey();int cut=key.lastIndexOf('/');JsonNode parent=node.at(key.substring(0,cut));if(!(parent instanceof ObjectNode))throw new IllegalArgumentException("Missing protocol section: "+key);if(node.at(key).isNumber())((ObjectNode)parent).put(key.substring(cut+1),new java.math.BigDecimal(entry.getValue().getText().trim()));else ((ObjectNode)parent).put(key.substring(cut+1),entry.getValue().getText());}
        String text=PackageReader.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        if(!text.equals(json.getText()))json.setText(text);formBase=node;
    }
    String draft() throws Exception {if(protocolTabs.getSelectedIndex()==0&&formBase!=null)applyForm();return json.getText();}
    private boolean dirty(){try{return directory!=null&&!PackageReader.JSON.readTree(draft()).equals(PackageReader.JSON.readTree(savedText));}catch(Exception ex){return directory!=null;}}
    private boolean discardAllowed(){return !dirty()||JOptionPane.showConfirmDialog(this,"Discard unsaved protocol edits?","Unsaved changes",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
    private JPanel dataPage() {
        JPanel p=page("Inspect project tables","Browse units, variables and observations before analysis.");JPanel bar=new JPanel(new FlowLayout(FlowLayout.LEFT));bar.setOpaque(false);bar.add(dataFiles);bar.add(new JLabel("Filter:"));bar.add(search);bar.add(button("Reload",this::loadTable));bar.add(rowCount);
        JPanel center=new JPanel(new BorderLayout(0,10));center.setOpaque(false);center.add(bar,BorderLayout.NORTH);center.add(new JScrollPane(dataTable),BorderLayout.CENTER);p.add(center,BorderLayout.CENTER);return p;
    }
    private void filter(){if(dataTable.getRowSorter() instanceof TableRowSorter<?> sorter)sorter.setRowFilter(search.getText().isEmpty()?null:RowFilter.regexFilter("(?i)"+Pattern.quote(search.getText())));}
    private JPanel reportPage() {
        JPanel p=page("Validation & design audit","Errors block format validation; warnings identify unresolved design information.");
        JPanel center=new JPanel(new BorderLayout(0,12));center.setOpaque(false);reportHeading.setFont(new Font("SansSerif",Font.BOLD,14));center.add(reportHeading,BorderLayout.NORTH);center.add(new JScrollPane(issueTable),BorderLayout.CENTER);p.add(center,BorderLayout.CENTER);
        JPanel bottom=new JPanel(new FlowLayout(FlowLayout.LEFT));bottom.setOpaque(false);bottom.add(button("Run validation",this::validateProject));bottom.add(button("Export report",this::exportReport));bottom.add(label("Design reviewed / analysis ready: not assessed",12,false));p.add(bottom,BorderLayout.SOUTH);return p;
    }
    private JPanel benchmarkPage() {
        JPanel p=page("Public benchmark catalog","Keep source provenance and data limitations visible.");
        JPanel list=new JPanel(new GridLayout(1,2,18,0));list.setOpaque(false);
        list.add(infoCard("PC_B001 · Scots pine","280 pots · 1,400 observations","Drought and recovery; standardized. Design review pending.","Open benchmark",()->loadBuiltin("PC_B001")));
        list.add(infoCard("PC_B002 · Grassland","196 plots · 1,176 harvests","Staging data only; treatment calendars still require mapping.","Preview staging",this::previewGrass));
        JPanel center=new JPanel(new BorderLayout(0,20));center.setOpaque(false);center.add(list,BorderLayout.NORTH);
        benchmarkDetails.setEditable(false);benchmarkDetails.setLineWrap(true);benchmarkDetails.setWrapStyleWord(true);benchmarkDetails.setFont(new Font("SansSerif",Font.PLAIN,13));
        benchmarkDetails.setText("PC_B001: https://zenodo.org/records/10100580\nArticle: 10.1093/treephys/tpae029\n\nPC_B002: https://zenodo.org/records/4306840\nArticle: 10.5194/bg-18-585-2021\n\nBoth datasets: CC BY 4.0. Full authors, checksums and extraction provenance are retained in benchmark/catalog.json.\n\nThese benchmarks evaluate data import and validation. Their true causal effects are unknown. No causal estimate is produced by this release.");center.add(new JScrollPane(benchmarkDetails),BorderLayout.CENTER);p.add(center,BorderLayout.CENTER);return p;
    }
    private void chooseProject(){if(!discardAllowed())return;JFileChooser chooser=new JFileChooser();chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)open(chooser.getSelectedFile().toPath(),false);}
    private void open(Path path,boolean temporary) {
        task("Opening project…",()->service.protocol(path),node->{directory=path.toAbsolutePath().normalize();loading=true;json.setText(pretty(node));savedText=json.getText();fillForm(node);loading=false;snapshot=null;issueModel.setRowCount(0);reportHeading.setText("Not validated yet");project.setText((temporary?"Temporary example · ":"")+node.path("title").asText());project.setToolTipText(directory.toString());summary.setText(node.path("trial_id").asText()+" · "+node.path("data_kind").asText()+" project");status.setText(temporary?"Example loaded — use Save project as to keep your changes":"Project loaded: "+directory);loadTable();layout.show(pages,"Data preview");});
    }
    public void loadBuiltin(String id){if(!discardAllowed())return;task("Loading built-in data…",()->service.builtin(id),path->open(path,true));}
    private void loadTable(){if(directory==null){status.setText("Open a project first");return;}Path file=directory.resolve((String)dataFiles.getSelectedItem());task("Loading table…",()->service.preview(file,2000),preview->showPreview(preview,file.getFileName().toString()));}
    private void showPreview(WorkspaceService.Preview preview,String name){dataModel.setDataVector(preview.rows().stream().map(List::toArray).toArray(Object[][]::new),preview.columns().toArray());for(int i=0;i<dataTable.getColumnCount();i++)dataTable.getColumnModel().getColumn(i).setPreferredWidth(145);filter();rowCount.setText(preview.rows().size()+" / "+preview.totalRows()+" rows");status.setText(name+" loaded"+ (preview.totalRows()>2000?" — preview limited to first 2,000 rows":""));}
    private void previewGrass(){task("Loading staged grassland data…",()->{try(var in=DesktopApp.class.getResourceAsStream("/builtin/PC_B002/harvests.staging.tsv")){Path file=Files.createTempFile("plantcausal-grass-",".tsv");Files.copy(Objects.requireNonNull(in),file,StandardCopyOption.REPLACE_EXISTING);return service.preview(file,2000);}},preview->{JTable table=new JTable(preview.rows().stream().map(List::toArray).toArray(Object[][]::new),preview.columns().toArray());table.setEnabled(false);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);JScrollPane scroll=new JScrollPane(table);scroll.setPreferredSize(new Dimension(850,420));JOptionPane.showMessageDialog(this,scroll,"PC_B002 staging · "+preview.totalRows()+" harvest records",JOptionPane.PLAIN_MESSAGE);});}
    private void saveProtocol(){if(directory==null){status.setText("Open a project first");return;}try{String text=draft();Path current=directory;task("Saving protocol…",()->{service.saveProtocol(current,text);return service.protocol(current);},node->{loading=true;json.setText(pretty(node));savedText=json.getText();fillForm(node);loading=false;snapshot=null;reportHeading.setText("Protocol saved — validate again");status.setText("Protocol saved");});}catch(Exception ex){error(ex);}}
    private void saveAs(){if(directory==null){status.setText("Open a project first");return;}JFileChooser c=new JFileChooser();c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);c.setDialogTitle("Choose an empty folder for your project");if(c.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;try{String text=draft();Path target=c.getSelectedFile().toPath(),source=directory;task("Saving project copy…",()->{service.saveProjectAs(source,target,text);return target;},path->open(path,false));}catch(Exception ex){error(ex);}}
    private void validateProject(){if(directory==null){status.setText("Open a project first");return;}if(dirty()){JOptionPane.showMessageDialog(this,"Save protocol edits before running validation.");layout.show(pages,"Protocol");return;}Path current=directory;task("Validating protocol and data…",()->service.validate(current),result->{snapshot=result;issueModel.setRowCount(0);long errors=0,warnings=0;for(var i:result.report().issues()){issueModel.addRow(new Object[]{i.severity(),i.code(),i.location(),i.message()});if(i.severity().equals("ERROR"))errors++;else warnings++;}reportHeading.setText((result.report().schemaValid()?"FORMAT PASS":"FORMAT FAIL")+" · "+errors+" errors · "+warnings+" warnings · "+result.report().unitCount()+" units · "+result.report().observationCount()+" observations");summary.setText(result.report().schemaValid()?"Format valid · design review required":"Format errors require attention");layout.show(pages,"Validation");status.setText("Validation complete — design and analysis readiness remain not assessed");});}
    private void exportReport(){if(snapshot==null||dirty()){JOptionPane.showMessageDialog(this,"Save edits and run validation before exporting.");return;}JFileChooser c=new JFileChooser();c.setSelectedFile(new java.io.File("plantcausal-validation.json"));if(c.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;Path target=c.getSelectedFile().toPath();if(Files.exists(target)&&JOptionPane.showConfirmDialog(this,"Replace this report?","Export",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;var current=snapshot;task("Exporting report…",()->{service.exportReport(current,target);return target;},path->status.setText("Report exported: "+path));}
    private static String pretty(JsonNode node){try{return PackageReader.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(node);}catch(Exception ex){throw new IllegalArgumentException(ex);}}
    private void error(Exception ex){status.setText("Operation failed");JTextArea text=new JTextArea(ex.getMessage()==null?ex.toString():ex.getMessage(),10,70);text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);JOptionPane.showMessageDialog(this,new JScrollPane(text),"PlantCausal",JOptionPane.ERROR_MESSAGE);}
    private void busyState(boolean value){busy=value;actions.forEach(b->b.setEnabled(!value));dataFiles.setEnabled(!value);json.setEditable(!value);fields.values().forEach(f->f.setEditable(!value));protocolTabs.setEnabled(!value);}
    private <T> void task(String title,Callable<T> work,Consumer<T> success){if(busy)return;busyState(true);status.setText(title);new SwingWorker<T,Void>(){protected T doInBackground()throws Exception{return work.call();}protected void done(){busyState(false);try{success.accept(get());}catch(Exception ex){Throwable cause=ex.getCause();error(cause instanceof Exception?(Exception)cause:ex);}}}.execute();}
}
