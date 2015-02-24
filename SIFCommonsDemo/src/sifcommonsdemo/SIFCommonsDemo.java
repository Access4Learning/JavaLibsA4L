package sifcommonsdemo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
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
import org.sifassociation.messaging.SIF3Payloads;
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
        
        ///*
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
        ///*
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
        //*/

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
    }

}