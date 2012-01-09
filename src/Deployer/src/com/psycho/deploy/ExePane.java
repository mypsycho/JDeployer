/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.text.Document;

import org.mypsycho.swing.TextAreaStream;
import org.mypsycho.swing.app.beans.Page;
import org.mypsycho.swing.app.beans.PagedFrame;
import org.mypsycho.swing.app.utils.SwingHelper;


/**
 * @author Peransin Nicolas
 */
public class ExePane extends JPanel {
    protected Executor model = null;



    protected static final String STOP_ACTION = "ExePane.Stop";
    protected static final String CLOSE_ACTION = "ExePane.Close";
    protected JButton activeButton = new JButton(); // Stop/Close
    protected JSplitPane streams = null;
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
    }


    protected void readStream(InputStream in, JTextArea out) {
        if (in == null) return;

        BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
        try {
            Document doc = out.getDocument();
            String line = buffer.readLine();
            while (line != null) { // End of Stream
                out.append(line);
                out.append("\n"); // In a JTextArea => not machine dependant
                out.setCaretPosition(doc.getLength());
                line = buffer.readLine();
            }
        } catch (IOException ioe) { // interne !?
            System.err.println("Error in reading stream of :"
                               + model.getCommand().getLine());
            ioe.printStackTrace();
        }
    }




    public boolean isRunning() {
        return activeButton.getActionCommand().equals(STOP_ACTION);
    }

    public void refresh() {
        Command cmd = model.getCommand();
        stateLabel.setForeground(ui.getColor(cmd));
        stateLabel.setText(ui.getState(cmd));

        activeButton.setAction(getActionMap().get(cmd.isRunning() ? "stop" : "close"));    
        
    }
    


    protected void buildDisplay(PagedFrame frame) {
        SwingHelper h = new SwingHelper(this);
        h.with("header", new GridLayout(1, 0), BorderLayout.PAGE_START)
            .add("line", new JLabel(model.getCommand().getLine(), JLabel.LEADING))
            .add("state", stateLabel)
            .back();
        
        h.add("streams", new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(std), new JScrollPane(err)), BorderLayout.CENTER);
        std.setEditable(false);
        err.setEditable(false);

        h.with("buttons", new FlowLayout(), BorderLayout.PAGE_END)
            .add("button", activeButton);

        
        
        String title = model.getCommand().getName();
        
        int titleIndex = 2;
        Page[] pages = frame.getPages();
        
        for (boolean found = true; found; titleIndex++) {
            found = false;
            for (Page page : pages) {
                if (title.equals(page.getTitle())) {
                    found = true;
                    title = model.getCommand().getName() + " (" + titleIndex + ")";
                }
            }
        }
        frame.addPage(title, this);



        streams.setDividerLocation(0.5);
        
        refresh();
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