/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.rmi.RemoteException;
import java.rmi.Remote;
import java.util.ConcurrentModificationException;

/**
 * @author Peransin Nicolas
 */
public interface ProcessRmi extends Remote {

    void start(Command cmd) throws ConcurrentModificationException, RemoteException;

    void stop(Command cmd) throws RemoteException;

    String getTraces(Command cmd, boolean std) throws RemoteException;
    
    Command[] register(ControlRmi controller) throws RemoteException;

    void unRegister(ControlRmi controller) throws RemoteException;

} // endinterface ClientRmi