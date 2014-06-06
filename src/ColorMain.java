import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.LinkedList;

/**
 * Created by petdav on 12/05/14.
 */
public class ColorMain extends JFrame {
    private JTabbedPane tabbedPane;
    private JSlider hueSlider;
    private JSlider satSlider;
    private JSlider valueSlider;
    private JTextField directoryField;
    private JRadioButton SQLScriptRadioButton;
    private JRadioButton directInsertRadioButton;
    private JCheckBox displayOutputCheckBox;
    private JButton processButton;
    private JButton directoryBrowse;
    private JSpinner hueSpinner;
    private JSpinner satSpinner;
    private JSpinner valueSpinner;
    private JTextField dumpOutputField;
    private JButton dumpOutputBrowse;
    private JRadioButton databaseRadioButton;
    private JRadioButton directoryRadioButton;
    private JPanel contentPane;
    private JPanel colorDisplay;
    private JTextField startVTTracking;
    private JTextField endVTTracking;
    private JSpinner numColorsSpinner;
    private JCheckBox debugOutputCheckBox;
    private JTextArea outputArea;
    private JFileChooser chooser;
    private boolean debug;
    private DebugUtil debugUtil;

    public ColorMain() {
        setContentPane(contentPane);
        setSize(500,400);
        setTitle("Representative Color Processing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        debugUtil = new DebugUtil(outputArea);

        numColorsSpinner.setValue(10);
        hueSlider.setValue(0);
        satSlider.setValue(0);
        valueSlider.setValue(0);
        hueSlider.setMaximum(128);
        hueSlider.setMinimum(-128);
        satSlider.setMaximum(128);
        satSlider.setMinimum(-128);
        valueSlider.setMaximum(128);
        valueSlider.setMinimum(-128);
        hueSpinner.setValue(hueSlider.getValue());
        satSpinner.setValue(satSlider.getValue());
        valueSpinner.setValue(valueSlider.getValue());
        directoryBrowse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);
                int retval = chooser.showOpenDialog(null);
                if (retval == JFileChooser.APPROVE_OPTION) {
                    directoryField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        dumpOutputBrowse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooser = new JFileChooser();
                int retval = chooser.showSaveDialog(null);
                if (retval == JFileChooser.APPROVE_OPTION) {
                    dumpOutputField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        hueSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                hueSpinner.setValue(hueSlider.getValue());
            }
        });
        satSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                satSpinner.setValue(satSlider.getValue());
            }
        });
        valueSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                valueSpinner.setValue(valueSlider.getValue());
            }
        });
        hueSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                hueSlider.setValue((int)hueSpinner.getValue());
            }
        });
        satSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                satSlider.setValue((int)satSpinner.getValue());
            }
        });
        valueSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                valueSlider.setValue((int)valueSpinner.getValue());
            }
        });

        processButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int hOffset, sOffset, vOffset;
                hOffset = hueSlider.getValue();
                sOffset = satSlider.getValue();
                vOffset = valueSlider.getValue();
                debug = debugOutputCheckBox.isSelected();
                RepColors col = new RepColors(debugUtil);
                col.debug = debug;
                ImageProc proc = new ImageProc("localhost", "VTMaster", "testuser", "test", "Textile_img", debug, debugUtil);

                col.refColors = proc.loadRefColors("Color_detail");
                int numColors = (int)numColorsSpinner.getValue();

                //Fetch images from database or directory
                LinkedList<ImageItem> images = new LinkedList<>();
                if (databaseRadioButton.isSelected()) {
                    String startID = startVTTracking.getText().trim().toUpperCase();
                    String endID = endVTTracking.getText().trim().toUpperCase();
                    proc.fetchImages("IMG_detail", startID, endID);
                    while (proc.hasNextImage()) {
                        images.add(proc.nextImage());
                    }
                }
                else {
                    LinkedList<File> files = new LinkedList<>();
                    recurseDirectory(new File(directoryField.getText()), files);

                    //Process all files in the list
                    for (File file : files) {
                        images.add(proc.readImageFromFile(file.getAbsolutePath()));
                    }
                }

                //Process images and insert into database or write to file
                if (!directInsertRadioButton.isSelected()) {
                    try {
                        BufferedWriter out = new BufferedWriter(new FileWriter(dumpOutputField.getText()));
                        //Insert the files or create the dump
                        for (ImageItem imageItem : images) {
                            debugUtil.debug("Processing image " + imageItem.vt_tracking, debug);
                            LinkedList<ColorCount> colors = col.processImage(imageItem, hOffset, sOffset, vOffset);
                            if (debug) {
                                String colorString = "\t";
                                int i = 0;
                                for (ColorCount colorCount: colors) {
                                    colorString += "\t" + colorCount.toString() + "\n";
                                    i++;
                                    if (i > numColors) break;
                                }
                                debugUtil.debug("Found colors:\n" + colorString, debug);
                            }
                            col.writeColors(proc, numColors, colors, out);
                        }
                        out.close();
                    }
                    catch (IOException er ) {
                        debugUtil.error("Could not open SQL script file for writing.");
                        if (debug) er.printStackTrace();
                    }
                }
                else {
                    for (ImageItem imageItem : images) {
                        debugUtil.debug("Processing image " + imageItem.vt_tracking, debug);
                        LinkedList<ColorCount> colors = col.processImage(imageItem, hOffset, sOffset, vOffset);
                        if (debug) {
                            String colorString = "\t";
                            int i = 0;
                            for (ColorCount colorCount: colors) {
                                colorString += "\t" + colorCount.toString() + "\n";
                                i++;
                                if (i > numColors) break;
                            }
                            debugUtil.debug("Found colors:\n" + colorString, debug);
                        }
                        col.insertColors(proc, numColors, colors);
                    }
                }

                JOptionPane.showMessageDialog(null, "Processing complete");
            }
        });
        directoryRadioButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                directoryField.setEnabled(directoryRadioButton.isSelected());
                directoryBrowse.setEnabled(directoryRadioButton.isSelected());
            }
        });
        SQLScriptRadioButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                dumpOutputField.setEnabled(SQLScriptRadioButton.isSelected());
                dumpOutputBrowse.setEnabled(SQLScriptRadioButton.isSelected());
            }
        });
        databaseRadioButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                startVTTracking.setEnabled(databaseRadioButton.isSelected());
                endVTTracking.setEnabled(databaseRadioButton.isSelected());
            }
        });
    }

    private void recurseDirectory(File path, LinkedList<File> list) {
        if (!path.isDirectory()) {
            list.add(path);
            debugUtil.debug("Adding file: " + path.getAbsolutePath(), debug);
        }
        else {
            File[] subFiles = path.listFiles();
            for (File file : subFiles) {
                debugUtil.debug("Descending into directory: " + file.getAbsolutePath(), debug);
                recurseDirectory(file, list);
            }
        }
    }

    public static void main(String[] args) {
        new ColorMain().setVisible(true);
    }
}
