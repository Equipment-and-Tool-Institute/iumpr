/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.system;

import static net.soliddesign.iumpr.IUMPR.NL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import net.soliddesign.iumpr.BuildNumber;
import net.soliddesign.iumpr.IUMPR;
import net.soliddesign.iumpr.bus.RP1210;
import net.soliddesign.iumpr.bus.j1939.J1939;
import net.soliddesign.iumpr.controllers.CollectResultsController;
import net.soliddesign.iumpr.controllers.DataPlateController;
import net.soliddesign.iumpr.controllers.MonitorCompletionController;
import net.soliddesign.iumpr.modules.BannerModule;
import net.soliddesign.iumpr.modules.BannerModule.Type;
import net.soliddesign.iumpr.modules.ComparisonModule;
import net.soliddesign.iumpr.modules.DTCModule;
import net.soliddesign.iumpr.modules.DateTimeModule;
import net.soliddesign.iumpr.modules.DiagnosticReadinessModule;
import net.soliddesign.iumpr.modules.EngineSpeedModule;
import net.soliddesign.iumpr.modules.MonitorTrackingModule;
import net.soliddesign.iumpr.modules.OBDTestsModule;
import net.soliddesign.iumpr.modules.ReportFileModule;
import net.soliddesign.iumpr.modules.TestDateTimeModule;
import net.soliddesign.iumpr.modules.VehicleInformationModule;
import net.soliddesign.iumpr.ui.UserInterfaceController;
import net.soliddesign.iumpr.ui.help.HelpView;

