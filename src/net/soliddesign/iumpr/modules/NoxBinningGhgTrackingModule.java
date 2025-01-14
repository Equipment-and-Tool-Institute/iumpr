/**
 * Copyright 2023 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.modules;

import static org.etools.j1939tools.modules.GhgTrackingModule.GHG_ACTIVE_100_HR;
import static org.etools.j1939tools.modules.GhgTrackingModule.GHG_ACTIVE_GREEN_HOUSE_100_HR;
import static org.etools.j1939tools.modules.GhgTrackingModule.GHG_ACTIVE_HYBRID_100_HR;
import static org.etools.j1939tools.modules.GhgTrackingModule.GHG_ACTIVE_HYBRID_CHG_DEPLETING_100_HR;
import static org.etools.j1939tools.modules.GhgTrackingModule.GHG_STORED_100_HR;
import static org.etools.j1939tools.modules.GhgTrackingModule.GHG_STORED_GREEN_HOUSE_100_HR;
import static org.etools.j1939tools.modules.GhgTrackingModule.GHG_STORED_HYBRID_100_HR;
import static org.etools.j1939tools.modules.GhgTrackingModule.GHG_STORED_HYBRID_CHG_DEPLETING_100_HR;
import static org.etools.j1939tools.modules.GhgTrackingModule.GHG_TRACKING_LIFETIME_GREEN_HOUSE_PG;
import static org.etools.j1939tools.modules.GhgTrackingModule.GHG_TRACKING_LIFETIME_HYBRID_CHG_DEPLETING_PG;
import static org.etools.j1939tools.modules.GhgTrackingModule.GHG_TRACKING_LIFETIME_HYBRID_PG;
import static org.etools.j1939tools.modules.GhgTrackingModule.GHG_TRACKING_LIFETIME_PG;
import static org.etools.j1939tools.modules.NOxBinningModule.NOx_LIFETIME_ACTIVITY_PGs;
import static org.etools.j1939tools.modules.NOxBinningModule.NOx_LIFETIME_PGs;
import static org.etools.j1939tools.modules.NOxBinningModule.NOx_TRACKING_ACTIVE_100_HOURS_PGs;
import static org.etools.j1939tools.modules.NOxBinningModule.NOx_TRACKING_STORED_100_HOURS_PGs;

import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.etools.j1939tools.CommunicationsListener;
import org.etools.j1939tools.bus.BusResult;
import org.etools.j1939tools.j1939.packets.DM24SPNSupportPacket;
import org.etools.j1939tools.j1939.packets.GenericPacket;
import org.etools.j1939tools.modules.DateTimeModule;
import org.etools.j1939tools.modules.GhgTrackingModule;
import org.etools.j1939tools.modules.NOxBinningModule;
import org.etools.j1939tools.utils.CollectionUtils;

import net.soliddesign.iumpr.controllers.ResultsListener;

/**
 */
public class NoxBinningGhgTrackingModule extends FunctionalModule {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Not a concern in desktop app.")
    static public class NoResponseException extends RuntimeException {

    }

    private final GhgTrackingModule ghgTrackingModule;
    private final NOxBinningModule nOxBinningModule;

    public NoxBinningGhgTrackingModule() {
        this(DateTimeModule.getInstance(), new GhgTrackingModule(DateTimeModule.getInstance()),
                new NOxBinningModule(DateTimeModule.getInstance()));
    }

    public NoxBinningGhgTrackingModule(DateTimeModule dateTimeModule, GhgTrackingModule ghgTrackingModule,
            NOxBinningModule nOxBinningModule) {
        super(dateTimeModule);
        this.ghgTrackingModule = ghgTrackingModule;
        this.nOxBinningModule = nOxBinningModule;
    }

    public void reportInformation(ResultsListener listener, List<Integer> obdAddresses) {
        for (int sa : obdAddresses) {
            getJ1939()
                    .requestDS("DS DM24 Request", DM24SPNSupportPacket.class, sa, listener)
                    .getPacket()
                    .flatMap(p -> p.left)
                    .ifPresent(p -> {
                        List<Integer> spns = p.getSupportedSpns().stream().map(spn -> spn.getSpn())
                                .collect(Collectors.toList());
                        if (spns.contains(12675)) {
                            testSp12675(sa, listener);
                        }
                        if (spns.contains(12730)) {
                            testSp12730(sa, listener);
                        }
                        if (spns.contains(12691)) {
                            testSp12691(sa, listener);
                        }
                        if (spns.contains(12797)) {
                            testSp12797(sa, listener);
                        }
                        if (spns.contains(12783)) {
                            testSp12783(sa, listener);
                        }
                    });
        }

    }

