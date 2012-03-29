package com.psycho.deploy.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.mypsycho.text.TextMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.psycho.deploy.Command;
import com.psycho.deploy.Controller;
import com.psycho.deploy.MainTable;
import com.psycho.deploy.ProcessWrapper;


/**
 * <p>Title : </p>
 * <p>Description : </p>
 * <p>Copyright : Copyright (c) 2004</p>
 * <p>Company : </p>
 * @author psycho Nicolas
 * @version 1.0
 */

public class XmlAccessor {
 
    
    TextMap texts = new TextMap();
            
    public XmlAccessor(TextMap ui) {
        texts = ui;
    }

    protected Transformer transformer = null;
    
    protected DocumentBuilder docBuilder = null;
    
    protected void ensureWriting() {
        if ((transformer != null) && (docBuilder != null))
            return;

        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty("doctype-system", XmlFilter.DTD_FILENAME);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            docBuilder = dbFactory.newDocumentBuilder();

        } catch (ParserConfigurationException pce) {
            throw new RuntimeException(pce);
        } catch (FactoryConfigurationError fce) {
            throw new RuntimeException(fce);
//        } catch (IllegalArgumentException iae) {
//            throw new IOException(iae.getMessage());
        } catch (TransformerFactoryConfigurationError tfce) {
            throw new RuntimeException(tfce);
        } catch (TransformerConfigurationException tce) {
            throw new RuntimeException(tce);
        }
    }

    public String save(Controller pCore, File pFile) throws IOException {
        ensureWriting();
        try {
            Document doc = docBuilder.newDocument();
            doc.appendChild(createDocument(pCore, doc));

            transformer.transform(new DOMSource(doc), new StreamResult(pFile));
            // To put in status bar
            return texts.get("io.Saved", pFile.getPath());

        } catch (TransformerException te) {
            throw new IOException(texts.get("io.ErrSaved", pFile.getPath()), te);
        }
    }

    public static Node createDocument(Controller pCore, Document pDoc) {
        Element result = pDoc.createElement(XmlFilter.MAIN_TOKEN);
        Map<String, Element> processors = new HashMap<String, Element>();

        int nbCommands = pCore.getMainTable().getRowCount();
        for (int indCommand=0; indCommand < nbCommands; indCommand++) {
            String processName = (String) pCore.getMainTable().getValueAt(
                    indCommand, MainTable.Column.processor.ordinal());
            ProcessWrapper process = pCore.getWrapper(processName);
            String commandName = (String) pCore.getMainTable().getValueAt(
                    indCommand, MainTable.Column.command.ordinal());
            Command cmd = process.get(commandName);
            Float delay  = (Float) pCore.getMainTable().getValueAt(
                    indCommand, MainTable.Column.delay.ordinal());

            Element xmlProcessor = processors.get(processName);
            if (xmlProcessor == null) { // create Element
                xmlProcessor = pDoc.createElement(XmlFilter.PROCESSOR_TOKEN);
                xmlProcessor.setAttribute(XmlFilter.NAME_TOKEN, processName);
                xmlProcessor.setAttribute(XmlFilter.IP_TOKEN, process.getAddress());
                xmlProcessor.setAttribute(XmlFilter.PORT_TOKEN,
                                          Integer.toString(process.getPort()));
                processors.put(processName, xmlProcessor);
                result.appendChild(pDoc.createTextNode("\n  ")); // For readability
                result.appendChild(xmlProcessor);
            }

            Element xmlCommand = pDoc.createElement(XmlFilter.COMMAND_TOKEN);
            xmlCommand.setAttribute(XmlFilter.NAME_TOKEN, commandName);
            xmlCommand.setAttribute(XmlFilter.LINE_TOKEN, cmd.getLine());
            // ??? String path = (cmd.getDirectory() == null) ? "" : cmd.getDirectory();
            xmlCommand.setAttribute(XmlFilter.PATH_TOKEN, cmd.getDirectory());
            xmlCommand.setAttribute(XmlFilter.ORDER_TOKEN, Integer.toString(indCommand+1));
            xmlCommand.setAttribute(XmlFilter.DELAY_TOKEN, delay.toString());
            xmlProcessor.appendChild(pDoc.createTextNode("\n    ")); // For readability
            xmlProcessor.appendChild(xmlCommand);
        }

        // Create empty processor !!!
        for (String processName : pCore.getWrapperNames()) {
            Element xmlProcessor = (Element) processors.get(processName);
            ProcessWrapper process = pCore.getWrapper(processName);
            if (xmlProcessor == null) { // create Element
                xmlProcessor = pDoc.createElement(XmlFilter.PROCESSOR_TOKEN);
                xmlProcessor.setAttribute(XmlFilter.NAME_TOKEN, processName);
                xmlProcessor.setAttribute(XmlFilter.IP_TOKEN, process.getAddress());
                xmlProcessor.setAttribute(XmlFilter.PORT_TOKEN,
                                          Integer.toString(process.getPort()));
                processors.put(processName, xmlProcessor);
                result.appendChild(pDoc.createTextNode("\n  ")); // For readability
                result.appendChild(xmlProcessor);
            }
        }
        result.appendChild(pDoc.createTextNode("\n")); // For readability

        return result;
    }


    
    /**
     * Returns the texts.
     *
     * @return the texts
     */
    public TextMap getTexts() {
        return texts;
    }


    protected static ErrorHandler defaultErrorHandler = new ErrorHandler() {
        public void error(SAXParseException spe) throws SAXException { throw spe; }
        public void fatalError(SAXParseException spe) throws SAXException { throw spe; }
        public void warning(SAXParseException spe) throws SAXException { throw spe; }
    };



    class XmlParser { 
        protected ProcessWrapper process;
        protected Element command;
        public XmlParser(ProcessWrapper pProcess, Element pCommand) {
        // Parser temporary object
        process = pProcess;
        command = pCommand;
    }
    }



    public String load(Controller pCore, File pFile) throws IOException {
       try {
            // Create XML context
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setIgnoringComments(true);
            DocumentBuilder xmlBuilder = factory.newDocumentBuilder();

            // XML Error handler
            factory.setValidating(true);
            xmlBuilder.setErrorHandler(defaultErrorHandler);

            // Find DTD from resources
            xmlBuilder.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId)
                        throws SAXException, IOException {
                    if (systemId.endsWith(XmlFilter.DTD_FILENAME)) {
                        URL res = XmlFilter.class.getResource(XmlFilter.DTD_FILENAME);
                        if (res != null)
                            return new InputSource(res.openStream());
                    }
                    return null;
                }
            });

            // Build description
            InputStream is = new FileInputStream(pFile);
            if (read(pCore, xmlBuilder.parse(is, "local").getDocumentElement())) {
                return texts.get("Loaded", new Object[] { pFile.getPath() });
            } else {
                return texts.get("WarningLoaded", new Object[] { pFile.getPath() });
            }

        } catch (Exception e) {
            Object[] values = new Object[] { pFile.getPath(), e.getMessage(), null };
            String message;
            if (e instanceof SAXParseException) {
                // Technical value
                values[2] = Integer.toString(((SAXParseException) e).getLineNumber());
                message = texts.get("ErrFileReadLine" , values);
            } else
                message = texts.get("ErrFileRead" , values);
            throw (IOException) new IOException(message).initCause(e);
        }
    }

    protected boolean read(Controller pCore, Element pDescription) {
        boolean noWarning = true;

        SortedMap<Float, Object> newCommands = new TreeMap<Float, Object>();
        NodeList processList = pDescription.getElementsByTagName(XmlFilter.PROCESSOR_TOKEN);
        for (int indProcess=0; indProcess<processList.getLength(); indProcess++) {
            pDescription = (Element) processList.item(indProcess);
            String processName = pDescription.getAttribute(XmlFilter.NAME_TOKEN);
            String ip = pDescription.getAttribute(XmlFilter.IP_TOKEN);
            int port = Integer.parseInt(pDescription.getAttribute(XmlFilter.PORT_TOKEN));

            // Build or update
            ProcessWrapper process = pCore.getWrapper(processName);
            if (process == null) {
                process = pCore.add(processName, ip, port);
            } else if (!process.isConnected()) {
                process.setAddress(ip);
                process.setPort(port);
                System.out.println(texts.get("ProcUpdated", new Object[] { processName }));
                noWarning = false;
            } else if (!(process.getAddress().equals(ip) && process.getPort() == port)) {
                System.out.println(texts.get("ProcNotUpdated", new Object[] { processName }));
                noWarning = false;
            }

            // Global Command list
            NodeList commandList = pDescription.getElementsByTagName(XmlFilter.COMMAND_TOKEN);
            for (int indCommand=0; indCommand<commandList.getLength(); indCommand++) {
                Element cmdDescr = (Element) commandList.item(indCommand);
                String commandName = cmdDescr.getAttribute(XmlFilter.NAME_TOKEN);

                Command command = process.get(commandName);
                if ((command != null) && (command.isRunning())) { // already exist
                    try {
                        command.setLine(cmdDescr.getAttribute(XmlFilter.LINE_TOKEN));
                        command.setDirectory(cmdDescr.getAttribute(XmlFilter.PATH_TOKEN));
                    } catch (Exception e) {
                        System.err.println(texts.get("CmdNotUpdated",
                                new Object[] { processName, commandName }));
                     noWarning = false;
                    }
                } else {
                    Float order = new Float(cmdDescr.getAttribute(XmlFilter.ORDER_TOKEN));
                    Object old = newCommands.get(order); // Optimized list
                    if (old == null) {
                        newCommands.put(order, new XmlParser(process, cmdDescr));
                    } else if (old instanceof List) {
                        List<Object> xmlParser = (List<Object>) old;
                        xmlParser.add(new XmlParser(process, cmdDescr));
                    } else {
                        List<Object> sames = new ArrayList<Object>();
                        sames.add(old);
                        sames.add(new XmlParser(process, cmdDescr));
                        newCommands.put(order, sames);
                    }
                }
            }
        }

        // Add new commands in launch order
        Object last = newCommands.remove(new Float(0.0f)); // 0.0 : special value !!!
        for (Object item : newCommands.values()) {
            if (item instanceof List) {
                List<Object> sames = (List<Object>) item;
                for (int indCommand=0; indCommand<sames.size(); indCommand++) {
                    XmlParser xml = (XmlParser) sames.get(indCommand);
                    Command command = xml.process.add(xml.command.getAttribute(XmlFilter.NAME_TOKEN),
                                                      xml.command.getAttribute(XmlFilter.LINE_TOKEN),
                                                      xml.command.getAttribute(XmlFilter.PATH_TOKEN));
                    float delay = Float.parseFloat(xml.command.getAttribute(XmlFilter.DELAY_TOKEN));
                    pCore.getMainTable().setDelay(command, xml.process, delay);
                }

            } else {
                XmlParser xml = (XmlParser) item;
                Command command = xml.process.add(xml.command.getAttribute(XmlFilter.NAME_TOKEN),
                                                  xml.command.getAttribute(XmlFilter.LINE_TOKEN),
                                                  xml.command.getAttribute(XmlFilter.PATH_TOKEN));
                float delay = Float.parseFloat(xml.command.getAttribute(XmlFilter.DELAY_TOKEN));
                pCore.getMainTable().setDelay(command, xml.process, delay);
            }
        }

        if (last != null) {
            if (last instanceof List) {
                List<Object> sames = (List<Object>) last;
                for (int indCommand=0; indCommand<sames.size(); indCommand++) {
                    XmlParser xml = (XmlParser) sames.get(indCommand);
                    Command command = xml.process.add(xml.command.getAttribute(XmlFilter.NAME_TOKEN),
                                                      xml.command.getAttribute(XmlFilter.LINE_TOKEN),
                                                      xml.command.getAttribute(XmlFilter.PATH_TOKEN));
                    float delay = Float.parseFloat(xml.command.getAttribute(XmlFilter.DELAY_TOKEN));
                    pCore.getMainTable().setDelay(command, xml.process, delay);
                }
            } else {
                XmlParser xml = (XmlParser) last;
                Command command = xml.process.add(xml.command.getAttribute(XmlFilter.NAME_TOKEN),
                                                  xml.command.getAttribute(XmlFilter.LINE_TOKEN),
                                                  xml.command.getAttribute(XmlFilter.PATH_TOKEN));
                    float delay = Float.parseFloat(xml.command.getAttribute(XmlFilter.DELAY_TOKEN));
                pCore.getMainTable().setDelay(command, xml.process, delay);
            }
        }
        return noWarning;
    }


} // endclass XmlFormatter