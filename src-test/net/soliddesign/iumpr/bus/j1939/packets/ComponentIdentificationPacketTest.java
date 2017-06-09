/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.bus.j1939.packets;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import net.soliddesign.iumpr.bus.Packet;

/**
 * Unit test for the {@link ComponentIdentificationPacket} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class ComponentIdentificationPacketTest {
    private static final String MAKE = "Solid Design";
    private static final String MODEL = "IUMPR Tool";
    private static final String SN = "000001";
    private static final String UN = "1234567890";

    @Test
    public void testMake() {
        byte[] data = (MAKE + "****").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals(MAKE, instance.getMake());
        assertEquals("", instance.getModel());
        assertEquals("", instance.getSerialNumber());
        assertEquals("", instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: Solid Design, Model: , Serial Number: , Unit Number: ";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testMakeModel() {
        byte[] data = (MAKE + "*" + MODEL + "***").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals(MAKE, instance.getMake());
        assertEquals(MODEL, instance.getModel());
        assertEquals("", instance.getSerialNumber());
        assertEquals("", instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: Solid Design, Model: IUMPR Tool, Serial Number: , Unit Number: ";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testMakeModelSerialNumberUnitNumber() {
        byte[] data = (MAKE + "*" + MODEL + "*" + SN + "*" + UN + "*").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals(MAKE, instance.getMake());
        assertEquals(MODEL, instance.getModel());
        assertEquals(SN, instance.getSerialNumber());
        assertEquals(UN, instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: Solid Design, Model: IUMPR Tool, Serial Number: 000001, Unit Number: 1234567890";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testMakeModelUnitNumber() {
        byte[] data = (MAKE + "*" + MODEL + "**" + UN + "*").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals(MAKE, instance.getMake());
        assertEquals(MODEL, instance.getModel());
        assertEquals("", instance.getSerialNumber());
        assertEquals(UN, instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: Solid Design, Model: IUMPR Tool, Serial Number: , Unit Number: 1234567890";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testMakeSerialNumber() {
        byte[] data = (MAKE + "**" + SN + "**").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals(MAKE, instance.getMake());
        assertEquals("", instance.getModel());
        assertEquals(SN, instance.getSerialNumber());
        assertEquals("", instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: Solid Design, Model: , Serial Number: 000001, Unit Number: ";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testMakeSerialNumberUnitNumber() {
        byte[] data = (MAKE + "**" + SN + "*" + UN + "*").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals(MAKE, instance.getMake());
        assertEquals("", instance.getModel());
        assertEquals(SN, instance.getSerialNumber());
        assertEquals(UN, instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: Solid Design, Model: , Serial Number: 000001, Unit Number: 1234567890";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testMakeUnitNumber() {
        byte[] data = (MAKE + "***" + UN + "*").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals(MAKE, instance.getMake());
        assertEquals("", instance.getModel());
        assertEquals("", instance.getSerialNumber());
        assertEquals(UN, instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: Solid Design, Model: , Serial Number: , Unit Number: 1234567890";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testModel() {
        byte[] data = ("*" + MODEL + "***").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals("", instance.getMake());
        assertEquals(MODEL, instance.getModel());
        assertEquals("", instance.getSerialNumber());
        assertEquals("", instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: , Model: IUMPR Tool, Serial Number: , Unit Number: ";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testModelSerialNumber() {
        byte[] data = ("*" + MODEL + "*" + SN + "**").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals("", instance.getMake());
        assertEquals(MODEL, instance.getModel());
        assertEquals(SN, instance.getSerialNumber());
        assertEquals("", instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: , Model: IUMPR Tool, Serial Number: 000001, Unit Number: ";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testModelSerialNumberUnitNumber() {
        byte[] data = ("*" + MODEL + "*" + SN + "*" + UN + "*").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals("", instance.getMake());
        assertEquals(MODEL, instance.getModel());
        assertEquals(SN, instance.getSerialNumber());
        assertEquals(UN, instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: , Model: IUMPR Tool, Serial Number: 000001, Unit Number: 1234567890";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testModelUnitNumber() {
        byte[] data = ("*" + MODEL + "**" + UN + "*").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals("", instance.getMake());
        assertEquals(MODEL, instance.getModel());
        assertEquals("", instance.getSerialNumber());
        assertEquals(UN, instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: , Model: IUMPR Tool, Serial Number: , Unit Number: 1234567890";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testNothing() {
        byte[] data = ("****").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals("", instance.getMake());
        assertEquals("", instance.getModel());
        assertEquals("", instance.getSerialNumber());
        assertEquals("", instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: , Model: , Serial Number: , Unit Number: ";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testPGN() {
        assertEquals(65259, ComponentIdentificationPacket.PGN);
    }

    @Test
    public void testSerialNumber() {
        byte[] data = ("**" + SN + "**").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals("", instance.getMake());
        assertEquals("", instance.getModel());
        assertEquals(SN, instance.getSerialNumber());
        assertEquals("", instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: , Model: , Serial Number: 000001, Unit Number: ";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testSerialNumberUnitNumber() {
        byte[] data = ("**" + SN + "*" + UN + "*").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals("", instance.getMake());
        assertEquals("", instance.getModel());
        assertEquals(SN, instance.getSerialNumber());
        assertEquals(UN, instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: , Model: , Serial Number: 000001, Unit Number: 1234567890";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testSpacesAreTrimmed() {
        byte[] data = (MAKE + "     *     " + MODEL + "     *     " + SN + "     *     " + UN + "     *     ")
                .getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals(MAKE, instance.getMake());
        assertEquals(MODEL, instance.getModel());
        assertEquals(SN, instance.getSerialNumber());
        assertEquals(UN, instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: Solid Design, Model: IUMPR Tool, Serial Number: 000001, Unit Number: 1234567890";
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testUnitNumber() {
        byte[] data = ("***" + UN + "*").getBytes(StandardCharsets.UTF_8);
        Packet packet = Packet.create(0, 0, data);
        ComponentIdentificationPacket instance = new ComponentIdentificationPacket(packet);
        assertEquals("", instance.getMake());
        assertEquals("", instance.getModel());
        assertEquals("", instance.getSerialNumber());
        assertEquals(UN, instance.getUnitNumber());
        String expected = "Component Identification from Engine #1 (0): Make: , Model: , Serial Number: , Unit Number: 1234567890";
        assertEquals(expected, instance.toString());
    }

}
