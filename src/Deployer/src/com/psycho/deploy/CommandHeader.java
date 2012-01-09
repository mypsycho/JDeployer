/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

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
public class CommandHeader extends JPanel implements ControlListener {

    protected ControlPane parent = null;
    protected ProcessWrapper process = null;
    protected Command model = null;
    protected PTextField name = new PTextField();
    protected PTextField line = new PTextField();
    protected PTextField path = new PTextField();

    

    protected JButton addRemoveB = new JButton(); // + Remove
    protected JLabel state = new JLabel();
    protected JButton startStopB = new JButton(); // + Stop
    

    public CommandHeader(ControlPane pParent) {
        super(new BorderLayout());
        parent = pParent;

         // Load Resources
//         for (int icon=0; icon<icons.length; icon++)
//             icons[icon] = new ImageIcon(WrapperHeader.class.getResource(ICON_NAMES[icon]));

         // Build graphic
        SwingHelper h = new SwingHelper(this);
        h.with("titles", new GridLayout(0, 1), BorderLayout.LINE_START)
            .add("name", new JLabel())
            .add("line", new JLabel())
            .add("dir", new JLabel())
            .back();
        h.with("editors", new GridLayout(0, 1), BorderLayout.CENTER)
            .add("name", name)
            .add("line", line)
            .add("dir", path)
            .back();
        h.with("actions", new GridLayout(0, 1), BorderLayout.LINE_END)
            .add("register", addRemoveB)
            .add("state", new JLabel("", JLabel.CENTER))
            .add("switch", startStopB)
            .back();


         // - Buttons
//         JComponent[] comps = { addRemoveB, startStopB };
//         for (int button=0; button<comps.length; button++) {
//             if (comps[button].getBorder() instanceof CompoundBorder) {
//                 CompoundBorder border = (CompoundBorder) comps[button].getBorder();
//                 border = BorderFactory.createCompoundBorder(
//                         border.getOutsideBorder(),
//                         BorderFactory.createEmptyBorder(0, SPACE, 0, SPACE));
//                 comps[button].setBorder(border);
//             }
//         }


         // - Layout
//         setBorder(BorderFactory.createEmptyBorder(SPACE, SPACE, 0, SPACE));
         // JPanel titles = new JPanel(new GridLayout(0, 1, SPACE, SPACE)); // 1 column

         // Preferred size must not be dynamique : Max possible size is required !!!
         int prefStateWidth = 0;
         for (Command.State stateValue : Command.State.values()) {
             String stateText = parent.getTexts().get(stateValue, 9999);
             
             state.setText(stateText); // 4 digit => max ?
             prefStateWidth = Math.max(prefStateWidth, state.getPreferredSize().width);
         }
         state.setPreferredSize(new Dimension(prefStateWidth,
                 state.getPreferredSize().height));


         name.addKeyListener(undoListener);
         line.addKeyListener(undoListener);
         path.addKeyListener(undoListener);
         
         // Init
         update();
    }
    
    public void setCommand(ProcessWrapper pProcess, Command pCmd) {
        if ((pCmd == model) && (pProcess == process)) {
            return;
        }

        process = pProcess;
        model = pCmd;

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
    protected boolean editable = true;
    public void setEditable(boolean pEditable) {
        if (pEditable == editable)  {
            return;
        }

        editable = pEditable;
        update();
    }
    
    protected void update() {

        startStopB.setAction(getActionMap().get(!isRunning() ? "start" : "stop"));
        addRemoveB.setAction(getActionMap().get(model == null ? "register" : "unregister"));
        if (model != null) {
            name.setText(model.getName());
            line.setText(model.getLine());
            path.setText(model.getDirectory()); // Null supported
        }
        // Let hope previous value is not used ...
        firePropertyChange("editable", !isEditable(), isEditable());        
        firePropertyChange("modifiable", !isModifiable(), isModifiable());        
        firePropertyChange("switchable", !isSwitchable(), isSwitchable());        
    }

    boolean isRunning() {
        return (model != null) && model.isRunning();
    }
    
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
                } else if (text == line) {
                    text.setText(model.getLine());
                } else if (text == path) {
                    text.setText(model.getDirectory());
                }
            }
        }
    };
    


    
    public boolean isModifiable() {
        return isEditable() && !isRunning();
    }
    
    public boolean isSwitchable() {
        return process.isConnected() && isEditable() && (model != null);
    }
    
    @Action(enabledProperty="editable")
    public void register() {
        try {
            process.add(name.getText(), line.getText(), path.getText());
        } catch (Exception e) {
            parent.setStatus(e);
        }
    }


    @Action(enabledProperty="modifiable")
    public void edit() {
        if (model != null) {
            try {
                process.rename(model, name.getText());
                model.setLine(line.getText());
                model.setDirectory(path.getText());

            } catch (Exception e) {
                parent.setStatus(e);
            }
            update();
        }
    }
    
    @Action(enabledProperty="modifiable")
    public void unregister() {
        process.remove(model);
    }
    
    
    @Action(enabledProperty="switchable")
    public void start() {
        try {
            process.start(model);
            parent.setStatus(null);
        } catch (Exception e) {
            parent.setStatus(e);
        }
    }
    
    @Action(enabledProperty="switchable")
    public void stop() {
        try {
            process.stop(model);
            parent.setStatus(null);
        } catch (Exception e) {
            parent.setStatus(e);
        }
    }


    public void systemChanged(ProcessWrapper pProcess, Command pCmd, int pEvent) {
        if ((model != null) && (pCmd == model)) {
            update();
        }
    }

    
} // endclass CommandPane