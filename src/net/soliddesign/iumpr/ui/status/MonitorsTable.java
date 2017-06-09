/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import net.soliddesign.iumpr.bus.j1939.packets.CompositeMonitoredSystem;
import net.soliddesign.iumpr.bus.j1939.packets.DM26TripDiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DM5DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.DiagnosticReadinessPacket;
import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem;
import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem.Status;
import net.soliddesign.iumpr.bus.j1939.packets.ParsedPacket;

/**
 * Table that displays the {@link MonitoredSystem}s from
 * {@link DM5DiagnosticReadinessPacket} and
 * {@link DM26TripDiagnosticReadinessPacket}
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class MonitorsTable extends StatusTable {

    /**
     * Data Structure for a row of data in the table
     */
    private static class Row {
        private final int id;
        private final String name;
        private final CompositeMonitoredSystem overallSystem;
        private final CompositeMonitoredSystem tripSystem;

        /**
         * Constructor for a new row
         *
         * @param system
         *            the {@link MonitoredSystem} to construct the row with
         * @param overall
         *            true to indicate this is from a
         *            {@link DM5DiagnosticReadinessPacket}; false to indicate
         *            this is from a {@link DM26TripDiagnosticReadinessPacket}
         */
        public Row(MonitoredSystem system, boolean overall) {
            name = system.getName().trim();
            id = system.getId();
            if (overall) {
                overallSystem = new CompositeMonitoredSystem(system);
                tripSystem = new CompositeMonitoredSystem(name, system.getSourceAddress(), id);
            } else {
                tripSystem = new CompositeMonitoredSystem(system);
                overallSystem = new CompositeMonitoredSystem(name, system.getSourceAddress(), id);
            }
        }

        /**
         * Updates the row with a new {@link MonitoredSystem}
         *
         * @param system
         *            the {@link MonitoredSystem} to add
         * @param overall
         *            true to indicate this is from a
         *            {@link DM5DiagnosticReadinessPacket}; false to indicate
         *            this is from a {@link DM26TripDiagnosticReadinessPacket}
         *
         * @return true if the Status changed
         */
        public boolean addSystem(MonitoredSystem system, boolean overall) {
            if (overall) {
                return overallSystem.addMonitoredSystems(system);
            } else {
                return tripSystem.addMonitoredSystems(system);
            }
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Status getOverallStatus() {
            return overallSystem == null ? null : overallSystem.getStatus();
        }

        public Status getTripStatus() {
            return tripSystem == null ? null : tripSystem.getStatus();
        }
    }

    /**
     * The Classes this table will display by column
     */
    private static final Class<?>[] COLUMN_CLASSES = new Class[] { String.class, Status.class, Status.class };

    /**
     * The name for display of the header columns
     */
    private static final String[] COLUMN_HEADERS = new String[] { "Vehicle Monitors", "Trip Status (DM26)",
            "Overall Status (DM5)" };

    private static final int NAME_COLUMN = 0;

    private static final int OVERALL_COLUMN = 2;

    private static final long serialVersionUID = -375381654128886556L;

    private static final int TRIP_COLUMN = 1;

    private DefaultTableModel model;

    private List<Row> rows;

    public MonitorsTable() {
        super();
        setDefaultRenderer(Status.class, new StatusRenderer());
    }

    /**
     * Finds the index of the given {@link MonitoredSystem} in the Rows
     *
     * @param system
     *            the {@link MonitoredSystem} of interest
     * @return the index or -1 if not found
     */
    private int getIndex(MonitoredSystem system) {
        for (int i = 0; i < getRows().size(); i++) {
            if (getRows().get(i).getId() == system.getId()) {
                return i;
            }
        }
        return -1;
    }

    private List<Row> getRows() {
        if (rows == null) {
            rows = new ArrayList<>();
        }
        return rows;
    }

    @Override
    protected DefaultTableModel getTableModel() {
        if (model == null) {
            model = new DefaultTableModel(COLUMN_HEADERS, 0) {

                private static final long serialVersionUID = 3301138182632120604L;

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return COLUMN_CLASSES[columnIndex];
                }

                @Override
                public int getColumnCount() {
                    return COLUMN_HEADERS.length;
                }

                @Override
                public int getRowCount() {
                    return getRows().size();
                }

                @Override
                public Object getValueAt(int row, int column) {
                    final Row rowValue = getRows().get(row);
                    switch (column) {
                    case TRIP_COLUMN:
                        return rowValue.getTripStatus();
                    case OVERALL_COLUMN:
                        return rowValue.getOverallStatus();
                    case NAME_COLUMN:
                        return rowValue.getName();
                    }
                    throw new IllegalArgumentException();
                }

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
        }
        return model;
    }

    @Override
    protected void process(ParsedPacket p) {
        if (p instanceof DiagnosticReadinessPacket) {
            DiagnosticReadinessPacket packet = (DiagnosticReadinessPacket) p;
            // The lists are used here to the systems are put into the table in
            // a consistent order each time the application is used
            processSystems(packet.getContinuouslyMonitoredSystems(), p instanceof DM5DiagnosticReadinessPacket);
            processSystems(packet.getNonContinuouslyMonitoredSystems(), p instanceof DM5DiagnosticReadinessPacket);
        }
    }

    private void processSystem(boolean overall, MonitoredSystem system) {
        int index = getIndex(system);
        if (index == -1) {
            getRows().add(new Row(system, overall));
            getTableModel().fireTableDataChanged();
        } else {
            if (getRows().get(index).addSystem(system, overall)) {
                getTableModel().fireTableDataChanged();
            }
        }
    }

    private void processSystems(List<MonitoredSystem> systems, boolean overall) {
        for (MonitoredSystem system : systems) {
            processSystem(overall, system);
        }
    }
}
