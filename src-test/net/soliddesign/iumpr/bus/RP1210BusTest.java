/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for the {@link RP1210Bus} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class RP1210BusTest {

    private static final int ADDRESS = 0xA5;
    private static final byte[] ADDRESS_CLAIM_PARAMS = new byte[] { (byte) ADDRESS, 0, 0, (byte) 0xE0, (byte) 0xFF, 0,
            (byte) 0x81, 0, 0, 0 };

    private Adapter adapter;

    @Mock
    private ScheduledExecutorService exec;

    private RP1210Bus instance;

    @Mock
    private Logger logger;

    @Mock
    private MultiQueue<Packet> queue;

    @Mock
    private RP1210Library rp1210Library;

    private ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

    private void createInstance() throws BusException {
        instance = new RP1210Bus(rp1210Library, exec, queue, adapter, ADDRESS, logger);
    }

    @Before
    public void setUp() throws Exception {
        adapter = new Adapter("Testing Adapter", "TST_ADPTR", (short) 42);
    }

    private void startInstance() throws Exception {
        when(rp1210Library.RP1210_ClientConnect(0, (short) 42, "J1939:Baud=Auto", 0, 0, (short) 0))
                .thenReturn((short) 1);
        when(rp1210Library.RP1210_SendCommand(eq((short) 19), eq((short) 1), aryEq(ADDRESS_CLAIM_PARAMS),
                eq((short) 10))).thenReturn((short) 0);
        when(rp1210Library.RP1210_SendCommand(eq((short) 16), eq((short) 1), aryEq(new byte[] { (byte) 1 }),
                eq((short) 1))).thenReturn((short) 0);
        when(rp1210Library.RP1210_SendCommand(eq((short) 3), eq((short) 1), aryEq(new byte[] {}),
                eq((short) 0))).thenReturn((short) 0);

        when(exec.scheduleAtFixedRate(runnableCaptor.capture(), eq(1L), eq(1L), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(null);

        createInstance();

        verify(rp1210Library).RP1210_ClientConnect(0, adapter.getDeviceId(), "J1939:Baud=Auto", 0, 0, (short) 0);
        verify(rp1210Library).RP1210_SendCommand(eq((short) 19), eq((short) 1), aryEq(ADDRESS_CLAIM_PARAMS),
                eq((short) 10));
        verify(rp1210Library).RP1210_SendCommand(eq((short) 16), eq((short) 1), aryEq(new byte[] { (byte) 1 }),
                eq((short) 1));
        verify(rp1210Library).RP1210_SendCommand(eq((short) 3), eq((short) 1), aryEq(new byte[] {}),
                eq((short) 0));
        verify(exec).scheduleAtFixedRate(any(Runnable.class), eq(1L), eq(1L), eq(TimeUnit.MILLISECONDS));
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(rp1210Library, exec, queue, logger);
    }

    @Test
    public void testConstructorAddressClaimFails() throws Exception {
        when(rp1210Library.RP1210_ClientConnect(0, (short) 42, "J1939:Baud=Auto", 0, 0, (short) 0))
                .thenReturn((short) 1);

        when(rp1210Library.RP1210_SendCommand(eq((short) 19), eq((short) 1), aryEq(ADDRESS_CLAIM_PARAMS),
                eq((short) 10))).thenReturn((short) -99);
        when(rp1210Library.RP1210_GetErrorMsg(eq((short) 99), any())).thenAnswer(arg0 -> {
            byte[] dest = (byte[]) arg0.getArgument(1);
            final byte[] src = "Testing Failure".getBytes();
            System.arraycopy(src, 0, dest, 0, src.length);
            return (short) 0;
        });
        ArgumentCaptor<Callable<Short>> submitCaptor = ArgumentCaptor.forClass(Callable.class);

        Future<Short> future = mock(Future.class);
        when(future.get()).thenReturn((short) 0);
        when(exec.submit(submitCaptor.capture())).thenReturn(future);

        try {
            createInstance();
            fail("An exception should have been thrown");
        } catch (BusException e) {
            assertEquals("Failed to configure adapter.", e.getMessage());
            assertEquals("Error (99): Testing Failure", e.getCause().getMessage());
        }

        verify(rp1210Library).RP1210_ClientConnect(0, adapter.getDeviceId(), "J1939:Baud=Auto", 0, 0, (short) 0);
        verify(rp1210Library).RP1210_SendCommand(eq((short) 19), eq((short) 1), aryEq(ADDRESS_CLAIM_PARAMS),
                eq((short) 10));
        verify(rp1210Library).RP1210_GetErrorMsg(eq((short) 99), any());
        verify(exec).submit(any(Callable.class));
        verify(exec).shutdown();
    }

    @Test
    public void testConstructorConnectFails() throws Exception {
        when(rp1210Library.RP1210_ClientConnect(0, (short) 42, "J1939:Baud=Auto", 0, 0, (short) 0))
                .thenReturn((short) 134);
        when(rp1210Library.RP1210_GetErrorMsg(eq((short) 134), any())).thenAnswer(arg0 -> {
            byte[] dest = (byte[]) arg0.getArgument(1);
            final byte[] src = "Device Not Connected".getBytes();
            System.arraycopy(src, 0, dest, 0, src.length);
            return (short) 0;
        });

        try {
            createInstance();
            fail("An exception should have been thrown");
        } catch (BusException e) {
            assertEquals("Error (134): Device Not Connected", e.getMessage());
        }

        verify(rp1210Library).RP1210_ClientConnect(0, adapter.getDeviceId(), "J1939:Baud=Auto", 0, 0, (short) 0);
        verify(rp1210Library).RP1210_GetErrorMsg(eq((short) 134), any());
    }

    @Test
    public void testConstructorEchoFails() throws Exception {
        when(rp1210Library.RP1210_ClientConnect(0, (short) 42, "J1939:Baud=Auto", 0, 0, (short) 0))
                .thenReturn((short) 1);

        when(rp1210Library.RP1210_SendCommand(eq((short) 19), eq((short) 1), aryEq(ADDRESS_CLAIM_PARAMS),
                eq((short) 10))).thenReturn((short) 0);
        when(rp1210Library.RP1210_SendCommand(eq((short) 16), eq((short) 1), aryEq(new byte[] { (byte) 1 }),
                eq((short) 1))).thenReturn((short) -99);
        when(rp1210Library.RP1210_GetErrorMsg(eq((short) 99), any())).thenAnswer(arg0 -> {
            byte[] dest = (byte[]) arg0.getArgument(1);
            final byte[] src = "Testing Failure".getBytes();
            System.arraycopy(src, 0, dest, 0, src.length);
            return (short) 0;
        });
        ArgumentCaptor<Callable<Short>> submitCaptor = ArgumentCaptor.forClass(Callable.class);

        Future<Short> future = mock(Future.class);
        when(future.get()).thenReturn((short) 0);
        when(exec.submit(submitCaptor.capture())).thenReturn(future);

        try {
            createInstance();
            fail("An exception should have been thrown");
        } catch (BusException e) {
            assertEquals("Failed to configure adapter.", e.getMessage());
            assertEquals("Error (99): Testing Failure", e.getCause().getMessage());
        }

        verify(rp1210Library).RP1210_ClientConnect(0, adapter.getDeviceId(), "J1939:Baud=Auto", 0, 0, (short) 0);
        verify(rp1210Library).RP1210_SendCommand(eq((short) 19), eq((short) 1), aryEq(ADDRESS_CLAIM_PARAMS),
                eq((short) 10));
        verify(rp1210Library).RP1210_SendCommand(eq((short) 16), eq((short) 1), aryEq(new byte[] { (byte) 1 }),
                eq((short) 1));
        verify(rp1210Library).RP1210_GetErrorMsg(eq((short) 99), any());
        verify(exec).submit(any(Callable.class));
        verify(exec).shutdown();
    }

    @Test
    public void testConstructorFilterFails() throws Exception {
        when(rp1210Library.RP1210_ClientConnect(0, (short) 42, "J1939:Baud=Auto", 0, 0, (short) 0))
                .thenReturn((short) 1);

        when(rp1210Library.RP1210_SendCommand(eq((short) 19), eq((short) 1), aryEq(ADDRESS_CLAIM_PARAMS),
                eq((short) 10))).thenReturn((short) 0);
        when(rp1210Library.RP1210_SendCommand(eq((short) 16), eq((short) 1), aryEq(new byte[] { (byte) 1 }),
                eq((short) 1))).thenReturn((short) 0);
        when(rp1210Library.RP1210_SendCommand(eq((short) 3), eq((short) 1), aryEq(new byte[] {}),
                eq((short) 0))).thenReturn((short) -99);

        when(rp1210Library.RP1210_GetErrorMsg(eq((short) 99), any())).thenAnswer(arg0 -> {
            byte[] dest = (byte[]) arg0.getArgument(1);
            final byte[] src = "Testing Failure".getBytes();
            System.arraycopy(src, 0, dest, 0, src.length);
            return (short) 0;
        });
        ArgumentCaptor<Callable<Short>> submitCaptor = ArgumentCaptor.forClass(Callable.class);

        Future<Short> future = mock(Future.class);
        when(future.get()).thenReturn((short) 0);
        when(exec.submit(submitCaptor.capture())).thenReturn(future);

        try {
            createInstance();
            fail("An exception should have been thrown");
        } catch (BusException e) {
            assertEquals("Failed to configure adapter.", e.getMessage());
            assertEquals("Error (99): Testing Failure", e.getCause().getMessage());
        }

        verify(rp1210Library).RP1210_ClientConnect(0, adapter.getDeviceId(), "J1939:Baud=Auto", 0, 0, (short) 0);
        verify(rp1210Library).RP1210_SendCommand(eq((short) 19), eq((short) 1), aryEq(ADDRESS_CLAIM_PARAMS),
                eq((short) 10));
        verify(rp1210Library).RP1210_SendCommand(eq((short) 16), eq((short) 1), aryEq(new byte[] { (byte) 1 }),
                eq((short) 1));
        verify(rp1210Library).RP1210_SendCommand(eq((short) 3), eq((short) 1), aryEq(new byte[] {}),
                eq((short) 0));
        verify(rp1210Library).RP1210_GetErrorMsg(eq((short) 99), any());
        verify(exec).submit(any(Callable.class));
        verify(exec).shutdown();
    }

    @Test
    public void testConstructorStopFails() throws Exception {
        when(rp1210Library.RP1210_ClientConnect(0, (short) 42, "J1939:Baud=Auto", 0, 0, (short) 0))
                .thenReturn((short) 1);

        when(rp1210Library.RP1210_SendCommand(eq((short) 19), eq((short) 1), aryEq(ADDRESS_CLAIM_PARAMS),
                eq((short) 10))).thenReturn((short) 0);
        when(rp1210Library.RP1210_SendCommand(eq((short) 16), eq((short) 1), aryEq(new byte[] { (byte) 1 }),
                eq((short) 1))).thenReturn((short) 0);
        when(rp1210Library.RP1210_SendCommand(eq((short) 3), eq((short) 1), aryEq(new byte[] {}),
                eq((short) 0))).thenReturn((short) -99);

        when(rp1210Library.RP1210_GetErrorMsg(eq((short) 99), any())).thenAnswer(arg0 -> {
            byte[] dest = (byte[]) arg0.getArgument(1);
            final byte[] src = "Testing Failure".getBytes();
            System.arraycopy(src, 0, dest, 0, src.length);
            return (short) 0;
        });
        ArgumentCaptor<Callable<Short>> submitCaptor = ArgumentCaptor.forClass(Callable.class);

        final RejectedExecutionException expectedCause = new RejectedExecutionException();
        when(exec.submit(submitCaptor.capture())).thenThrow(expectedCause);

        try {
            createInstance();
            fail("An exception should have been thrown");
        } catch (BusException e) {
            assertEquals("Failed to stop RP1210.", e.getMessage());
            assertEquals(expectedCause, e.getCause());
        }

        verify(rp1210Library).RP1210_ClientConnect(0, adapter.getDeviceId(), "J1939:Baud=Auto", 0, 0, (short) 0);
        verify(rp1210Library).RP1210_SendCommand(eq((short) 19), eq((short) 1), aryEq(ADDRESS_CLAIM_PARAMS),
                eq((short) 10));
        verify(rp1210Library).RP1210_SendCommand(eq((short) 16), eq((short) 1), aryEq(new byte[] { (byte) 1 }),
                eq((short) 1));
        verify(rp1210Library).RP1210_SendCommand(eq((short) 3), eq((short) 1), aryEq(new byte[] {}),
                eq((short) 0));
        verify(rp1210Library).RP1210_GetErrorMsg(eq((short) 99), any());
        verify(exec).submit(any(Callable.class));
        verify(exec).shutdown();
    }

    @Test
    public void testGetAddress() throws Exception {
        startInstance();
        assertEquals(ADDRESS, instance.getAddress());
    }

    @Test
    public void testGetConnectionSpeed() throws Exception {
        when(rp1210Library.RP1210_SendCommand(eq((short) 45), eq((short) 1), any(), eq((short) 17))).then(arg0 -> {
            byte[] bytes = arg0.getArgument(2);
            byte[] value = "500000".getBytes();
            System.arraycopy(value, 0, bytes, 0, value.length);
            return null;
        });
        startInstance();
        assertEquals(500000, instance.getConnectionSpeed());
        verify(rp1210Library).RP1210_SendCommand(eq((short) 45), eq((short) 1), any(), eq((short) 17));
    }

    @Test
    public void testPoll() throws Exception {
        Packet packet = Packet.create(0x1234, 0x56, 0x77, 0x88, 0x99, 0xAA, 0xBB, 0xCC, 0xDD, 0xEE);
        byte[] encodedPacket = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x34, (byte) 0x12, (byte) 0x00,
                (byte) 0x06, (byte) 0x56, (byte) 0x34, (byte) 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB,
                (byte) 0xCC, (byte) 0xDD, (byte) 0xEE };
        when(rp1210Library.RP1210_ReadMessage(eq((short) 1), any(byte[].class), eq((short) 2048), eq((short) 0)))
                .thenAnswer(arg0 -> {
                    byte[] data = arg0.getArgument(1);
                    System.arraycopy(encodedPacket, 0, data, 0, encodedPacket.length);
                    return (short) encodedPacket.length;
                }).thenReturn((short) 0);

        startInstance();
        Runnable runnable = runnableCaptor.getValue();
        runnable.run();

        ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
        verify(queue).add(packetCaptor.capture());
        Packet actual = packetCaptor.getValue();

        assertEquals(packet, actual);
        verify(rp1210Library, times(2)).RP1210_ReadMessage(eq((short) 1), any(byte[].class), eq((short) 2048),
                eq((short) 0));
    }

    @Test
    public void testPollFails() throws Exception {
        when(rp1210Library.RP1210_ReadMessage(eq((short) 1), any(byte[].class), eq((short) 2048), eq((short) 0)))
                .thenReturn((short) -99);

        when(rp1210Library.RP1210_GetErrorMsg(eq((short) 99), any())).thenAnswer(arg0 -> {
            byte[] dest = (byte[]) arg0.getArgument(1);
            final byte[] src = "Testing Failure".getBytes();
            System.arraycopy(src, 0, dest, 0, src.length);
            return (short) 0;
        });

        startInstance();
        Runnable runnable = runnableCaptor.getValue();
        runnable.run();

        verify(queue, never()).add(any(Packet.class));
        verify(rp1210Library).RP1210_ReadMessage(eq((short) 1), any(byte[].class), eq((short) 2048), eq((short) 0));
        verify(rp1210Library).RP1210_GetErrorMsg(eq((short) 99), any());
        verify(logger).log(eq(Level.SEVERE), eq("Failed to read RP1210"), any(BusException.class));
    }

    @Test
    public void testPollTransmitted() throws Exception {
        Packet packet = Packet.create(0x06, 0x1234, 0x56, true, (byte) 0x77, (byte) 0x88, (byte) 0x99,
                (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE);
        byte[] encodedPacket = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01, (byte) 0x34, (byte) 0x12, (byte) 0x00,
                (byte) 0x06, (byte) 0x56, (byte) 0x34, (byte) 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB,
                (byte) 0xCC, (byte) 0xDD, (byte) 0xEE };
        when(rp1210Library.RP1210_ReadMessage(eq((short) 1), any(byte[].class), eq((short) 2048), eq((short) 0)))
                .thenAnswer(arg0 -> {
                    byte[] data = arg0.getArgument(1);
                    System.arraycopy(encodedPacket, 0, data, 0, encodedPacket.length);
                    return (short) encodedPacket.length;
                }).thenReturn((short) 0);

        startInstance();
        Runnable runnable = runnableCaptor.getValue();
        runnable.run();

        ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
        verify(queue).add(packetCaptor.capture());
        Packet actual = packetCaptor.getValue();

        assertEquals(packet, actual);
        verify(rp1210Library, times(2)).RP1210_ReadMessage(eq((short) 1), any(byte[].class), eq((short) 2048),
                eq((short) 0));
    }

    @Test
    public void testPollWithImposter() throws Exception {
        Packet packet = Packet.create(0x06, 0x1234, ADDRESS, false, (byte) 0x77, (byte) 0x88, (byte) 0x99,
                (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE);
        byte[] encodedPacket = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x34, (byte) 0x12, (byte) 0x00,
                (byte) 0x06, (byte) ADDRESS, (byte) 0x34, (byte) 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xAA,
                (byte) 0xBB,
                (byte) 0xCC, (byte) 0xDD, (byte) 0xEE };
        when(rp1210Library.RP1210_ReadMessage(eq((short) 1), any(byte[].class), eq((short) 2048), eq((short) 0)))
                .thenAnswer(arg0 -> {
                    byte[] data = arg0.getArgument(1);
                    System.arraycopy(encodedPacket, 0, data, 0, encodedPacket.length);
                    return (short) encodedPacket.length;
                }).thenReturn((short) 0);

        startInstance();
        Runnable runnable = runnableCaptor.getValue();
        runnable.run();

        ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
        verify(queue).add(packetCaptor.capture());
        Packet actual = packetCaptor.getValue();

        assertEquals(packet, actual);
        verify(rp1210Library, times(2)).RP1210_ReadMessage(eq((short) 1), any(byte[].class), eq((short) 2048),
                eq((short) 0));
        verify(logger).log(Level.WARNING, "Another module is using this address");
    }

    @Test
    public void testRead() throws Exception {
        startInstance();
        Stream<Packet> stream = Stream.empty();
        when(queue.stream(1250, TimeUnit.MILLISECONDS)).thenReturn(stream);
        assertSame(stream, instance.read(1250, TimeUnit.MILLISECONDS));
        verify(queue).stream(1250, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testReadWithArgs() throws Exception {
        startInstance();
        Stream<Packet> stream = Stream.empty();
        when(queue.stream(99, TimeUnit.NANOSECONDS)).thenReturn(stream);
        assertSame(stream, instance.read(99, TimeUnit.NANOSECONDS));
        verify(queue).stream(99, TimeUnit.NANOSECONDS);
    }

    @Test
    public void testSend() throws Exception {
        Packet packet = Packet.create(0x1234, 0x56, 0x77, 0x88, 0x99, 0xAA, 0xBB, 0xCC, 0xDD, 0xEE);
        byte[] encodedPacket = new byte[] { (byte) 0x34, (byte) 0x12, (byte) 0x00, (byte) 0x06, (byte) 0x56,
                (byte) 0x34, (byte) 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD,
                (byte) 0xEE };
        ArgumentCaptor<Callable<Short>> submitCaptor = ArgumentCaptor.forClass(Callable.class);

        Future<Short> future = mock(Future.class);
        when(future.get()).thenReturn((short) 0);
        when(exec.submit(submitCaptor.capture())).thenReturn(future);

        startInstance();
        instance.send(packet);

        Callable<Short> callable = submitCaptor.getValue();
        callable.call();

        verify(exec).submit(any(Callable.class));
        verify(rp1210Library).RP1210_SendMessage(eq((short) 1), aryEq(encodedPacket), eq((short) 14), eq((short) 0),
                eq((short) 0));
    }

    @Test
    public void testSendFails() throws Exception {
        Packet packet = Packet.create(0x1234, 0x56, 0x77, 0x88, 0x99, 0xAA, 0xBB, 0xCC, 0xDD, 0xEE);
        byte[] encodedPacket = new byte[] { (byte) 0x34, (byte) 0x12, (byte) 0x00, (byte) 0x06, (byte) 0x56,
                (byte) 0x34, (byte) 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD,
                (byte) 0xEE };

        ArgumentCaptor<Callable<Short>> submitCaptor = ArgumentCaptor.forClass(Callable.class);

        Future<Short> future = mock(Future.class);
        when(future.get()).thenReturn((short) -99);
        when(exec.submit(submitCaptor.capture())).thenReturn(future);

        when(rp1210Library.RP1210_GetErrorMsg(eq((short) 99), any())).thenAnswer(arg0 -> {
            byte[] dest = (byte[]) arg0.getArgument(1);
            final byte[] src = "Testing Failure".getBytes();
            System.arraycopy(src, 0, dest, 0, src.length);
            return (short) 0;
        });

        startInstance();
        try {
            instance.send(packet);
            fail("An exception should have been thrown");
        } catch (BusException e) {
            assertEquals("Failed to send: 18123456 77 88 99 AA BB CC DD EE", e.getMessage());
            assertEquals("Error (99): Testing Failure", e.getCause().getMessage());
        }

        Callable<Short> callable = submitCaptor.getValue();
        callable.call();

        verify(exec).submit(any(Callable.class));
        verify(rp1210Library).RP1210_SendMessage(eq((short) 1), aryEq(encodedPacket), eq((short) 14), eq((short) 0),
                eq((short) 0));
        verify(rp1210Library).RP1210_GetErrorMsg(eq((short) 99), any());
    }

    @Test
    public void testStop() throws Exception {
        ArgumentCaptor<Callable<Short>> submitCaptor = ArgumentCaptor.forClass(Callable.class);

        Future<Short> future = mock(Future.class);
        when(future.get()).thenReturn((short) -99);
        when(exec.submit(submitCaptor.capture())).thenReturn(future);

        startInstance();
        instance.stop();

        Callable<Short> callable = submitCaptor.getValue();
        callable.call();

        verify(rp1210Library).RP1210_ClientDisconnect((short) 1);
        verify(exec).submit(any(Callable.class));
        verify(exec).shutdown();

    }

    @Test
    public void testStopFails() throws Exception {

        ArgumentCaptor<Callable<Short>> submitCaptor = ArgumentCaptor.forClass(Callable.class);

        final RejectedExecutionException expectedCause = new RejectedExecutionException();
        when(exec.submit(submitCaptor.capture())).thenThrow(expectedCause);

        startInstance();
        try {
            instance.stop();
            fail("An exception should have been thrown");
        } catch (BusException e) {
            assertEquals("Failed to stop RP1210.", e.getMessage());
            assertEquals(expectedCause, e.getCause());
        }

        verify(exec).submit(any(Callable.class));
        verify(exec).shutdown();
    }

}
