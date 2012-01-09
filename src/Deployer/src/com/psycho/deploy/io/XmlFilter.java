package com.psycho.deploy.io;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * <p>Title : </p>
 * <p>Description : </p>
 * <p>Copyright : Copyright (c) 2004</p>
 * <p>Company : </p>
 * @author psycho Nicolas
 * @version 1.0
 */

public class XmlFilter extends FileFilter {

    // It is well-know that EXTENSION is in lower case
    public static final String EXTENSION = ".xml";
    
    public static final String    DTD_FILENAME = "Deployer.dtd";

    public static final String      MAIN_TOKEN = "deployer";
    public static final String PROCESSOR_TOKEN = "processor";
    public static final String   COMMAND_TOKEN = "command";

    public static final String      NAME_TOKEN = "name";
    public static final String        IP_TOKEN = "ip";
    public static final String      PORT_TOKEN = "port";
    public static final String      LINE_TOKEN = "line";
    public static final String      PATH_TOKEN = "path";
    public static final String     ORDER_TOKEN = "order";
    public static final String     DELAY_TOKEN = "delay";

    
    protected final String description;
    public XmlFilter(String text) {
        description = text;
    }

    public boolean accept(File f) {
        return f.isDirectory() 
                || f.getName().toLowerCase().endsWith(XmlFilter.EXTENSION);
    }

    public String getDescription() {
        return description;
    }

    public static File ensureExtension(File f) {
        if (f.getName().toLowerCase().endsWith(XmlFilter.EXTENSION))
            return f;
        else
            return new File(f.getPath() + EXTENSION);
    }

} // endclass XmlExchange