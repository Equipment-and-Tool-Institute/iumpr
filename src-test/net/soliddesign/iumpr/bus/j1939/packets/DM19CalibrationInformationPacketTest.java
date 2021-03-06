/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import static net.soliddesign.iumpr.IUMPR.NL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import net.soliddesign.iumpr.bus.Packet;
import net.soliddesign.iumpr.bus.j1939.packets.DM19CalibrationInformationPacket.CalibrationInformation;

/**
 * Unit tests the {@link DM19CalibrationInformationPacket} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DM19CalibrationInformationPacketTest {

    @Test
    public void testCalibrationInformationAndToStringWithOne() {
        Packet packet = Packet.create(0, 0, 0x51, 0xBA, 0xFE, 0xBD, 0x41, 0x4E, 0x54, 0x35, 0x41, 0x53, 0x52, 0x31,
                0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);
        DM19CalibrationInformationPacket instance = new DM19CalibrationInformationPacket(packet);
        List<CalibrationInformation> calInfos = instance.getCalibrationInformation();
        assertNotNull(calInfos);
        assertEquals(1, calInfos.size());
        CalibrationInformation calInfo = calInfos.get(0);
        assertEquals("ANT5ASR1", calInfo.getCalibrationIdentification());
        assertEquals("0xBDFEBA51", calInfo.getCalibrationVerificationNumber());

        String expected = "DM19 from Engine #1 (0): CAL ID of ANT5ASR1 and CVN of 0xBDFEBA51";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testCalibrationInformationAndToStringWithOneNoBlanks() {
        Packet packet = Packet.create(0, 0, 0x51, 0xBA, 0xFE, 0xBD, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
                0x38, 0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35);
        DM19CalibrationInformationPacket instance = new DM19CalibrationInformationPacket(packet);
        List<CalibrationInformation> calInfos = instance.getCalibrationInformation();
        assertNotNull(calInfos);
        assertEquals(1, calInfos.size());
        CalibrationInformation calInfo = calInfos.get(0);
        assertEquals("0123456789012345", calInfo.getCalibrationIdentification());
        assertEquals("0xBDFEBA51", calInfo.getCalibrationVerificationNumber());

        String expected = "DM19 from Engine #1 (0): CAL ID of 0123456789012345 and CVN of 0xBDFEBA51";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testCalibrationInformationWithThree() {
        Packet packet = Packet.create(0, 0,
                // Cal #1
                0x51, 0xBA, 0xFE, 0xBD, 0x41, 0x4E, 0x54, 0x35, 0x41, 0x53, 0x52, 0x31, 0x20, 0x20, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00,

                // Cal #2
                0x96, 0xBF, 0xDC, 0x40, 0x50, 0x42, 0x54, 0x35, 0x4D, 0x50, 0x52, 0x33, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00,

                // Cal #3
                0x40, 0x91, 0xB9, 0x3E, 0x52, 0x50, 0x52, 0x42, 0x42, 0x41, 0x31, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00);
        DM19CalibrationInformationPacket instance = new DM19CalibrationInformationPacket(packet);

        List<CalibrationInformation> calInfos = instance.getCalibrationInformation();
        assertNotNull(calInfos);
        assertEquals(3, calInfos.size());
        CalibrationInformation calInfo1 = calInfos.get(0);
        assertEquals("ANT5ASR1", calInfo1.getCalibrationIdentification());
        assertEquals("0xBDFEBA51", calInfo1.getCalibrationVerificationNumber());

        CalibrationInformation calInfo2 = calInfos.get(1);
        assertEquals("PBT5MPR3", calInfo2.getCalibrationIdentification());
        assertEquals("0x40DCBF96", calInfo2.getCalibrationVerificationNumber());

        CalibrationInformation calInfo3 = calInfos.get(2);
        assertEquals("RPRBBA10", calInfo3.getCalibrationIdentification());
        assertEquals("0x3EB99140", calInfo3.getCalibrationVerificationNumber());

        String expected = "DM19 from Engine #1 (0): [" + NL + "  CAL ID of ANT5ASR1 and CVN of 0xBDFEBA51" + NL
                + "  CAL ID of PBT5MPR3 and CVN of 0x40DCBF96" + NL + "  CAL ID of RPRBBA10 and CVN of 0x3EB99140" + NL
                + "]";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testCalibrationInformationWithThreeDigitCVN() {
        Packet packet = Packet.create(0, 0, 0x00, 0xAC, 0xFF, 0x33, 0x41, 0x4E, 0x54, 0x35, 0x41, 0x53, 0x52, 0x31,
                0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);
        DM19CalibrationInformationPacket instance = new DM19CalibrationInformationPacket(packet);
        List<CalibrationInformation> calInfos = instance.getCalibrationInformation();
        assertNotNull(calInfos);
        assertEquals(1, calInfos.size());
        CalibrationInformation calInfo = calInfos.get(0);
        assertEquals("ANT5ASR1", calInfo.getCalibrationIdentification());
        assertEquals("0x33FFAC00", calInfo.getCalibrationVerificationNumber());

        String expected = "DM19 from Engine #1 (0): CAL ID of ANT5ASR1 and CVN of 0x33FFAC00";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testCalibrationInformationWithTwoDigitCVN() {
        Packet packet = Packet.create(0, 0, 0xDE, 0xE5, 0x00, 0x00, 0x31, 0x32, 0x44, 0x42, 0x42, 0x32, 0x30, 0x30,
                0x30, 0x32, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);
        DM19CalibrationInformationPacket instance = new DM19CalibrationInformationPacket(packet);
        List<CalibrationInformation> calInfos = instance.getCalibrationInformation();
        assertNotNull(calInfos);
        assertEquals(1, calInfos.size());
        CalibrationInformation calInfo = calInfos.get(0);
        assertEquals("12DBB20002", calInfo.getCalibrationIdentification());
        assertEquals("0x0000E5DE", calInfo.getCalibrationVerificationNumber());

        String expected = "DM19 from Engine #1 (0): CAL ID of 12DBB20002 and CVN of 0x0000E5DE";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testCalibrationInformationWithZeroDigitCVN() {
        Packet packet = Packet.create(0, 0, 0x00, 0x00, 0x00, 0x00, 0x41, 0x4E, 0x54, 0x35, 0x41, 0x53, 0x52, 0x31,
                0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);
        DM19CalibrationInformationPacket instance = new DM19CalibrationInformationPacket(packet);
        List<CalibrationInformation> calInfos = instance.getCalibrationInformation();
        assertNotNull(calInfos);
        assertEquals(1, calInfos.size());
        CalibrationInformation calInfo = calInfos.get(0);
        assertEquals("ANT5ASR1", calInfo.getCalibrationIdentification());
        assertEquals("0x00000000", calInfo.getCalibrationVerificationNumber());

        String expected = "DM19 from Engine #1 (0): CAL ID of ANT5ASR1 and CVN of 0x00000000";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testEqualsAndHashcode() {
        Packet packet1 = Packet.create(0xBADF, 0xFE, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88);
        DM19CalibrationInformationPacket instance1 = new DM19CalibrationInformationPacket(packet1);

        Packet packet2 = Packet.create(0xBADF, 0xFE, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88);
        DM19CalibrationInformationPacket instance2 = new DM19CalibrationInformationPacket(packet2);

        assertTrue(instance1.equals(instance2));
        assertTrue(instance2.equals(instance1));
        assertTrue(instance1.hashCode() == instance2.hashCode());
    }

    @Test
    public void testEqualsThis() {
        Packet packet = Packet.create(0xBADF, 0xFE, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88);
        DM19CalibrationInformationPacket instance = new DM19CalibrationInformationPacket(packet);
        assertTrue(instance.equals(instance));
        assertTrue(instance.hashCode() == instance.hashCode());
    }

    @Test
    public void testNotEqualsObject() {
        Packet packet = Packet.create(0xBADF, 0xFE, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88);
        DM19CalibrationInformationPacket instance = new DM19CalibrationInformationPacket(packet);
        assertFalse(instance.equals(new Object()));
    }

    @Test
    public void testPGN() {
        assertEquals(54016, DM19CalibrationInformationPacket.PGN);
    }

}
