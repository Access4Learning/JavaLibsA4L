package sifcommonsdemo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.sifassociation.messaging.SIFJsonSchemaValidator;
import org.sifassociation.schema.JsonPathPlus;
import org.sifassociation.schema.SIFJsonSchemaUtil;
import org.sifassociation.util.SIFFileUtil;

/**
 * Simple JSON Schema reference resolver demo.
 * 
 * @author jlovell
 */
public class SIFCommonsDemo {
    // Initializing the log4j system properly.
    static Logger logger = Logger.getLogger(SIFCommonsDemo.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Load a JSON Schema with improved reference resolution.
     * 
     * @param schemaPath Path to the schema file
     * @return The schema with all references resolved
     */
    /**
     * Save a JSON Schema to a file
     * 
     * @param schema The schema to save
     * @param filePath The path to save to
     * @return true if successful, false otherwise
     */
    private static boolean saveSchema(JsonNode schema, String filePath) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), schema);
            logger.info("Schema saved to: " + filePath);
            return true;
        } catch (IOException ex) {
            logger.error("Error saving schema: " + ex.getMessage(), ex);
            return false;
        }
    }
    
    /**
     * Demonstrates the use of SIFJsonSchemaUtil to analyze JSON Schema files
     * 
     * @param schemaPath Path to the JSON Schema file
     */
    public static void demonstrateJsonSchemaAnalysis(String schemaPath) {
        System.out.println("\n============= JSON SCHEMA ANALYSIS DEMONSTRATION =============\n");
        System.out.println("Analyzing JSON Schema file: " + schemaPath);
        
        // Load the schema
        JsonNode schema = SIFJsonSchemaUtil.loadSchema(schemaPath);
        if (schema == null) {
            System.out.println("Error loading schema file");
            return;
        }
        
        // Get all paths
        List<JsonPathPlus> paths = SIFJsonSchemaUtil.getAllPaths(schema);
        System.out.println("\nFound " + paths.size() + " JSON paths in the schema");
        
        // Get root objects
        List<JsonPathPlus> rootObjects = SIFJsonSchemaUtil.getRootObjects(schema);
        System.out.println("Found " + rootObjects.size() + " root objects in the schema");
        
        // Get definitions/types
        Map<String, JsonNode> definitions = SIFJsonSchemaUtil.getDefinitions(schema);
        System.out.println("Found " + definitions.size() + " type definitions in the schema");
        
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
        
        for (JsonPathPlus path : paths) {
            // Skip very deep paths to keep the table readable
            if (path.getPath().split("\\.").length > 4) continue;
            
            System.out.printf("%-40s | %-10s | %-12s | %-12s | %-8s | %s\n",
                    truncateString(path.getPath(), 40),
                    path.isMandatory() ? "Required" : "Optional",
                    path.getJsonType() != null ? path.getJsonType() : "unknown",
                    truncateString(path.getFormat(), 12),
                    path.isRepeatable() ? "Yes" : "No",
                    truncateString(path.getDocumentation(), 30));
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
        
        for (JsonPathPlus path : paths) {
            // Only show paths with constraints
            if ((path.getEnumerations() != null && !path.getEnumerations().isEmpty()) ||
                (path.getPatterns() != null && !path.getPatterns().isEmpty()) ||
                (path.getMinLength() != null && !path.getMinLength().isEmpty()) ||
                (path.getMaxLength() != null && !path.getMaxLength().isEmpty())) {
                
                String lengthConstraints = "";
                if (path.getMinLength() != null && !path.getMinLength().isEmpty()) {
                    lengthConstraints += "min=" + path.getMinLength();
                }
                if (path.getMaxLength() != null && !path.getMaxLength().isEmpty()) {
                    if (!lengthConstraints.isEmpty()) {
                        lengthConstraints += ", ";
                    }
                    lengthConstraints += "max=" + path.getMaxLength();
                }
                
                System.out.printf("%-40s | %-30s | %-30s | %s\n",
                        truncateString(path.getPath(), 40),
                        truncateString(path.getEnumerations(), 30),
                        truncateString(path.getPatterns(), 30),
                        truncateString(lengthConstraints, 30));
            }
        }
        
        System.out.println("\n============= END OF JSON SCHEMA ANALYSIS =============\n");
    }
    
    /**
     * Helper method to truncate strings for table display
     */
    private static String truncateString(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Helper method to count the number of $ref occurrences in a schema
     * 
     * @param node JSON node to search
     * @return Number of references found
     */
    private static int countReferences(JsonNode node) {
        if (node == null) {
            return 0;
        }
        
        int count = 0;
        
        // Check if this node has a $ref
        if (node.has("$ref")) {
            count++;
        }
        
        // Check child nodes in objects
        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                count += countReferences(node.get(fieldName));
            }
        }
        
        // Check child nodes in arrays
        if (node.isArray()) {
            for (JsonNode item : node) {
                count += countReferences(item);
            }
        }
        
        return count;
    }
    
    public static void main(String[] args) throws Exception {
        // Configure log4j.
        BasicConfigurator.configure();
        
        // Set logging level (minimum log message to be reported).
        logger.setLevel(org.apache.log4j.Level.WARN);
        Logger.getLogger("org.springframework").setLevel(org.apache.log4j.Level.WARN);
        Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.WARN);
        
        // Process command line args
        String schemaPath = "resources/examples/student.schema.json";
        if (args.length > 0) {
            schemaPath = args[0];
        }
        
        // First analyze the original schema
        System.out.println("\n============= SCHEMA ANALYSIS =============\n");
        demonstrateJsonSchemaAnalysis(schemaPath);
        
        
        System.out.println("\n============= SCHEMA USE =============\n");

        String JEDxSchemaPath = "resources/examples/worker_compensation_report.jschema";
        String JEDxDataPath = "resources/examples/worker_compensation_report.json";
        
        String JEDxSchema = SIFFileUtil.readFile(JEDxSchemaPath);
        System.out.println("Example Schema:");
        System.out.println(JEDxSchema);
        
        String JEDxData = SIFFileUtil.readFile(JEDxDataPath);
        System.out.println("");
        System.out.println("Example Payload:");
        System.out.println(JEDxData);
        
        Thread.sleep(1000);  // Debug
        
        SIFJsonSchemaValidator.ValidationResult validatePayload = SIFJsonSchemaValidator.validatePayload(JEDxData, JEDxSchemaPath);
        if(validatePayload.isValid()) {
            System.out.println("Example Payload: Valid");
        }
        else {
            System.out.println("Example Payload: Invalid");
            System.out.println(validatePayload.getErrorSummary());
        }
    }            
}