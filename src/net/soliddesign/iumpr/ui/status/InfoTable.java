/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.DefaultTableModel;

import org.etools.j1939tools.j1939.packets.DM20MonitorPerformanceRatioPacket;
import org.etools.j1939tools.j1939.packets.DM21DiagnosticReadinessPacket;
import org.etools.j1939tools.j1939.packets.ParsedPacket;

import net.soliddesign.iumpr.modules.DiagnosticReadinessModule;

/**
 * The {@link StatusTable} that display {@link DM21DiagnosticReadinessPacket}
 * data
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class InfoTable extends StatusTable {

    private static class Row {
        private Boolean changed;
        private final String name;
        private final String unit;
        private Double value;

        public Row(String name, String unit) {
            this.name = name;
            this.unit = unit;
        }

        public boolean setValue(double value) {
            if (this.value == null) {
                this.value = value;
                return true;
            } else if (value != this.value) {
                changed = value > this.value;
                this.value = value;
                return true;
            }
            return false;
        }
    }

    private static final int CHANGED_COLUMN = 0;

    /**
     * The Classes this table will display by column
     */
    private static final Class<?>[] COLUMN_CLASSES = new Class[] { Boolean.class, String.class, Double.class,
            String.class };

    /**
     * The name for display of the header columns
     */
    private static final String[] COLUMN_HEADERS = new String[] { "Changed", "Name", "Value", "Unit" };
    private static final int DSCC_ROW = 1;
    private static final int IGN_ROW = 2;
    private static final int NAME_COLUMN = 1;
    private static final int OBD_ROW = 3;
    private static final long serialVersionUID = -3970982945546533844L;
    private static final int TSCC_ROW = 0;
    private static final int UNITS_COLUMN = 3;
    private static final int VALUE_COLUMN = 2;

    private int ignitionCycles = 0;

    private DefaultTableModel model;

    private int obdCounts = 0;

    /**
     * The Packets, by source address, that are being sent for DM20 request
     */
    private final Map<Integer, DM20MonitorPerformanceRatioPacket> packetMap = new HashMap<>();

    private List<Row> rows;

    public InfoTable() {
        super();
        getColumnModel().removeColumn(getColumnModel().getColumn(CHANGED_COLUMN));
        setDefaultRenderer(Double.class, new DecimalRenderer(CHANGED_COLUMN));
        setDefaultRenderer(String.class, new HighlightRenderer(CHANGED_COLUMN));
    }

    private List<Row> getRows() {
        if (rows == null) {
            rows = new ArrayList<>(4);
            rows.add(new Row("Time Since Code Clear from DM21", "Minutes"));
            rows.add(new Row("Distance Since Code Clear from DM21", "Miles"));
            rows.add(new Row("Ignition Cycles from DM20", "Cycles"));
            rows.add(new Row("OBD Monitoring Conditions Encountered from DM20", "Counts"));
        }
        return rows;
    }

    @Override
    protected DefaultTableModel getTableModel() {
        if (model == null) {

            model = new DefaultTableModel(COLUMN_HEADERS, 0) {

                private static final long serialVersionUID = 8054504898616084411L;

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return COLUMN_CLASSES[columnIndex];
                }

                @Override
                public int getRowCount() {
                    return getRows().size();
                }

                @Override
                public Object getValueAt(int row, int column) {
                    Row rowValue = getRows().get(row);
                    switch (column) {
                    case VALUE_COLUMN:
                        return rowValue.value;
                    case UNITS_COLUMN:
                        return rowValue.unit;
                    case CHANGED_COLUMN:
                        return rowValue.changed;
                    case NAME_COLUMN:
                        return rowValue.name;
                    }

                    return super.getValueAt(row, column);
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
        boolean update = false;
        if (p instanceof DM21DiagnosticReadinessPacket) {
            DM21DiagnosticReadinessPacket packet = (DM21DiagnosticReadinessPacket) p;
            update |= getRows().get(TSCC_ROW).setValue(packet.getMinutesSinceDTCsCleared());
            update |= getRows().get(DSCC_ROW).setValue(packet.getMilesSinceDTCsCleared());
        } else if (p instanceof DM20MonitorPerformanceRatioPacket) {
            DM20MonitorPerformanceRatioPacket packet = (DM20MonitorPerformanceRatioPacket) p;
            packetMap.put(p.getSourceAddress(), packet);

            Collection<DM20MonitorPerformanceRatioPacket> packets = packetMap.values();

            int cycles = DiagnosticReadinessModule.getIgnitionCycles(packets);
            if (cycles > -1) {
                ignitionCycles = cycles;
                update |= getRows().get(IGN_ROW).setValue(ignitionCycles);
            }

            int counts = DiagnosticReadinessModule.getOBDCounts(packets);
            if (counts > -1) {
                obdCounts = counts;
                update |= getRows().get(OBD_ROW).setValue(obdCounts);
            }
        }
        if (update) {
            getTableModel().fireTableDataChanged();
        }
    }
}
