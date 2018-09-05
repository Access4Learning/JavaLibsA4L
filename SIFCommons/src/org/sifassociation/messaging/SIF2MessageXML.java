package org.sifassociation.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.xml.bind.DatatypeConverter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import nu.xom.*;
import org.sifassociation.util.SIFXOMUtil;

/**
 * Mutable class for creating and storing SIF messages.
 * 
 * Note:  This class is designed to be transport agnostic.
 * 
 * Note:  This class is NOT designed to simplify payload handling or generation.
 * 
 * XOM was chosen as the XML library for the following reasons:
 * - The author's love of a good tree API.
 * - Efficient SAX parsing.
 * - Excellent namespace support.
 * - Strong XPath support.
 * - Inherent document validity.
 * - Schema validation (while parsing, must know SAX layer).
 * - First class UTF-8 support!
 * - Do not forget, pretty printing.
 * 
 * Designed to convert SIF messages from HTTP to SOAP seamlessly.
 * Since it is mutable, a telescoping construction pattern is employed.
 * 
 * @author jlovell
 * @version 3.0
 * @since 3.0
 */
public final class SIF2MessageXML implements ISIFMessageXML {
    
    private XPathContext namespaces;  // So we can evaulate XPaths properly.
    private String transport;  // So we can support any desired transport.
    private String original;  // So we can recall the original message.
    // So we can communicate what more was needed to have a complete message.
    private List<String> missing;
    private SIFVersion infrastructureVersion;
    private SIFVersion payloadVersion;
    private String zoneId;  // URI which uniquely idetifies the Zone.
    // Header parts present to both transports.
    private boolean security;  // Enable/Disable security serialization.
    // 0: No authentication required and a valid certificate does not need to be
    //    presented.
    // 1: A valid certificate must be presented.
    // 2: A valid certificate from a trusted certificate authority must be
    //    presented.
    // 3: A valid certificate from a trusted certificate authority must be
    //    presented and the CN field of the certificate's Subject entry must
    //    match the host sending the certificate.
    private int authenticationLevel;
    // 0: No encryption required.
    // 1: Symmetric key length of at least 40 bits is to be used.
    // 2: Symmetric key length of at least 56 bits is to be used.
    // 3: Symmetric key length of at least 80 bits is to be used.
    // 4: Symmetric key length of at least 128 bits is to be used.
    private int encryptionLevel;
    private XMLGregorianCalendar timestamp;  // Native xs:dateTime
    private String sourceId;  // The name of the message creator.
    private String destinationId;  // The name of the destination (if any).
    private List<String> contexts;
    private long packetNumber;  // Technically an unsigned integer.
    private boolean morePackets;  // false = No, true = Yes
    private List<SIFVersion> responseVersions;
    private String topicName;  // Name of the object or service.
    private String eventAction;  // Add, Change, or Delete;
    private long maxBufferSize;  // Technically an unsigned integer.
    private String service;
    private String operation;
    
    private String type;  // The type of the SIF message (i.e. SIF_Event).
    
    // WSAddressing
    private URL to;  // So we know where the message is to be sent.
    private URL from;  // So we know where the message came from.
    private SIFRefId messageId;  // So we have the GUID for the message.
    private String action;  // So we know what the message instigates.
    private String sourcedTo;  // So we know what sent the related message. 
    private SIFRefId relatesTo;  // So we know what other message...
    private String relationshipType;  // So we know how the other message...
    private URL replyTo;  // RESERVED
    private URL faultTo;  // RESERVED
    
    private Document payload;  // So we can wrap the the proper type.
    
    /**
     * Ensures all variables are initialized.
     * 
     * Note:  RefIds and URLs may be null.
     * 
     * Minimally must call ... in order to have a complete and valid message.
     *  setType, setMessageId, and setPayload
     *  or
     *  parse
     * 
     * @since 3.0
     */
    public SIF2MessageXML() throws MalformedURLException, ParsingException, IOException, DatatypeConfigurationException {
        // So we start with reasonable defaults.
        
        // So we know about globally understood namespaces.
        // Default binding: xml http://www.w3.org/XML/1998/namespace
        namespaces = new XPathContext();
        addNamespace("soap", 
                "http://schemas.xmlsoap.org/soap/envelope/");
        addNamespace("wsa", 
                "http://www.w3.org/2005/08/addressing"); 
        addNamespace("t", // Transport/Global Header
                "http://www.sifassociation.org/transport/soap/2.x");
        addNamespace("m", // Message wrapper NOT Payload/Data Model.
                "http://www.sifassociation.org/message/soap/2.x");
        addNamespace("xsi", 
                "http://www.w3.org/2001/XMLSchema-instance");
        addNamespace("xs", 
                "http://www.w3.org/2001/XMLSchema");
        setTransport("SOAP");
        setNamespace("http://www.sifinfo.org/infrastructure/2.x");  // sif
        original = "";
        missing = new ArrayList<String>();
        setInfrastructureVersion(new SIFVersion("2.7"));
        setPayloadVersion(new SIFVersion("2.7"));
        setZoneId("urn:sif:zone:PRODUCT.TestHarness3.SIF");
        setSecurity(false);
        setAuthenticationLevel(0);
        setEncryptionLevel(0);
        // So we have the current date and time
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        XMLGregorianCalendar now = DatatypeFactory.newInstance().
                newXMLGregorianCalendar(gregorianCalendar);
        setTimestamp(now);
        setSourceId("");
        setDestinationId("");
        contexts = new ArrayList<String>();
        setPacketNumber(1);
        setMorePackets(false);
        responseVersions = new ArrayList<SIFVersion>();
        setTopicName("");
        setEventAction("");
        setMaxBufferSize(0);
        setService("");
        setOperation("");
        setType("Ack");
        setTo(new URL("http://www.w3.org/2005/08/addressing/anonymous"));
        setFrom(new URL("http://www.w3.org/2005/08/addressing/anonymous"));
        setMessageId(null);
        setAction("");
        setSourcedTo("");
        setRelatesTo((SIFRefId)null);
        setRelationshipType("");
        setReplyTo(null);
        setFaultTo(null);
        payload = null;
    }

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
     * @throws ParsingException (XOM)
     * @throws IOException
     * @since 3.0
     */
    @Override
    public void parse(String XML) throws ParsingException, IOException, DatatypeConfigurationException {
        original = XML;
        
        // So we have a tree to inspect.
        Builder parser = new Builder();
        Document doc = parser.build(XML, null);
        
        // So we interpret the message properly.
        String key = "http://schemas.xmlsoap.org/soap/envelope/:Envelope";
        Element root = doc.getRootElement();
        if(key.equals(root.getNamespaceURI() + ":" + root.getLocalName())) {
            commitSOAP(doc);
        }
        else {
            commitHTTP(doc);
        }
        
        // So we know if the message is complete.
        checkComplete();
    }

