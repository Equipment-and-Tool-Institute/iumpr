/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.controllers;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem;
import net.soliddesign.iumpr.bus.j1939.packets.PerformanceRatio;
import net.soliddesign.iumpr.modules.BannerModule;
import net.soliddesign.iumpr.modules.BannerModule.Type;
import net.soliddesign.iumpr.modules.ComparisonModule;
import net.soliddesign.iumpr.modules.DateTimeModule;
import net.soliddesign.iumpr.modules.DiagnosticReadinessModule;
import net.soliddesign.iumpr.modules.EngineSpeedModule;
import net.soliddesign.iumpr.modules.MonitorTrackingModule;
import net.soliddesign.iumpr.modules.VehicleInformationModule;

/**
 * The {@link Controller} that tracks monitors of vehicle to determine if it has
 * completed any diagnostic tests
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class MonitorCompletionController extends Controller {

    private final MonitorTrackingModule monitorTrackingModule;

    /**
     * Constructor
     */
    public MonitorCompletionController() {
        this(Executors.newScheduledThreadPool(2), new EngineSpeedModule(), new BannerModule(Type.MONITOR_LOG),
                new DateTimeModule(), new VehicleInformationModule(), new DiagnosticReadinessModule(),
                new ComparisonModule());
    }

    /**
     * Constructor needed to pass values to {@link MonitorTrackingModule}
     */
    private MonitorCompletionController(ScheduledExecutorService executor, EngineSpeedModule engineSpeedModule,
            BannerModule bannerModule, DateTimeModule dateTimeModule, VehicleInformationModule vehicleInformationModule,
            DiagnosticReadinessModule diagnosticReadinessModule, ComparisonModule comparisonModule) {
        this(executor, engineSpeedModule, bannerModule, dateTimeModule, vehicleInformationModule,
                diagnosticReadinessModule, comparisonModule,
                new MonitorTrackingModule(dateTimeModule, diagnosticReadinessModule, engineSpeedModule, executor));
    }

    /**
     * Constructor exposed for testing
     *
     * @param executor
     *            the {@link ScheduledExecutorService} For production this needs
     *            to have at least two threads
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
     * @param comparisonModule
     *            the {@link ComparisonModule}
     * @param monitorTrackingModule
     *            the {@link MonitorTrackingModule}
     */
    public MonitorCompletionController(ScheduledExecutorService executor, EngineSpeedModule engineSpeedModule,
            BannerModule bannerModule, DateTimeModule dateTimeModule, VehicleInformationModule vehicleInformationModule,
            DiagnosticReadinessModule diagnosticReadinessModule, ComparisonModule comparisonModule,
            MonitorTrackingModule monitorTrackingModule) {
        super(executor, engineSpeedModule, bannerModule, dateTimeModule, vehicleInformationModule,
                diagnosticReadinessModule, comparisonModule);
        this.monitorTrackingModule = monitorTrackingModule;
    }

    /**
     * Ends the Tracking Monitor Completion
     */
    public void endTracking() {
        getMonitorTrackingModule().endTracking();
    }

    @Override
    protected void finished() {
        // This MUST be reset at the end of the process.
        getMonitorTrackingModule().resetEndFlag();
        super.finished();
    }

    private MonitorTrackingModule getMonitorTrackingModule() {
        return monitorTrackingModule;
    }

    @Override
    protected int getTotalSteps() {
        return 13 + 4; // +4 is for the compareToVehicle
    }

    @Override
    protected void run() throws Throwable {
        getMonitorTrackingModule().setJ1939(getJ1939());

        // Function C Steps
        // Step 1 handled in the super
        // Step 2 doesn't need to happen/handled in the super
        // Step 3 handled in the super
        // Steps 4 - 9 handled in the super

        // Steps 9 & 10
        incrementProgress("Generating Header");
        getBannerModule().reportHeader(getListener());

        // Step 11A
        addBlankLineToReport();
        incrementProgress("Requesting VIN");
        getVehicleInformationModule().reportVin(getListener());

        // Step 11B
        addBlankLineToReport();
        incrementProgress("Requesting Calibration Information");
        getVehicleInformationModule().reportCalibrationInformation(getListener());

        // Steps 12-13
        addBlankLineToReport();
        incrementProgress("Requesting DM21");
        getDiagnosticReadinessModule().reportDM21(getListener(), getReportFileModule().getMinutesSinceCodeClear());

        // Step 14 Get DM5/DM20
        // these have already been read in the report file module

        // Steps 15 - 19
        incrementProgress("Tracking Monitor Completion Status");
        getMonitorTrackingModule().trackMonitors(getListener(), getReportFileModule());

        // Step 22 Display DM5 Vehicle Composite
        addBlankLineToReport();
        String lastDm5Time = getMonitorTrackingModule().getLastDm5Time();
        if (lastDm5Time != null) {
            incrementProgress("Reporting Monitored Systems Results");
            Set<MonitoredSystem> lastSystems = getMonitorTrackingModule().getLastSystems();
            String initialTime = getDateTimeModule().format(getReportFileModule().getInitialMonitorsTime());
            getDiagnosticReadinessModule().reportMonitoredSystems(getListener(),
                    getReportFileModule().getInitialMonitors(), lastSystems, initialTime, lastDm5Time);
        } else {
            getListener().onResult(getTime() + " Monitored Systems Results cannot be reported");
            incrementProgress("Monitored Systems Results cannot be reported");
        }

        // Step 23
        addBlankLineToReport();
        String lastDm20Time = getMonitorTrackingModule().getLastDm20Time();
        if (lastDm20Time != null) {
            incrementProgress("Reporting Performance Ratio Results");
            Set<PerformanceRatio> lastRatios = getMonitorTrackingModule().getLastRatios();
            int lastIgnitionCycles = getMonitorTrackingModule().getLastIgnitionCycles();
            int lastObdCounts = getMonitorTrackingModule().getLastObdCounts();
            String initialTime = getDateTimeModule().format(getReportFileModule().getInitialRatiosTime());
            getDiagnosticReadinessModule().reportPerformanceRatios(getListener(),
                    getReportFileModule().getInitialRatios(), lastRatios,
                    getReportFileModule().getInitialIgnitionCycles(), lastIgnitionCycles,
                    getReportFileModule().getInitialOBDCounts(), lastObdCounts, initialTime, lastDm20Time);
        } else {
            getListener().onResult(getTime() + " Performance Ratio Results cannot be reported");
            incrementProgress("Performance Ratio Results cannot be reported");
        }

        // Step 24
        addBlankLineToReport();
        incrementProgress("Requesting DM21");
        getDiagnosticReadinessModule().reportDM21(getListener(), getReportFileModule().getMinutesSinceCodeClear());

        // Step 25
        addBlankLineToReport();
        incrementProgress("Reading Vehicle Distance");
        getVehicleInformationModule().reportVehicleDistance(getListener());

        // Step 26
        addBlankLineToReport();
        incrementProgress("Requesting Engine Hours");
        getVehicleInformationModule().reportEngineHours(getListener());

        // Step 27
        addBlankLineToReport();
        incrementProgress("Generating Quality Information");
        getReportFileModule().reportAndResetCommunicationQuality(getListener());
    }

}
