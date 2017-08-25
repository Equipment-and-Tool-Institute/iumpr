/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static net.soliddesign.iumpr.IUMPR.NL;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.soliddesign.iumpr.IUMPR;
import net.soliddesign.iumpr.bus.Packet;
import net.soliddesign.iumpr.bus.j1939.J1939;
import net.soliddesign.iumpr.bus.j1939.packets.DM19CalibrationInformationPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM19CalibrationInformationPacket.CalibrationInformation;
import net.soliddesign.iumpr.bus.j1939.packets.DM20MonitorPerformanceRatioPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM21DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM5DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem;
import net.soliddesign.iumpr.bus.j1939.packets.PerformanceRatio;
import net.soliddesign.iumpr.bus.j1939.packets.VehicleIdentificationPacket;
import net.soliddesign.iumpr.controllers.ResultsListener;

/**
 * The {@link FunctionalModule} that's responsible for the log file
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class ReportFileModule extends FunctionalModule implements ResultsListener {

    /**
     * The problems that can occur from reading an existing report file
     */
    enum Problem {
        CAL_INCONSISTENT("The calibrations found in the file do not match."),

        CAL_NOT_PRESENT("There is no calibration in the file."),

        DATE_INCONSISTENT("The dates in the file are inconsistent."),

        DATE_NOT_PRESENT("There is no date in the file."),

        DATE_RESET("The date in the file is in the future."),

        MONITORS_NOT_PRESENT("There are no Monitored Systems in the file."),

        RATIOS_NOT_PRESENT("There are no Performance Ratios in the file."),

        TSCC_NOT_PRESENT("There is no Time Since Code Clear in the file."),

        VIN_INCONSISTENT("The VINs in the file do not match."),

        VIN_NOT_PRESENT("There is no VIN in the file.");

        private final String string;

        private Problem(String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    /**
     * Used internally to interrupt file scanning if there's an problem reading
     * the file
     */
    static class ReportFileException extends IOException {
        private static final long serialVersionUID = 7020510052695189808L;
        private final Problem problem;

        private ReportFileException(Problem problem) {
            super(problem.toString());
            this.problem = problem;
        }

        /**
         * The {@link Problem} that caused the exception
         *
         * @return {@link Problem}
         */
        public Problem getProblem() {
            return problem;
        }
    }

    /**
     * The String indicating the end of a Data Collection Log Session
     */
    private static final String COLLECTION_LOG_FOOTER = "Data Collection Log END OF REPORT";

    /**
     * The text that indicates the Data Plate Section is complete
     */
    private static final String DATA_PLATE_SECTION_FOOTER = BannerModule.Type.DATA_PLATE + " "
            + BannerModule.END_OF_REPORT;

    /**
     * The text that indicates there's an excessive TSCC Gap in the file
     */
    private static final String TIME_GAP_MESSAGE = "ERROR Excess Time Since Code Cleared Gap of";

    /**
     * The Calibrations from the file mapped to source address
     */
    private Map<Integer, DM19CalibrationInformationPacket> calMap = new HashMap<>();

    /**
     * The number of Data Collection Log Sessions in the file
     */
    private int collectionLogs;

    /**
     * The number of Excessive Time Gaps found in the file
     */
    private int excessiveTimeGaps;

    /**
     * The initial value for the number of ignition cycles
     */
    private int initialIgnitionCycles;

    /**
     * The {@link MonitoredSystem}s that were first captured in the report file
     */
    private Set<MonitoredSystem> initialMonitors = new HashSet<>();

    /**
     * The time, from the file, the {@link MonitoredSystem}s were initialized
     */
    private LocalDateTime initialMonitorsTime;

    /**
     * The initial value for the Number of OBD Monitoring Conditions Encountered
     */
    private int initialOBDCounts;

    /**
     * The {@link PerformanceRatio} that were first captured in the report file
     */
    private final Set<PerformanceRatio> initialRatios = new HashSet<>();

    /**
     * The time, from the file, the {@link PerformanceRatio}s were initialized
     */
    private LocalDateTime initialRatiosTime;

    private LocalDateTime lastInstant;

    /**
     * Flag indicating if the file is new or existing
     */
    private boolean newFile;

    /**
     * The Number of queries for the session
     */
    private int queries;

    /**
     * Flag to indicate if the {@link PerformanceRatio} and
     * {@link MonitoredSystem}s can be read from the file. This is set to true
     * after the DTCs have been cleared until the end of the first Data Plate
     * Section
     */
    private boolean ratiosAndMonitorsCanBeRead;

    /**
     * The File were the report is being compiled
     */
    private File reportFile;

    /**
     * The number of timeouts found in the report file for this session
     */
    private int timeouts;

    /**
     * The Time Since Code Clear from the file
     */
    private int tscc = Integer.MIN_VALUE;

    /**
     * The Vehicle Identification Number found in an existing report file
     */
    private String vin = null;

    /**
     * The Writer used to write results to the report file
     */
    private BufferedWriter writer;

    /**
     * Constructor
     */
    public ReportFileModule() {
        this(new DateTimeModule());
    }

    /**
     * Constructor exposed for testing
     *
     * @param dateTimeModule
     *            The {@link DateTimeModule}
     */
    public ReportFileModule(DateTimeModule dateTimeModule) {
        super(dateTimeModule);
    }

    /**
     * Checks the line for calibrations
     *
     * @param line
     *            the line to check
     * @return true if the line contained calibrations
     * @throws ReportFileException
     *             if the calibrations were inconsistent with the previous
     *             calibrations
     */
    private boolean checkCals(String line) throws ReportFileException {
        DM19CalibrationInformationPacket dm19 = parseCal(line);
        if (dm19 != null) {
            int source = dm19.getSourceAddress();
            DM19CalibrationInformationPacket existing = calMap.get(source);
            if (existing != null) {
                if (!existing.equals(dm19)) {
                    throw new ReportFileException(Problem.CAL_INCONSISTENT);
                }
            } else {
                calMap.put(source, dm19);
            }
            return true;
        }
        return false;
    }

    /**
     * Checks the line for the date
     *
     * @param line
     *            the line to check
     * @throws ReportFileException
     *             if the date went backwards or is in the future
     */
    private void checkDate(String line) throws ReportFileException {
        LocalDateTime lineInstant = parseDateTime(line);

        if (lineInstant != null) {
            if (lastInstant != null) {
                if (lineInstant.isBefore(lastInstant)) {
                    throw new ReportFileException(Problem.DATE_INCONSISTENT);
                }
                if (lineInstant.isAfter(LocalDateTime.now())) {
                    throw new ReportFileException(Problem.DATE_RESET);
                }
            }
            lastInstant = lineInstant;
        }
    }

    /**
     * Checks the line to determine if it contains information indicating the
     * DTCs were cleared
     *
     * @param line
     *            the line to check
     * @return true if the line indicates the codes were cleared
     */
    private boolean checkDTCsCleared(String line) {
        if (line.contains(DTCModule.DTCS_CLEARED)) {
            ratiosAndMonitorsCanBeRead = true;
            tscc = Integer.MIN_VALUE;
            return true;
        }
        return false;
    }

    /**
     * Checks to see if this is the end of a report section
     *
     * @param result
     *            the result line to read
     * @return true if the line was parsed; false if this was not the end of the
     *         report
     */
    private boolean checkEndOfReport(String result) {
        if (result.contains(DATA_PLATE_SECTION_FOOTER)) {
            if (ratiosAndMonitorsCanBeRead) {
                ratiosAndMonitorsCanBeRead = false;
                // Consolidate the Monitored Systems from various modules into a
                // single set
                initialMonitors = new HashSet<>(DiagnosticReadinessModule.getCompositeSystems(initialMonitors, true));
            }
            return true;
        }
        return false;
    }

    /**
     * Checks to see if the line contains {@link MonitoredSystem}s
     *
     * @param line
     *            the line to check
     * @return true if the line contained {@link MonitoredSystem}s
     */
    private boolean checkMonitors(String line) {
        DM5DiagnosticReadinessPacket packet = parseMonitors(line);
        if (packet != null) {
            if (ratiosAndMonitorsCanBeRead) {
                // There's a sweet spot in the data plate report section after
                // the codes are cleared where the DM5s are valid
                initialMonitors.addAll(packet.getMonitoredSystems());
                if (packet.getSourceAddress() == J1939.ENGINE_ADDR) {
                    initialMonitorsTime = parseDateTime(line);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Checks the line to determine if it contains {@link PerformanceRatio}s
     *
     * @param line
     *            the line to check
     * @return true if the line contains {@link PerformanceRatio}s
     */
    private boolean checkRatios(String line) {
        DM20MonitorPerformanceRatioPacket packet = parseRatios(line);
        if (packet != null) {
            if (ratiosAndMonitorsCanBeRead) {
                // There's a sweet spot in the data plate report section after
                // the codes are cleared where the DM20s are valid
                initialRatios.addAll(packet.getRatios());
                if (packet.getSourceAddress() == J1939.ENGINE_ADDR) {
                    initialRatiosTime = parseDateTime(line);
                    initialIgnitionCycles = packet.getIgnitionCycles();
                    initialOBDCounts = packet.getOBDConditionsCount();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Checks the line to determine if it contains a TSCC value
     *
     * @param line
     *            the line to check
     * @return true if it contains a TSCC value
     */
    private boolean checkTscc(String line) {
        int lineTscc = parseTscc(line);

        if (lineTscc != Integer.MIN_VALUE) {
            tscc = lineTscc;
            return true;
        }
        return false;
    }

    /**
     * Checks the line to determine if it contains a VIN
     *
     * @param line
     *            the line to check
     * @return true if it contains a VIN
     * @throws ReportFileException
     *             if the found VIN doesn't match the existing VIN
     */
    private boolean checkVin(String line) throws ReportFileException {
        String lineVin = parseVin(line);

        if (lineVin != null) {
            if (vin != null && !vin.equals(lineVin)) {
                throw new ReportFileException(Problem.VIN_INCONSISTENT);
            }
            vin = lineVin;
            return true;
        }
        return false;
    }

    /**
     * Returns the {@link Set} of {@link CalibrationInformation} that was found
     * in the existing report file
     *
     * @return {@link Set} of {@link CalibrationInformation}
     */
    public Set<CalibrationInformation> getCalibrations() {
        return calMap.values().stream().flatMap(p -> p.getCalibrationInformation().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Returns the VIN found in the report file. If the report file is new, null
     * is returned
     *
     * @return String
     */
    public String getFileVin() {
        return vin;
    }

    /**
     * Returns the number of ignition cycles when the {@link PerformanceRatio}s
     * were initialized
     *
     * @return int
     */
    public int getInitialIgnitionCycles() {
        return initialIgnitionCycles;
    }

    /**
     * Returns the initial {@link MonitoredSystem}s found in the file
     *
     * @return {@link Set} of {@link MonitoredSystem}
     */
    public Set<MonitoredSystem> getInitialMonitors() {
        return initialMonitors;
    }

    /**
     * Returns the {@link LocalDateTime} when the {@link MonitoredSystem}s were
     * initialized
     *
     * @return {@link LocalDateTime}
     */
    public LocalDateTime getInitialMonitorsTime() {
        return initialMonitorsTime;
    }

    /**
     * Returns the number of OBD Monitoring Conditions Encountered when the
     * {@link PerformanceRatio}s were initialized
     *
     * @return int
     */
    public int getInitialOBDCounts() {
        return initialOBDCounts;
    }

    /**
     * Returns the initial values for the {@link PerformanceRatio}s found in the
     * file
     *
     * @return {@link Set} of {@link PerformanceRatio}
     */
    public Set<PerformanceRatio> getInitialRatios() {
        return initialRatios;
    }

    /**
     * Returns the {@link LocalDateTime} when the {@link PerformanceRatio}s were
     * initialized
     *
     * @return {@link LocalDateTime}
     */
    public LocalDateTime getInitialRatiosTime() {
        return initialRatiosTime;
    }

    private Logger getLogger() {
        return IUMPR.getLogger();
    }

    /**
     * Returns the Time Since Code Clear from the file, in minutes
     *
     * @return int
     */
    public int getMinutesSinceCodeClear() {
        return tscc;
    }

    /**
     * Increments the total number of queries
     */
    public void incrementQueries() {
        queries++;
    }

    /**
     * Returns true to indicate the report file is new; false to indicate the
     * report file was already existing
     *
     * @return boolean
     */
    public boolean isNewFile() {
        return newFile;
    }

    @Override
    public void onComplete(boolean success) {
        // Don't care
    }

    /**
     * Called when the tool exits so it can be noted in the log file
     */
    public void onProgramExit() {
        try {
            if (writer != null) {
                write(getDateTime() + " End of " + BannerModule.TOOL_NAME + " Execution" + NL);
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error writing end of program statement", e);
        }
    }

    @Override
    public void onProgress(int currentStep, int totalSteps, String message) {
        // Don't care
    }

    @Override
    public void onProgress(String message) {
        // Don't care
    }

    @Override
    public void onResult(List<String> results) {
        for (String result : results) {
            onResult(result);
        }
    }

    @Override
    public void onResult(String result) {
        try {
            write(result);
            writer.flush();

            updateCounters(result);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error Writing to file", e);
        }
    }

    @Override
    public void onUrgentMessage(String message, String title, int type) {
        // Don't care
    }

    private DM19CalibrationInformationPacket parseCal(String line) {
        Packet packet = parsePacket(DM19CalibrationInformationPacket.PGN, line);
        return packet == null ? null : new DM19CalibrationInformationPacket(packet);
    }

    private LocalDateTime parseDateTime(String line) {
        TemporalAccessor lineInstant = null;

        int endIndex = line.indexOf(" ");
        if (endIndex > 0) {
            String time = line.substring(0, endIndex);
            try {
                lineInstant = getDateTimeModule().parse(time);
            } catch (DateTimeParseException e) {
                // Don't care - it happens
            }
        }

        if (lineInstant instanceof LocalTime) {
            return ((LocalTime) lineInstant).atDate(lastInstant.toLocalDate());
        } else if (lineInstant instanceof LocalDateTime) {
            return (LocalDateTime) lineInstant;
        } else {
            return null;
        }
    }

    private DM5DiagnosticReadinessPacket parseMonitors(String line) {
        Packet packet = parsePacket(DM5DiagnosticReadinessPacket.PGN, line);
        return packet == null ? null : new DM5DiagnosticReadinessPacket(packet);
    }

    private Packet parsePacket(int pgn, String line) {
        if (line.contains(String.format("%04X", pgn))) {
            int index = line.indexOf(" ");
            String trimmedLine = line.substring(index + 1);
            Packet packet = Packet.parse(trimmedLine);
            if (packet != null && packet.getId() == pgn) {
                return packet;
            }
        }
        return null;
    }

    private DM20MonitorPerformanceRatioPacket parseRatios(String line) {
        Packet packet = parsePacket(DM20MonitorPerformanceRatioPacket.PGN, line);
        return packet == null ? null : new DM20MonitorPerformanceRatioPacket(packet);
    }

    private int parseTscc(String line) {
        int lineTscc = Integer.MIN_VALUE;
        Packet packet = parsePacket(DM21DiagnosticReadinessPacket.PGN, line);
        if (packet != null) {
            DM21DiagnosticReadinessPacket dm21 = new DM21DiagnosticReadinessPacket(packet);
            lineTscc = (int) dm21.getMinutesSinceDTCsCleared();
        }

        int fromIndex = line.indexOf(DM21DiagnosticReadinessPacket.TSCC_LINE);
        if (fromIndex != -1) {
            try {
                // Get the Time Since Code Clear from the line
                int beginIndex = line.indexOf(":", fromIndex) + 1;
                String tscc = line.substring(beginIndex).replace("minutes", "").trim();
                lineTscc = Integer.parseInt(tscc);
            } catch (NumberFormatException e) {
                // The line couldn't be parsed. Oh well...
            }
        }
        return lineTscc;
    }

    private String parseVin(String line) {
        String lineVin = null;

        Packet packet = parsePacket(VehicleIdentificationPacket.PGN, line);
        if (packet != null) {
            VehicleIdentificationPacket vip = new VehicleIdentificationPacket(packet);
            lineVin = vip.getVin();
        }

        int fromIndex = line.indexOf(VehicleIdentificationPacket.NAME + " from ");
        if (fromIndex != -1) {
            // Get the VIN from the line
            int beginIndex = line.indexOf(":", fromIndex) + 1;
            lineVin = line.substring(beginIndex, line.length()).trim();
        }
        return lineVin;
    }

    /**
     * Reports the communication quality with the module for the current session
     * and resets the counters
     *
     * @param listener
     *            the {@link ResultsListener} that will be notified of the
     *            results
     */
    public void reportAndResetCommunicationQuality(ResultsListener listener) {
        listener.onResult(getDateTime() + " Total Queries: " + queries);
        listener.onResult(getDateTime() + " Total Time Out Errors: " + timeouts);
        resetQueries();
        resetTimeouts();
    }

    /**
     * Reports the information about the report file
     *
     * @param listener
     *            the {@link ResultsListener} that will be notified of the
     *            results
     */
    public void reportFileInformation(ResultsListener listener) {
        listener.onResult(
                getDateTime() + (isNewFile() ? " New" : " Existing") + " File: " + reportFile.getAbsolutePath());
    }

    /**
     * Reports the quality of the report collection process
     *
     * @param listener
     *            the {@link ResultsListener} that will be notified of the
     *            results
     */
    public void reportQuality(ResultsListener listener) {
        // +1 is because this will be written during the Collection Log Report
        // and it needs to include itself
        listener.onResult(getDateTime() + " Total Data Collection Log Reports: " + (collectionLogs + 1));
        listener.onResult(getDateTime() + " Total Excessive Time Since Code Clear Gaps: " + excessiveTimeGaps);
    }

    private void resetCounters() {
        vin = null;
        tscc = Integer.MIN_VALUE;
        calMap.clear();
        initialRatios.clear();
        initialMonitors.clear();
        ratiosAndMonitorsCanBeRead = false;
        initialIgnitionCycles = Integer.MIN_VALUE;
        initialOBDCounts = Integer.MIN_VALUE;
        initialMonitorsTime = null;
        initialRatiosTime = null;
        lastInstant = null;
    }

    /**
     * Resets the number of queries found in the file
     */
    private void resetQueries() {
        queries = 0;
    }

    /**
     * Resets the number of timeouts found in the file
     */
    private void resetTimeouts() {
        timeouts = 0;
    }

    /**
     * Scans an existing file
     *
     * @param listener
     *            the {@link ResultsListener} that will be notified of progress
     * @throws IOException
     *             if there is a problem scanning the file
     */
    private void scanFile(ResultsListener listener) throws IOException {
        Problem[] problem = new Problem[1];
        try {
            int lines = (int) Files.lines(reportFile.toPath()).parallel().count();
            int[] step = new int[] { 0 };
            Files.lines(reportFile.toPath()).sequential().filter(t -> problem[0] == null).forEach(line -> {
                listener.onProgress(++step[0], lines, "Scanning Report File");
                if (line.contains(COLLECTION_LOG_FOOTER)) {
                    collectionLogs++;
                } else if (line.contains(TIME_GAP_MESSAGE)) {
                    excessiveTimeGaps++;
                } else {
                    try {
                        checkDate(line);

                        // Flag used to skip re-parsing the line if it's already
                        // been parsed
                        boolean parsed = false;
                        if (!parsed) {
                            parsed = checkDTCsCleared(line);
                        }
                        if (!parsed) {
                            parsed = checkMonitors(line);
                        }
                        if (!parsed) {
                            parsed = checkRatios(line);
                        }
                        if (!parsed) {
                            parsed = checkVin(line);
                        }
                        if (!parsed) {
                            parsed = checkTscc(line);
                        }
                        if (!parsed) {
                            parsed = checkCals(line);
                        }
                        if (!parsed) {
                            parsed = checkEndOfReport(line);
                        }
                    } catch (Exception e) {
                        if (e instanceof ReportFileException) {
                            problem[0] = ((ReportFileException) e).getProblem();
                        } else {
                            getLogger().log(Level.SEVERE, "Error parsing line: " + line, e);
                        }
                    }
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }

        if (problem[0] != null) {
            throw new ReportFileException(problem[0]);
        } else if (vin == null) {
            throw new ReportFileException(Problem.VIN_NOT_PRESENT);
        } else if (lastInstant == null) {
            throw new ReportFileException(Problem.DATE_NOT_PRESENT);
        } else if (tscc == Integer.MIN_VALUE) {
            throw new ReportFileException(Problem.TSCC_NOT_PRESENT);
        } else if (calMap.isEmpty()) {
            throw new ReportFileException(Problem.CAL_NOT_PRESENT);
        } else if (initialMonitors.isEmpty()) {
            throw new ReportFileException(Problem.MONITORS_NOT_PRESENT);
        } else if (initialRatios.isEmpty()) {
            throw new ReportFileException(Problem.RATIOS_NOT_PRESENT);
        }
    }

    /**
     * Sets the flag to indicate if the file is new or not
     *
     * @param isNewFile
     *            true indicates the file is new
     */
    public void setNewFile(boolean isNewFile) {
        newFile = isNewFile;
    }

    /**
     * Sets the File that will be used to log results to
     *
     * @param listener
     *            the {@link ResultsListener} that will be notified of progress
     * @param reportFile
     *            the File used for the report
     * @param isNewFile
     *            true to indicate the report file is new; false to indicate the
     *            report file was already existing
     * @throws IOException
     *             if there is problem with the file
     *
     */
    public void setReportFile(ResultsListener listener, File reportFile, boolean isNewFile) throws IOException {
        setNewFile(isNewFile);
        resetQueries();
        resetTimeouts();
        collectionLogs = 0;
        excessiveTimeGaps = 0;

        if (writer != null) {
            writer.close();
            writer = null;
        }

        resetCounters();

        if (reportFile != null) {
            this.reportFile = reportFile;
            if (!isNewFile) {
                try {
                    scanFile(listener);
                } catch (IOException e) {
                    resetCounters();
                    throw e;
                }
            }
            writer = Files.newBufferedWriter(reportFile.toPath(), StandardOpenOption.APPEND);
        }
    }

    /**
     * Updates the counters with values from the result
     *
     * @param result
     *            the result to scan
     * @throws ReportFileException
     *             if there's an inconsistency with the existing values and the
     *             new values
     */
    private void updateCounters(String result) throws ReportFileException {
        String[] lines = result.split(NL);
        if (lines.length > 1) {
            for (String line : lines) {
                updateCounters(line);
            }
        } else {
            // flag is used to skip parsing the result if it's already been
            // parsed
            boolean parsed = false;
            if (isNewFile()) {
                checkDate(result);
                if (!parsed) {
                    parsed = checkDTCsCleared(result);
                }
                if (!parsed) {
                    parsed = checkMonitors(result);
                }
                if (!parsed) {
                    parsed = checkRatios(result);
                }

                if (!parsed) {
                    parsed = checkCals(result);
                }
                if (!parsed) {
                    parsed = checkVin(result);
                }
                if (!parsed) {
                    parsed = checkEndOfReport(result);
                }
            }

            if (!parsed) {
                parsed = checkTscc(result);
            }

            if (!parsed) {
                if (result.contains(TX)) {
                    incrementQueries();
                } else if (result.contains(TIMEOUT_MESSAGE)) {
                    timeouts++;
                } else if (result.contains(COLLECTION_LOG_FOOTER)) {
                    collectionLogs++;
                } else if (result.contains(TIME_GAP_MESSAGE)) {
                    excessiveTimeGaps++;
                }
            }
        }
    }

    /**
     * Writes a result to the report file
     *
     * @param result
     *            the result to write
     * @throws IOException
     *             if there is a problem writing to the file
     */
    private void write(String result) throws IOException {
        writer.write(result + NL);
    }

}
