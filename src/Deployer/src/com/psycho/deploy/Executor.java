/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * @author Peransin Nicolas
 */
public class Executor {
    
    protected Command cmd = null;
    protected Processor parent = null;
    protected Process runner = null;
    
    protected OutputStream errorStream = null;
    protected OutputStream inputStream = null;

    //    protected int executionStatus = Command.RUNNING_STATE;
//    protected int result = 0;

    public Executor(Processor p, Command c) {
        parent = p;
        cmd = c;

    }

    public synchronized void start() {
        if (runner != null) {
            throw new IllegalStateException();
        }
        
        
        try {

            File dir = null;
            String dirName = (cmd.getDirectory() != null) ? cmd.getDirectory().trim() : null;
            if ((dirName != null) && !dirName.isEmpty()) {
                dir = new File(dirName);
            }
            
            runner = Runtime.getRuntime().exec(cmd.getLine(), null, dir);
            cmd.setState(Command.State.running);

            parent.firePropertyChange(this, "state");
            
            readStream("std", runner.getInputStream(), inputStream);
            readStream("err", runner.getErrorStream(), errorStream);
                    

        } catch (IOException ioe) { // Error while creating process
            cmd.setState(Command.State.error);
            System.err.println(parent.getTexts().get("Error.Command.New", 
                    cmd.getName(), ioe.getMessage()));
        }


        
        
    }
    
    public boolean equals(Object o) {
        if (o instanceof Executor) {
            return cmd.equals(((Executor) o).cmd);
        } else if (o instanceof Command) {
            return cmd.equals(o);
        } else {
            return super.equals(o);
        }
    }

    
    /**
     * Returns the parent.
     *
     * @return the parent
     */
    public Processor getParent() {
        return parent;
    }
    
    protected void readStream(String suffix, final InputStream in, final OutputStream out) {

        new Thread(new Runnable() {
            
            @Override
            public void run() {        
                BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
                PrintWriter res = new PrintWriter(out != null ? 
                        new OutputStreamWriter(out) : new NulWriter());

                try {
                    for (String line = buffer.readLine(); line != null; line = buffer.readLine()) {
                        // End of Stream
                        res.println(line);
                    }
                } catch (IOException ioe) { // interne !?
                    res.println(ioe);
                } finally {
                    closeQuitely(res);
                    closeQuitely(buffer); // if (!running) ??
                }
                
            }
            
            void closeQuitely(Closeable c) {
                try {
                    c.close();
                } catch (Exception ignore) {
                }
            }
        }, cmd.getName() + suffix).start();

    }
    

    
    
    static class NulWriter extends Writer {


        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
        }

        @Override
        public void flush() throws IOException { 
        }


        @Override
        public void close() throws IOException {
        }

    }
    

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return cmd.hashCode();
    }
    
    public void run() {
        try {

            File dir = null;
            String dirName = (cmd.getDirectory() != null) ? cmd.getDirectory().trim() : null;
            if ((dirName != null) && !dirName.isEmpty()) {
                dir = new File(dirName);
            }
            
            runner = Runtime.getRuntime().exec(cmd.getLine(), null, dir);
            cmd.setState(Command.State.running);


        } catch (IOException ioe) { // Error while creating process
            cmd.setState(Command.State.error);
            System.err.println(parent.getTexts().get("ErrNewProc", cmd.getName()));
            ioe.printStackTrace();
        }

        parent.firePropertyChange(this, "state");


        if (runner != null) {
            parent.started(cmd);
            try {
                // Thread synchronization trouble
                cmd.setState(Command.State.finished,
                             runner.waitFor());

                parent.firePropertyChange(this, "finished");

            } catch (InterruptedException ie) {
                cmd.setState(Command.State.interrupted);
                parent.firePropertyChange(this, "interrupted");
                runner.destroy();
            }
        }



        parent.finished(cmd);
    }

//    public int getExecutionStatus() { return executionStatus; }
//    public int getResultCode() { return result; }


    public Command getCommand() { 
        return cmd; 
    }


    
    

    public void endProcess() {
        if (runner != null) {
            runner.destroy();
        }
    }

    
    /**
     * Returns the errorStream.
     *
     * @return the errorStream
     */
    public OutputStream getErrorStream() {
        return errorStream;
    }

    
    /**
     * Sets the errorStream.
     *
     * @param errorStream the errorStream to set
     */
    public synchronized void setErrorStream(OutputStream out) {
        if (runner != null) {
            throw new IllegalStateException();
        }

        this.errorStream = out;
    }

    
    /**
     * Returns the inputStream.
     *
     * @return the inputStream
     */
    public OutputStream getInputStream() {
        return inputStream;
    }

    
    /**
     * Sets the inputStream.
     *
     * @param inputStream the inputStream to set
     */
    public synchronized void setInputStream(OutputStream out) {
        if (runner != null) {
            throw new IllegalStateException();
        }
        this.inputStream = out;
    }

    
    
} // endclass Executor