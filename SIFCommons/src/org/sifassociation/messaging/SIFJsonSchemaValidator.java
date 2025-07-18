package org.sifassociation.messaging;

import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.Problem;
import org.leadpony.justify.api.ProblemHandler;
import org.leadpony.justify.api.SpecVersion;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.json.stream.JsonParser;

/**
 * Utility class for validating JSON payloads against JSON Schema using Justify 3.1.0.
 * 
 * This class provides methods to validate JSON strings against JSON Schema documents,
 * with support for both file-based and URL-based schema loading.
 * 
 * @author jlovell
 * @version 3.0
 * @since 3.0
 */
public class SIFJsonSchemaValidator {
    
    private static final Logger logger = Logger.getLogger(SIFJsonSchemaValidator.class.getName());
    private static final JsonValidationService service = JsonValidationService.newInstance();
    
    /**
     * Validation result containing success status and any validation problems.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public String getErrorSummary() {
            if (errors.isEmpty()) {
                return "No validation errors";
            }
            return "Validation errors: " + String.join("; ", errors);
        }
    }
    
    /**
     * Validates a JSON payload string against a JSON Schema loaded from a file path.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaPath The file path to the JSON Schema
     * @return ValidationResult containing success status and any errors
     */
    public static ValidationResult validatePayload(String jsonPayload, String schemaPath) {
        if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
            return new ValidationResult(false, List.of("JSON payload cannot be null or empty"));
        }
        
        try {
            Path path = Paths.get(schemaPath);
            if (!Files.exists(path)) {
                return new ValidationResult(false, List.of("Schema file not found: " + schemaPath));
            }
            
            try (InputStream schemaStream = Files.newInputStream(path)) {
                return validatePayload(jsonPayload, schemaStream);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading schema file: " + schemaPath, e);
            return new ValidationResult(false, List.of("Error reading schema file: " + e.getMessage()));
        }
    }
    
    /**
     * Validates a JSON payload string against a JSON Schema loaded from a URL.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaUrl The URL to the JSON Schema
     * @return ValidationResult containing success status and any errors
     */
    public static ValidationResult validatePayload(String jsonPayload, URL schemaUrl) {
        if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
            return new ValidationResult(false, List.of("JSON payload cannot be null or empty"));
        }
        
        try (InputStream schemaStream = schemaUrl.openStream()) {
            return validatePayload(jsonPayload, schemaStream);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading schema from URL: " + schemaUrl, e);
            return new ValidationResult(false, List.of("Error reading schema from URL: " + e.getMessage()));
        }
    }
    
    /**
     * Validates a JSON payload string against a JSON Schema provided as a string.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaString The JSON Schema as a string
     * @return ValidationResult containing success status and any errors
     */
    public static ValidationResult validatePayloadWithSchemaString(String jsonPayload, String schemaString) {
        if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
            return new ValidationResult(false, List.of("JSON payload cannot be null or empty"));
        }
        
        if (schemaString == null || schemaString.trim().isEmpty()) {
            return new ValidationResult(false, List.of("JSON schema cannot be null or empty"));
        }
        
        try (StringReader schemaReader = new StringReader(schemaString)) {
            JsonSchema schema = service.readSchema(schemaReader);
            return validateWithSchema(jsonPayload, schema);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing schema string", e);
            return new ValidationResult(false, List.of("Error parsing schema: " + e.getMessage()));
        }
    }
    
    /**
     * Validates a JSON payload string against a JSON Schema loaded from an InputStream.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaStream The InputStream containing the JSON Schema
     * @return ValidationResult containing success status and any errors
     */
    private static ValidationResult validatePayload(String jsonPayload, InputStream schemaStream) {
        try {
            JsonSchema schema = service.readSchema(schemaStream);
            return validateWithSchema(jsonPayload, schema);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error reading or parsing schema", e);
            return new ValidationResult(false, List.of("Error reading or parsing schema: " + e.getMessage()));
        }
    }
    
    /**
     * Validates a JSON payload string against a compiled JSON Schema.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schema The compiled JSON Schema
     * @return ValidationResult containing success status and any errors
     */
    private static ValidationResult validateWithSchema(String jsonPayload, JsonSchema schema) {
        List<String> errors = new ArrayList<>();
        
        // Create a problem handler to collect validation errors
        ProblemHandler problemHandler = problems -> {
            for (Problem problem : problems) {
                errors.add(problem.getMessage());
            }
        };
        
        try {
            // Create a validating parser
            try (JsonParser parser = service.createParser(new StringReader(jsonPayload), schema, problemHandler)) {
                // Force full parse + validation
                while (parser.hasNext()) {
                    parser.next();
                }
            }
            
            // Check if validation succeeded (no errors collected)
            return new ValidationResult(errors.isEmpty(), errors);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during JSON validation", e);
            errors.add("Error during JSON validation: " + e.getMessage());
            return new ValidationResult(false, errors);
        }
    }
    
    /**
     * Convenience method to check if a JSON payload is valid against a schema file.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaPath The file path to the JSON Schema
     * @return true if the payload is valid, false otherwise
     */
    public static boolean isValid(String jsonPayload, String schemaPath) {
        return validatePayload(jsonPayload, schemaPath).isValid();
    }
    
    /**
     * Convenience method to check if a JSON payload is valid against a schema URL.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaUrl The URL to the JSON Schema
     * @return true if the payload is valid, false otherwise
     */
    public static boolean isValid(String jsonPayload, URL schemaUrl) {
        return validatePayload(jsonPayload, schemaUrl).isValid();
    }
    
    /**
     * Convenience method to check if a JSON payload is valid against a schema string.
     * 
     * @param jsonPayload The JSON payload string to validate
     * @param schemaString The JSON Schema as a string
     * @return true if the payload is valid, false otherwise
     */
    public static boolean isValidWithSchemaString(String jsonPayload, String schemaString) {
        return validatePayloadWithSchemaString(jsonPayload, schemaString).isValid();
    }
}