/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.rmi.RemoteException;
import java.rmi.Remote;

/**
 * @author Peransin Nicolas
 */
public interface ControlRmi extends Remote {

    public void requestProcessed(Command c) throws RemoteException;
    // (Command==null) <=> Processor closing
    //


} // endinterface CentralRmi