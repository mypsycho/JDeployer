/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


/**
 * @author Peransin Nicolas
 */
public abstract class RmiConstants {
    /* Do not detect already executing */

    public static final String PORT_PROPERTY = "com.psycho.deploy.port";
    public static final int DEFAULT_PORT = 2099;
    private static int RMI_PORT = DEFAULT_PORT;

    private static boolean PROPERTIES_READ = false;

    protected static void checkRmiContext() {
        if (PROPERTIES_READ) {
            return;
        }

        // Reading properties : Port
        boolean wrongProperty = false;
        try {
            int portSpecified = Integer.parseInt(System.getProperty(PORT_PROPERTY,
                    Integer.toString(RMI_PORT)));
            if (portSpecified > 0) {
                RmiConstants.RMI_PORT = portSpecified;
            } else {
                wrongProperty = true;
            }
        } catch (NumberFormatException nfe) {
            wrongProperty = true;
        }
        if (wrongProperty) {
            System.err.println("RmiConstants.InvalidPort" + 
                    new Object[] { PORT_PROPERTY, System.getProperty(PORT_PROPERTY),
                    Integer.toString(DEFAULT_PORT) });
        }
        // Stub identification
//        String rmiStubPropery = "java.rmi.server.codebase";
//        Class thisClass = RmiConstants.class;
//        String thisClassLocation = thisClass.getResource("/"
//               + thisClass.getName().replaceAll("[.]", "/")+".class").toExternalForm();
//
//        // Keep the ending slash !!!
//        String codebaseRMI = thisClassLocation.substring(0,
//                thisClassLocation.length() - thisClass.getName().length()-6);
//
//        if (System.getProperty(rmiStubPropery) != null) {
//            System.setProperty(rmiStubPropery, System.getProperty(rmiStubPropery) + " " + codebaseRMI);
//        } else
//            System.setProperty(rmiStubPropery, codebaseRMI);

        PROPERTIES_READ = true;
    }

    public static int getRmiPort() {
        checkRmiContext();
        return RMI_PORT;
    }

    protected static Registry RMI_REGISTRY = null;
    public static synchronized Registry getRmiRegistry() {
        checkRmiContext();
        if (RMI_REGISTRY == null) {
            try { // Identify RmiRegistry
                RMI_REGISTRY = LocateRegistry.createRegistry(RMI_PORT);
            } catch (RemoteException re) { // Launch RmiRegistry
                throw new RuntimeException("PortBusy");
//                            new Object[] { String.valueOf(RMI_PORT), String.valueOf(DEFAULT_PORT),
//                            PORT_PROPERTY }));
            }
        }
        return RMI_REGISTRY;
    }

    static public Object lookup(String ip, int port, String id) throws NotBoundException,
            java.net.MalformedURLException, RemoteException {
        if (port > 0) {
            return Naming.lookup("rmi://" + ip + ":" + port + "/" + id);
        } else {
            return Naming.lookup("rmi://" + ip + "/" + id);
        }
    }



} // endclass RmiConstants