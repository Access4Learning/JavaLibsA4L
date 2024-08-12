/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sifassociation.XMLJSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import org.sifassociation.util.SIFXOMUtil;

public class PESCNative implements IXmlJson {
    private static final PESCNative INSTANCE = new PESCNative();
    
    private PESCNative() {
    }

    public static PESCNative getInstance() {
        return INSTANCE;
    }
    
    @Override
    public String json2xml(String json) {
        // So we build the diresired XML tree.
        // SIF Unity uses PESC JSON Notation
        // Rules for JSON to XML Conversion
	// 1   "e": null -> <e/>
        //      A JSON key with a null value translates to an empty XML element.
	// 2   "e": "text" -> <e>text</e>
	//      A JSON key with a string value translates to an XML element with text content.
	// 3   "e": {"@name": "value"} -> <e name="value" />
	//      A JSON key with an object containing keys starting with @ translates to an XML element with attributes.
	// 4   "e": {"@name": "value", "#text": "text"} -> <e name="value">text</e>
	//      A JSON key with an object containing both attributes (keys starting with @) and a #text key translates to an XML element with attributes and text content.
	// 5   "e": {"a": "text", "b": "text"} -> <e> <a>text</a> <b>text</b> </e>
	//      A JSON key with an object containing other keys translates to an XML element with nested child elements.
	// 6   "e": {"a": ["text", "text"]} -> <e> <a>text</a> <a>text</a> </e>
	//      A JSON key with an object containing an array translates to an XML element with repeated child elements.
        
        return json2xml(json, null, null);
    }

