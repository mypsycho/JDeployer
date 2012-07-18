/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 * @author Peransin Nicolas
 */
public class Command implements Serializable {

    PropertyChangeSupport listeners = new PropertyChangeSupport(this);
    
    public enum State {
        idle, running, interrupted, error, finished
    }


    protected String name = null;
    protected String line = "";
    protected String directory = null;

    protected State state = State.idle;

    protected Integer result = 0;

    public Command(String pName, String pLine, String pDirectory) {
        setName(pName); // ??? Forbid   \ / : * ? < > |
        setLine(pLine);
        setDirectory(pDirectory);
    }

    public void addPropertyChangeListener(
            PropertyChangeListener listener) {
        listeners.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        listeners.removePropertyChangeListener(listener);
    }


    public void setName(String pName) {
        String old = name;
        pName = pName.trim();
        if (pName.length() == 0) {
            throw new IllegalArgumentException("Command.NoName");
        }
        if (isRunning()) {
            throw new IllegalStateException("Command.Running");
        }

        name = pName;
        listeners.firePropertyChange("name", old, name);
    }
    
    public String getName() { 
        return name; 
    }

    public String getLine() { 
        return line; 
    }
    
    public void setLine(String pLine) {
        String old = line;
        pLine = pLine.trim();
        if (pLine.equals(line)) {
            return;
        }
        if (pLine.length() == 0) {
            throw new IllegalArgumentException("Command.Empty");
        }

        line = pLine;
        listeners.firePropertyChange("line", old, directory);
    }

    public String getDirectory() { 
        return directory; 
    }
    public void setDirectory(String pDirectory) {
        String old = directory;
        if (pDirectory != null) {
            pDirectory = pDirectory.trim();
            if (pDirectory.equals(directory)) {
                return;
            }
        } else if (directory == null) {
            return;
        }

        directory = (pDirectory != null) && (pDirectory.length() >= 0) ? pDirectory : null;
        listeners.firePropertyChange("directory", old, directory);
    }



    public void setState(State pState, Integer pResult) { 
        state = pState;
        result = pResult; 
    }
    
    public void setState(State pState) { 
        setState(pState, null);
    }
    
    public State getState() { 
        return state; 
    }
    public Integer getResult() { 
        return result; 
    }


    public boolean isRunning() {
        return state == State.running;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    public boolean equals(Object pObject) {
        if (pObject instanceof Command) {
            return name.equals( ((Command) pObject).name);
        } else if (pObject instanceof Executor) {
            return equals(((Executor) pObject).getCommand());
        } else {
            return false;
        }
    }




} // endclass Command