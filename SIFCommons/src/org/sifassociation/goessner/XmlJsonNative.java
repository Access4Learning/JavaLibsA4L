/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.goessner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.ParsingException;
import org.sifassociation.util.SIFXOMUtil;

/**
 * Uses the Goessner Notation guidelines to convert using native Java means:
 * XML -> JSON
 * JSON -> XML
 * 
 * @author jlovell
 * @see http://goessner.net/
 */
public class XmlJsonNative implements IXmlJson{
    private static final XmlJsonNative INSTANCE = new XmlJsonNative();
    
    private XmlJsonNative() {
    }

    /**
     * So we can convert;
     * 
     * @return The one and only true XmlJsonNative instance.
     */
    public static XmlJsonNative getInstance() {
        return INSTANCE;
    }    
    
    @Override
    public String json2xml(String json) {
        if(null == json || json.isEmpty()) {
            return "";
        }
        // So we have an accurate representation of the JSON as a tree.
        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode jsonRoot;
        try {
            jsonRoot = jsonMapper.readTree(json);
        } catch (IOException ex) {
            Logger.getLogger(XmlJsonNative.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        
        
        // So we build the diresired XML tree.
        // SIF 3 uses Gossner Notation
        // See:  http://www.xml.com/pub/a/2006/05/31/converting-between-xml-and-json.html
        // Patterns:
        // 1 	"e": null                                   <e/>
        // 2 	"e": "text"                                 <e>text</e>
        // 3 	"e":{"@name": "value"}                      <e name="value" />
        // 4 	"e": { "@name": "value", "#text": "text" }  <e name="value">text</e>
        // 5 	"e": { "a": "text", "b": "text" }           <e> <a>text</a> <b>text</b> </e>
        // 6 	"e": { "a": ["text", "text"] }              <e> <a>text</a> <a>text</a> </e>
        // 7 	"e": { "#text": "text", "a": "text" }       <e> text <a>text</a> </e>
        // To Do:  Use the above to create a set of tests for the interface.
        Element xmlRoot;
        // So we visit every element.
        xmlRoot = json2xmlTree(jsonRoot);
        
        // So we have the resulting JSON.
        String xml;
        xml = SIFXOMUtil.pretty(xmlRoot);
        return xml;
    }

    /**
     * Sets up and returns the conversion of the JSON tree to an XML one.
     * 
     * @param jsonCurrent  Initially the wrapper node of the JSON tree.
     * @return  New root XOM Element of the (converted JSON) XML tree.
     */
    private Element json2xmlTree(JsonNode jsonCurrent) {
        // So we fail rather than blowing up.
        if(null == jsonCurrent) {
            return null;
        }
        // So we get past the outer wrapper and know the current nodes name.
        Iterator<Map.Entry<String, JsonNode>> fields;
        fields = jsonCurrent.fields();
        Map.Entry<String, JsonNode> next = null;
        if(fields.hasNext()) {
            next = fields.next();
        }
        if(null != next) {
            return XmlJsonNative.this.json2xmlTree(next.getKey(), next.getValue(), "");
        }
        return null;
    }
    
    /**
     * Does the recursion to build the XML tree.
     * 
     * Note: DO NOT DIRECTLY CALL
     * 
     * @param name  Key of the current JSON node.
     * @param jsonCurrent  The JSON node to process now.
     * @param namespace  The last detected default namespace.  May change.
     * @return  Root XOM Element of the (converted JSON) XML tree.
     */
    private Element json2xmlTree(String name, JsonNode jsonCurrent, String namespace) {
        // Head
        JsonNode namespaceNode = jsonCurrent.get("@xmlns");
        if(null != namespaceNode) {
            namespace = namespaceNode.textValue();
        }
        Element xmlCurrent;
        xmlCurrent = new Element(name, namespace);
        // 1 	"e": null                                   <e/>
        // 2 	"e": "text"                                 <e>text</e>
        if(jsonCurrent.isValueNode()) {
            String value = jsonCurrent.textValue();
            if(null != value) {
                xmlCurrent.appendChild(value);
            }
        }
        
        // Recurse and build.
        Iterator<Map.Entry<String,JsonNode>> fields = jsonCurrent.fields();
        while(fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            // So we can skip nodes that are handled special (don't just become XML nodes).
            String nextName = next.getKey();
            JsonNode nextNode = next.getValue();
            // 3 	"e":{"@name": "value"}                      <e name="value" />
            if(nextName.startsWith("@")) {
                if(0 != "@xmlns".compareTo(nextName)) {
                    // So we have our attribute.
                    nextName = nextName.substring(1);
                    xmlCurrent.addAttribute(new Attribute(nextName, nextNode.textValue()));
                }
            }
            // 4 	"e": { "@name": "value", "#text": "text" }  <e name="value">text</e>
            else if(0 == "#text".compareTo(nextName)) {
                xmlCurrent.appendChild(name);
            }
            // 6 	"e": { "a": ["text", "text"] }              <e> <a>text</a> <a>text</a> </e>
            else if(nextNode instanceof ArrayNode) {
                for(int i = 0; i < ((ArrayNode)nextNode).size(); i++) {
                    xmlCurrent.appendChild(json2xmlTree(nextName, ((ArrayNode)nextNode).get(i) , namespace));
                }
            }
            // 5 	"e": { "a": "text", "b": "text" }           <e> <a>text</a> <b>text</b> </e>
            else {
                xmlCurrent.appendChild(json2xmlTree(nextName, nextNode , namespace));
            }
        }
        
        // Tail
        return xmlCurrent;        
    }
    
    @Override
    public String xml2json(String xml) {
        if(null == xml || xml.isEmpty()) {
            return "";
        }
        // So we have an accurate representation of the XML as a tree.
        Builder parser = new Builder();
        Document xmlTree = null;
        try {
            xmlTree = parser.build(xml, null);
        } catch (IOException | ParsingException ex) {
            Logger.getLogger(XmlJsonNative.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        Element xmlRoot = xmlTree.getRootElement();
        
        // So we build the diresired JSON tree.
        // SIF 3 uses Gossner Notation
        // See:  http://www.xml.com/pub/a/2006/05/31/converting-between-xml-and-json.html
        // Patterns:
        // 1 	<e/>                                    "e": null
        // 2 	<e>text</e>                             "e": "text"
        // 3 	<e name="value" />                      "e":{"@name": "value"}
        // 4 	<e name="value">text</e>                "e": { "@name": "value", "#text": "text" }
        // 5 	<e> <a>text</a> <b>text</b> </e> 	"e": { "a": "text", "b": "text" }
        // 6 	<e> <a>text</a> <a>text</a> </e> 	"e": { "a": ["text", "text"] }
        // 7 	<e> text <a>text</a> </e>               "e": { "#text": "text", "a": "text" }
        // To Do:  Use the above to create a set of tests for the interface.
        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode jsonRoot = jsonMapper.createObjectNode();
        // So we visit every element.
        String rootNamespace = xmlRoot.getNamespaceURI();  // To Do:  Include as default in JSON root @xmlns and anytime it changes.        
        xml2jsonTree(xmlRoot, "", jsonRoot, jsonMapper, false);
        
        // So we have the resulting JSON.
        String json;
        try {
            json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonRoot);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(XmlJsonNative.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        return json;
    }
    
    /**
     * Recursive function that builds the JSON tree from the XML tree.
     * 
     * Note:  Names are written such that they make sense in the general case.
     * Note:  Initial values should be seeded as described below.
     * Note:  Does not Implement support for mixed content (pattern 7).
     * 
     * @param xmlCurrent  Initially the root element of the XML tree to convert.  Changes to the depth-first node in the XML tree as we recurse.
     * @param parentNamespace  Initially the empty string, so the root default namespace is included.  When it changes, a new default namespace (@xmlns) is added.
     * @param jsonParent  Initially any empty JsonNode.  This is built up by this function to be the JSON tree.  So keep a handle on this outside of this function.
     * @param jsonMapper  Initially the JSON mapper used to create jsonParent.  Should not change.
     * @param noLable     Initially false.  Flag to omit the element name from the JSON object.  Should remain false, except where conversion requires it (array members of pattern 6).
     */
    private static void xml2jsonTree(Node xmlCurrent, String parentNamespace, JsonNode jsonParent, 
            ObjectMapper jsonMapper, boolean noLable) {
        // Head
        // So we know what we are currently building.
        JsonNode jsonCurrent = null;
        JsonNode arrayCurrent = null;
        // So we don't attempt to add non-elements (from the XML tree) to the JSON tree.
        if(!(xmlCurrent instanceof Element)) {
            return;
        }
        Elements currentChildren = ((Element) xmlCurrent).getChildElements();
        int attributeCount = ((Element) xmlCurrent).getAttributeCount();        
        if(jsonParent instanceof ArrayNode) {
            if(0 == attributeCount && 0 == currentChildren.size()) {
                // So we stop the value.
                ((ArrayNode) jsonParent).add(((Element)xmlCurrent).getValue());
            }
            else {
                // So we keep going inside the array.
                ObjectNode jsonRoot = jsonMapper.createObjectNode();
                xml2jsonTree(xmlCurrent, parentNamespace, 
                    jsonRoot, jsonMapper, true);  // noLable = true
                
                // So array entries share its name (not each have their own).
                ObjectNode keylessRoot = jsonMapper.createObjectNode();
                Iterator<Map.Entry<String, JsonNode>> iterator = jsonRoot.fields();
                while(iterator.hasNext()) {
                    Map.Entry<String, JsonNode> next = iterator.next();
                    if(next.getValue() instanceof ObjectNode) {
                        keylessRoot.setAll((ObjectNode) next.getValue());
                    }
                    else {
                        keylessRoot.set(next.getKey(), next.getValue());
                    }
                }
                ((ArrayNode) jsonParent).add(keylessRoot);
            }
        }        
        else {             
            // 1 	<e/>                                    "e": null
            if(0 == attributeCount && 0 == currentChildren.size() && xmlCurrent.getValue().isEmpty()) {
                jsonCurrent = ((ObjectNode) jsonParent).set(((Element) xmlCurrent).getLocalName(), null);
            }
            // 2 	<e>text</e>                             "e": "text"
            else if(0 == attributeCount && 0 == currentChildren.size() && !xmlCurrent.getValue().isEmpty()) {
                jsonCurrent = ((ObjectNode) jsonParent).put(((Element) xmlCurrent).getLocalName(), xmlCurrent.getValue());
            }
            // 3 	<e name="value" />                      "e":{"@name": "value"}
            else if(0 != attributeCount && 0 != currentChildren.size()) {
                for(int j = 0; j < attributeCount; j++) {
                    Attribute attribute = ((Element) xmlCurrent).getAttribute(j);
                    jsonCurrent = ((ObjectNode) jsonParent).putObject(((Element) xmlCurrent).getLocalName());
                    ((ObjectNode) jsonCurrent).put("@"+attribute.getLocalName(), attribute.getValue());
                }
            }
            // 4 	<e name="value">text</e>                "e": { "@name": "value", "#text": "text" }
            else if(0 != attributeCount && 0 == currentChildren.size()) {
                for(int j = 0; j < attributeCount; j++) {
                    Attribute attribute = ((Element) xmlCurrent).getAttribute(j);
                    jsonCurrent = ((ObjectNode) jsonParent).put("@"+attribute.getLocalName(), attribute.getValue());
                }
                if(!xmlCurrent.getValue().isEmpty()) {
                    jsonCurrent = ((ObjectNode) jsonParent).put("#text", xmlCurrent.getValue());
                }
            }        
            // 5 	<e> <a>text</a> <b>text</b> </e> 	"e": { "a": "text", "b": "text" }
            else if(0 == attributeCount && 0 != currentChildren.size() && !isList(((Element)xmlCurrent))) {
                if(noLable) {
                    jsonCurrent = jsonParent;
                }
                else {
                    jsonCurrent = ((ObjectNode) jsonParent).putObject(((Element) xmlCurrent).getLocalName());
                }
            }
            // 6 	<e> <a>text</a> <a>text</a> </e> 	"e": { "a": ["text", "text"] }
            else if(0 == attributeCount && 0 != currentChildren.size() && isList(((Element)xmlCurrent))) {
                jsonCurrent = ((ObjectNode) jsonParent).putObject(((Element) xmlCurrent).getLocalName());
                Element firstElement = ((Element) xmlCurrent).getChildElements().get(0);
                arrayCurrent = ((ObjectNode) jsonCurrent).putArray(firstElement.getLocalName());
            }            
            // 7 	<e> text <a>text</a> </e>               "e": { "#text": "text", "a": "text" }
            // SIF doesn't use mixed content, so not implimenting 7.
        }        
        // So we stop when we do not have a JSON element on which to build.
        if(null == jsonCurrent) {
            return;
        }
        // So we handle changes in namespace.
        if(0 != parentNamespace.compareTo(((Element) xmlCurrent).getNamespaceURI())) {
            parentNamespace = ((Element) xmlCurrent).getNamespaceURI();
            ((ObjectNode) jsonCurrent).put("@xmlns", parentNamespace);
        }
        // So we include lables by default.
        noLable = false;        
        // Recurse
        // So we keep going with the most current JSON node.
        if(null != arrayCurrent) {
            jsonCurrent = arrayCurrent;
        }
        // So we visit each child.
        for (int i = 0; i < xmlCurrent.getChildCount(); i++) {
            xml2jsonTree(xmlCurrent.getChild(i), parentNamespace, 
                    jsonCurrent, jsonMapper, noLable);
        } 
        // Tail
    }    
    
    /**
     * Looks at the XMl nodes immediate children and determines if they all have the same qualified name.
     * 
     * @param xmlCurrent  The current node in the tree we want to know if it is a list.
     * @return  True if all the children's qualified names match.  False if the children have different names.
     */
    private static boolean isList(Element xmlCurrent) {
        Elements childElements = xmlCurrent.getChildElements();
        int size = childElements.size();
        if(2 > size) {
            return false;
        }
        Element firstElement = childElements.get(0);
        Element currentElement = null;
        for(int i = 1; i < size; i++) {
            currentElement = childElements.get(i);
            if(0 != firstElement.getQualifiedName().compareTo(currentElement.getQualifiedName())) {
                return false;
            }
        }
        return true;
    }
}
