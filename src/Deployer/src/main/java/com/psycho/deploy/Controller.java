/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.io.File;
import java.io.IOException;
import java.rmi.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.psycho.deploy.io.XmlAccessor;

/**
 * @author Peransin Nicolas
 */
public class Controller {

    
    private XmlAccessor accessor;

    protected MainTable sortedCommands = new MainTable(this);

    private UiCommon ui;
    
    public Controller(UiCommon texts, XmlAccessor xml) {
        ui = texts;
        accessor = xml;
        addControlListener(sortedCommands);
    }


    public MainTable getMainTable() { 
        return sortedCommands; 
    }



    /**
     * Returns the texts.
     *
     * @return the texts
     */
    public UiCommon getTexts() {
        return ui;
    }
    
    // --- Shortcuts ---------------------------------------------------------
    public void disconnectAll() {
        for (String wrapperName : wrappers.keySet()) {
            wrappers.get(wrapperName).disconnect();
        }
    }

    public void connectAll() {
        for (String wrapperName : wrappers.keySet()) {
            try {
                wrappers.get(wrapperName).connect();
            } catch (ConnectException ce) {
                System.err.println(ce.getMessage());
            }
        }
    }

    public boolean hasConnected() {
        for (String wrapperName : wrappers.keySet()) {
            if ( wrappers.get(wrapperName).isConnected()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasDisconnected() {
        for (String wrapperName : wrappers.keySet()) {
            if (!wrappers.get(wrapperName).isConnected()) {
                return true;
            }
        }
        return false;
    }

    public ProcessWrapper getWrapper(Command cmd) {
        for (ProcessWrapper wrapper : wrappers.values()) {
            if (wrapper.contains(cmd)) {
                return wrapper;
            }
        }
        return null;
    }
    
    public void startAll() {
        // Seek for last to started => prevent from waiting
        int lastToStart = sortedCommands.getRowCount() - 1;
        for (boolean lastFound = false; (lastToStart>=0) && (!lastFound); ) {
            Command cmd = sortedCommands.get(lastToStart);
            boolean active = sortedCommands.isActive(lastToStart);
            lastFound = active && getWrapper(cmd).isConnected() && (!cmd.isRunning());
            if (!lastFound) {
                lastToStart--;
            }
        }

        boolean oneStarted = false;
        for (int indCommand=0; indCommand <= lastToStart; indCommand++) {
            Command cmd = sortedCommands.get(indCommand);
            Float delay = getCommandDelay(indCommand);

            if ((delay.floatValue() > 0.0f) && (oneStarted))
                try {
                    Thread.sleep(Math.round(delay.floatValue()*1000.0f)); // Delay in s
                } catch (InterruptedException ie) { // Stopping launch all !!
                    return;
                }

            if (getWrapper(cmd).isConnected() && sortedCommands.isActive(lastToStart)) {
                if (!cmd.isRunning())
                    try {
                        getWrapper(cmd).start(cmd);
                        oneStarted = true;
                    } catch (ConnectException ce) {
                        System.err.println(ce.getMessage());
                    }
            }
            if (Thread.interrupted()) {
                return;
            }
        }
    }

    Float getCommandDelay(int index) {
        return (Float) sortedCommands.getValueAt(index, MainTable.Column.delay.ordinal());
    }


    
    public void startAll(ProcessWrapper process) {
        // Seek for last to started => prevent from waiting
       int lastToStart = sortedCommands.getRowCount()-1;
       for (boolean lastFound=false; (lastToStart>=0) && (!lastFound); ) {
           Command cmd = sortedCommands.get(lastToStart);
           boolean active = sortedCommands.isActive(lastToStart);
           lastFound = active && process.equals(getWrapper(cmd)) && (!cmd.isRunning());
           if (!lastFound) {
               lastToStart--;
           }
       }

       boolean oneStarted = false;
       for (int iCmd=0; iCmd <= lastToStart; iCmd++) {
           Command cmd = sortedCommands.get(iCmd);
           boolean active = sortedCommands.isActive(lastToStart);
           Float delay = getCommandDelay(iCmd);

           if ((delay.floatValue() > 0.0f) && (oneStarted)) {
               try {
                   Thread.sleep(Math.round(delay.floatValue()*1000.0f));
               } catch (InterruptedException ie) { // Stopping launch all !!
                   return;
               }
           }
           
           if (process.equals(getWrapper(cmd)) && active) {
               if (!cmd.isRunning()) {
                   try {
                       getWrapper(cmd).start(cmd);
                       oneStarted = true;
                   } catch (ConnectException ignored) {}
               }
           }
           if (Thread.interrupted()) {
               return;
           }
       }
   }


    public void stopAll() {
        for (String wrapperName : wrappers.keySet()) {
            try {
                wrappers.get(wrapperName).stopAll();
            } catch (ConnectException ignored) {
            }
        }
    }

    public boolean hasStartable() {
        for (String wrapperName : wrappers.keySet()) {
            ProcessWrapper process = wrappers.get(wrapperName);
            if (process.isConnected() && process.hasStopped()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasStoppable() {
        for (String wrapperName : wrappers.keySet()) {
            ProcessWrapper process = wrappers.get(wrapperName);
            if (process.isConnected() && process.hasRunning()) {
                return true;
            }
        }
        return false;
    }



    // --- Manage Wrappers ----------------------------------------------------

    protected Map<String, ProcessWrapper> wrappers = new HashMap<String, ProcessWrapper>();

    public Set<String> getWrapperNames() {
        return wrappers.keySet();
    }

    public ProcessWrapper getWrapper(String pName) {
        return (ProcessWrapper) wrappers.get(pName.trim());
    }

    public ProcessWrapper add(String pName, String pAddress, int pPort)
            throws IllegalArgumentException {
        pName = pName.trim();
        if (pName.length() == 0) {
            throw new IllegalArgumentException(ui.get("ErrProcNameMissing"));
        }
        if (wrappers.get(pName) != null) {
            throw new IllegalArgumentException(ui.get("ErrProcAlreadyExist", pName ));
        }
        pAddress = pAddress.trim();
        if (pAddress.length() == 0) {
            throw new IllegalArgumentException(ui.get("ErrProcAddressMissing"));
        }
        if (pPort <= 0) {
            throw new IllegalArgumentException(ui.get("ErrProcWrongPort",
                    Integer.toString(pPort) )); // Technical value : no Locale
        }
        ProcessWrapper process = new ProcessWrapper(this, pName, pAddress, pPort);
        wrappers.put(process.getName(), process);
        dispatchEvent(process, ControlListener.PROCESSOR_ADDED);
        return process;
    }

    public void remove(ProcessWrapper pProcess) {
        if (wrappers.remove(pProcess.getName()) != null) {
            dispatchEvent(pProcess, ControlListener.PROCESSOR_REMOVED);
        }
    }

    public void rename(ProcessWrapper pProcess, String pName) throws IllegalArgumentException {
        pName = pName.trim();
        if (pName.equals(pProcess.getName())) {// No change
            return;
        }
        
        // Rejected operation
        if (wrappers.get(pName) != null)
            throw new IllegalArgumentException(ui.get("ErrProcAlreadyExist",
                    new Object[] { pName } ));

        if (pName.length() == 0)
            throw new IllegalArgumentException(ui.get("ErrProcNameMissing"));

        if (wrappers.remove(pProcess.getName()) == null)
            throw new IllegalArgumentException(pProcess.getName()
                    + " processor not found to rename.");  // Internal error => No local


        pProcess.setName(pName);
        wrappers.put(pProcess.getName(), pProcess);
        dispatchEvent(pProcess, ControlListener.PROCESSOR_UPDATED);
    }



    // --- Manage Event Listeners ----------------------------------------------------

    protected List<ControlListener> listeners = new ArrayList<ControlListener>();
    public void addControlListener(ControlListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeControlListener(ControlListener listener) {
        listeners.remove(listener);
    }

    public void dispatchEvent(ProcessWrapper pProcess, int pEvent) {
        if ((wrappers.get(pProcess.getName()) != null) // Only if pProcess is handled
            || (pEvent == ControlListener.PROCESSOR_REMOVED)) {
            dispatchEvent(pProcess, null, pEvent);
        }
    }

    public void dispatchEvent(ProcessWrapper pProcess, Command pCmd, int pEvent) {
        if ((wrappers.get(pProcess.getName()) != null) // Only if pProcess is handled
            || (pEvent == ControlListener.PROCESSOR_REMOVED)) {
            for (int indListener=0; indListener<listeners.size(); indListener++) {
                (listeners.get(indListener)).systemChanged(pProcess, pCmd, pEvent);
            }
        }
    }

    // --- File Management ----------------------------------------------------------

    public String loadFile(File pFileToLoad) throws IOException {
        return accessor.load(this, pFileToLoad);
    }

    public String saveFile(File pFileToSave) throws IOException {
        return accessor.save(this, pFileToSave);
    }


} // endclass CentralCore