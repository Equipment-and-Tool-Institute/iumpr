/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static net.soliddesign.iumpr.IUMPR.NL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.etools.j1939tools.bus.Adapter;
import org.etools.j1939tools.j1939.packets.DM19CalibrationInformationPacket.CalibrationInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.soliddesign.iumpr.controllers.TestResultsListener;
import net.soliddesign.iumpr.modules.ReportFileModule.Problem;
import net.soliddesign.iumpr.modules.ReportFileModule.ReportFileException;

/**
 * Unit tests for the {@link ReportFileModule} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class ReportFileModuleTest {

    private File file;

    private ReportFileModule instance;
    private TestResultsListener listener;
    private Logger logger;

    @Before
    public void setUp() throws Exception {
        file = File.createTempFile("test", ".iumpr");
        file.deleteOnExit();
        listener = new TestResultsListener();

        TestDateTimeModule dateTimeModule = new TestDateTimeModule() {
            @Override
            public DateTimeFormatter getTimeFormatter() {
                return getSuperTimeFormatter();
            }
        };

        logger = mock(Logger.class);
        instance = new ReportFileModule(dateTimeModule, logger);
    }

    @After
    public void tearDown() throws Exception {
        if (!file.delete()) {
            System.err.println("Could not delete test file");
        }
    }

    @Test
    public void testCalInfoBroadcast() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 1CD3FF00 57 FD CF 54 35 52 32 30 33 31 30 30 41 30 30 30 30 30 30 30"
                        + NL);
        writer.write(
                "2017-02-11T16:42:21.890 1CD3FF00 57 FD CF 54 35 52 32 30 33 31 30 30 41 30 30 30 30 30 30 30"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.close();
        try {
            instance.setReportFile(listener, file, false);
        } catch (ReportFileException e) {
            assertEquals(Problem.MONITORS_NOT_PRESENT, e.getProblem());
        }
    }

    @Test
    public void testCalInfoCanNotBeParsed() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000, 96, BF, DC, 40, 50, 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.close();
        try {
            instance.setReportFile(listener, file, false);
        } catch (ReportFileException e) {
            assertEquals(Problem.CAL_NOT_PRESENT, e.getProblem());
        }
        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(Integer.MIN_VALUE, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(0, instance.getCalibrations().size());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

    @Test
    public void testCalInfoDuplicated() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);

        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
        writer.write("2017-03-05T12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00" + NL);
        writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
        writer.close();

        instance.setReportFile(listener, file, false);
        assertEquals(false, instance.isNewFile());
        assertEquals("ASDFGHJKLASDFGHJKL", instance.getFileVin());
        assertEquals(14, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(1, instance.getCalibrations().size());

        CalibrationInformation calInfo = instance.getCalibrations().iterator().next();
        assertEquals("PBT5MPR3", calInfo.getCalibrationIdentification().trim());
        assertEquals("0x40DCBF96", calInfo.getCalibrationVerificationNumber());
    }

    @Test
    public void testCalInfoInconsistent() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write(
                "2017-02-11T16:42:21.890 18D30000 97 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.close();
        try {
            instance.setReportFile(listener, file, false);
        } catch (ReportFileException e) {
            assertEquals(Problem.CAL_INCONSISTENT, e.getProblem());
        }
        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(Integer.MIN_VALUE, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(0, instance.getCalibrations().size());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

    @Test
    public void testCalInfoNotPresent() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.close();

        try {
            instance.setReportFile(listener, file, false);
            fail("An exception should have been thrown.");
        } catch (ReportFileException e) {
            assertEquals(Problem.CAL_NOT_PRESENT, e.getProblem());
        }
        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(Integer.MIN_VALUE, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(0, instance.getCalibrations().size());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

    @Test
    public void testCalInfoWithTwoModules() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write(
                "2017-02-11T16:43:16.362 18D30055 A8 73 89 13 4E 4F 78 2D 53 41 45 31 34 61 20 41 54 49 31 20"
                        + NL);

        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
        writer.write("2017-03-05T12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00" + NL);
        writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
        writer.close();

        instance.setReportFile(listener, file, false);
        assertEquals(false, instance.isNewFile());
        assertEquals("ASDFGHJKLASDFGHJKL", instance.getFileVin());
        assertEquals(14, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(2, instance.getCalibrations().size());

        int count = 0;
        for (CalibrationInformation info : instance.getCalibrations()) {
            if ("PBT5MPR3".equals(info.getCalibrationIdentification().trim())
                    && "0x40DCBF96".equals(info.getCalibrationVerificationNumber())) {
                count |= 1;
            }
            if ("NOx-SAE14a ATI1".equals(info.getCalibrationIdentification().trim())
                    && "0x138973A8".equals(info.getCalibrationVerificationNumber())) {
                count |= 2;
            }
        }

        assertEquals(3, count);
    }

    @Test
    public void testIncrementQueries() throws Exception {
        instance.setReportFile(listener, file, true);

        instance.incrementQueries();
        instance.incrementQueries();
        instance.incrementQueries();

        instance.reportAndResetCommunicationQuality(listener);

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Total Queries: 3" + NL;
        expected += "2007-12-03T10:15:30.000 Total Time Out Errors: 0" + NL;
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testMonitorsAreNotUpdatedWithExistingFile() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
        writer.write("2017-03-05T12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00" + NL);
        writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
        writer.close();

        instance.setReportFile(listener, file, false);

        assertEquals(LocalDateTime.parse("2017-03-05T12:21:45.090"), instance.getInitialMonitorsTime());
        assertEquals(16, instance.getInitialMonitors().size());

        instance.onResult("2017-03-05T12:23:45.090 18FECE00 00 00 14 37 E0 1E E0 1E");
        assertEquals(LocalDateTime.parse("2017-03-05T12:21:45.090"), instance.getInitialMonitorsTime());
    }

    @Test
    public void testMonitorsNotPresent() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
        writer.write("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00" + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
        writer.close();

        try {
            instance.setReportFile(listener, file, false);
        } catch (ReportFileException e) {
            assertEquals(Problem.MONITORS_NOT_PRESENT, e.getProblem());
        }

        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(Integer.MIN_VALUE, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(0, instance.getCalibrations().size());

        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

    @Test
    public void testNewFileUpdatesInformation() throws Exception {
        instance.setReportFile(listener, file, true);
        assertEquals(true, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(Integer.MIN_VALUE, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(0, instance.getCalibrations().size());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());

        instance.onResult("2017-03-01T09:50:23.386 Global DM19 (Calibration Information) Request" + NL
                + "2017-03-01T09:50:23.386 18EAFFF9 00 D3 00 (TX)" + NL +
                "2017-03-01T09:50:25.639 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20"
                + NL +
                "DM19 from Engine #1 (0): CAL ID of PBT5MPR3 and CVN of 0x40DCBF96");

        instance.onResult("  Time Since DTCs Cleared:                      14 minutes");

        instance.onResult(
                "2017-03-01T09:50:26.317 18FEEC00 33 48 41 4D 4B 53 54 4E 30 46 4C 35 37 35 30 31 32 2A"
                        + NL + "Vehicle Identification from Engine #1 (0): 3HAMKSTN0FL575012");

        instance.onResult("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared.");
        instance.onResult("12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E");
        instance.onResult("  Time Since DTCs Cleared:                      14 minutes");
        instance.onResult("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00");
        instance.onResult("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");

        assertEquals("3HAMKSTN0FL575012", instance.getFileVin());
        assertEquals(14, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(1, instance.getCalibrations().size());

        CalibrationInformation calInfo = instance.getCalibrations().iterator().next();
        assertEquals("PBT5MPR3", calInfo.getCalibrationIdentification().trim());
        assertEquals("0x40DCBF96", calInfo.getCalibrationVerificationNumber());

        assertEquals(LocalDateTime.parse("2017-03-05T12:21:45.090"), instance.getInitialMonitorsTime());
        assertEquals(16, instance.getInitialMonitors().size());
        assertEquals(LocalDateTime.parse("2017-03-05T12:21:47.610"), instance.getInitialRatiosTime());
        assertEquals(1, instance.getInitialRatios().size());
        assertEquals(12, instance.getInitialIgnitionCycles());
        assertEquals(1, instance.getInitialOBDCounts());
    }

    @Test
    public void testOnCompleteFalse() {
        instance.onComplete(false);
        // Nothing (bad) happens;
    }

    @Test
    public void testOnCompleteTrue() {
        instance.onComplete(true);
        // Nothing (bad) happens;
    }

    @Test
    public void testOnProgramExit() throws Exception {
        instance.setReportFile(listener, file, true);
        instance.onProgramExit();
        List<String> lines = Files.readAllLines(file.toPath());
        String expected = "2007-12-03T10:15:30.000 End of IUMPR Data Collection Tool Execution";
        assertEquals(expected, lines.get(0));
    }

    @Test
    public void testOnProgramExitWithoutFile() throws Exception {
        instance.onProgramExit();
        // Nothing (bad) happens
    }

    @Test
    public void testOnProgress() {
        instance.onProgress(5, 10, "Message");
        // Nothing (bad) happens
    }

    @Test
    public void testOnProgressWithMessage() {
        instance.onProgress("Message");
        // Nothing (bad) happens
    }

    @Test
    public void testOnResult() throws Exception {
        instance.setReportFile(listener, file, true);
        List<String> results = new ArrayList<>();
        results.add("Line 1");
        results.add("Line 2");
        results.add("Line 3");
        instance.onResult(results);
        List<String> lines = Files.readAllLines(file.toPath());
        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }

    @Test
    public void testRatioCountersUseMax() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
        writer.write("2017-03-05T12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("2017-03-05T12:21:46.090 18FECE01 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("2017-03-05T12:21:46.190 18FECE17 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00" + NL);
        writer.write("2017-03-05T12:21:48.610 18C20055 1C 00 11 00 CB 14 F8 00 00 01 00" + NL);
        writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
        writer.close();

        instance.setReportFile(listener, file, false);
        assertEquals(false, instance.isNewFile());
        assertEquals("ASDFGHJKLASDFGHJKL", instance.getFileVin());
        assertEquals(14, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(1, instance.getCalibrations().size());

        CalibrationInformation calInfo = instance.getCalibrations().iterator().next();
        assertEquals("PBT5MPR3", calInfo.getCalibrationIdentification().trim());
        assertEquals("0x40DCBF96", calInfo.getCalibrationVerificationNumber());

        assertEquals(LocalDateTime.parse("2017-03-05T12:21:45.090"), instance.getInitialMonitorsTime());
        assertEquals(16, instance.getInitialMonitors().size());
        assertEquals(LocalDateTime.parse("2017-03-05T12:21:47.610"), instance.getInitialRatiosTime());
        assertEquals(2, instance.getInitialRatios().size());
        assertEquals(28, instance.getInitialIgnitionCycles());
        assertEquals(17, instance.getInitialOBDCounts());
    }

    @Test
    public void testRatiosAreNotUpdatedWithExistingFile() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
        writer.write("2017-03-05T12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00" + NL);
        writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
        writer.close();

        instance.setReportFile(listener, file, false);

        assertEquals(LocalDateTime.parse("2017-03-05T12:21:47.610"), instance.getInitialRatiosTime());
        assertEquals(1, instance.getInitialRatios().size());
        assertEquals(12, instance.getInitialIgnitionCycles());
        assertEquals(1, instance.getInitialOBDCounts());

        instance.onResult("2017-03-05T12:23:47.610 18C20000 1C 00 11 00 CA 14 F8 00 00 01 00 CB 14 F8 00 00 01 00");
        assertEquals(LocalDateTime.parse("2017-03-05T12:21:47.610"), instance.getInitialRatiosTime());
        assertEquals(1, instance.getInitialRatios().size());
        assertEquals(12, instance.getInitialIgnitionCycles());
        assertEquals(1, instance.getInitialOBDCounts());
    }

    @Test
    public void testRatiosNotPresent() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
        writer.write("2017-03-05T12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
        writer.close();

        try {
            instance.setReportFile(listener, file, false);
        } catch (ReportFileException e) {
            assertEquals(Problem.RATIOS_NOT_PRESENT, e.getProblem());
        }

        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(Integer.MIN_VALUE, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(0, instance.getCalibrations().size());

        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

    @Test
    public void testReportAndResetCommunicationQuality() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
        writer.write("2017-03-05T12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00" + NL);
        writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
        writer.close();
        instance.setReportFile(listener, file, false);
        assertEquals(false, instance.isNewFile());

        List<String> results = new ArrayList<>();
        results.add("asdlkfalsdjkf (TX)");
        results.add("asdlkfalsdjkf");
        results.add("asdlkfalsdjkf (TX)");
        results.add("asdlkfalsdjkf (TX)");
        results.add("asdlkfalsdjkf");
        results.add("Error: Timeout - No Response.");
        results.add("asdfasdf Error: Timeout - No Response.");

        instance.onResult(results);

        instance.reportAndResetCommunicationQuality(listener);

        String expected = "";
        expected += "2007-12-03T10:15:30.000 Total Queries: 3" + NL;
        expected += "2007-12-03T10:15:30.000 Total Time Out Errors: 2" + NL;
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testReportFileInformationWithExistingFile() throws Exception {
        String path = file.getAbsolutePath();

        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
        writer.write("2017-03-05T12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00" + NL);
        writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
        writer.close();

        instance.setReportFile(listener, file, false);
        instance.setAdapter(new Adapter("Nexiq USBLink 2", "NULN2R32", (short) 1));
        instance.reportFileInformation(listener);

        String expected = "2007-12-03T10:15:30.000 Existing File: " + path + NL +
                "2007-12-03T10:15:30.000 Selected Adapter: NULN2R32 - Nexiq USBLink 2" + NL;
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testReportFileInformationWithNewFile() throws Exception {
        File reportFile = mock(File.class);
        when(reportFile.getAbsolutePath()).thenReturn("files/users/report.iumpr");
        when(reportFile.toPath()).thenReturn(file.toPath());
        instance.setReportFile(listener, reportFile, true);
        instance.setAdapter(new Adapter("Nexiq USBLink 2", "NULN2R32", (short) 1));

        instance.reportFileInformation(listener);

        String expected = "2007-12-03T10:15:30.000 New File: files/users/report.iumpr" + NL +
                "2007-12-03T10:15:30.000 Selected Adapter: NULN2R32 - Nexiq USBLink 2" + NL;
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testReportQuality() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write("Data Collection Log END OF REPORT" + NL);
        writer.write("asdfasdf Data Collection Log END OF REPORT" + NL);
        writer.write("Data Collection Log END OF REPORT asdfasdf" + NL);
        writer.write("ERROR Excess Time Since Code Cleared Gap of 70 minutes" + NL);
        writer.write("ERROR Excess Time Since Code Cleared Gap of 90 minutes" + NL);
        writer.write("ERROR Excess Time DTC Cleared Gap of 70 minutes" + NL);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
        writer.write("2017-03-05T12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00" + NL);
        writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
        writer.close();

        instance.setReportFile(listener, file, false);
        assertEquals(false, instance.isNewFile());

        instance.onResult("ERROR Excess Time Since Code Cleared Gap of 100 minutes");
        instance.reportQuality(listener);
        String expected = "";
        expected += "2007-12-03T10:15:30.000 Total Data Collection Log Reports: 4" + NL;
        expected += "2007-12-03T10:15:30.000 Total Excessive Time Since Code Clear Gaps: 3" + NL;
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testReportQualityWithReportedResults() throws Exception {
        List<String> results = new ArrayList<>();

        results.add("Data Collection Log END OF REPORT");
        results.add("asdfasdf Data Collection Log END OF REPORT");
        results.add("Data Collection Log END OF REPORT asdfasdf");
        results.add("Vehicle Identification from Engine #1 (0): 09876543210987654321");
        results.add("VIN from Engine #1 (0): ASDFGHJKLASDFGHJKL");

        instance.setReportFile(listener, file, true);
        assertEquals(true, instance.isNewFile());
        instance.onResult(results);

        instance.reportQuality(listener);
        String expected = "";
        expected += "2007-12-03T10:15:30.000 Total Data Collection Log Reports: 4" + NL;
        expected += "2007-12-03T10:15:30.000 Total Excessive Time Since Code Clear Gaps: 0" + NL;
        assertEquals(expected, listener.getResults());
    }

    @Test
    public void testScanFileAndGetInfo() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("SPN 237 - Vehicle Identification Number: Supports Data Stream with data length 0 bytes" + NL);
        writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
        writer.write("2017-03-05T12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00" + NL);
        // The following lines tests an out of order date sequence from packets
        // capture during monitor completion.
        writer.write("2017-03-05T12:43:53.076 Begin Tracking Monitor Completion Status" + NL);
        writer.write("12:46:55.854 DM5 Packet(s) Received" + NL);
        writer.write("12:46:45.933 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("2017-03-05T12:54:38.658 End Tracking Monitor Completion Status. 65 Total Cycles." + NL);
        writer.write("2017-03-05T13:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
        writer.close();

        instance.setReportFile(listener, file, false);
        assertEquals(false, instance.isNewFile());
        assertEquals("ASDFGHJKLASDFGHJKL", instance.getFileVin());
        assertEquals(14, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(1, instance.getCalibrations().size());

        CalibrationInformation calInfo = instance.getCalibrations().iterator().next();
        assertEquals("PBT5MPR3", calInfo.getCalibrationIdentification().trim());
        assertEquals("0x40DCBF96", calInfo.getCalibrationVerificationNumber());

        assertEquals(LocalDateTime.parse("2017-03-05T12:21:45.090"), instance.getInitialMonitorsTime());
        assertEquals(16, instance.getInitialMonitors().size());
        assertEquals(LocalDateTime.parse("2017-03-05T12:21:47.610"), instance.getInitialRatiosTime());
        assertEquals(1, instance.getInitialRatios().size());
        assertEquals(12, instance.getInitialIgnitionCycles());
        assertEquals(1, instance.getInitialOBDCounts());

        verify(logger).log(Level.WARNING, "The dates in the file are inconsistent.");
        verifyNoMoreInteractions(logger);
    }

    @Test
    public void testScanFileWithDateInconsistent() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write("2017-02-11T16:43:34.218 VIN from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-02-11T16:44:20.679 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-02-11T16:44:21.676 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-02-11T16:44:22.679 VIN from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.close();

        try {
            instance.setReportFile(listener, file, false);
            fail("An exception should have been thrown.");
        } catch (ReportFileException e) {
            assertEquals(Problem.DATE_INCONSISTENT, e.getProblem());
        }
        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

    @Test
    public void testScanFileWithNewFileAndGetInfo() throws Exception {
        instance.setReportFile(listener, file, true);
        assertEquals(true, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(Integer.MIN_VALUE, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(0, instance.getCalibrations().size());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(null, instance.getInitialRatiosTime());
    }

    @Test
    public void testSetReportFileWithNull() throws Exception {
        instance.setReportFile(listener, null, false);
        // Nothing (bad) happens
        assertEquals(null, instance.getFileVin());
    }

    @Test
    public void testTimeSinceCodeClearCanNotBeParsed() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      Not Available" + NL);

        writer.write("2017-02-11T16:44:20.676 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.close();

        try {
            instance.setReportFile(listener, file, false);
            fail("An exception should have been thrown");
        } catch (ReportFileException e) {
            assertEquals(Problem.TSCC_NOT_PRESENT, e.getProblem());
        }
        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(Integer.MIN_VALUE, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(0, instance.getCalibrations().size());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

    @Test
    public void testTimeSinceCodeClearCanNotBeParsedWithPacket() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("2017-02-11T16:43:18.770 18C10000, 00, 00, 00, 00 00 00 0E 00" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.close();

        try {
            instance.setReportFile(listener, file, false);
            fail("An exception should have been thrown");
        } catch (ReportFileException e) {
            assertEquals(Problem.TSCC_NOT_PRESENT, e.getProblem());
        }
        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(Integer.MIN_VALUE, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(0, instance.getCalibrations().size());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

    @Test
    public void testTimeSinceCodeClearNotPresent() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("2017-02-11T16:44:20.676 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.close();

        try {
            instance.setReportFile(listener, file, false);
            fail("An exception should have been thrown");
        } catch (ReportFileException e) {
            assertEquals(Problem.TSCC_NOT_PRESENT, e.getProblem());
        }
        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(Integer.MIN_VALUE, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(0, instance.getCalibrations().size());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

    @Test
    public void testTimeSinceCodeClearWithPacket() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("2017-02-11T16:43:18.770 18C10000 00 00 00 00 00 00 0E 00" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
        writer.write("2017-03-05T12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00" + NL);
        writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
        writer.close();

        instance.setReportFile(listener, file, false);
        assertEquals(false, instance.isNewFile());
        assertEquals("ASDFGHJKLASDFGHJKL", instance.getFileVin());
        assertEquals(14, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(1, instance.getCalibrations().size());
        CalibrationInformation calInfo = instance.getCalibrations().iterator().next();
        assertEquals("PBT5MPR3", calInfo.getCalibrationIdentification().trim());
        assertEquals("0x40DCBF96", calInfo.getCalibrationVerificationNumber());

        instance.onResult("2017-03-05T12:43:18.770 18C10000 00 00 00 00 00 00 1E 00");
        assertEquals(30, instance.getMinutesSinceCodeClear(), 0.0001);

        instance.onResult("  Time Since DTCs Cleared:                      100 minutes");
        assertEquals(100, instance.getMinutesSinceCodeClear(), 0.0001);
    }

    @Test
    public void testValuesWithExsitingFileThenNewFile() throws Exception {
        {
            Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
            writer.write(
                    "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                            + NL);
            writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
            writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
            writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
            writer.write("2017-03-05T12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
            writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
            writer.write("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00" + NL);
            writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
            writer.close();

            instance.setReportFile(listener, file, false);
            assertEquals(false, instance.isNewFile());
            assertEquals("ASDFGHJKLASDFGHJKL", instance.getFileVin());
            assertEquals(14, instance.getMinutesSinceCodeClear(), 0.0001);
            assertEquals(1, instance.getCalibrations().size());

            CalibrationInformation calInfo = instance.getCalibrations().iterator().next();
            assertEquals("PBT5MPR3", calInfo.getCalibrationIdentification().trim());
            assertEquals("0x40DCBF96", calInfo.getCalibrationVerificationNumber());

            assertEquals(LocalDateTime.parse("2017-03-05T12:21:45.090"), instance.getInitialMonitorsTime());
            assertEquals(16, instance.getInitialMonitors().size());

            assertEquals(LocalDateTime.parse("2017-03-05T12:21:47.610"), instance.getInitialRatiosTime());
            assertEquals(1, instance.getInitialRatios().size());
            assertEquals(12, instance.getInitialIgnitionCycles());
            assertEquals(1, instance.getInitialOBDCounts());
        }

        {
            File file2 = File.createTempFile("test", ".iumpr");
            file2.deleteOnExit();

            instance.setReportFile(listener, file2, true);
            assertEquals(true, instance.isNewFile());
            assertEquals(null, instance.getFileVin());
            assertEquals(Integer.MIN_VALUE, instance.getMinutesSinceCodeClear(), 0.0001);
            assertEquals(0, instance.getCalibrations().size());

            assertEquals(null, instance.getInitialMonitorsTime());
            assertEquals(0, instance.getInitialMonitors().size());

            assertEquals(null, instance.getInitialRatiosTime());
            assertEquals(0, instance.getInitialRatios().size());
            assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
            assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
        }
    }

    @Test
    public void testValuesWithTwoDifferentFiles() throws Exception {
        {
            Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
            writer.write(
                    "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                            + NL);
            writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
            writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
            writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
            writer.write("2017-03-05T12:21:45.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
            writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
            writer.write("2017-03-05T12:21:47.610 18C20000 0C 00 01 00 CA 14 F8 00 00 01 00" + NL);
            writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
            writer.close();

            instance.setReportFile(listener, file, false);
            assertEquals(false, instance.isNewFile());
            assertEquals("ASDFGHJKLASDFGHJKL", instance.getFileVin());
            assertEquals(14, instance.getMinutesSinceCodeClear(), 0.0001);
            assertEquals(1, instance.getCalibrations().size());

            CalibrationInformation calInfo = instance.getCalibrations().iterator().next();
            assertEquals("PBT5MPR3", calInfo.getCalibrationIdentification().trim());
            assertEquals("0x40DCBF96", calInfo.getCalibrationVerificationNumber());

            assertEquals(LocalDateTime.parse("2017-03-05T12:21:45.090"), instance.getInitialMonitorsTime());
            assertEquals(16, instance.getInitialMonitors().size());

            assertEquals(LocalDateTime.parse("2017-03-05T12:21:47.610"), instance.getInitialRatiosTime());
            assertEquals(1, instance.getInitialRatios().size());
            assertEquals(12, instance.getInitialIgnitionCycles());
            assertEquals(1, instance.getInitialOBDCounts());
        }

        // To reset the steps
        listener = new TestResultsListener();
        {
            File file2 = File.createTempFile("test", ".iumpr");
            file2.deleteOnExit();
            Writer writer = Files.newBufferedWriter(file2.toPath(), StandardOpenOption.WRITE);
            writer.write(
                    "2017-02-11T16:42:21.889 18D30000 97 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                            + NL);
            writer.write("  Time Since DTCs Cleared:                      16 minutes" + NL);
            writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJK" + NL);
            writer.write("2017-03-05T12:21:43.838 Diagnostic Trouble Codes were successfully cleared." + NL);
            writer.write("2017-03-05T12:21:46.090 18FECE00 00 00 14 37 E0 1E E0 1E" + NL);
            writer.write("  Time Since DTCs Cleared:                      16 minutes" + NL);
            writer.write("2017-03-05T12:21:48.610 18C20000 1C 00 11 00 CA 14 F8 00 00 01 00" + NL);
            writer.write("2017-03-05T12:21:56.495 IUMPR Data Collection Tool Data Plate Report END OF REPORT");
            writer.close();

            instance.setReportFile(listener, file2, false);
            assertEquals(false, instance.isNewFile());
            assertEquals("ASDFGHJKLASDFGHJK", instance.getFileVin());
            assertEquals(16, instance.getMinutesSinceCodeClear(), 0.0001);
            assertEquals(1, instance.getCalibrations().size());

            CalibrationInformation calInfo = instance.getCalibrations().iterator().next();
            assertEquals("PBT5MPR3", calInfo.getCalibrationIdentification().trim());
            assertEquals("0x40DCBF97", calInfo.getCalibrationVerificationNumber());

            assertEquals(LocalDateTime.parse("2017-03-05T12:21:46.090"), instance.getInitialMonitorsTime());
            assertEquals(16, instance.getInitialMonitors().size());

            assertEquals(LocalDateTime.parse("2017-03-05T12:21:48.610"), instance.getInitialRatiosTime());
            assertEquals(1, instance.getInitialRatios().size());
            assertEquals(28, instance.getInitialIgnitionCycles());
            assertEquals(17, instance.getInitialOBDCounts());
        }
    }

    @Test
    public void testVinFailsToParsePacket() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write(
                "2017-02-12T15:16:15.163 18FEEC00, 33, 48, 41, 4D, 4B 53 54 4E 30 46 4C 35 37 35 30 31 32 2A" + NL);
        writer.close();

        try {
            instance.setReportFile(listener, file, false);
            fail("An exception should have been thrown.");
        } catch (ReportFileException e) {
            assertEquals(Problem.VIN_NOT_PRESENT, e.getProblem());
        }
        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
    }

    @Test
    public void testVinInconsistent() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write("Vehicle Identification from Engine #1 (0): 12345678901234567890" + NL);
        writer.write("Vehicle Identification from Engine #1 (0): 09876543210987654321" + NL);
        writer.close();

        try {
            instance.setReportFile(listener, file, false);
            fail("An exception should have been thrown.");
        } catch (ReportFileException e) {
            assertEquals(Problem.VIN_INCONSISTENT, e.getProblem());
        }
        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

    @Test
    public void testVinInconsistentWithPacket() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write(
                "2017-02-12T15:16:15.163 18FEEC00 33 48 41 4D 4B 53 54 4E 30 46 4C 35 37 35 30 31 32 2A"
                        + NL);
        writer.write("Vehicle Identification from Engine #1 (0): 12345678901234567890" + NL);
        writer.close();

        try {
            instance.setReportFile(listener, file, false);
            fail("An exception should have been thrown.");
        } catch (ReportFileException e) {
            assertEquals(Problem.VIN_INCONSISTENT, e.getProblem());
        }
        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

    @Test
    public void testVinNotPresent() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.close();

        try {
            instance.setReportFile(listener, file, false);
            fail("An Exception should have been thrown");
        } catch (ReportFileException e) {
            assertEquals(Problem.VIN_NOT_PRESENT, e.getProblem());
        }
        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

    @Test
    public void testWithFutureDate() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11T16:42:21.889 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11T16:44:12.164 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        final String string = instance.getDateTimeModule().format(LocalDateTime.now().plusDays(2));
        writer.write(string + " Information" + NL);
        writer.close();

        try {
            instance.setReportFile(listener, file, false);
            fail("An Exception should have been thrown");
        } catch (ReportFileException e) {
            assertEquals(Problem.DATE_RESET, e.getProblem());
        }

        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(Integer.MIN_VALUE, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(0, instance.getCalibrations().size());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

    @Test
    public void testWithoutDate() throws Exception {
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.WRITE);
        writer.write(
                "2017-02-11 18D30000 96 BF DC 40 50 42 54 35 4D 50 52 33 20 20 20 20 20 20 20 20 20 20 20 20"
                        + NL);
        writer.write("  Time Since DTCs Cleared:                      14 minutes" + NL);
        writer.write("2017-02-11 Vehicle Identification from Engine #1 (0): ASDFGHJKLASDFGHJKL" + NL);
        writer.close();
        try {
            instance.setReportFile(listener, file, false);
            fail("An exception should have been thrown");
        } catch (ReportFileException e) {
            assertEquals(Problem.DATE_NOT_PRESENT, e.getProblem());
        }
        assertEquals(false, instance.isNewFile());
        assertEquals(null, instance.getFileVin());
        assertEquals(Integer.MIN_VALUE, instance.getMinutesSinceCodeClear(), 0.0001);
        assertEquals(0, instance.getCalibrations().size());
        assertEquals(null, instance.getInitialMonitorsTime());
        assertEquals(0, instance.getInitialMonitors().size());
        assertEquals(null, instance.getInitialRatiosTime());
        assertEquals(0, instance.getInitialRatios().size());
        assertEquals(Integer.MIN_VALUE, instance.getInitialIgnitionCycles());
        assertEquals(Integer.MIN_VALUE, instance.getInitialOBDCounts());
    }

}
