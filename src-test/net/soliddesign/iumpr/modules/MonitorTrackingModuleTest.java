/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.soliddesign.iumpr.IUMPR.NL;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.junit.MockitoJUnitRunner;

import net.soliddesign.iumpr.bus.Packet;
import net.soliddesign.iumpr.bus.j1939.J1939;
import net.soliddesign.iumpr.bus.j1939.packets.DM20MonitorPerformanceRatioPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM21DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM26TripDiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM5DiagnosticReadinessPacket;
import net.soliddesign.iumpr.controllers.TestResultsListener;

/**
 * Unit tests for the {@link MonitorTrackingModule} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MonitorTrackingModuleTest {

    /**
     * The maximum time to wait for the tracking to finish - be generous for
     * slow, old computers
     */
    private static final int WAIT_TIME = 1000; // milliseconds

    private DateTimeModule dateTimeModule;

    @Mock
    private DiagnosticReadinessModule diagnosticReadinessModule;

    @Mock
    private EngineSpeedModule engineSpeedModule;

    @Mock
    private ScheduledExecutorService executor;

    private MonitorTrackingModule instance;

    @Mock
    private J1939 j1939;

    private TestResultsListener listener;

    private final boolean[] lock = new boolean[] { false };

    @Mock
    private ReportFileModule reportFileModule;

    private List<DM20MonitorPerformanceRatioPacket> getDm20Packets() {
        return singletonList(
                new DM20MonitorPerformanceRatioPacket(Packet.create(49664, 0x00, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)));
    }

    private List<DM21DiagnosticReadinessPacket> getDm21Packets() {
        return singletonList(new DM21DiagnosticReadinessPacket(Packet.create(49408, 0, 1, 2, 3, 4, 5, 6, 7, 8)));
    }

    private List<DM26TripDiagnosticReadinessPacket> getDm26Packets() {
        return singletonList(new DM26TripDiagnosticReadinessPacket(Packet.create(64952, 0, 1, 2, 3, 4, 5, 6, 7, 8)));
    }

    private List<DM5DiagnosticReadinessPacket> getDm5Packets() {
        return singletonList(new DM5DiagnosticReadinessPacket(Packet.create(65230, 0x00, 1, 2, 3, 4, 5, 6, 7, 8)));
    }

    /**
     * Helper method to call the trackMonitors method and capture the runnable
     * that is used internally
     *
     * @return {@link Runnable}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Runnable runInstance() {
        ScheduledFuture future = mock(ScheduledFuture.class);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(executor.scheduleAtFixedRate(runnableCaptor.capture(), eq(0L), eq(1L),
                eq(TimeUnit.SECONDS))).thenReturn(future);
        when(future.isCancelled()).thenReturn(false);

        new Thread(() -> {
            instance.trackMonitors(listener, reportFileModule);
            synchronized (lock) {
                lock[0] = true;
                lock.notify();
            }
        }).start();

        Runnable runnable = null;

        do {
            try {
                runnable = runnableCaptor.getValue();
            } catch (MockitoException e) {
                // Don't care
            }
        } while (runnable == null);
        return runnable;
    }

    @Before
    public void setUp() throws Exception {
        dateTimeModule = new TestDateTimeModule();
        listener = new TestResultsListener();
        instance = new MonitorTrackingModule(dateTimeModule, diagnosticReadinessModule, engineSpeedModule, executor);
        instance.setJ1939(j1939);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(diagnosticReadinessModule, engineSpeedModule, executor, j1939,
                reportFileModule);
    }

    @Test
    public void testDoesNotWriteOnChangeOfDM21() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);

        when(diagnosticReadinessModule.getDM5Packets(null, false)).thenReturn(getDm5Packets());

        when(diagnosticReadinessModule.getDM20Packets(null, false)).thenReturn(getDm20Packets());

        when(diagnosticReadinessModule.getDM26Packets(null, false)).thenReturn(getDm26Packets());

        List<DM21DiagnosticReadinessPacket> dm21Packets2 = singletonList(
                new DM21DiagnosticReadinessPacket(Packet.create(49408, 0, 8, 7, 6, 5, 4, 3, 2, 1)));
        when(diagnosticReadinessModule.getDM21Packets(null, false)).thenReturn(getDm21Packets())
                .thenReturn(dm21Packets2);

        Runnable runnable = runInstance();
        runnable.run(); // Initial Read Vehicle
        // Count down
        for (int i = 0; i < 9; i++) {
            runnable.run();
        }
        runnable.run(); // Second Read Vehicle

        instance.endTracking();

        synchronized (lock) {
            if (!lock[0]) {
                lock.wait(WAIT_TIME);
            }
        }

        String expectedResults = "";
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 Begin Tracking Monitor Completion Status" + NL;
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 End Tracking Monitor Completion Status. 2 Total Cycles." + NL;

        assertEquals(expectedResults, listener.getResults());

        String expectedMessages = "";
        expectedMessages += "Monitors/Ratios Update #1: Reading Engine Speed" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM5" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM20" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM26" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM21" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 10 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 9 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 8 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 7 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 6 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 5 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 4 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 3 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 2 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 1 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Reading Engine Speed" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM5" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM20" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM26" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM21" + NL;
        expectedMessages += "Monitors/Ratios Update #2: 10 Seconds Until Next Update";
        assertEquals(expectedMessages, listener.getMessages());

        verify(executor).scheduleAtFixedRate(runnable, 0L, 1L, TimeUnit.SECONDS);
        verify(engineSpeedModule, times(2)).isEngineCommunicating();
        verify(reportFileModule, times(8)).incrementQueries();
        verify(diagnosticReadinessModule, times(2)).getDM5Packets(null, false);
        verify(diagnosticReadinessModule, times(2)).getDM20Packets(null, false);
        verify(diagnosticReadinessModule, times(2)).getDM26Packets(null, false);
        verify(diagnosticReadinessModule, times(2)).getDM21Packets(null, false);
        verify(j1939).interrupt();
    }

    @Test
    public void testDoesNotWriteOnChangeOfDM26() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);

        when(diagnosticReadinessModule.getDM5Packets(null, false)).thenReturn(getDm5Packets());

        when(diagnosticReadinessModule.getDM20Packets(null, false)).thenReturn(getDm20Packets());

        List<DM26TripDiagnosticReadinessPacket> dm26Packets2 = singletonList(
                new DM26TripDiagnosticReadinessPacket(Packet.create(64952, 0, 8, 7, 6, 5, 4, 3, 2, 1)));
        when(diagnosticReadinessModule.getDM26Packets(null, false)).thenReturn(getDm26Packets())
                .thenReturn(dm26Packets2);

        when(diagnosticReadinessModule.getDM21Packets(null, false)).thenReturn(getDm21Packets());

        Runnable runnable = runInstance();
        runnable.run(); // Initial Read Vehicle
        // Count down
        for (int i = 0; i < 9; i++) {
            runnable.run();
        }
        runnable.run(); // Second Read Vehicle

        instance.endTracking();

        synchronized (lock) {
            if (!lock[0]) {
                lock.wait(WAIT_TIME);
            }
        }

        String expectedResults = "";
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 Begin Tracking Monitor Completion Status" + NL;
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 End Tracking Monitor Completion Status. 2 Total Cycles." + NL;

        assertEquals(expectedResults, listener.getResults());

        String expectedMessages = "";
        expectedMessages += "Monitors/Ratios Update #1: Reading Engine Speed" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM5" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM20" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM26" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM21" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 10 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 9 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 8 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 7 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 6 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 5 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 4 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 3 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 2 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 1 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Reading Engine Speed" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM5" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM20" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM26" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM21" + NL;
        expectedMessages += "Monitors/Ratios Update #2: 10 Seconds Until Next Update";
        assertEquals(expectedMessages, listener.getMessages());

        verify(executor).scheduleAtFixedRate(runnable, 0L, 1L, TimeUnit.SECONDS);
        verify(engineSpeedModule, times(2)).isEngineCommunicating();
        verify(reportFileModule, times(8)).incrementQueries();
        verify(diagnosticReadinessModule, times(2)).getDM5Packets(null, false);
        verify(diagnosticReadinessModule, times(2)).getDM20Packets(null, false);
        verify(diagnosticReadinessModule, times(2)).getDM26Packets(null, false);
        verify(diagnosticReadinessModule, times(2)).getDM21Packets(null, false);
        verify(j1939).interrupt();
    }

    @Test
    public void testHaltsOnEnd() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(diagnosticReadinessModule.getDM5Packets(null, false)).thenReturn(getDm5Packets());
        when(diagnosticReadinessModule.getDM20Packets(null, false)).thenReturn(getDm20Packets());
        when(diagnosticReadinessModule.getDM26Packets(null, false)).thenReturn(getDm26Packets());
        when(diagnosticReadinessModule.getDM21Packets(null, false)).thenReturn(getDm21Packets());

        Runnable runnable = runInstance();
        runnable.run();
        instance.endTracking();
        runnable.run();

        synchronized (lock) {
            if (!lock[0]) {
                lock.wait(WAIT_TIME);
            }
        }

        String expectedResults = "";
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 Begin Tracking Monitor Completion Status" + NL;
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 End Tracking Monitor Completion Status. 1 Total Cycles." + NL;

        assertEquals(expectedResults, listener.getResults());

        String expectedMessages = "";
        expectedMessages += "Monitors/Ratios Update #1: Reading Engine Speed" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM5" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM20" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM26" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM21" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 10 Seconds Until Next Update";

        assertEquals(expectedMessages, listener.getMessages());

        verify(executor).scheduleAtFixedRate(runnable, 0L, 1L, TimeUnit.SECONDS);
        verify(engineSpeedModule).isEngineCommunicating();
        verify(reportFileModule, times(4)).incrementQueries();
        verify(diagnosticReadinessModule).getDM5Packets(null, false);
        verify(diagnosticReadinessModule).getDM20Packets(null, false);
        verify(diagnosticReadinessModule).getDM26Packets(null, false);
        verify(diagnosticReadinessModule).getDM21Packets(null, false);
        verify(j1939, times(2)).interrupt();
    }

    @Test
    public void testHandlesEndBeforeStarting() throws Exception {
        instance.endTracking();
        instance.trackMonitors(listener, reportFileModule);
        String expected = "";
        expected += "" + NL;
        expected += "2007-12-03T10:15:30.000 Begin Tracking Monitor Completion Status" + NL;
        expected += "" + NL;
        expected += "2007-12-03T10:15:30.000 End Tracking Monitor Completion Status. 0 Total Cycles." + NL;
        String actual = listener.getResults();
        assertEquals(expected, actual);
        verify(j1939).interrupt();
    }

    @Test
    public void testQueriesEvery10SecondsAndWritesEvery3Minutes() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(diagnosticReadinessModule.getDM5Packets(null, false)).thenReturn(getDm5Packets());
        when(diagnosticReadinessModule.getDM20Packets(null, false)).thenReturn(getDm20Packets());
        when(diagnosticReadinessModule.getDM26Packets(null, false)).thenReturn(getDm26Packets());
        when(diagnosticReadinessModule.getDM21Packets(null, false)).thenReturn(getDm21Packets());

        Runnable runnable = runInstance();

        assertEquals(null, instance.getLastDm20Time());
        assertEquals(null, instance.getLastDm5Time());
        assertEquals(-1, instance.getLastIgnitionCycles());
        assertEquals(-1, instance.getLastObdCounts());
        assertEquals(null, instance.getLastRatios());
        assertEquals(null, instance.getLastSystems());

        runnable.run(); // Initial Read Vehicle
        for (int j = 0; j < 35; j++) {
            // Count down
            for (int i = 0; i < 9; i++) {
                runnable.run();
            }
            runnable.run(); // Read Vehicle
        }

        instance.endTracking();

        synchronized (lock) {
            if (!lock[0]) {
                lock.wait(WAIT_TIME);
            }
        }

        assertEquals("2007-12-03T10:15:30.000", instance.getLastDm20Time());
        assertEquals("2007-12-03T10:15:30.000", instance.getLastDm5Time());
        assertEquals(513, instance.getLastIgnitionCycles());
        assertEquals(1027, instance.getLastObdCounts());
        assertEquals(1, instance.getLastRatios().size());
        assertEquals(16, instance.getLastSystems().size());

        String expectedResults = "";
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 Begin Tracking Monitor Completion Status" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM5 Packet(s) Received" + NL;
        expectedResults += "10:15:30.000 18FECE00 01 02 03 04 05 06 07 08" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM20 Packet(s) Received" + NL;
        expectedResults += "10:15:30.000 18C20000 01 02 03 04 05 06 07 08 09 0A 0B" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM26 Packet(s) Received" + NL;
        expectedResults += "10:15:30.000 18FDB800 01 02 03 04 05 06 07 08" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM5 Packet(s) Received" + NL;
        expectedResults += "10:15:30.000 18FECE00 01 02 03 04 05 06 07 08" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM20 Packet(s) Received" + NL;
        expectedResults += "10:15:30.000 18C20000 01 02 03 04 05 06 07 08 09 0A 0B" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM26 Packet(s) Received" + NL;
        expectedResults += "10:15:30.000 18FDB800 01 02 03 04 05 06 07 08" + NL;
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 End Tracking Monitor Completion Status. 36 Total Cycles." + NL;

        assertEquals(expectedResults, listener.getResults());

        if (NL.length() == 2) {
            assertEquals(28150, listener.getMessages().length());
        } else {
            assertEquals(27620, listener.getMessages().length());
        }

        verify(executor).scheduleAtFixedRate(runnable, 0L, 1L, TimeUnit.SECONDS);
        verify(reportFileModule, times(36 * 4)).incrementQueries();
        verify(engineSpeedModule, times(36)).isEngineCommunicating();
        verify(diagnosticReadinessModule, times(36)).getDM5Packets(null, false);
        verify(diagnosticReadinessModule, times(36)).getDM20Packets(null, false);
        verify(diagnosticReadinessModule, times(36)).getDM26Packets(null, false);
        verify(diagnosticReadinessModule, times(36)).getDM21Packets(null, false);
        verify(j1939).interrupt();
    }

    @Test
    public void testStopsWithDM5() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(diagnosticReadinessModule.getDM5Packets(null, false)).thenReturn(emptyList());

        Runnable runnable = runInstance();
        runnable.run();

        synchronized (lock) {
            if (!lock[0]) {
                lock.wait(WAIT_TIME);
            }
        }

        String expectedResults = "";
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 Begin Tracking Monitor Completion Status" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM5 Error: Timeout - No Response." + NL;
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 End Tracking Monitor Completion Status. 1 Total Cycles." + NL;

        assertEquals(expectedResults, listener.getResults());

        String expectedMessages = "";
        expectedMessages += "Monitors/Ratios Update #1: Reading Engine Speed" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM5";

        assertEquals(expectedMessages, listener.getMessages());

        verify(executor).scheduleAtFixedRate(runnable, 0L, 1L, TimeUnit.SECONDS);
        verify(engineSpeedModule).isEngineCommunicating();
        verify(reportFileModule).incrementQueries();
        verify(diagnosticReadinessModule).getDM5Packets(null, false);
        verify(j1939).interrupt();
    }

    @Test
    public void testStopsWithoutDM20() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(diagnosticReadinessModule.getDM5Packets(null, false)).thenReturn(getDm5Packets());
        when(diagnosticReadinessModule.getDM20Packets(null, false)).thenReturn(emptyList());

        Runnable runnable = runInstance();
        runnable.run();

        synchronized (lock) {
            if (!lock[0]) {
                lock.wait(WAIT_TIME);
            }
        }

        String expectedResults = "";
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 Begin Tracking Monitor Completion Status" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM20 Error: Timeout - No Response." + NL;
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 End Tracking Monitor Completion Status. 1 Total Cycles." + NL;

        assertEquals(expectedResults, listener.getResults());

        String expectedMessages = "";
        expectedMessages += "Monitors/Ratios Update #1: Reading Engine Speed" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM5" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM20";

        assertEquals(expectedMessages, listener.getMessages());

        verify(executor).scheduleAtFixedRate(runnable, 0L, 1L, TimeUnit.SECONDS);
        verify(engineSpeedModule).isEngineCommunicating();
        verify(reportFileModule, times(2)).incrementQueries();
        verify(diagnosticReadinessModule).getDM5Packets(null, false);
        verify(diagnosticReadinessModule).getDM20Packets(null, false);
        verify(j1939).interrupt();
    }

    @Test
    public void testStopsWithoutDM21() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(diagnosticReadinessModule.getDM5Packets(null, false)).thenReturn(getDm5Packets());
        when(diagnosticReadinessModule.getDM20Packets(null, false)).thenReturn(getDm20Packets());
        when(diagnosticReadinessModule.getDM26Packets(null, false)).thenReturn(getDm26Packets());
        when(diagnosticReadinessModule.getDM21Packets(null, false)).thenReturn(emptyList());

        Runnable runnable = runInstance();
        runnable.run();

        synchronized (lock) {
            if (!lock[0]) {
                lock.wait(WAIT_TIME);
            }
        }

        String expectedResults = "";
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 Begin Tracking Monitor Completion Status" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM21 Error: Timeout - No Response." + NL;
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 End Tracking Monitor Completion Status. 1 Total Cycles." + NL;

        assertEquals(expectedResults, listener.getResults());

        String expectedMessages = "";
        expectedMessages += "Monitors/Ratios Update #1: Reading Engine Speed" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM5" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM20" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM26" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM21";

        assertEquals(expectedMessages, listener.getMessages());

        verify(executor).scheduleAtFixedRate(runnable, 0L, 1L, TimeUnit.SECONDS);
        verify(engineSpeedModule).isEngineCommunicating();
        verify(reportFileModule, times(4)).incrementQueries();
        verify(diagnosticReadinessModule).getDM5Packets(null, false);
        verify(diagnosticReadinessModule).getDM20Packets(null, false);
        verify(diagnosticReadinessModule).getDM26Packets(null, false);
        verify(diagnosticReadinessModule).getDM21Packets(null, false);
        verify(j1939).interrupt();
    }

    @Test
    public void testStopsWithoutDM26() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);
        when(diagnosticReadinessModule.getDM5Packets(null, false)).thenReturn(getDm5Packets());
        when(diagnosticReadinessModule.getDM20Packets(null, false)).thenReturn(getDm20Packets());
        when(diagnosticReadinessModule.getDM26Packets(null, false)).thenReturn(emptyList());

        Runnable runnable = runInstance();
        runnable.run();

        synchronized (lock) {
            if (!lock[0]) {
                lock.wait(WAIT_TIME);
            }
        }

        String expectedResults = "";
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 Begin Tracking Monitor Completion Status" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM26 Error: Timeout - No Response." + NL;
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 End Tracking Monitor Completion Status. 1 Total Cycles." + NL;

        assertEquals(expectedResults, listener.getResults());

        String expectedMessages = "";
        expectedMessages += "Monitors/Ratios Update #1: Reading Engine Speed" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM5" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM20" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM26";

        assertEquals(expectedMessages, listener.getMessages());

        verify(executor).scheduleAtFixedRate(runnable, 0L, 1L, TimeUnit.SECONDS);
        verify(engineSpeedModule).isEngineCommunicating();
        verify(reportFileModule, times(3)).incrementQueries();
        verify(diagnosticReadinessModule).getDM5Packets(null, false);
        verify(diagnosticReadinessModule).getDM20Packets(null, false);
        verify(diagnosticReadinessModule).getDM26Packets(null, false);
        verify(j1939).interrupt();
    }

    @Test
    public void testStopsWithoutEngineComm() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(false);

        Runnable runnable = runInstance();
        runnable.run();

        synchronized (lock) {
            if (!lock[0]) {
                lock.wait(WAIT_TIME);
            }
        }

        String expectedResults = "";
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 Begin Tracking Monitor Completion Status" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 Engine Speed Error: Timeout - No Response." + NL;
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 End Tracking Monitor Completion Status. 1 Total Cycles." + NL;

        assertEquals(expectedResults, listener.getResults());

        String expectedMessages = "";
        expectedMessages += "Monitors/Ratios Update #1: Reading Engine Speed";
        assertEquals(expectedMessages, listener.getMessages());

        verify(executor).scheduleAtFixedRate(runnable, 0L, 1L, TimeUnit.SECONDS);
        verify(engineSpeedModule).isEngineCommunicating();
        verify(j1939).interrupt();
    }

    @Test
    public void testWritesOnChangeOfDM20() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);

        when(diagnosticReadinessModule.getDM5Packets(null, false)).thenReturn(getDm5Packets());

        DM20MonitorPerformanceRatioPacket dm20Packet = new DM20MonitorPerformanceRatioPacket(
                Packet.create(49664, 0x00, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1));
        List<DM20MonitorPerformanceRatioPacket> dm20Packets2 = singletonList(dm20Packet);
        when(diagnosticReadinessModule.getDM20Packets(null, false)).thenReturn(getDm20Packets())
                .thenReturn(dm20Packets2);

        when(diagnosticReadinessModule.getDM26Packets(null, false)).thenReturn(getDm26Packets());

        when(diagnosticReadinessModule.getDM21Packets(null, false)).thenReturn(getDm21Packets());

        Runnable runnable = runInstance();
        runnable.run(); // Initial Read Vehicle
        // Count down
        for (int i = 0; i < 9; i++) {
            runnable.run();
        }
        runnable.run(); // Second Read Vehicle

        instance.endTracking();

        synchronized (lock) {
            if (!lock[0]) {
                lock.wait(WAIT_TIME);
            }
        }

        String expectedResults = "";
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 Begin Tracking Monitor Completion Status" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 Ratios Updated" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM5 Packet(s) Received" + NL;
        expectedResults += "10:15:30.000 18FECE00 01 02 03 04 05 06 07 08" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM20 Packet(s) Received" + NL;
        expectedResults += "10:15:30.000 18C20000 0B 0A 09 08 07 06 05 04 03 02 01" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM26 Packet(s) Received" + NL;
        expectedResults += "10:15:30.000 18FDB800 01 02 03 04 05 06 07 08" + NL;
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 End Tracking Monitor Completion Status. 2 Total Cycles." + NL;

        assertEquals(expectedResults, listener.getResults());

        String expectedMessages = "";
        expectedMessages += "Monitors/Ratios Update #1: Reading Engine Speed" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM5" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM20" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM26" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM21" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 10 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 9 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 8 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 7 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 6 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 5 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 4 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 3 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 2 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 1 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Reading Engine Speed" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM5" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM20" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM26" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM21" + NL;
        expectedMessages += "Monitors/Ratios Update #2: 10 Seconds Until Next Update";
        assertEquals(expectedMessages, listener.getMessages());

        verify(executor).scheduleAtFixedRate(runnable, 0L, 1L, TimeUnit.SECONDS);
        verify(engineSpeedModule, times(2)).isEngineCommunicating();
        verify(reportFileModule, times(8)).incrementQueries();
        verify(diagnosticReadinessModule, times(2)).getDM5Packets(null, false);
        verify(diagnosticReadinessModule, times(2)).getDM20Packets(null, false);
        verify(diagnosticReadinessModule, times(2)).getDM26Packets(null, false);
        verify(diagnosticReadinessModule, times(2)).getDM21Packets(null, false);
        verify(j1939).interrupt();
    }

    @Test
    public void testWritesOnChangeOfDM5() throws Exception {
        when(engineSpeedModule.isEngineCommunicating()).thenReturn(true);

        DM5DiagnosticReadinessPacket dm5Packet2 = new DM5DiagnosticReadinessPacket(
                Packet.create(65230, 0x00, 8, 7, 6, 5, 4, 3, 2, 1));
        List<DM5DiagnosticReadinessPacket> dm5Packets2 = singletonList(dm5Packet2);
        when(diagnosticReadinessModule.getDM5Packets(null, false)).thenReturn(getDm5Packets()).thenReturn(dm5Packets2);

        when(diagnosticReadinessModule.getDM20Packets(null, false)).thenReturn(getDm20Packets());

        when(diagnosticReadinessModule.getDM26Packets(null, false)).thenReturn(getDm26Packets());

        when(diagnosticReadinessModule.getDM21Packets(null, false)).thenReturn(getDm21Packets());

        Runnable runnable = runInstance();
        runnable.run(); // Initial Read Vehicle
        // Count down
        for (int i = 0; i < 9; i++) {
            runnable.run();
        }
        runnable.run(); // Second Read Vehicle

        instance.endTracking();

        synchronized (lock) {
            if (!lock[0]) {
                lock.wait(WAIT_TIME);
            }
        }

        String expectedResults = "";
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 Begin Tracking Monitor Completion Status" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 Monitors Updated" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM5 Packet(s) Received" + NL;
        expectedResults += "10:15:30.000 18FECE00 08 07 06 05 04 03 02 01" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM20 Packet(s) Received" + NL;
        expectedResults += "10:15:30.000 18C20000 01 02 03 04 05 06 07 08 09 0A 0B" + NL;
        expectedResults += "" + NL;
        expectedResults += "10:15:30.000 DM26 Packet(s) Received" + NL;
        expectedResults += "10:15:30.000 18FDB800 01 02 03 04 05 06 07 08" + NL;
        expectedResults += "" + NL;
        expectedResults += "2007-12-03T10:15:30.000 End Tracking Monitor Completion Status. 2 Total Cycles." + NL;

        assertEquals(expectedResults, listener.getResults());

        String expectedMessages = "";
        expectedMessages += "Monitors/Ratios Update #1: Reading Engine Speed" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM5" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM20" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM26" + NL;
        expectedMessages += "Monitors/Ratios Update #1: Requesting DM21" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 10 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 9 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 8 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 7 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 6 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 5 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 4 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 3 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 2 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #1: 1 Seconds Until Next Update" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Reading Engine Speed" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM5" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM20" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM26" + NL;
        expectedMessages += "Monitors/Ratios Update #2: Requesting DM21" + NL;
        expectedMessages += "Monitors/Ratios Update #2: 10 Seconds Until Next Update";
        assertEquals(expectedMessages, listener.getMessages());

        verify(executor).scheduleAtFixedRate(runnable, 0L, 1L, TimeUnit.SECONDS);
        verify(engineSpeedModule, times(2)).isEngineCommunicating();
        verify(reportFileModule, times(8)).incrementQueries();
        verify(diagnosticReadinessModule, times(2)).getDM5Packets(null, false);
        verify(diagnosticReadinessModule, times(2)).getDM20Packets(null, false);
        verify(diagnosticReadinessModule, times(2)).getDM26Packets(null, false);
        verify(diagnosticReadinessModule, times(2)).getDM21Packets(null, false);
        verify(j1939).interrupt();
    }
}