    /**
     * Take a parsed SOAP message and commit its parts to the class variables.
     * 
     * To Do:  Support receiving SOAP faults.
     * 
     * @param tree
     * @since 3.0
     */
    private void commitSOAP(Document tree) throws ParsingException, IOException, DatatypeConfigurationException {
        // So we recreate the same kind of message by default.
        setTransport("SOAP");
        
        // So we can readily work with the SOAP header.
        Element root = tree.getRootElement();
        String soap = getNamespace("soap");
        Element header = root.getFirstChildElement("Header", soap);
        
        // WS Addressing
        // Note:  Omitting elements where allowed!
        String wsa = getNamespace("wsa");
        Element current = header.getFirstChildElement("To", wsa);
        if(null != current) {
            setTo(new URL(current.getValue()));
        }
        
        // To Do:  Is this really optional.
        current = header.getFirstChildElement("MessageID", wsa);
        if(null != current) {
            setMessageId(new SIFRefId(current.getValue()));
        }
        
        current = header.getFirstChildElement("Action", wsa);
        setAction(current.getValue());
        setType(SIFActionUtil.ActionToType(current.getValue()));
        
        current = header.getFirstChildElement("RelatesTo", wsa);
        if(null != current) {
            this.setRelatesTo(current.getValue());
        }
        
        // So we can readily work with the SIF header.
        String t = getNamespace("t");  // SIF Global Transport Namespace
        Element sifHeader = header.getFirstChildElement("SIFHeader", t);

        current = sifHeader.getFirstChildElement("Timestamp", t);
        setTimestamp(current.getValue());
        
        current = sifHeader.getFirstChildElement("ZoneId", t);
        if(null != current) {
            setZoneId(current.getValue());
        }
        
        current = sifHeader.getFirstChildElement("InfrastructureVersion", t);
        setInfrastructureVersion(new SIFVersion(current.getValue()));
        
        current = sifHeader.getFirstChildElement("DataModel", t);
        if(null != current) {
            setNamespace(current.getValue());
        }
        
        current = sifHeader.getFirstChildElement("DataModelVersion", t);
        if(null != current) {
            setPayloadVersion(new SIFVersion(current.getValue()));
        }
        
        // So we can readily work with the security information.
        Element sec = sifHeader.getFirstChildElement("Security", t);
        if(null != sec) {
            setSecurity(true);
            Element secure = sec.getFirstChildElement("SecureChannel", t);
            if(null != secure) {
                current = secure.getFirstChildElement("AuthenticationLevel", t);
                setAuthenticationLevel(Integer.parseInt(current.getValue()));
                current = secure.getFirstChildElement("EncryptionLevel", t);
                setEncryptionLevel(Integer.parseInt(current.getValue()));
            }
        }
        
        current = sifHeader.getFirstChildElement("SourceId", t);
        setSourceId(current.getValue());
        
        current = sifHeader.getFirstChildElement("DestinationId", t);
        if(null != current) {
            setDestinationId(current.getValue());
        }
        
        // So we can readily work with the packet information.
        Element pac = sifHeader.getFirstChildElement("PacketData", t);
        if(null != pac) {
            current = pac.getFirstChildElement("PacketNumber", t);
            setPacketNumber(Long.parseLong(current.getValue()));
            current = pac.getFirstChildElement("MorePackets", t);
            setMorePackets(current.getValue());
        }
        
        current = sifHeader.getFirstChildElement("SourceId", t);
        setSourceId(current.getValue());
        
        current = sifHeader.getFirstChildElement("DestinationId", t);
        if(null != current) {
            setDestinationId(current.getValue());
        }
        
        // So we can readily work with the context information.
        Element parent = sifHeader.getFirstChildElement("Contexts", t);
        if(null != parent) {
            Elements children = parent.getChildElements();
            for(int i = 0; i < children.size(); i++) {
                current = children.get(i);
                addContext(current.getValue());
            }
        }
        
        current = sifHeader.getFirstChildElement("TopicName", t);
        if(null != current) {
            setTopicName(current.getValue());
        }
        
        current = sifHeader.getFirstChildElement("EventAction", t);
        if(null != current) {
            setEventAction(current.getValue());
        }
        
        current = sifHeader.getFirstChildElement("MaxBufferSize", t);
        if(null != current) {
            setMaxBufferSize(Long.parseLong(current.getValue()));
        }
        
        Elements children = sifHeader.getChildElements("ResponseVersion", t);
        for(int i = 0; i < children.size(); i++) {
            current = children.get(i);
            addResponseVersion(new SIFVersion(current.getValue()));
        }
        
         // So we can readily work with the Zone Service information.
        Element zoneService = sifHeader.getFirstChildElement("ZoneServiceData", 
                t);
        if(null != zoneService) {
            current = zoneService.getFirstChildElement("Operation", t);
            if(null != current) {setOperation(current.getValue());}
            current = zoneService.getFirstChildElement("ServiceMsgId", t);
            // To Do:  This is more complicated than this.
            setRelatesTo(current.getValue());
        }
        
        /* PAYLOADS!!! */
                
        // So we keep the message payload.
        setPayload(root.getFirstChildElement("Body", soap));
    }
    
