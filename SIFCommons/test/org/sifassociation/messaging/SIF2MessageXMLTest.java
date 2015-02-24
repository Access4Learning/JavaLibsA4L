/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.messaging;

import org.sifassociation.messaging.SIF2MessageXML;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import nu.xom.*;
import static org.junit.Assert.fail;
import org.junit.*;
import org.sifassociation.messaging.SimpleErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.sifassociation.util.SIFFileUtil;

/**
 *
 * @author jlovell
 */
public class SIF2MessageXMLTest {
    
    List<SIF2MessageXML> messages = null;
    
    public SIF2MessageXMLTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() throws MalformedURLException, ParsingException, IOException, DatatypeConfigurationException {
        // So we have all the classic messages to work with.
        String path = "resources/messages/HTTP/"; 
        Pattern filter = Pattern.compile("^.*\\.(x|X)(m|M)(l|L)$");
        String file;
        messages = new ArrayList<SIF2MessageXML>();
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles(); 
        for(int i = 0; i < listOfFiles.length; i++) {
            if(listOfFiles[i].isFile()) {
                file = listOfFiles[i].getName();
                if(filter.matcher(file).matches()) {
                    SIF2MessageXML message = new SIF2MessageXML();
                    message.parse(SIFFileUtil.readFile(path + file));
                    messages.add(message);
                }
            }
        }
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of toString method, of class SIF2MessageXML.
     */
    @Test
    public void testToString() throws MalformedURLException, ParsingException, IOException, DatatypeConfigurationException, ParserConfigurationException, SAXException {
        // http://www.edankert.com/validate.html
        
        // Classic
        Source[] classics = new Source[1];
        // SIF_Message: http://www.sifinfo.org/infrastructure/2.x
        classics[0] = new StreamSource("resources/schemas/HTTP/SIF_Message.xsd");
        Builder HTTPBuilder = getBuilder(classics);
        
        // SOAP
        Source[] services = new Source[5];
        // SOAP Envelope: http://schemas.xmlsoap.org/soap/envelope/
        services[0] = new StreamSource("resources/schemas/SOAP/envelope.xsd");
        // WS-Addressing: http://schemas.xmlsoap.org/soap/envelope/
        services[1] = new StreamSource("resources/schemas/SOAP/Addressing.xsd");
        // SIFHeaders: http://www.sifassociation.org/transport/soap/2.x
        services[2] = new StreamSource("resources/schemas/SOAP/Transport-1.xsd");
        // Non-data payloads: "http://www.sifassociation.org/message/soap/2.x"
        services[3] = new StreamSource("resources/schemas/SOAP/Messaging-1.xsd");
        // US data objects: http://www.sifinfo.org/infrastructure/2.x
        services[4] = new StreamSource("resources/schemas/HTTP/SIF_Message.xsd");
        Builder SOAPBuilder = getBuilder(services);
        
        // So we do the actual validation.
        for(SIF2MessageXML message : messages) {
            // HTTP
            System.out.println(message.toString());
            HTTPBuilder.build(message.toString(), null);
            
            // SOAP
            message.setTransport("SOAP");
            System.out.println(message.toString());
            Document SOAPMessage = SOAPBuilder.build(message.toString(), null);
            
            // So we do SOME checking on parsing SOAP messages.
            if(! ("Ack".equals(message.getType()) ||
                    "ServiceInput".equals(message.getType()) ||
                    "ServiceNotify".equals(message.getType()))) {
                SIF2MessageXML parsed = new SIF2MessageXML();
                parsed.parse(message.toString());
                parsed.setTransport("HTTP");
                System.out.println(parsed.toString());
                HTTPBuilder.build(parsed.toString(), null);
            }
        }
        
    }

    /*
     * So we can validate XML for the given namespace, against the specified schema.
     */
    static Builder getBuilder(Source[] schemaPaths) throws SAXException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        SchemaFactory schemaFactory = 
                SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        factory.setSchema(schemaFactory.newSchema(schemaPaths));
        
        SAXParser parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setErrorHandler(new SimpleErrorHandler());

        return new Builder(reader);
    }
    
}
