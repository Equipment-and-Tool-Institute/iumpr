/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import static net.soliddesign.iumpr.IUMPR.NL;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import net.soliddesign.iumpr.bus.Packet;

/**
 * Unit tests the {@link DiagnosticTroubleCodePacket} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DiagnosticTroubleCodePacketTest {

    @Test
    public void testGetAmberWarningLampStatusFastFlash() {
        int[] data = new int[] { 0x04, 0x04, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.FAST_FLASH, instance.getAmberWarningLampStatus());
        assertEquals(LampStatus.FAST_FLASH, instance.getAmberWarningLampStatus());
    }

    @Test
    public void testGetAmberWarningLampStatusOff() {
        int[] data = new int[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.OFF, instance.getAmberWarningLampStatus());
        assertEquals(LampStatus.OFF, instance.getAmberWarningLampStatus());
    }

    @Test
    public void testGetAmberWarningLampStatusOn() {
        int[] data = new int[] { 0x04, 0x0C, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.ON, instance.getAmberWarningLampStatus());
        assertEquals(LampStatus.ON, instance.getAmberWarningLampStatus());
    }

    @Test
    public void testGetAmberWarningLampStatusSlowFlash() {
        int[] data = new int[] { 0x04, 0x00, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.SLOW_FLASH, instance.getAmberWarningLampStatus());
        assertEquals(LampStatus.SLOW_FLASH, instance.getAmberWarningLampStatus());
    }

    @Test
    public void testGetDtcsEmptyWithEightBytes() {
        int[] data = new int[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(0, instance.getDtcs().size());
    }

    @Test
    public void testGetDtcsEmptyWithGrandfathered() {
        int[] data = new int[] { 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);
        assertEquals(0, instance.getDtcs().size());
    }

    @Test
    public void testGetDtcsEmptyWithSixBytes() {
        int[] data = new int[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(0, instance.getDtcs().size());
    }

    @Test
    public void testGetDtcsOneWithEightBytes() {
        int[] data = new int[] { 0x00, 0xFF, 0x61, 0x02, 0x13, 0x00, 0xFF, 0xFF };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        final List<DiagnosticTroubleCode> dtcs = instance.getDtcs();
        assertEquals(1, dtcs.size());
        assertEquals(609, dtcs.get(0).getSuspectParameterNumber());
    }

    @Test
    public void testGetDtcsOneWithSixBytes() {
        int[] data = new int[] { 0x00, 0xFF, 0x61, 0x02, 0x13, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        final List<DiagnosticTroubleCode> dtcs = instance.getDtcs();
        assertEquals(1, dtcs.size());
        assertEquals(609, dtcs.get(0).getSuspectParameterNumber());
    }

    @Test
    public void testGetDtcsThree() {
        int[] data = new int[] { 0x00, 0xFF, 0x61, 0x02, 0x13, 0x00, 0x21, 0x06, 0x1F, 0x00, 0xEE, 0x10, 0x04, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        final List<DiagnosticTroubleCode> dtcs = instance.getDtcs();
        assertEquals(3, dtcs.size());
        assertEquals(609, dtcs.get(0).getSuspectParameterNumber());
        assertEquals(1569, dtcs.get(1).getSuspectParameterNumber());
        assertEquals(4334, dtcs.get(2).getSuspectParameterNumber());
    }

    @Test
    public void testGetMalfunctionIndicatorLampStatusFastFlash() {
        int[] data = new int[] { 0x40, 0x40, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.FAST_FLASH, instance.getMalfunctionIndicatorLampStatus());
        assertEquals(LampStatus.FAST_FLASH, instance.getMalfunctionIndicatorLampStatus());
    }

    @Test
    public void testGetMalfunctionIndicatorLampStatusOff() {
        int[] data = new int[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.OFF, instance.getMalfunctionIndicatorLampStatus());
        assertEquals(LampStatus.OFF, instance.getMalfunctionIndicatorLampStatus());
    }

    @Test
    public void testGetMalfunctionIndicatorLampStatusOn() {
        int[] data = new int[] { 0x40, 0xC0, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.ON, instance.getMalfunctionIndicatorLampStatus());
        assertEquals(LampStatus.ON, instance.getMalfunctionIndicatorLampStatus());
    }

    @Test
    public void testGetMalfunctionIndicatorLampStatusSlowFlash() {
        int[] data = new int[] { 0x40, 0x00, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.SLOW_FLASH, instance.getMalfunctionIndicatorLampStatus());
        assertEquals(LampStatus.SLOW_FLASH, instance.getMalfunctionIndicatorLampStatus());
    }

    @Test
    public void testGetProtectLampStatusFastFlash() {
        int[] data = new int[] { 0x01, 0x01, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.FAST_FLASH, instance.getProtectLampStatus());
        assertEquals(LampStatus.FAST_FLASH, instance.getProtectLampStatus());
    }

    @Test
    public void testGetProtectLampStatusOff() {
        int[] data = new int[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.OFF, instance.getProtectLampStatus());
        assertEquals(LampStatus.OFF, instance.getProtectLampStatus());
    }

    @Test
    public void testGetProtectLampStatusOn() {
        int[] data = new int[] { 0x01, 0x03, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.ON, instance.getProtectLampStatus());
        assertEquals(LampStatus.ON, instance.getProtectLampStatus());
    }

    @Test
    public void testGetProtectLampStatusSlowFlash() {
        int[] data = new int[] { 0x01, 0x00, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.SLOW_FLASH, instance.getProtectLampStatus());
        assertEquals(LampStatus.SLOW_FLASH, instance.getProtectLampStatus());
    }

    @Test
    public void testGetRedStopLampStatusFastFlash() {
        int[] data = new int[] { 0x10, 0x10, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.FAST_FLASH, instance.getRedStopLampStatus());
        assertEquals(LampStatus.FAST_FLASH, instance.getRedStopLampStatus());
    }

    @Test
    public void testGetRedStopLampStatusOff() {
        int[] data = new int[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.OFF, instance.getRedStopLampStatus());
        assertEquals(LampStatus.OFF, instance.getRedStopLampStatus());
    }

    @Test
    public void testGetRedStopLampStatusOn() {
        int[] data = new int[] { 0x10, 0x30, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.ON, instance.getRedStopLampStatus());
        assertEquals(LampStatus.ON, instance.getRedStopLampStatus());
    }

    @Test
    public void testGetRedStopLampStatusSlowFlash() {
        int[] data = new int[] { 0x10, 0x00, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x00, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);

        assertEquals(LampStatus.SLOW_FLASH, instance.getRedStopLampStatus());
        assertEquals(LampStatus.SLOW_FLASH, instance.getRedStopLampStatus());
    }

    @Test
    public void testToString() {
        int[] data = new int[] { 0x54, 0x4F, 0x61, 0x02, 0x13, 0x00, 0x21, 0x06, 0x1F, 0x00, 0xEE, 0x10, 0x04, 0x00 };
        Packet packet = Packet.create(0x123456, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);
        String expected = "DM from Engine #1 (0): MIL: Fast Flash, RSL: Slow Flash, AWL: On, PL: Off" + NL
                + "DTC: Controller #2 (609) Received Network Data In Error (19) 0 times" + NL
                + "DTC: Engine Protection Torque Derate (1569) Condition Exists (31) 0 times" + NL
                + "DTC: Aftertreatment 1 Diesel Exhaust Fluid Doser 1 Absolute Pressure (4334) Voltage Below Normal, Or Shorted To Low Source (4) 0 times";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testToStringNoDtcs() {
        int[] data = new int[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
        Packet packet = Packet.create(0x123456, 0x00, data);
        DiagnosticTroubleCodePacket instance = new DiagnosticTroubleCodePacket(packet);
        String expected = "DM from Engine #1 (0): MIL: Off, RSL: Off, AWL: Off, PL: Off" + NL + "No DTCs";
        assertEquals(expected, instance.toString());
    }

}
