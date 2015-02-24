/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAnnotationItem;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeOrGroupRef;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaContentModel;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * So we can traverse Apache XmlSchema instances consistently.
 * Design:  Follow all parent types down to a simple type.
 * Assumes SIF style schemas (for now):
 * - No element references!
 * - No element groups.
 * - No element lists.
 * - No element unions.
 * - No attribute references.
 * - No attribute groups.
 * - May collect known annotations.
 * - AND almost certainly more!!!
 * 
 * @author jlovell
 * @since 3.0
 */
public class SIFXmlSchemaUtil {
    
    public static void traverse(XmlSchemaObject current, IElementVisit visit)  {
        // So we can call this liberally.
        if(null == current) {return;}
        
        // So we make the expected head recursion call.
        if(current instanceof XmlSchemaObject) {
            visit.head(current);
        }
        
        // So we walk the entire tree (from this point down).
        if(current instanceof XmlSchemaElement) {
            // So the tree "always" has its next type.
            if(null == ((XmlSchemaElement)current).getSchemaType()) {
                XmlSchemaType type = getNextType(
                        ((XmlSchemaElement)current).getSchemaTypeName(), 
                        ((XmlSchemaElement)current).getParent());
                if(type instanceof XmlSchemaType) {
                    ((XmlSchemaElement)current).setType(type);
                }
                else {
                    ((XmlSchemaElement)current).setType(
                            new XmlSchemaSimpleType(
                                new XmlSchema(), false));                            
                }
            }
            traverse(((XmlSchemaElement)current).getSchemaType(), visit);
        }
        else if(current instanceof XmlSchemaAttribute) {
            // So the tree "always" has its next type.
            if(null == ((XmlSchemaAttribute)current).getSchemaType()) {
                XmlSchemaType type = getNextType(
                    ((XmlSchemaAttribute)current).getSchemaTypeName(),
                    ((XmlSchemaAttribute)current).getParent());
                if(type instanceof XmlSchemaSimpleType) {
                    ((XmlSchemaAttribute)current).setSchemaType(
                            (XmlSchemaSimpleType)type);
                }
                else{
                    ((XmlSchemaAttribute)current).setSchemaType(
                            new XmlSchemaSimpleType(
                                new XmlSchema(), false));
                }
            }
            traverse(((XmlSchemaAttribute)current).getSchemaType(), visit);
        }
        else if(current instanceof XmlSchemaComplexType) {
            // So we can include the complex type's attributes.
            handleAttributes(((XmlSchemaComplexType)current).getAttributes(),
                    visit);
            // So we handle compositors.
            XmlSchemaParticle particle = 
                    ((XmlSchemaComplexType)current).getParticle();
            nextHandler(particle, visit);
            // So we handle complex content.
            XmlSchemaContentModel model = 
                    ((XmlSchemaComplexType)current).getContentModel();
            if(model instanceof XmlSchemaComplexContent) {
                XmlSchemaContent content = 
                        ((XmlSchemaComplexContent)model).getContent();
                if(content instanceof XmlSchemaComplexContentExtension) {
                    XmlSchemaComplexContentExtension extension = 
                            (XmlSchemaComplexContentExtension) content;
                    //System.out.println(extension.getBaseTypeName());  // Debug
                    handleAttributes(extension.getAttributes(), visit);
                    XmlSchemaType type = 
                            getNextType(extension.getBaseTypeName(), 
                            ((XmlSchemaComplexType)current).getParent());
                    if(type instanceof XmlSchemaType) {
                        traverse(type, visit);
                    }
                    else {
                        traverse(
                                new XmlSchemaSimpleType(
                                    new XmlSchema(), 
                                    false), 
                                visit);                            
                    }
                    nextHandler(extension.getParticle(), visit);
                }
                else if(content instanceof XmlSchemaComplexContentRestriction) {
                    XmlSchemaComplexContentRestriction restriction = 
                            (XmlSchemaComplexContentRestriction) content;
                    //System.out.println(restriction.getBaseTypeName());  // Debug
                    handleAttributes(restriction.getAttributes(), visit);
                    XmlSchemaType type = 
                            getNextType(restriction.getBaseTypeName(), 
                            ((XmlSchemaComplexType)current).getParent());
                    if(type instanceof XmlSchemaType) {
                        traverse(type, visit);
                    }
                    else {
                        traverse(
                                new XmlSchemaSimpleType(
                                    new XmlSchema(), 
                                    false), 
                                visit);                            
                    }
                    nextHandler(restriction.getParticle(), visit);
                }
            }
            else if(model instanceof XmlSchemaSimpleContent) {
                XmlSchemaContent content = 
                    ((XmlSchemaSimpleContent)model).getContent();
                if(content instanceof XmlSchemaSimpleContentExtension) {
                    XmlSchemaSimpleContentExtension extension = 
                            (XmlSchemaSimpleContentExtension) content;
                    //System.out.println(extension.getBaseTypeName());  // Debug
                    handleAttributes(extension.getAttributes(), visit);
                    XmlSchemaType type = 
                            getNextType(extension.getBaseTypeName(), 
                            ((XmlSchemaComplexType)current).getParent());
                    if(type instanceof XmlSchemaType) {
                        traverse(type, visit);
                    }
                    else {
                        traverse(
                                new XmlSchemaSimpleType(
                                    new XmlSchema(), 
                                    false), 
                                visit);                            
                    }
                }
                else if(content instanceof XmlSchemaSimpleContentRestriction) {
                    XmlSchemaSimpleContentRestriction restriction = 
                            (XmlSchemaSimpleContentRestriction) content;
                    //System.out.println(restriction.getBaseTypeName());  // Debug
                    handleAttributes(restriction.getAttributes(), visit);
                    XmlSchemaType type = 
                            getNextType(restriction.getBaseTypeName(), 
                             ((XmlSchemaComplexType)current).getParent());
                    if(type instanceof XmlSchemaType) {
                        traverse(type, visit);
                    }
                    else {
                        traverse(
                                new XmlSchemaSimpleType(
                                    new XmlSchema(), 
                                    false), 
                                visit);                            
                    }
                }
            }

        }
        
        // So we make the expected tail recursion call.
        if(current instanceof XmlSchemaObject) {
            visit.tail(current);
        }
    }
    
