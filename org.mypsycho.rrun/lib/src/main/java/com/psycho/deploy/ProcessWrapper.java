/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Peransin Nicolas
 */
public class ProcessWrapper implements ControlRmi {

    protected Controller parent = null;
    protected ProcessRmi process = null;
    protected boolean connecting = false;

    protected String name = null; // Symbolic name
    protected String ipAddress = null; // Machine name or textual IP@
    protected int port = 0;
    // of Command : synchronized
    protected volatile List<Command> commands = new ArrayList<Command>();


    public ProcessWrapper(Controller pController, String pName,
                          String ip, int pPort) throws IllegalArgumentException {
        try {
            UnicastRemoteObject.exportObject(this, 0);
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        }

        parent = pController;
        name = pName;
        port = pPort;
        ipAddress = ip;
    }


    public void requestProcessed(final Command c) throws RemoteException {
        java.awt.EventQueue.invokeLater(new Runnable() { // To prevent lock
            public void run() { requestProcessedImpl(c); }
        });
    }

    public synchronized void requestProcessedImpl(Command cmd) {
        if (cmd == null) {

            // Implies all executions are stopped
            for (Command command : commands) {
                if (!command.isRunning()) {
                    continue;
                }
                command.setState(Command.State.interrupted, 0);
                parent.dispatchEvent(this, command, ControlListener.COMMAND_UPDATED);
            }

            process = null; // no more distant processor
            parent.dispatchEvent(this, ControlListener.PROCESSOR_DISCONNECTED);
        } else {
            int index = commands.indexOf(cmd);
            if (index == -1) {
                addCommand(cmd);
                cmd.addPropertyChangeListener(cmdListener);
                parent.dispatchEvent(this, cmd, ControlListener.COMMAND_ADDED);
            } else {
                Command command = commands.get(index);
                // Update command with new state
                command.setState(cmd.getState(), cmd.getResult());
                command.setLine(cmd.getLine());
                command.setDirectory(cmd.getDirectory());
                parent.dispatchEvent(this, command, ControlListener.COMMAND_UPDATED);
            }
        }
    }
    
    

    public void disconnect() {
        if (process == null) {
            return;
        }
        try {
            process.unRegister(this);
        } catch (RemoteException re) { /* Connexion lost  */ }

        process = null;
        for (Command cmd : commands) {
            if (cmd.getState() != Command.State.idle) {
                cmd.setState(Command.State.idle);
                parent.dispatchEvent(this, cmd, ControlListener.COMMAND_UPDATED);
            }
        }

        parent.dispatchEvent(this, ControlListener.PROCESSOR_DISCONNECTED);
    }

    public synchronized void connect() throws ConnectException {
        if (process != null) {
            return;
        }
        
        Command[] runningCommands = null;
        try {
            connecting = true;
            parent.dispatchEvent(this, ControlListener.PROCESSOR_CONNECTED);
            
            process = (ProcessRmi) RmiConstants.lookup(ipAddress, port, Processor.RMI_NAME);
            runningCommands = process.register(this);
        } catch (MalformedURLException mue) {
            process = null;
            throw new ConnectException("Illegal RMI url", mue);
        } catch (NotBoundException nbe) {
            process = null;
            throw new ConnectException(err("NotFound", name, ipAddress), nbe);
        } catch (RemoteException re) {
            process = null;
            throw new ConnectException(err("Remote", name), re);
        } finally {
            connecting = false;
            if (process == null) {
                parent.dispatchEvent(this, ControlListener.PROCESSOR_DISCONNECTED);
            }
        }

        

        // Add new Command and update existing
        List<Command> copyList = new LinkedList<Command>(commands);
        for (Command cmd : runningCommands) {
            int index = copyList.indexOf(cmd);
            if (index != -1) {
                Command oldCommand = copyList.remove(index);
                // update state and command
                oldCommand.setLine(cmd.getLine());
                oldCommand.setDirectory(cmd.getDirectory());
                oldCommand.setState(Command.State.running);
                parent.dispatchEvent(this, oldCommand, ControlListener.COMMAND_UPDATED);
            } else {
                addCommand(cmd);
                cmd.addPropertyChangeListener(cmdListener);
                cmd.setState(Command.State.running);
                parent.dispatchEvent(this, cmd, ControlListener.COMMAND_ADDED);
            }
        }

        // All others are forced to IDLE => previous suppress result
        while (!copyList.isEmpty()) { // this algo beacause LinkedList
            Command cmd = copyList.remove(0);
            if (cmd.getState() != Command.State.idle) {
                cmd.setState(Command.State.idle);
                parent.dispatchEvent(this, cmd, ControlListener.COMMAND_UPDATED);
            }
        }
    }

    private void addCommand(Command cmd) {
        assert Thread.holdsLock(this);
        List<Command> tmp = new ArrayList<Command>(commands);
        tmp.add(cmd);
        commands = tmp;
    }
    
    public boolean isConnected() { 
        return connecting || process != null;
    }

