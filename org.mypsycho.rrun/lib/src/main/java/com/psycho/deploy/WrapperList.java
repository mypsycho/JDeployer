/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;


/**
 * @author Peransin Nicolas
 */
public class WrapperList extends DefaultComboBoxModel implements ControlListener {

    
    protected Controller core = null;

    public WrapperList(Controller pCore) {
        pCore.addControlListener(this);
//        insertElementAt(null, 0);
    }


    public Object getSelectedItem() {
        throw new IllegalAccessError("Use getDisplay().getSelectedValue()");
    }



    public void systemChanged(final ProcessWrapper pProcess, final Command pCmd, final int pEvent) {

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        systemChanged(pProcess, pCmd, pEvent);
                    }
                });
            return;
        }
        
        int index = -1;
        switch (pEvent) {
            case ControlListener.PROCESSOR_ADDED: // Ajouter au display + select
//                insertElementAt(pProcess, getSize()-1);
                addElement(pProcess);
//                getDisplay().setSelectedValue(pProcess, true);
                break;

            case ControlListener.PROCESSOR_UPDATED: // Ajouter au display + select
                index = getIndexOf(pProcess);
                fireContentsChanged(this, index, index);
                break;

            case ControlListener.PROCESSOR_REMOVED: // Ajouter au display + select
                removeElement(pProcess);
                break;

            case ControlListener.PROCESSOR_CONNECTED:
            case ControlListener.PROCESSOR_DISCONNECTED:
                index = getIndexOf(pProcess);
                fireContentsChanged(this, index, index);
                break;
                
            default: // ignored
        }
    }


} // endclass ClientList