    // So we can follow non-top-level types.    
    public static XmlSchemaType getNextType(QName name, XmlSchema schema) {
        XmlSchemaType type;
        type = (XmlSchemaType) schema.getTypeByName(name);
        // So if the lookup fails we try again specifying a LIKELY namespace.
        String namespace = schema.getLogicalTargetNamespace();
        if(null == type && null != namespace && null != name) {
            QName typeName = new QName(namespace, name.getLocalPart());
            type = (XmlSchemaType) schema.getTypeByName(typeName);
        }
        return type;
    }
    
    // So we can include attributes consistently.
    private static void handleAttributes(
            List<XmlSchemaAttributeOrGroupRef> attributes, 
            IElementVisit visit) {
        if(0 < attributes.size()) {
            for(XmlSchemaAttributeOrGroupRef attribute : attributes) {
                traverse(attribute, visit);
            }
        }
    }
    
    /* So we can handle nested compositors. */
    
    private static void nextHandler(Object item, 
                                    IElementVisit visit) {
        if(null != item) {
            if(item instanceof XmlSchemaSequence) {
                handleSequence((XmlSchemaSequence)item, visit);
            }
            else if(item instanceof XmlSchemaChoice) {
                handleChoice((XmlSchemaChoice)item, visit);
            }
            else if (item instanceof XmlSchemaAll) {
                handleAll((XmlSchemaAll)item, visit);
            }
        }
    }
    
