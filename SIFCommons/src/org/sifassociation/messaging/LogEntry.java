/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sifassociation.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import nu.xom.*;
import org.sifassociation.goessner.XmlJson;
import org.sifassociation.util.SIFAuthUtil;

/**
 * The log entry class intended to be depended on by all implementations of 
 * IMessageLog.
 * 
 * Note:  Depends on XOM.
 * 
 * @author jlovell
 * @since 3.0
 * @version 3.0
 */
public class LogEntry {
    protected String identification = "";
    protected String categorization = "sent";  // received, error
    protected String display = "";
    protected XMLGregorianCalendar timestamp = null;
    
    protected String relatedURL = "";
    protected String requestMethod = "";
    protected String requestID = "";
    protected SIFHttpHeaders httpHeaders = null;
    protected String httpBody = "";
    protected Element parsedBody = null;
    protected String error = "";

    protected String cipher = "";
    protected String protocol = "";
    protected List<SIFCertificateInfo> certificates = null;
    
    public LogEntry() {
        // So we have the current date and time
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        try {
            timestamp = DatatypeFactory.newInstance().
                    newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException ex) {
            Logger.getLogger(LogEntry.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally {
            httpHeaders = new SIFHttpHeaders();
            certificates = new ArrayList<SIFCertificateInfo>();
        }
    }
    
    public void parse(String entry) {
        Builder parser = new Builder();
        Document doc = null;
        try {
            doc = parser.build(entry, null);
        } catch (ParsingException ex) {
            Logger.getLogger(LogEntry.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LogEntry.class.getName()).log(Level.SEVERE, null, ex);
        }
        Element root = doc.getRootElement();
        
        // Information
        setIdentification(root.getFirstChildElement("identification").getValue());
        setCategorization(root.getFirstChildElement("categorization").getValue());
        setDisplay(root.getFirstChildElement("display").getValue());
        try {
            timestamp = SIFAuthUtil.stringToXMLGregorianCalendar(
                    root.getFirstChildElement("timestamp").getValue());
        } catch (DatatypeConfigurationException ex) {
            Logger.getLogger(LogEntry.class.getName()).log(Level.SEVERE, null, ex);
        }
        setRelatedURL(root.getFirstChildElement("url").getValue());
        setRequestMethod(root.getFirstChildElement("requestMethod").getValue());
        setRequestID(root.getFirstChildElement("requestID").getValue());
        
        // Headers
        SIFHttpHeaders headers = new SIFHttpHeaders();
        Element parent = root.getFirstChildElement("httpHeaders");
        Elements children = parent.getChildElements();
        for(int i = 0; i < children.size(); i++) {
            headers.addHttpHeader(children.get(i).getValue());
        }
        httpHeaders = headers;
        
        // Body
        setHttpBody(root.getFirstChildElement("httpBody").getValue());
        
        // Error
        setError(root.getFirstChildElement("error").getValue());

        // Protocol
        setProtocol(root.getFirstChildElement("protocol").getValue());
        
        // Cipher
        setCipher(root.getFirstChildElement("cipher").getValue());
        
        // Certificates
        Element certificatesRoot = root.getFirstChildElement("certificates");
        Elements certs = certificatesRoot.getChildElements();
        for(int i = 0; i < certs.size(); i++) {
            // Certificate
            Element certificate = certs.get(i);
            SIFCertificateInfo info = new SIFCertificateInfo();
            info.parse(certificate);
            certificates.add(info);
        }
    }
    
    public void fromRequest(String id, HttpServletRequest request, String body) {
        // Information
        this.setIdentification(id);
        this.setCategorization("received");
        this.setDisplay(request.getMethod());
        
        // So the URL includes the query string.
        String url = request.getRequestURL().toString();
        if(null != request.getQueryString()) {
            url =  url + "?" + request.getQueryString();
        }
        this.setRelatedURL(url);
        this.setRequestMethod(request.getMethod());
        
        //  Headers
        Enumeration<String> names = request.getHeaderNames();
        while(names.hasMoreElements()) {
            String name = names.nextElement();
            String value = request.getHeader(name);
            this.httpHeaders.addHttpHeader(name, value);
        }
        
        // Body
        this.setHttpBody(body);
    }
    
    public void fromResponse(String id, HttpServletResponse response, 
            String body, HttpServletRequest request) {
        // Information
        this.setIdentification(id);
        this.setCategorization("sent");
        this.setDisplay(Integer.toString(response.getStatus()));
        
        // So the URL includes the query string.
        String url = request.getRequestURL().toString();
        if(null != request.getQueryString()) {
            url =  url + "?" + request.getQueryString();
        }
        this.setRelatedURL(url);
        this.setRequestMethod(request.getMethod());
        
        //  Headers
        for(String name : response.getHeaderNames())
        {
            String value = response.getHeader(name);
            this.httpHeaders.addHttpHeader(name, value);
        }
        
        // Body
        this.setHttpBody(body);        
    }
    
    /**
     * So we can work with (usually serialize) the log entry.
     * 
     * @param synopsis If "true" listing, else entire entry.
     * @return Root element representing the log entry.
     * 
     * @see toString
     * @since 3.0
     */
    public Element toXOM(boolean synopsis) {
        Element root = new Element("entry");
        
        Element id = new Element("identification");
        root.appendChild(id);
        id.appendChild(identification.toString());
        
        Element classification = new Element("categorization");
        root.appendChild(classification);
        classification.appendChild(categorization);
        
        Element label = new Element("display");
        root.appendChild(label);
        label.appendChild(display);
        
        Element stamp = new Element("timestamp");
        root.appendChild(stamp);
        if(null != timestamp) {
            stamp.appendChild(timestamp.toString());
        }
        
        if(! synopsis) {
            // So we can show the URL employeed.
            Element url = new Element("url");
            root.appendChild(url);
            url.appendChild(relatedURL);
            
            // So the response handler knows the HTTP request method.
            Element method = new Element("requestMethod");
            root.appendChild(method);
            method.appendChild(requestMethod);
            
            // So we can retrieve the request entry.
            Element request = new Element("requestID");
            root.appendChild(request);
            request.appendChild(requestID);
            
            // So we can know what httpHeaders were actually used.
            Element headers = new Element("httpHeaders");
            root.appendChild(headers);
            ArrayList<String> usedHeaders = httpHeaders.getHttpHeaders();
            for(String usedHeader : usedHeaders) {
                Element header = new Element("httpHeader");
                header.appendChild(usedHeader);
                headers.appendChild(header);
            }
            
            // So we can show the message exactly as it was conveyed.
            // So we have a copy of even an invalid message.
            Element body = new Element("httpBody");
            root.appendChild(body);
            if(0 != "".compareTo(httpBody)) {
                body.appendChild(httpBody);
            }
            
            // So we have a pretty printed version of the message ready.
            Element parsed = new Element("parsedBody");
            root.appendChild(parsed);
            if(null != parsedBody) {
                parsed.appendChild(new Element(parsedBody));
            }
            
            // So we know if we have encoutered a problem.
            Element problem = new Element("error");
            root.appendChild(problem);
            problem.appendChild(error);
            
            // So we know about the protocol used.
            Element negotiated = new Element("protocol");
            root.appendChild(negotiated);
            negotiated.appendChild(protocol);
            
            // So we know about the encryption used.
            Element suite = new Element("cipher");
            root.appendChild(suite);
            suite.appendChild(cipher);
            
            // So we know about the certificates received.
            Element certs = new Element("certificates");
            root.appendChild(certs);
            for(SIFCertificateInfo info : certificates) {
                certs.appendChild(info.toXOM());
            }
        }
            
        return root;
    }
    
    /**
     * So we can serialize the entire log entry.
     * 
     * Note:  Prints the XOM elements as pretty XML.
     * 
     * @return XML representing the log entry.
     * 
     * @since 3.0
     */
    public String toString() {
        Element root = toXOM(false);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Serializer serializer;
        
        String output = "";
        boolean success = true;
        try {
            serializer = new Serializer(out, "UTF-8");
            serializer.setIndent(2);
            serializer.write(new Document(root));
            serializer.flush();
            output = out.toString("UTF-8");
            // So we drop the XML version and encoding declarations.
            output = output.substring(output.indexOf(
                    serializer.getLineSeparator()) + 1);              
        } catch (Exception ex) {
            success = false;
        }
        
        if(success) {
            return output;
        }
        
        return root.toXML();
    }
    
    /**
     * So we can access the unique ID of the current log entry.
     * 
     * @return The unique identifier  of this log entry.
     * 
     * @since 3.0
     */
    public String getIdentification() {
        return identification;
    }

    /**
     * So we can set the unique ID of the current log entry.
     * 
     * Note:  It is up to the container to ensure this field is unique.
     * 
     * @param identification 
     * 
     * @since 3.0
     */
    public void setIdentification(String identification) {
        this.identification = identification;
    }

    /**
     * So we can access how this message has been categorized.
     * 
     * @return 
     * 
     * @see setCategorization for possible values.
     * @since 3.0
     */
    public String getCategorization() {
        return categorization;
    }

    /**
     * So we can set the category of the message.
     * 
     * Designed to help the user understand what is going on.
     * 
     * @param classification 
     *        sent = Attempted to send this message.
     *        received = Successfully consumed a message.
     *        error = This message communicates an error state.
     * 
     * @throws IllegalArgumentException if the categorization is not well 
     *         understood.
     * @since 3.0
     */
    public void setCategorization(String classification) {
        List<String> options = Collections.unmodifiableList(Arrays.asList(
                "sent", "received", "error"));
        if(! options.contains(classification)) {
            throw new IllegalArgumentException(classification + "is not a valid"
                    + " message classification.");
        }
        
        this.categorization = classification;
    }

    public SIFHttpHeaders getHeaders() {
        return httpHeaders;
    }
    
    /**
     * Retrieves the message exactly as it was conveyed.
     * 
     * @return 
     * 
     * @since 3.0
     */
    public String getHttpBody() {
        return httpBody;
    }

    /**
     * Keeps the message both exactly as it was conveyed and as parse.
     * 
     * Note:  If not valid XML the parsed copy is not kept.
     * 
     * @param httpBody 
     * 
     * @since 3.0
     */
    public void setHttpBody(String httpBody) {
        this.httpBody = httpBody;
        
        // So we can search the XML (if it is XML).
        Builder parser = new Builder();
        Document doc = null;
        try {
            doc = parser.build(httpBody, null);
        } catch (Exception ex) {
            String temp = "";
            try {
                // The HTTP body was not XML (this is okay), see if we can convert.
                temp = XmlJson.getInstance().json2xml(httpBody);
            } catch (ScriptException | NoSuchMethodException | ParsingException | IOException ex1) {
                // Not JSON either, (this is okay) we just won't be able to search/format.
            }
            if(!temp.isEmpty()) {
                try {
                    doc = parser.build(temp, null);
                } catch (ParsingException | IOException ex1) {
                    // Still just fine!
                }
            }
        }
        if(doc != null) {
            this.parsedBody = new Element(doc.getRootElement());
        }
    }

    // So we can easily inspect the HTTP body.
    public Element getParsedBody() {
        return this.parsedBody;
    }
    
    /**
     * So we can show our very short description of this log entry.
     * 
     * @return 
     * 
     * @since
     */
    public String getDisplay() {
        return display;
    }

    /**
     * So we can give this long entry any very short description we want.
     * 
     * @param display The very short description of this log entry.
     * 
     * @since 3.0
     */
    public void setDisplay(String display) {
        this.display = display;
    }

    /**
     * So we can investigate any non message payload errors that occur.
     * 
     * @return (Presumably) non XML error message.
     * 
     * @since 3.0
     */
    public String getError() {
        return error;
    }

    /**
     * So we can express any non message payload errors (such as an exception).
     * 
     * @param error 
     * 
     * @since 3.0
     */
    public void setError(String error) {
        this.error = error;
    }

    public String getRelatedURL() {
        return relatedURL;
    }

    public void setRelatedURL(String relatedURL) {
        this.relatedURL = relatedURL;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * So we can support query parameters.
     * 
     * @param url
     * @return
     * @throws UnsupportedEncodingException 
     * @see http://stackoverflow.com/questions/13592236/parse-a-uri-string-into-name-value-collection
     * @author Pr0gr4mm3r
     * @since 3.1
     * @version 3.1
     */
    private static Map<String, List<String>> splitQuery(URL url) throws UnsupportedEncodingException {
        final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
        if(null != url.getQuery()) {  // Added JWL
            final String[] pairs = url.getQuery().split("&");
            for (String pair : pairs) {
                final int idx = pair.indexOf("=");
                final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                if (!query_pairs.containsKey(key)) {
                    query_pairs.put(key, new LinkedList<String>());
                }
                final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
                query_pairs.get(key).add(value);
            }
        }
        return query_pairs;
    }
    
    public String getParameter(String key) {
        String value = "";
        
        URL temp = null;
        try {
            temp = new URL(this.getRelatedURL());
        } catch (MalformedURLException ex) {
            Logger.getLogger(LogEntry.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(null != temp) {
            Map<String, List<String>> parameters = null;
            try {
                parameters = splitQuery(temp);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(LogEntry.class.getName()).log(Level.SEVERE, null, ex);
            }
            if(null != parameters) {
                List<String> matches = parameters.get(key);
                if(null != matches && 1 == matches.size()) {
                    value = matches.get(0);
                }
            }
        }
        
        return value;
    }
    
    public List<SIFCertificateInfo> getCertificates() {
        return certificates;
    }

    /**
     * So we can check if this log entry matches the given XPath.
     * 
     * Note:  The collection class may implement searching without this helper.
     * 
     * @param xpath
     * @param namespaceT "transport"
     * @param namespaceP "payload"
     * @return 
     */
    public boolean isMatch(String xpath, String namespaceT, String namespaceP) {
        if(null != parsedBody && 0 < xpath.length()) {
            XPathContext namespaces = new XPathContext();
            namespaces.addNamespace("t", namespaceT);
            namespaces.addNamespace("p", namespaceP);
            Document temp = new Document(new Element(parsedBody));
            Nodes matches = null;
            try {
                matches = temp.query(xpath, namespaces);
            } catch (Exception ex) {
                // Invalid XPath, no problem (there simply will be no matches).
            }
            if(null != matches && 0 < matches.size()) {
                return true;
            }
        }
        
        return false;
    }

}
