/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.messaging;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.XPathContext;
import org.sifassociation.util.SIFXOMUtil;

/**
 *
 * @author jlovell
 */
public class SIF3Message implements ISIFMessageXML {

    private XPathContext namespaces;  // So we can evaulate XPaths properly.
    private String transport;  // So we can support any desired transport.
    private SIFRefId messageId;  // So we have the GUID for the message.
    private String type;  // The type of the SIF message (i.e. POST).
    
    private URL relatedURL;  // So we can know: object type & parameters.
    private Document payload;  // So we can wrap the the proper type.
    
    SIFHttpHeaders headers;  // So we can set headers not automatically handled.

    public SIF3Message() {
        namespaces = new XPathContext();
        addNamespace("in30", "http://www.sifassociation.org/infrastructure/3.0");
        addNamespace("us30", "http://www.sifassociation.org/datamodel/us/3.0");
        setTransport("REST");
        headers = new SIFHttpHeaders();
        setMessageId(new SIFRefId());
    }
    
    
    // To Do: Consider an abstract class.
    @Override
    public boolean checkPayload(String xPath) {
        if (0 < payload.query(xPath, namespaces).size()) {return true;}
        
        return false;
    }

    @Override
    public String getTransport() {
        return transport;
    }

    @Override
    public void parse(String XML) throws Exception {
        // So we have a tree to inspect.
        Builder parser = new Builder();
        payload = parser.build(XML, null);
    }

    @Override
    public void setTransport(String indicator) {
        // So we validate the supplied transport.
        if(null == indicator) {
            throw new NullPointerException("A String was expected, null was "
                    + "passed.");
        }
        else if("REST".equalsIgnoreCase(indicator)) {
            transport = "REST";
        }
        else {
            throw new IllegalArgumentException(indicator + " given REST"
                    + " expected.");
        }
    }

    @Override
    public SIFRefId getMessageId() {      
        return messageId;
    }

    // To Do: Consider an abstract class.
    public void setMessageId(SIFRefId messageId) {
        this.messageId = messageId;
        this.getHttpHeaders().addHttpHeader("messageId", messageId.toString());
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    /**
     * Sets the type of the message in SOAP form.
     * 
     * @param type 
     * @since 3.0
     */
    public void setType(String type) {        
        List<String> options = Collections.unmodifiableList(Arrays.asList(
                "GET", "POST", "PUT", "DELETE"));
        if(! options.contains(type)) {
            throw new IllegalArgumentException(type + " is not a valid message "
                    + "type.");
        }        
        
        this.type = type;
    }
    
    /**
     * Sets the root namespace of the payload (used in the SIF header).
     * 
     * Using the "sif" prefix this xpath may be used by member XPath functions.
     * 
     * @param namespace  The namespace of the payload regardless of scope.
     * @since 3.0
     */
    // To Do: Consider an abstract class.
    public void setNamespace(String namespace) {
        namespaces.addNamespace("sif", namespace);
    }
    
    /**
     * 
     * 
     * @param prefix
     * @return String  The namespace URI of the prefix specified.
     * @since 3.0
     */
    // To Do: Consider an abstract class.
    public String getNamespace(String prefix) {
        return namespaces.lookup(prefix);
    }
    
    /**
     * Allows additional namespaces so that payloads can be deeply inspected.
     * 
     * If the prefix is already bound in this context, the new replaces the old.
     * Binding a prefix to null removes the declaration.
     * 
     * @param prefix
     * @param namespace  URI
     * @since 3.0
     */
    // To Do: Consider an abstract class.
    public void addNamespace(String prefix, String namespace) {
        namespaces.addNamespace(prefix, namespace);
    }

    public URL getRelatedURL() {
        return relatedURL;
    }

    public void setRelatedURL(URL relatedURL) {
        this.relatedURL = relatedURL;
    }
    
    @Override
    public SIFHttpHeaders getHttpHeaders() {
        return headers;
    }    
    
    @Override
    public String toString() {
        try {
            return SIFXOMUtil.pretty(payload.getRootElement());
        } catch (Exception ex) {
            return "<SerializationError>" + SIF3Message.class.getName() + ": "
                    + ex + "</SerializationError>";
        }
    }

}
