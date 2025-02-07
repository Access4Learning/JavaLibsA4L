package org.sifassociation.XMLJSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import nu.xom.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sifassociation.util.SIFXOMUtil;

// Note:  This uses Jackson to support JSON first round-triping.
public class JacksonNative implements IXmlJson {

    private static final String PREFIX = "escaped_"; // Prefix for escaped keys
    private static final Pattern HEX_PATTERN = Pattern.compile("__([0-9A-Fa-f]{2})__");
    
    private static final JacksonNative INSTANCE = new JacksonNative();

    private JacksonNative() {
    }

    public static JacksonNative getInstance() {
        return INSTANCE;
    }

    private static final Logger LOG = Logger.getLogger(JacksonNative.class.getName());

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String json2xml(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "";
        }

        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(json);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Failed to parse JSON", ex);
            return "";
        }

        // So invalid XML tag names are converted to something compatible.
        rootNode = preprocessJsonKeys(rootNode);
        
        Element xmlRoot = new Element("root");
        xmlRoot.addAttribute(new Attribute("json-anonymous", "true"));

        if (rootNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) rootNode;
            for (JsonNode item : arrayNode) {
                xmlRoot.appendChild(convertJsonToXml("item", item));
            }
        } else if (rootNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                xmlRoot.appendChild(convertJsonToXml(entry.getKey(), entry.getValue()));
            }
        } else {
            Element valueElement = new Element("value");
            valueElement.appendChild(rootNode.asText());
            xmlRoot.appendChild(valueElement);
        }

        return SIFXOMUtil.pretty(xmlRoot);
    }
    
    // Method to preprocess JSON keys to ensure valid XML tag names
    private JsonNode preprocessJsonKeys(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            // Create a new ObjectNode to store processed keys
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode updatedNode = objectMapper.createObjectNode();

            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String originalKey = entry.getKey();
                String validKey = escapeInvalidXmlName(originalKey); // Transform the key
                ((com.fasterxml.jackson.databind.node.ObjectNode) updatedNode).set(validKey, preprocessJsonKeys(entry.getValue()));
            }
            return updatedNode;
        } else if (jsonNode.isArray()) {
            // Recursively process array elements
            ObjectMapper objectMapper = new ObjectMapper();
            com.fasterxml.jackson.databind.node.ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode element : jsonNode) {
                arrayNode.add(preprocessJsonKeys(element));
            }
            return arrayNode;
        } else {
            // Return as-is for primitive types
            return jsonNode;
        }
    }

    /**
     * Escapes an invalid XML name to make it valid.
     * Encodes invalid characters in a reversible format.
     *
     * @param name the original name
     * @return a valid XML name
     * @throws IllegalArgumentException if the name is null or empty
     */
    private String escapeInvalidXmlName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        // Validate if the name is a valid XML name
        boolean isValid = true;

        // XML names must start with a letter or underscore
        char firstChar = name.charAt(0);
        if (!Character.isLetter(firstChar) && firstChar != '_') {
            isValid = false;
        }

        // Check the rest of the characters
        if (isValid) {
            for (int i = 1; i < name.length(); i++) {
                char c = name.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != '-' && c != '_' && c != '.') {
                    isValid = false;
                    break;
                }
            }
        }

        // If valid, return the original name
        if (isValid) {
            return name;
        }

        // Escape invalid characters
        StringBuilder escapedName = new StringBuilder(PREFIX);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.') {
                escapedName.append(c);
            } else {
                // Encode invalid character as __HEX__
                escapedName.append("__").append(String.format("%02X", (int) c)).append("__");
            }
        }

        return escapedName.toString();
    }
    
    private Element convertJsonToXml(String key, JsonNode node) {
        Element element = new Element(key);

        if (node.isValueNode()) {
            if (node.isNumber()) {
                element.addAttribute(new Attribute("json-number", "true"));
                element.appendChild(node.asText());
            } else if (node.isTextual()) {
                if (node.asText().isEmpty()) {
                    element.addAttribute(new Attribute("json-null", "false"));
                }
                element.appendChild(node.asText());
            } else if (!node.isNull()) {
                element.appendChild(node.asText());
            }
            return element;
        }

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String fieldName = entry.getKey();
                JsonNode fieldValue = entry.getValue();

                if (fieldName.startsWith("@")) {
                    element.addAttribute(new Attribute(fieldName.substring(1), fieldValue.asText()));
                } else if ("#text".equals(fieldName)) {
                    element.appendChild(fieldValue.asText());
                } else {
                    element.appendChild(convertJsonToXml(fieldName, fieldValue));
                }
            }
        } else if (node.isArray()) {
            element.addAttribute(new Attribute("json-array", "true"));
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode item : arrayNode) {
                Element child = convertJsonToXml("entry", item);
                element.appendChild(child);
            }
            return element;
        }

        return element;
    }

 /******************************************************************************/
    
    @Override
    public String xml2json(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return "";
        }

        Builder builder = new Builder();
        Document doc;
        try {
            doc = builder.build(xml, null);
        } catch (ParsingException | IOException ex) {
            LOG.log(Level.SEVERE, "Failed to parse XML", ex);
            return "";
        }

        Element rootElement = doc.getRootElement();
        JsonNode jsonRoot;

        if ("true".equals(rootElement.getAttributeValue("json-anonymous"))) {
            Elements children = rootElement.getChildElements();
            if (children.size() == 0) {
                jsonRoot = mapper.createArrayNode();
            } else {
                boolean allItems = true;
                for (int i = 0; i < children.size(); i++) {
                    if (!"item".equals(children.get(i).getLocalName())) {
                        allItems = false;
                        break;
                    }
                }
                if (allItems) {
                    ArrayNode arrayNode = mapper.createArrayNode();
                    for (int i = 0; i < children.size(); i++) {
                        arrayNode.add(elementToJson(children.get(i)));
                    }
                    jsonRoot = arrayNode;
                } else {
                    ObjectNode objectNode = mapper.createObjectNode();
                    for (int i = 0; i < children.size(); i++) {
                        addChildToObjectNode(objectNode, children.get(i));
                    }
                    jsonRoot = objectNode;
                }
            }
        } else {
            ObjectNode objectNode = mapper.createObjectNode();
            addChildToObjectNode(objectNode, rootElement);
            jsonRoot = objectNode;
        }

        // So original JSON keys are restored.
        jsonRoot = postprocessJsonKeys(jsonRoot);
        
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonRoot);
        } catch (JsonProcessingException ex) {
            LOG.log(Level.SEVERE, "Failed to write JSON", ex);
            return "";
        }
    }

    // Method to posttrocess XML names to restore the JSON key names.
    private JsonNode postprocessJsonKeys(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            // Create a new ObjectNode to store processed keys
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode updatedNode = objectMapper.createObjectNode();

            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String validKey = entry.getKey();
                String originalKey = restoreJsonName(validKey);  // Transform the key
                ((com.fasterxml.jackson.databind.node.ObjectNode) updatedNode).set(originalKey, postprocessJsonKeys(entry.getValue()));
            }
            return updatedNode;
        } else if (jsonNode.isArray()) {
            // Recursively process array elements
            ObjectMapper objectMapper = new ObjectMapper();
            com.fasterxml.jackson.databind.node.ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode element : jsonNode) {
                arrayNode.add(postprocessJsonKeys(element));
            }
            return arrayNode;
        } else {
            // Return as-is for primitive types
            return jsonNode;
        }
    }
    
    /**
     * Restores the original tag name from its escaped form.
     *
     * @param escapedTagName the escaped tag name
     * @return the original tag name
     */
    private String restoreJsonName(String escapedTagName) {
        if (!escapedTagName.startsWith(PREFIX)) {
            return escapedTagName; // Not an escaped tag
        }

        String rawName = escapedTagName.substring(PREFIX.length());

        // Replace __HEX__ sequences with their corresponding characters
        Matcher matcher = HEX_PATTERN.matcher(rawName);
        StringBuffer restored = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1); // Extract the hex value (e.g., "40")
            int charCode = Integer.parseInt(hex, 16); // Convert hex to character code
            matcher.appendReplacement(restored, String.valueOf((char) charCode)); // Append decoded character
        }
        matcher.appendTail(restored);

        return restored.toString();
    }    
    
    private JsonNode elementToJson(Element element) {
        Elements children = element.getChildElements();
        int attrCount = element.getAttributeCount();
        String textValue = getDirectText(element);

        if (children.size() == 0) {
            // Check for the json-null attribute explicitly
            String jsonNullAttr = element.getAttributeValue("json-null");
            if ("true".equals(jsonNullAttr)) {
                return NullNode.instance; // Explicit null
            } else if ("false".equals(jsonNullAttr)) {
                return new TextNode(""); // Explicit empty string
            }

            // Check for the json-number attribute explicitly
            String jsonNumberAttr = element.getAttributeValue("json-number");
            if ("true".equals(jsonNumberAttr)) {
                try {
                    // Parse the text as a number
                    return new DoubleNode(Double.parseDouble(textValue));
                } catch (NumberFormatException e) {
                    LOG.log(Level.WARNING, "Invalid number format", e);
                    return NullNode.instance; // Invalid number treated as null
                }
            }

            // Handle case where the element has no meaningful children or attributes
            if (attrCount == 0) {
                if (textValue.isEmpty()) {
                    return new TextNode(""); // Treat as empty string
                } else {
                    return new TextNode(textValue); // Return the text value
                }
            }

            // If the element has attributes other than json-* metadata
            ObjectNode obj = mapper.createObjectNode();
            for (int i = 0; i < attrCount; i++) {
                Attribute attr = element.getAttribute(i);
                String attrName = attr.getLocalName();
                if (!attrName.startsWith("json-")) {
                    obj.put("@" + attrName, attr.getValue()); // Add non-metadata attributes
                }
            }
            if (!textValue.isEmpty()) {
                obj.put("#text", textValue); // Include the direct text value, if present
            }
            return obj;
        }

        if (children.size() == 0 && attrCount == 1 && "true".equals(element.getAttributeValue("json-number"))) {
            try {
                return new DoubleNode(Double.parseDouble(textValue));
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "Invalid number format in json-number", e);
                return NullNode.instance;
            }
        }

        if ("true".equals(element.getAttributeValue("json-array"))) {
            ArrayNode arrayNode = mapper.createArrayNode();
            for (int i = 0; i < children.size(); i++) {
                arrayNode.add(elementToJson(children.get(i)));
            }
            return arrayNode;
        }

        ObjectNode objectNode = mapper.createObjectNode();

        for (int i = 0; i < attrCount; i++) {
            Attribute attr = element.getAttribute(i);
            if (!"json-array".equals(attr.getLocalName()) && !"json-null".equals(attr.getLocalName()) && !"json-number".equals(attr.getLocalName())) {
                objectNode.put("@" + attr.getLocalName(), attr.getValue());
            }
        }

        if (!textValue.isEmpty() && !"true".equals(element.getAttributeValue("json-number"))) {
            objectNode.put("#text", textValue);
        } else if ("true".equals(element.getAttributeValue("json-number"))) {
            try {
                return new DoubleNode(Double.parseDouble(textValue));
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "Invalid number format in json-number", e);
                return NullNode.instance;
            }
        }

        Map<String, List<Element>> groupedChildren = new LinkedHashMap<>();
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            groupedChildren.computeIfAbsent(child.getLocalName(), k -> new ArrayList<>()).add(child);
        }

        for (Map.Entry<String, List<Element>> entry : groupedChildren.entrySet()) {
            String childName = entry.getKey();
            List<Element> childElements = entry.getValue();
            if (childElements.size() > 1) {
                ArrayNode arrayNode = mapper.createArrayNode();
                for (Element child : childElements) {
                    arrayNode.add(elementToJson(child));
                }
                objectNode.set(childName, arrayNode);
            } else {
                objectNode.set(childName, elementToJson(childElements.get(0)));
            }
        }

        return objectNode;
    }

    private void addChildToObjectNode(ObjectNode parentObj, Element childEl) {
        String childName = childEl.getLocalName();

        if (parentObj.has(childName)) {
            JsonNode existing = parentObj.get(childName);
            if (!existing.isArray()) {
                ArrayNode newArray = mapper.createArrayNode();
                newArray.add(existing);
                parentObj.set(childName, newArray);
            }
            ((ArrayNode) parentObj.get(childName)).add(elementToJson(childEl));
        } else {
            parentObj.set(childName, elementToJson(childEl));
        }
    }

    private static String getDirectText(Element element) {
        StringBuilder sb = new StringBuilder();
        int childCount = element.getChildCount();
        for (int i = 0; i < childCount; i++) {
            Node n = element.getChild(i);
            if (n instanceof Text) {
                sb.append(((Text) n).getValue());
            }
        }
        return sb.toString().trim();
    }
}