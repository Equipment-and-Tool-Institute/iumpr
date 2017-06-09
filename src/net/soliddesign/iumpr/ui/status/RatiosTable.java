/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultRowSorter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import net.soliddesign.iumpr.bus.j1939.packets.DM20MonitorPerformanceRatioPacket;
import net.soliddesign.iumpr.bus.j1939.packets.ParsedPacket;
import net.soliddesign.iumpr.bus.j1939.packets.PerformanceRatio;
import net.soliddesign.iumpr.modules.ReportFileModule;

/**
 * The {@link StatusTable} that display
 * {@link DM20MonitorPerformanceRatioPacket} {@link PerformanceRatio}s
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class RatiosTable extends StatusTable {

    /**
     * Represents a row of data for the table
     */
    private static class Row {
        private Boolean changed;
        private int denominator;
        private final int id;
        private final String name;
        private int numerator;
        private Boolean numeratorEverChanged;
        private final String source;

        public Row(PerformanceRatio ratio, Boolean numeratorChanged) {
            name = ratio.getName();
            source = ratio.getSource();
            id = ratio.getId();
            numerator = ratio.getNumerator();
            denominator = ratio.getDenominator();
            numeratorEverChanged = numeratorChanged;
        }

        /**
         * Processes the given {@link PerformanceRatio}. If the result is an
         * increased Numerator or Denominator, true is returned
         *
         * @param ratio
         *            the {@link PerformanceRatio} to process
         * @return boolean if the row changed
         */
        public boolean update(PerformanceRatio ratio) {
            boolean result = false;
            if (ratio.getNumerator() != numerator) {
                changed = ratio.getNumerator() > numerator;
                numeratorEverChanged = changed;
                numerator = ratio.getNumerator();
                result = true;
            }
            if (ratio.getDenominator() != denominator) {
                denominator = ratio.getDenominator();
                result = true;
            }
            return result;
        }
    }

    /**
     * The Classes this table will display by column
     */
    private static final Class<?>[] COLUMN_CLASSES = new Class[] { Boolean.class, Boolean.class, String.class,
            String.class, Integer.class, Integer.class };

    /**
     * The name for display of the header columns.
     *
     * The Changed column is hidden from display
     */
    private static final String[] COLUMN_HEADERS = new String[] { "Row Changed", "Numerator Changed", "Source",
            "Monitor from DM20", "Numerator", "Denominator" };

    private static final int DENOMINATOR_COLUMN = 5;

    private static final int NAME_COLUMN = 3;

    private static final int NUMERATOR_CHANGED_COLUMN = 1;

    private static final int NUMERATOR_COLUMN = 4;

    private static final int ROW_CHANGED_COLUMN = 0;

    private static final long serialVersionUID = 5016555285354740896L;

    private static final int SOURCE_COLUMN = 2;

    /**
     * Quick lookup Map of ID to {@link PerformanceRatio}
     */
    private final Map<Integer, PerformanceRatio> initialRatios = new HashMap<>();

    private DefaultTableModel model;

    private List<Row> rows;

    public RatiosTable() {
        super();

        HighlightRenderer integerRenderer = new HighlightRenderer(ROW_CHANGED_COLUMN);
        integerRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        setDefaultRenderer(Integer.class, integerRenderer);

        HighlightRenderer highlightRenderer = new HighlightRenderer(NUMERATOR_CHANGED_COLUMN);
        highlightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        getColumnModel().getColumn(NUMERATOR_COLUMN).setCellRenderer(highlightRenderer);

        setDefaultRenderer(String.class, new HighlightRenderer(ROW_CHANGED_COLUMN));

        DefaultRowSorter<?, ?> sorter = (DefaultRowSorter<?, ?>) getRowSorter();
        List<RowSorter.SortKey> list = new ArrayList<>();
        list.add(new RowSorter.SortKey(SOURCE_COLUMN, SortOrder.ASCENDING));
        list.add(new RowSorter.SortKey(NAME_COLUMN, SortOrder.ASCENDING));
        sorter.setSortKeys(list);
        sorter.sort();

        // Hide the Changed Column
        getColumnModel().removeColumn(getColumnModel().getColumn(NUMERATOR_CHANGED_COLUMN));
        getColumnModel().removeColumn(getColumnModel().getColumn(ROW_CHANGED_COLUMN));
    }

    /**
     * Finds the index in the Rows for the given {@link PerformanceRatio}
     *
     * @param ratio
     *            the {@link PerformanceRatio} of interest
     * @return the index or -1 if not found
     */
    private int findIndex(PerformanceRatio ratio) {
        for (int i = 0; i < getRows().size(); i++) {
            if (getRows().get(i).id == ratio.getId()) {
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
                private static final long serialVersionUID = -6229337962755674806L;

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
                    case ROW_CHANGED_COLUMN:
                        return rowValue.changed;
                    case NUMERATOR_CHANGED_COLUMN:
                        return rowValue.numeratorEverChanged;
                    case NAME_COLUMN:
                        return rowValue.name;
                    case NUMERATOR_COLUMN:
                        return rowValue.numerator;
                    case DENOMINATOR_COLUMN:
                        return rowValue.denominator;
                    case SOURCE_COLUMN:
                        return rowValue.source;
                    }
                    return null;
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
        if (p instanceof DM20MonitorPerformanceRatioPacket) {
            DM20MonitorPerformanceRatioPacket packet = (DM20MonitorPerformanceRatioPacket) p;
            packet.getRatios().stream().forEach(ratio -> processRatio(ratio));
        }
    }

    private void processRatio(PerformanceRatio ratio) {
        int index = findIndex(ratio);
        if (index == -1) {
            PerformanceRatio initialRatio = initialRatios.get(ratio.getId());
            Boolean numeratorChanged = null;
            if (initialRatio == null) {
                numeratorChanged = false;
            } else {
                if (ratio.getNumerator() > initialRatio.getNumerator()) {
                    numeratorChanged = true;
                } else if (ratio.getNumerator() < initialRatio.getNumerator()) {
                    numeratorChanged = false;
                }
            }
            getRows().add(new Row(ratio, numeratorChanged));
            getTableModel().fireTableDataChanged();
        } else {
            Row row = getRows().get(index);
            if (row.update(ratio)) {
                getTableModel().fireTableDataChanged();
            }
        }
    }

    /**
     * Sets the {@link ReportFileModule} that provides the initial
     * {@link PerformanceRatio} values
     *
     * @param reportFileModule
     *            the {@link ReportFileModule} to set
     */
    public void setReportFileModule(ReportFileModule reportFileModule) {
        Set<PerformanceRatio> ratios = reportFileModule.getInitialRatios();
        for (PerformanceRatio ratio : ratios) {
            initialRatios.put(ratio.getId(), ratio);
        }
    }

}
