/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.mypsycho.swing.app.ComponentManager;
import org.mypsycho.swing.app.task.Task;
import org.mypsycho.swing.app.utils.SwingHelper;

/**
 * @author Peransin Nicolas
 */
public class ControlPane extends JSplitPane implements ControlListener {


    protected Controller core = null;
    protected WrapperList wrappers = null;
    protected WrapperHeader wrapper = null;
    protected WrapperHeader newWrapper = null;
    protected CommandTable commands = null;
    protected CommandEditor cmdEditor = null;
    protected JTable cmdTable = null;
    
    protected JTabbedPane switchPane = new JTabbedPane();
    
    protected MoveableTable mainTable = null;
    protected ProcessWrapper selected = null;
    
    public ControlPane(Controller pCore) {
        super(JSplitPane.HORIZONTAL_SPLIT);
        core = pCore;
        mainTable = new MoveableTable(this);
        
        commands = new CommandTable(this);
        cmdEditor = new CommandEditor(this);
        cmdTable = new JTable(commands);

        
        // Build graphic
        wrappers = new WrapperList(core);

        wrapper = new WrapperHeader(this);
        newWrapper = new WrapperHeader(this) {
            protected void update() {
                super.update();
                connectB.setAction(getActionMap().get("connect"));
                connectB.setEnabled(false);
            }
        };
        

        SwingHelper h = new SwingHelper(this);
        h.add(LEFT, new JScrollPane(new JList(wrappers)));
        h.with(RIGHT, switchPane)
            .with("global", new BorderLayout())
                .add("new", newWrapper, BorderLayout.PAGE_START)
                .add("table", mainTable, BorderLayout.CENTER)
                .back()
            .with("process", new BorderLayout())
                .add("id", wrapper, BorderLayout.PAGE_START)
                .add("cmds", new JScrollPane(cmdTable), BorderLayout.CENTER)
                .add("editor", cmdEditor, BorderLayout.PAGE_END)
                .back()
            .back();
            
        JList list = h.get("left?view");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // list.setSelectionInterval(0, 0);
        list.setCellRenderer(wrapperRenderer);
        
        cmdTable.setRowSelectionAllowed(true);
        cmdTable.setColumnSelectionAllowed(false);
        cmdTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        
        switchPane.setEnabledAt(1, list.getSelectedValue() != null);
        
        ListEventListenener lel = new ListEventListenener(list);
        list.getSelectionModel().addListSelectionListener(lel);
        // list.addMouseListener(this); // deselection in wrapper list
        cmdTable.getSelectionModel().addListSelectionListener(lel);
        cmdTable.addMouseListener(lel); // deselection in process list
        
        pCore.addControlListener(this);
    }


