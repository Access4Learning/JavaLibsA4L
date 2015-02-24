package org.sifassociation.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import org.sifassociation.util.SIFFileUtil;
import org.sifassociation.util.SIFXOMUtil;

/**
 * Collection of static methods to help create common SIF 3.x payloads.
 * 
 * @author jlovell
 * @since 3.0
 * @version 3.0
 */
public class SIF3Payloads {
    
    public static String createEnvironment(
            String solutionId, 
            String method,
            String userToken,
            String name,
            String applicationKey,            
            SIFVersion infrastructureVersion,
            String dataModel,
            String applicationVendor,
            String applicationProduct, 
            String applicationVersion) {
        // So we create and environment payload with all the manditory fields.
        if(null == method || null == name ||
                null == applicationKey || null == infrastructureVersion ||
                null == dataModel || null == applicationProduct) {
            throw new IllegalArgumentException("Manditory field cannot be null.");
        }
        
        Element template;
        template = SIF3Payloads.getResourceRoot("environment.xml");
        
        ArrayList<Element> limits = new ArrayList<Element>();
        
        // So we both set the correct namespace and work with it.
        String ns = "http://www.sifassociation.org/infrastructure/" + 
                infrastructureVersion.toString();
        SIFXOMUtil.renamespace(template, ns, limits);
        
        SIFXOMUtil.editValue(template, "solutionId", ns, solutionId, limits);
        SIFXOMUtil.editValue(template, "authenticationMethod", ns, method, 
                limits);
        SIFXOMUtil.editValue(template, "userToken", ns, userToken, limits);
        SIFXOMUtil.editValue(template, "consumerName", ns, name, limits);
        SIFXOMUtil.editValue(template, "applicationKey", ns, applicationKey, 
                limits);
        SIFXOMUtil.editValue(template, "supportedInfrastructureVersion", ns, 
                infrastructureVersion.toString(), limits);
        SIFXOMUtil.editValue(template, "dataModelNamespace", ns, dataModel, 
                limits);
        SIFXOMUtil.editValue(template, "vendorName", ns, applicationVendor, 
                limits);
        SIFXOMUtil.editValue(template, "productName", ns, applicationProduct, 
                limits);
        SIFXOMUtil.editValue(template, "productVersion", ns, applicationVersion, 
                limits);
        
        return SIFXOMUtil.pretty(template);
    }

    /* To support places where we need multiple entries. */
    
    // So we can create renamespace any infrastructure payload.
    public static String createFromString(String xml,
            SIFVersion infrastructureVersion) {        
        // So we can work with the generated payload like any other here.
        Builder parser = new Builder();
        Document doc = null;
        try {
            doc = parser.build(xml, null);
        } catch (ParsingException ex) {
            Logger.getLogger(SIF3Payloads.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SIF3Payloads.class.getName()).log(Level.SEVERE, null, ex);
        }
        Element template;
        template = doc.getRootElement();
        
        // So we both set the correct namespace and work with it.
        String ns = "http://www.sifassociation.org/infrastructure/" + 
                infrastructureVersion.toString();
        ArrayList<Element> limits = new ArrayList<Element>();
        SIFXOMUtil.renamespace(template, ns, limits);
        
        return SIFXOMUtil.pretty(template);
    }
    
    // So we can ask to access all aspects of an object or provide it.
    // Note: Sigular, demos how to be multiple utilizing createProvisionEntry.
    public static String createProvisionRequest(String zone, String service, 
            String context, String type, boolean provide) {
        Element template;
        template = SIF3Payloads.getResourceRoot("provision.xml");
        
        ArrayList<Element> limits = new ArrayList<Element>();

        String ns = "";
        
        // So we set the singular attributes (for this function).
        SIFXOMUtil.editAttribute(template, "provisionedZone", ns, "id", zone, 
                limits);

        // So we drop the service section and can one or more back.
        Element services = (Element) template.query("//services").get(0);
        services.getFirstChildElement("service").detach();
        services.appendChild(createProvisionService(service, context, type, 
                provide));
        
        return SIFXOMUtil.pretty(template);
    }
    
