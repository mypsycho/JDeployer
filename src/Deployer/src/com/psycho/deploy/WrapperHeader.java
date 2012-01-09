/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.rmi.ConnectException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.mypsycho.swing.PTextField;
import org.mypsycho.swing.app.Action;
import org.mypsycho.swing.app.utils.SwingHelper;


/**
 * @author Peransin Nicolas
 */
public class WrapperHeader extends JPanel implements ControlListener {


    protected ControlPane parent = null;
    protected ProcessWrapper model = null;
    protected PTextField name = new PTextField();
    protected PTextField address = new PTextField();
    protected PTextField port = new PTextField(); // On fly controlled field ??


    protected JButton connectB = new JButton(); // + Disconnect
    protected JButton addB = new JButton(); // + Remove
    protected JButton startMoreB = new JButton();
    protected JButton stopMoreB = new JButton();

    public WrapperHeader(ControlPane pParent) {
        super(new BorderLayout());
        parent = pParent;

        SwingHelper h = new SwingHelper(this);
        h.with("titles", new GridLayout(0, 1), BorderLayout.PAGE_START)
            .add("name", new JLabel())
            .add("ip", new JLabel())
            .add("port", new JLabel())
            .back();
        h.with("editors", new GridLayout(0, 1), BorderLayout.CENTER)
            .add("name", name).add("ip", address)
            .with("port", new BorderLayout())
                .add("text", port, BorderLayout.CENTER)
                .add("hint", new JLabel(), BorderLayout.PAGE_END)
                .back()
            .back();
        h.with("actions", new GridLayout(0, 1), BorderLayout.PAGE_END) // on column
            .add("register", addB)
            .add("connect", connectB)
            .with("all", new GridLayout(1, 0)) // on row
                .add("start", new JButton())
                .add("stop", new JButton());
                

        // Bind events
        parent.getController().addControlListener(this);


        KeyAdapter undoListener = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ESCAPE) {// Only on escape
                    return;
                }
                JTextField text = (JTextField) e.getSource();
                
                if (model == null) {
                    text.setText("");
                } else {

                    if (text == name) {
                        text.setText(model.getName());
                    } else if (text == address) {
                        text.setText(model.getAddress());
                    } else if (text == port) {
                        text.setText("" + model.getPort());
                    }
                }
            }
        };
        
        name.addKeyListener(undoListener);
        address.addKeyListener(undoListener);
        port.addKeyListener(undoListener);


        // Init
        update();
    }

    
    
    public void setProcess(ProcessWrapper pModel) {
        if (model == pModel) {
            return;
        }
        model = pModel;
        update();
    }

    protected boolean editable = true;
    
    public void setEditable(boolean pEditable) {
        if (pEditable == editable) {
            return;
        }
        editable = pEditable;
        update();
    }

    
    /**
     * Returns the editable.
     *
     * @return the editable
     */
    public boolean isEditable() {
        return editable;
    }
    
    protected void update() {
        addB.setAction(getActionMap().get(model == null ? "register" : "unregister"));
        connectB.setAction(getActionMap().get(isConnected() ? "connect" : "disconnect"));
        addB.setEnabled(editable);
        name.setEditable(editable);
        
        firePropertyChange("editable", !isEditable(), isEditable());
        firePropertyChange("modifiable", !isModifiable(), isModifiable());
        firePropertyChange("startable", !isStartable(), isStartable());
        firePropertyChange("stoppable", !isStoppable(), isStoppable());
        firePropertyChange("connectable", !isConnectable(), isConnectable());

    }

    public boolean isStartable() {
        return (model != null) && model.isConnected() && editable && model.hasStopped();
    }

    public boolean isStoppable() {
        return (model != null) && model.isConnected() && editable && model.hasRunning();
    }

    public boolean isConnectable() {
        return (model != null) && editable;
    }
    
    /**
     * Do something TODO.
     * <p>Details of the function.</p>
     *
     * @return
     */
    public boolean isConnected() {
        return (model != null) && model.isConnected();
    }


    public boolean isModifiable() {
        return editable && !isConnected();
    }
    
    @Action(enabledProperty="connectable")
    public void connect() {
        try {
            model.connect();
            parent.setStatus("");
        } catch (ConnectException e) {
            parent.setStatus(e);
        }
    }
    
    @Action(enabledProperty="connectable")
    public void disconnect() {
        model.disconnect();
        parent.setStatus("");
    }
    @Action(enabledProperty="startable")
    public void startAll() {
        parent.startCommands(model);
    }
    
    @Action(enabledProperty="stoppable")
    public void stopAll() {
        try {
            model.stopAll();
            parent.setStatus("");
        } catch (Exception e) {
            parent.setStatus(e);
        }
    }
    

    
    @Action(enabledProperty="editable")
    public void register() {
        try {
            ProcessWrapper w = parent.getController().add(name.getText(), 
                    address.getText(), readPort());
            
            // !!! XXX PATCH !!!
            name.setText("");
            address.setText("");
            port.setText("");
                                       
            w.connect();
            
        } catch (Exception e) {

            parent.setStatus(e);
        }
        
    }
    
    @Action(enabledProperty="editable")
    public void unregister() {
        if (model.isConnected()) {
            model.disconnect();
        }
        parent.getController().remove(model);        
    }

    
    @Action(enabledProperty="modifiable")
    public void edit() {
        try {
            if (model != null) {
                parent.getController().rename(model, name.getText());
                model.setAddress(address.getText());
                model.setPort(readPort());
            }
        } catch (Exception e) {
            parent.setStatus(e);
        }
    }
    
    public int readPort() throws IllegalArgumentException {
        String portInput = port.getText().trim();
        try {
            return (portInput.length() > 0) ? Integer.parseInt(portInput) 
                    : RmiConstants.DEFAULT_PORT;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(
                    parent.getTexts().get("Error.InvalidPort", portInput));
        }
    }



    public void systemChanged(ProcessWrapper pProcess, Command pCmd, int pEvent) {
        if ((model != null) && model.equals(pProcess)) {
            update();
        }
    }

} // endclass WrapperPane