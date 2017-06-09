/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.system;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import net.soliddesign.iumpr.bus.Bus;
import net.soliddesign.iumpr.bus.BusException;
import net.soliddesign.iumpr.bus.Packet;
import net.soliddesign.iumpr.bus.simulated.Sim;

/**
 * Simulated Engine used for System Testing
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class Engine implements AutoCloseable {
    private static final Charset A_UTF8 = StandardCharsets.UTF_8;
    private final static int ADDR = 0x00;
    private static final byte[] COMPONENT_ID = "INT*570261221315646M13*570HM2U3545277**".getBytes(A_UTF8);
    private static final byte[] DISTANCE = as4Bytes(256345 * 8); // km
    /*
     * Calibration ID must be 16 bytes
     */
    private static final byte[] ENGINE_CAL_ID1 = "PBT5MPR3        ".getBytes(A_UTF8);
    private static final byte[] ENGINE_CVN1 = as4Bytes(0x40DCBF96);

    private static final byte[] ENGINE_HOURS = as4Bytes(3564 * 20); // hrs
    private static final byte[] ENGINE_SPEED = as2Bytes(500 * 8); // rpm
    private static final byte NA = (byte) 0xFF;
    private static final byte[] NA3 = new byte[] { NA, NA, NA };
    private static final byte[] NA4 = new byte[] { NA, NA, NA, NA };

    /*
     * VIN can be any length up to 200 bytes, but should end with *
     */
    private static final byte[] VIN = "3HAMKSTN0FL575012*".getBytes(A_UTF8);

    private static byte[] as2Bytes(int a) {
        byte[] ret = new byte[4];
        ret[0] = (byte) (a & 0xFF);
        ret[1] = (byte) ((a >> 8) & 0xFF);
        return ret;
    }

    private static byte[] as4Bytes(int a) {
        byte[] ret = new byte[4];
        ret[0] = (byte) (a & 0xFF);
        ret[1] = (byte) ((a >> 8) & 0xFF);
        ret[2] = (byte) ((a >> 16) & 0xFF);
        ret[3] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }

    private static byte[] combine(byte[]... bytes) {
        int length = 0;
        for (byte[] b : bytes) {
            length += b.length;
        }
        ByteBuffer bb = ByteBuffer.allocate(length);
        for (byte[] data : bytes) {
            bb.put(data);
        }
        return bb.array();
    }

    private static boolean isDM7For(int spn, Packet packet) {
        boolean isId = packet.getId() == 58112;
        if (!isId) {
            return false;
        }
        boolean isTestId = packet.get(0) == 247;
        boolean isFmi = (packet.get(3) & 0x1F) == 31;
        int reqSpn = (((packet.get(3) & 0xE0) << 11) & 0xFF0000) | ((packet.get(2) << 8) & 0xFF00)
                | (packet.get(1) & 0xFF);
        boolean isSpn = spn == reqSpn;
        boolean result = isTestId && isFmi && isSpn;
        return result;
    }

    private static boolean isRequestFor(int pgn, Packet packet) {
        return (packet.getId() == (0xEA00 | ADDR) || packet.getId() == 0xEAFF)
                && packet.get24(0) == pgn;
    }

    private final Sim sim;

    public Engine(Bus bus) throws BusException {
        sim = new Sim(bus);

        // xmsn rate is actually engine speed dependent
        sim.schedule(100, 100, TimeUnit.MILLISECONDS,
                () -> Packet.create(61444, ADDR, combine(NA3, ENGINE_SPEED, NA3)));
        sim.schedule(100, 100, TimeUnit.MILLISECONDS, () -> Packet.create(65248, ADDR, combine(NA4, DISTANCE)));
        sim.response(p -> isRequestFor(65259, p), () -> Packet.create(65259, ADDR, COMPONENT_ID));
        sim.response(p -> isRequestFor(65253, p), () -> Packet.create(65253, ADDR, combine(ENGINE_HOURS, NA4)));
        sim.response(p -> isRequestFor(65260, p), () -> Packet.create(65260, ADDR, VIN));
        sim.response(p -> isRequestFor(54016, p),
                () -> Packet.create(54016, ADDR, combine(ENGINE_CVN1, ENGINE_CAL_ID1)));

        // DM6
        sim.response(p -> isRequestFor(65231, p), () -> Packet.create(65231, ADDR, 0x00, 0x00, 0x00, 0x00, 0x00));
        // DM12
        sim.response(p -> isRequestFor(65236, p), () -> Packet.create(65236, ADDR, 0x00, 0x00, 0x00, 0x00, 0x00));
        // DM23
        sim.response(p -> isRequestFor(64949, p), () -> Packet.create(64949, ADDR, 0x00, 0x00, 0x00, 0x00, 0x00));
        // DM28
        sim.response(p -> isRequestFor(64896, p), () -> Packet.create(64896, ADDR, 0x00, 0x00, 0x00, 0x00, 0x00));
        // DM11
        sim.response(p -> isRequestFor(65235, p),
                () -> Packet.create(0xE8FF, ADDR, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        // DM5
        sim.response(p -> isRequestFor(65230, p),
                () -> Packet.create(65230, ADDR, 0x00, 0x00, 0x14, 0x37, 0xE0, 0x1E, 0xE0, 0x1E));
        // DM26
        sim.response(p -> isRequestFor(0xFDB8, p),
                () -> Packet.create(0xFDB8, ADDR, 0x00, 0x00, 0x00, 0x37, 0xC0, 0x1E, 0xC0, 0x1E));
        // DM20
        sim.response(p -> isRequestFor(0xC200, p),
                () -> Packet.create(0xC200, ADDR,
                        0x0C, 0x00, // Ignition Cycles
                        0 & 0xFF, (0 >> 8) & 0xFF, // OBD Counts
                        // Monitors 3 Bytes SPN, 2 bytes: Num, Dem
                        0xCA, 0x14, 0xF8, 0x00, 0x00, 0 & 0xFF, (0 >> 8) & 0xFF,
                        0xB8, 0x12, 0xF8, 0x00, 0x00, 0 & 0xFF, (0 >> 8) & 0xFF,
                        0xBC, 0x14, 0xF8, 0x01, 0x00, 0 & 0xFF, (0 >> 8) & 0xFF,
                        0xF8, 0x0B, 0xF8, 0 & 0xFF, (0 >> 8) & 0xFF, 0 & 0xFF,
                        (0 >> 8) & 0xFF,
                        0xC6, 0x14, 0xF8, 0x00, 0x00, 0 & 0xFF, (0 >> 8) & 0xFF,
                        0xF2, 0x0B, 0xF8, 0x00, 0x00, 0 & 0xFF, (0 >> 8) & 0xFF,
                        0xC9, 0x14, 0xF8, 0x00, 0x00, 0 & 0xFF, (0 >> 8) & 0xFF,
                        0xEF, 0x0B, 0xF8, 0 & 0xFF, (0 >> 8) & 0xFF, 0 & 0xFF,
                        (0 >> 8) & 0xFF));

        // DM21
        sim.response(p -> isRequestFor(49408, p),
                () -> Packet.create(49408, ADDR, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x77));

        // DM24 supported SPNs
        sim.response(p -> isRequestFor(64950, p),
                () -> Packet.create(64950, ADDR, 0x66, 0x00, 0x1B, 0x01, 0x95, 0x04, 0x1B, 0x02));
        // DM30 response for DM7 Request for SPN 102
        sim.response(p -> isDM7For(102, p),
                () -> Packet.create(0xA4F9, ADDR,
                        0xF7, 0x66, 0x00, 0x12, 0xD0, 0x00, 0x00, 0xFB, 0xFF, 0xFF, 0xFF, 0xFF,
                        0xF7, 0x66, 0x00, 0x10, 0x6A, 0x00, 0x00, 0xFB, 0xFF, 0xFF, 0xFF, 0xFF,
                        0xF7, 0x66, 0x00, 0x0A, 0x0C, 0x00, 0x00, 0xFB, 0xFF, 0xFF, 0xFF, 0xFF));
        // DM30 response for DM7 Request for SPN 1173
        sim.response(p -> isDM7For(1173, p), () -> Packet.create(0xA4F9, ADDR, 0xF7, 0x95, 0x04, 0x10, 0x66, 0x01, 0x00,
                0xFB, 0xFF, 0xFF, 0xFF, 0xFF));
    }

    @Override
    public void close() throws Exception {
        sim.close();
    }

}
