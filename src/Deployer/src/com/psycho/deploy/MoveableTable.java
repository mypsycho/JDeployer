/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.mypsycho.swing.app.Action;
import org.mypsycho.swing.app.utils.SwingHelper;

/**
 * @author Peransin Nicolas
 */
public class MoveableTable extends JPanel {

    protected static final int SPACE = 5;

    protected ControlPane parent = null;
    protected MainTable modelTable = null;
    protected JTable table = null;

    Map<String, Boolean> enableds = new HashMap<String, Boolean>();

    /**
     * Returns the enableds.
     *
     * @return the enableds
     */
    public boolean getEnableds(String prop) {
        Boolean b = enableds.get(prop);
        return b != null ? b : false;
    }

    /**
     * Sets the enableds.
     *
     * @param enableds the enableds to set
     */
    public void setEnableds(String prop, boolean enabled) {
        boolean old = getEnableds(prop);
        enableds.put(prop, enabled);
        firePropertyChange("enableds(" + prop + ")", old, enabled);
    }


    protected MoveableEventListener eventHandler = new MoveableEventListener();
    public MoveableTable(ControlPane pParent) {
        super (new BorderLayout(SPACE, SPACE));
        parent = pParent;

        SwingHelper h = new SwingHelper(this);
        h.add("cmds", new JScrollPane(new JTable(modelTable)), BorderLayout.CENTER);
        h.with("buttons", new GridLayout(0, 1), BorderLayout.LINE_START)
            .add("moveUp", new JButton()).add("moveDown", new JButton());
        
        // - Table
        modelTable = parent.getController().getMainTable();
        JTable table = h.get("cmds?view");
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        // - Buttons

//            if (comps[button].getBorder() instanceof CompoundBorder) {
//                CompoundBorder border = (CompoundBorder) comps[button].getBorder();
//                border = BorderFactory.createCompoundBorder(
//                        border.getOutsideBorder(),
//                        BorderFactory.createEmptyBorder(0, SPACE, 0, SPACE));
//                comps[button].setBorder(border);
//            }

        // Handle events
        table.getSelectionModel().addListSelectionListener(eventHandler);
        table.addMouseListener(eventHandler);
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            table.clearSelection();
        }
    }

    @Action(enabledProperty="moveUp")
    public void moveUp() {
        int row = table.getSelectedRow();
        modelTable.moveUp(row);
        table.setRowSelectionInterval(row - 1, row - 1);
    }
    
    @Action(enabledProperty="moveDown")
    public void moveDown() {
        int row = table.getSelectedRow();
        modelTable.moveDown(row);
        table.setRowSelectionInterval(row + 1, row + 1);
    }
    
    protected class MoveableEventListener extends MouseAdapter
            implements ListSelectionListener {
        
        public void mouseClicked(MouseEvent me) {
            int row = table.getSelectedRow();
            
            if (!((row != -1) && (me.getClickCount() == 2) && (me.getButton() == 1))) {
                return;
            }

            try {
                Command toToggle = modelTable.get(table.getSelectedRow());
                ProcessWrapper process = parent.getController().getWrapper(toToggle);
                if (!process.isConnected()) {
                    process.connect();
                }
                if (!toToggle.isRunning()) {
                    process.start(toToggle);
                } else {
                    process.stop(toToggle);
                }
            } catch (Exception e) {
                parent.setStatus(e);
            }

        }


        public void valueChanged(ListSelectionEvent lse) {
            int row = table.getSelectedRow();
            boolean moveable = (row == -1) && editable;

            setEnableds("moveUp", moveable && modelTable.isRowUpMovable(row));
            setEnableds("moveDown", moveable && modelTable.isRowDownMovable(row));
        }
    }

    protected boolean editable = true;
    public void setEditable(boolean pEditable) {
        if (pEditable == editable) {
            return;
        }
        editable = pEditable;
        if (!editable && table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
        }
        modelTable.setEditable(editable);

        eventHandler.valueChanged(null);
    }

} // endclass MoveableTable