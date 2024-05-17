/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.controllers;

import static net.soliddesign.iumpr.controllers.Controller.Ending.ABORTED;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JOptionPane;

import org.etools.j1939tools.modules.DateTimeModule;

import net.soliddesign.iumpr.modules.BannerModule;
import net.soliddesign.iumpr.modules.BannerModule.Type;
import net.soliddesign.iumpr.modules.ComparisonModule;
import net.soliddesign.iumpr.modules.DTCModule;
import net.soliddesign.iumpr.modules.DiagnosticReadinessModule;
import net.soliddesign.iumpr.modules.EngineSpeedModule;
import net.soliddesign.iumpr.modules.NoxBinningGhgTrackingModule;
import net.soliddesign.iumpr.modules.VehicleInformationModule;

/**
 * The {@link Controller} that Collects the Vehicle Information and write the
 * Data Plate to the report. This corresponds to Function B in the process
 * document
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DataPlateController extends Controller {

    private final DTCModule dtcModule;

    private final NoxBinningGhgTrackingModule noxBinningGhgTrackingModule;

    /**
     * Constructor
     */
    public DataPlateController() {
        this(Executors.newSingleThreadScheduledExecutor(), new EngineSpeedModule(), new BannerModule(Type.DATA_PLATE),
                DateTimeModule.getInstance(), new VehicleInformationModule(), new DiagnosticReadinessModule(), new DTCModule(),
                new ComparisonModule(), new NoxBinningGhgTrackingModule());
    }

    /**
     * Constructor exposed for testing
     *
     * @param executor
     *            the {@link ScheduledExecutorService}
     * @param engineSpeedModule
     *            the {@link EngineSpeedModule}
     * @param bannerModule
     *            the {@link BannerModule}
     * @param dateTimeModule
     *            the {@link DateTimeModule}
     * @param vehicleInformationModule
     *            the {@link VehicleInformationModule}
     * @param diagnosticReadinessModule
     *            the {@link DiagnosticReadinessModule}
     * @param dtcModule
     *            the {@link DTCModule}
     * @param comparisonModule
     *            the {@link ComparisonModule}
     */
    public DataPlateController(ScheduledExecutorService executor, EngineSpeedModule engineSpeedModule,
            BannerModule bannerModule, DateTimeModule dateTimeModule, VehicleInformationModule vehicleInformationModule,
            DiagnosticReadinessModule diagnosticReadinessModule, DTCModule dtcModule,
            ComparisonModule comparisonModule, NoxBinningGhgTrackingModule noxBinningGhgTrackingModule) {
        super(executor, engineSpeedModule, bannerModule, dateTimeModule, vehicleInformationModule,
                diagnosticReadinessModule, comparisonModule);
        this.dtcModule = dtcModule;
        this.noxBinningGhgTrackingModule = noxBinningGhgTrackingModule;
    }

    private NoxBinningGhgTrackingModule getNoxBinningHghTrackingModule() {
        return noxBinningGhgTrackingModule;
    }

    @Override
    protected int getTotalSteps() {
        return 25 + 4; // +4 is for the compareToVehicle;
    }

    @Override
    protected void run() throws Throwable {
        dtcModule.setJ1939(getJ1939());
        getNoxBinningHghTrackingModule().setJ1939(getJ1939());

        // Step 1 is handled in the super

        // Step 2 is done in the UI
        // Step 3 is handled by the adapter
        // Step 4 we are assuming SA0 is Function0
        // Steps 5 - 11 is done by the the super

        // Steps 12 & 13
        incrementProgress("Generating Header");
        getBannerModule().reportHeader(getListener());

        // Step 14A - File name and access
        addBlankLineToReport();
        incrementProgress("Gathering File Information");
        getReportFileModule().reportFileInformation(getListener());

        // Step 14B - Connection Speed
        addBlankLineToReport();
        incrementProgress("Reading Connection Speed");
        getVehicleInformationModule().reportConnectionSpeed(getListener());

        // Steps 15-17 - Address Claim
        addBlankLineToReport();
        incrementProgress("Address Claim");
        getVehicleInformationModule().reportAddressClaim(getListener());

        // Steps 18/19
        addBlankLineToReport();
        incrementProgress("Requesting VIN");
        getVehicleInformationModule().reportVin(getListener());

        // Steps 18/19
        addBlankLineToReport();
        incrementProgress("Requesting Calibration Information");
        getVehicleInformationModule().reportCalibrationInformation(getListener());

        // Steps 20/21
        addBlankLineToReport();
        incrementProgress("Requesting DM21");
        getDiagnosticReadinessModule().reportDM21(getListener(), getReportFileModule().getMinutesSinceCodeClear());

        // Steps 22 (23 is missing?) 24
        addBlankLineToReport();
        incrementProgress("Requesting HD OBD Modules");
        List<Integer> obdModules = getDiagnosticReadinessModule().getOBDModules(getListener());
        if (obdModules.isEmpty()) {
            getListener().onMessage("No HD OBD Modules were detected.", "No HD OBD Modules",
                    JOptionPane.ERROR_MESSAGE);
            setEnding(ABORTED);
        }

        // Step 25
        addBlankLineToReport();
        incrementProgress("Requesting Component Identification");
        getVehicleInformationModule().reportComponentIdentification(getListener());

        // Step 26 we already did in Step 14
        // Steps 27 & 28 we are skipping as this was handled by the UI

        // Steps 29-33
        addBlankLineToReport();
        if (getReportFileModule().isNewFile()) {
            if (!getEngineSpeedModule().isEngineNotRunning()) {
                getListener().onUrgentMessage("Please turn the Engine OFF with Key ON.", "Adjust Key Switch",
                        JOptionPane.WARNING_MESSAGE);

                while (!getEngineSpeedModule().isEngineNotRunning()) {
                    updateProgress("Waiting for Key ON, Engine OFF...");
                    Thread.sleep(500);
                }
            }

            incrementProgress("Clearing Active Codes");
            boolean dm11Response = dtcModule.reportDM11(getListener(), obdModules);
            if (!dm11Response) {
                getListener().onMessage("The Diagnostic Trouble Codes were unable to be cleared.",
                        "Clearing DTCs Failed", JOptionPane.ERROR_MESSAGE);
                setEnding(ABORTED);
            }
        } else {
            incrementProgress("Existing file; Codes Not Cleared");
            getListener().onResult("Existing file; Codes Not Cleared");
        }

        // Step 34
        addBlankLineToReport();
        incrementProgress("Requesting DM5");
        boolean dm5Response = getDiagnosticReadinessModule().reportDM5(getListener());
        if (!dm5Response) {
            // Step 38 Abort if no response
            getListener().onMessage("There were no DM5s received.", "Communications Error",
                    JOptionPane.ERROR_MESSAGE);
            setEnding(ABORTED);
        }

        // Step 35
        addBlankLineToReport();
        incrementProgress("Requesting DM26");
        boolean dm26Response = getDiagnosticReadinessModule().reportDM26(getListener());
        if (!dm26Response) {
            // Step 38 Abort if no response
            getListener().onMessage("There were no DM26s received.", "Communications Error",
                    JOptionPane.ERROR_MESSAGE);
            setEnding(ABORTED);
        }

        // Step 36
        addBlankLineToReport();
        incrementProgress("Requesting DM20");
        boolean dm20Response = getDiagnosticReadinessModule().reportDM20(getListener());
        if (!dm20Response) {
            // Step 38 Abort if no response
            getListener().onMessage("There were no DM20s received.", "Communications Error",
                    JOptionPane.ERROR_MESSAGE);
            setEnding(ABORTED);
        }

        // Step 37 is handled automatically by our design

        // Step 39 Save DM5/DM20 - by including them in the report, they are

        boolean dtcsPresent = false;
        // Step 40
        addBlankLineToReport();
        incrementProgress("Requesting DM6");
        dtcsPresent |= dtcModule.reportDM6(getListener());

        // Step 40
        addBlankLineToReport();
        incrementProgress("Requesting DM12");
        dtcsPresent |= dtcModule.reportDM12(getListener());

        // Step 40
        addBlankLineToReport();
        incrementProgress("Requesting DM23");
        dtcsPresent |= dtcModule.reportDM23(getListener());

        // Step 40
        addBlankLineToReport();
        incrementProgress("Requesting DM28");
        dtcsPresent |= dtcModule.reportDM28(getListener());

        // Step 41-42 - Notify the user but complete report
        if (dtcsPresent) {
            getListener().onMessage("There were Diagnostic Trouble Codes reported.", "DTCs Exist",
                    JOptionPane.WARNING_MESSAGE);
        }

        // 42.1 issue #84 Insert NOX Binning and GHG Tracking Queries
        addBlankLineToReport();
        incrementProgress("Requesting NOX Binning and GHG Tracking");
        getNoxBinningHghTrackingModule().reportInformation(getListener(), obdModules);

        // Step 43
        addBlankLineToReport();
        incrementProgress("Requesting DM21");
        getDiagnosticReadinessModule().reportDM21(getListener(), getReportFileModule().getMinutesSinceCodeClear());

        // Step 44
        addBlankLineToReport();
        incrementProgress("Reading Vehicle Distance");
        getVehicleInformationModule().reportVehicleDistance(getListener());

        // Step 45
        addBlankLineToReport();
        incrementProgress("Requesting Engine Hours");
        getVehicleInformationModule().reportEngineHours(getListener());

        // Step 46
        addBlankLineToReport();
        incrementProgress("Generating Quality Information");
        getReportFileModule().reportAndResetCommunicationQuality(getListener());

        // Step 47
        getReportFileModule().setNewFile(false);

        // Step 48 is handled by the super
    }

}
