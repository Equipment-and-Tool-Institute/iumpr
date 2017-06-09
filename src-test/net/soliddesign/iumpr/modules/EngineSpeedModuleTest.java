/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import net.soliddesign.iumpr.bus.Packet;
import net.soliddesign.iumpr.bus.j1939.J1939;
import net.soliddesign.iumpr.bus.j1939.packets.EngineSpeedPacket;

/**
 * The Unit tests for the {@link EngineSpeedModule} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class EngineSpeedModuleTest {

    private static final int PGN = EngineSpeedPacket.PGN;

    private EngineSpeedModule instance;

    @Mock
    private J1939 j1939;

    /**
     * Creates an {@link EngineSpeedPacket} with the given engine speed
     *
     * @param speed
     *            the engine speed in 1/8 RPMs
     * @return an {@link EngineSpeedPacket}
     */
    private EngineSpeedPacket getEngineSpeedPacket(int speed) {
        return new EngineSpeedPacket(Packet.create(PGN, 0x00, 0, 0, 0, speed & 0xFF, (speed >> 8) & 0xFF, 0, 0, 0));
    }

    @Before
    public void setUp() throws Exception {
        instance = new EngineSpeedModule();
        instance.setJ1939(j1939);
    }

    @After
    public void tearDown() {
        verify(j1939).read(EngineSpeedPacket.class, 0x00, 300, TimeUnit.MILLISECONDS);
        verifyNoMoreInteractions(j1939);
    }

    @Test
    public void testEngineCommunicating() {
        EngineSpeedPacket packet = getEngineSpeedPacket(0);
        when(j1939.read(EngineSpeedPacket.class, 0x00, 300, TimeUnit.MILLISECONDS)).thenReturn(Optional.of(packet));
        assertTrue(instance.isEngineCommunicating());
    }

    @Test
    public void testEngineNotCommunicating() {
        when(j1939.read(EngineSpeedPacket.class, 0x00, 300, TimeUnit.MILLISECONDS)).thenReturn(Optional.empty());
        assertFalse(instance.isEngineCommunicating());
    }

    @Test
    public void testEngineNotRunning() {
        EngineSpeedPacket packet = getEngineSpeedPacket(0);
        when(j1939.read(EngineSpeedPacket.class, 0x00, 300, TimeUnit.MILLISECONDS)).thenReturn(Optional.of(packet));
        assertFalse(instance.isEngineRunning());
    }

    @Test
    public void testEngineRunning() {
        EngineSpeedPacket packet = getEngineSpeedPacket(2500);
        when(j1939.read(EngineSpeedPacket.class, 0x00, 300, TimeUnit.MILLISECONDS)).thenReturn(Optional.of(packet));
        assertTrue(instance.isEngineRunning());
    }

    @Test
    public void testIsEngineRunningError() {
        EngineSpeedPacket packet = getEngineSpeedPacket(0xFE00);
        when(j1939.read(EngineSpeedPacket.class, 0x00, 300, TimeUnit.MILLISECONDS)).thenReturn(Optional.of(packet));
        assertFalse(instance.isEngineRunning());
    }

    @Test
    public void testIsEngineRunningNoCommunication() {
        when(j1939.read(EngineSpeedPacket.class, 0x00, 300, TimeUnit.MILLISECONDS)).thenReturn(Optional.empty());
        assertFalse(instance.isEngineRunning());
    }

    @Test
    public void testIsEngineRunningNotAvailable() {
        EngineSpeedPacket packet = getEngineSpeedPacket(0xFFFF);
        when(j1939.read(EngineSpeedPacket.class, 0x00, 300, TimeUnit.MILLISECONDS)).thenReturn(Optional.of(packet));
        assertFalse(instance.isEngineRunning());
    }

}