    public String json2xml(String json, List<String> attributePaths, Map<String, String> namespaces) {
        if(null == json || json.isEmpty()) {
            return "";
        }
        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode jsonRoot;
        try {
            jsonRoot = jsonMapper.readTree(json);
        } catch (IOException ex) {
            Logger.getLogger(PESCNative.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        
        Element xmlRoot;
        if (attributePaths == null || attributePaths.isEmpty()) {
            xmlRoot = json2xmlTree(jsonRoot, namespaces);
        } else {
            xmlRoot = json2xmlTreeWithFilter(jsonRoot, attributePaths, namespaces);
        }
        
        String xml = SIFXOMUtil.pretty(xmlRoot);
        return xml;
    }

    private Element json2xmlTree(JsonNode jsonCurrent, Map<String, String> namespaces) {
        if(null == jsonCurrent) {
            return null;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = jsonCurrent.fields();
        Map.Entry<String, JsonNode> next = null;
        if(fields.hasNext()) {
            next = fields.next();
        }
        if(null != next) {
            return json2xmlTree(next.getKey(), next.getValue(), "", namespaces);
        }
        return null;
    }
    
    private Element json2xmlTreeWithFilter(JsonNode jsonCurrent, List<String> attributePaths, Map<String, String> namespaces) {
        if(null == jsonCurrent) {
            return null;
        }
        ObjectNode filteredJson = new ObjectMapper().createObjectNode();
        for (String path : attributePaths) {
            JsonNode value = getValueByPath(jsonCurrent, path.split("\\."));
            if (value != null) {
                setValueByPath(filteredJson, path.split("\\."), value);
            }
        }
        return json2xmlTree(filteredJson, namespaces);
    }

    private Element json2xmlTree(String name, JsonNode jsonCurrent, String namespace, Map<String, String> namespaces) {
        if (namespaces != null && namespaces.containsKey(name)) {
            namespace = namespaces.get(name);
        }
        Element xmlCurrent = new Element(name, namespace);
        
        // Rule 1: "e": null -> <e/>
        // Rule 2: "e": "text" -> <e>text</e>
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
            String nextName = next.getKey();
            JsonNode nextNode = next.getValue();

            // Rule 3: "e":{"@name": "value"} -> <e name="value" />
            if(nextName.startsWith("@")) {
                if(0 != "@xmlns".compareTo(nextName)) {
                    nextName = nextName.substring(1);
                    xmlCurrent.addAttribute(new Attribute(nextName, nextNode.textValue()));
                }
            }
            // Rule 4: "e": { "@name": "value", "#text": "text" } -> <e name="value">text</e>
            else if(0 == "#text".compareTo(nextName)) {
                xmlCurrent.appendChild(nextNode.textValue());
            }
            // Rule 6: "e": { "a": ["text", "text"] } -> <e> <a>text</a> <a>text</a> </e>
            else if(nextNode instanceof ArrayNode) {
                for(int i = 0; i < ((ArrayNode)nextNode).size(); i++) {
                    xmlCurrent.appendChild(json2xmlTree(nextName, ((ArrayNode)nextNode).get(i), namespace, namespaces));
                }
            }
            // Rule 5: "e": { "a": "text", "b": "text" } -> <e> <a>text</a> <b>text</b> </e>
            else {
                xmlCurrent.appendChild(json2xmlTree(nextName, nextNode, namespace, namespaces));
            }
        }
        
        return xmlCurrent;        
    }

    private JsonNode getValueByPath(JsonNode jsonCurrent, String[] path) {
        JsonNode currentNode = jsonCurrent;
        for (int i = 0; i < path.length; i++) {
            currentNode = currentNode.path(path[i]);
            if (currentNode.isMissingNode()) {
                return null;
            }
        }
        return currentNode;
    }

    private void setValueByPath(ObjectNode jsonCurrent, String[] path, JsonNode value) {
        ObjectNode currentNode = jsonCurrent;
        for (int i = 0; i < path.length - 1; i++) {
            if (!currentNode.has(path[i])) {
                currentNode.set(path[i], new ObjectMapper().createObjectNode());
            }
            currentNode = (ObjectNode) currentNode.get(path[i]);
        }
        currentNode.set(path[path.length - 1], value);
    }

    private List<String> getAttributePathsFromFile(String filePath, String rootElement) {
        List<String> attributePaths = new ArrayList<>();
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(content);
            JsonNode rootPaths = rootNode.path(rootElement).path("attributePaths");

            if (rootPaths.isArray()) {
                for (JsonNode pathNode : rootPaths) {
                    attributePaths.add(pathNode.asText());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PESCNative.class.getName()).log(Level.SEVERE, null, ex);
        }
        return attributePaths;
    }

    private List<String> getRepeatableElementsFromFile(String filePath, String rootElement) {
        List<String> repeatableElements = new ArrayList<>();
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(content);
            JsonNode rootRepeatables = rootNode.path(rootElement).path("repeatableElements");

            if (rootRepeatables.isArray()) {
                for (JsonNode repeatableNode : rootRepeatables) {
                    repeatableElements.add(repeatableNode.asText());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PESCNative.class.getName()).log(Level.SEVERE, null, ex);
        }
        return repeatableElements;
    }

    private Map<String, String> getNamespacesFromFile(String filePath, String rootElement) {
        Map<String, String> namespaces = new HashMap<>();
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(content);
            JsonNode rootNamespaces = rootNode.path(rootElement).path("namespaces");

            if (rootNamespaces.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = rootNamespaces.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    namespaces.put(field.getKey(), field.getValue().asText());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PESCNative.class.getName()).log(Level.SEVERE, null, ex);
        }
        return namespaces;
    }

    @Override
    public String xml2json(String xml) {
        // So we build the diresired JSON tree.
        // SIF Unity uses PESC JSON
        // See: http://specification.sifassociation.org/Implementation/NA/4.3/background/PESCJSON.pdf
        // Rules for XML to JSON Conversion
	// 1    <e/> -> "e": null
	//      An empty XML element translates to a JSON key with a null value.
	// 2    <e>text</e> -> "e": "text"
	//      An XML element with text content translates to a JSON key with a string value.
	// 3    <e name="value" /> -> "e": {"@name": "value"}
	//      An XML element with attributes translates to a JSON key with an object containing keys starting with @ for each attribute.
	// 4    <e name="value">text</e> -> "e": {"@name": "value", "#text": "text"}
	//      An XML element with both attributes and text content translates to a JSON key with an object containing both attribute keys (starting with @) and a #text key for the text content.
	// 5    <e> <a>text</a> <b>text</b> </e> -> "e": {"a": "text", "b": "text"}
	//      An XML element with nested child elements translates to a JSON key with an object containing other keys representing the child elements.
	// 6    <e> <a>text</a> <a>text</a> </e> -> "e": {"a": ["text", "text"]}
	//      Repeated XML child elements translate to a JSON key with an object containing an array for the repeated elements.
        // To Do:  Use the above to create a set of tests for the interface
        
        return xml2json(xml, null, null, null);
    }
    
    public String xml2json(String xml, List<String> attributePaths, List<String> repeatableElements, List<String> mixedContent) {
        if (null == xml || xml.isEmpty()) {
            return "";
        }
        Builder builder = new Builder();
        Document doc;
        try {
            doc = builder.build(xml, null);
        } catch (Exception ex) {
            Logger.getLogger(PESCNative.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }

        Element rootElement = doc.getRootElement();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonRoot = mapper.createObjectNode();

        if (attributePaths == null || attributePaths.isEmpty()) {
            xml2jsonTree(rootElement, jsonRoot, repeatableElements, mixedContent);
        } else {
            xml2jsonTreeWithFilter(rootElement, jsonRoot, attributePaths, repeatableElements, mixedContent);
        }

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonRoot);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(PESCNative.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
    }

    private void xml2jsonTree(Element xmlCurrent, ObjectNode jsonCurrent, List<String> repeatableElements, List<String> mixedContent) {
        Elements children = xmlCurrent.getChildElements();

        // Rule 1 and Rule 2: Handle text content or empty elements
        if (children.size() == 0) {
            if (xmlCurrent.getValue() != null && !xmlCurrent.getValue().isEmpty()) {
                jsonCurrent.put(xmlCurrent.getLocalName(), xmlCurrent.getValue());
            } else {
                jsonCurrent.putNull(xmlCurrent.getLocalName());
            }
        }

        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            String currentPath = xmlCurrent.getLocalName() + "." + child.getLocalName();
            if (isRepeatableElement(repeatableElements, currentPath)) {
                ArrayNode arrayNode;
                if (jsonCurrent.has(child.getLocalName())) {
                    arrayNode = (ArrayNode) jsonCurrent.get(child.getLocalName());
                } else {
                    arrayNode = jsonCurrent.putArray(child.getLocalName());
                }
                ObjectNode childJson = new ObjectMapper().createObjectNode();
                arrayNode.add(childJson);
                xml2jsonTree(child, childJson, repeatableElements, mixedContent);
            } else {
                ObjectNode childJson = jsonCurrent.putObject(child.getLocalName());
                xml2jsonTree(child, childJson, repeatableElements, mixedContent);
            }
        }

        // Rule 3: Handle attributes
        for (int i = 0; i < xmlCurrent.getAttributeCount(); i++) {
            Attribute attribute = xmlCurrent.getAttribute(i);
            jsonCurrent.put("@" + attribute.getLocalName(), attribute.getValue());
        }

        // Rule 4 and Rule 6: Handle mixed content and arrays
        if (xmlCurrent.getValue() != null && !xmlCurrent.getValue().isEmpty()) {
            if (isMixedContentElement(mixedContent, xmlCurrent.getLocalName())) {
                jsonCurrent.put("#text", xmlCurrent.getValue());
            } else {
                jsonCurrent.put(xmlCurrent.getLocalName(), xmlCurrent.getValue());
            }
        }
    }

    private void xml2jsonTreeWithFilter(Element xmlCurrent, ObjectNode jsonCurrent, List<String> attributePaths, List<String> repeatableElements, List<String> mixedContent) {
        Elements children = xmlCurrent.getChildElements();

        // Rule 1 and Rule 2: Handle text content or empty elements
        if (children.size() == 0) {
            if (xmlCurrent.getValue() != null && !xmlCurrent.getValue().isEmpty()) {
                jsonCurrent.put(xmlCurrent.getLocalName(), xmlCurrent.getValue());
            } else {
                jsonCurrent.putNull(xmlCurrent.getLocalName());
            }
        }

        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            String currentPath = xmlCurrent.getLocalName() + "." + child.getLocalName();
            if (isPathInFilter(attributePaths, currentPath)) {
                if (isRepeatableElement(repeatableElements, currentPath)) {
                    ArrayNode arrayNode;
                    if (jsonCurrent.has(child.getLocalName())) {
                        arrayNode = (ArrayNode) jsonCurrent.get(child.getLocalName());
                    } else {
                        arrayNode = jsonCurrent.putArray(child.getLocalName());
                    }
                    ObjectNode childJson = new ObjectMapper().createObjectNode();
                    arrayNode.add(childJson);
                    xml2jsonTreeWithFilter(child, childJson, attributePaths, repeatableElements, mixedContent);
                } else {
                    ObjectNode childJson = jsonCurrent.putObject(child.getLocalName());
                    xml2jsonTreeWithFilter(child, childJson, attributePaths, repeatableElements, mixedContent);
                }
            }
        }

        // Rule 3: Handle attributes
        for (int i = 0; i < xmlCurrent.getAttributeCount(); i++) {
            Attribute attribute = xmlCurrent.getAttribute(i);
            String attributePath = xmlCurrent.getLocalName() + ".@" + attribute.getLocalName();
            if (isPathInFilter(attributePaths, attributePath)) {
                jsonCurrent.put("@" + attribute.getLocalName(), attribute.getValue());
            }
        }

        // Rule 4 and Rule 6: Handle mixed content and arrays
        if (xmlCurrent.getValue() != null && !xmlCurrent.getValue().isEmpty()) {
            if (isMixedContentElement(mixedContent, xmlCurrent.getLocalName())) {
                jsonCurrent.put("#text", xmlCurrent.getValue());
            } else {
                jsonCurrent.put(xmlCurrent.getLocalName(), xmlCurrent.getValue());
            }
        }
    }

    private boolean isPathInFilter(List<String> attributePaths, String path) {
        for (String attributePath : attributePaths) {
            if (attributePath.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRepeatableElement(List<String> repeatableElements, String path) {
        return repeatableElements.contains(path);
    }

    private boolean isMixedContentElement(List<String> mixedContent, String elementName) {
        return mixedContent.contains(elementName);
    }

}