    public void systemChanged(final ProcessWrapper pProcess, final Command pCmd, final int pEvent) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        systemChanged(pProcess, pCmd, pEvent);
                    }
                });
            return;
        }
        
        if (pProcess != selected) {
            return;
        }
        commands.fireTableDataChanged();

        if (pEvent == ControlListener.COMMAND_ADDED) {
            int last = commands.getRowCount() - 1;
            cmdTable.setRowSelectionInterval(last, last);
        } else if (pEvent == ControlListener.COMMAND_UPDATED) {
            // Prevent selection loss
            int selectedRow = commands.find(pCmd);
            cmdTable.setRowSelectionInterval(selectedRow, selectedRow);
        }
    }


    public UiCommon getTexts() {
        return getController().getTexts();
    }
    
    final ListCellRenderer wrapperRenderer = new DefaultListCellRenderer() {
        Font connectedFont = getFont().deriveFont(Font.PLAIN);  
        Font disconnectedFont = getFont().deriveFont(Font.ITALIC);  
        
        public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index,
                                               isSelected, cellHasFocus);
            if (value == null) {
                setText(getDefaultText());
            } else if (value instanceof ProcessWrapper) {
                ProcessWrapper w = (ProcessWrapper) value;
                setFont(w.isConnected() ? connectedFont : disconnectedFont);
                setText(w.getName());
            }
            return this;
        }
        

    };
    
    String defaultText = " < New processor > ";

    
    /**
     * Returns the defaultText.
     *
     * @return the defaultText
     */
    public String getDefaultText() {
        return defaultText;
    }

    
    /**
     * Sets the defaultText.
     *
     * @param defaultText the defaultText to set
     */
    public void setDefaultText(String defaultText) {
        this.defaultText = defaultText;
    }
    
    
    public Controller getController() { 
        return core; 
    }



    // --- List Event Management -------------------------------------------
    protected class ListEventListenener extends MouseAdapter implements ListSelectionListener {

        
        protected int rowLaunch = -1;
        protected int rowSelected = -1;

        protected JList list = null;

        public ListEventListenener(JList pList) {
            list = pList;
        }

        protected boolean newSelection = false;
        public void valueChanged(ListSelectionEvent e) {
            if (e.getSource() == list.getSelectionModel()) {
                listChanged(e);
            } else if (e.getSource() == cmdTable.getSelectionModel()) {
                tableChanged(e);
            }
        }

        public void tableChanged(ListSelectionEvent e) {
            if (cmdTable.getSelectedRow() == rowSelected) {
                return;
            }
            newSelection = true;
            rowSelected = cmdTable.getSelectedRow();
            if ((rowSelected != -1) && (selected != null)) {
                cmdEditor.setCommand(selected, selected.get(rowSelected));
            } else {
                cmdEditor.setCommand(selected, null);
            }
        }


        public void listChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }
            selected = (ProcessWrapper) list.getSelectedValue();
            if (selected == null) {
                switchPane.setEnabledAt(1, false);
                switchPane.setSelectedIndex(0);
                // cdSwitcher.show(switchPane, GLOBAL_LIST_ID);
            } else {
                switchPane.setEnabledAt(1, true);
                // cdSwitcher.show(switchPane, PROCESS_LIST_ID);
                cmdTable.clearSelection();
                commands.setProcess(selected); // Clear Selection ?
                cmdEditor.setCommand(selected, null);
            }
            wrapper.setProcess(selected);

            newSelection = true;
        }

        // --- Selection off ------------------------------
        public void mouseClicked(MouseEvent e) {
            if (e.getSource() == list) {
                listClicked(e);
            } else if (e.getSource() == cmdTable) {
                tableClicked(e);
            }
        }

        public void tableClicked(MouseEvent me) {
            if (me.getClickCount() == 1) {
                if (cmdTable.getSelectedRow() != -1) { // Save for double click
                    rowLaunch = cmdTable.getSelectedRow();
                }
                if (!newSelection) { // Same row has been selected 
                    cmdTable.clearSelection();
                }
                newSelection = false;
                
            } else if ((me.getClickCount() == 2) && (me.getButton() == 1)) {
                // On double click, start or stop the process
                cmdTable.getSelectionModel().setSelectionInterval(rowLaunch, rowLaunch);
                toggleCommand(selected.get(rowLaunch));
            } // if 3 click or more, element stays selected 
        }

        public void listClicked(MouseEvent e) {
            Point p = new Point(e.getX(), e.getY());
            int index = list.locationToIndex(p);
            Rectangle inside = list.getCellBounds(index, index);
            if (inside.contains(p) && (!newSelection)) {
                list.clearSelection();
            }
            newSelection = false;
        }

    } // endclass ListEventListenener

    public void toggleCommand(final Command toToggle) {
        final ProcessWrapper proc = getController().getWrapper(toToggle);
        
        ComponentManager.getContext(this).getTaskService().execute(new Task<Void, Void>() {

            protected Void doInBackground() throws Exception {
                if (!proc.isConnected()) {
                    proc.connect();
                    // wrapper.update(); //?
                }
                if (!toToggle.isRunning()) {
                    proc.start(toToggle);
                } else {
                    proc.stop(toToggle);
                }
                return null;
            }
            protected void failed(Throwable cause) {
                setStatus(cause);
            }
        });
    }


    /**
     * Sets the status.
     *
     * @param status the status to set
     */
    public void setStatus(Object status) {
        firePropertyChange("status", null, status);
    }


    public boolean isRunning() {
        return launching != null;
    }

    public void startCommands(ProcessWrapper pProcess) throws IllegalStateException {
        if (isRunning()) { // Inner error => No translation
            throw new IllegalStateException("Sequence already in progress");
        }
        new Launcher(pProcess).start();
        firePropertyChange("running", false, true);
    }


    public void stopCommands() throws IllegalStateException {
        if (!isRunning()) { // Inner error => No translation
            throw new IllegalStateException("No sequence in progress");
        }
        launching.interrupt();
    }


    protected Launcher launching = null; // No synchro: accessed by Dispatch Thread
    protected class Launcher extends Thread {
        protected ProcessWrapper process = null;
        protected boolean finished = false;

        public Launcher(ProcessWrapper pProcess) {
            process = pProcess;
            launching = this;
            setEditable(false);
        }

        public void run() {
            if (!finished) {
                if (process != null) {
                    core.startAll(process);
                } else {
                    core.startAll();
                }
                finished = true;
                EventQueue.invokeLater(this);
            } else {
                setEditable(true);
                launching = null;
                firePropertyChange("running", true, false);
            }
       }
    }


    public void setEditable(boolean pEnable) {
        wrapper.setEditable(pEnable); // Disable fields + buttons
        mainTable.setEditable(pEnable); // Disable moving !!!
        cmdEditor.setEditable(pEnable); // Disable fields + buttons
    }



} // endclass CentralPane