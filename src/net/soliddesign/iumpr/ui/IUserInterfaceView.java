/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui;

import javax.swing.JDialog;

/**
 * The interface for the Graphical User Interface
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public interface IUserInterfaceView {

    /**
     * Adds the given result to the report text area
     *
     * @param result
     *            the result to add
     */
    void appendResults(String result);

    /**
     * Displays a {@link JDialog}
     *
     * @param message
     *            the text of the dialog
     * @param title
     *            the title of the dialog
     * @param type
     *            the type of the dialog
     */
    void displayDialog(String message, String title, int type);

    /**
     * Shows the File Chooser
     */
    void displayFileChooser();

    /**
     * Enables or disables the Adapter Selector Combo Box
     *
     * @param enabled
     *            true to enable the control, false to disable the control
     */
    void setAdapterComboBoxEnabled(boolean enabled);

    /**
     * Enables or disables the Collect Tests Results Button
     *
     * @param enabled
     *            true to enable the control, false to disable the control
     */
    void setCollectTestResultsButtonEnabled(boolean enabled);

    /**
     * Sets the text in the Engine Calibrations Field
     *
     * @param text
     *            the text to set
     */
    void setEngineCals(String text);

    /**
     * Enables or disables the Generate Data Plate Button
     *
     * @param enabled
     *            true to enable the control, false to disable the control
     */
    void setGenerateDataPlateButtonEnabled(boolean enabled);

    /**
     * Enables or disables the Monitor Completion Button
     *
     * @param enabled
     *            true to enable the control, false to disable the control
     */
    void setMonitorCompletionButtonEnabled(boolean enabled);

    /**
     * Sets the text that is displayed on the progress bar
     *
     * @param text
     *            the text to display
     */
    void setProgressBarText(String text);

    /**
     * Sets the value on the progress bar
     *
     * @param min
     *            the minimum value of the bar
     * @param max
     *            the maximum value of the bar
     * @param value
     *            the value of the bar
     */
    void setProgressBarValue(int min, int max, int value);

    /**
     * Enables or disables the Read Vehicle Information Button
     *
     * @param enabled
     *            true to enable the control, false to disable the control
     */
    void setReadVehicleInfoButtonEnabled(boolean enabled);

    /**
     * Enables or disables the Select File Button
     *
     * @param enabled
     *            true to enable the control, false to disable the control
     */
    void setSelectFileButtonEnabled(boolean enabled);

    /**
     * Sets the text on the Select File Button
     *
     * @param text
     *            the text to set
     */
    void setSelectFileButtonText(String text);

    /**
     * Shows or hides the Monitor Completion Status View
     *
     * @param visible
     *            true to show; false to hide
     */
    void setStatusViewVisible(boolean visible);

    /**
     * Enables or disables the Stop Button
     *
     * @param enabled
     *            true to enable the control, false to disable the control
     */
    void setStopButtonEnabled(boolean enabled);

    /**
     * Sets the text on the Vehicle Identification Number Field
     *
     * @param vin
     *            the VIN to set
     */
    void setVin(String vin);

}