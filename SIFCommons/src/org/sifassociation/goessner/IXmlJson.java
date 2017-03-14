/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.goessner;

/**
 * Interface for various implementations of conversions using Goessner Notation.
 * XML -> JSON
 * JSON -> XML
 * 
 * @author jlovell
 * @see http://goessner.net/
 */
public interface IXmlJson {

    /**
     * Just a wrapper for the JavaScript version.
     *
     * @param json The JSON string to convert.
     * @return XML version of the JSON string or empty string.
     */
    String json2xml(String json);

    /**
     * Just a wrapper for the JavaScript version.
     *
     * @param xml The XML string to convert.
     * @return JSON version of the XML string or empty string.
     */
    String xml2json(String xml);
    
}
