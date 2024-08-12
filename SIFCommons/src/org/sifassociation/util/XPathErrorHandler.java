/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.sifassociation.util;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

public class XPathErrorHandler implements ErrorHandler {

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        System.err.println("Warning: " + getErrorMessage(exception));
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        System.err.println("Error: " + getErrorMessage(exception));
        throw exception;  // Rethrow to capture error details in the calling code
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        System.err.println("Fatal error: " + getErrorMessage(exception));
        throw exception;  // Rethrow to capture error details in the calling code
    }

    private String getErrorMessage(SAXParseException exception) {
        return "Line " + exception.getLineNumber() +
               ", Column " + exception.getColumnNumber() +
               ": " + exception.getMessage();
    }
}