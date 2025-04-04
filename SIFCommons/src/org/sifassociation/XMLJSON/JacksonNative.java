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
        // No longer checking json-anonymous here—it's handled in elementToJson.
        JsonNode jsonRoot = elementToJson(rootElement);

        if ("true".equals(rootElement.getAttributeValue("json-anonymous")) &&
                "root".equals(rootElement.getLocalName())) {
            // Annonymous/Singular
            // So original JSON keys are restored.
            jsonRoot = postprocessJsonKeys(jsonRoot);            
        }
        else {  // Named/Collection
            // Wrap in an object keyed by the root element name.
            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.set(rootElement.getLocalName(), jsonRoot);

            // So original JSON keys are restored.
            jsonRoot = postprocessJsonKeys(wrapper);
        }

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
        // --- Special handling for json-anonymous elements ---
        if ("true".equals(element.getAttributeValue("json-anonymous"))) {
            // Gather non‑json-* attributes.
            ObjectNode anonAttrs = mapper.createObjectNode();
            int attrCount = element.getAttributeCount();
            for (int i = 0; i < attrCount; i++) {
                Attribute attr = element.getAttribute(i);
                String attrName = attr.getLocalName();
                if (!attrName.startsWith("json-")) {
                    anonAttrs.put("@" + attrName, attr.getValue());
                }
            }
            // Also capture any direct text.
            String textValue = getDirectText(element);
            if (!textValue.isEmpty()) {
                anonAttrs.put("#text", textValue);
            }
            Elements children = element.getChildElements();
            // If no children, return the attributes (and text) as the node.
            if (0 == children.size()) {
                return anonAttrs;
            }
            // Check if all children are named "item". If so, produce an array.
            boolean allItems = true;
            for (Element child : children) {
                if (!"item".equals(child.getLocalName())) {
                    allItems = false;
                    break;
                }
            }
            if (allItems) {
                ArrayNode arrayNode = mapper.createArrayNode();
                for (Element child : children) {
                    arrayNode.add(elementToJson(child));
                }
                // Merge anonymous attributes into each item.
                if (anonAttrs.size() > 0) {
                    for (int i = 0; i < arrayNode.size(); i++) {
                        JsonNode item = arrayNode.get(i);
                        if (item.isObject()) {
                            ((ObjectNode) item).setAll(anonAttrs);
                        }
                    }
                }
                return arrayNode;
            } else {
                // Otherwise, build an object:
                ObjectNode anonObj = mapper.createObjectNode();
                anonObj.setAll(anonAttrs);
                // Process children normally:
                Map<String, List<Element>> grouped = new LinkedHashMap<>();
                for (Element child : children) {
                    grouped.computeIfAbsent(child.getLocalName(), k -> new ArrayList<>()).add(child);
                }
                for (Map.Entry<String, List<Element>> entry : grouped.entrySet()) {
                    String childName = entry.getKey();
                    List<Element> childList = entry.getValue();
                    if (childList.size() == 1) {
                        anonObj.set(childName, elementToJson(childList.get(0)));
                    } else {
                        ArrayNode arr = mapper.createArrayNode();
                        for (Element c : childList) {
                            arr.add(elementToJson(c));
                        }
                        anonObj.set(childName, arr);
                    }
                }
                return anonObj;
            }
        }

        // --- Normal processing for non-anonymous elements ---
        Elements children = element.getChildElements();
        int attrCount = element.getAttributeCount();
        String textValue = getDirectText(element);

        // If there are no children, handle json-null, json-number, or just return text.
        if (0 == children.size()) {
            String jsonNullAttr = element.getAttributeValue("json-null");
            if ("true".equals(jsonNullAttr)) {
                return NullNode.instance;
            } else if ("false".equals(jsonNullAttr)) {
                return new TextNode("");
            }
            if ("true".equals(element.getAttributeValue("json-number"))) {
                try {
                    return new DoubleNode(Double.parseDouble(textValue));
                } catch (NumberFormatException e) {
                    LOG.log(Level.WARNING, "Invalid number format", e);
                    return NullNode.instance;
                }
            }
            // No children and possibly some attributes: return either a simple text node or an object.
            if (attrCount == 0) {
                return new TextNode(textValue);
            }
            ObjectNode simpleObj = mapper.createObjectNode();
            for (int i = 0; i < attrCount; i++) {
                Attribute attr = element.getAttribute(i);
                String attrName = attr.getLocalName();
                if (!attrName.startsWith("json-")) {
                    simpleObj.put("@" + attrName, attr.getValue());
                }
            }
            if (!textValue.isEmpty()) {
                simpleObj.put("#text", textValue);
            }
            return simpleObj;
        }

        // If the element is explicitly marked as a JSON array.
        if ("true".equals(element.getAttributeValue("json-array"))) {
            ArrayNode arrayNode = mapper.createArrayNode();
            for (Element child : children) {
                arrayNode.add(elementToJson(child));
            }
            return arrayNode;
        }

        // Otherwise, process as an object.
        ObjectNode obj = mapper.createObjectNode();
        // Process non-json-* attributes.
        for (int i = 0; i < attrCount; i++) {
            Attribute attr = element.getAttribute(i);
            String localName = attr.getLocalName();
            if (!localName.startsWith("json-")) {
                obj.put("@" + localName, attr.getValue());
            }
        }
        if (!textValue.isEmpty() && !"true".equals(element.getAttributeValue("json-number"))) {
            obj.put("#text", textValue);
        } else if ("true".equals(element.getAttributeValue("json-number"))) {
            try {
                return new DoubleNode(Double.parseDouble(textValue));
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "Invalid number format in json-number", e);
                return NullNode.instance;
            }
        }
        // Group child elements by their local name.
        Map<String, List<Element>> groupedChildren = new LinkedHashMap<>();
        for (Element child : children) {
            groupedChildren.computeIfAbsent(child.getLocalName(), k -> new ArrayList<>()).add(child);
        }
        // --- Special grouping: if the only child group is "root" and each such child is json-anonymous,
        // then treat that group like an array (i.e. drop the "root" key and return an array of its converted items) ---
        if (groupedChildren.size() == 1 && groupedChildren.containsKey("root") &&
            groupedChildren.get("root").stream().allMatch(e -> "true".equals(e.getAttributeValue("json-anonymous")))) {
            ArrayNode arrayNode = mapper.createArrayNode();
            for (Element rootChild : groupedChildren.get("root")) {
                arrayNode.add(elementToJson(rootChild));
            }
            return arrayNode;
        }
        // Default: add each group to the object.
        for (Map.Entry<String, List<Element>> entry : groupedChildren.entrySet()) {
            String childName = entry.getKey();
            List<Element> childList = entry.getValue();
            if (childList.size() == 1) {
                obj.set(childName, elementToJson(childList.get(0)));
            } else {
                ArrayNode arr = mapper.createArrayNode();
                for (Element c : childList) {
                    arr.add(elementToJson(c));
                }
                obj.set(childName, arr);
            }
        }
        return obj;
    }

    private void addChildToObjectNode(ObjectNode parent, Element child) {
        String key = child.getLocalName();
        JsonNode childJson = elementToJson(child);
        if (parent.has(key)) {
            JsonNode existing = parent.get(key);
            ArrayNode arrayNode;
            if (existing.isArray()) {
                arrayNode = (ArrayNode) existing;
            } else {
                arrayNode = mapper.createArrayNode();
                arrayNode.add(existing);
                parent.set(key, arrayNode);
            }
            arrayNode.add(childJson);
        } else {
            parent.set(key, childJson);
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