/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.messaging;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author jlovell
 */
public class SIFHttpHeaders{
    protected ArrayList<String> httpHeaders; // So we can know: security, paging, and etc.

    public SIFHttpHeaders() {
        httpHeaders = new ArrayList<String>(10);
    }

    public void addHttpHeader(String header) {
        this.httpHeaders.add(header);
    }

    public void addHttpHeader(String name, String value) {
        String combined = name + ": " + value;
        this.httpHeaders.add(combined);
    }

    public ArrayList<String> getHttpHeaders() {
        return httpHeaders;
    }

    public void setHttpHeaders(Collection<String> headers) {
        httpHeaders = new ArrayList<String>(10);
        httpHeaders.addAll(headers);
    }

    /**
     * So we can quickly and easily access a headers value.
     * 
     * @param name  The case-insensitive name of the header.
     * @return   The value associated with the name or the empty string.
     */
    public String getHeaderValue(String name) {
        for(String header : httpHeaders) {
            if(0 == name.compareToIgnoreCase(getName(header))) {
                return getValue(header);
            }
        }
        return "";
    }
    
    public static String getName(String header) {
        return header.substring(0, header.indexOf(": "));
    }
    
    public static String getValue(String header) {
        return header.substring(header.indexOf(": ") + 2, header.length());
    }
    
}