    public static Element createProvisionService(String service, 
            String context, String type, boolean provide) {
        Element template;
        template = SIF3Payloads.getResourceRoot("provision.xml");
        
        ArrayList<Element> limits = new ArrayList<Element>();

        String ns = "";
        
        // So we replicate the service section.
        Element entry = (Element) template.query("//service").get(0);
        entry.detach();
        
        SIFXOMUtil.editAttribute(entry, "service", ns, "name", service, 
                limits);
        SIFXOMUtil.editAttribute(entry, "service", ns, "contextId", context, 
                limits);
        SIFXOMUtil.editAttribute(entry, "service", ns, "type", type, 
                limits);

        // So we request tha access that fits our role.
        if(provide) {
            SIFXOMUtil.editAttribute(entry, "right", ns, "type", "PROVIDE", 
                    limits);            
        }
        else {
            // So we can set with the requested rights.
            Element rights = (Element) entry.query("//rights").get(0);
            Element right = rights.getFirstChildElement("right", ns);
            right.detach();

            // So we know what rights to request.
            List<String> requests = Collections.unmodifiableList(Arrays.asList(
                    "QUERY", "CREATE", "UPDATE", "DELETE", "SUBSCRIBE"));
            
            for(String request : requests) {
                Element current = new Element(right);
                Attribute edit = current.getAttribute("type");
                edit.setValue(request);
                rights.appendChild(current);
            }
        }
        
        return entry;
    }
    
    // Creates a singly entry in multiple form.
    // Note:  Mostly put here for consistency and demonstraition purposes.
    public static String createProviderRequest(String id, String type, 
            String service, String context, String zone, String provider,
            boolean dynamic, boolean paged, int size, boolean count, String url) {
        Element providers = new Element("providers");
        Element entry = new Element(createProviderEntry(id, type, service, 
                context, zone, provider, dynamic, paged, size, count, url));
        providers.appendChild(entry);
        
        return SIFXOMUtil.pretty(providers);
    }
    
    public static Element createProviderEntry(String id, String type, 
            String service, String context, String zone, String provider,
            boolean dynamic, boolean paged, int size, boolean count, String url) {
        Element template;
        template = SIF3Payloads.getResourceRoot("provider.xml");
 
        ArrayList<Element> limits = new ArrayList<Element>();

        // So we add conditional elements before the namespace complicates it.
        if(paged) {
            Element support = (Element) template.query("//querySupport").get(0);
            
            Element current = new Element("maxPageSize");
            current.appendChild(Integer.toString(size));
            support.appendChild(current);
            
            current = new Element("totalCount");
            current.appendChild(Boolean.toString(count));
            support.appendChild(current);
        }
        
        // So we both set the correct namespace and work with it.
        String ns = "";
        SIFXOMUtil.renamespace(template, ns, limits);
        
        template.getAttribute("id").setValue(id);
        SIFXOMUtil.editValue(template, "serviceType", ns, type, limits);
        SIFXOMUtil.editValue(template, "serviceName", ns, service, limits);
        SIFXOMUtil.editValue(template, "contextId", ns, context, limits);
        SIFXOMUtil.editValue(template, "zoneId", ns, zone, limits);
        SIFXOMUtil.editValue(template, "providerName", ns, provider, limits);
        SIFXOMUtil.editValue(template, "dynamicQuery", ns, 
                Boolean.toString(dynamic), limits);
        SIFXOMUtil.editValue(template, "paged", ns, Boolean.toString(paged), 
                limits);        
        SIFXOMUtil.editValue(template, "location", ns, url, limits);
        
        return template;
    }
    
    /* Where single is all we need. */
    
    public static String createQueue(String name, String polling, String uri, 
            String connections, SIFVersion infrastructureVersion) {      
        Element template;
        template = SIF3Payloads.getResourceRoot("queue.xml");
        
        ArrayList<Element> limits = new ArrayList<Element>();
        
        // So we both set the correct namespace and work with it.
        String ns = "http://www.sifassociation.org/infrastructure/" + 
                infrastructureVersion.toString();
        SIFXOMUtil.renamespace(template, ns, limits);
        
        SIFXOMUtil.editValue(template, "polling", ns, polling, limits);
        SIFXOMUtil.editValue(template, "name", ns, name, limits);
        SIFXOMUtil.editValue(template, "ownerUri", ns, uri, limits);
        SIFXOMUtil.editValue(template, "maxConcurrentConnections", ns, 
                connections, limits);
        
        return SIFXOMUtil.pretty(template);
    }

