/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.packets.DM20MonitorPerformanceRatioPacket;
import org.etools.j1939tools.j1939.packets.DM21DiagnosticReadinessPacket;
import org.etools.j1939tools.j1939.packets.DM26TripDiagnosticReadinessPacket;
import org.etools.j1939tools.j1939.packets.DM5DiagnosticReadinessPacket;
import org.etools.j1939tools.j1939.packets.MonitoredSystem;
import org.etools.j1939tools.j1939.packets.ParsedPacket;
import org.etools.j1939tools.j1939.packets.PerformanceRatio;

import net.soliddesign.iumpr.IUMPR;
import net.soliddesign.iumpr.modules.ReportFileModule;
import net.soliddesign.iumpr.resources.Resources;
import net.soliddesign.iumpr.ui.IUserInterfaceController;
import net.soliddesign.iumpr.ui.SwingExecutor;

/**
 * Displays the relative information while monitoring the completion status of
 * tests
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class StatusView extends JFrame {

    private static final long serialVersionUID = -3249069779087332761L;

    private static final int TIMEOUT_PERIOD = 10; // MILLISECONDS

    /**
     * The {@link JSplitPane} that's in the bottom half of the topSplitPane
     */
    private JSplitPane bottomSplitPane;

    /**
     * Contains the information about the communication status
     */
    private JPanel commPanel;

    /**
     * The main {@link JPanel} for this class
     */
    private JPanel contentPane;

    /**
     * Contains the controls for the view
     */
    private JPanel controlsPanel;

    /**
     * Displays the DM20 communication status
     */
    private JProgressBar dm20ProgressBar;

    /**
     * Displays the DM21 communication status
     */
    private JProgressBar dm21ProgressBar;

    /**
     * Displays the DM26 communication status
     */
    private JProgressBar dm26ProgressBar;

    /**
     * Displays the Dm5 communication status
     */
    private JProgressBar dm5ProgressBar;

    /**
     * Executor used to read messages and updates communication status
     */
    private ScheduledExecutorService executor;

    /**
     * The {@link JTable} that displays general information
     */
    private InfoTable infoTable;

    /**
     * The {@link JScrollPane} for the infoTable
     */
    private JScrollPane infoTableScrollPane;

    /**
     * The {@link J1939} for communications with the vehicle
     */
    private J1939 j1939;

    /**
     * The {@link ScheduledFuture} for the bus monitor task
     */
    private ScheduledFuture<?> monitorFuture;

    /**
     * The {@link JTable} that displays the {@link MonitoredSystem} s
     */
    private MonitorsTable monitorsTable;

    /**
     * The {@link JScrollPane} for the monitorsTable
     */
    private JScrollPane monitorsTableScrollPane;

    /**
     * The {@link JTable} that displays the {@link PerformanceRatio} s
     */
    private RatiosTable ratiosTable;

    /**
     * The {@link JScrollPane} for the ratiosTable
     */
    private JScrollPane ratiosTableScrollPane;

    /**
     * The button used to stop the monitoring and close the frame
     */
    private JButton stopButton;

    private final SwingExecutor swingExecutor;

    /**
     * The {@link ScheduledFuture} from the timeout task
     */
    private ScheduledFuture<?> timeoutFuture;

    /**
     * The {@link JSplitPane} that contains the infoTable and bottomSplitPane
     */
    private JSplitPane topSplitPane;

    /**
     * The {@link IUserInterfaceController}
     */
    private final IUserInterfaceController uiController;

    /**
     * Create the View.
     *
     * @param uiController
     *            the {@link IUserInterfaceController}
     */
    public StatusView(IUserInterfaceController uiController) {
        this(uiController, Executors.newScheduledThreadPool(2), new SwingExecutor());
    }

    /**
     * Constructor exposed for testing
     *
     * @param uiController
     *            the {@link IUserInterfaceController}
     * @param executor
     *            the {@link ScheduledExecutorService} for periodic tasks
     * @param swingExecutor
     *            the {@link SwingExecutor} that will make changes to the UI on
     *            the Swing Thread
     */
    public StatusView(IUserInterfaceController uiController, ScheduledExecutorService executor,
            SwingExecutor swingExecutor) {
        this.uiController = uiController;
        this.executor = executor;
        this.swingExecutor = swingExecutor;

        // Require the user to push the "stop" button to close this view
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setBounds(50, 50, 900, 550);
        setIconImage(Resources.getLogoImage());

        // Center the frame on the screen
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                StatusView.this.dispose();
            }
        });
    }

    /**
     * Decrements the value of the given progress bar
     *
     * @param progressBar
     *            the {@link JProgressBar} to decrement
     */
    private void decrement(JProgressBar progressBar) {
        int value = progressBar.getValue();
        if (value > progressBar.getMinimum()) {
            progressBar.setValue(value - TIMEOUT_PERIOD);
        } else {
            progressBar.setString(progressBar.getName() + " Timeout");
        }
    }

    /**
     * De-initializes this so it can be reconstructed
     */
    private void deinitialize() {
        bottomSplitPane = null;
        commPanel = null;
        contentPane = null;
        controlsPanel = null;
        dm20ProgressBar = null;
        dm26ProgressBar = null;
        dm5ProgressBar = null;
        infoTable = null;
        infoTableScrollPane = null;
        monitorsTable = null;
        monitorsTableScrollPane = null;
        ratiosTable = null;
        ratiosTableScrollPane = null;
        stopButton = null;
        topSplitPane = null;
    }

    @Override
    public void dispose() {
        stop();
        deinitialize();
        uiController.onStatusViewClosed();
        super.dispose();
    }

    private JSplitPane getBottomSplitPane() {
        if (bottomSplitPane == null) {
            bottomSplitPane = new JSplitPane();
            bottomSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            bottomSplitPane.setTopComponent(getRatiosTableScrollPane());
            bottomSplitPane.setBottomComponent(getMonitorsTableScrollPane());
            bottomSplitPane.setResizeWeight(0.45);
            bottomSplitPane.setBorder(BorderFactory.createEmptyBorder());
        }
        return bottomSplitPane;
    }

    private JPanel getCommPanel() {
        if (commPanel == null) {
            commPanel = new JPanel();
            commPanel.setBorder(new LineBorder(new Color(0, 0, 0)));

            GridBagLayout gbl_commsPanel = new GridBagLayout();
            gbl_commsPanel.columnWidths = new int[] { 0 };
            gbl_commsPanel.rowHeights = new int[] { 0, 0, 0, 0 };
            gbl_commsPanel.columnWeights = new double[] { 1.0 };
            gbl_commsPanel.rowWeights = new double[] { 1, 1, 1, 1 };
            commPanel.setLayout(gbl_commsPanel);

            GridBagConstraints gbcDm5ProgressBar = new GridBagConstraints();
            gbcDm5ProgressBar.insets = new Insets(5, 5, 5, 5);
            gbcDm5ProgressBar.gridx = 0;
            gbcDm5ProgressBar.gridy = 0;
            gbcDm5ProgressBar.fill = GridBagConstraints.BOTH;
            commPanel.add(getDm5ProgressBar(), gbcDm5ProgressBar);

            GridBagConstraints gbcDm20ProgressBar = new GridBagConstraints();
            gbcDm20ProgressBar.insets = new Insets(0, 5, 5, 5);
            gbcDm20ProgressBar.gridx = 0;
            gbcDm20ProgressBar.gridy = 1;
            gbcDm20ProgressBar.fill = GridBagConstraints.BOTH;
            commPanel.add(getDm20ProgressBar(), gbcDm20ProgressBar);

            GridBagConstraints gbcDm26ProgressBar = new GridBagConstraints();
            gbcDm26ProgressBar.insets = new Insets(0, 5, 5, 5);
            gbcDm26ProgressBar.gridx = 0;
            gbcDm26ProgressBar.gridy = 2;
            gbcDm26ProgressBar.fill = GridBagConstraints.BOTH;
            commPanel.add(getDm26ProgressBar(), gbcDm26ProgressBar);

            GridBagConstraints gbcDm21ProgressBar = new GridBagConstraints();
            gbcDm21ProgressBar.insets = new Insets(0, 5, 5, 5);
            gbcDm21ProgressBar.gridx = 0;
            gbcDm21ProgressBar.gridy = 3;
            gbcDm21ProgressBar.fill = GridBagConstraints.BOTH;
            commPanel.add(getDm21ProgressBar(), gbcDm21ProgressBar);
        }
        return commPanel;
    }

    private JPanel getContentPanel() {
        if (contentPane == null) {
            contentPane = new JPanel();
            contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

            GridBagLayout gbl_contentPane = new GridBagLayout();
            gbl_contentPane.columnWidths = new int[] { 0 };
            gbl_contentPane.rowHeights = new int[] { 0, 0, };
            gbl_contentPane.columnWeights = new double[] { 1.0 };
            gbl_contentPane.rowWeights = new double[] { 1, 0 };
            contentPane.setLayout(gbl_contentPane);

            GridBagConstraints gbc_splitPane = new GridBagConstraints();
            gbc_splitPane.insets = new Insets(5, 5, 5, 5);
            gbc_splitPane.fill = GridBagConstraints.BOTH;
            gbc_splitPane.gridx = 0;
            gbc_splitPane.gridy = 0;
            contentPane.add(getTopSplitPane(), gbc_splitPane);

            GridBagConstraints gbc_controlsPanel = new GridBagConstraints();
            gbc_controlsPanel.insets = new Insets(0, 5, 5, 5);
            gbc_controlsPanel.fill = GridBagConstraints.BOTH;
            gbc_controlsPanel.gridx = 0;
            gbc_controlsPanel.gridy = 1;
            contentPane.add(getControlsPanel(), gbc_controlsPanel);
        }
        return contentPane;
    }

    private JPanel getControlsPanel() {
        if (controlsPanel == null) {
            controlsPanel = new JPanel();
            controlsPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
            GridBagLayout gbl_controlsPanel = new GridBagLayout();
            gbl_controlsPanel.columnWidths = new int[] { 0, 0 };
            gbl_controlsPanel.rowHeights = new int[] { 0 };
            gbl_controlsPanel.columnWeights = new double[] { 2.0, 1.0 };
            gbl_controlsPanel.rowWeights = new double[] { Double.MIN_VALUE };
            controlsPanel.setLayout(gbl_controlsPanel);

            GridBagConstraints gbc_stopButton = new GridBagConstraints();
            gbc_stopButton.insets = new Insets(5, 5, 5, 5);
            gbc_stopButton.fill = GridBagConstraints.BOTH;
            gbc_stopButton.gridx = 0;
            gbc_stopButton.gridy = 0;
            controlsPanel.add(getStopButton(), gbc_stopButton);

            GridBagConstraints gbc_commPanel = new GridBagConstraints();
            gbc_commPanel.insets = new Insets(5, 0, 5, 5);
            gbc_commPanel.fill = GridBagConstraints.BOTH;
            gbc_commPanel.gridx = 1;
            gbc_commPanel.gridy = 0;
            controlsPanel.add(getCommPanel(), gbc_commPanel);
        }
        return controlsPanel;
    }

    JProgressBar getDm20ProgressBar() {
        if (dm20ProgressBar == null) {
            dm20ProgressBar = new JProgressBar();
            dm20ProgressBar.setMinimum(0);
            dm20ProgressBar.setMaximum(10000);
            dm20ProgressBar.setValue(0);
            dm20ProgressBar.setToolTipText("The communication status for DM20 messages.");
            dm20ProgressBar.setString("DM20");
            dm20ProgressBar.setStringPainted(true);
            dm20ProgressBar.setName("DM20");
        }
        return dm20ProgressBar;
    }

    JProgressBar getDm21ProgressBar() {
        if (dm21ProgressBar == null) {
            dm21ProgressBar = new JProgressBar();
            dm21ProgressBar.setMinimum(0);
            dm21ProgressBar.setMaximum(10000);
            dm21ProgressBar.setValue(0);
            dm21ProgressBar.setToolTipText("The communication status for DM21 messages.");
            dm21ProgressBar.setString("DM21");
            dm21ProgressBar.setStringPainted(true);
            dm21ProgressBar.setName("DM21");
        }
        return dm21ProgressBar;
    }

    JProgressBar getDm26ProgressBar() {
        if (dm26ProgressBar == null) {
            dm26ProgressBar = new JProgressBar();
            dm26ProgressBar.setMinimum(0);
            dm26ProgressBar.setMaximum(10000);
            dm26ProgressBar.setValue(0);
            dm26ProgressBar.setToolTipText("The communication status for DM26 messages.");
            dm26ProgressBar.setString("DM26");
            dm26ProgressBar.setStringPainted(true);
            dm26ProgressBar.setName("DM26");
        }
        return dm26ProgressBar;
    }

    JProgressBar getDm5ProgressBar() {
        if (dm5ProgressBar == null) {
            dm5ProgressBar = new JProgressBar();
            dm5ProgressBar.setMinimum(0);
            dm5ProgressBar.setMaximum(10000);
            dm5ProgressBar.setValue(0);
            dm5ProgressBar.setToolTipText("The communication status for DM5 messages.");
            dm5ProgressBar.setString("DM5");
            dm5ProgressBar.setStringPainted(true);
            dm5ProgressBar.setName("DM5");
        }
        return dm5ProgressBar;
    }

    private InfoTable getInfoTable() {
        if (infoTable == null) {
            infoTable = new InfoTable();
        }
        return infoTable;
    }

    private JScrollPane getInfoTableScrollPane() {
        if (infoTableScrollPane == null) {
            infoTableScrollPane = new JScrollPane();
            infoTableScrollPane.setViewportView(getInfoTable());
        }
        return infoTableScrollPane;
    }

    private Logger getLogger() {
        return IUMPR.getLogger();
    }

    private MonitorsTable getMonitorsTable() {
        if (monitorsTable == null) {
            monitorsTable = new MonitorsTable();
        }
        return monitorsTable;
    }

    private JScrollPane getMonitorsTableScrollPane() {
        if (monitorsTableScrollPane == null) {
            monitorsTableScrollPane = new JScrollPane();
            monitorsTableScrollPane.setViewportView(getMonitorsTable());
        }
        return monitorsTableScrollPane;
    }

    private RatiosTable getRatiosTable() {
        if (ratiosTable == null) {
            ratiosTable = new RatiosTable();
        }
        return ratiosTable;
    }

    private JScrollPane getRatiosTableScrollPane() {
        if (ratiosTableScrollPane == null) {
            ratiosTableScrollPane = new JScrollPane();
            ratiosTableScrollPane.setViewportView(getRatiosTable());
        }
        return ratiosTableScrollPane;
    }

    JButton getStopButton() {
        if (stopButton == null) {
            stopButton = new JButton("Stop Tracking Monitor Completion Status");
            stopButton.addActionListener(e -> StatusView.this.dispose());
        }
        return stopButton;
    }

    private JSplitPane getTopSplitPane() {
        if (topSplitPane == null) {
            topSplitPane = new JSplitPane();
            topSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            topSplitPane.setTopComponent(getInfoTableScrollPane());
            topSplitPane.setBottomComponent(getBottomSplitPane());
            topSplitPane.setResizeWeight(0.22);
            topSplitPane.setBorder(BorderFactory.createEmptyBorder());
        }
        return topSplitPane;
    }

    /**
     * @wbp.parser.entryPoint
     */
    private void initialize() {
        setContentPane(getContentPanel());
        revalidate();
    }

    private void processPacket(ParsedPacket packet) {
        swingExecutor.execute(() -> {
            try {
                if (packet instanceof DM5DiagnosticReadinessPacket) {
                    reset(getDm5ProgressBar());
                } else if (packet instanceof DM20MonitorPerformanceRatioPacket) {
                    reset(getDm20ProgressBar());
                } else if (packet instanceof DM26TripDiagnosticReadinessPacket) {
                    reset(getDm26ProgressBar());
                } else if (packet instanceof DM21DiagnosticReadinessPacket) {
                    reset(getDm21ProgressBar());
                }
                getRatiosTable().process(packet);
                getMonitorsTable().process(packet);
                getInfoTable().process(packet);
            } catch (Exception e) {
                // Shouldn't happen, but log in case it does
                getLogger().log(Level.SEVERE, "Error Reading Packets", e);
            }
        });
    }

    /**
     * Resets the given progress bar to maximum value
     *
     * @param progressBar
     *            the {@link JProgressBar} to reset
     */
    private void reset(JProgressBar progressBar) {
        progressBar.setValue(progressBar.getMaximum());
        progressBar.setString(progressBar.getName());
    }

    /**
     * Sets the {@link ReportFileModule}
     *
     * @param reportFileModule
     *            the {@link ReportFileModule} to set
     */
    public void setReportFileModule(ReportFileModule reportFileModule) {
        getRatiosTable().setReportFileModule(reportFileModule);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            initialize();
            start();
        } else {
            stop();
            deinitialize();
        }
        super.setVisible(b);
    }

    private void start() {
        setTitle("Tracking Monitor Completion Status for " + uiController.getVin());
        j1939 = uiController.getNewJ1939();
        startTimeoutTimer();
        startBusMonitor();
    }

    /**
     * Starts the task that monitors the bus
     */
    private void startBusMonitor() {
        monitorFuture = executor.schedule(() -> {
            try {
                j1939.read()
                        .filter(t -> !monitorFuture.isCancelled())
                        .flatMap(p -> p.left.stream())
                        .forEach(this::processPacket);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error Reading Packets", e);
            }
        }, 1, TimeUnit.MILLISECONDS);
    }

    /**
     * Starts the timer that will decrement the progress bars to illustrate the
     * time elapsed since the last time the message was received
     */
    private void startTimeoutTimer() {
        timeoutFuture = executor.scheduleWithFixedDelay(() -> {
            swingExecutor.execute(() -> {
                decrement(getDm5ProgressBar());
                decrement(getDm20ProgressBar());
                decrement(getDm26ProgressBar());
                decrement(getDm21ProgressBar());
            });
        }, TIMEOUT_PERIOD, TIMEOUT_PERIOD, TimeUnit.MILLISECONDS);
    }

    private void stop() {
        if (monitorFuture != null) {
            monitorFuture.cancel(true);
        }
        if (timeoutFuture != null) {
            timeoutFuture.cancel(true);
        }
        j1939 = null;
    }
}
