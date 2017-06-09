/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui.status;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Changes the background color when the value in the row has changed
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 */
public class HighlightRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 2491058020605869108L;

    private final int changedColumn;

    /**
     * Constructor
     *
     * @param changedColumn
     *            the column index that determines if the value changed
     */
    public HighlightRenderer(int changedColumn) {
        this.changedColumn = changedColumn;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int col) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

        int modelRow = table.convertRowIndexToModel(row);
        Boolean status = (Boolean) table.getModel().getValueAt(modelRow, changedColumn);
        if (status == null) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        } else {
            setBackground(status ? Color.GREEN : Color.RED);
        }
        return this;
    }
}
