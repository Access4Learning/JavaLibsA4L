/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.messaging;

/**
 * Minimal interface to parse, convert, and serialize.
 * 
 * @author jlovell
 * @since 3.0
 */
public interface ISIFMessageXML {

    /**
     * For checking if the payload matches an expectation.
     *
     * Note:  Classic SIF message payloads must include an outer SIF_Body tag.
     * 
     * @param xPath
     * @return boolean  True if the XPath is found, else false.
     * @since 3.0
     */
    boolean checkPayload(String xPath);

    /**
     * Retrieves the current desired transport.
     *
     * Note: Must reflect the message after parsing, but may be changed.
     *
     * @return String  HTTP or SOAP (capitalized).
     * @since 3.0
     */
    String getTransport();

    /**
     * Parses an valid SIF message and commits contents to this class.
     *
     * Note:  This should be the only place "original" is set (or modified).
     *
     * To Do:  Add schema validation (XOM style).
     *
     * Postcondition:  "Missing" comprises a list of omitted components.
     *
     * @param XML  The text of the messages (sans any transport headers).
     * @throws Exception (Based on the implementation of the interface.)
     * @since 3.0
     */
    void parse(String XML) throws Exception;
    
    /**
     * Set the desired transport for XML serialization.
     *
     * @param indicator  HTTP or SOAP (case insensitive).
     * @throws NullPointerException if indicator is null.
     * @throws IllegalArgumentException if the transport is not expected.
     * @since 3.0
     */
    void setTransport(String indicator);

    /**
     * Retrieves the message ID (form varies based on the messages transport).
     * 
     * @return "Unique" String on what has been parsed or set.
     * @since 3.0
     */
    public String getMessageId();
    
    /**
     * Retrieves the type in the form for the current transport.
     * 
     * @return String  The type of message.
     * @since 3.0
     */
    public String getType();

    // To Do:  Get the type straightened out!
    SIFHttpHeaders getHttpHeaders();
    
    /**
     * Serialize the contents of this class to either HTTP or SOAP text.
     *
     * To Do:  Add pretty print member variable and enforce here.
     *
     * @return String
     * @see setTransport
     * @see getHTTP
     * @see getSOAP
     * @since 3.0
     */
    @Override
    public String toString();
    
}