    private static void handleSequence(XmlSchemaSequence sequence,
                                       IElementVisit visit) {
        for(Object item : sequence.getItems()) {
            if (item instanceof XmlSchemaElement) {
                traverse((XmlSchemaElement)item, visit);
            }
            else {
                nextHandler(item, visit);
            }
        }
    }
    
    private static void handleChoice(XmlSchemaChoice choice, 
                                     IElementVisit visit) {
        for(Object item : choice.getItems()) {
            if (item instanceof XmlSchemaElement) {
                traverse((XmlSchemaElement)item, visit);
            }
            else {
                nextHandler(item, visit);
            }
        }   
    }
    
    private static void handleAll(XmlSchemaAll all, IElementVisit visit) {
        for(Object item : all.getItems()) {
            if (item instanceof XmlSchemaElement) {
                traverse((XmlSchemaElement)item, visit);
            }
            else {
                nextHandler(item, visit);
            }
        }
    }

    /* Non-traversal utililities */
    
    // So working with our numerous anotations is simplified.
    // Assumes:  All top level tag names are unique.
    public static Map<String, String> getAppInfos(XmlSchemaAnnotation annotation) {
        if(null != annotation) {
            Map<String, String> appInfos = new HashMap<String, String>(6);
            List<XmlSchemaAnnotationItem> items = annotation.getItems();
            for(XmlSchemaAnnotationItem item : items) {
                NodeList markup = null;
                if(item instanceof XmlSchemaAppInfo) {
                    markup = ((XmlSchemaAppInfo)item).getMarkup();
                    if(null != markup) {
                        for(int i = 0; i < markup.getLength(); i++) {
                            Node infoItem = markup.item(i);
                            appInfos.put(infoItem.getLocalName(), 
                                    infoItem.getTextContent());
                        }
                    }
                }
            }
            return appInfos;
        }
        return null;
    }

    // So show our users the documentation is easier.
    public static String getDocumentation(XmlSchemaAnnotation annotation) {
        if(null != annotation) {
            for(XmlSchemaAnnotationItem item : annotation.getItems()) {
                if(item instanceof XmlSchemaDocumentation) {
                    NodeList markup = ((XmlSchemaDocumentation)item).getMarkup();
                    for(int i = 0; i < markup.getLength(); i++) {
                        Node entry = markup.item(i);
                        return entry.getTextContent();
                    }
                }
            }
        }
        return "";
    }
 
