/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.util.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

/**
 * @author Peransin Nicolas
 */
public class ProcessWrapper implements ControlRmi {

    protected Controller parent = null;
    protected ProcessRmi process = null;

    protected String name = null; // Symbolic name
    protected String ipAddress = null; // Machine name or textual IP@
    protected int port = 0;
    protected List<Command> commands = new ArrayList<Command>(); // of Command : synchronized


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
                commands.add(cmd);
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
        if (process == null) return;
        try {
            process.unRegister(this);
        } catch (RemoteException re) { /* Connexion lost  */ }

        process = null;
        for (int indCommand=0; indCommand<commands.size(); indCommand++) {
            Command cmd = commands.get(indCommand);
            if (cmd.getState() != Command.State.idle) {
                cmd.setState(Command.State.idle);
                parent.dispatchEvent(this, cmd, ControlListener.COMMAND_UPDATED);
            }
        }

        parent.dispatchEvent(this, ControlListener.PROCESSOR_DISCONNECTED);
    }

    public synchronized void connect() throws ConnectException {
        if (process != null) return;
        Command[] runningCommands = null;
        try {
            process = (ProcessRmi) RmiConstants.lookup(ipAddress, port, Processor.RMI_NAME);
            runningCommands = process.register(this);
        } catch (MalformedURLException mue) {
            process = null;
            throw new ConnectException(parent.getTexts().get("ProcessWrapper.ErrURL"));
        } catch (NotBoundException nbe) {
            process = null;
            throw new ConnectException(parent.getTexts().get("ProcessWrapper.ErrNoProcessor"));
        } catch (RemoteException re) {
            process = null;
            throw new ConnectException(parent.getTexts().get("ProcessWrapper.ErrNoConnection"));
        }

        parent.dispatchEvent(this, ControlListener.PROCESSOR_CONNECTED);

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
                commands.add(cmd);
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

    public boolean isConnected() { return process != null; }

    public Controller getController() { return parent; }
    public String getName() { return name; }
    public void setName(String pName) {
        name = pName;
        // Can not notify a change : Do not know when really effective
    }

    public String getAddress() { return ipAddress; }
    public void setAddress(String pAddress) {
        pAddress = pAddress.trim();
        if (ipAddress.equals(pAddress))
            return;

        if (pAddress.length()==0)
            throw new IllegalArgumentException(parent.getTexts().get("ProcessWrapper.ErrAddressEmpty"));

        if (isConnected())
            throw new IllegalStateException(parent.getTexts().get("ProcessWrapper.ErrAddressChange"));


        ipAddress = pAddress;
        parent.dispatchEvent(this, ControlListener.PROCESSOR_UPDATED);
    }

    public int getPort() { return port; }
    public void setPort(int pPort) {
        if (pPort == port) {
            return;
        }

        if (pPort <= 0) {
            throw new IllegalArgumentException(parent.getTexts().get("ProcessWrapper.ErrInvalidPort"));
        }
        if (isConnected()) {
            throw new IllegalStateException(parent.getTexts().get("ProcessWrapper.ErrPortChange"));
        }
        port = pPort;
        parent.dispatchEvent(this, ControlListener.PROCESSOR_UPDATED);
    }



    // --- Commands management ----------------------------------------------

    public synchronized int size() { return commands.size(); }
    public synchronized Command get(int pIndex) {
        return commands.get(pIndex);
    }
    public synchronized Command get(String pName) {
        for (int indCommand = 0; indCommand<commands.size(); indCommand++) {
            Command cmd = commands.get(indCommand);
            if (cmd.getName().equals(pName))
                return cmd;
        }
        return null;
    }

    public synchronized void remove(Command pCmd) {
        if (commands.remove(pCmd))
            parent.dispatchEvent(this, pCmd, ControlListener.COMMAND_REMOVED);
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
        if (pName.length()==0)
            throw new IllegalArgumentException(parent.getTexts().get("Command.ErrNameMissing")); // !!

        if (get(pName) != null)
            throw new IllegalArgumentException(parent.getTexts().get("ProcessWrapper.ErrCommandAlreadyExist",
                    new Object[] { pName }));

        pLine = pLine.trim(); // Smart transform in string array with "" management ?
        if (pLine.length()==0)
            throw new IllegalArgumentException(parent.getTexts().get("ProcessWrapper.ErrCommandMissing"));

        Command cmd = new Command(pName, pLine, pPath);
        cmd.addPropertyChangeListener(cmdListener);
        
        commands.add(cmd);
        parent.dispatchEvent(this, cmd, ControlListener.COMMAND_ADDED);
        return cmd;
    }

    public void rename(Command pCmd, String pName) throws IllegalArgumentException {
        pName = pName.trim();
        if (pName.equals(pCmd.getName())) // No change
            return;

        if (get(pName) != null)
            throw new IllegalArgumentException(parent.getTexts().get("ProcessWrapper.ErrCommandAlreadyExist",
                    new Object[] { pName }));

        // No check of bound
        pCmd.setName(pName);
        parent.dispatchEvent(this, pCmd, ControlListener.COMMAND_UPDATED);
    }


    public void start(Command pCmd) throws ConnectException {
        try {
            if (process != null) {
                process.start(pCmd);
            }
        } catch (RemoteException re) {
            disconnect();
            throw new ConnectException(parent.getTexts().get("ProcessWrapper.ErrConnectionLost"));
        }
    }

    public void stop(Command pCmd) throws ConnectException {
        try {
            if (process != null) {
                process.stop(pCmd);
            }
        } catch (RemoteException re) {
            disconnect();
            throw new ConnectException(parent.getTexts().get("ProcessWrapper.ErrConnectionLost"));
        }
    }


    // --- Shortcuts ---------------------------------------------------------
    public synchronized boolean hasRunning() {
        for (int indCommand=0; indCommand<commands.size(); indCommand++) {
            Command cmd = commands.get(indCommand);
            if (cmd.isRunning()) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean hasStopped() {
        for (int indCommand=0; indCommand<commands.size(); indCommand++) {
            Command cmd = commands.get(indCommand);
            if (!cmd.isRunning()) {
                return true;
            }
        }
        return false;
    }

    public synchronized void stopAll() throws ConnectException {
        for (int indCommand = 0; indCommand < commands.size(); indCommand++) {
            stop(commands.get(indCommand));
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