    public static String createSubscription(String zoneID, String contextID, 
            String serviceType, String serviceName, String queueId,
            SIFVersion infrastructureVersion)  {
        Element template;
        template = SIF3Payloads.getResourceRoot("subscription.xml");
        
        ArrayList<Element> limits = new ArrayList<Element>();
        
        // So we both set the correct namespace and work with it.
        String ns = "http://www.sifassociation.org/infrastructure/" + 
                infrastructureVersion.toString();
        SIFXOMUtil.renamespace(template, ns, limits);
        
        SIFXOMUtil.editValue(template, "zoneId", ns, zoneID, limits);
        SIFXOMUtil.editValue(template, "contextId", ns, contextID, limits);
        SIFXOMUtil.editValue(template, "serviceType", ns, serviceType, limits);
        SIFXOMUtil.editValue(template, "serviceName", ns, serviceName, limits);
        SIFXOMUtil.editValue(template, "queueId", ns, queueId, limits);
        
        return SIFXOMUtil.pretty(template);
    }
    
    public static String deleteMultiple(String IDs, 
            SIFVersion infrastructureVersion) {
        Element template;
        template = SIF3Payloads.getResourceRoot("delete.xml");
        
        ArrayList<Element> limits = new ArrayList<Element>();
        
        // So we both set the correct namespace and work with it.
        String ns = "http://www.sifassociation.org/infrastructure/" + 
                infrastructureVersion.toString();
        SIFXOMUtil.renamespace(template, ns, limits);
        
        // So we both drop the "empty" element from the template and use it to
        // create multiple meaningful delete elements.
        Element parent = template.getFirstChildElement("deletes", ns);
        Element entry = parent.getFirstChildElement("delete", ns);
        parent.removeChildren();
        for(String ID : IDs.split(" "))  {
            Element current = new Element(entry);
            current.addAttribute(new Attribute("id", ID));
            parent.appendChild(current);
        }
        
        return SIFXOMUtil.pretty(template);
    }
    
    // So we can include errors in other returns.
    public static Element createErrorElement(String code, String scope, String message,
            String description, SIFVersion infrastructureVersion)  {
        Element template;
        template = SIF3Payloads.getResourceRoot("error.xml");
        
        ArrayList<Element> limits = new ArrayList<Element>();
        
        // So we both set the correct namespace and work with it.
        String ns = "http://www.sifassociation.org/infrastructure/" + 
                infrastructureVersion.toString();
        SIFXOMUtil.renamespace(template, ns, limits);
        
        SIFXOMUtil.editValue(template, "code", ns, code, limits);
        SIFXOMUtil.editValue(template, "scope", ns, scope, limits);
        SIFXOMUtil.editValue(template, "message", ns, message, limits);
        
        if(null != description) {
            Element current = new Element("description", ns);
            current.appendChild(description);
            template.appendChild(current);
        }
        
        return template;
    }
    
    public static String createError(String code, String scope, String message,
            String description, SIFVersion infrastructureVersion)  {
        
        return SIFXOMUtil.pretty(createErrorElement(code, scope, message, 
                description, infrastructureVersion));
    }
    
    // So we can create (very simple) alerts.
    public static Element createAlertElement(String reporter, SIFVersion infrastructureVersion)  {
        Element template;
        template = SIF3Payloads.getResourceRoot("alert.xml");
        
        ArrayList<Element> limits = new ArrayList<Element>();
        
        // So we both set the correct namespace and work with it.
        String ns = "http://www.sifassociation.org/infrastructure/" + 
                infrastructureVersion.toString();
        SIFXOMUtil.renamespace(template, ns, limits);
        
        SIFXOMUtil.editValue(template, "reporter", ns, reporter, limits);
        
        return template;
    }
    
    public static String createAlert(String reporter, SIFVersion infrastructureVersion)  {
        
        return SIFXOMUtil.pretty(createAlertElement(reporter, infrastructureVersion));
    }
    
    // So we can get our XML templates from the classpath with one line.
    private static Element getResourceRoot(String filename) {
        String resource = "";
        
        // So we can get the file from our classpath.
        InputStream resourceStream = null;
        try {
            resourceStream = Class.forName("org.sifassociation.messaging.SIF3Payloads").getClassLoader().getResourceAsStream(filename);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SIF3Payloads.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            resource = SIFFileUtil.readInputStream(resourceStream);
        } catch (IOException ex) {
            Logger.getLogger(SIF3Payloads.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // So we can work with it as a XOM XML tree.
        Builder parser = new Builder();
        Document doc = null;
        try {
            doc = parser.build(resource, null);
        } catch (ParsingException ex) {
            Logger.getLogger(SIF3Payloads.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SIF3Payloads.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return doc.getRootElement();
    }
    
}
