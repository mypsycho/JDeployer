/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

/**
 * @author Peransin Nicolas
 */
public interface ControlListener {

    public static final int PROCESSOR_ADDED=0;
    public static final int PROCESSOR_UPDATED=1;
    public static final int PROCESSOR_CONNECTED=2;
    public static final int PROCESSOR_DISCONNECTED=3;
    public static final int PROCESSOR_REMOVED=4;

    public static final int COMMAND_ADDED=20;
    public static final int COMMAND_UPDATED=21; // Started or stopped
    public static final int COMMAND_REMOVED=23;

    public void systemChanged(ProcessWrapper pProcess, Command pCmd, int pEvent);

} // endinterface CentralListener