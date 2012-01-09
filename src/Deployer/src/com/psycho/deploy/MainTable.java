/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.util.LinkedList;
import java.util.List;

import javax.swing.table.AbstractTableModel;


/**
 * @author Peransin Nicolas
 */
public class MainTable extends AbstractTableModel implements ControlListener {

    // --- Constantes --------------------------------------------
    
    public enum Column {
        processor, command, state, delay;
    }
    private static final Column[] COLUMNS = Column.values();


    // --- Members --------------------------------------------

    protected List<RowCommand> commands = new LinkedList<RowCommand>();

    Controller parent;
    
    /**
     * @param controller
     */
    public MainTable(Controller controller) {
        parent = controller;
    }

    // --- TableModel methods -------------------------------------
    public String getColumnName(int column) { 
        return COLUMNS[column].name();
    }
    
    public int getRowCount() { 
        return commands.size();
    }
    
    public int getColumnCount() { 
        return COLUMNS.length;
    }

    
    public Object getValueAt(int row, int col) {
        RowCommand rCmd = commands.get(row);

        switch (COLUMNS[col]) {
            case processor : 
                return rCmd.process.getName();
            case command : 
                return rCmd.command.getName();
            case state : 
                return parent.getTexts().getState(rCmd.command);
            case delay : 
                return rCmd.delay;
            default : 
                return null;
        }
    }
    
    public boolean isCellEditable(int pRow, int pColumn) {
        return editable && (pColumn == Column.delay.ordinal());
    }
    
    public void setValueAt(Object value, int pRow, int pColumn) {
        // Only delay column is editable
        if (pColumn != Column.delay.ordinal()) // Internal error
            throw new IllegalArgumentException("Unexpected change at column " + pColumn
                    + " : " + value);

        if (value instanceof String) {
            setValueAt((String) value, pRow, pColumn);
        } else if (value instanceof Float) {
            setValueAt((Float) value, pRow, pColumn);
        } else { // Internal error
            throw new IllegalArgumentException("Unexpected value at delay column : "
                    + value);
        }
    }

    public void setValueAt(String value, int pRow, int pColumn) {
        try { // Input error
            setValueAt(new Float(value), pRow, pColumn);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException(parent.getTexts().get("Error.Sequence.InvalidDelay", value));
        }
    }

    public void setValueAt(Float value, int pRow, int pColumn) {
        if (value.floatValue() < 0.0) { // Input error
            throw new RuntimeException(parent.getTexts().get("Error.Sequence.InvalidDelay", value));
        }
        (commands.get(pRow)).delay = value;

        fireTableCellUpdated(pRow, pColumn);
    }


    // --- Data access ------------------------------------
    public Command get(int index) {
        return (commands.get(index)).command;
    }

    public void setDelay(Command pCommand, ProcessWrapper pProcess, float pDelay) {
        int index = find(pCommand, pProcess);
        RowCommand row = (RowCommand) commands.get(index);
        row.setDelay(pDelay);
    }

    // --- Movable ---------------------------------
    public boolean isRowUpMovable(int pRow) { 
        return pRow > 0; 
    }

    protected void moveUp(int pRow) {
        RowCommand row = commands.remove(pRow); // Optimized for LinkedList
        commands.add(pRow-1, row);
        fireTableRowsUpdated(pRow-1, pRow);
    }

    protected void moveDown(int pRow) {
        moveUp(pRow+1);
    }
    
    public boolean isRowDownMovable(int pRow) { 
        return pRow < commands.size()-1; 
    }

    protected boolean editable = true;
    public void setEditable(boolean pEditable) {
        editable = pEditable;
    }


    // --- Tabel elements --------------------------------------
    protected class RowCommand {
        protected Float delay = new Float(0);
        protected Command command = null;
        protected ProcessWrapper process = null;

        public RowCommand(Command pCommand, ProcessWrapper pProcess) {
            command = pCommand;
            process = pProcess;
        }

        public void setDelay(float pDelay) {
            delay = new Float(pDelay);
        }
    } // endclass RowCommand

    // --- Handle commands -------------------------------------
    public synchronized void systemChanged(ProcessWrapper pProcess, Command pCmd, int pEvent) {
        switch (pEvent) {
            case ControlListener.COMMAND_ADDED :
                add(pCmd, pProcess);
                break;
            case ControlListener.COMMAND_REMOVED :
                remove(pCmd, pProcess);
                break;
            case ControlListener.COMMAND_UPDATED :
                int index = find(pCmd, pProcess);
                fireTableRowsUpdated(index, index);
                break;

            case ControlListener.PROCESSOR_REMOVED :
                removeAll(pProcess);
                break;
//            case CentralListener.PROCESSOR_ADDED : break; // ??
        }

    }

    protected int find(Command pCommand, ProcessWrapper pProcess) {
        for (int indRow=0; indRow<commands.size(); indRow++) {
            RowCommand row = (RowCommand) commands.get(indRow);
            if (row.command.equals(pCommand) && row.process.equals(pProcess))
                return indRow;
        }
        return -1;
    }

    protected void add(Command pCommand, ProcessWrapper pProcess) {
        commands.add(new RowCommand(pCommand, pProcess));
        fireTableRowsInserted(commands.size()-1, commands.size()-1);
    }

    protected void remove(Command pCommand, ProcessWrapper pProcess) {
        int index = find(pCommand, pProcess);
        commands.remove(index);
        fireTableRowsDeleted(index, index);
    }

    protected void removeAll(ProcessWrapper pProcess) {
        // Warning size changed as elements are removed
        for (int indRow=0; indRow<commands.size(); indRow++) {
            RowCommand row = (RowCommand) commands.get(indRow);
            if (row.process.equals(pProcess)) {
                commands.remove(indRow);
                fireTableRowsDeleted(indRow, indRow);
                indRow--;
            }
        }
    }



} // endclass OrderedCommandsTable