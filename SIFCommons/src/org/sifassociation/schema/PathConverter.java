/*
 * Conversion utilities between JsonPathPlus and XPathPlus
 */
package org.sifassociation.schema;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

/**
 * Utility class for converting between JSON and XML paths and their
 * metadata containers.
 * 
 * @author claude
 * @since 3.0
 */
public class PathConverter {
    
    /**
     * Convert a JSON path to an XPath
     * 
     * @param jsonPath JSON path (dot notation)
     * @return Equivalent XPath
     */
    public static String jsonPathToXPath(String jsonPath) {
        if (jsonPath == null || jsonPath.isEmpty()) {
            return "";
        }
        
        // Replace dots with slashes
        String xpath = jsonPath.replace('.', '/');
        
        // Handle array notation
        xpath = xpath.replace("[]", "");
        
        // Ensure path starts with /
        if (!xpath.startsWith("/")) {
            xpath = "/" + xpath;
        }
        
        return xpath;
    }
    
    /**
     * Convert an XPath to a JSON path
     * 
     * @param xpath XPath expression
     * @return Equivalent JSON path
     */
    public static String xpathToJsonPath(String xpath) {
        if (xpath == null || xpath.isEmpty()) {
            return "";
        }
        
        // Remove leading slash if present
        String jsonPath = xpath;
        if (jsonPath.startsWith("/")) {
            jsonPath = jsonPath.substring(1);
        }
        
        // Replace slashes with dots
        jsonPath = jsonPath.replace('/', '.');
        
        return jsonPath;
    }
    
    /**
     * Convert a JsonPathPlus object to an XPathPlus object
     * 
     * @param jsonPathPlus Source JsonPathPlus object
     * @return Equivalent XPathPlus object
     */
    public static XPathPlus convertToXPathPlus(JsonPathPlus jsonPathPlus) {
        if (jsonPathPlus == null) {
            return null;
        }
        
        XPathPlus xpathPlus = new XPathPlus();
        
        // Convert path
        xpathPlus.setPath(jsonPathToXPath(jsonPathPlus.getPath()));
        
        // Copy common properties
        xpathPlus.setMandatory(jsonPathPlus.isMandatory());
        xpathPlus.setDocumentation(jsonPathPlus.getDocumentation());
        xpathPlus.setType(jsonPathPlus.getType());
        xpathPlus.setEnumerations(jsonPathPlus.getEnumerations());
        xpathPlus.setPatterns(jsonPathPlus.getPatterns());
        xpathPlus.setRepeatable(jsonPathPlus.isRepeatable());
        xpathPlus.setMinLength(jsonPathPlus.getMinLength());
        xpathPlus.setMaxLength(jsonPathPlus.getMaxLength());
        
        // Copy metadata
        Map<String, String> metadata = new HashMap<>();
        if (jsonPathPlus.getMetadata() != null) {
            metadata.putAll(jsonPathPlus.getMetadata());
        }
        
        // Add JSON-specific properties as metadata
        if (jsonPathPlus.getTitle() != null && !jsonPathPlus.getTitle().isEmpty()) {
            metadata.put("title", jsonPathPlus.getTitle());
        }
        if (jsonPathPlus.getJsonType() != null && !jsonPathPlus.getJsonType().isEmpty()) {
            metadata.put("jsonType", jsonPathPlus.getJsonType());
        }
        if (jsonPathPlus.getFormat() != null && !jsonPathPlus.getFormat().isEmpty()) {
            metadata.put("format", jsonPathPlus.getFormat());
        }
        if (jsonPathPlus.getMinimum() != null && !jsonPathPlus.getMinimum().isEmpty()) {
            metadata.put("minimum", jsonPathPlus.getMinimum());
        }
        if (jsonPathPlus.getMaximum() != null && !jsonPathPlus.getMaximum().isEmpty()) {
            metadata.put("maximum", jsonPathPlus.getMaximum());
        }
        
        xpathPlus.setAppInfos(metadata);
        
        return xpathPlus;
    }
    
    /**
     * Convert an XPathPlus object to a JsonPathPlus object
     * 
     * @param xpathPlus Source XPathPlus object
     * @return Equivalent JsonPathPlus object
     */
    public static JsonPathPlus convertToJsonPathPlus(XPathPlus xpathPlus) {
        if (xpathPlus == null) {
            return null;
        }
        
        JsonPathPlus jsonPathPlus = new JsonPathPlus();
        
        // Convert path
        jsonPathPlus.setPath(xpathToJsonPath(xpathPlus.getPath()));
        
        // Copy common properties
        jsonPathPlus.setMandatory(xpathPlus.isMandatory());
        jsonPathPlus.setDocumentation(xpathPlus.getDocumentation());
        jsonPathPlus.setType(xpathPlus.getType());
        jsonPathPlus.setEnumerations(xpathPlus.getEnumerations());
        jsonPathPlus.setPatterns(xpathPlus.getPatterns());
        jsonPathPlus.setRepeatable(xpathPlus.isRepeatable());
        jsonPathPlus.setMinLength(xpathPlus.getMinLength());
        jsonPathPlus.setMaxLength(xpathPlus.getMaxLength());
        
        // Copy base metadata
        Map<String, String> metadata = new HashMap<>();
        if (xpathPlus.getAppInfos() != null) {
            metadata.putAll(xpathPlus.getAppInfos());
            
            // Extract JSON-specific properties from metadata
            if (metadata.containsKey("title")) {
                jsonPathPlus.setTitle(metadata.get("title"));
                metadata.remove("title");
            }
            if (metadata.containsKey("jsonType")) {
                jsonPathPlus.setJsonType(metadata.get("jsonType"));
                metadata.remove("jsonType");
            }
            if (metadata.containsKey("format")) {
                jsonPathPlus.setFormat(metadata.get("format"));
                metadata.remove("format");
            }
            if (metadata.containsKey("minimum")) {
                jsonPathPlus.setMinimum(metadata.get("minimum"));
                metadata.remove("minimum");
            }
            if (metadata.containsKey("maximum")) {
                jsonPathPlus.setMaximum(metadata.get("maximum"));
                metadata.remove("maximum");
            }
        }
        
        // Infer JSON type from XML type if possible
        if (xpathPlus.getType() != null) {
            QName type = xpathPlus.getType();
            String localPart = type.getLocalPart();
            
            if ("string".equals(localPart)) {
                jsonPathPlus.setJsonType("string");
            } else if ("integer".equals(localPart) || "int".equals(localPart) || "long".equals(localPart) || 
                       "short".equals(localPart) || "byte".equals(localPart)) {
                jsonPathPlus.setJsonType("integer");
            } else if ("decimal".equals(localPart) || "float".equals(localPart) || "double".equals(localPart)) {
                jsonPathPlus.setJsonType("number");
            } else if ("boolean".equals(localPart)) {
                jsonPathPlus.setJsonType("boolean");
            } else if ("anyType".equals(localPart)) {
                jsonPathPlus.setJsonType("object");
            }
        }
        
        jsonPathPlus.setMetadata(metadata);
        
        return jsonPathPlus;
    }
}