    /**
     * Take a parsed classic message and commit its parts to the class variables.
     * 
     * @param tree
     * @since 3.0
     */
    private void commitHTTP(Document tree) throws DatatypeConfigurationException, ParsingException, IOException {
        // So we recreate the same kind of message by default.
        setTransport("HTTP");
        
        // So we know the namespace of the message (these differ by country).
        Element root = tree.getRootElement();
        Element current = null;
        String ns = root.getNamespaceURI();
        setNamespace(ns);
        SIFVersion version = new SIFVersion(root.getAttributeValue("Version"));
        this.setInfrastructureVersion(version);
        this.setPayloadVersion(version);
        
        // So we know the type of the message.
        current = root.getChildElements().get(0);
        setType(current.getLocalName());
        
        // While some intermal omissions are allowed, the header is common.
        Element header = null;
        header = current.getFirstChildElement("SIF_Header", ns); 
        
        // So we know the message's identifier.
        setMessageId(new SIFRefId(header.getFirstChildElement("SIF_MsgId", ns).
                getValue()));
        
        // So we keep the message's timestamp.
        setTimestamp(header.getFirstChildElement("SIF_Timestamp", ns).
                getValue());
        
        // So we keep the security requirements for delivery by the ZIS.
        current = header.getFirstChildElement("SIF_Security", ns);
        if(null != current) {
            setSecurity(true);
            current = current.getFirstChildElement("SIF_SecureChannel", ns);
            setAuthenticationLevel(Integer.parseInt(
                    current.getFirstChildElement(
                    "SIF_AuthenticationLevel", ns).getValue()));
            this.setEncryptionLevel(Integer.parseInt(
                    current.getFirstChildElement(
                    "SIF_EncryptionLevel", ns).getValue()));
        }
        
        // So we know where the message originated.
        setSourceId(header.getFirstChildElement("SIF_SourceId", ns).getValue());
        
        // So we know the intended recipient (if specified).
        current = header.getFirstChildElement("SIF_DestinationId", ns);
        if(null != current) {
            setDestinationId(current.getValue());
        }
        
        // So we know the contexts this message applies to.
        current = header.getFirstChildElement("SIF_Contexts", ns);
        if(null != current) {
            Elements collection = current.getChildElements();
            for(int i = 0; i < collection.size(); i++) {
                current = collection.get(i);
                addContext(current.getValue());
            }
        }
        
        // So we can pickup addition components one level up.
        //Element wrapper = ((Element)header.getParent());
        Element wrapper = root.getChildElements().get(0); 
        
        // Additional mandatory components for SIF_Ack.
        current = wrapper.getFirstChildElement("SIF_OriginalSourceId", ns); 
        if(null != current) {
            setSourcedTo(current.getValue());
        }
        current = wrapper.getFirstChildElement("SIF_OriginalMsgId", ns); 
        if(null != current) {
            setRelatesTo(current.getValue());
        }
        
        // To Do:  Add other message specific understood payloads here!
        
        if(! "Register".equals(type)) {
            Elements children = wrapper.getChildElements();
            for(int i = 0; i < children.size(); i++) {
                Element child = children.get(i);
                if("SIF_Version".equals(child.getLocalName())) {
                    addResponseVersion(new SIFVersion(child.getValue()));
                }
            }
            current = wrapper.getFirstChildElement("SIF_MaxBufferSize", ns);
            if(null != current) {
                setMaxBufferSize(Long.parseLong(current.getValue()));
            }
        }

        // Additional mandatory components for SIF_Response.
        // Supports SIF_ServiceInput, SIF_ServiceOutput and SIF_ServiceNotify.
        current = wrapper.getFirstChildElement("SIF_RequestMsgId", ns);
        if(null != current) {
            setRelatesTo(current.getValue());
        }
        // To Do:  This is more complicated than this!
        current = wrapper.getFirstChildElement("SIF_ServiceMsgId", ns);
        if(null != current) {
            setRelatesTo(current.getValue());
        }
        current = wrapper.getFirstChildElement("SIF_PacketNumber", ns);
        if(null != current) {
            setPacketNumber(Long.parseLong(current.getValue()));
        }
        current = wrapper.getFirstChildElement("SIF_MorePackets", ns);
        if(null != current) {
            setMorePackets(current.getValue());
        }
        current = wrapper.getFirstChildElement("SIF_Service", ns);
        if(null != current) {
            setService(current.getValue());
        }
        current = wrapper.getFirstChildElement("SIF_Operation", ns);
        if(null != current) {
            setOperation(current.getValue());
        }
                        
        /* PAYLOADS!!! */   
        
        // So we can treat thingss that are not part of the header as payload.
        final List<String> headerElements = 
                Collections.unmodifiableList(Arrays.asList(
            // Common
            "SIF_Header",                       // Header (duh)
            // SIF_Response
            "SIF_RequestMsgId",                 // Header:  wsa:Relates
            "SIF_PacketNumber",                 // Header
            "SIF_MorePackets",                  // Header
            // SIF_Ack
            "SIF_OriginalSourceId",             // Header
            "SIF_OriginalMsgId",                // Header:  wsa:Relates
            // SIF_Register
            "SIF_Version",                      // Header:  ResponseVersionType + Body of Register
            "SIF_MaxBufferSize",                // Header + Body of Register
            // Zone Services
            "SIF_Service",                      // No CURRENT equivalent.
            "SIF_Operation",                    // Header:  Operation
            "SIF_ServiceMsgId"));               // Header:  wsa:Relates
        
        // So we can treat elements of register that are otherwise part of the
        // header properly.
        final List<String> registerElements = 
                Collections.unmodifiableList(Arrays.asList(
            "SIF_Version",
            "SIF_MaxBufferSize"));
        
        // So we keep the messages payload.
        Element body = new Element("SIF_Body", ns);
        Elements peers = wrapper.getChildElements();
        for(int i = 0; i < peers.size(); i++) {
            current = peers.get(i);
            if((! headerElements.contains(current.getLocalName())) ||
                    ("Register".equals(type) && 
                    registerElements.contains(current.getLocalName()))) {
                current.detach();
                body.appendChild(current);
            }
        }
                
        // So we have the type's extension (when present).
        String extension = "";
        if("Request".equals(type) || "Response".equals(type) 
                || "ServiceOutput".equals(type)) {
            current = body.getChildElements().get(body.getChildCount() - 1);
            extension = current.getLocalName();
        }
        else if("SystemControl".equals(type)) {
            current = body.getChildElements().get(0);
            current = current.getChildElements().get(0);
            extension = current.getLocalName();
        }
        
        // So we have the corrisponding SOAP action.
        String wsaAction = SIFActionUtil.TypeToAction(getType(), extension);
        setAction(wsaAction);     
        
        // So we get other needed information from the body.
        if("Event".equals(type)) {
            current = (Element)body.getChild(0);
            current = current.getFirstChildElement("SIF_EventObject", ns);
            setEventAction(current.getAttributeValue("Action"));
        }
        
        setPayload(body);
    }

