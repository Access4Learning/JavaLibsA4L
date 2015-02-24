/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.util;

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author jlovell
 */
public class XSDErrorHandler implements ErrorHandler {
    List<String> errors = null;

    public XSDErrorHandler() {
        this.errors = new ArrayList<String>();
    }

    public boolean hasError() {
        return !errors.isEmpty();
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    @Override
    public void warning(SAXParseException e) throws SAXException {
        errors.add("Warning: " + e.getMessage() + " At:" + e.getLineNumber() + "," + e.getColumnNumber());
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        errors.add("Error: " + e.getMessage() + " At:" + e.getLineNumber() + "," + e.getColumnNumber());
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        errors.add("FatalError: " + e.getMessage() + " At:" + e.getLineNumber() + "," + e.getColumnNumber());
    }
}
