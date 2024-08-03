/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.system;

import static net.soliddesign.iumpr.IUMPR.NL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.etools.j1939tools.bus.RP1210;
import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.modules.DateTimeModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.soliddesign.iumpr.BuildNumber;
import net.soliddesign.iumpr.IUMPR;
import net.soliddesign.iumpr.controllers.CollectResultsController;
import net.soliddesign.iumpr.controllers.DataPlateController;
import net.soliddesign.iumpr.controllers.MonitorCompletionController;
import net.soliddesign.iumpr.modules.BannerModule;
import net.soliddesign.iumpr.modules.BannerModule.Type;
import net.soliddesign.iumpr.modules.ComparisonModule;
import net.soliddesign.iumpr.modules.DTCModule;
import net.soliddesign.iumpr.modules.DiagnosticReadinessModule;
import net.soliddesign.iumpr.modules.EngineSpeedModule;
import net.soliddesign.iumpr.modules.MonitorTrackingModule;
import net.soliddesign.iumpr.modules.NoxBinningGhgTrackingModule;
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

        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(2,
                r -> new Thread(r, "SystemTest"));

        BuildNumber buildNumber = new BuildNumber() {
            @Override
            public String getVersionNumber() {
                return "0.2.3 - 2017/02/25 07:45";
            }
        };

        DiagnosticReadinessModule diagnosticReadinessModule = new DiagnosticReadinessModule(dateTimeModule);
        EngineSpeedModule engineSpeedModule = new EngineSpeedModule();
        MonitorCompletionController monitorCompletionController = new MonitorCompletionController(scheduledThreadPool,
                engineSpeedModule,
                new BannerModule(Type.MONITOR_LOG, dateTimeModule, buildNumber),
                dateTimeModule,
                new VehicleInformationModule(dateTimeModule),
                diagnosticReadinessModule,
                new ComparisonModule(),
                new MonitorTrackingModule(dateTimeModule,
                        diagnosticReadinessModule,
                        engineSpeedModule,
                        scheduledThreadPool));

        controller = new UserInterfaceController(view,
                new DataPlateController(Executors.newSingleThreadScheduledExecutor(),
                        new EngineSpeedModule(),
                        new BannerModule(Type.DATA_PLATE, dateTimeModule, buildNumber),
                        dateTimeModule,
                        new VehicleInformationModule(dateTimeModule),
                        new DiagnosticReadinessModule(dateTimeModule),
                        new DTCModule(dateTimeModule), new ComparisonModule(), new NoxBinningGhgTrackingModule()),
                new CollectResultsController(Executors.newSingleThreadScheduledExecutor(),
                        new EngineSpeedModule(),
                        new BannerModule(Type.COLLECTION_LOG, dateTimeModule, buildNumber),
                        dateTimeModule,
                        new VehicleInformationModule(dateTimeModule),
                        new DiagnosticReadinessModule(dateTimeModule),
                        new OBDTestsModule(dateTimeModule),
                        new ComparisonModule(), new NoxBinningGhgTrackingModule()),
                monitorCompletionController,
                new ComparisonModule(),
                new RP1210(),
                new ReportFileModule(dateTimeModule, IUMPR.getLogger()),
                Runtime.getRuntime(),
                Executors.newSingleThreadExecutor(),
                new HelpView());
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
        controller.onAdapterComboBoxItemSelected(RP1210.LOOP_BACK_ADAPTER);

        // Select an existing file
        File testFile = File.createTempFile("testing", ".iumpr");
        assertTrue(testFile.delete());
        try (InputStream in = SystemTest.class.getResourceAsStream("expectedNewFile.txt")) {
            Files.copy(in, testFile.toPath());
        }
        controller.onFileChosen(testFile).join();

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked().join();
        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        // Generate Data Plate
        controller.onGenerateDataPlateButtonClicked().join();

        // Monitor Tracking Completion
        controller.onMonitorCompletionButtonClicked();
        Thread.sleep(2000);
        // End Tracking right away
        ((MonitorCompletionController) controller.getActiveController()).endTracking();

        // Collect Test Results
        controller.onCollectTestResultsButtonClicked().join();

        // Read the expected results
        String expectedResults = getFileContents("expectedExistingFileResults.txt");
        assertEquals((expectedResults + IUMPR.NL).replace(" ", ""), view.getResults().replace(" ", ""));

        String fileResults = Files.lines(testFile.toPath()).filter(l -> !l.contains("File:"))
                .collect(Collectors.joining(IUMPR.NL));
        String expectedFileResults = getFileContents("expectedExistingFile.txt");
        assertEquals((expectedFileResults + IUMPR.NL).replace(" ", ""), fileResults.replace(" ", ""));

        // Check the messages sent back to the UI
        String expectedMessages = getFileContents("expectedExistingFileMessages.txt");
        assertEquals(expectedMessages, view.getMessages());
    }

    @Test
    public void testExistingFileWithBadCal() throws Exception {
        // Select the Adapter
        assertEquals(1, view.getEnabled());
        controller.onAdapterComboBoxItemSelected(RP1210.LOOP_BACK_ADAPTER);

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
        controller.onFileChosen(testFile).join();

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked().join();

        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        assertEquals(7, view.getEnabled());

        assertEquals("", view.getResults());

        // Check the messages sent back to the UI

        StringBuilder sb = new StringBuilder();
        // sb.append("Select Report File").append(NL);
        for (int i = 0; i < 170; i++) {
            sb.append("Scanning Report File").append(NL);
        }
        sb.append("Push Read Vehicle Info Button").append(NL);
        sb.append("Connecting RP1210").append(NL);
        sb.append("Reading Vehicle Identification Number").append(NL);
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
        controller.onAdapterComboBoxItemSelected(RP1210.LOOP_BACK_ADAPTER);

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
        controller.onFileChosen(testFile).join();

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked().join();
        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        assertEquals(7, view.getEnabled());

        assertEquals("", view.getResults());

        // Check the messages sent back to the UI

        StringBuilder sb = new StringBuilder();
        // sb.append("Select Report File").append(NL);
        for (int i = 0; i < 170; i++) {
            sb.append("Scanning Report File").append(NL);
        }
        sb.append("Push Read Vehicle Info Button").append(NL);
        sb.append("Connecting RP1210").append(NL);
        sb.append("Reading Vehicle Identification Number").append(NL);
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
        controller.onAdapterComboBoxItemSelected(RP1210.LOOP_BACK_ADAPTER);

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
        controller.onFileChosen(testFile).join();

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked().join();
        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        assertEquals("", view.getResults());

        // Check the messages sent back to the UI

        StringBuilder sb = new StringBuilder();
        // sb.append("Select Report File").append(NL);
        for (int i = 0; i < 170; i++) {
            sb.append("Scanning Report File").append(NL);
        }
        sb.append("Push Read Vehicle Info Button").append(NL);
        sb.append("Connecting RP1210").append(NL);
        sb.append("Reading Vehicle Identification Number").append(NL);
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
        controller.onAdapterComboBoxItemSelected(RP1210.LOOP_BACK_ADAPTER);

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

        controller.onFileChosen(testFile).join();

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked().join();
        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        assertEquals("", view.getResults());

        // Check the messages sent back to the UI

        StringBuilder sb = new StringBuilder();
        // sb.append("Select Report File").append(NL);
        for (int i = 0; i < 170; i++) {
            sb.append("Scanning Report File").append(NL);
        }
        sb.append("Push Read Vehicle Info Button").append(NL);
        sb.append("Connecting RP1210").append(NL);
        sb.append("Reading Vehicle Identification Number").append(NL);
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
        controller.onAdapterComboBoxItemSelected(RP1210.LOOP_BACK_ADAPTER);

        // Check enabled status and connect the engine
        J1939 j1939 = controller.getNewJ1939();
        engine = new Engine(j1939.getBus());

        // Select a new file
        File testFile = File.createTempFile("testing", ".iumpr");
        // File cannot exist so the controller sees it as new
        assertTrue(testFile.delete());
        controller.onFileChosen(testFile).join();

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked().join();
        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        // Generate Data Plate
        controller.onGenerateDataPlateButtonClicked().join();

        // Monitor Tracking Completion
        controller.onMonitorCompletionButtonClicked();

        while (!view.statusViewVisible) {
            Thread.sleep(100);
        }
        // Let the tracking run through one cycle
        Thread.sleep(TimeUnit.SECONDS.toMillis(60));

        // End Tracking
        MonitorCompletionController mcc = (MonitorCompletionController) controller.getActiveController();
        mcc.endTracking();

        // Collect Test Results
        controller.onCollectTestResultsButtonClicked().join();

        // The line with the file is removed from the results because it's
        // variable

        // Read the expected results
        String expectedResults = getFileContents("expectedNewFile.txt");
        assertEquals((expectedResults + IUMPR.NL).replace(" ", ""), view.getResults().replace(" ", ""));

        String fileResults = Files.lines(testFile.toPath()).filter(l -> !l.contains("File:"))
                .collect(Collectors.joining(IUMPR.NL));
        assertEquals(expectedResults.replace(" ", ""), fileResults.replace(" ", ""));

        // Check the messages sent back to the UI
        String expectedMessages = getFileContents("expectedNewFileMessages.txt");
        assertEquals(expectedMessages, view.getMessages());
    }

    @Test
    public void testStoppingGenerateDataPlate() throws Exception {
        // Select the Adapter
        assertEquals(1, view.getEnabled());
        controller.onAdapterComboBoxItemSelected(RP1210.LOOP_BACK_ADAPTER);

        // Check enabled status and connect the engine
        J1939 j1939 = controller.getNewJ1939();
        engine = new Engine(j1939.getBus());

        // Select a new file
        File testFile = File.createTempFile("testing", ".iumpr");
        // File cannot exist so the controller sees it as new
        assertTrue(testFile.delete());
        controller.onFileChosen(testFile).join();

        // Read Vehicle Information
        controller.onReadVehicleInfoButtonClicked().join();
        assertEquals("3HAMKSTN0FL575012", view.vin);
        assertEquals("CAL ID of PBT5MPR3 and CVN of 0x40DCBF96", view.cals);

        // Generate Data Plate
        controller.onGenerateDataPlateButtonClicked().join();

        Thread.sleep(10000);

        controller.onStopButtonClicked();

        // The line with the file is removed from the results because it's
        // variable

        String expectedResults = getFileContents("expectedNewFileStopped.txt");

        assertEquals(expectedResults.replace(" ", "") + IUMPR.NL, view.getResults().replace(" ", ""));

        String fileResults = Files.lines(testFile.toPath()).filter(l -> !l.contains("File:"))
                .collect(Collectors.joining(IUMPR.NL));
        assertEquals(expectedResults.replace(" ", ""), fileResults.replace(" ", ""));

        // Check the messages sent back to the UI
        String expectedMessages = getFileContents("expectedNewFileMessagesStopped.txt");
        assertEquals(expectedMessages, view.getMessages());
    }
}
