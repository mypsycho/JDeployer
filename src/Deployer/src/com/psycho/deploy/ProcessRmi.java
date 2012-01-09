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

    public void start(Command cmd) throws ConcurrentModificationException, RemoteException;

    public void stop(Command cmd) throws RemoteException;

    public Command[] register(ControlRmi controller) throws RemoteException;

    public void unRegister(ControlRmi controller) throws RemoteException;

} // endinterface ClientRmi