    private List<GenericPacket> requestPackets(CommunicationsListener listener, int sa, int... pgns) {
        IntFunction<GenericPacket> mapper = pgn -> {
            BusResult<GenericPacket> requestDS = getJ1939().requestDS(null, pgn,
                    getJ1939().createRequestPacket(pgn, sa), listener);
            Optional<GenericPacket> ret = requestDS.getPacket().flatMap(p -> p.left);
            if (ret.isEmpty()) {
                throw new NoResponseException();
            }
            return ret.get();
        };
        try {
            return IntStream.of(pgns)
                    .mapToObj(mapper)
                    .collect(Collectors.toList());
        } catch (NoResponseException e) {
            return List.of();
        }
    }

    private void testSp12675(int sa, ResultsListener listener) {
        int[] pgns = Stream.of(new int[][] { NOx_LIFETIME_PGs, NOx_LIFETIME_ACTIVITY_PGs })
                .flatMapToInt(x -> IntStream.of(x))
                .filter(x -> x != 0)
                .toArray();
        var nOxPackets = requestPackets(listener, sa, pgns);
        if (nOxPackets.size() == pgns.length) {
            listener.onResult(nOxBinningModule.format(nOxPackets));
        }

        int[] pgns2 = CollectionUtils.join(NOx_TRACKING_ACTIVE_100_HOURS_PGs,
                NOx_TRACKING_STORED_100_HOURS_PGs);
        var nOx100HourPackets = requestPackets(listener, sa, pgns2);
        if (nOx100HourPackets.size() == 2) {
            listener.onResult(nOxBinningModule.format(nOx100HourPackets));
        }
    }

    private void testSp12691(int sa, ResultsListener listener) {
        int[] pgns = { GHG_TRACKING_LIFETIME_GREEN_HOUSE_PG };
        var lifetimeGhgPackets = requestPackets(listener, sa, pgns);

        var ghg100HrPackets = requestPackets(listener, sa,
                GHG_ACTIVE_GREEN_HOUSE_100_HR,
                GHG_STORED_GREEN_HOUSE_100_HR);

        if (ghg100HrPackets.size() == 2 || lifetimeGhgPackets.size() == pgns.length) {
            listener.onResult(ghgTrackingModule.formatTechTable(Stream.concat(lifetimeGhgPackets.stream(),
                    ghg100HrPackets.stream())
                    .collect(Collectors.toList())));
        }
    }

    private void testSp12730(int sa, ResultsListener listener) {
        int[] pgns = { GHG_TRACKING_LIFETIME_PG };
        var ghgTrackingLifetimePackets = requestPackets(listener, sa, pgns);

        int[] pgns1 = { GHG_ACTIVE_100_HR, GHG_STORED_100_HR };
        var ghgTrackingPackets = requestPackets(listener, sa, pgns1);

        if (ghgTrackingLifetimePackets.size() == pgns.length || ghgTrackingPackets.size() == pgns1.length) {
            listener.onResult(ghgTrackingModule.formatTrackingTable(Stream.concat(ghgTrackingLifetimePackets.stream(),
                    ghgTrackingPackets.stream())
                    .collect(Collectors.toList())));
        }
    }

    private void testSp12783(int sa, ResultsListener listener) {
        int[] pgns = { GHG_TRACKING_LIFETIME_HYBRID_CHG_DEPLETING_PG };
        var ghgLifeTimePackets = requestPackets(listener, sa, pgns);

        int[] pgns1 = { GHG_STORED_HYBRID_CHG_DEPLETING_100_HR, GHG_ACTIVE_HYBRID_CHG_DEPLETING_100_HR };
        var hybridChargeOpsPackets = requestPackets(listener, sa, pgns1);

        if (hybridChargeOpsPackets.size() == pgns1.length || ghgLifeTimePackets.size() == pgns.length) {
            listener.onResult(ghgTrackingModule.formatXevTable(Stream.concat(ghgLifeTimePackets.stream(),
                    hybridChargeOpsPackets.stream())
                    .collect(Collectors.toList())));
        }
    }

    private void testSp12797(int sa, ResultsListener listener) {
        int[] pgns = { GHG_TRACKING_LIFETIME_HYBRID_PG };
        var ghgTrackingPackets = requestPackets(listener, sa, pgns);

        int[] hybridLifeTimePgs = { GHG_STORED_HYBRID_100_HR, GHG_ACTIVE_HYBRID_100_HR };
        var ghgPackets = requestPackets(listener, sa, hybridLifeTimePgs);

        if (ghgTrackingPackets.size() == pgns.length || ghgPackets.size() == hybridLifeTimePgs.length) {
            listener.onResult(ghgTrackingModule.formatXevTable(Stream.concat(ghgTrackingPackets.stream(),
                    ghgPackets.stream())
                    .collect(Collectors.toList())));
        }
    }

}
