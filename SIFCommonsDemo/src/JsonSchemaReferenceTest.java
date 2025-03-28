import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Set;

/**
 * Test program for JSON Schema reference resolution
 */
public class JsonSchemaReferenceTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        String schemaPath = args.length > 0 ? args[0] : "resources/json/organization/OrganizationType.json";
        
        System.out.println("\n========= JSON SCHEMA REFERENCE RESOLVER =========\n");
        System.out.println("Testing reference resolution for: " + schemaPath);
        
        // First analyze the schema to find all references
        analyzeSchemaReferences(schemaPath);
        
        // Then resolve and save the schema
        resolveSchemaReferences(schemaPath);
    }
    
    private static void analyzeSchemaReferences(String schemaPath) {
        System.out.println("\n==== Step 1: Analyzing Schema References ====\n");
        try {
            // Load the schema
            JsonNode schema = loadSchema(schemaPath);
            if (schema == null) {
                return;
            }
            
            // Find all $ref nodes in the schema
            Set<String> references = JsonSchemaReferenceResolver.findAllReferences(schema);
            
            System.out.println("Found " + references.size() + " references in schema:");
            for (String ref : references) {
                System.out.println("  - " + ref);
            }
        } catch (Exception e) {
            System.out.println("Error analyzing schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void resolveSchemaReferences(String schemaPath) {
        System.out.println("\n==== Step 2: Resolving Schema References ====\n");
        try {
            // Define output path
            String outputPath = schemaPath.replace(".json", "_resolved.json");
            
            // Resolve and save the schema
            boolean success = JsonSchemaReferenceResolver.resolveAndSave(schemaPath, outputPath);
            
            if (success) {
                System.out.println("\nSuccess! Schema has been resolved and saved to:");
                System.out.println("  " + outputPath);
                
                // Check for remaining references
                JsonNode resolvedSchema = loadSchema(outputPath);
                if (resolvedSchema != null) {
                    Set<String> remainingRefs = JsonSchemaReferenceResolver.findAllReferences(resolvedSchema);
                    
                    if (remainingRefs.isEmpty()) {
                        System.out.println("\n✅ All references successfully resolved!");
                    } else {
                        System.out.println("\n⚠️ Some references could not be fully resolved (" + remainingRefs.size() + "):");
                        for (String ref : remainingRefs) {
                            System.out.println("  - " + ref);
                        }
                        System.out.println("\nThese are likely circular references that were detected and safely handled.");
                    }
                }
            } else {
                System.out.println("❌ Failed to resolve schema references");
            }
        } catch (Exception e) {
            System.out.println("❌ Error resolving schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static JsonNode loadSchema(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("❌ Schema file not found: " + filePath);
                return null;
            }
            
            JsonNode schema = mapper.readTree(file);
            System.out.println("📄 Schema loaded: " + filePath);
            return schema;
        } catch (Exception e) {
            System.out.println("❌ Error loading schema: " + e.getMessage());
            return null;
        }
    }
}