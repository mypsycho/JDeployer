/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.SwingPropertyChangeSupport;


/**
 * Note :
 *  - Concerning synchronization :
 *   Global synchronization to avoid lockup on executors and central synchronization
 *   
 * @author Peransin Nicolas
 */
public class Processor implements ProcessRmi {

    public static InetAddress HERE = null;
    public static final String RMI_NAME = Processor.class.getName();

    // Remember the launch order
    protected List<Executor> executors = new LinkedList<Executor>(); // List of Array management
    protected List<ControlRmi> centrals = new  LinkedList<ControlRmi>(); // Set of CentralRmi : unicity to be handled

    protected UiCommon texts;


    PropertyChangeSupport listeners = new SwingPropertyChangeSupport(this);
    
    public Processor(UiCommon ui) {
        texts = ui;

        try {
            Registry reg = RmiConstants.getRmiRegistry();
            UnicastRemoteObject.exportObject(this, 0);
            reg.bind(RMI_NAME, this);
            HERE = InetAddress.getLocalHost();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } catch (AlreadyBoundException abe) { // ClientCore already launch : bye
            throw new RuntimeException(texts.get("Error.AlreadyStarted"));
        }
    }
    
    /**
     * Returns the texts.
     *
     * @return the texts
     */
    public UiCommon getTexts() {
        return texts;
    }


    public void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        listeners.removePropertyChangeListener(listener);
    }
    
    
    public int getPort() {
        return RmiConstants.getRmiPort();
    }


    void firePropertyChange(Executor exe, String prop) {
        listeners.firePropertyChange(new PropertyChangeEvent(exe, prop, null, exe));
    }
    
    // --- Remote Control --------------------------------------

    public synchronized void start(Command cmd) throws RemoteException {
        if (!executors.contains(cmd)) {
            Executor exe = new Executor(this, cmd);
            firePropertyChange(exe, "new");
            
            exe.start();
            executors.add(exe);

            // Notify others Centrals of start !!!
            eventInCommand(cmd);
            

        } else {// Synchronisation error => No call back, no exception
            throw new ConcurrentModificationException(texts.get("Error.Command.AlreadyStarted", 
                    cmd.getName()));
        }
    }

    public synchronized void stop(Command cmd) throws RemoteException {
        int place = executors.indexOf(cmd);
        if (place >= 0) {
            Executor exe = executors.get(place);
            exe.endProcess();
        }  else {// Synchronisation error => No call back, no exception
            throw new ConcurrentModificationException(texts.get("Error.Command.NotFound", 
                    cmd.getName()));
        }
    }

    // Register centrals
    public synchronized Command[] register(ControlRmi ctr) throws RemoteException {
        if (!centrals.contains(ctr)) {
            centrals.add(ctr);
        }

        Command[] result = new Command[executors.size()];
        for (int indExe=0; indExe<result.length; indExe++) {
            result[indExe] = (executors.get(indExe)).getCommand();
        }
        return result;
    }

    public synchronized void unRegister(ControlRmi ctr) throws RemoteException {
        centrals.remove(ctr);
    }


    // --- Notification to centrals --------------------------------------

    public synchronized void started(Command cmd) {
        eventInCommand(cmd);
    }

    public synchronized void finished(Command cmd) {
        if (executors.remove(cmd)) {
            eventInCommand(cmd);
        } else {// Internal error
            throw new RuntimeException("Wrong state with \"" + cmd.getName()
                                       + "\" : Cannot remove");
        }
    }

    protected synchronized void eventInCommand(Command cmd) {
        // CAUTION list size and order changing on error
        for (Iterator<ControlRmi> i = centrals.iterator(); i.hasNext() ;  ) {

            ControlRmi central = i.next();
            try {
                central.requestProcessed(cmd);
            } catch (RemoteException ex) {
                i.remove();
            }
        }
    }


    public synchronized void disconnect() {
        while (!centrals.isEmpty()) {
            ControlRmi control = centrals.get(0);
            try {
                control.requestProcessed(null);
            } catch (RemoteException ignored) { }
            centrals.remove(0);
        }

        for (int indExe=0; indExe < executors.size(); indExe++) {
            Executor exe = executors.get(indExe);
            exe.endProcess();
        }
    }

    public synchronized Executor[] getExecutors() {
        return executors.toArray(new Executor[executors.size()]);
    }

} // endclass ClientCore