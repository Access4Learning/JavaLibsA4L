import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A simplified JSON Schema analyzer that doesn't require our full utility classes
 */
public class JsonSchemaAnalyzer {
    
    // Define a class to hold JSON path information
    static class JsonPathInfo {
        String path;
        boolean required;
        String type;
        String format;
        boolean isArray;
        String description;
        String enumValues;
        String pattern;
        String minLength;
        String maxLength;
        
        public JsonPathInfo(String path) {
            this.path = path;
        }
    }
    
    public static void main(String[] args) throws Exception {
        // Set up Jackson
        ObjectMapper mapper = new ObjectMapper();
        
        // Path to our JSON Schema file
        String schemaPath = "resources/examples/student.schema.json";
        System.out.println("\n============= JSON SCHEMA ANALYSIS DEMONSTRATION =============\n");
        System.out.println("Analyzing JSON Schema file: " + schemaPath);
        
        // Load the schema
        JsonNode schema = mapper.readTree(new File(schemaPath));
        System.out.println("Schema loaded successfully! Root type: " + schema.getNodeType());
        
        // Collect paths
        List<JsonPathInfo> paths = new ArrayList<>();
        collectPaths(schema, "$", new HashMap<>(), paths);
        
        // Print summary
        System.out.println("\nFound " + paths.size() + " JSON paths in the schema");
        
        // Print summary table of fields
        System.out.println("\n===== JSON Schema Field Summary Table =====");
        System.out.printf("%-40s | %-10s | %-12s | %-12s | %-8s | %s\n", 
                "JSON Path", "Required", "Type", "Format", "Array", "Description");
        System.out.println(String.format("%s+%s+%s+%s+%s+%s", 
                "----------------------------------------", 
                "------------", 
                "--------------", 
                "--------------",
                "----------",
                "--------------------"));
        
        for (JsonPathInfo path : paths) {
            // Skip very deep paths to keep the table readable
            if (path.path.split("\\.").length > 5) continue;
            
            System.out.printf("%-40s | %-10s | %-12s | %-12s | %-8s | %s\n",
                    truncateString(path.path, 40),
                    path.required ? "Required" : "Optional",
                    path.type != null ? path.type : "",
                    truncateString(path.format, 12),
                    path.isArray ? "Yes" : "No",
                    truncateString(path.description, 30));
        }
        
        // Print validation constraints table
        System.out.println("\n===== JSON Schema Validation Constraints =====");
        System.out.printf("%-40s | %-30s | %-30s | %s\n", 
                "JSON Path", "Enum Values", "Pattern", "Length Constraints");
        System.out.println(String.format("%s+%s+%s+%s", 
                "----------------------------------------", 
                "--------------------------------", 
                "--------------------------------",
                "--------------------"));
        
        for (JsonPathInfo path : paths) {
            // Only show paths with constraints
            if ((path.enumValues != null && !path.enumValues.isEmpty()) ||
                (path.pattern != null && !path.pattern.isEmpty()) ||
                (path.minLength != null && !path.minLength.isEmpty()) ||
                (path.maxLength != null && !path.maxLength.isEmpty())) {
                
                String lengthConstraints = "";
                if (path.minLength != null && !path.minLength.isEmpty()) {
                    lengthConstraints += "min=" + path.minLength;
                }
                if (path.maxLength != null && !path.maxLength.isEmpty()) {
                    if (!lengthConstraints.isEmpty()) {
                        lengthConstraints += ", ";
                    }
                    lengthConstraints += "max=" + path.maxLength;
                }
                
                System.out.printf("%-40s | %-30s | %-30s | %s\n",
                        truncateString(path.path, 40),
                        truncateString(path.enumValues, 30),
                        truncateString(path.pattern, 30),
                        truncateString(lengthConstraints, 30));
            }
        }
    }
    
    /**
     * Recursive function to collect all paths in the JSON Schema
     */
    private static void collectPaths(JsonNode node, String path, Map<String, List<String>> requiredProps, List<JsonPathInfo> paths) {
        if (node.has("type")) {
            // Create a path info for this node
            JsonPathInfo info = new JsonPathInfo(path);
            
            // Set basic properties
            info.type = node.get("type").asText();
            info.isArray = "array".equals(info.type);
            
            // Get required status
            String parentPath = getParentPath(path);
            String propertyName = getPropertyName(path);
            info.required = isRequired(parentPath, propertyName, requiredProps);
            
            // Get description
            if (node.has("description")) {
                info.description = node.get("description").asText();
            }
            
            // Get format
            if (node.has("format")) {
                info.format = node.get("format").asText();
            }
            
            // Get validation constraints
            if (node.has("enum") && node.get("enum").isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode value : node.get("enum")) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(value.asText());
                }
                info.enumValues = sb.toString();
            }
            
            if (node.has("pattern")) {
                info.pattern = node.get("pattern").asText();
            }
            
            if (node.has("minLength")) {
                info.minLength = node.get("minLength").asText();
            }
            
            if (node.has("maxLength")) {
                info.maxLength = node.get("maxLength").asText();
            }
            
            paths.add(info);
        }
        
        // Handle object properties
        if (node.has("properties") && node.get("properties").isObject()) {
            JsonNode properties = node.get("properties");
            
            // Collect required properties
            List<String> required = new ArrayList<>();
            if (node.has("required") && node.get("required").isArray()) {
                for (JsonNode req : node.get("required")) {
                    required.add(req.asText());
                }
            }
            requiredProps.put(path, required);
            
            // Process each property
            Iterator<String> fieldNames = properties.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                String childPath = path.equals("$") ? "$." + fieldName : path + "." + fieldName;
                collectPaths(properties.get(fieldName), childPath, requiredProps, paths);
            }
        }
        
        // Handle array items
        if (node.has("items")) {
            String itemsPath = path + "[]";
            collectPaths(node.get("items"), itemsPath, requiredProps, paths);
        }
        
        // Handle additionalProperties
        if (node.has("additionalProperties") && node.get("additionalProperties").isObject()) {
            String addlPath = path + ".*";
            collectPaths(node.get("additionalProperties"), addlPath, requiredProps, paths);
        }
    }
    
    /**
     * Check if a property is required
     */
    private static boolean isRequired(String parentPath, String propertyName, Map<String, List<String>> requiredProps) {
        if (requiredProps.containsKey(parentPath)) {
            List<String> required = requiredProps.get(parentPath);
            return required.contains(propertyName);
        }
        return false;
    }
    
    /**
     * Get the parent path
     */
    private static String getParentPath(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0) {
            return path.substring(0, lastDot);
        }
        return "$";
    }
    
    /**
     * Get the property name from a path
     */
    private static String getPropertyName(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0 && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1);
        }
        if (path.startsWith("$.")) {
            return path.substring(2);
        }
        return "";
    }
    
    /**
     * Helper method to truncate strings for table display
     */
    private static String truncateString(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}