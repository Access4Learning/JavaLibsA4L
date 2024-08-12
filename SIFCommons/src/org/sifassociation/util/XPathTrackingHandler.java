/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.sifassociation.util;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XPathTrackingHandler extends DefaultHandler {
    private final int targetLine;
    private final int targetColumn;
    private final StringBuilder currentXPath = new StringBuilder();
    private String foundXPath;
    private Locator locator;

    public XPathTrackingHandler(int targetLine, int targetColumn) {
        this.targetLine = targetLine;
        this.targetColumn = targetColumn;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        currentXPath.append("/").append(qName);

        // Check if this is the location of the error
        if (locator.getLineNumber() == targetLine && locator.getColumnNumber() <= targetColumn) {
            foundXPath = currentXPath.toString();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        int lastIndex = currentXPath.lastIndexOf("/");
        if (lastIndex != -1) {
            currentXPath.delete(lastIndex, currentXPath.length());
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // Check if the current position matches the target line and column for text content
        if (locator.getLineNumber() == targetLine && locator.getColumnNumber() <= targetColumn) {
            // Capture the current XPath for a text node error
            if (foundXPath == null) {
                foundXPath = currentXPath.toString();
            }
        }
    }

    public String getXPath() {
        // Return the foundXPath or the currentXPath if foundXPath is null
        return foundXPath != null ? foundXPath : currentXPath.toString();
    }
}