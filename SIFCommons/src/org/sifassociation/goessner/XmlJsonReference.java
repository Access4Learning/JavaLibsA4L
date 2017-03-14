/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.goessner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.*;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import org.sifassociation.util.SIFFileUtil;
import org.sifassociation.util.SIFXOMUtil;

/**
 * Uses the official reference implementation on Goessner Notation to convert:
 * XML -> JSON
 * JSON -> XML
 * Singleton in order to help prevent stack overflow errors!
 * 
 * @author jlovell
 * @see http://goessner.net/
 */
public class XmlJsonReference implements IXmlJson {
    ScriptEngine engine = null;
    private static final XmlJsonReference INSTANCE = new XmlJsonReference();

    /**
     *  Used internally to initialize a working instance!
     */
    private XmlJsonReference() {        
        ScriptEngineManager factory = new ScriptEngineManager();
        engine = factory.getEngineByName("JavaScript");
        try {
            addToEngine("xml2json.js");
        } catch (ClassNotFoundException | IOException | ScriptException ex) {
            Logger.getLogger(XmlJsonReference.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            addToEngine("parse2json.js");
        } catch (ClassNotFoundException | IOException | ScriptException ex) {
            Logger.getLogger(XmlJsonReference.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            addToEngine("json2xml.js");
        } catch (ClassNotFoundException | IOException | ScriptException ex) {
            Logger.getLogger(XmlJsonReference.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            addToEngine("parse2xml.js");
        } catch (ClassNotFoundException | IOException | ScriptException ex) {
            Logger.getLogger(XmlJsonReference.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * So we can convert;
     * 
     * @return The one and only true XmlJsonReference instance.
     */
    public static XmlJsonReference getInstance() {
        return INSTANCE;
    }
    
    /**
     * Just a wrapper for the JavaScript version.
     * 
     * @param xml The XML string to convert.
     * @return JSON version of the XML string or empty string.
     */
    @Override
    public String xml2json(String xml) {
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        String json = "";
        Invocable inv = (Invocable) engine;
        try {
            json = (String) inv.invokeFunction("parse2json", stream, "  ");
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(XmlJsonReference.class.getName()).log(Level.SEVERE, null, ex);
        }
        return json;
    }
    
    /**
     * Just a wrapper for the JavaScript version.
     * 
     * @param json The JSON string to convert.
     * @return XML version of the JSON string or empty string.
     */
    @Override
    public String json2xml(String json) {
        String xml;
        Invocable inv = (Invocable) engine;
        try {
            xml = (String) inv.invokeFunction("parse2xml", json, "  ");
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(XmlJsonReference.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        // So we can work with the converted XML.
        Builder parser = new Builder();
        Document doc;
        try {
            doc = parser.build(xml, null);
        } catch (ParsingException | IOException ex) {
            Logger.getLogger(XmlJsonReference.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        Element root = doc.getRootElement();
        // So our converted XML is human readable.
        xml = SIFXOMUtil.pretty(root);
        return xml;
    }
    
    /**
     * 
     * 
     * @param filename  The name of the file (with extension) in the class path 
     * to add to the JavaScript engine.
     * Note:  The order in which this functional is called matters!
     * 
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws ScriptException 
     */
    private void addToEngine(String filename) throws ClassNotFoundException, IOException, ScriptException {
        InputStream resourceStream = Class.forName("org.sifassociation.goessner.XmlJsonReference").getClassLoader().getResourceAsStream(filename);
        String script = SIFFileUtil.readInputStream(resourceStream);
        engine.eval(script);
    }
}
