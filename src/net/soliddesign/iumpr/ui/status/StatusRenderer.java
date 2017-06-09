/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem;
import net.soliddesign.iumpr.bus.j1939.packets.MonitoredSystem.Status;

/**
 * Renders a {@link MonitoredSystem} {@link Status} in the table
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 */
public class StatusRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 5323779374353482225L;

    public StatusRenderer() {
        super();
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        setValue(value == null ? "" : value.toString().trim());

        if (value == Status.COMPLETE) {
            setBackground(Color.GREEN);
        } else if (value == Status.NOT_SUPPORTED) {
            setBackground(Color.LIGHT_GRAY);
        } else if (isSelected) {
            setBackground(table.getSelectionBackground());
        } else {
            setBackground(table.getBackground());
        }

        return this;
    }
}