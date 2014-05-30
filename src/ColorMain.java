import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
    private JTextField singleFileField;
    private JButton singleFileBrowse;
    private JTextField dumpOutputField;
    private JButton dumpOutputBrowse;
    private JRadioButton singleFileRadioButton;
    private JRadioButton directoryRadioButton;
    private JPanel contentPane;
    private JFileChooser chooser;

    public ColorMain() {
        setContentPane(contentPane);
        setSize(contentPane.getPreferredSize());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        hueSpinner.setValue(hueSlider.getValue());
        satSpinner.setValue(satSlider.getValue());
        valueSpinner.setValue(valueSlider.getValue());
        singleFileBrowse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooser = new JFileChooser();
                int retval = chooser.showOpenDialog(null);
                if (retval == JFileChooser.APPROVE_OPTION) {
                    singleFileField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
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
                LinkedList<File> files = new LinkedList<>();
                if (singleFileRadioButton.isSelected()) {
                    files.add(new File(singleFileField.getText()));
                }
                else {
                    recurseDirectory(new File(directoryField.getText()), files);
                }
                RepColors col = new RepColors();
                ImageProc proc = new ImageProc("localhost", "VTMaster", "testuser", "test", "Textile_img", col.debug);
                col.refColors = proc.loadRefColors("Color_detail");

                //Process all files in the list
                for (File file : files) {
                    LinkedList<ColorCount> colors = col.processImage(ImageProc.readImageFromFile(file.getAbsolutePath()));

                    if (directInsertRadioButton.isSelected()) {
                        col.writeColors(proc, 10, colors);
                    }
                    else {

                        System.out.println("[WARN] Not implemented.");
                    }
                }
            }
        });
    }

    private void recurseDirectory(File path, LinkedList<File> list) {
        if (!path.isDirectory()) {
            list.add(path);
            System.out.println("[DEBUG] Adding file: " + path.getAbsolutePath());
        }
        else {
            File[] subFiles = path.listFiles();
            for (File file : subFiles) {
                System.out.println("[DEBUG] Descending into directory: " + file.getAbsolutePath());
                recurseDirectory(file, list);
            }
        }
    }

    public static void main(String[] args) {
        new ColorMain().setVisible(true);
    }
}
