/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.controllers;

import static net.soliddesign.iumpr.controllers.Controller.Ending.ABORTED;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import net.soliddesign.iumpr.bus.j1939.packets.DM20MonitorPerformanceRatioPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM5DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem;
import net.soliddesign.iumpr.bus.j1939.packets.PerformanceRatio;
import net.soliddesign.iumpr.modules.BannerModule;
import net.soliddesign.iumpr.modules.BannerModule.Type;
import net.soliddesign.iumpr.modules.ComparisonModule;
import net.soliddesign.iumpr.modules.DateTimeModule;
import net.soliddesign.iumpr.modules.DiagnosticReadinessModule;
import net.soliddesign.iumpr.modules.EngineSpeedModule;
import net.soliddesign.iumpr.modules.OBDTestsModule;
import net.soliddesign.iumpr.modules.VehicleInformationModule;

/**
 * The {@link Controller} that Collects the Tests Results from the vehicle
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class CollectResultsController extends Controller {

    private OBDTestsModule obdTestsModule;

    /**
     * Constructor
     */
    public CollectResultsController() {
        this(Executors.newSingleThreadScheduledExecutor(), new EngineSpeedModule(),
                new BannerModule(Type.COLLECTION_LOG), new DateTimeModule(), new VehicleInformationModule(),
                new DiagnosticReadinessModule(), new OBDTestsModule(), new ComparisonModule());
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
     * @param obdTestsModule
     *            the {@link OBDTestsModule}
     * @param comparisonModule
     *            the {@link ComparisonModule}
     */
    public CollectResultsController(ScheduledExecutorService executor, EngineSpeedModule engineSpeedModule,
            BannerModule bannerModule, DateTimeModule dateTimeModule, VehicleInformationModule vehicleInformationModule,
            DiagnosticReadinessModule diagnosticReadinessModule, OBDTestsModule obdTestsModule,
            ComparisonModule comparisonModule) {
        super(executor, engineSpeedModule, bannerModule, dateTimeModule, vehicleInformationModule,
                diagnosticReadinessModule, comparisonModule);
        this.obdTestsModule = obdTestsModule;
    }

    @Override
    protected int getTotalSteps() {
        return 17 + 4; // +4 is for the compareToVehicle
    }

    @Override
    protected void run() throws Throwable {
        obdTestsModule.setJ1939(getJ1939());

        // This is "Function E"

        // Step 1 handled in super
        // Step 2 handled in super
        // Step 3 handled in super

        // Steps 4 & 5
        incrementProgress("Generating Header");
        getBannerModule().reportHeader(getListener());

        // Step 6
        addBlankLineToReport();
        incrementProgress("Requesting VIN");
        getVehicleInformationModule().reportVin(getListener());

        // Step 7
        addBlankLineToReport();
        incrementProgress("Requesting Calibration Information");
        getVehicleInformationModule().reportCalibrationInformation(getListener());

        // Steps 8 & 9
        addBlankLineToReport();
        incrementProgress("Requesting DM21");
        getDiagnosticReadinessModule().reportDM21(getListener(), getReportFileModule().getMinutesSinceCodeClear());

        addBlankLineToReport();
        incrementProgress("Requesting HD OBD Modules");
        List<Integer> obdModules = getDiagnosticReadinessModule().getOBDModules(getListener());
        if (obdModules.isEmpty()) {
            setEnding(ABORTED);
        }

        // Steps 10-29
        addBlankLineToReport();
        incrementProgress("Requesting OBD Test Results");
        obdTestsModule.reportOBDTests(getListener(), obdModules);

        // Step 30
        addBlankLineToReport();
        incrementProgress("Requesting DM5");
        List<DM5DiagnosticReadinessPacket> dm5Packets = getDiagnosticReadinessModule().getDM5Packets(getListener(),
                false);
        addBlankLineToReport();
        Set<MonitoredSystem> systems = DiagnosticReadinessModule.getSystems(dm5Packets);
        String initialMonitorsTime = getDateTimeModule().format(getReportFileModule().getInitialMonitorsTime());
        getDiagnosticReadinessModule().reportMonitoredSystems(getListener(), getReportFileModule().getInitialMonitors(),
                systems, initialMonitorsTime, getDateTime());

        // Step 31
        addBlankLineToReport();
        incrementProgress("Requesting DM26");
        getDiagnosticReadinessModule().reportDM26(getListener());

        // Step 32
        addBlankLineToReport();
        incrementProgress("Requesting DM20");
        List<DM20MonitorPerformanceRatioPacket> dm20Packets = getDiagnosticReadinessModule()
                .getDM20Packets(getListener(), false);
        addBlankLineToReport();
        Set<PerformanceRatio> ratios = DiagnosticReadinessModule.getRatios(dm20Packets);
        int ignitionCycles = DiagnosticReadinessModule.getIgnitionCycles(dm20Packets);
        int obdCounts = DiagnosticReadinessModule.getOBDCounts(dm20Packets);
        String initialRatiosTime = getDateTimeModule().format(getReportFileModule().getInitialRatiosTime());
        getDiagnosticReadinessModule().reportPerformanceRatios(getListener(), getReportFileModule().getInitialRatios(),
                ratios, getReportFileModule().getInitialIgnitionCycles(), ignitionCycles,
                getReportFileModule().getInitialOBDCounts(), obdCounts, initialRatiosTime, getDateTime());

        // Step 33 Store DM5 and DM20
        // Don't need to do this; they are stored in the report file

        // Step 34
        addBlankLineToReport();
        incrementProgress("Requesting DM21");
        getDiagnosticReadinessModule().reportDM21(getListener(), getReportFileModule().getMinutesSinceCodeClear());

        // Step 35
        addBlankLineToReport();
        incrementProgress("Reading Vehicle Distance");
        getVehicleInformationModule().reportVehicleDistance(getListener());

        // Step 36
        addBlankLineToReport();
        incrementProgress("Requesting Engine Hours");
        getVehicleInformationModule().reportEngineHours(getListener());

        // Step 37A
        addBlankLineToReport();
        incrementProgress("Requesting VIN");
        getVehicleInformationModule().reportVin(getListener());

        // Step 37B
        addBlankLineToReport();
        incrementProgress("Requesting Calibration Information");
        getVehicleInformationModule().reportCalibrationInformation(getListener());

        // Step 38
        addBlankLineToReport();
        incrementProgress("Generating Quality Information");
        getReportFileModule().reportAndResetCommunicationQuality(getListener());
        getReportFileModule().reportQuality(getListener());

        // Step 39 handled in the super
    }

}
