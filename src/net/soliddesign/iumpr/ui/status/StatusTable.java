/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.etools.j1939tools.bus.j1939.packets.ParsedPacket;

/**
 * Abstract JTable that contains common code for display of status information
 * while monitoring completion of tests
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public abstract class StatusTable extends JTable {

    private static final long serialVersionUID = -2915833751571563445L;

    public StatusTable() {
        setAutoCreateColumnsFromModel(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        setGridColor(Color.BLACK);
        setModel(getTableModel());
        setAutoCreateRowSorter(true);
        ((DefaultTableCellRenderer) getTableHeader().getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.CENTER);
    }

    /**
     * Returns the {@link DefaultTableModel} used in this table
     *
     * @return {@link DefaultTableModel}
     */
    protected abstract TableModel getTableModel();

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        int rendererWidth = component.getPreferredSize().width;
        TableColumn tableColumn = getColumnModel().getColumn(column);
        tableColumn.setPreferredWidth(
                Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
        return component;
    }

    /**
     * Processes a {@link ParsedPacket} for display
     *
     * @param packet
     *            the {@link ParsedPacket} to process
     */
    protected abstract void process(ParsedPacket packet);

}