    /**
     * Takes a message and prepares this one for use in responding to the first.
     * 
     * Note:  The resulting message is NOT to include a payload!
     * 
     * Minimally must call ... in order to have a complete and valid message.
     *  setSourceId, setInfrastructureVersion, setMessageId, and setPayload
     * 
     * @param base 
     * @since 3.0
     */
    public void ack(SIF2MessageXML base) {
        this.setSourcedTo(base.getSourceId());
        this.setRelatesTo(base.getMessageId());
        this.setType("Ack");
        
        // May (probably not) use checkComplete to test results here.
    }    
    
    /**
     * Checks that the member variables contain information to form a complete
     * SIF message.
     * 
     * Note:  This should be the only place "missing" is set (or modified).
     * 
     * Usages: received messages, converted messages, created messages. 
     * 
     * @return boolean (true == complete, false ==  incomplete)
     * @since 3.0
     */
    public boolean checkComplete() {
        missing = new ArrayList<String>();
        
        return true;
    }
    
    /**
     * 
     * @return List<String> (Human readable list of missing components.)
     */
    public List<String> getMissing() {
        return missing;
    }
    
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
    public String toString() {
        // Common printing settings.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Serializer serializer;
        try {
            serializer = new Serializer(out, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return "<SerializationError>" + SIF2MessageXML.class.getName() + ": "
                    + ex + "</SerializationError>";
        }
        serializer.setIndent(2);
        
        
        // So we produce XML for the desired transport.
        if("HTTP".equals(getTransport())) {
            try {
                serializer.write(getHTTP());
            } catch (IOException ex) {
                return "<SerializationError>" + SIF2MessageXML.class.getName() + 
                    ": " + ex + "</SerializationError>";
            }
        }
        else {
            try {
                serializer.write(getSOAP());
            } catch (IOException ex) {
                return "<SerializationError>" + SIF2MessageXML.class.getName() +
                    ": " + ex + "</SerializationError>";
            }
        }
        
        // So we have the results in UTF-8 String form.
        try {
            serializer.flush();
        } catch (IOException ex) {
            return "<SerializationError>" + SIF2MessageXML.class.getName() + ": "
                    + ex + "</SerializationError>";
        }
        String output = "<SerializationError/>";
        try {
            output = out.toString("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return "<SerializationError>" + SIF2MessageXML.class.getName() + ": "
                    + ex + "</SerializationError>";
        }
        
        // So we drop the XML version and encoding declarations.
        output = output.substring(output.indexOf(serializer.getLineSeparator())
                + 1);
        
        return output;
    }
    
    /**
     * Allows inspection and serialization of the message in SOAP form.
     * 
     * @return Document (XOM)
     * @since 3.0
     */
    private Document getSOAP() {        
        // So we include the generic SOAP envelope.
        String soap = getNamespace("soap");
        String wsa = getNamespace("wsa");
        
        Element root = new Element("soap:Envelope", soap);
        root.addNamespaceDeclaration("wsa", wsa);
        
        Element soapHeader = new Element("soap:Header", soap);
        root.appendChild(soapHeader);
        
        // So we include WS-Addressng as defined by the SIF Association.
        String anonymous = "http://www.w3.org/2005/08/addressing/anonymous";
        
        Element current = new Element("wsa:To", wsa);
        String value = getTo().toString();
        if((! value.isEmpty()) && (0 != anonymous.compareToIgnoreCase(value))) {
            current.appendChild(value);
            soapHeader.appendChild(current);
        }
        
        value = getFrom().toString();
        if((! value.isEmpty()) && (0 != anonymous.compareToIgnoreCase(value))) {
            current = new Element("wsa:From", wsa);
            current.appendChild(value);
            soapHeader.appendChild(current);
        }
        
        SIFRefId id = getMessageId();
        if(null != id) {
            current = new Element("wsa:MessageID", wsa);
            current.appendChild(id.toString());
            soapHeader.appendChild(current);
        }
        
        // To Do:  If the action is missing should we throw an exception?
        current = new Element("wsa:Action", wsa);
        current.appendChild(getAction());
        soapHeader.appendChild(current);
        
        if("Ack".equals(getType()) || 
                "Response".equals(getType()) || 
                "ServiceOutput".equals(getType())) {
            current = new Element("wsa:RelatesTo", wsa);
            // WS-Addressing 3.1
            SIFRefId relatesId = getRelatesTo();
            if(null == relatesId) {
                // So we don't add an empty optional element.
            }
            else {
                current.appendChild(relatesId.toString());
            }
            Attribute rt = new Attribute("RelationshipType", 
                    "http://www.w3.org/2005/08/addressing/reply");
            current.addAttribute(rt);
            soapHeader.appendChild(current);
        }
        
        // So we have all the parts of the SIFHeader.
        String t = getNamespace("t");
        
        Element header = new Element("SIFHeader", t);
        soapHeader.appendChild(header);
        
        current = new Element("Timestamp", t);
        current.appendChild(getTimestamp().toString());
        header.appendChild(current);
        
        current = new Element("ZoneId", t);
        current.appendChild(getZoneId());
        header.appendChild(current);
        
        current = new Element("InfrastructureVersion", t);
        current.appendChild(getInfrastructureVersion().toString());
        header.appendChild(current);
        
        String dataModel = getNamespace();
        if(! dataModel.isEmpty()) {
            current = new Element("DataModel", t);
            current.appendChild(dataModel);
            header.appendChild(current);
        }
        
        String dataVersion = getPayloadVersion().toString();
        if(! dataVersion.isEmpty()) {
            current = new Element("DataModelVersion", t);
            current.appendChild(dataVersion);
            header.appendChild(current);
        }
        
        if(isSecurity()) {
            current = new Element("Security", t);
            header.appendChild(current);
            
            Element channel = new Element("SecureChannel", t);
            current.appendChild(channel);
        
            current = new Element("AuthenticationLevel", t);
            current.appendChild(Integer.toString(getAuthenticationLevel()));
            channel.appendChild(current);
            
            current = new Element("EncryptionLevel", t);
            current.appendChild(Integer.toString(getEncryptionLevel()));
            channel.appendChild(current);
        }
        
        current = new Element("SourceId", t);
        current.appendChild(getSourceId());
        header.appendChild(current);
        
        String destination = this.getDestinationId();
        if(! destination.isEmpty()) {
            current = new Element("DestinationId", t);
            current.appendChild(destination);
            header.appendChild(current);
        }
        
        if("Response".equals(getType()) || 
                "ServiceInput".equals(getType()) ||
                "ServiceOutput".equals(getType()) ||
                "ServiceNotify".equals(getType())) {
            Element packetData = new Element("PacketData", t);
            header.appendChild(packetData);
            
            current = new Element("PacketNumber", t);
            current.appendChild(Long.toString(getPacketNumber()));
            packetData.appendChild(current);
        
            current = new Element("MorePackets", t);
            current.appendChild(morePackets ? "Yes" : "No");
            packetData.appendChild(current);
        }
        
        if(! contexts.isEmpty()) {
            Element collection = new Element("Contexts", t);
            header.appendChild(collection);
            
            for(String context : contexts) {
                current = new Element("Context", t);
                current.appendChild(context);
                collection.appendChild(current);
            }
        }
        
        String topic = getTopicName();
        if(! topic.isEmpty()) {
            current = new Element("TopicName", t);
            current.appendChild(topic);
            header.appendChild(current);
        }
        
        String event = getEventAction();
        if(! event.isEmpty()) {
            current = new Element("EventAction", t);
            current.appendChild(event);
            header.appendChild(current);
        }
        
        Long buffer = getMaxBufferSize();
        if(0 < buffer) {
            current = new Element("MaxBufferSize", t);
            current.appendChild(buffer.toString());
            header.appendChild(current);
        }
        
        for(SIFVersion version : responseVersions) {
            current = new Element("ResponseVersion", t);
            current.appendChild(version.toString());
            header.appendChild(current);
        }
        
        if("ServiceInput".equals(getType()) ||
                "ServiceOutput".equals(getType()) ||
                "ServiceNotify".equals(getType())) {
            Element service = new Element("ZoneServiceData", t);
            header.appendChild(service);
        
            if(0 < operation.length()) {
                current = new Element("Operation", t);
                current.appendChild(getOperation());
                service.appendChild(current);
            }
            
            current = new Element("ServiceMsgId", t);
            // Only an empty element is allowed by the XSD/WSDL.
            SIFRefId relatesId = getRelatesTo();
            if(null != relatesId) {
                current.appendChild(relatesId.toString());
            }
            service.appendChild(current);
        }
        
        /* PAYLOADS!!! */
        
        // So we have all the parts of the payload.
        String m = getNamespace("m");
        
        // So we do not modify the parsed in payload.
        Element body = new Element(getPayload().getRootElement());
        
        // So we know what types to convert.
        List<String> converts = Collections.unmodifiableList(Arrays.asList(
        "Ack", "Register", "Provide", "Provision", "Query", "ExtendedQuery", 
        "Response", "Subscribe", "SystemControl", "Unprovide", "Unsubscribe"));
        
        /* So the payload reflects the transport. */
        
        // So we know where to stop our conversions.
        List<Element> limits = new ArrayList<Element>();
        
        // Inner data.
        if(converts.contains(type)) {
            if("http://www.sifinfo.org/infrastructure/2.x".equalsIgnoreCase(
                    body.getNamespaceURI())) {
                SIFXOMUtil.lStrip(body, "SIF_", limits);
                SIFXOMUtil.renamespace(body, m, limits);
            }
            
            // So we drop a SystemControl tag not used in the SOAP transport.
            Element extra = body.getFirstChildElement("SystemControlData", m);
            if(null != extra) {
                Elements children = extra.getChildElements();
                body.removeChildren();
                for(int i = 0; i < children.size(); i++) {
                    Element child = children.get(i);
                    child.detach();
                    body.appendChild(child);
                }
            }
            
            // So we add the extra Register tag for this transport.
            if(type.equals("Register") &&
                    null == body.getFirstChildElement("Register", m)) {
                body.setLocalName("Register");
                body.setNamespaceURI(m);
                Element register = new Element("Body", soap);
                register.appendChild(body);
                body = register;
            }
        }
        
        // So we have the correct payload wrapper.
        body.setLocalName("Body");
        body.setNamespaceURI(soap);
        
        // So we include the (possibly converted) payload.
        root.appendChild(body);
        
        return new Document(root);
    }
    
    /**
     * Allows inspection and serialization of the message in HTTP form.
     * 
     * @return Document (XOM)
     * @since 3.0
     */
    private Document getHTTP() {
        // So we only retrieve the needed namespace once.
        String ns = getNamespace();
        
        // So we can add xsi:nil="true" whenever we need it.
        String xsi = "http://www.w3.org/2001/XMLSchema-instance";
        Attribute nil = new Attribute("xsi:nil", xsi, "true");
                
        // So we have all common componenets of a message.
        Element root = new Element("SIF_Message", ns);
        root.addNamespaceDeclaration("xsi", xsi);
        Attribute version = new Attribute("Version",
                infrastructureVersion.toString());
        root.addAttribute(version);
        
        // So subtypes that are now types in WS are treated as the latter.
        Element wrapper = null;
        if("Query".equals(type) || "ExtendedQuery".equals(type)) {
            wrapper = new Element("SIF_Request", ns);
        }
        else {
            wrapper = new Element(this.getType(), ns);
        }
        root.appendChild(wrapper);
        
        Element header = new Element("SIF_Header", ns);
        wrapper.appendChild(header);
        
        Element current = new Element("SIF_MsgId", ns);
        current.appendChild(getMessageId().toString());
        header.appendChild(current);
        
        current = new Element("SIF_Timestamp", ns);
        current.appendChild(getTimestamp().toString());
        header.appendChild(current);
                
        // So we have all the needed infrequent components.
        if(isSecurity()) {
            current = new Element("SIF_Security", ns);
            header.appendChild(current);
            
            Element channel = new Element("SIF_SecureChannel", ns);
            current.appendChild(channel);
        
            current = new Element("SIF_AuthenticationLevel", ns);
            current.appendChild(Integer.toString(getAuthenticationLevel()));
            channel.appendChild(current);
            
            current = new Element("SIF_EncryptionLevel", ns);
            current.appendChild(Integer.toString(getEncryptionLevel()));
            channel.appendChild(current);
        }
        
        // Common component include here to staisfy sequence requirement.
        current = new Element("SIF_SourceId", ns);
        current.appendChild(getSourceId());
        header.appendChild(current);

        
        if(! getDestinationId().isEmpty()) {
            current = new Element("SIF_DestinationId", ns);
            current.appendChild(getDestinationId());
            header.appendChild(current);
        }
        
        if(! contexts.isEmpty()) {
            Element collection = new Element("SIF_Contexts", ns);
            header.appendChild(collection);
            
            for(String context : contexts) {
                current = new Element("SIF_Context", ns);
                current.appendChild(context);
                collection.appendChild(current);
            }
        }
        
        if("SIF_Ack".equals(getType())) {
            current = new Element("SIF_OriginalSourceId", ns);
            // 4.2.2.1 If couldn't parse message, use empty with xsi:nil="true."
            String sourcedTo = getSourcedTo();
            if(null == sourcedTo || sourcedTo.isEmpty()) {
                current.addAttribute((Attribute)nil.copy());
            }
            else {
                current.appendChild(sourcedTo);
            }
            wrapper.appendChild(current);
            current = new Element("SIF_OriginalMsgId", ns);
            SIFRefId relatesId = getRelatesTo();
            if(null == relatesId) {
                current.addAttribute((Attribute)nil.copy());
            }
            else {
                current.appendChild(relatesId.toString());
            }
            wrapper.appendChild(current);
        }
        if("SIF_Response".equals(getType())) {
            current = new Element("SIF_RequestMsgId", ns);
            // 4.2.2.1 If couldn't parse message, use empty with xsi:nil="true."
            SIFRefId relatesId = getRelatesTo();
            if(null == relatesId) {
                current.addAttribute((Attribute)nil.copy());
            }
            else {
                current.appendChild(relatesId.toString());
            }
            wrapper.appendChild(current);
        }
        if("SIF_ServiceInput".equals(getType()) ||
                "SIF_ServiceNotify".equals(getType())) {
            current = new Element("SIF_Service", ns);
            current.appendChild(getService());
            wrapper.appendChild(current);
            current = new Element("SIF_Operation", ns);
            current.appendChild(getOperation());
            wrapper.appendChild(current);
        }
        if("SIF_ServiceInput".equals(getType()) ||
                "SIF_ServiceOutput".equals(getType()) ||
                "SIF_ServiceNotify".equals(getType())) {
            current = new Element("SIF_ServiceMsgId", ns);
            // 4.2.2.1 If couldn't parse message, use empty with xsi:nil="true."
            SIFRefId relatesId = getRelatesTo();
            if(null == relatesId) {
                current.addAttribute((Attribute)nil.copy());
            }
            else {
                current.appendChild(relatesId.toString());
            }
            wrapper.appendChild(current);
        }
        if(! responseVersions.isEmpty()) {
            for(SIFVersion responseVersion : getResponseVersions()) {
                current = new Element("SIF_Version", ns);
                current.appendChild(responseVersion.toString());
                wrapper.appendChild(current);
            }
        }
        long bufferSize = getMaxBufferSize();
        if(0 != bufferSize) {
            current = new Element("SIF_MaxBufferSize", ns);
            current.appendChild(getMaxBufferSize().toString());
            wrapper.appendChild(current);
        }
        if("SIF_Response".equals(getType()) || 
                "SIF_ServiceInput".equals(getType()) ||
                "SIF_ServiceOutput".equals(getType()) ||
                "SIF_ServiceNotify".equals(getType())) {
            current = new Element("SIF_PacketNumber", ns);
            current.appendChild(Long.toString(getPacketNumber()));
            wrapper.appendChild(current);
            
            current = new Element("SIF_MorePackets", ns);
            if(isMorePackets()) {
                current.appendChild("Yes");
            }
            else {
                current.appendChild("No");
            }
            wrapper.appendChild(current);
        }
        
        /* PAYLOADS!!! */
                
        // So we do not modify the parsed in payload.
        Element body = new Element(getPayload().getRootElement());
        
        // So we know what types to convert.
        List<String> converts = Collections.unmodifiableList(Arrays.asList(
        "Ack", "Register", "Provide", "Provision", "Query", "ExtendedQuery", 
        "Response", "Subscribe", "SystemControl", "Unprovide", "Unsubscribe"));
        
        // So we know where to stop our conversions.
        List<Element> limits = new ArrayList<Element>();
        if("Response".equals(type)) {
            limits.add(new Element("Rows", 
                    "http://www.sifassociation.org/message/soap/2.x"));
        }
        
        // So the payload reflects the transport.
        if(converts.contains(type)) {
            if(! body.getLocalName().startsWith("SIF_")) {
                SIFXOMUtil.prepend(body, "SIF_", limits);
            }            
            if(! ns.equalsIgnoreCase(body.getNamespaceURI())) {
                SIFXOMUtil.renamespace(body, ns, limits);
            }
            
            // So we add the extra SystemControl tag for this transport.
            if(type.equals("SystemControl") &&
                null == body.getFirstChildElement("SIF_SystemControlData", ns)) {
                Element temp = new Element("SIF_SystemControlData", ns);
                Elements children = body.getChildElements();
                for(int i = 0; i < children.size(); i++) {
                    Element child = children.get(i);
                    child.detach();
                    temp.appendChild(child);
                }
                body.removeChildren();
                body.appendChild(temp);
            }
        }
        
        
        if(type.equals("Register")) {
            // So we drop the inner body Register tag for this transport.
            if(null != body.getFirstChildElement("SIF_Register", ns)) {
                body = body.getFirstChildElement("SIF_Register", ns);
            }
            
            // So we communicate the proper Push protocol.
            Element edit = body.getFirstChildElement("SIF_Protocol", ns);
            if(null != edit) {
                Attribute pt = edit.getAttribute("Type");
                if(pt.getValue().startsWith("HTTPS")) {
                    pt.setValue("HTTPS");
                }
                else {
                    pt.setValue("HTTP");
                }
            }
            
        }
        
        // So we include the (possibly converted) payload.
        Elements children = body.getChildElements();
        for(int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            child.detach();
            wrapper.appendChild(child);
        }
        
        return new Document(root);
    }

    // To Do: Change back to private!!!
    private Document getPayload() {
        return payload;
    }

    /**
     * So our payload is protected from external edits.
     * 
     * @return A copy of the messages payload. 
     */
    public Document getPayloadCopy() {
        return new Document(payload);
    }

    /**
     * So we can get the current form of the message as a XOM Document.
     * 
     * @return The built-up message.
     */
    public Element getRootElement() {
        if("HTTP".equals(this.getTransport())) {
            return this.getHTTP().getRootElement();
        }
        else {
            return this.getSOAP().getRootElement();
        }
    }
    
    /**
     * Sets the primary payload based on the XML of the passed object.
     * 
     * Note:  If XOM Documents or Element are passed they are not parsed again.
     * 
     * Note:  If the passed object is not a XOM object, toString() must return 
     *        the desired XML payload.
     * 
     * @param o  Object that represents the desired payload.
     * @throws NullPointerException if o is null.
     * @throws IllegalArgumentException if o is not an object.
     * @throws ParsingException (XOM)
     * @throws IOException
     */
    public void setPayload(Object o) throws ParsingException, IOException {
        // So we validate the supplied version.
        if(null == o) {
            throw new NullPointerException("An object was expected, null was "
                    + "passed.");
        }
        else if(!(o instanceof Object)){
            throw new IllegalArgumentException("Payload must be an object!");
        }
        
        // So we do not convert payloads already in the desired form or close.
        if(o instanceof Document) {
            payload = (Document)o;
        }
        else if(o instanceof Element) {
            ((Element)o).detach();
            payload = new Document((Element)o);
        }
        // So we have the needed XOM document.
        else if(o instanceof String) {
            // So we can accept empty strings.
            if(0 == ((String)o).length()) {
                o = "<payload />";
            }
            Builder parser = new Builder();
            Document doc = parser.build((String)o, null);
            payload = doc;
        }
        else {
            throw new IllegalArgumentException(o.toString() + " is of a type "
                    + "not expected for a SIFMessage payload.");
        }
    }
    
    /**
     * For checking if the payload matches an expectation.
     * 
     * Note:  Classic SIF message payloads must include an outer SIF_Body tag.
     * 
     * @param xPath  
     * @return boolean  True if the XPath is found, else false.
     * @since 3.0
     */
    @Override
    public boolean checkPayload(String xPath) {
        if (0 < payload.query(xPath, namespaces).size()) {return true;}
        
        return false;
    }
    
    /**
     * So we can retrieve the message as it was before we parsed it.
     * 
     * @return String  The original message.
     * @since 3.0
     */
    public String getOriginal() {
        return original;
    }
    
    /**
     * Set the desired transport for XML serialization.
     * 
     * @param indicator  HTTP or SOAP (case insensitive).
     * @throws NullPointerException if indicator is null.
     * @throws IllegalArgumentException if the transport is not expected.
     * @since 3.0
     */
    @Override
    public void setTransport(String indicator) {
        // So we validate the supplied transport.
        if(null == indicator) {
            throw new NullPointerException("A String was expected, null was "
                    + "passed.");
        }
        else if("HTTP".equalsIgnoreCase(indicator)) {
            transport = "HTTP";
        }
        else if("SOAP".equalsIgnoreCase(indicator)) {
            transport = "SOAP";
        }
        else {
            throw new IllegalArgumentException(indicator + " given HTTP or SOAP"
                    + " expected.");
        }
    }
    
    /**
     * Retrieves the current desired transport.
     * 
     * Note: Must reflect the message after parsing, but may be changed.
     * 
     * @return String  HTTP or SOAP (capitalized).
     * @since 3.0
     */
    @Override
    public String getTransport() {
        return transport;
    }

    /**
     * Gets the root namespace of the payload.
     * 
     * @return  String  The namespace of the payload regardless of scope.
     * @since 3.0
     */
    public String getNamespace() {
        return namespaces.lookup("sif");
    }

    /**
     * Sets the root namespace of the payload (used in the SIF header).
     * 
     * Using the "sif" prefix this xpath may be used by member XPath functions.
     * 
     * @param namespace  The namespace of the payload regardless of scope.
     * @since 3.0
     */
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
    public void addNamespace(String prefix, String namespace) {
        namespaces.addNamespace(prefix, namespace);
    }

    /**
     * So we can honor the contexts in the message elsewhere.
     * 
     * @return A shallow copy of the messages context.
     */
    public List<String> getContexts() {
        return new ArrayList(contexts);
    }
    
    /**
     * Add a context to the message.
     * 
     * @param context
     * @since 3.0
     */
    public void addContext(String context) {
        contexts.add(context);
    }
    
    /**
     * Removes a known context from the message.
     * 
     * @param context
     * @return 
     * @since 3.0
     */
    public boolean removeContext(String context) {
        return contexts.remove(context);
    }
    
    /**
     * Add an acceptable version to receive to the message.
     * 
     * @param responseVersion 
     * @since 3.0
     */
    public void addResponseVersion(SIFVersion responseVersion) {
        responseVersions.add(responseVersion);
    }
    
    /**
     * Remove a known acceptable version to receive from the message.
     * 
     * @param responseVersion
     * @return 
     * @since 3.0
     */
    public boolean removeContext(SIFVersion responseVersion) {
        return responseVersions.remove(responseVersion);
    }
    
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getAuthenticationLevel() {
        return authenticationLevel;
    }

    public void setAuthenticationLevel(int authenticationLevel) {
        this.authenticationLevel = authenticationLevel;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    public int getEncryptionLevel() {
        return encryptionLevel;
    }

    public void setEncryptionLevel(int encryptionLevel) {
        this.encryptionLevel = encryptionLevel;
    }

    public URL getFaultTo() {
        return faultTo;
    }

    public void setFaultTo(URL faultTo) {
        this.faultTo = faultTo;
    }

    public URL getFrom() {
        return from;
    }

    public void setFrom(URL from) {
        this.from = from;
    }

    public SIFVersion getInfrastructureVersion() {
        return infrastructureVersion;
    }

    public void setInfrastructureVersion(SIFVersion infrastructureVersion) {
        this.infrastructureVersion = infrastructureVersion;
    }

    /**
     * Retrieves the message ID (form is based on the messages transport).
     * 
     * @return SIFRefId  Based on what has been parsed or set.
     * @since 3.0
     */
    public SIFRefId getMessageId() {
        // So simple retrieval does not cause an exception.
        if(null == messageId) {
            return null;
        }
        
        // So we get the ID in the form that matches the transport.
        if("SOAP".equals(getTransport())) {
            messageId.setGeneric(true);
        }
        else {
            messageId.setGeneric(false);
        }
        
        return messageId;
    }

    public void setMessageId(SIFRefId messageId) {
        this.messageId = messageId;
    }
    
    public boolean isMorePackets() {
        return morePackets;
    }

    public void setMorePackets(boolean morePackets) {
        this.morePackets = morePackets;
    }

    /**
     * So we can commit "more packets" consistently between transports.
     * 
     * @param morePackets
     * @since 3.0
     */
    public void setMorePackets(String morePackets) {
        if("Yes".equals(morePackets)) {
            setMorePackets(true);
        }
        else {
            setMorePackets(false);
        }
    }
    
    public long getPacketNumber() {
        return packetNumber;
    }

    public void setPacketNumber(long packetNumber) {
        this.packetNumber = packetNumber;
    }

    public SIFVersion getPayloadVersion() {
        return payloadVersion;
    }

    public void setPayloadVersion(SIFVersion payloadVersion) {
        this.payloadVersion = payloadVersion;
    }
    
    /**
     * Retrieves the related ID (form is based on the messages transport).
     * 
     * @return SIFRefId  Based on what has been parsed or set.
     * @since 3.0
     */
    public SIFRefId getRelatesTo() {
        // So this ID is always direclty compatible with the classic transport.
        if(null != relatesTo) {
            relatesTo.setGeneric(false);
        }
        
        return relatesTo;
    }

    public void setRelatesTo(SIFRefId relatesTo) {
        this.relatesTo = relatesTo;
    }
    
    /**
     * So we can handle blank IDs (that couldn't be parsed).
     * 
     * @param identifier 
     * @since 3.2.1
     */
    public void setRelatesTo(String identifier) {
        if(null == identifier || identifier.isEmpty()) {
            this.relatesTo = null;
        }
        else {
            //this.setRelatesTo(new SIFRefId(identifier));
            this.relatesTo = new SIFRefId(identifier);
        }
    }    
    
    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public URL getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(URL replyTo) {
        this.replyTo = replyTo;
    }

    public boolean isSecurity() {
        return security;
    }

    public void setSecurity(boolean security) {
        this.security = security;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public XMLGregorianCalendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(XMLGregorianCalendar timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * So we can commit timestamps consistently between transports.
     * 
     * @param timestamp
     * @throws DatatypeConfigurationException
     * @since 3.0
     */
    public void setTimestamp(String timestamp) throws DatatypeConfigurationException {
        Calendar calendar = DatatypeConverter.parseDateTime(timestamp);
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(calendar.getTimeInMillis());
        setTimestamp(DatatypeFactory.newInstance().
                    newXMLGregorianCalendar(gregorianCalendar));
    }
    
    public URL getTo() {
        return to;
    }

    public void setTo(URL to) {
        this.to = to;
    }

    /**
     * Retrieves the type in the form for the current transport.
     * 
     * @return String  The type of message.
     * @since 3.0
     */
    public String getType() {
        if("HTTP".equals(getTransport())) {
            return "SIF_" + type;
        }

        return type;
    }
    
    /**
     * Sets the type of the message in SOAP form.
     * 
     * @param type 
     * @since 3.0
     */
    public void setType(String type) {
        if(type.startsWith("SIF_")) {
            type = type.substring(4);
        }
        
        List<String> options = Collections.unmodifiableList(Arrays.asList(
                "Ack", "Event", "ExtendedQuery", "Provide", "Provision", 
                "Register", "Request", "Response", "Subscribe", "SystemControl",
                "Unprovide", "Unregister", "Unsubscribe", "ServiceOutput",
                "ServiceInput", "ServiceNotify"));
        if(! options.contains(type)) {
            throw new IllegalArgumentException(type + " is not a valid message "
                    + "type.");
        }        
        
        this.type = type;
    }
    
    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public Long getMaxBufferSize() {
        return maxBufferSize;
    }

    public void setMaxBufferSize(long maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }

    // So we can loop through and make comparisons to other versions.
    public List<SIFVersion> getResponseVersions() {
        return responseVersions;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getEventAction() {
        return eventAction;
    }

    /**
     * So we only allow legal EventActions (Add, Change, or Delete).
     * 
     * @param eventAction 
     * @since 3.0
     */
    public void setEventAction(String eventAction) {
        List<String> options = Collections.unmodifiableList(Arrays.asList(
                "", "Add", "Change", "Delete"));
        if(! options.contains(eventAction)) {
            throw new IllegalArgumentException(eventAction + "is not a valid "
                    + "EventAction.");
        }
        
        this.eventAction = eventAction;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getSourcedTo() {
        return sourcedTo;
    }
    
    public void setSourcedTo(String sourcedTo) {
        this.sourcedTo = sourcedTo;
    }

    @Override
    public SIFHttpHeaders getHttpHeaders() {
        return new SIFHttpHeaders();
    }
    
}
