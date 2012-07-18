/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.mypsycho.beans.Injectable;
import org.mypsycho.beans.InjectionContext;
import org.mypsycho.swing.TextAreaStream;
import org.mypsycho.swing.app.utils.SwingHelper;


/**
 * @author Peransin Nicolas
 */
public class ExePane extends JPanel implements Injectable {
    protected Executor model = null;



    protected static final String STOP_ACTION = "ExePane.Stop";
    protected static final String CLOSE_ACTION = "ExePane.Close";
    protected JButton activeButton = new JButton(); // Stop/Close
    // protected JSplitPane streams = null;
    protected JLabel stateLabel = new JLabel();


    protected JTextArea std = new JTextArea();

    protected JTextArea err = new JTextArea();

    UiCommon ui = new UiCommon(this);

    public ExePane(Executor mdl, UiCommon texts) {
        super(new BorderLayout());
        ui = texts;
        model = mdl;
        
        mdl.getParent().addPropertyChangeListener(new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == model) {
                    if (EventQueue.isDispatchThread()) {
                        refresh();
                    } else {
                        EventQueue.invokeLater(new Runnable() {

                            public void run() {
                                refresh();
                            }
                        });
                    }
                }
                
            }
        });
        
        mdl.setInputStream(new TextAreaStream(std));
        mdl.setErrorStream(new TextAreaStream(err));
        
        SwingHelper h = new SwingHelper(this);
        h.with("header", new BorderLayout(), BorderLayout.PAGE_START)
            .add("line", new JLabel(model.getCommand().getLine()), BorderLayout.CENTER)
            .add("state", stateLabel, BorderLayout.LINE_END)
            .back();
        
//        h.add("streams", new JSplitPane(JSplitPane.VERTICAL_SPLIT,
//                new JScrollPane(std), new JScrollPane(err)), BorderLayout.CENTER);

        h.with("streams", new JTabbedPane(), BorderLayout.CENTER)
            .add("std", new JScrollPane(std)).add("err", new JScrollPane(err)).back();

        
        // streams = 
        std.setEditable(false);
        err.setEditable(false);

        h.with("buttons", new FlowLayout(), BorderLayout.PAGE_END)
            .add("button", activeButton);
        
    }


    /**
     * Do something TODO.
     * <p>Details of the function.</p>
     *
     * @param std2
     */
    public String getOuputValue(boolean stdOutput) {
        return (stdOutput ? std : err).getText();
    }
    
    public Executor getModel() {
        return model;
    }
    
    public boolean isRunning() {
        return getCommand().isRunning();
    }

    public void initResources(InjectionContext context) {
        refresh();
        // ((JSplitPane) new SwingHelper(this).get("streams")).setDividerLocation(0.5);
    }
    
    void refresh() {
        Command cmd = model.getCommand();
        stateLabel.setForeground(ui.getColor(cmd));
        stateLabel.setText(ui.getState(cmd));

        activeButton.setAction(getActionMap().get(cmd.isRunning() ? "stop" : "close"));
    }
    
    public Command getCommand() {
        return model.getCommand();
    }


    public void stop() {
        if (isRunning()) {
            model.endProcess();
        }
    }

    public void close() {
        if (isRunning()) {
            return;
        }
        Container parent = getParent();
        if (parent != null) {
            parent.remove(this);
        }
    }


} // endclass ExecutorPane