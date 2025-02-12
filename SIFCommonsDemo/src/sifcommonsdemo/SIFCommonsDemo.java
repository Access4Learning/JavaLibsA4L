package sifcommonsdemo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.xml.bind.JAXB;
import jakarta.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.sifassociation.XMLJSON.JacksonNative;
import org.sifassociation.messaging.SIF2MessageXML;
import org.sifassociation.messaging.SIF2Payloads;
import org.sifassociation.messaging.SIF3Payloads;
import org.sifassociation.messaging.SIFRefId;
import org.sifassociation.messaging.SIFVersion;
import org.sifassociation.querying.EXistXQueryREST;
import org.sifassociation.querying.IXQuery;
import org.sifassociation.testing.IScriptHooks;
import org.sifassociation.util.XSDErrorHandler;
import org.sifassociation.util.ResultPage;
import org.sifassociation.util.SIFEXistUtil;
import org.sifassociation.util.SIFFileUtil;
import org.sifassociation.util.SIFURLUtil;
import org.sifassociation.util.SIFXOMUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.xml.sax.XMLReader;

/**
 * Takes an infix command line comparison of SIF versions and evaluates.
 * 
 * @author jlovell
 */
public class SIFCommonsDemo {
    // Initializing the log4j system properly.
    static Logger logger = Logger.getLogger(SIFCommonsDemo.class);
    
