/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.messaging;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import static org.junit.Assert.*;

/**
 *
 * @author jlovell
 */
public class SimpleErrorHandler implements ErrorHandler {
    @Override
    public void warning(SAXParseException e) throws SAXException {
        fail(e.getMessage());
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        fail(e.getMessage());
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        fail(e.getMessage());
    }
}

