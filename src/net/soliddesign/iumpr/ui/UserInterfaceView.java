/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;

import org.etools.j1939tools.bus.Adapter;

import net.soliddesign.iumpr.BuildNumber;
import net.soliddesign.iumpr.IUMPR;
import net.soliddesign.iumpr.resources.Resources;
import net.soliddesign.iumpr.ui.status.StatusView;

/**
 * The View for the User Interface.
 *
 * NOTE: Logic is not present in the class as it will be difficult to unit test.
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class UserInterfaceView implements IUserInterfaceView {

    private static final String SELECT_FILE = "Select File...";

    static {
        // scale the fonts for JOptionPanes
        float FONT_SCALE = 2.0f;
        Font font = new JOptionPane().getFont();
        font = font.deriveFont(font.getSize() * FONT_SCALE);
        UIManager.put("OptionPane.messageFont", font);
        UIManager.put("OptionPane.buttonFont", font);
    }

    static public void scaleFont(Component c, double scale) {
        Font font = c.getFont();
        if (font != null) {
            c.setFont(font.deriveFont((float) (font.getSize() * scale)));
        }
        if (c instanceof Container) {
            for (var d : ((Container) c).getComponents()) {
                scaleFont(d, scale);
            }
        }
    }

    private JButton abortButton;

    private JComboBox<Adapter> adapterComboBox;

    private JLabel adapterLabel;

    private final BuildNumber buildNumber;

    private JLabel calsLabel;

    private JScrollPane calsScrollPane;

    private JTextArea calsTextField;

    private JButton collectTestResultsButton;

    private JComboBox<String> connectionStringComboBox;

    /**
     * The controller for the behavior of this view
     */
    private final IUserInterfaceController controller;

    private JFileChooser fileChooser;

    private JLabel fileLabel;

    private JFrame frame;

    private JButton generateDataPlateButton;

    private JButton helpButton;

    private JButton monitorCompletionButton;

    private JProgressBar progressBar;

    private JButton readVehicleInfoButton;

    private JPanel reportControlPanel;

    private JScrollPane reportScrollPane;

    private JPanel reportSetupPanel;

    private JTextArea reportTextArea;

    private JButton selectFileButton;

    private JSplitPane splitPane;

    private final StatusView statusView;

    private final SwingExecutor swingExecutor;

    private JPanel topPanel;

    private JPanel vehicleInfoPanel;

    private JLabel vinLabel;

    private JTextField vinTextField;

    /**
     * Default Constructor
     *
     * @wbp.parser.entryPoint
     */
    public UserInterfaceView() {
        swingExecutor = new SwingExecutor();
        controller = new UserInterfaceController(this);
        buildNumber = new BuildNumber();
        statusView = new StatusView(controller);
        initialize();
    }

    /**
     * Constructor exposed for testing
     *
     * @param contoller
     *            The {@link UserInterfaceController} that will control the UI
     * @param buildNumber
     *            The {@link BuildNumber} that will return the build number
     * @param statusView
     *            The {@link StatusView} that will monitor the test completion
     *            status
     * @param swingExecutor
     *            The {@link SwingExecutor} used to make updates to the UI on
     *            the Swing Thread
     */
    UserInterfaceView(IUserInterfaceController contoller, BuildNumber buildNumber, StatusView statusView,
            SwingExecutor swingExecutor) {
        controller = contoller;
        this.buildNumber = buildNumber;
        this.statusView = statusView;
        this.swingExecutor = swingExecutor;
        initialize();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceView#appendResults(java.lang.
     * String)
     */
    @Override
    public void appendResults(String result) {
        refreshUI(() -> getReportTextArea().append(result));
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceView#displayDialog(java.lang.
     * String, java.lang.String, int)
     */
    @Override
    public void displayDialog(String message, String title, int type, boolean modal) {
        if (modal) {
            try {
                SwingUtilities.invokeAndWait(() -> JOptionPane.showConfirmDialog(getFrame(), message, title,
                        JOptionPane.OK_CANCEL_OPTION, type));
            } catch (InvocationTargetException | InterruptedException e) {
                IUMPR.getLogger().log(Level.SEVERE, "Error displaying dialog", e);
            }
        } else {
            refreshUI(() -> JOptionPane.showMessageDialog(getFrame(), message, title, type));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceView#displayFileChooser()
     */
    @Override
    public void displayFileChooser() {
        refreshUI(() -> {
            int result = getFileChooser().showDialog(getFrame(), "Use File");
            if (result == JFileChooser.APPROVE_OPTION) {
                getController().onFileChosen(getFileChooser().getSelectedFile());
            }
        });
    }

    /**
     * Creates, caches and returns the Adapter Combo Box Selector
     *
     * @return JComboBox
     */
    JComboBox<Adapter> getAdapterComboBox() {
        if (adapterComboBox == null) {
            adapterComboBox = new JComboBox<>(getController().getAdapters().toArray(Adapter[]::new));
            adapterComboBox.setToolTipText("RP1210 Communications Adapter");
            adapterComboBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Adapter adapter = (Adapter) e.getItem();
                    getController().onAdapterComboBoxItemSelected(adapter);
                    getConnectionStringCombo().removeAllItems();
                    String defConnectionString = Preferences.userNodeForPackage(getClass()).get("connection string",
                            null);
                    for (String s : adapter.getConnectionStrings()) {
                        getConnectionStringCombo().addItem(s);
                    }
                    getConnectionStringCombo().setSelectedItem(defConnectionString);

                    Preferences.userNodeForPackage(getClass()).put("adapter", adapter.getName());
                }
            });

            adapterComboBox.setSelectedIndex(-1); // default to no selection
            String defAdapter = Preferences.userNodeForPackage(getClass()).get("adapter", null);
            getController().getAdapters().stream()
                    .filter(a -> a.getName().equals(defAdapter)).findFirst()
                    .ifPresent(a -> adapterComboBox.setSelectedItem(a));
        }
        return adapterComboBox;
    }

    /**
     * Creates, caches and returns the label for the Adapter Combo Box Selector
     *
     * @return JLabel
     */
    private JLabel getAdapterLabel() {
        if (adapterLabel == null) {
            adapterLabel = new JLabel("Vehicle Adapter:");
        }
        return adapterLabel;
    }

    /**
     * Returns the {@link BuildNumber} that reads the version number
     *
     * @return the instance of the {@link BuildNumber}
     */
    private BuildNumber getBuildNumber() {
        return buildNumber;
    }

    /**
     * Creates, caches and returns the label for the Engine Calibrations Text
     * Field
     *
     * @return JLabel
     */
    private JLabel getCalsLabel() {
        if (calsLabel == null) {
            calsLabel = new JLabel("<html>Cal<br>IDs:</html>", SwingConstants.CENTER);
            calsLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            calsLabel.setToolTipText("Engine Calibrations");
        }
        return calsLabel;
    }

    /**
     * Creates, caches and returns the scroll pane that contains the Report Text
     * Area
     *
     * @return JScrollPane
     */
    private JScrollPane getCalsScrollPane() {
        if (calsScrollPane == null) {
            calsScrollPane = new JScrollPane(getCalsTextField());
            calsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            calsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Dimension dimension = new Dimension(200, 100);
            calsScrollPane.setPreferredSize(dimension);
            calsScrollPane.setMinimumSize(dimension);
        }
        return calsScrollPane;
    }

    /**
     * Creates, caches and returns the Engine Calibrations Text Field
     *
     * @return JTextArea
     */
    JTextArea getCalsTextField() {
        if (calsTextField == null) {
            calsTextField = new JTextArea();
            calsTextField.setEditable(false);
            calsTextField.setToolTipText("Engine Calibrations");
        }
        return calsTextField;
    }

    /**
     * Creates, caches and returns the Collect Test Results Button
     *
     * @return JButton
     */
    JButton getCollectTestResultsButton() {
        if (collectTestResultsButton == null) {
            collectTestResultsButton = new JButton("Collect Tests Results");
            collectTestResultsButton.setEnabled(false);
            collectTestResultsButton.addActionListener(e -> {
                getController().onCollectTestResultsButtonClicked();
            });
        }
        return collectTestResultsButton;
    }

    private JComboBox<String> getConnectionStringCombo() {
        if (connectionStringComboBox == null) {
            connectionStringComboBox = new JComboBox<>();
            connectionStringComboBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    getController().onAdapterConnectionString((String) e.getItem());
                    Preferences.userNodeForPackage(getClass()).put("connection string", (String) e.getItem());
                }
            });
        }
        return connectionStringComboBox;
    }

    /**
     * Returns the {@link UserInterfaceController} that controls the view
     *
     * @return the {@link UserInterfaceController}
     */
    private IUserInterfaceController getController() {
        return controller;
    }

    /**
     * Creates, caches and returns the File Chooser
     *
     * @return JFileChooser
     */
    JFileChooser getFileChooser() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            final FileNameExtensionFilter filter = new FileNameExtensionFilter("IUMPR Data Files",
                    UserInterfaceController.FILE_SUFFIX);
            fileChooser.setFileFilter(filter);
            fileChooser.setDialogTitle("Create or Select Report File");

        }
        return fileChooser;
    }

    /**
     * Returns the main frame that contains the user interface
     *
     * @return JFrame
     */
    public JFrame getFrame() {
        if (frame == null) {
            frame = new JFrame();
            frame.setTitle("IUMPR Data Collection Tool v" + getBuildNumber().getVersionNumber());
            frame.setBounds(100, 100, 1600, 1000);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setIconImage(Resources.getLogoImage());
        }
        return frame;
    }

    /**
     * Creates, caches and returns the Generate Data Plate Button
     *
     * @return JButton
     */
    JButton getGenerateDataPlateButton() {
        if (generateDataPlateButton == null) {
            generateDataPlateButton = new JButton("Generate Vehicle Data Plate");
            generateDataPlateButton.setEnabled(false);
            generateDataPlateButton.addActionListener(e -> {
                getController().onGenerateDataPlateButtonClicked();
            });
        }
        return generateDataPlateButton;
    }

    /**
     * Creates, caches and returns the Help Button
     *
     * @return JButton
     */
    JButton getHelpButton() {
        if (helpButton == null) {
            helpButton = new JButton("Help");
            helpButton.addActionListener(e -> {
                getController().onHelpButtonClicked();
            });
        }
        return helpButton;
    }

    /**
     * Creates, caches and returns the Monitor Completion Button
     *
     * @return JButton
     */
    JButton getMonitorCompletionButton() {
        if (monitorCompletionButton == null) {
            monitorCompletionButton = new JButton("Track Monitor Completion Status");
            monitorCompletionButton.setEnabled(false);
            monitorCompletionButton.addActionListener(e -> {
                getController().onMonitorCompletionButtonClicked();
            });
        }
        return monitorCompletionButton;
    }

    /**
     * Creates, caches and returns the Progress Bar
     *
     * @return JProgressBar
     */
    JProgressBar getProgressBar() {
        if (progressBar == null) {
            progressBar = new JProgressBar();
            progressBar.setStringPainted(true);
            progressBar.setString("Select Vehicle Adapter");
        }
        return progressBar;
    }

    JButton getReadVehicleInfoButton() {
        if (readVehicleInfoButton == null) {
            readVehicleInfoButton = new JButton(
                    "<html><center>Read</center><center>Vehicle</center><center>Info</center><html>");
            readVehicleInfoButton.setToolTipText("Queries the vehicle for VIN and Calibrations");
            readVehicleInfoButton.setEnabled(false);
            readVehicleInfoButton.setHorizontalAlignment(SwingConstants.CENTER);
            readVehicleInfoButton.setHorizontalTextPosition(SwingConstants.CENTER);
            readVehicleInfoButton.addActionListener(e -> {
                getController().onReadVehicleInfoButtonClicked();
            });
        }
        return readVehicleInfoButton;
    }

    /**
     * Creates, caches and returns the Panel that contains the Report Controls
     *
     * @return JPanel
     */
    private JPanel getReportControlPanel() {
        if (reportControlPanel == null) {
            reportControlPanel = new JPanel();
            reportControlPanel.setBorder(new LineBorder(new Color(0, 0, 0)));

            GridBagLayout layout = new GridBagLayout();
            layout.columnWidths = new int[] { 0, 0 };
            layout.rowHeights = new int[] { 0, 0 };
            layout.columnWeights = new double[] { 1.0, 1.0 };
            layout.rowWeights = new double[] { 1.0, 1.0 };
            reportControlPanel.setLayout(layout);

            GridBagConstraints gbc1 = new GridBagConstraints();
            gbc1.insets = new Insets(5, 5, 5, 5);
            gbc1.fill = GridBagConstraints.BOTH;
            gbc1.gridx = 0;
            gbc1.gridy = 0;
            reportControlPanel.add(getGenerateDataPlateButton(), gbc1);

            GridBagConstraints gbc2 = new GridBagConstraints();
            gbc2.insets = new Insets(5, 0, 5, 5);
            gbc2.fill = GridBagConstraints.BOTH;
            gbc2.gridx = 1;
            gbc2.gridy = 0;
            reportControlPanel.add(getStopButton(), gbc2);

            GridBagConstraints gbc3 = new GridBagConstraints();
            gbc3.insets = new Insets(0, 5, 5, 5);
            gbc3.fill = GridBagConstraints.BOTH;
            gbc3.gridx = 0;
            gbc3.gridy = 1;
            reportControlPanel.add(getMonitorCompletionButton(), gbc3);

            GridBagConstraints gbc4 = new GridBagConstraints();
            gbc4.insets = new Insets(0, 0, 5, 5);
            gbc4.fill = GridBagConstraints.BOTH;
            gbc4.gridx = 1;
            gbc4.gridy = 1;
            reportControlPanel.add(getCollectTestResultsButton(), gbc4);
        }
        return reportControlPanel;
    }

    /**
     * Creates, caches and returns the scroll pane that contains the Report Text
     * Area
     *
     * @return JScrollPane
     */
    private JScrollPane getReportScrollPane() {
        if (reportScrollPane == null) {
            reportScrollPane = new JScrollPane(getReportTextArea());
            reportScrollPane.setMinimumSize(new Dimension(500, 100));
            reportScrollPane.setPreferredSize(new Dimension(500, 100));
            new SmartScroller(reportScrollPane, SmartScroller.VERTICAL, SmartScroller.END);
            reportScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            reportScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }
        return reportScrollPane;
    }

    /**
     * Creates, caches and returns the Panel that contains the controls to setup
     * the report
     *
     * @return JPanel
     */
    private JPanel getReportSetupPanel() {
        if (reportSetupPanel == null) {
            reportSetupPanel = new JPanel();
            reportSetupPanel.setBorder(new LineBorder(new Color(0, 0, 0)));

            GridBagLayout layout = new GridBagLayout();
            layout.columnWidths = new int[] { 0, 0, 0 };
            layout.rowHeights = new int[] { 0, 0 };
            layout.columnWeights = new double[] { 0.0, Double.MIN_VALUE, 0.0 };
            layout.rowWeights = new double[] { 0, 0 };
            reportSetupPanel.setLayout(layout);

            GridBagConstraints adapterLabelGbc = new GridBagConstraints();
            adapterLabelGbc.insets = new Insets(5, 5, 5, 5);
            adapterLabelGbc.anchor = GridBagConstraints.EAST;
            adapterLabelGbc.gridx = 0;
            adapterLabelGbc.gridy = 0;
            reportSetupPanel.add(getAdapterLabel(), adapterLabelGbc);

            GridBagConstraints comboBoxGbc = new GridBagConstraints();
            comboBoxGbc.insets = new Insets(5, 0, 5, 5);
            comboBoxGbc.anchor = GridBagConstraints.WEST;
            comboBoxGbc.gridx = 1;
            comboBoxGbc.gridy = 0;
            JPanel p = new JPanel(new BorderLayout());
            p.add(getAdapterComboBox(), BorderLayout.CENTER);
            p.add(getConnectionStringCombo(), BorderLayout.EAST);
            reportSetupPanel.add(p, comboBoxGbc);

            GridBagConstraints fileLabelGbc = new GridBagConstraints();
            fileLabelGbc.insets = new Insets(0, 5, 5, 5);
            fileLabelGbc.anchor = GridBagConstraints.EAST;
            fileLabelGbc.gridx = 0;
            fileLabelGbc.gridy = 1;
            reportSetupPanel.add(getSelectFileLabel(), fileLabelGbc);

            GridBagConstraints fileButtonGbc = new GridBagConstraints();
            fileButtonGbc.insets = new Insets(0, 0, 5, 5);
            fileButtonGbc.anchor = GridBagConstraints.WEST;
            fileButtonGbc.gridx = 1;
            fileButtonGbc.gridy = 1;
            reportSetupPanel.add(getSelectFileButton(), fileButtonGbc);

            GridBagConstraints helpButtonGbc = new GridBagConstraints();
            helpButtonGbc.insets = new Insets(5, 0, 5, 5);
            helpButtonGbc.anchor = GridBagConstraints.WEST;
            helpButtonGbc.gridx = 2;
            helpButtonGbc.gridy = 0;
            helpButtonGbc.gridheight = 2;
            helpButtonGbc.fill = GridBagConstraints.BOTH;
            reportSetupPanel.add(getHelpButton(), helpButtonGbc);
        }
        return reportSetupPanel;
    }

    /**
     * Creates, caches and returns the Text Area that displays the report
     *
     * @return JTextArea
     */
    JTextArea getReportTextArea() {
        if (reportTextArea == null) {
            reportTextArea = new JTextArea(0, 80);
            reportTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            reportTextArea.setEditable(false);
            reportTextArea.setLineWrap(false);
            DefaultCaret caret = (DefaultCaret) reportTextArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.UPDATE_WHEN_ON_EDT);
        }
        return reportTextArea;
    }

    /**
     * Creates, caches and returns the Select File Button
     *
     * @return JButton
     */
    JButton getSelectFileButton() {
        if (selectFileButton == null) {
            selectFileButton = new JButton(SELECT_FILE);
            selectFileButton.setToolTipText("Select or create the file for the report");
            selectFileButton.addActionListener(event -> {
                getController().onSelectFileButtonClicked();
            });
        }
        return selectFileButton;
    }

    /**
     * Creates, caches and returns the label for the Select File Button
     *
     * @return JLabel
     */
    private JLabel getSelectFileLabel() {
        if (fileLabel == null) {
            fileLabel = new JLabel("Report File:");
        }
        return fileLabel;
    }

    private JSplitPane getSplitPane() {
        if (splitPane == null) {
            splitPane = new JSplitPane();
            splitPane.setContinuousLayout(true);
            splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            splitPane.setTopComponent(getTopPanel());
            splitPane.setBottomComponent(getReportScrollPane());
            splitPane.setBorder(BorderFactory.createEmptyBorder());
        }
        return splitPane;
    }

    /**
     * Creates, caches and returns the Stop Button
     *
     * @return JButton
     */
    JButton getStopButton() {
        if (abortButton == null) {
            abortButton = new JButton("Stop");
            abortButton.setEnabled(false);
            abortButton.addActionListener(e -> {
                getController().onStopButtonClicked();
            });
        }
        return abortButton;
    }

    private JPanel getTopPanel() {
        if (topPanel == null) {
            topPanel = new JPanel();
            topPanel.setBorder(BorderFactory.createEmptyBorder());
            GridBagLayout layout = new GridBagLayout();
            layout.columnWidths = new int[] { 0, 0 };
            layout.rowHeights = new int[] { 0, 0, 0, 0, };
            layout.columnWeights = new double[] { 1.0, 1.0 };
            layout.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE, 0.0 };
            topPanel.setLayout(layout);

            GridBagConstraints reportSetupPanelGbc = new GridBagConstraints();
            reportSetupPanelGbc.insets = new Insets(5, 5, 5, 5);
            reportSetupPanelGbc.fill = GridBagConstraints.BOTH;
            reportSetupPanelGbc.anchor = GridBagConstraints.WEST;
            reportSetupPanelGbc.gridheight = 1;
            reportSetupPanelGbc.gridwidth = 2;
            reportSetupPanelGbc.gridx = 0;
            reportSetupPanelGbc.gridy = 0;
            topPanel.add(getReportSetupPanel(), reportSetupPanelGbc);

            GridBagConstraints vehicleInfoPanelGbc = new GridBagConstraints();
            vehicleInfoPanelGbc.insets = new Insets(0, 5, 5, 5);
            vehicleInfoPanelGbc.fill = GridBagConstraints.BOTH;
            vehicleInfoPanelGbc.anchor = GridBagConstraints.WEST;
            vehicleInfoPanelGbc.gridwidth = 2;
            vehicleInfoPanelGbc.gridx = 0;
            vehicleInfoPanelGbc.gridy = 1;
            vehicleInfoPanelGbc.weighty = 1;
            topPanel.add(getVehicleInfoPanel(), vehicleInfoPanelGbc);

            GridBagConstraints reportControlPanelGbc = new GridBagConstraints();
            reportControlPanelGbc.insets = new Insets(0, 5, 5, 5);
            reportControlPanelGbc.fill = GridBagConstraints.BOTH;
            reportControlPanelGbc.anchor = GridBagConstraints.WEST;
            reportControlPanelGbc.gridheight = 1;
            reportControlPanelGbc.gridwidth = 2;
            reportControlPanelGbc.gridx = 0;
            reportControlPanelGbc.gridy = 2;
            reportControlPanelGbc.weighty = 1;
            topPanel.add(getReportControlPanel(), reportControlPanelGbc);

            GridBagConstraints progressBarGbc = new GridBagConstraints();
            progressBarGbc.insets = new Insets(0, 5, 5, 5);
            progressBarGbc.fill = GridBagConstraints.BOTH;
            progressBarGbc.gridx = 0;
            progressBarGbc.gridy = 3;
            progressBarGbc.gridwidth = 2;
            topPanel.add(getProgressBar(), progressBarGbc);
        }
        return topPanel;
    }

    /**
     * Creates, caches and returns the Panel that contains the information about
     * the vehicle
     *
     * @return JPanel
     */
    private JPanel getVehicleInfoPanel() {
        if (vehicleInfoPanel == null) {
            vehicleInfoPanel = new JPanel();
            vehicleInfoPanel.setBorder(new LineBorder(new Color(0, 0, 0)));

            GridBagLayout panelLayout = new GridBagLayout();
            panelLayout.columnWidths = new int[] { 0, 0, 0 };
            panelLayout.rowHeights = new int[] { 0, 30 };
            panelLayout.columnWeights = new double[] { 0.0, Double.MIN_VALUE, 0.0 };
            panelLayout.rowWeights = new double[] { 0.0, 1.0 };
            vehicleInfoPanel.setLayout(panelLayout);

            GridBagConstraints vinLabelGbc = new GridBagConstraints();
            vinLabelGbc.insets = new Insets(5, 5, 5, 5);
            vinLabelGbc.anchor = GridBagConstraints.EAST;
            vinLabelGbc.gridx = 0;
            vinLabelGbc.gridy = 0;
            vehicleInfoPanel.add(getVinLabel(), vinLabelGbc);

            GridBagConstraints vinTextFieldGbc = new GridBagConstraints();
            vinTextFieldGbc.insets = new Insets(5, 0, 5, 5);
            vinTextFieldGbc.anchor = GridBagConstraints.WEST;
            vinTextFieldGbc.fill = GridBagConstraints.BOTH;
            vinTextFieldGbc.gridx = 1;
            vinTextFieldGbc.gridy = 0;
            vehicleInfoPanel.add(getVinTextField(), vinTextFieldGbc);

            GridBagConstraints calsLabelGbc = new GridBagConstraints();
            calsLabelGbc.insets = new Insets(0, 5, 5, 5);
            calsLabelGbc.fill = GridBagConstraints.BOTH;
            calsLabelGbc.anchor = GridBagConstraints.EAST;
            calsLabelGbc.gridx = 0;
            calsLabelGbc.gridy = 1;
            vehicleInfoPanel.add(getCalsLabel(), calsLabelGbc);

            GridBagConstraints calsTextFieldGbc = new GridBagConstraints();
            calsTextFieldGbc.insets = new Insets(0, 0, 5, 5);
            calsTextFieldGbc.anchor = GridBagConstraints.WEST;
            calsTextFieldGbc.fill = GridBagConstraints.BOTH;
            calsTextFieldGbc.gridx = 1;
            calsTextFieldGbc.gridy = 1;
            vehicleInfoPanel.add(getCalsScrollPane(), calsTextFieldGbc);

            GridBagConstraints readVehicleInfoButtonGbc = new GridBagConstraints();
            readVehicleInfoButtonGbc.insets = new Insets(5, 0, 5, 5);
            readVehicleInfoButtonGbc.fill = GridBagConstraints.BOTH;
            readVehicleInfoButtonGbc.gridx = 2;
            readVehicleInfoButtonGbc.gridy = 0;
            readVehicleInfoButtonGbc.gridheight = 2;
            vehicleInfoPanel.add(getReadVehicleInfoButton(), readVehicleInfoButtonGbc);
        }
        return vehicleInfoPanel;
    }

    /**
     * Creates, caches and returns the label for the Vehicle Identification
     * Number Text Field
     *
     * @return JLabel
     */
    private JLabel getVinLabel() {
        if (vinLabel == null) {
            vinLabel = new JLabel("VIN:");
            vinLabel.setToolTipText("Vehicle Identification Number");
        }
        return vinLabel;
    }

    /**
     * Creates, caches and returns the Vehicle Identification Number Text Field
     *
     * @return JTextFIeld
     */
    JTextField getVinTextField() {
        if (vinTextField == null) {
            vinTextField = new JTextField();
            vinTextField.setToolTipText("Vehicle Identification Number");
            vinTextField.setEditable(false);
        }
        return vinTextField;
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        getFrame().getContentPane().add(getSplitPane());
        getFrame().revalidate();
        getFrame().setLocationRelativeTo(null);
        getFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                getController().disconnect();
            }
        });
        scaleFont(getFrame(), 2.0);
    }

    /**
     * Helper method the runs the given {@link Runnable} in the Swing Thread to
     * update the UI
     *
     * @param runnable
     *            the {@link Runnable} to execute
     */
    private void refreshUI(Runnable runnable) {
        swingExecutor.execute(runnable);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.soliddesign.iumpr.ui.IUserInterfaceView#setAdapterComboBoxEnabled(
     * boolean)
     */
    @Override
    public void setAdapterComboBoxEnabled(boolean enabled) {
        refreshUI(() -> {
            getAdapterComboBox().setEnabled(enabled);
            getConnectionStringCombo().setEnabled(enabled);
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceView#
     * setCollectTestResultsButtonEnabled(boolean)
     */
    @Override
    public void setCollectTestResultsButtonEnabled(boolean enabled) {
        refreshUI(() -> getCollectTestResultsButton().setEnabled(enabled));
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceView#setEngines(java.lang.
     * String)
     */
    @Override
    public void setEngineCals(String text) {
        refreshUI(() -> {
            getCalsTextField().setText(text);
            getCalsTextField().setCaretPosition(0);
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceView#
     * setGenerateDataPlateButtonEnabled(boolean)
     */
    @Override
    public void setGenerateDataPlateButtonEnabled(boolean enabled) {
        refreshUI(() -> getGenerateDataPlateButton().setEnabled(enabled));
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceView#
     * setMonitorCompletionButtonEnabled(boolean)
     */
    @Override
    public void setMonitorCompletionButtonEnabled(boolean enabled) {
        refreshUI(() -> getMonitorCompletionButton().setEnabled(enabled));
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceView#
     * setProgressBarText(String)
     */
    @Override
    public void setProgressBarText(String text) {
        refreshUI(() -> getProgressBar().setString(text));
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceView#setProgressBarValue(int,
     * int, int)
     */
    @Override
    public void setProgressBarValue(int min, int max, int value) {
        refreshUI(() -> {
            getProgressBar().setMinimum(min);
            getProgressBar().setMaximum(max);
            getProgressBar().setValue(value);
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceView#
     * setReadVehicleInfoButtonEnabled(boolean)
     */
    @Override
    public void setReadVehicleInfoButtonEnabled(boolean enabled) {
        refreshUI(() -> getReadVehicleInfoButton().setEnabled(enabled));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.soliddesign.iumpr.ui.IUserInterfaceView#setSelectFileButtonEnabled(
     * boolean)
     */
    @Override
    public void setSelectFileButtonEnabled(boolean enabled) {
        refreshUI(() -> getSelectFileButton().setEnabled(enabled));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.soliddesign.iumpr.ui.IUserInterfaceView#setSelectFileButtonText(java.
     * lang.String)
     */
    @Override
    public void setSelectFileButtonText(String text) {
        refreshUI(() -> getSelectFileButton().setText(text == null ? SELECT_FILE : text));
    }

    @Override
    public void setStatusViewVisible(boolean visible) {
        refreshUI(() -> {
            statusView.setVisible(visible);
            if (visible) {
                statusView.setReportFileModule(getController().getReportFileModule());
            }
        });
    }

    @Override
    public void setStopButtonEnabled(boolean enabled) {
        refreshUI(() -> getStopButton().setEnabled(enabled));
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceView#setVin(java.lang.String)
     */
    @Override
    public void setVin(String vin) {
        refreshUI(() -> getVinTextField().setText(vin));
    }
}
