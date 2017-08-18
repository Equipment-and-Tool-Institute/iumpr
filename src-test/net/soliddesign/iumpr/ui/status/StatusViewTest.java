/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.swing.WindowConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.soliddesign.iumpr.bus.j1939.J1939;
import net.soliddesign.iumpr.bus.j1939.packets.DM20MonitorPerformanceRatioPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM21DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM26TripDiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM5DiagnosticReadinessPacket;
import net.soliddesign.iumpr.ui.IUserInterfaceController;
import net.soliddesign.iumpr.ui.SwingExecutor;

/**
 * Unit tests for the {@link StatusView} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class StatusViewTest {

    @Mock
    private IUserInterfaceController controller;

    @Mock
    private ScheduledExecutorService executor;

    private StatusView instance;

    private SwingExecutor swingExecutor;

    @Before
    public void setUp() throws Exception {
        swingExecutor = new SwingExecutor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        instance = new StatusView(controller, executor, swingExecutor);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(controller, executor);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "result of verify().getVin() isn't needed")
    public void testSetVisibleThenClose() throws Exception {
        when(controller.getVin()).thenReturn("12345678901234567890");
        J1939 j1939 = mock(J1939.class);
        when(controller.getNewJ1939()).thenReturn(j1939);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        ScheduledFuture monitorFuture = mock(ScheduledFuture.class);
        when(executor.schedule(runnableCaptor.capture(), eq(1L), eq(TimeUnit.MILLISECONDS))).thenReturn(monitorFuture);

        ScheduledFuture timeoutFuture = mock(ScheduledFuture.class);
        when(executor.scheduleWithFixedDelay(runnableCaptor.capture(), eq(10L), eq(10L), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(timeoutFuture);

        // Test start up
        instance.setVisible(true);
        assertEquals(WindowConstants.DO_NOTHING_ON_CLOSE, instance.getDefaultCloseOperation());
        assertEquals("Tracking Monitor Completion Status for 12345678901234567890", instance.getTitle());

        verify(controller).getVin();
        verify(controller).getNewJ1939();
        verify(executor).schedule(runnableCaptor.capture(), eq(1L), eq(TimeUnit.MILLISECONDS));
        verify(executor).scheduleWithFixedDelay(runnableCaptor.capture(), eq(10L), eq(10L),
                eq(TimeUnit.MILLISECONDS));

        // Test Timers
        Runnable timeoutRunnable = runnableCaptor.getAllValues().get(0);
        Runnable monitorRunnable = runnableCaptor.getAllValues().get(1);

        // Check initial values
        assertEquals(0, instance.getDm20ProgressBar().getMinimum());
        assertEquals(10000, instance.getDm20ProgressBar().getMaximum());
        assertEquals(0, instance.getDm20ProgressBar().getValue());
        assertEquals("DM20", instance.getDm20ProgressBar().getString());

        assertEquals(0, instance.getDm21ProgressBar().getMinimum());
        assertEquals(10000, instance.getDm21ProgressBar().getMaximum());
        assertEquals(0, instance.getDm21ProgressBar().getValue());
        assertEquals("DM21", instance.getDm21ProgressBar().getString());

        assertEquals(0, instance.getDm26ProgressBar().getMinimum());
        assertEquals(10000, instance.getDm26ProgressBar().getMaximum());
        assertEquals(0, instance.getDm26ProgressBar().getValue());
        assertEquals("DM26", instance.getDm26ProgressBar().getString());

        assertEquals(0, instance.getDm5ProgressBar().getMinimum());
        assertEquals(10000, instance.getDm5ProgressBar().getMaximum());
        assertEquals(0, instance.getDm5ProgressBar().getValue());
        assertEquals("DM5", instance.getDm5ProgressBar().getString());

        // Allow for timeout
        for (int i = 0; i < 1001; i++) {
            timeoutRunnable.run();
        }

        assertEquals(0, instance.getDm20ProgressBar().getValue());
        assertEquals("DM20 Timeout", instance.getDm20ProgressBar().getString());

        assertEquals(0, instance.getDm21ProgressBar().getValue());
        assertEquals("DM21 Timeout", instance.getDm21ProgressBar().getString());

        assertEquals(0, instance.getDm26ProgressBar().getValue());
        assertEquals("DM26 Timeout", instance.getDm26ProgressBar().getString());

        assertEquals(0, instance.getDm5ProgressBar().getValue());
        assertEquals("DM5 Timeout", instance.getDm5ProgressBar().getString());

        // Send packets to reset values
        DM5DiagnosticReadinessPacket dm5Packet = mock(DM5DiagnosticReadinessPacket.class);
        DM20MonitorPerformanceRatioPacket dm20Packet = mock(DM20MonitorPerformanceRatioPacket.class);
        DM26TripDiagnosticReadinessPacket dm26Packet = mock(DM26TripDiagnosticReadinessPacket.class);
        DM21DiagnosticReadinessPacket dm21Packet = mock(DM21DiagnosticReadinessPacket.class);

        when(j1939.read()).thenReturn(Stream.of(dm5Packet, dm20Packet, dm26Packet, dm21Packet));
        monitorRunnable.run();

        // Check for reset
        assertEquals(10000, instance.getDm20ProgressBar().getValue());
        assertEquals("DM20", instance.getDm20ProgressBar().getString());

        assertEquals(10000, instance.getDm21ProgressBar().getValue());
        assertEquals("DM21", instance.getDm21ProgressBar().getString());

        assertEquals(10000, instance.getDm26ProgressBar().getValue());
        assertEquals("DM26", instance.getDm26ProgressBar().getString());

        assertEquals(10000, instance.getDm5ProgressBar().getValue());
        assertEquals("DM5", instance.getDm5ProgressBar().getString());

        // Test Close
        instance.getStopButton().doClick();
        Thread.sleep(100);
        verify(timeoutFuture).cancel(true);
        verify(monitorFuture).cancel(true);
        verify(controller, times(2)).onStatusViewClosed();
    }

}
