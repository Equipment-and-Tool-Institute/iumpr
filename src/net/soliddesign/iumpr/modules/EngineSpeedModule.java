/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import java.util.concurrent.TimeUnit;

import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.packets.EngineSpeedPacket;
import org.etools.j1939tools.modules.DateTimeModule;

/**
 * {@link FunctionalModule} used to determine if the Engine is communicating
 * and/or running
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class EngineSpeedModule extends FunctionalModule {

    public EngineSpeedModule() {
        super(DateTimeModule.getInstance());
    }

    private EngineSpeedPacket getEngineSpeedPacket() {
        // The transmission rate changes based upon the engine speed. 100 ms is
        // the longest period between messages when the engine is off
        return getJ1939().read(EngineSpeedPacket.class, J1939.ENGINE_ADDR, 300, TimeUnit.MILLISECONDS)
                .flatMap(p -> p.left).orElse(null);
    }

    /**
     * Returns true if the Engine Speed is returned from the engine indicating
     * the engine is communicating
     *
     * @return true if the engine is communicating; false if the engine is not
     *         communicating
     */
    public boolean isEngineCommunicating() {
        return getEngineSpeedPacket() != null;
    }

    /**
     * Returns true if the Engine is communicating with an Engine Speed less
     * than 300 RPM.
     *
     * @return true if the engine is not running; false otherwise
     */
    public boolean isEngineNotRunning() {
        EngineSpeedPacket packet = getEngineSpeedPacket();
        return !(packet == null || packet.isError() || packet.isNotAvailable() || packet.getEngineSpeed() > 300);
    }

}
