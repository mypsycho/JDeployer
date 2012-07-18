/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.mypsycho.swing.app.Action;
import org.mypsycho.swing.app.Application;
import org.mypsycho.swing.app.ApplicationListener;
import org.mypsycho.swing.app.FrameView;
import org.mypsycho.swing.app.beans.PagedFrame;
import org.mypsycho.swing.app.task.Task;

import com.psycho.deploy.io.XmlAccessor;
import com.psycho.deploy.io.XmlFilter;

/**
 * @author Peransin Nicolas
 */
public class ControlApplication extends CommonApplication {


    protected Controller control = null;
    protected JFileChooser fc = new JFileChooser();
    
    
    public class AppFrame extends PagedFrame {
        
        ControlPane pane;
        AppFrame(final ControlPane p) {
            super(ControlApplication.this);

            pane = p;
            
            addPage("main", pane);
            setConsoleVisible(false);
            setTabsVisible(false);
            
            pane.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("running".equals(evt.getPropertyName())) {
                        enableActions();
                    } else if ("status".equals(evt.getPropertyName())) {
                        updateStatus(evt.getNewValue());
                    }
                    
                }
            });
            
            control.addControlListener(new ControlListener() {
                public void systemChanged(ProcessWrapper pProcess, Command pCmd, int pEvent) {
                    enableActions();
                }
                
            });
            enableActions();
        }
        


        void enableActions() {
            boolean running = pane.isRunning();
            
            setEnableds("load", !running);
            setEnableds("save", !running);
            setEnableds("saveAs", !running);
            
            setEnableds("connect", !running && control.hasDisconnected());
            setEnableds("disconnect", !running && control.hasConnected());
            setEnableds("go", !running && control.hasStartable());
            setEnableds("stop", !running && control.hasStoppable());
            setEnableds("interrupt", running);
        }


        Map<String, Boolean> enableds = new HashMap<String, Boolean>();
        
        /**
         * Returns the enableds.
         *
         * @return the enableds
         */
        public boolean getEnableds(String prop) {
            Boolean b = enableds.get(prop);
            return b != null ? b : false;
        }

        
        /**
         * Sets the enableds.
         *
         * @param enableds the enableds to set
         */
        public void setEnableds(String prop, boolean enabled) {
            boolean old = getEnableds(prop);
            enableds.put(prop, enabled);
            firePropertyChange("enableds(" + prop + ")", old, enabled);
        }

        
        @Action(enabledProperty="enableds(load)")
        public void load() {
            if (fc.showOpenDialog(pane) == JFileChooser.APPROVE_OPTION) {
                setFile(fc.getSelectedFile());
                updateStatus(new Callable<String>() {
                    public String call() throws Exception {
                        return control.loadFile(currentFile);
                    }
                });

            }
        }
        
        @Action(enabledProperty="enableds(saveAs)")
        public void saveAs() {
            if (fc.showSaveDialog(pane) == JFileChooser.APPROVE_OPTION) {
                // Ensure extension : for hand-written file names
                final File choosen = XmlFilter.ensureExtension(fc.getSelectedFile());
                setFile(choosen);
                save();
            }
        }
        
        @Action(enabledProperty="enableds(save)")
        public void save() {
            if (currentFile == null) {
                saveAs();
            } else {
                updateStatus(new Callable<String>() {
                    public String call() throws Exception {
                        return control.saveFile(currentFile);
                    }
                });
            }
        }
        
        
        @Action(enabledProperty="enableds(connect)")
        public Task<Void,Void> connect() {
            return new Task<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    control.connectAll();
                    return null;
                }
            };
            
        }
        
        @Action(enabledProperty="enableds(disconnect)")
        public Task<Void,Void> disconnect() {
            return new Task<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    control.disconnectAll();
                    return null;
                }
            };            
        }


        @Action(enabledProperty="enableds(go)")
        public void go() {
            try {
                pane.startCommands(null);
            } catch (Exception e) {
                updateStatus(e); // May not be translated
            }
        }

        @Action(enabledProperty="enableds(interrupt)")
        public void interrupt() {
            try {
                pane.stopCommands();
            } catch (Exception e) {
                updateStatus(e); // May not be translated
            }
        }
        
        
        @Action(enabledProperty="enableds(stop)")
        public void stop() {
            control.stopAll();
            updateStatus(null);
        }

        @Action(selectedProperty="enableds(popupError)")
        public void popupError() {
            // marking action
        }

        public void updateStatus(Callable<String> status) {
            try {
                updateStatus(((Callable<String>) status).call()); 
            } catch (Exception e) {
                updateStatus(e);
            }
        }
        
        public void updateStatus(Object status) {
            String msg = null;
            if (status instanceof Throwable) {
                Throwable err = (Throwable) status;
                err.printStackTrace(getConsoleStream());
                msg = err.getMessage();
            } else if (status != null) {
                msg = String.valueOf(status);
            }
            
            if ((status instanceof Throwable) && getEnableds("popupError")) {
                showOption(this, "error", new JOptionPane(msg, JOptionPane.ERROR_MESSAGE));
            } else {
                setStatus(msg);
            }

        }

    }

    @Override
    protected void initialize(String[] args) {
        super.initialize(args);
        control = new Controller(texts, new XmlAccessor(texts));
        addApplicationListener(new ApplicationListener.Adapter() {
            @Override
            public void willExit(EventObject event) {
                control.disconnectAll();
            }
        });
        
    }
    
    protected void startup() {
        
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new XmlFilter(getProperty("DeployFilter.description")));

        final AppFrame f = new AppFrame(new ControlPane(control));
        

        show(new FrameView(this, f));
        // getMainView().maximize();

//        int height = 0;

        // Uniforme buttons size (Change because of icons)
//        for (int button=0; button<buttons.length; button++) {
//            if (buttons[button] == null) {
//                continue; // Separator
//            }
//            JButton b = (JButton) buttons[button][0];
//            b.setMaximumSize(new Dimension(b.getMaximumSize().width, height));
//        }

    }


    protected File currentFile = null;
    public void setFile(File pLastFile) {
        String frameTitle = getProperty("view(mainFrame).title");
        if (pLastFile != null) {
            String path = pLastFile.getAbsolutePath();
            try {
                path = pLastFile.getCanonicalPath();
            } catch (IOException ioe) {
            }
            frameTitle = frameTitle + " - " + path;

        }
        fc.setSelectedFile(pLastFile);
        getMainFrame().setTitle(frameTitle);
        currentFile = pLastFile;
    }


    public static void main(String[] args) {
        Application app = new ControlApplication();
        app.launch(args);
    }

} // endclass ControlFrame