    // So we can get the schema roots of an XML Schema file consistently.
    public static Map<QName, XmlSchemaElement> getRootElements(String filePath) {
        // So we load the indicated file.
        InputStream is = null;
        try {
            is = new FileInputStream(filePath);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SIFXmlSchemaUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // So we have the correct realitive directory.
        int last = filePath.lastIndexOf(File.separator);
        String dirPath = "";
        if(last > 0) {
            dirPath = filePath.substring(0, last);
        }        
        return getRootElements(is, dirPath);
    }
    
    // So we can get the schema roots of an XML Schema file consistently.
    public static Map<QName, XmlSchemaElement> getRootElements(
            InputStream is, String basePath) {
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        if(null != basePath && 0 != "".compareTo(basePath)) {
            schemaCol.setBaseUri(basePath);
        }
        XmlSchema schema = schemaCol.read(new StreamSource(is));    
        return schema.getElements();    
    }
    
    /* SIF Specific/Demo Functionality */
    
    // So we can retrieve the service names.
    public static Set<QName> getSIFServices(String filePath) {
        Map<QName, XmlSchemaElement> elements = getRootElements(filePath);
        return getSIFServices(elements); 
    }
    
    // So we can retrieve the service names.
    public static Set<QName> getSIFServices(InputStream is, String basePath) {
        Map<QName, XmlSchemaElement> elements = getRootElements(is, basePath);
        return getSIFServices(elements); 
    }
    
    // So we can reuse the root elements.
    // So if we change how we determin what is a service it is consistently reflected.
    public static Set<QName> getSIFServices(Map<QName, XmlSchemaElement> elements) {
        // So we only use the plural form as a unique service.
        // Assumes:  SIF style service names.
        Set<QName> services = new HashSet<QName>();
        Set<QName> keys = elements.keySet();
        for(QName key : keys) {
            // So we can match by naming convention.
            QName plural = new QName(
                    key.getNamespaceURI(), 
                    key.getLocalPart() + "s");
            if(!keys.contains(plural)) {
                services.add(key);
            }
            /*
            // So we can match by annotation.
            XmlSchemaAnnotation annotation = elements.get(key).getAnnotation();
            if(null != annotation) {
                Map appInfos = SIFXmlSchemaUtil.getAppInfos(annotation);
                if(null != annotation) {
                    String collection = (String) appInfos.get("isCollectionObject");
                    if(null != collection && 0 == "yes".compareTo(collection)) {
                        services.add(key);
                    }
                }
            }
            */
        }
        return services; 
    }

    // So we have the XPaths resulting from all the service roots.
    // So we have the mandiotry status and SIF characteristics for each XPath.
    public static List<XPathPlus> getSIFXPaths(String filePath, QName service) {
        Map<QName, XmlSchemaElement> elements = getRootElements(filePath);
        return getSIFXPaths(elements, service);
    }
    
    // So we have the XPaths resulting from all the service roots.
    // So we have the mandiotry status and SIF characteristics for each XPath.
    public static List<XPathPlus> getSIFXPaths(
            InputStream is, String basePath, QName service) {
        Map<QName, XmlSchemaElement> elements = getRootElements(is, basePath);
        return getSIFXPaths(elements, service);
    }
    
    // So we have the XPaths resulting from all the service roots.
    // So we have the mandiotry status and SIF characteristics for each XPath.
    public static List<XPathPlus> getSIFXPaths(
            Map<QName, XmlSchemaElement> elements, QName service) {
        Set<QName> services = SIFXmlSchemaUtil.getSIFServices(elements);
        PathMandatoryVisit visitor = new PathMandatoryVisit();

        for(QName key : services) {
            if(key.equals(service)) {
                XmlSchemaElement element = elements.get(key);
                SIFXmlSchemaUtil.traverse(element, visitor);
            }
        }
        List<XPathPlus> fields = new ArrayList<XPathPlus>();
        for(XPathPlus field : visitor.getPaths() ) {
            fields.add(field);
        }
        return fields;
    }
    
    // So we have the SIF objects rather than the collections.
    // So we have the events status and other annotations for each object.
    public static List<XPathPlus> getSIFObjects(String filePath, QName service) {
        Map<QName, XmlSchemaElement> elements = getRootElements(filePath);
        return getSIFObjects(elements, service);
    }
    
    // So we have the SIF objects rather than the collections.
    // So we have the events status and other annotations for each object.
    public static List<XPathPlus> getSIFObjects(
            InputStream is, String basePath, QName service) {
        Map<QName, XmlSchemaElement> elements = getRootElements(is, basePath);
        return getSIFObjects(elements, service);
    }
    
    // So we have the SIF objects rather than the collections.
    // So we have the events status and other annotations for each object.
    public static List<XPathPlus> getSIFObjects(
            Map<QName, XmlSchemaElement> elements, QName service) {
        Set<QName> services = SIFXmlSchemaUtil.getSIFServices(elements);
        PathObjectVisit visitor = new PathObjectVisit();

        for(QName key : services) {
            if(key.equals(service)) {
                XmlSchemaElement element = elements.get(key);
                SIFXmlSchemaUtil.traverse(element, visitor);
            }
        }
        List<XPathPlus> fields = new ArrayList<XPathPlus>();
        for(XPathPlus field : visitor.getPaths() ) {
            fields.add(field);
        }
        return fields;
    }
}
