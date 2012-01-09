/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;

import javax.swing.JOptionPane;

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
                    setStatus(text);
                }
            });
            addPage(" ", pane);
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
        v.iconify();
    }

    ApplicationListener closeListener = new ApplicationListener.Adapter() {
        public boolean canExit(EventObject event) {
            return Integer.valueOf(JOptionPane.YES_OPTION).equals(showOption(event, "exit"));
        }

        public void willExit(java.util.EventObject event) {
            engine.disconnect();
        };
    };

}
