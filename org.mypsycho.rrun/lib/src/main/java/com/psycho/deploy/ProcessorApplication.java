/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;
import java.util.concurrent.Callable;

import javax.swing.JOptionPane;

import org.mypsycho.swing.app.Application;
import org.mypsycho.swing.app.ApplicationListener;
import org.mypsycho.swing.app.FrameView;
import org.mypsycho.swing.app.beans.PagedFrame;


/**
 * @author Peransin Nicolas
 */
public class ProcessorApplication extends CommonApplication {

    private Processor engine = null;

    public class AppFrame extends PagedFrame {

        ProcessPane pane;

        public AppFrame() {
            super(ProcessorApplication.this);
            
            pane = new ProcessPane(this, engine, texts);
            pane.addPropertyChangeListener("status", new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    Object status = evt.getNewValue();
                    String text = String.valueOf(status);
                    if (status instanceof Exception) {
                        text = ((Exception) status).getMessage();
                        text = texts.get(text);
                    }
                    updateStatus(text);
                }
            });
            addPage(" ", pane);
            engine.setTraceProvider(pane);
            
        }


        public void updateStatus(Object status) {
            String msg = null;
            if (status instanceof Callable) {
                try {
                    updateStatus(((Callable<String>) status).call()); 
                } catch (Exception e) {
                    updateStatus(e);
                }
                
            }
            if (status instanceof Throwable) {
                Throwable err = (Throwable) status;
                err.printStackTrace(getConsoleStream());
                msg = err.getMessage();
            } else if (status != null) {
                msg = String.valueOf(status);
            }
            
            setStatus(msg);
        }

        
        public void stopAll() {
            pane.stopAll();
        }

        public void clean() {
            pane.clean();
        }
    }


    /* (non-Javadoc)
     * @see com.psycho.swing.app.Application#initialize(java.lang.String[])
     */
    @Override
    protected void initialize(String[] args) {
        super.initialize(args);

        if (args.length > 0) {
            System.setProperty(RmiConstants.PORT_PROPERTY, args[0]);
        }
        getContext().getResourceManager().inject(texts);
        engine = new Processor(texts);
        addApplicationListener(closeListener);
    }

    /* (non-Javadoc)
     * @see com.psycho.swing.app.Application#startup()
     */
    @Override
    protected void startup() {
        FrameView v = new FrameView(this, new AppFrame());
        show(v);
        // v.iconify();
    }

    ApplicationListener closeListener = new ApplicationListener.Adapter() {
        public boolean canExit(EventObject event) {
            if (engine.getExecutors().length == 0) {
                return true;
            }
            
            return Integer.valueOf(JOptionPane.YES_OPTION).equals(showOption(event, "exit"));
        }

        public void willExit(java.util.EventObject event) {
            engine.disconnect();
        };
    };
    
    public static void main(String[] args) {
        Application app = new ProcessorApplication();
        app.launch(args);
    }

}
