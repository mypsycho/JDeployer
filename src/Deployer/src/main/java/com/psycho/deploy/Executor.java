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
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * @author Peransin Nicolas
 */
public class Executor {
    
    
    protected Command cmd = null;
    protected Processor parent = null;
    protected Process runner = null;
    
    private static final int ISTD = 0;
    private static final int IERR = 1;
    protected OutputStream[] streams = { null, null};


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
            readStream("std", runner.getInputStream(), ISTD);
            readStream("err", runner.getErrorStream(), IERR);
            
            cmd.setState(Command.State.running);
            parent.firePropertyChange(this, "state");
            waitForEnd();

        } catch (IOException ioe) { // Error while creating process
            cmd.setState(Command.State.error);
            parent.firePropertyChange(this, "state");
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
    
    protected void readStream(String suffix, final InputStream in, final int iOut) {

        new Thread(cmd.getName() + suffix) {
            
            Closeable out;
            
            void openOutput() {
                synchronized (streams) {
                    while (streams[iOut] == null) {
                        try {
                            streams.wait();
                        } catch (InterruptedException ie) {
                            return;
                        }
                    }
                }

                if (streams[iOut] instanceof PrintStream) {
                    out = streams[iOut];
                } else {
                    out = new PrintWriter(new OutputStreamWriter(streams[iOut]));
                }
            }
            
            @Override
            public void run() {

                openOutput();
                
                BufferedReader buffer = null;
                try {
                    buffer = new BufferedReader(new InputStreamReader(in));
                    for (String line = buffer.readLine(); line != null; line = buffer.readLine()) {
                        // End of Stream
                        print(line);
                    }
                } catch (IOException ioe) { // interne !?
                    print(ioe);
                } finally {
                    closeQuitely(out);
                    closeQuitely(buffer); // if (!running) ??
                }

            }

            void print(String line) {
                if (out instanceof PrintStream) {
                    ((PrintStream) out).println(line);
                } else {
                    ((PrintWriter) out).println(line);
                }
            }
            
            void print(Throwable t) {
                if (out instanceof PrintStream) {
                    ((PrintStream) out).println(t);
                } else {
                    ((PrintWriter) out).println(t);
                }
            }
            
            void closeQuitely(Closeable c) {
                try {
                    c.close();
                } catch (Exception ignore) {
                }
            }
        }.start();

    }
    

    
//    
//    static class NulWriter extends Writer {
//
//
//        @Override
//        public void write(char[] cbuf, int off, int len) throws IOException {
//        }
//
//        @Override
//        public void flush() throws IOException { 
//        }
//
//
//        @Override
//        public void close() throws IOException {
//        }
//
//    }
    

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return cmd.hashCode();
    }
    
    protected void waitForEnd() {
        if (runner == null) {
            return;
        }
    
        new Thread(cmd.getName() + "-watch") {
    
            public void run() {
                if (runner != null) {
                    parent.started(cmd);
                    try {
                        // Thread synchronization trouble
                        int result = runner.waitFor();
                        if (cmd.getState() == Command.State.running) {
                            cmd.setState(Command.State.finished, result);
                            parent.firePropertyChange(Executor.this, "state");
                        }

                    } catch (InterruptedException ie) {
                        endProcess();
                    }
                }
                parent.finished(cmd);
            }
        }.start();
    }

//    public int getExecutionStatus() { return executionStatus; }
//    public int getResultCode() { return result; }


    public Command getCommand() { 
        return cmd; 
    }


    public void endProcess() {
        if (runner != null) {
            cmd.setState(Command.State.interrupted);
            runner.destroy();
            parent.firePropertyChange(Executor.this, "state");
        }
    }

    public synchronized void setStream(int index, OutputStream out) {
        synchronized (streams) {
            if ((runner != null) && (streams[index] != null)) {
                throw new IllegalStateException();
            }

            streams[index] = out;
            streams.notifyAll();
        }

    }

    
    
    /**
     * Returns the errorStream.
     *
     * @return the errorStream
     */
    public OutputStream getErrorStream() {
        return streams[IERR];
    }

    
    /**
     * Sets the errorStream.
     *
     * @param errorStream the errorStream to set
     */
    public void setErrorStream(OutputStream out) {
        setStream(IERR, out);
    }

    
    /**
     * Returns the inputStream.
     *
     * @return the inputStream
     */
    public OutputStream getInputStream() {
        return streams[ISTD];
    }

    
    /**
     * Sets the inputStream.
     *
     * @param inputStream the inputStream to set
     */
    public synchronized void setInputStream(OutputStream out) {
        setStream(ISTD, out);
    }

    
    
} // endclass Executor