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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.mypsycho.swing.app.beans.Page;
import org.mypsycho.swing.app.beans.PagedFrame;
import org.mypsycho.swing.app.utils.SwingHelper;

/**
 * @author Peransin Nicolas
 */
public class ProcessPane extends JPanel {

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
        h.with("editor", new BorderLayout(), BorderLayout.CENTER)
            .with("labels", new GridLayout(0, 1))
                .add("id", new JLabel("", JLabel.TRAILING))
                .add("name", new JLabel("", JLabel.TRAILING))
                .add("line", new JLabel("", JLabel.TRAILING))
                .add("path", new JLabel("", JLabel.TRAILING))
                .back()
            .with("fields", new GridLayout(0, 1))
                .with("id", new BorderLayout())
                    .add("host", ip, BorderLayout.CENTER)
                    .add("port", port, BorderLayout.LINE_END)
                    .back()
                .add("name", title)
                .add("line", cmd)
                .add("path", path)
                .back()
            .back();
        h.with("buttons", new FlowLayout())
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

    public void addNewPane(Executor exe) {
        final ExePane exePane = new ExePane(exe, texts);
        
        EventQueue.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                exePane.buildDisplay(mainFrame);
            }
        });
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
                if (!exe.isRunning()) {
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
                    exe.close();
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
    



//    public static void main(String[] args) {
//        try { // For testing
//            Language.getLanguage();
//            ProcessPane test = new ProcessPane();
//            WorkshopFactory.getWorkshop().addWindow(test.TITLE, test);
//
//            // Setting target
//            if (args.length > 0)
//                test.ip.setText(InetAddress.getByName(args[0]).getHostAddress());
//            els
//                test.ip.setText(InetAddress.getLocalHost().getHostName());
//
//            // Setting Command
//            if (args.length > 1) {
//                String cmd = args[1];
//                for (int arg=2; arg<args.length; arg++)
//                    cmd = cmd + " " + args[arg];
//                test.cmd.setText(cmd); // long file
//                test.title.setText("Basic test");
//            } else  {
//                test.cmd.setText("cmd /C type c:\\winnt\\setupapi.log"); // long file
//                test.title.setText("Print long file");
//            }
//
//            WorkshopFactory.getWorkshop().setMessagesVisible(false);
//            WorkshopFactory.getWorkshop().setTitle("Processor tester");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

} // endclass ProcessPane