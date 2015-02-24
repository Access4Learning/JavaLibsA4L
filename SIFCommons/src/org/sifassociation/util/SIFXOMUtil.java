/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.Serializer;
import nu.xom.XPathContext;
import org.xml.sax.XMLReader;

/**
 * Class contains tools to traverse a XOM tree and take common actions.
 * 
 * @author jlovell
 * @version 3.0
 * @since 3.0
 */
public class SIFXOMUtil {
    
    /* Output */
    
    // So we can pretty print.
    public static String pretty(Element oldRoot) {    
        Element root = new Element(oldRoot);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Serializer serializer = new Serializer(out);
        serializer.setIndent(2);
            try {
                serializer.write(new Document(root));
            } catch (IOException ex) {
                return root.toXML();
            }

        String output;
            try {
                output = out.toString("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                return root.toXML();
            }

        // So we drop the XML version and encoding declarations.
        return output.substring(output.indexOf(serializer.getLineSeparator()) + 1);        
    }
    
    /* Edit */
    
    /**
     * Strips remove from the front of the root's and its children's names.
     * 
     * @param root  Where to start.  This is what gets modified!
     * @param remove  The string to strip from the left hand side.
     * @param limits The elements whose children will be ignored.
     */
    public static void lStrip(Element root, String remove, 
            List<Element> limits) {
        traverse(root, new IElementAction() {
            @Override
                public void action(Element node, String indicator) {
                    // So each element is modified.
                    String local = node.getLocalName();
                    if(local.startsWith(indicator)) {
                        node.setLocalName(local.substring(indicator.length()));
                    }
                }
            }
            , remove, limits);
    }
    
    /**
     * Prepends add to the front of the root's and its children's names.
     * 
     * @param root  Where to start.  This is what gets modified!
     * @param remove  The string to prepend.
     * @param limits The elements whose children will be ignored.
     */
    public static void prepend(Element root, final String add, 
            List<Element> limits) {
        traverse(root, new IElementAction() {
            @Override
                public void action(Element node, String indicator) {
                    // So each element is modified.
                    String local = node.getLocalName();
                    node.setLocalName(add.concat(local));
                }
            }
            , add, limits);
    }
     
    /**
     * Replaces the namespace root and its children are in.
     * 
     * @param root  Where to start.  This is what gets modified!
     * @param namespace  The namespace all the modified nodes will belong to.
     * @param limits The elements whose children will be ignored.
     */
    public static void renamespace(Element root, String namespace, 
            List<Element> limits) {
        traverse(root, new IElementAction() {
            @Override
                public void action(Element node, String indicator) {
                    // So each element is modified.
                    node.setNamespaceURI(indicator);
                }
            }
            , namespace, limits);
    }

    // So we can quickly and consistently edit all matching values.
    // Pass in a null value to remove identified element.
    public static void editValue(Element root, final String tagName, 
            final String namespace, final String value, List<Element> limits) {
        traverse(root, new IElementAction() {
            @Override
                public void action(Element node, String indicator) {
                    // So each matching element is modified.
                    if(0 == tagName.compareTo(node.getLocalName()) &&
                            0 == namespace.compareTo(node.getNamespaceURI())) {
                        // So we remove unwanted elements.
                        if(null == value) {
                            node.detach();
                        }
                        else {
                            node.removeChildren();
                            node.appendChild(value);
                        }
                    }
                }
            }
            , value, limits);        
    }
    
    // So we can quickly and consistently edit all matching attribute values.
    public static void editAttribute(Element root, final String tagName, 
            final String namespace, final String attributeName, 
            final String value, List<Element> limits) {
        traverse(root, new IElementAction() {
            @Override
                public void action(Element node, String indicator) {
                    // So each matching element is modified.
                    if(0 == tagName.compareTo(node.getLocalName()) &&
                            0 == namespace.compareTo(node.getNamespaceURI())) {
                        Attribute edit = node.getAttribute(attributeName);
                        if(null != edit) {
                            edit.setValue(value);
                        }
                    }
                }
            }
            , value, limits);        
    }
    
    /**
     * Visits all elements in the (sub)tree indicated by root and takes action.
     * 
     * @param root  Where to start.  This is what gets modified!
     * @param visit  What action to take at each element.
     * @param indicator The modification to be made.
     * @param limits The elements whose children will be ignored.
     */
    private static void traverse(Element root, IElementAction visit, 
            String indicator, List<Element> limits) {
        // So we start at the beginning.
        Element current = root;
        
        // So we modify every node.
        List<Element> children = new ArrayList<Element>();
        int i = 0;
        
        // So we visit every element.
        while(null != current) {
            if(! contains(limits, current)) {
                combine(children, current.getChildElements());
            }
            
            visit.action(current, indicator);
                        
            current = (children.size() > i) ? (Element)children.get(i) : null;
            i++;
        }
    }
    
    /* Retrieve via XPath */
    
    // Returns the string of the first matching XPath or null.
    // Note: To use the ns namespace use the prefix "ns" in your xpath.
    // Example: /ns:environment/ns:consumerName
    public static String getFirstXPath(String xpath, Element root, String ns) {
        // So we use the specified namespace.
        XPathContext xpc = new XPathContext();
        xpc.addNamespace("ns", ns);

        // So we execute the query.
        Nodes results = root.query(xpath, xpc);
        if(0 < results.size()) {
            Node first = results.get(0);
            if(null != first) {
                return first.getValue();
            }
        }
        
        // So we know the difference between failure and a blank field.
        return null;
    }
    
    /* Supporting Partial Updates */
    
    // So we can quickly and consistently find all the data XPaths
    public static Set<String> getXPaths(Element root) {
        List<String> parents = new ArrayList<String>();;
        Set<String> xpaths = new LinkedHashSet<String>();
        
        buildXPath(root, parents, xpaths);
        
        return xpaths;
    }

    // So we follow each child in the XML tree.
    private static void buildXPath(Element next, List<String> parents, 
            Set <String> xpaths) {
        // HEAD
        
        Elements children = next.getChildElements();
        
        // So we remember.
        parents.add(next.getQualifiedName());
        
        // So we keep what we went looking for.
        if(0 == children.size()) {
            xpaths.add(toXPath(parents, ""));
        }
        for(int i = 0; i < next.getAttributeCount(); i++) {
            xpaths.add(toXPath(parents, "@" + next.getAttribute(i).getQualifiedName()));
        }
        
        // RECURSE
        for(int i = 0; i < children.size(); i++) {
            buildXPath(children.get(i), parents, xpaths);
        }
        
        // TAIL
        
        // So we forget.
        parents.remove(parents.size()-1);
    }

    // So we can quickly and consistently find all the high level lists.
    public static Set<String> getListXPaths(Element root) {
        List<String> parents = new ArrayList<String>();
        Set<String> xpaths = new LinkedHashSet<String>();
        
        buildListXPath(root, parents, xpaths);
        
        return xpaths;
    }

    // So we find each high level list in the XML tree.
    private static void buildListXPath(Element next, List<String> parents, 
            Set <String> xpaths) {
        // HEAD
        
        Elements children = next.getChildElements();
        
        // So we remember.
        parents.add(next.getQualifiedName());
        
        // So we keep what we went looking for.
        Element last = null;
        Element current = null;
        for(int i = 0; i < children.size(); i++) {
            current = children.get(i);
            if(null != last) {
                if(similar(current, last)) {
                    xpaths.add(toXPath(parents, ""));
                    parents.remove(parents.size()-1);
                    return;
                }
            }
            last = current;
        }
        
        // RECURSE
        for(int i = 0; i < children.size(); i++) {
            buildListXPath(children.get(i), parents, xpaths);
        }
        
        // TAIL
        
        // So we forget.
        parents.remove(parents.size()-1);
    }
    
    // So we can quickly and consistently combine the pieces of an XPath.
    private static String toXPath(List<String> parents, String child) {
        String combined = "";
        for(String parent : parents) {
            combined = combined + "/" + parent;
        }
        if(!child.isEmpty()) {
            combined = combined + "/" + child;
        }
        return combined;
    }
    
    // So we know if we are updating a field or replacing a list.
    // Returns the highest level list or the empty string if a field.
    public static String startsWithList(String xpath, Set<String> lists) {
        for(String list : lists) {
            if(xpath.startsWith(list)) {
                return list;
            }
        }
        return "";
    }
    
    /* SIFisms */
    
    /**
     * So we can support all know ID attributes consistently.
     * 
     * @param object  Root of the element we want an ID for.
     * @return The retrieved id or the empty string.
     */
    public static String getObjectId(Element object) {
        String current = null;
        current = object.getAttributeValue("refId");
        if(null != current) {
           return current;
        }
        current = object.getAttributeValue("id");
        if(null != current) {
           return current;
        }
        current = object.getAttributeValue("RefId");
        if(null != current) {
           return current;
        }
        
        return "";
    }
    
    public static Attribute createObjectId(String namespace, String value) {
        String key = "RefId";
        if(namespace.startsWith("http://www.sifassociation.org/datamodel/na/3.")) {
            key = "refId";
        }
        else if(namespace.startsWith("http://www.sifassociation.org/infrastructure/3.")) {
            key = "id";
        }
        
        return new Attribute(key, value);
    }
    
    /* Helpers */
    
    /**
     * So instead of working with Elements we can work with standard Java Lists.
     * 
     * @param existing  The list of elements that already exists (this grows).
     * @param append  The elements to add to the list.
     * @since 3.0
     */
    public static void combine(List<Element> existing, Elements append) {
        for(int i = 0; append.size() > i; i++) {
            existing.add(append.get(i));
        }
    }

    /**
     * So we can compare similar elements.
     * 
     * Note: Only the tag's namespace and name get compared.
     * 
     * @param x  One element to compare.
     * @param y  One element to compare.
     * @return similar: true  different: false
     * @since 3.0
     */
    private static boolean similar(Element x, Element y) {
        return (x.getNamespaceURI() + ":" + x.getLocalName()).equalsIgnoreCase(
                y.getNamespaceURI() + ":" + y.getLocalName());
    }
    
    /**
     * So we can look for similar elements.
     * 
     * @param haystack  The List of elements to inspect.
     * @param needle  The element to find a match for.
     * @return  combined: true  not combined: false 
     * @since 3.0
     */
    private static boolean contains(List<Element> haystack, Element needle) {
        
        for(int i = 0; i < haystack.size(); i++) {
            if(similar(haystack.get(i), needle)) {
                return true;
            }
        }
        
        return false;
    }

    // So we can simply validate a single XML file against a single XSD file.
    public static boolean validate(String xml, String xsdURL) {
        return validate(xml, xsdURL, new StringBuilder());
    }
    
    // So we can simply validate a single XML file against a single XSD file.
    public static boolean validate(String xml, String xsdURL, 
            StringBuilder details) {
        XSDErrorHandler errors = new XSDErrorHandler();
        
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);

            SchemaFactory schemaFactory = 
                SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            factory.setSchema(schemaFactory.newSchema(
                new Source[] {new StreamSource(xsdURL)}));
            
            SAXParser parser = factory.newSAXParser();
            XMLReader reader = parser.getXMLReader();

            reader.setErrorHandler(errors);

            Builder builder = new Builder(reader);
            builder.build(xml, null);
        } catch (Exception ex) {
            Logger.getLogger(SIFXOMUtil.class.getName()).log(Level.SEVERE, null, ex);
            details.append("<p>" +  ex.getLocalizedMessage() + "</p>");
            return false;
        }

        for(String error : errors.errors) {
            details.append("<p>" + error + "</p>");
        }
        
        return !errors.hasError();
    }

}
