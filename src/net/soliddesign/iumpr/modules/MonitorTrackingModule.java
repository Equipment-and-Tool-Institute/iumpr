/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.soliddesign.iumpr.IUMPR;
import net.soliddesign.iumpr.NumberFormatter;
import net.soliddesign.iumpr.bus.j1939.packets.DM20MonitorPerformanceRatioPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM21DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM26TripDiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM5DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem;
import net.soliddesign.iumpr.bus.j1939.packets.ParsedPacket;
import net.soliddesign.iumpr.bus.j1939.packets.PerformanceRatio;
import net.soliddesign.iumpr.controllers.ResultsListener;

/**
 * This will periodically query the vehicle for DM5, DM20, DM26 and DM21 until
 * interrupted providing the last of those values read
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class MonitorTrackingModule extends FunctionalModule {

    /**
     * The current cycle this is performing
     */
    private long cycle = 0;

    private final DiagnosticReadinessModule diagnosticReadinessModule;

    /**
     * Flag to indicate the tracking should end. This is not the same as abort.
     * This just ends the "every 10 second" tracking of monitor completion and
     * finishes the execution
     */
    private boolean end = false;

    /**
     * The engine speed module to check if the engine is communicating
     */
    private final EngineSpeedModule engineSpeedModule;

    /**
     * The executor used to run periodic tasks
     */
    private final ScheduledExecutorService executor;

    /**
     * The {@link ScheduledFuture} returned when scheduling the vehicle reading
     * task
     */
    private ScheduledFuture<?> future;

    /**
     * The last time, as a formatted string, that a DM20 was received
     */
    private String lastDm20Time;

    /**
     * The last time, as a formatted string, that a DM5 was received
     */
    private String lastDm5Time;

    /**
     * The last value received for the number of ignition cycles
     */
    private int lastIgnitionCycles = -1;

    /**
     * The last value received for the number of ODB Monitoring Conditions
     * Encountered
     */
    private int lastObdCounts = -1;

    /**
     * The last {@link PerformanceRatio} received
     */
    private Set<PerformanceRatio> lastRatios;

    /**
     * The last {@link MonitoredSystem} received
     */
    private Set<MonitoredSystem> lastSystems;

    /**
     * Used to provide a wait/notify mechanism for the threads
     */
    private final Future<?>[] lock = new Future[1];

    /**
     * The counter for the number of cycles that have passed without writing to
     * the report file
     */
    private int writeCount = 0;

    /**
     * Constructor
     *
     * @param dateTimeModule
     *            the {@link DateTimeModule} for reporting time
     * @param diagnosticReadinessModule
     *            the {@link DiagnosticReadinessModule} for reading DM5, DM20,
     *            DM21 and DM26 from the vehicle
     * @param engineSpeedModule
     *            the {@link EngineSpeedModule} for checking engine
     *            communications
     * @param executor
     *            the {@link ScheduledExecutorService} for running periodic
     *            tasks
     */
    public MonitorTrackingModule(DateTimeModule dateTimeModule, DiagnosticReadinessModule diagnosticReadinessModule,
            EngineSpeedModule engineSpeedModule, ScheduledExecutorService executor) {
        super(dateTimeModule);
        this.diagnosticReadinessModule = diagnosticReadinessModule;
        this.engineSpeedModule = engineSpeedModule;
        this.executor = executor;
    }

    /**
     * Helper method to end the tracking process and throws an
     * {@link InterruptedException}
     *
     * @throws InterruptedException
     *             because the process is ending
     */
    private void end() throws InterruptedException {
        end = true;
        throw new InterruptedException();
    }

    /**
     * Ends the Tracking Monitor Completion
     */
    public void endTracking() {
        end = true;
        notifyEnding();
    }

    /**
     * @return the diagnosticReadinessModule
     */
    private DiagnosticReadinessModule getDiagnosticReadinessModule() {
        return diagnosticReadinessModule;
    }

    /**
     * @return the engineSpeedModule
     */
    private EngineSpeedModule getEngineSpeedModule() {
        return engineSpeedModule;
    }

    /**
     * @return the lastDm20Time
     */
    public String getLastDm20Time() {
        return lastDm20Time;
    }

    /**
     * @return the lastDm5Time
     */
    public String getLastDm5Time() {
        return lastDm5Time;
    }

    /**
     * @return the lastIgnitionCycles
     */
    public int getLastIgnitionCycles() {
        return lastIgnitionCycles;
    }

    /**
     * @return the lastObdCounts
     */
    public int getLastObdCounts() {
        return lastObdCounts;
    }

    /**
     * @return the lastRatios
     */
    public Set<PerformanceRatio> getLastRatios() {
        return lastRatios;
    }

    /**
     * @return the lastSystems
     */
    public Set<MonitoredSystem> getLastSystems() {
        return lastSystems;
    }

    private Logger getLogger() {
        return IUMPR.getLogger();
    }

    /**
     * Creates and returned the {@link Runnable} that will be used to provide
     * updates the user and query the vehicle
     *
     * @return {@link Runnable}
     */
    private Runnable getRunnable(final ReportFileModule reportFileModule, ResultsListener listener) {
        writeCount = 0; // reset the write counter

        return new Runnable() {

            private int secondsToWait = 1; // query the vehicle the first time

            @Override
            public void run() {
                try {
                    String prefix1 = "Monitors/Ratios Update #";
                    String prefix2 = NumberFormatter.format(cycle) + ": ";
                    secondsToWait--;
                    if (secondsToWait == 0) {
                        cycle++;
                        prefix2 = NumberFormatter.format(cycle) + ": ";
                        readVehicle(prefix1 + prefix2, reportFileModule, listener);
                        secondsToWait = 10;
                    }
                    updateProgress(listener, prefix1 + prefix2 + secondsToWait + " Seconds Until Next Update");
                } catch (Exception e) {
                    if (!(e instanceof InterruptedException)) {
                        getLogger().log(Level.SEVERE, "Error while reading vehicle", e);
                    }
                    notifyEnding();
                }
            }
        };
    }

    private void notifyEnding() {
        synchronized (lock) {
            if (lock[0] != null) {
                lock[0].cancel(true);
            }
            lock.notify();
        }
    }

    /**
     * Helper method to write a blank line before the result
     *
     * @param listener
     *            the {@link ResultsListener} to give the result to
     * @param result
     *            the result to provide
     */
    private void onResult(ResultsListener listener, String result) {
        listener.onResult("");
        listener.onResult(result);
    }

    /**
     * Reads the DM5, DM20 and DM26 data, writing it to the report if values
     * changed or if it's time to do so
     *
     * @param prefix
     *            the prefix that's appended to update messages to the user
     *            interface
     * @param reportFileModule
     *            the {@link ReportFileModule} that keeps track of queries
     * @param listener
     *            the {@link ResultsListener} that's notified of the results
     * @throws InterruptedException
     *             if packets aren't received or the user has pressed "Stop"
     */
    private void readVehicle(String prefix, ReportFileModule reportFileModule, ResultsListener listener)
            throws InterruptedException {
        // Flag to indicate if packet should be written to the log this
        // iteration
        boolean writeToReport = false;

        // FUNCTION D Step 3
        updateProgress(listener, prefix + "Reading Engine Speed");
        if (!getEngineSpeedModule().isEngineCommunicating()) {
            onResult(listener, getTime() + " Engine Speed " + TIMEOUT_MESSAGE);
            end();
        }

        // FUNCTION D Step 6
        updateProgress(listener, prefix + "Requesting DM5");
        List<DM5DiagnosticReadinessPacket> dm5Packets = getDiagnosticReadinessModule().getDM5Packets(null, false);
        reportFileModule.incrementQueries();
        if (dm5Packets.isEmpty()) {
            onResult(listener, getTime() + " DM5 " + TIMEOUT_MESSAGE);
            end();
        } else {
            lastDm5Time = getTime();
            Set<MonitoredSystem> systems = DiagnosticReadinessModule.getSystems(dm5Packets);
            if (getLastSystems() != null && !Objects.equals(getLastSystems(), systems)) {
                onResult(listener, getTime() + " Monitors Updated");
                writeToReport = true;
            }
            lastSystems = systems;
        }

        // FUNCTION D Step 5
        updateProgress(listener, prefix + "Requesting DM20");
        List<DM20MonitorPerformanceRatioPacket> dm20Packets = getDiagnosticReadinessModule().getDM20Packets(null,
                false);
        reportFileModule.incrementQueries();
        if (dm20Packets.isEmpty()) {
            onResult(listener, getTime() + " DM20 " + TIMEOUT_MESSAGE);
            end();
        } else {
            lastDm20Time = getTime();
            Set<PerformanceRatio> ratios = DiagnosticReadinessModule.getRatios(dm20Packets);
            lastIgnitionCycles = DiagnosticReadinessModule.getIgnitionCycles(dm20Packets);
            lastObdCounts = DiagnosticReadinessModule.getOBDCounts(dm20Packets);
            if (getLastRatios() != null && !Objects.equals(getLastRatios(), ratios)) {
                onResult(listener, getTime() + " Ratios Updated");
                writeToReport = true;
            }
            lastRatios = ratios;
        }

        // FUNCTION D Step 7
        updateProgress(listener, prefix + "Requesting DM26");
        List<DM26TripDiagnosticReadinessPacket> dm26Packets = getDiagnosticReadinessModule().getDM26Packets(null,
                false);
        reportFileModule.incrementQueries();
        if (dm26Packets.isEmpty()) {
            onResult(listener, getTime() + " DM26 " + TIMEOUT_MESSAGE);
            end();
        }

        // FUNCTION D Step 8 (it says DM12 but means DM21)
        updateProgress(listener, prefix + "Requesting DM21");
        List<DM21DiagnosticReadinessPacket> dm21Packets = getDiagnosticReadinessModule().getDM21Packets(null, false);
        reportFileModule.incrementQueries();
        if (dm21Packets.isEmpty()) {
            onResult(listener, getTime() + " DM21 " + TIMEOUT_MESSAGE);
            end();
        }

        // FUNCTION C Steps 17 & 18
        writeCount++;
        // 18 = 18*10 -> 180 seconds -> 3 minutes
        // Packets are to be written every 3 minutes or on change
        if (writeCount >= 18 || writeToReport) {
            writeCount = 0;

            onResult(listener, getTime() + " DM5 Packet(s) Received");
            writePackets(listener, dm5Packets);

            onResult(listener, getTime() + " DM20 Packet(s) Received");
            writePackets(listener, dm20Packets);

            onResult(listener, getTime() + " DM26 Packet(s) Received");
            writePackets(listener, dm26Packets);
        }
    }

    /**
     * Resets the "end" Flag that interrupts the execution
     */
    public void resetEndFlag() {
        end = false;
    }

    @SuppressFBWarnings(value = "WA_NOT_IN_LOOP", justification = "Loop is not necessary.")
    public void trackMonitors(ResultsListener listener, ReportFileModule reportFileModule) {
        lastIgnitionCycles = -1;
        lastObdCounts = -1;
        cycle = 0;

        onResult(listener, getTime() + " Begin Tracking Monitor Completion Status");

        if (!end) {
            future = executor.scheduleAtFixedRate(getRunnable(reportFileModule, listener), 0L, 1L, TimeUnit.SECONDS);
            synchronized (lock) {
                lock[0] = future;
                if (!future.isCancelled()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        getLogger().log(Level.INFO, "Wait Interrupted");
                    }
                }
            }
        }

        // Step 20-21
        onResult(listener, getTime() + " End Tracking Monitor Completion Status. "
                + NumberFormatter.format(cycle) + " Total Cycles.");
    }

    /**
     * Helper method to update the progress to the listener and check the end
     * flag
     *
     * @param listener
     *            the {@link ResultsListener} to update
     * @param message
     *            the message to pass to the listener
     * @throws InterruptedException
     *             if the end flag has been set
     */
    private void updateProgress(ResultsListener listener, String message) throws InterruptedException {
        if (end) {
            throw new InterruptedException();
        }
        listener.onProgress(message);
    }

    /**
     * Helper method to write the given packets to the report
     *
     * @param packets
     *            the packets to write to the report
     */
    private void writePackets(ResultsListener listener, List<? extends ParsedPacket> packets) {
        packets.stream().forEach(t -> listener.onResult(getTime() + " " + t.getPacket().toString()));
    }
}
