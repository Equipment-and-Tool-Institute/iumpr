/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.system;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.soliddesign.iumpr.IUMPR;
import net.soliddesign.iumpr.ui.IUserInterfaceView;

/**
 * Implementation of {@link IUserInterfaceView} used during system testing
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class TestUserInterfaceView implements IUserInterfaceView {

    private boolean adapterEnabled = true;

    public String cals;

    private boolean collectTestResultsEnabled;

    private boolean generateDatePlateEnabled;

    public String lastDialog;

    private final List<String> messages = new ArrayList<>();

    private boolean monitorCompletionEnabled;

    private boolean readVehicleInfoEnabled;

    private final List<String> results = new ArrayList<>();

    private boolean selectFileEnabled;

    public boolean statusViewVisible;

    private boolean stopEnabled;

    public String vin;

    @Override
    public void appendResults(String result) {
        results.add(result);
    }

    @Override
    public void displayDialog(String message, String title, int type, boolean modal) {
        lastDialog = message;
    }

    @Override
    public void displayFileChooser() {
        fail("Not implemented");
    }

    /**
     * Returns a bit or-ed value indicating if various controls are enabled
     *
     * @return int
     */
    public int getEnabled() {
        int result = 0;
        result |= adapterEnabled ? 0b00000001 : 0;
        result |= selectFileEnabled ? 0b00000010 : 0;
        result |= readVehicleInfoEnabled ? 0b00000100 : 0;
        result |= generateDatePlateEnabled ? 0b00001000 : 0;
        result |= stopEnabled ? 0b00010000 : 0;
        result |= monitorCompletionEnabled ? 0b00100000 : 0;
        result |= collectTestResultsEnabled ? 0b01000000 : 0;
        return result;
    }

    public String getMessages() {
        return messages.stream().collect(Collectors.joining(IUMPR.NL));
    }

    public String getResults() {
        return results.stream().filter(l -> !l.contains("File:")).collect(Collectors.joining());
    }

    @Override
    public void setAdapterComboBoxEnabled(boolean enabled) {
        adapterEnabled = enabled;
    }

    @Override
    public void setCollectTestResultsButtonEnabled(boolean enabled) {
        collectTestResultsEnabled = enabled;
    }

    @Override
    public void setEngineCals(String text) {
        cals = text;
    }

    @Override
    public void setGenerateDataPlateButtonEnabled(boolean enabled) {
        generateDatePlateEnabled = enabled;
    }

    @Override
    public void setMonitorCompletionButtonEnabled(boolean enabled) {
        monitorCompletionEnabled = enabled;
    }

    @Override
    public void setProgressBarText(String text) {
        messages.add(text);
    }

    @Override
    public void setProgressBarValue(int min, int max, int value) {

    }

    @Override
    public void setReadVehicleInfoButtonEnabled(boolean enabled) {
        readVehicleInfoEnabled = enabled;
    }

    @Override
    public void setSelectFileButtonEnabled(boolean enabled) {
        selectFileEnabled = enabled;
    }

    @Override
    public void setSelectFileButtonText(String text) {

    }

    @Override
    public void setStatusViewVisible(boolean visible) {
        statusViewVisible = visible;
    }

    @Override
    public void setStopButtonEnabled(boolean enabled) {
        stopEnabled = enabled;
    }

    @Override
    public void setVin(String vin) {
        this.vin = vin;
    }

}