/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui;

import static net.soliddesign.iumpr.IUMPR.NL;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import net.soliddesign.iumpr.IUMPR;
import net.soliddesign.iumpr.bus.Adapter;
import net.soliddesign.iumpr.bus.Bus;
import net.soliddesign.iumpr.bus.BusException;
import net.soliddesign.iumpr.bus.RP1210;
import net.soliddesign.iumpr.bus.RP1210Bus;
import net.soliddesign.iumpr.bus.j1939.J1939;
import net.soliddesign.iumpr.controllers.CollectResultsController;
import net.soliddesign.iumpr.controllers.Controller;
import net.soliddesign.iumpr.controllers.DataPlateController;
import net.soliddesign.iumpr.controllers.MonitorCompletionController;
import net.soliddesign.iumpr.controllers.ResultsListener;
import net.soliddesign.iumpr.modules.ComparisonModule;
import net.soliddesign.iumpr.modules.ReportFileModule;
import net.soliddesign.iumpr.ui.help.HelpView;

/**
 * The Class that controls the behavior of the {@link UserInterfaceView}
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class UserInterfaceController implements IUserInterfaceController {

    /**
     * The default extension for report files created by this application
     */
    static final String FILE_SUFFIX = "iumpr";

    /**
     * The {@link Controller} that is currently executing
     */
    private Controller activeController;

    /**
     * The possible {@link Adapter} that can be used for communications with the
     * vehicle
     */
    private List<Adapter> adapters;

    private Bus bus;

    private CollectResultsController collectResultsController;

    private ComparisonModule comparisonModule;

    private DataPlateController dataPlateController;

    private final Executor executor;

    private final HelpView helpView;

    private MonitorCompletionController monitorCompletionController;

    private boolean newFile;

    /**
     * The {@link File} where the report is stored
     */
    private File reportFile;

    private final ReportFileModule reportFileModule;

    private final RP1210 rp1210;

    /**
     * The Adapter being used to communicate with the vehicle
     */
    private Adapter selectedAdapter;

    /**
     * The {@link IUserInterfaceView} that is being controlled
     */
    private final IUserInterfaceView view;

    private String vin;

    /**
     * Default Constructor
     *
     * @param view
     *            The {@link UserInterfaceView} to control
     */
    public UserInterfaceController(IUserInterfaceView view) {
        this(view, new DataPlateController(), new CollectResultsController(),
                new MonitorCompletionController(), new ComparisonModule(), new RP1210(), new ReportFileModule(),
                Runtime.getRuntime(), Executors.newSingleThreadExecutor(), new HelpView());
    }

    /**
     * Constructor used for testing
     *
     * @param view
     *            The {@link UserInterfaceView} to control
     * @param dataPlateController
     *            the {@link DataPlateController}
     * @param collectResultsController
     *            the {@link CollectResultsController}
     * @param monitorCompletionController
     *            the {@link MonitorCompletionController}
     * @param comparisonModule
     *            the {@link ComparisonModule}
     * @param rp1210
     *            the {@link RP1210}
     * @param reportFileModule
     *            the {@link ReportFileModule}
     * @param runtime
     *            the {@link Runtime}
     * @param executor
     *            the {@link Executor} used to execute {@link Thread} s
     * @param helpView
     *            the {@link HelpView} that will display help for the
     *            application
     */
    public UserInterfaceController(IUserInterfaceView view, DataPlateController dataPlateController,
            CollectResultsController collectResultsController, MonitorCompletionController monitorCompletionController,
            ComparisonModule comparisonModule, RP1210 rp1210, ReportFileModule reportFileModule,
            Runtime runtime, Executor executor, HelpView helpView) {
        this.view = view;
        this.dataPlateController = dataPlateController;
        this.collectResultsController = collectResultsController;
        this.monitorCompletionController = monitorCompletionController;
        this.comparisonModule = comparisonModule;
        this.rp1210 = rp1210;
        this.reportFileModule = reportFileModule;
        this.executor = executor;
        this.helpView = helpView;
        runtime.addShutdownHook(new Thread(() -> reportFileModule.onProgramExit(), "Shutdown Hook Thread"));
    }

    private void checkSetupComplete() {
        getView().setAdapterComboBoxEnabled(true);
        getView().setSelectFileButtonEnabled(true);
        if (getSelectedAdapter() == null) {
            getView().setProgressBarText("Select Vehicle Adapter");
            getView().setSelectFileButtonEnabled(false);
        } else if (getReportFile() == null) {
            getView().setProgressBarText("Select Report File");
        } else {
            getView().setProgressBarText("Push Read Vehicle Info Button");
            getView().setReadVehicleInfoButtonEnabled(true);
        }
    }

    private void collectTestResultsComplete(boolean success) {
        getView().setCollectTestResultsButtonEnabled(true);
        getView().setMonitorCompletionButtonEnabled(true);
        setActiveController(null);
    }

    private void dataPlateReportComplete(boolean success) {
        if (success) {
            getView().setCollectTestResultsButtonEnabled(true);
            getView().setMonitorCompletionButtonEnabled(true);
        } else {
            getView().setReadVehicleInfoButtonEnabled(true);
            getView().setGenerateDataPlateButtonEnabled(true);
        }
        setActiveController(null);
    }

    @Override
    public void disconnect() {
        if (bus != null && bus instanceof RP1210Bus) {
            try {
                ((RP1210Bus) bus).stop();
            } catch (BusException e) {
                getLogger().log(Level.SEVERE, "Unable to disconnect from adapter", e);
            }
        }
    }

    /**
     * Returns the {@link Controller} that is currently executing
     *
     * @return the activeController
     */
    public Controller getActiveController() {
        return activeController;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceController#getAdapters()
     */
    @Override
    public List<Adapter> getAdapters() {
        if (adapters == null) {
            adapters = new ArrayList<>();
            try {
                adapters.addAll(rp1210.getAdapters());
            } catch (Exception e) {
                getView().displayDialog("The List of Communication Adapters could not be loaded.", "Failure",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        return adapters;
    }

    private ComparisonModule getComparisonModule() {
        return comparisonModule;
    }

    private Logger getLogger() {
        return IUMPR.getLogger();
    }

    @Override
    public J1939 getNewJ1939() {
        return new J1939(bus);
    }

    /**
     * Return the Report File
     *
     * @return the reportFile
     */
    File getReportFile() {
        return reportFile;
    }

    /**
     * Returns the {@link ReportFileModule}
     *
     * @return the {@link ReportFileModule}
     */
    @Override
    public ReportFileModule getReportFileModule() {
        return reportFileModule;
    }

    private ResultsListener getResultsListener() {
        return new ResultsListener() {
            @Override
            public void onComplete(boolean success) {
                if (getActiveController() instanceof DataPlateController) {
                    dataPlateReportComplete(success);
                } else if (getActiveController() instanceof CollectResultsController) {
                    collectTestResultsComplete(success);
                } else if (getActiveController() instanceof MonitorCompletionController) {
                    monitorCompletionComplete(success);
                }
            }

            @Override
            public void onProgress(int currentStep, int totalSteps, String message) {
                getView().setProgressBarValue(0, totalSteps, currentStep);
                getView().setProgressBarText(message);
            }

            @Override
            public void onProgress(String message) {
                getView().setProgressBarText(message);
            }

            @Override
            public void onResult(List<String> results) {
                for (String result : results) {
                    getView().appendResults(result + NL);
                }
            }

            @Override
            public void onResult(String result) {
                getView().appendResults(result + NL);
            }

            @Override
            public void onUrgentMessage(String message, String title, int type) {
                // This is executed in a runnable because the user may take a
                // long time to respond
                executor.execute(() -> getView().displayDialog(message, title, type));
            }
        };
    }

    /**
     * Returns the selected Adapter
     *
     * @return the selectedAdapter
     */
    Adapter getSelectedAdapter() {
        return selectedAdapter;
    }

    private IUserInterfaceView getView() {
        return view;
    }

    @Override
    public String getVin() {
        return vin;
    }

    /**
     * @return the newFile
     */
    private boolean isNewFile() {
        return newFile;
    }

    private void monitorCompletionComplete(boolean success) {
        getView().setCollectTestResultsButtonEnabled(true);
        getView().setMonitorCompletionButtonEnabled(true);
        setActiveController(null);
        getView().setStatusViewVisible(false);
    }

    @Override
    public void onAdapterComboBoxItemSelected(String selectedAdapterName) {
        // Connecting to the adapter can take "a while"
        executor.execute(() -> {
            resetView();
            getView().setAdapterComboBoxEnabled(false);
            getView().setSelectFileButtonEnabled(false);
            getView().setProgressBarText("Connecting to Adapter");

            Adapter matchedAdapter = null;
            for (Adapter adapter : getAdapters()) {
                String name = adapter.getName();
                if (name.equals(selectedAdapterName)) {
                    matchedAdapter = adapter;
                    break;
                }
            }
            setSelectedAdapter(matchedAdapter);
            checkSetupComplete();
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceController#
     * onCollectTestResultsButtonClicked()
     */
    @Override
    public void onCollectTestResultsButtonClicked() {
        getView().setCollectTestResultsButtonEnabled(false);
        getView().setMonitorCompletionButtonEnabled(false);
        runController(collectResultsController);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.soliddesign.iumpr.ui.IUserInterfaceController#onFileChosen(java.io.
     * File)
     */
    @Override
    public void onFileChosen(File file) {
        executor.execute(() -> {
            resetView();
            getView().setAdapterComboBoxEnabled(false);
            getView().setSelectFileButtonEnabled(false);
            getView().setProgressBarText("Scanning Report File");

            try {
                File reportFile = setupReportFile(file);
                setReportFile(reportFile);
                getView().setSelectFileButtonText(reportFile.getAbsolutePath());
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Error Reading File", e);
                reportFile = null;

                getView().setSelectFileButtonText(null);
                String message = "File cannot be used.";
                if (e.getMessage() != null) {
                    message += NL + e.getMessage();
                }
                message += NL + "Please select a different file.";

                getView().displayDialog(message, "File Error", JOptionPane.ERROR_MESSAGE);
            }
            checkSetupComplete();
            getView().setAdapterComboBoxEnabled(true);
            getView().setSelectFileButtonEnabled(true);
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceController#
     * onGenerateDataPlateButtonClicked()
     */
    @Override
    public void onGenerateDataPlateButtonClicked() {
        getView().setGenerateDataPlateButtonEnabled(false);
        getView().setReadVehicleInfoButtonEnabled(false);
        getView().setAdapterComboBoxEnabled(false);
        getView().setSelectFileButtonEnabled(false);
        runController(dataPlateController);
    }

    @Override
    public void onHelpButtonClicked() {
        helpView.setVisible(true);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceController#
     * onMonitorCompletionButtonClicked()
     */
    @Override
    public void onMonitorCompletionButtonClicked() {
        getView().setMonitorCompletionButtonEnabled(false);
        getView().setCollectTestResultsButtonEnabled(false);
        getView().setStatusViewVisible(true);
        runController(monitorCompletionController);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceController#
     * onReadVehicleInfoButtonClicked()
     */
    @Override
    public void onReadVehicleInfoButtonClicked() {
        executor.execute(() -> {
            resetView();
            getView().setAdapterComboBoxEnabled(false);
            getView().setSelectFileButtonEnabled(false);
            boolean result = false;

            ResultsListener resultsListener = getResultsListener();
            try {
                resultsListener.onProgress(1, 6, "Reading Vehicle Identification Number");
                vin = getComparisonModule().getVin();
                getView().setVin(vin);

                resultsListener.onProgress(2, 6, "Reading Vehicle Calibrations");
                String cals = getComparisonModule().getCalibrationsAsString();
                getView().setEngineCals(cals);

                result = getComparisonModule().compareFileToVehicle(resultsListener, getReportFileModule(), 2, 6);
            } catch (IOException e) {
                getView().setProgressBarText(e.getMessage());
                getView().displayDialog(e.getMessage(), "Communications Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                if (result) {
                    getView().setProgressBarText("Push Generate Vehicle Data Plate Button");
                }
                getView().setGenerateDataPlateButtonEnabled(result);
                getView().setStopButtonEnabled(result);
                getView().setReadVehicleInfoButtonEnabled(true);
                getView().setAdapterComboBoxEnabled(true);
                getView().setSelectFileButtonEnabled(true);
            }
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see net.soliddesign.iumpr.ui.IUserInterfaceController#
     * onSelectFileButtonClicked()
     */
    @Override
    public void onSelectFileButtonClicked() {
        getView().displayFileChooser();
    }

    @Override
    public void onStatusViewClosed() {
        monitorCompletionController.endTracking();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.soliddesign.iumpr.ui.IUserInterfaceController#onStopButtonClicked()
     */
    @Override
    public void onStopButtonClicked() {
        if (getActiveController() != null && getActiveController().isActive()) {
            getActiveController().stop();
            getView().setStatusViewVisible(false);
        }
    }

    private void resetView() {
        getComparisonModule().reset();
        vin = null;
        getView().setVin("");
        getView().setEngineCals("");
        getView().setGenerateDataPlateButtonEnabled(false);
        getView().setStopButtonEnabled(false);
        getView().setReadVehicleInfoButtonEnabled(false);
    }

    private void runController(Controller controller) {
        setActiveController(controller);
        getActiveController().execute(getResultsListener(), getNewJ1939(), getReportFileModule());
    }

    /**
     * Sets the {@link Controller} actively being executed. This is exposed for
     * testing.
     *
     * @param controller
     *            the active {@link Controller}
     */
    void setActiveController(Controller controller) {
        if (activeController != null && activeController.isActive()) {
            activeController.stop();
        }
        activeController = controller;
    }

    private void setBus(Bus bus) throws BusException {
        this.bus = bus;
        getComparisonModule().setJ1939(getNewJ1939());
    }

    /**
     * @param newFile
     *            the newFile to set
     */
    private void setNewFile(boolean newFile) {
        this.newFile = newFile;
    }

    /**
     * Sets the Report File with no additional logic. This should only be used
     * for testing.
     *
     * @param file
     *            the report file to use
     * @throws IOException
     *             if there is a problem setting the report file
     */
    void setReportFile(File file) throws IOException {
        getReportFileModule().setReportFile(getResultsListener(), file, isNewFile());
        reportFile = file;
    }

    /**
     * Sets the selected adapter. This should only be used for testing.
     *
     * @param selectedAdapter
     *            the selectedAdapter to set
     */
    private void setSelectedAdapter(Adapter selectedAdapter) {
        try {
            Bus bus;
            if (selectedAdapter != null) {
                bus = rp1210.setAdapter(selectedAdapter, 0xF9);
            } else {
                bus = null;
            }
            this.selectedAdapter = selectedAdapter;
            if (bus != null) {
                setBus(bus);
            }
        } catch (BusException e) {
            getLogger().log(Level.SEVERE, "Error Setting Adapter", e);
            getView().displayDialog("Communications could not be established using the selected adapter.",
                    "Communication Failure", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Checks the given {@link File} to determine if it's a valid file for using
     * to store the Report. If it's valid, the report file is returned.
     *
     * @param file
     *            the {@link File} to check
     * @return The file to be used for the report
     * @throws IOException
     *             if the file cannot be used
     */
    private File setupReportFile(File file) throws IOException {
        File reportFile = file;
        if (!file.exists()) {
            setNewFile(true);
            // Append the file extension if the file doesn't have one.
            if (!file.getName().endsWith("." + FILE_SUFFIX)) {
                return setupReportFile(new File(file.getAbsolutePath() + "." + FILE_SUFFIX));
            }
            if (!reportFile.createNewFile()) {
                throw new IOException("File cannot be created");
            }
        } else {
            setNewFile(false);
            boolean writable = false;
            try {
                writable = reportFile.canWrite();
            } catch (SecurityException e) {
                writable = false;
            }
            if (!writable) {
                throw new IOException("File cannot be written");
            }
        }
        return reportFile;
    }
}