    public static void main(String[] args) throws Exception {
        // Configure log4j.
        BasicConfigurator.configure();
        
        // Set logging levell (minimum log message to be reported).
        // To Do:  Load the level from a configuration file.
        logger.setLevel(Level.WARN);
        Logger.getLogger("org.springframework").setLevel(Level.WARN);
        Logger.getLogger("org.apache.http").setLevel(Level.WARN);
        
        /*
        
        // So we know how to turn any example into an XQuery.
        String xml = "<SIF_Query xmlns=\"http://www.sifinfo.org/infrastructure/2.x\"><SIF_QueryObject ObjectName=\"StudentPersonal\"/><SIF_Example><StudentPersonal><Name Type=\"04\"><LastName>Smith</LastName><FirstName>John</FirstName></Name></StudentPersonal></SIF_Example></SIF_Query>";
        
        Builder parser = new Builder();
        Document doc = null;
        doc = parser.build(xml, null);
        Element example = doc.getRootElement();
        
        System.out.println(SIFXOMUtil.pretty(example));
        System.out.println();
        
        System.out.println(SIFXOMUtil.query2XQuery(example));
        
        // So we know how to turn any non-example SIF_Query into an XQuery.

        xml = "<SIF_Query xmlns=\"http://www.sifinfo.org/infrastructure/2.x\"><SIF_QueryObject ObjectName=\"LibraryPatronStatus\"/><SIF_ConditionGroup Type=\"None\"><SIF_Conditions Type=\"And\"><SIF_Condition><SIF_Element>@SIF_RefObject</SIF_Element><SIF_Operator>EQ</SIF_Operator><SIF_Value>StaffPersonal</SIF_Value></SIF_Condition><SIF_Condition><SIF_Element>@SIF_RefId</SIF_Element><SIF_Operator>EQ</SIF_Operator><SIF_Value>D3E34B359D75101A8C3D00AA001A1652</SIF_Value></SIF_Condition></SIF_Conditions></SIF_ConditionGroup></SIF_Query>";

        doc = parser.build(xml, null);
        Element query = doc.getRootElement();
        
        System.out.println(SIFXOMUtil.pretty(query));
        System.out.println();

        System.out.println(SIFXOMUtil.query2XQuery(query));
        
        //*/
        /*
        
        // So we know how to only include the desired results (green list).
        String xml = "<user refId=\"fc7dd9d6-80ce-11e8-adc0-fa7ae01bbebc\" xmlns=\"http://www.sifinfo.org/infrastructure/2.x\"><name login=\"jsmith\"><first>John</first><last>Smith</last></name></user>";
        
        Builder parser = new Builder();
        Document doc = parser.build(xml, null);
        Element user = doc.getRootElement();
        
        System.out.println(SIFXOMUtil.pretty(user));
        
        List<String> xpaths = new ArrayList();
        xpaths.add("/user/name/last");
        xpaths.add("/user/name/@login");
        
        for(String xpath : xpaths) {
            System.out.println(xpath);
        }
        
        SIFXOMUtil.greenList(user, xpaths);
        System.out.println(SIFXOMUtil.pretty(user));
        
        //*/
        /*
        
        // So we know how to turn any SIF_ExtendedQuery into an XQuery.
        String xml = "<SIF_ExtendedQuery xmlns=\"http://www.sifinfo.org/infrastructure/2.x\"><SIF_Select Distinct=\"false\" RowCount=\"All\"><SIF_Element ObjectName=\"StudentPersonal\"/></SIF_Select><SIF_From ObjectName=\"StudentPersonal\"/></SIF_ExtendedQuery>";
        
        Builder parser = new Builder();
        Document doc = parser.build(xml, null);
        Element extendedQuery = doc.getRootElement();
        
        System.out.println(SIFXOMUtil.pretty(extendedQuery));
        System.out.println();
        
        System.out.println(SIFXOMUtil.extendedQuery2XQuery(extendedQuery));
        
        // Extended with a join.
        xml = "<SIF_ExtendedQuery xmlns=\"http://www.sifassociation.org/datamodel/na/3.3\">\n" +
              "  <SIF_Select Distinct=\"false\" RowCount=\"All\">\n" +
              "    <SIF_Element ObjectName=\"xCourse\"></SIF_Element>\n" +
              "    <SIF_Element ObjectName=\"xCourse\">*</SIF_Element>\n" +
              "    <SIF_Element ObjectName=\"xRoster\">@refId</SIF_Element>\n" +
              "    <SIF_Element ObjectName=\"xRoster\">schoolSectionId</SIF_Element>\n" +
              "  </SIF_Select>\n" +
              "  <SIF_From ObjectName=\"xCourse\">\n" +
              "    <SIF_Join Type=\"FullOuter\">\n" +
              "      <SIF_JoinOn>\n" +
              "        <SIF_LeftElement ObjectName=\"xCourse\">@refId</SIF_LeftElement>\n" +
              "        <SIF_RightElement ObjectName=\"xRoster\">courseRefId</SIF_RightElement>\n" +
              "      </SIF_JoinOn>\n" +
              "    </SIF_Join>\n" +
              "  </SIF_From>\n" +
              "  <SIF_Where>\n" +
              "    <SIF_ConditionGroup Type=\"None\">\n" +
              "      <SIF_Conditions Type=\"And\">\n" +
              "        <SIF_Condition>\n" +
              "          <SIF_Element ObjectName=\"xCourse\">applicableEducationLevels/applicableEducationLevel</SIF_Element>\n" +
              "          <SIF_Operator>EQ</SIF_Operator>\n" +
              "          <SIF_Value>07</SIF_Value>\n" +
              "        </SIF_Condition>\n" +
              "      </SIF_Conditions>\n" +
              "    </SIF_ConditionGroup>\n" +
              "  </SIF_Where>\n" +
              "<SIF_OrderBy>" +
              "<SIF_Element ObjectName=\"xRoster\" Ordering=\"Descending\">schoolSectionId</SIF_Element>" +
              "</SIF_OrderBy>" +
              "</SIF_ExtendedQuery>";
        
        doc = parser.build(xml, null);
        extendedQuery = doc.getRootElement();

        System.out.println(SIFXOMUtil.pretty(extendedQuery));
        System.out.println();
        
        System.out.println(SIFXOMUtil.extendedQuery2XQuery(extendedQuery));
               
        // So we know how to turn data from an extended query into SIF_ExtendedQueryResults.
        // Note:  In production "RESULTS" root tag will have to be added to use 
        //        the code here unchanged.
        String xmlResults = SIFFileUtil.readFile("resources/examples/joined.xml");
        //System.out.println(xml);  // Debug
        
        doc = parser.build(xmlResults, null);
        Element resultsJoined = doc.getRootElement();
        
        System.out.println(SIFXOMUtil.pretty(resultsJoined));  // Debug
        
        System.out.println(SIFXOMUtil.pretty(SIFXOMUtil.resultsJoined2extendedResults(extendedQuery, resultsJoined)));
        
        //*/

        /*
        
        // So we create clean SIF_Response messages.
        String ns = "http://www.sifinfo.org/infrastructure/2.x";
        SIF2MessageXML response = null;
        response = new SIF2MessageXML();
        if(null != response) {
            response.setType("Response");
            response.setTransport("HTTP");
            String hostname = "192.168.4.84";
            String sequence = "127";
            SIFRefId refId = null;
            refId = new SIFRefId(hostname, Integer.parseInt(sequence));
            if(null != refId) {
                response.setMessageId(refId.toString());
            }
            response.setRelatesTo("43B9E0A001651000007F0C4DE9CBABBA");
            response.setMorePackets("Yes");
            Element payload = new Element("SIF_Response", ns);
            payload.appendChild(new Element("SIF_ObjectData", ns));
            response.setPayload(payload);
            System.out.println(response.toString());
            System.out.println(response.toString().getBytes("UTF-8").length);
        }

        //*/
        
        /*
        // So we know how to create SIF 3 payloads.
        System.out.println(SIF3Payloads.createEnvironment(
                null,
                "BASIC", 
                null, 
                "Harness 0", 
                "testSuiteConsumer", 
                new SIFVersion("3.0.2"), 
                "http://www.sifassociation.org/datamodel/na/3.1", 
                "SIF Association", 
                "SIF Test Harness", 
                null));
        
        System.out.println(SIF3Payloads.createProvisionRequest(
                "environment-global", 
                "k12Students", 
                "NON-DEFAULT",
                "UTILITY", 
                false));
        
        System.out.println(SIF3Payloads.createQueue(
                "LONG", 
                "Attendance", 
                null, 
                "1", 
                new SIFVersion("3.5")));
        
        System.out.println(SIF3Payloads.createSubscription(
                "CurrentZone", 
                "DEFAULT", 
                "OBJECT", 
                "k12Student", 
                "1103f2c2-0145-1000-0001-14109fdcaf83", 
                new SIFVersion("4.8")));
        
        System.out.println(SIF3Payloads.deleteMultiple(
                "11086b1c-0145-1000-0008-14109fdcaf83 11086b1d-0145-1000-0009-14109fdcaf83 11086b1e-0145-1000-000a-14109fdcaf83",
                new SIFVersion("2.8r1")));
        
        System.out.println(SIFXOMUtil.pretty(SIF3Payloads.createProviderEntry(
                "11086b1f-0145-1000-000a-14109fdcaf83",
                "OBJECT",
                "k12Students",
                "DEFAULT",
                "funZone",
                "testSuiteAdaptor",
                false,
                true,
                100,
                true,
                "https://some.awsome.com/URL/")));
        
        System.out.println(SIF3Payloads.createProviderRequest(
                "11086b1f-0145-1000-000a-14109fdcaf83",
                "OBJECT",
                "k12Students",
                "DEFAULT",
                "funZone",
                "testSuiteAdaptor",
                false,
                true,
                100,
                true,
                "https://some.awsome.com/URL/"));        
        //*/
        
        /*
        // So we know what collection we are working with.
        SIFEXistUtil rest = new SIFEXistUtil("http://localhost:8080/exist/rest/db/3.0", "admin", "jfdm8th10");
        
        // So we know how to do a partial update.
        Builder parser = new Builder();
        Document doc = null;        
        
        String partial = SIFFileUtil.readFile("../../Downloads/Diff/student_part.xml");
        rest.updateResource("/student4.xml", partial);
        //*/
        
        /*
        // So we know how to efficiently grab a page of XQuery results.
        ResultPage page = rest.getXQueryPage("", "declare default element namespace \"http://www.sifassociation.org/datamodel/us/3.0\";\n/student", 1, 10);
        for(int i = 0; i < page.getResults().size(); i++) {
            System.out.println(SIFXOMUtil.pretty(page.getResults().get(i)));
        }
        System.out.println(page.getHits());
        
        
        // So we see how to download an entire collection.
        ResultPage collection = rest.readCollection("");
        for(int i = 0; i < collection.getResults().size(); i++) {
            String name = collection.getResults().get(i).getAttribute("name").getValue();
            System.out.println(name);
            String resource = rest.readResource("/" + name);
            System.out.println(resource);
            SIFFileUtil.writeFile("../../Downloads/Backup/" + name, resource);
        }        
        System.out.println(collection.getHits());
        
        
        // So we see how to delete an entire collection's contents.
        for(int i = 0; i < collection.getResults().size(); i++) {
            String name = collection.getResults().get(i).getAttribute("name").getValue();
            System.out.println(name);
            rest.deleteResource("/" + name);
        }
        System.out.println(collection.getHits());
        
        
        // So we see how to restore/copy an entire collection's contents.
        for(int i = 0; i < collection.getResults().size(); i++) {
            String name = collection.getResults().get(i).getAttribute("name").getValue();
            System.out.println(name);
            String content = SIFFileUtil.readFile("../../Downloads/Backup/" + name);
            rest.createResource("/" + name, content);
        }
        System.out.println(collection.getHits());
        //*/

        
        /*
        IXQuery eXist = new EXistXQueryREST("http://localhost:8080/exist/rest/db/3.0");
        eXist.run("declare default element namespace \"http://www.sifassociation.org/datamodel/us/3.0\";\n/student");        
        while(eXist.hasNext()) {
            System.out.println(eXist.getNext());
        }
        //*/
        
        /*
        // So we can validate against our schemas.
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        SchemaFactory schemaFactory = 
            SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        factory.setSchema(schemaFactory.newSchema(
            new Source[] {new StreamSource(args[1])}));

        SAXParser parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        XSDErrorHandler errors = new XSDErrorHandler();
        reader.setErrorHandler(errors);

        Builder builder = new Builder(reader);
        builder.build(SIFFileUtil.readFile(args[0]), null);

        if(errors.hasError()) {
            for(String error : errors.getErrors()) {
                System.out.println(error);
            }
        }
        //*/
        
        /* Demonstraights how to dynamically load and execute JRuby scripts! */
        /*
        // So we can load all the scripts defined in the XML configuration file.
        Builder parser = new Builder();
        String XML = null;
        try {
            XML = SIFFileUtil.readFile("SIFCommonsDemo.xml");
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(SIFCommonsDemo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        Document doc = null;
        try {
            doc = parser.build(XML, null);
        } catch (ParsingException ex) {
            java.util.logging.Logger.getLogger(SIFCommonsDemo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(SIFCommonsDemo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        Element root = doc.getRootElement();
        List<Element> peers = new ArrayList<Element>();
        SIFXOMUtil.combine(peers, root.getChildElements());
        List<String> beans = new ArrayList<String>();
        for(Element bean : peers) {
            String qName = bean.getNamespaceURI() + ":" + bean.getLocalName();
            if("http://www.springframework.org/schema/lang:jruby".equals(qName)) {
                if("org.sifassociation.testing.IScriptHooks".equals(bean.
                        getAttribute("script-interfaces").getValue())) {
                    beans.add(bean.getAttribute("id").getValue());
                }

            }
        }

        // Load and run all the JRuby classes within our Spring configuration!!!
        ApplicationContext ctx = 
                new FileSystemXmlApplicationContext("SIFCommonsDemo.xml");
        for(String bean : beans) {
            System.out.println("Running JRuby Spring Bean: " + bean);
            System.out.print("\n");
            IScriptHooks hooks = (IScriptHooks) ctx.getBean(bean);
            hooks.run();
        }
        System.out.println("All JRuby Spring Beans have been run!!!");
        */

        /*
        String url = "http://localhost/sif/requests/addresss/address;zone=temp;context=trial";
        System.out.println("");
        System.out.println(url);
        System.out.println("Resource: " + SIFURLUtil.getResource(url));
        System.out.println("Collection: " + SIFURLUtil.getCollection(url));
        
        url = "http://localhost/sif/requests/addresss/address";
        System.out.println("");
        System.out.println(url);
        System.out.println("Resource: " + SIFURLUtil.getResource(url));
        System.out.println("Collection: " + SIFURLUtil.getCollection(url));
        */

        // So we can evaluate Jackson for round tripping the hard
        // way:  JSON->XML->JSON
        System.out.println("\nInput:");
        String jsonInput = SIFFileUtil.readFile("resources/examples/input.json");
        System.out.println(jsonInput);
        System.out.println("\nMiddle:");
        String xmlMiddle = JacksonNative.getInstance().json2xml(jsonInput);
        System.out.println(xmlMiddle);
        ///*
        System.out.println("\nRound Tripped:");
        String jsonAfter = JacksonNative.getInstance().xml2json(xmlMiddle);
        System.out.println(jsonAfter);
        //*/
    }            
}