    public Controller getController() {
        return parent;
    }
    public String getName() {
        return name;
    }
    public void setName(String pName) {
        name = pName;
        // Can not notify a change : Do not know when really effective
    }

    public String getAddress() { return ipAddress; }
    public void setAddress(String pAddress) {
        pAddress = pAddress.trim();
        if (ipAddress.equals(pAddress))
            return;

        if (pAddress.length()==0) {
            throw new IllegalArgumentException(err("NoAddress"));
        }

        if (isConnected()) {
            throw new IllegalStateException(err("InvalidState"));
        }


        ipAddress = pAddress;
        parent.dispatchEvent(this, ControlListener.PROCESSOR_UPDATED);
    }

    String err(String id) {
        return parent.getTexts().get("Error.Proc." + id);
    }
    
    String err(String id, Object... params) {
        return parent.getTexts().get("Error.Proc." + id, params);
    }
    
    String cerr(String id) {
        return parent.getTexts().get("Error.Command." + id);
    }
    
    String cerr(String id, Object... params) {
        return parent.getTexts().get("Error.Command." + id, params);
    }
    
    public int getPort() {
        return port;
    }
    public void setPort(int pPort) {
        if (pPort == port) {
            return;
        }

        if (pPort <= 0) {
            throw new IllegalArgumentException(err("InvalidPort"));
        }
        if (isConnected()) {
            throw new IllegalStateException(err("InvalidState"));
        }
        port = pPort;
        parent.dispatchEvent(this, ControlListener.PROCESSOR_UPDATED);
    }



    // --- Commands management ----------------------------------------------

    public int size() { 
        return commands.size();
    }
    public Command get(int pIndex) {
        return commands.get(pIndex);
    }
    public Command get(String pName) {
        for (Command cmd : commands) {
            if (cmd.getName().equals(pName)) {
                return cmd;
            }
        }
        return null;
    }

    public synchronized void remove(Command pCmd) {
        List<Command> tmp = new ArrayList<Command>(commands);
        boolean changed = tmp.remove(pCmd);
        commands = tmp;
        if (changed) {
            parent.dispatchEvent(this, pCmd, ControlListener.COMMAND_REMOVED);
        }
    }

    private PropertyChangeListener cmdListener = new PropertyChangeListener() {
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            getController().dispatchEvent(ProcessWrapper.this, (Command) evt.getSource(),
                    ControlListener.COMMAND_UPDATED);
            
        }
    };
    
    public synchronized Command add(String pName, String pLine, String pPath)
            throws IllegalArgumentException {

        pName = pName.trim();
        if (pName.length()==0) {
            throw new IllegalArgumentException(cerr("NoName")); // !!
        }

        if (get(pName) != null) {
            throw new IllegalArgumentException(cerr("Duplicated", pName));
        }

        pLine = pLine.trim(); // Smart transform in string array with "" management ?
        if (pLine.length() == 0) {
            throw new IllegalArgumentException(cerr("Empty"));
        }

        Command cmd = new Command(pName, pLine, pPath);
        cmd.addPropertyChangeListener(cmdListener);
                
        addCommand(cmd);
        parent.dispatchEvent(this, cmd, ControlListener.COMMAND_ADDED);
        return cmd;
    }

    public void rename(Command pCmd, String pName) throws IllegalArgumentException {
        pName = pName.trim();
        if (pName.equals(pCmd.getName())) {// No change
            return;
        }

        if (get(pName) != null) {
            throw new IllegalArgumentException(err("Duplicated", pName));
        }
        
        // No check of bound
        pCmd.setName(pName);
        parent.dispatchEvent(this, pCmd, ControlListener.COMMAND_UPDATED);
    }

    public String getTraces(Command pCmd, boolean std) throws ConnectException {
        try {
            if (process != null) {
                return process.getTraces(pCmd, std);
            }
            return "";
        } catch (RemoteException re) {
            disconnect();
            throw new ConnectException(err("ConnectLost"), re);
        }
    }
    
    
    public void start(Command pCmd) throws ConnectException {
        try {
            if (process != null) {
                process.start(pCmd);
            }
        } catch (RemoteException re) {
            disconnect();
            throw new ConnectException(err("ConnectLost"), re);
        }
    }

    public void stop(Command pCmd) throws ConnectException {
        try {
            if (process != null) {
                process.stop(pCmd);
            }
        } catch (RemoteException re) {
            disconnect();
            throw new ConnectException(err("ConnectLost"), re);
        }
    }


    // --- Shortcuts ---------------------------------------------------------
    public boolean hasRunning() {
        for (Command cmd : commands) {
            if (cmd.isRunning()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasStopped() {
        for (Command cmd : commands) {
            if (!cmd.isRunning()) {
                return true;
            }
        }
        return false;
    }

    public void stopAll() throws ConnectException {
        for (Command cmd : commands) {
            stop(cmd);
        }
    }


    /**
     * Do something TODO.
     * <p>Details of the function.</p>
     *
     * @param cmd
     * @return
     */
    public boolean contains(Command cmd) {
        return commands.contains(cmd);
    }




} // endclass ClientWrapper