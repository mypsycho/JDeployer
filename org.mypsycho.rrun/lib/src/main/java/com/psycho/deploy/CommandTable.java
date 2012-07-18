/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import javax.swing.table.AbstractTableModel;


/**
 * @author Peransin Nicolas
 */
public class CommandTable extends AbstractTableModel {
    
    enum Column {
        name, cmd, state
    }
    private static final Column[] COLUMNS = Column.values();

    protected ControlPane parent = null;
    protected ProcessWrapper model = null;

    public CommandTable(ControlPane pParent) {
        parent = pParent;
    }

    public String getColumnName(int column) { 
        return COLUMNS[column].name(); 
    }
    public int getColumnCount() { 
        return COLUMNS.length;
    }

    public int getRowCount() { 
        return (model != null) ? model.size() : 0; 
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Command cmd = model.get(rowIndex);
        switch (COLUMNS[columnIndex]) {
            case name:
                return cmd.getName();
            case cmd:
                return cmd.getLine();
            case state:
                return parent.getTexts().getState(cmd);
        }
        return null;
    }

    public void setProcess(ProcessWrapper pProcess) {
        if (model == pProcess) {
            return;
        }
        model = pProcess;
        fireTableDataChanged();
    }

    protected int find(Command pCommand) {
        for (int indRow=0; indRow<model.size(); indRow++) {
            if (model.get(indRow).equals(pCommand)) {
                return indRow;
            }
        }
        return -1;
    }



} // endclass CommandTable