/**
 * System test that exercises as much of the application as possible. It creates
 * a mock {@link TestUserInterfaceView} then interacts with a real
 * {@link UserInterfaceController} connected to a simulated engine.
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@Ignore // FIXME These need fixed after all the changes are completed
public class SystemTest {

    private UserInterfaceController controller;

    private Engine engine;

    private TestUserInterfaceView view;

    private String getFileContents(String fileName) throws IOException {
        String string = "";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(SystemTest.class.getResourceAsStream(fileName), StandardCharsets.UTF_8))) {
            string = reader.lines().filter(l -> !l.contains("File:")).collect(Collectors.joining(IUMPR.NL));
        }
        return string;
    }

    @Before
    public void setUp() throws Exception {
        IUMPR.setTesting(true);

        view = new TestUserInterfaceView();

        DateTimeModule dateTimeModule = new TestDateTimeModule();

        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(2);

        BuildNumber buildNumber = new BuildNumber() {
            @Override
            public String getVersionNumber() {
                return "0.2.3 - 2017/02/25 07:45";
            }
        };

        DiagnosticReadinessModule diagnosticReadinessModule = new DiagnosticReadinessModule(dateTimeModule);
        EngineSpeedModule engineSpeedModule = new EngineSpeedModule();
        MonitorCompletionController monitorCompletionController = new MonitorCompletionController(scheduledThreadPool,
                engineSpeedModule, new BannerModule(Type.MONITOR_LOG, dateTimeModule, buildNumber), dateTimeModule,
                new VehicleInformationModule(dateTimeModule), diagnosticReadinessModule, new ComparisonModule(),
                new MonitorTrackingModule(dateTimeModule, diagnosticReadinessModule, engineSpeedModule,
                        scheduledThreadPool));

        controller = new UserInterfaceController(view,
                new DataPlateController(Executors.newSingleThreadScheduledExecutor(), new EngineSpeedModule(),
                        new BannerModule(Type.DATA_PLATE, dateTimeModule, buildNumber), dateTimeModule,
                        new VehicleInformationModule(dateTimeModule), new DiagnosticReadinessModule(dateTimeModule),
                        new DTCModule(dateTimeModule), new ComparisonModule()),
                new CollectResultsController(Executors.newSingleThreadScheduledExecutor(), new EngineSpeedModule(),
                        new BannerModule(Type.COLLECTION_LOG, dateTimeModule, buildNumber), dateTimeModule,
                        new VehicleInformationModule(dateTimeModule), new DiagnosticReadinessModule(dateTimeModule),
                        new OBDTestsModule(dateTimeModule), new ComparisonModule()),
                monitorCompletionController, new ComparisonModule(), new RP1210(), new ReportFileModule(dateTimeModule),
                Runtime.getRuntime(), Executors.newSingleThreadExecutor(), new HelpView());
    }

    @After
    public void tearDown() throws Exception {
        IUMPR.setTesting(false);
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    public void testExistingFile() throws Exception {
        // Select the Adapter
        assertEquals(1, view.getEnabled());
        controller.onAdapterComboBoxItemSelected("Loop Back Adapter");
        // Give time for the adapter to be connected
        wait(1, 3, 3, TimeUnit.SECONDS);

        // Check enabled status and connect the engine
        J1939 j1939 = controller.getNewJ1939();
        engine = new Engine(j1939.getBus());

        // Select an existing file
        File testFile = File.createTempFile("testing", ".iumpr");
        assertTrue(testFile.delete());
        try (InputStream in = SystemTest.class.getResourceAsStream("expectedNewFile.txt")) {
            Files.copy(in, testFile.toPath());
        }
        controller.onFileChosen(testFile);
        // Give time for the file to be scanned
        wait(3, 7, 5, TimeUnit.SECONDS);

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked();
        // Wait for vehicle info to be read
        wait(7, 31, 10, TimeUnit.SECONDS);
        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        // Generate Data Plate
        controller.onGenerateDataPlateButtonClicked();
        // Wait for Generate Data Plate to end
        wait(31, 112, 60, TimeUnit.SECONDS);

        // Monitor Tracking Completion
        controller.onMonitorCompletionButtonClicked();
        // Wait of Monitor Tracking to begin
        wait(112, 16, 1, TimeUnit.SECONDS);
        // End Tracking right away
        MonitorCompletionController mcc = (MonitorCompletionController) controller.getActiveController();
        mcc.endTracking();
        // Wait for the tracking to end
        wait(16, 112, 30, TimeUnit.SECONDS);

        // Collect Test Results
        controller.onCollectTestResultsButtonClicked();
        // Wait for Collecting Test Results to begin
        wait(112, 16, 1, TimeUnit.SECONDS);
        // Wait for Collecting Test Results to complete
        wait(16, 112, 30, TimeUnit.SECONDS);

        // The line with the file is removed from the results because it's
        // variable

        // Read the expected results
        String expectedResults = getFileContents("expectedExistingFileResults.txt");

        assertEquals(expectedResults + IUMPR.NL, view.getResults());

        String fileResults = Files.lines(testFile.toPath()).filter(l -> !l.contains("File:"))
                .collect(Collectors.joining(IUMPR.NL));
        String expectedFileResults = getFileContents("expectedExistingFile.txt");
        assertEquals(expectedFileResults, fileResults);

        // Check the messages sent back to the UI
        String expectedMessages = getFileContents("expectedExistingFileMessages.txt");
        assertEquals(expectedMessages, view.getMessages());
    }

    @Test
    public void testExistingFileWithBadCal() throws Exception {
        // Select the Adapter
        assertEquals(1, view.getEnabled());
        controller.onAdapterComboBoxItemSelected("Loop Back Adapter");
        // Give time for the adapter to be connected
        wait(1, 3, 3, TimeUnit.SECONDS);

        // Check enabled status and connect the engine
        J1939 j1939 = controller.getNewJ1939();
        engine = new Engine(j1939.getBus());

        // Select a new file
        File testFile = File.createTempFile("testing", ".iumpr");
        // File cannot exist so the controller sees it as new
        assertTrue(testFile.delete());
        try (InputStream in = SystemTest.class.getResourceAsStream("invalidCal.txt")) {
            Files.copy(in, testFile.toPath());
        }
        controller.onFileChosen(testFile);
        // Give time for the file to be scanned
        wait(3, 7, 5, TimeUnit.SECONDS);

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked();
        // Wait for vehicle info to be read
        wait(7, 7, 5, TimeUnit.SECONDS);
        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        assertEquals(7, view.getEnabled());

        assertEquals("", view.getResults());

        // Check the messages sent back to the UI

        StringBuilder sb = new StringBuilder();
        sb.append("Connecting to Adapter").append(NL);
        sb.append("Select Report File").append(NL);
        for (int i = 0; i < 170; i++) {
            sb.append("Scanning Report File").append(NL);
        }
        sb.append("Push Read Vehicle Info Button").append(NL);
        sb.append("Reading VIN from Engine").append(NL);
        sb.append("Reading Vehicle Calibrations").append(NL);
        sb.append("Reading VIN from Vehicle").append(NL);
        sb.append("Reading Calibrations from Vehicle").append(NL);
        sb.append("Calibration Mismatch");
        assertEquals(sb.toString(), view.getMessages());

        String expectedDialog = "";
        expectedDialog += "The selected report file calibrations do not match the vehicle calibrations." + NL;
        expectedDialog += "" + NL;
        expectedDialog += "The Report Calibrations:" + NL;
        expectedDialog += "CAL ID of This Is Not a ca and CVN of 0x40DCBF96" + NL;
        expectedDialog += "" + NL;
        expectedDialog += "The Vehicle Calibrations:" + NL;
        expectedDialog += "CAL ID of PBT5MPR3 and CVN of 0x40DCBF96";
        assertEquals(expectedDialog, view.lastDialog);
    }

    @Test
    public void testExistingFileWithBadVin() throws Exception {
        // Select the Adapter
        assertEquals(1, view.getEnabled());
        controller.onAdapterComboBoxItemSelected("Loop Back Adapter");
        // Give time for the adapter to be connected
        wait(1, 3, 3, TimeUnit.SECONDS);

        // Check enabled status and connect the engine
        J1939 j1939 = controller.getNewJ1939();
        engine = new Engine(j1939.getBus());

        // Select a new file
        File testFile = File.createTempFile("testing", ".iumpr");
        // File cannot exist so the controller sees it as new
        assertTrue(testFile.delete());
        try (InputStream in = SystemTest.class.getResourceAsStream("invalidVin.txt")) {
            Files.copy(in, testFile.toPath());
        }
        controller.onFileChosen(testFile);
        // Give time for the file to be scanned
        wait(3, 7, 5, TimeUnit.SECONDS);

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked();
        // Wait for vehicle info to be read
        wait(7, 7, 5, TimeUnit.SECONDS);
        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        assertEquals(7, view.getEnabled());

        assertEquals("", view.getResults());

        // Check the messages sent back to the UI

        StringBuilder sb = new StringBuilder();
        sb.append("Connecting to Adapter").append(NL);
        sb.append("Select Report File").append(NL);
        for (int i = 0; i < 170; i++) {
            sb.append("Scanning Report File").append(NL);
        }
        sb.append("Push Read Vehicle Info Button").append(NL);
        sb.append("Reading VIN from Engine").append(NL);
        sb.append("Reading Vehicle Calibrations").append(NL);
        sb.append("Reading VIN from Vehicle").append(NL);
        sb.append("VIN Mismatch");
        assertEquals(sb.toString(), view.getMessages());

        assertEquals(
                "The VIN found in the selected report file (THIS_is_An_inVALid_vin) does not match the VIN read from the vehicle (3HAMKSTN0FL575012).",
                view.lastDialog);
    }

    @Test
    public void testExistingFileWithGapTSCC() throws Exception {
        // Select the Adapter
        assertEquals(1, view.getEnabled());
        controller.onAdapterComboBoxItemSelected("Loop Back Adapter");
        // Give time for the adapter to be connected
        wait(1, 3, 3, TimeUnit.SECONDS);

        // Check enabled status and connect the engine
        J1939 j1939 = controller.getNewJ1939();
        engine = new Engine(j1939.getBus());

        // Select a new file
        File testFile = File.createTempFile("testing", ".iumpr");
        // File cannot exist so the controller sees it as new
        assertTrue(testFile.delete());
        try (InputStream in = SystemTest.class.getResourceAsStream("tsccGap.txt")) {
            Files.copy(in, testFile.toPath());
        }
        controller.onFileChosen(testFile);
        // Give time for the file to be scanned
        wait(3, 7, 5, TimeUnit.SECONDS);

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked();
        // Wait for vehicle info to be read
        wait(7, 31, 5, TimeUnit.SECONDS);
        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        assertEquals("", view.getResults());

        // Check the messages sent back to the UI

        StringBuilder sb = new StringBuilder();
        sb.append("Connecting to Adapter").append(NL);
        sb.append("Select Report File").append(NL);
        for (int i = 0; i < 170; i++) {
            sb.append("Scanning Report File").append(NL);
        }
        sb.append("Push Read Vehicle Info Button").append(NL);
        sb.append("Reading VIN from Engine").append(NL);
        sb.append("Reading Vehicle Calibrations").append(NL);
        sb.append("Reading VIN from Vehicle").append(NL);
        sb.append("Reading Calibrations from Vehicle").append(NL);
        sb.append("Reading Time Since Code Cleared from Vehicle").append(NL);
        sb.append("Selected Report File Matches Connected Vehicle").append(NL);
        sb.append("Push Generate Vehicle Data Plate Button");

        assertEquals(sb.toString(), view.getMessages());

        assertEquals("The Time Since Code Cleared has an excessive gap of 13056 minutes.", view.lastDialog);
    }

    @Test
    public void testExistingFileWithResetTSCC() throws Exception {
        // Select the Adapter
        assertEquals(1, view.getEnabled());
        controller.onAdapterComboBoxItemSelected("Loop Back Adapter");
        // Give time for the adapter to be connected
        wait(1, 3, 3, TimeUnit.SECONDS);

        // Check enabled status and connect the engine
        J1939 j1939 = controller.getNewJ1939();
        engine = new Engine(j1939.getBus());

        // Select a new file
        File testFile = File.createTempFile("testing", ".iumpr");
        // File cannot exist so the controller sees it as new
        assertTrue(testFile.delete());
        try (InputStream in = SystemTest.class.getResourceAsStream("tsccReset.txt")) {
            Files.copy(in, testFile.toPath());
        }

        controller.onFileChosen(testFile);
        // Give time for the file to be scanned
        wait(3, 7, 5, TimeUnit.SECONDS);

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked();
        // Wait for vehicle info to be read
        wait(7, 31, 5, TimeUnit.SECONDS);
        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        assertEquals("", view.getResults());

        // Check the messages sent back to the UI

        StringBuilder sb = new StringBuilder();
        sb.append("Connecting to Adapter").append(NL);
        sb.append("Select Report File").append(NL);
        for (int i = 0; i < 170; i++) {
            sb.append("Scanning Report File").append(NL);
        }
        sb.append("Push Read Vehicle Info Button").append(NL);
        sb.append("Reading VIN from Engine").append(NL);
        sb.append("Reading Vehicle Calibrations").append(NL);
        sb.append("Reading VIN from Vehicle").append(NL);
        sb.append("Reading Calibrations from Vehicle").append(NL);
        sb.append("Reading Time Since Code Cleared from Vehicle").append(NL);
        sb.append("Selected Report File Matches Connected Vehicle").append(NL);
        sb.append("Push Generate Vehicle Data Plate Button");
        assertEquals(sb.toString(), view.getMessages());

        assertEquals("The Time Since Code Cleared was reset. The difference is 17408 minutes.", view.lastDialog);
    }

    @Test
    public void testNewFile() throws Exception {
        // Select the Adapter
        assertEquals(1, view.getEnabled());
        controller.onAdapterComboBoxItemSelected("Loop Back Adapter");
        // Give time for the adapter to be connected
        wait(1, 3, 3, TimeUnit.SECONDS);

        // Check enabled status and connect the engine
        J1939 j1939 = controller.getNewJ1939();
        engine = new Engine(j1939.getBus());

        // Select a new file
        File testFile = File.createTempFile("testing", ".iumpr");
        // File cannot exist so the controller sees it as new
        assertTrue(testFile.delete());
        controller.onFileChosen(testFile);
        // Give time for the file to be scanned
        wait(3, 7, 5, TimeUnit.SECONDS);

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked();
        // Wait for vehicle info to be read
        wait(7, 31, 10, TimeUnit.SECONDS);
        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        // Generate Data Plate
        controller.onGenerateDataPlateButtonClicked();
        // Wait for Generate Data Plate to end
        wait(31, 112, 60, TimeUnit.SECONDS);

        // Monitor Tracking Completion
        controller.onMonitorCompletionButtonClicked();
        // Wait of Monitor Tracking to begin
        wait(112, 16, 1, TimeUnit.SECONDS);

        assertTrue(view.statusViewVisible);
        // Let the tracking run through one cycle
        Thread.sleep(TimeUnit.MINUTES.toMillis(4));

        // End Tracking
        MonitorCompletionController mcc = (MonitorCompletionController) controller.getActiveController();
        mcc.endTracking();
        // Wait for the tracking to end
        wait(16, 112, 30, TimeUnit.SECONDS);

        // Collect Test Results
        controller.onCollectTestResultsButtonClicked();
        // Wait for Collecting Test Results to begin
        wait(112, 16, 1, TimeUnit.SECONDS);
        // Wait for Collecting Test Results to complete
        wait(16, 112, 30, TimeUnit.SECONDS);

        // The line with the file is removed from the results because it's
        // variable

        // Read the expected results
        String expectedResults = getFileContents("expectedNewFile.txt");

        assertEquals(expectedResults + IUMPR.NL, view.getResults());

        String fileResults = Files.lines(testFile.toPath()).filter(l -> !l.contains("File:"))
                .collect(Collectors.joining(IUMPR.NL));
        assertEquals(expectedResults, fileResults);

        // Check the messages sent back to the UI
        String expectedMessages = getFileContents("expectedNewFileMessages.txt");
        assertEquals(expectedMessages, view.getMessages());
    }

    @Test
    public void testStoppingGenerateDataPlate() throws Exception {
        // Select the Adapter
        assertEquals(1, view.getEnabled());
        controller.onAdapterComboBoxItemSelected("Loop Back Adapter");
        // Give time for the adapter to be connected
        wait(1, 3, 3, TimeUnit.SECONDS);

        // Check enabled status and connect the engine
        J1939 j1939 = controller.getNewJ1939();
        engine = new Engine(j1939.getBus());

        // Select a new file
        File testFile = File.createTempFile("testing", ".iumpr");
        // File cannot exist so the controller sees it as new
        assertTrue(testFile.delete());
        controller.onFileChosen(testFile);
        // Give time for the file to be scanned
        wait(3, 7, 5, TimeUnit.SECONDS);

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked();
        // Wait for vehicle info to be read
        wait(7, 31, 10, TimeUnit.SECONDS);
        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        // Generate Data Plate
        controller.onGenerateDataPlateButtonClicked();
        // Wait for Generate Data Plate to begin
        wait(31, 16, 60, TimeUnit.SECONDS);

        Thread.sleep(10000);

        controller.onStopButtonClicked();
        wait(16, 28, 3, TimeUnit.SECONDS);

        // The line with the file is removed from the results because it's
        // variable

        String expectedResults = getFileContents("expectedNewFileStopped.txt");

        assertEquals(expectedResults + IUMPR.NL, view.getResults());

        String fileResults = Files.lines(testFile.toPath()).filter(l -> !l.contains("File:"))
                .collect(Collectors.joining(IUMPR.NL));
        assertEquals(expectedResults, fileResults);

        // Check the messages sent back to the UI
        String expectedMessages = getFileContents("expectedNewFileMessagesStopped.txt");
        assertEquals(expectedMessages, view.getMessages());
    }

    /**
     * Helper method to wait on the UI changes
     *
     * @param startingValue
     *            the enabled mask that the UI will be at when the wait starts.
     * @param endingValue
     *            the enabled mask that the UI is expected to be at to end the
     *            wait
     * @param timeout
     *            the maximum time to wait for the UI to change
     * @param timeUnit
     *            the {@link TimeUnit} of the timeout
     * @throws InterruptedException
     *             if the sleep is interrupted
     */
    private void wait(int startingValue, int endingValue, long timeout, TimeUnit timeUnit) throws InterruptedException {
        long maxTime = timeUnit.toMillis(timeout);
        long start = System.currentTimeMillis();

        while (view.getEnabled() == startingValue) {
            if (System.currentTimeMillis() - start > maxTime) {
                fail("no response.  final value was " + view.getEnabled());
            }
            Thread.sleep(100);
        }

        while (view.getEnabled() != endingValue) {
            if (System.currentTimeMillis() - start > maxTime) {
                fail("no response.  final value was " + view.getEnabled());
            }
            Thread.sleep(100);
        }
    }
}
