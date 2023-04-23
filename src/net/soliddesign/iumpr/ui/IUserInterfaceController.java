/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui;

import java.io.File;
import java.util.List;

import org.etools.j1939tools.bus.Adapter;
import org.etools.j1939tools.bus.j1939.J1939;

import net.soliddesign.iumpr.modules.ReportFileModule;

/**
 * The interface for a UI Controller
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public interface IUserInterfaceController {

    /**
     * Disconnects the vehicle communications adapter
     */
    void disconnect();

    /**
     * Returns the Adapters that are available for communicating with the
     * vehicle
     *
     * @return List
     */
    List<Adapter> getAdapters();

    /**
     * Returns a new instance of J1939 for communicating with the vehicle
     *
     * @return {@link J1939}
     */
    J1939 getNewJ1939();

    /**
     * Returns the {@link ReportFileModule} used to verify the report file
     *
     * @return {@link ReportFileModule}
     */
    ReportFileModule getReportFileModule();

    /**
     * Returns the Vehicle Identification Number
     *
     * @return {@link String}
     */
    String getVin();

    /**
     * Called when the user has selected an adapter
     *
     * @param selectedAdapterName
     *            the name of the selected adapter
     */
    void onAdapterComboBoxItemSelected(String selectedAdapterName);

    /**
     * Called when the {@link UserInterfaceView} Collect Tests Results Button is
     * clicked
     */
    void onCollectTestResultsButtonClicked();

    /**
     * Called when the use has selected a report file
     *
     * @param file
     *            the file to use for the report
     */
    void onFileChosen(File file);

    /**
     * Called when the {@link UserInterfaceView} Generate Data Plate Button is
     * clicked
     */
    void onGenerateDataPlateButtonClicked();

    /**
     * Called when the {@link UserInterfaceView} Help Button is clicked
     */
    void onHelpButtonClicked();

    /**
     * Called when the {@link UserInterfaceView} Monitor Completion Button is
     * clicked
     */
    void onMonitorCompletionButtonClicked();

    /**
     * Called when the {@link UserInterfaceView} Read Vehicle Information Button
     * is clicked
     */
    void onReadVehicleInfoButtonClicked();

    /**
     * Called when the Select File Button has been clicked
     */
    void onSelectFileButtonClicked();

    /**
     * Called when the Monitor Completion Status View is closed
     */
    void onStatusViewClosed();

    /**
     * Called when the {@link UserInterfaceView} Stop Button is clicked
     */
    void onStopButtonClicked();

}