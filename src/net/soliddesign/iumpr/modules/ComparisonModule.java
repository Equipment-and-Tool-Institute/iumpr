/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static net.soliddesign.iumpr.IUMPR.NL;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import net.soliddesign.iumpr.bus.j1939.packets.DM19CalibrationInformationPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM19CalibrationInformationPacket.CalibrationInformation;
import net.soliddesign.iumpr.bus.j1939.packets.DM21DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.VehicleIdentificationPacket;
import net.soliddesign.iumpr.controllers.ResultsListener;

/**
 * {@link FunctionalModule} that compares the report file and vehicle
 * information
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class ComparisonModule extends FunctionalModule {

    /**
     * Helper method to convert a {@link Set} of {@link CalibrationInformation}
     * to a {@link String}
     *
     * @param cals
     *            the {@link Set} of {@link CalibrationInformation}
     * @return {@link String}
     */
    private static String calibrationAsString(Set<CalibrationInformation> cals) {
        return cals.stream().map(t -> t.toString()).collect(Collectors.joining(NL));
    }

    /**
     * The calibrations read from the vehicle
     */
    private Set<CalibrationInformation> calibrations;

    /**
     * The Time Since Code Cleared read from the vehicle
     */
    private Double minutesSinceCodeClear;

    /**
     * The Vehicle Identification Number read from the vehicle
     */
    private String vin;

    public ComparisonModule() {
        super(new DateTimeModule());
    }

    /**
     * Compares the information from the vehicle to the information from the
     * report file
     *
     * @param listener
     *            the {@link ResultsListener} that will be used to display
     *            dialogs to the user
     * @param reportFileModule
     *            the {@link ReportFileModule} that has information about the
     *            report file
     * @param currentStep
     *            the current step in the progress. This value will be
     *            incremented
     * @param maxSteps
     *            the maximum number of steps in the process
     *
     * @return true if the process can continue; false if the differences are
     *         too great to continue
     *
     * @throws IOException
     *             if the information cannot be retrieved from the vehicle
     */
    public boolean compareFileToVehicle(ResultsListener listener, ReportFileModule reportFileModule, int currentStep,
            int maxSteps)
            throws IOException {

        if (reportFileModule.isNewFile()) {
            listener.onProgress(currentStep + 4, maxSteps, "Selected Report File is new - not checked with vehicle.");
            return true;
        }

        listener.onProgress(++currentStep, maxSteps, "Reading VIN from Vehicle");
        if (!Objects.equals(reportFileModule.getFileVin(), getVin())) {
            reportVINMismatch(listener, reportFileModule.getFileVin());
            listener.onProgress(maxSteps, maxSteps, "VIN Mismatch");
            return false;
        }

        listener.onProgress(++currentStep, maxSteps, "Reading Calibrations from Vehicle");
        if (!Objects.equals(reportFileModule.getCalibrations(), getCalibrations())) {
            reportCalibrationMismatch(listener, reportFileModule.getCalibrations());
            listener.onProgress(maxSteps, maxSteps, "Calibration Mismatch");
            return false;
        }

        listener.onProgress(++currentStep, maxSteps, "Reading Time Since Code Cleared from Vehicle");
        double delta = getMinutesSinceCodeClear() - reportFileModule.getMinutesSinceCodeClear();
        if (delta < 0) {
            reportTSCCReset(listener, delta);
        } else if (delta > DiagnosticReadinessModule.TSCC_GAP_LIMIT) {
            reportTSCCGap(listener, delta);
        }

        listener.onProgress(++currentStep, maxSteps, "Selected Report File Matches Connected Vehicle");
        return true;
    }

    /**
     * Queries the vehicle for all {@link CalibrationInformation} from the
     * modules
     *
     * @return a {@link Set} of {@link CalibrationInformation}
     * @throws IOException
     *             if there are no {@link CalibrationInformation} returned
     */
    private Set<CalibrationInformation> getCalibrations() throws IOException {
        if (calibrations == null) {
            calibrations = getJ1939().requestMultiple(DM19CalibrationInformationPacket.class)
                    .flatMap(t -> t.getCalibrationInformation().stream()).collect(Collectors.toSet());
            if (calibrations.isEmpty()) {
                throw new IOException("Timeout Error Reading Calibrations");
            }
        }
        return calibrations;
    }

    /**
     * Queries the vehicle for all {@link CalibrationInformation} from the
     * modules. A {@link String} of the resulting {@link CalibrationInformation}
     * is returned
     *
     * @return {@link String}
     * @throws IOException
     *             if there are no {@link CalibrationInformation} returned
     */
    public String getCalibrationsAsString() throws IOException {
        return calibrationAsString(getCalibrations());
    }

    /**
     * Queries the vehicle to read the last time the DTCs were cleared
     *
     * @return the timeSinceCodeClear the minutes the engine has run since the
     *         DTCs were cleared
     * @throws IOException
     *             if no value is returned from the vehicle
     */
    public double getMinutesSinceCodeClear() throws IOException {
        if (minutesSinceCodeClear == null) {
            try {
                minutesSinceCodeClear = getJ1939().requestMultiple(DM21DiagnosticReadinessPacket.class)
                        .mapToDouble(t -> t.getMinutesSinceDTCsCleared()).max().getAsDouble();
            } catch (NoSuchElementException e) {
                throw new IOException("Timeout Error Reading Time Since Code Cleared");
            }
        }
        return minutesSinceCodeClear;
    }

    /**
     * Queries the vehicle for the VIN.
     *
     * @return the Vehicle Identification Number as a {@link String}
     * @throws IOException
     *             if no value is returned from the vehicle or different VINs
     *             are returned
     */
    public String getVin() throws IOException {
        if (vin == null) {
            Set<String> vins = getJ1939().requestMultiple(VehicleIdentificationPacket.class).map(t -> t.getVin())
                    .collect(Collectors.toSet());
            if (vins.size() == 0) {
                throw new IOException("Timeout Error Reading VIN");
            }
            if (vins.size() > 1) {
                throw new IOException("Different VINs Received");
            }
            vin = vins.stream().findFirst().get();
        }
        return vin;
    }

    /**
     * Helper method to report a calibration mismatch
     *
     * @param listener
     *            the {@link ResultsListener} that will be notified
     * @param fileCals
     *            the {@link CalibrationInformation} from the file
     * @throws IOException
     *             if the calibrations cannot be read from the vehicle
     */
    private void reportCalibrationMismatch(ResultsListener listener, Set<CalibrationInformation> fileCals)
            throws IOException {
        String message = "The selected report file calibrations do not match the vehicle calibrations." + NL + NL +
                "The Report Calibrations:" + NL + calibrationAsString(fileCals) + NL + NL
                + "The Vehicle Calibrations:" + NL + calibrationAsString(getCalibrations());
        listener.onMessage(message, "Calibrations Mismatch", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Helper method to report an excessive time since code cleared gap
     *
     * @param listener
     *            the {@link ResultsListener}that will be notified
     * @param delta
     *            the difference between the current TSCC and the file TSCC
     */
    private void reportTSCCGap(ResultsListener listener, double delta) {
        String message = "The Time Since Code Cleared has an excessive gap of " + (int) delta + " minutes.";
        listener.onMessage(message, "Time SCC Excess Gap Error", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Helper method to report a reset in the time since code cleared
     *
     * @param listener
     *            the {@link ResultsListener}that will be notified
     * @param delta
     *            the difference between the current TSCC and the file TSCC
     */
    private void reportTSCCReset(ResultsListener listener, double delta) {
        String message = "The Time Since Code Cleared was reset. The difference is " + Math.abs((int) delta)
                + " minutes.";
        listener.onMessage(message, "Time SCC Reset Error", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Helper method to report a VIN mismatch
     *
     * @param listener
     *            the {@link ResultsListener}that will be notified
     * @param fileVin
     *            the VIN from the file
     * @throws IOException
     *             if the VIN cannot be read from the vehicle
     */
    private void reportVINMismatch(ResultsListener listener, String fileVin) throws IOException {
        String message = "The VIN found in the selected report file (" + fileVin +
                ") does not match the VIN read from the vehicle (" + getVin() + ").";
        listener.onMessage(message, "VIN Mismatch", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Clears the cached values that have been read from the vehicle
     */
    public void reset() {
        vin = null;
        minutesSinceCodeClear = null;
        calibrations = null;
    }

}
