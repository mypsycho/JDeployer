/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.mypsycho.swing.app.beans.Page;
import org.mypsycho.swing.app.beans.PagedFrame;
import org.mypsycho.swing.app.utils.SwingHelper;

/**
 * @author Peransin Nicolas
 */
public class ProcessPane extends JPanel implements TraceProvider {

    protected JTextField ip = new JTextField();
    protected JTextField port = new JTextField(5); // column ??
    protected JTextField title = new JTextField();
    protected JTextField cmd = new JTextField();
    protected JTextField path = new JTextField();


    
    final PagedFrame mainFrame;
    final Processor processor;
    final UiCommon texts;
    
    public ProcessPane(PagedFrame parent, Processor proc, UiCommon ui) {
        super(new BorderLayout());
        texts = ui;
        mainFrame = parent;
        processor = proc;
        
        SwingHelper h = new SwingHelper(this);
        h.with("editor", new BorderLayout(), BorderLayout.PAGE_START)
            .with("labels", new GridLayout(0, 1), BorderLayout.LINE_START)
                .label("id")
                .label("name")
                .label("line")
                .label("path")
                .back()
            .with("fields", new GridLayout(0, 1), BorderLayout.CENTER)
                .with("id", new BorderLayout())
                    .add("host", ip, BorderLayout.CENTER)
                    .add("port", port, BorderLayout.LINE_END)
                    .back()
                .add("name", title)
                .add("line", cmd)
                .add("path", path)
                .back()
            .back();
        h.with("buttons", new FlowLayout(), BorderLayout.PAGE_END)
            .add("start", new JButton())
            .add("clean", new JButton());


        ip.setText(Processor.HERE.getHostName());
        ip.setEditable(false);
        port.setText(Integer.toString(RmiConstants.getRmiPort()));
        port.setEditable(false);

        processor.addPropertyChangeListener(new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("new".equals(evt.getPropertyName())) {
                    addNewPane((Executor) evt.getNewValue());
                }
                
            }
        });
        
    }

    public void addNewPane(final Executor exe) {
        
        EventQueue.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                ExePane pane = new ExePane(exe, texts);
                
                String title = pane.getCommand().getName();
                for (Page page : mainFrame.getPages()) {
                    if (title.equals(page.getTitle())) {
                        mainFrame.remove(page);
                    }
                }

                mainFrame.setSelected(mainFrame.addPage(title, pane));
            }
        });
    }
    
    public String getExecutorText(final Command exe, final boolean std) {
        final String[] result = new String[] { "" };
        try {
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    for (Page page : mainFrame.getPages()) {
                        if (page.getComponent() instanceof ExePane) {
                            ExePane pane = (ExePane) page.getComponent();
                            if (exe.equals(pane.getModel())) {
                                result[0] = pane.getOuputValue(std);
                                break;
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            setStatus(e);
        }
        
        
        return result[0];
    }
    
    public void start() {
        if (title.getText().trim().isEmpty()) {
            setStatus(texts.get("Error.Command.NoName"));
        } else if (cmd.getText().trim().isEmpty()) {
            setStatus(texts.get("Error.Command.Empty"));
        } else try {
            processor.start(new Command(title.getText().trim(),
                    cmd.getText().trim(),
                    path.getText()));
        } catch (Exception e) {
            setStatus(e);
        }


    }
    
    
    public void stopAll() {
        for (Page page : mainFrame.getPages()) {
            if (page.getComponent() instanceof ExePane) {
                ExePane exe = (ExePane) page.getComponent();
                if (exe.isRunning()) {
                    exe.stop();
                }
            }
        }
    }
    
    public void clean() {
        for (Page page : mainFrame.getPages()) {
            if (page.getComponent() instanceof ExePane) {
                ExePane exe = (ExePane) page.getComponent();
                if (!exe.isRunning()) {
                    mainFrame.remove(page);
                }
            }
        }
    }



    /**
     * Sets the status.
     *
     * @param status the status to set
     */
    public void setStatus(Object status) {
        firePropertyChange("status", null, status);
    }
    
} // endclass ProcessPane