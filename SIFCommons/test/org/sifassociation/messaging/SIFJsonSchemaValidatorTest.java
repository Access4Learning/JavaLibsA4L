package org.sifassociation.messaging;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit tests for SIFJsonSchemaValidator.
 * 
 * @author jlovell
 */
public class SIFJsonSchemaValidatorTest {
    
    private static final String VALID_JSON_PAYLOAD = "{\n" +
            "  \"name\": \"John Doe\",\n" +
            "  \"age\": 30,\n" +
            "  \"email\": \"john@example.com\"\n" +
            "}";
    
    private static final String INVALID_JSON_PAYLOAD = "{\n" +
            "  \"name\": \"John Doe\",\n" +
            "  \"age\": \"thirty\",\n" +  // age should be number, not string
            "  \"email\": \"john@example.com\"\n" +
            "}";
    
    private static final String MALFORMED_JSON_PAYLOAD = "{\n" +
            "  \"name\": \"John Doe\",\n" +
            "  \"age\": 30,\n" +
            "  \"email\": \"john@example.com\"\n" +
            // Missing closing brace
            "";
    
    private static final String PERSON_SCHEMA = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"name\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"age\": {\n" +
            "      \"type\": \"integer\",\n" +
            "      \"minimum\": 0\n" +
            "    },\n" +
            "    \"email\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"format\": \"email\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\"name\", \"age\"],\n" +
            "  \"additionalProperties\": false\n" +
            "}";
    
    private static final String INVALID_SCHEMA = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"name\": {\n" +
            "      \"type\": \"string\"\n" +
            "    }\n" +
            "  },\n" +  // Missing closing brace - malformed JSON
            "";
    
    private static Path tempSchemaFile;
    
    @BeforeClass
    public static void setUpClass() throws IOException {
        // Create a temporary schema file for testing
        tempSchemaFile = Files.createTempFile("test_schema", ".json");
        Files.write(tempSchemaFile, PERSON_SCHEMA.getBytes());
    }
    
    @AfterClass
    public static void tearDownClass() throws IOException {
        // Clean up temporary file
        if (tempSchemaFile != null && Files.exists(tempSchemaFile)) {
            Files.delete(tempSchemaFile);
        }
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test successful validation with valid JSON payload and schema string.
     */
    @Test
    public void testValidatePayloadWithSchemaString_Valid() {
        SIFJsonSchemaValidator.ValidationResult result = 
                SIFJsonSchemaValidator.validatePayloadWithSchemaString(VALID_JSON_PAYLOAD, PERSON_SCHEMA);
        
        assertTrue("Valid payload should pass validation", result.isValid());
        assertTrue("Valid payload should have no errors", result.getErrors().isEmpty());
        assertEquals("Error summary should indicate no errors", "No validation errors", result.getErrorSummary());
    }

    /**
     * Test failed validation with invalid JSON payload and schema string.
     */
    @Test
    public void testValidatePayloadWithSchemaString_Invalid() {
        SIFJsonSchemaValidator.ValidationResult result = 
                SIFJsonSchemaValidator.validatePayloadWithSchemaString(INVALID_JSON_PAYLOAD, PERSON_SCHEMA);
        
        assertFalse("Invalid payload should fail validation", result.isValid());
        assertFalse("Invalid payload should have errors", result.getErrors().isEmpty());
        assertTrue("Error summary should contain validation errors", 
                result.getErrorSummary().contains("Validation errors"));
    }

    /**
     * Test validation with malformed JSON payload.
     */
    @Test
    public void testValidatePayloadWithSchemaString_MalformedJson() {
        SIFJsonSchemaValidator.ValidationResult result = 
                SIFJsonSchemaValidator.validatePayloadWithSchemaString(MALFORMED_JSON_PAYLOAD, PERSON_SCHEMA);
        
        assertFalse("Malformed JSON should fail validation", result.isValid());
        assertFalse("Malformed JSON should have errors", result.getErrors().isEmpty());
        assertTrue("Error should mention JSON validation", 
                result.getErrorSummary().toLowerCase().contains("validation"));
    }

    /**
     * Test validation with null payload.
     */
    @Test
    public void testValidatePayloadWithSchemaString_NullPayload() {
        SIFJsonSchemaValidator.ValidationResult result = 
                SIFJsonSchemaValidator.validatePayloadWithSchemaString(null, PERSON_SCHEMA);
        
        assertFalse("Null payload should fail validation", result.isValid());
        assertFalse("Null payload should have errors", result.getErrors().isEmpty());
        assertTrue("Error should mention null payload", 
                result.getErrorSummary().toLowerCase().contains("null"));
    }

    /**
     * Test validation with empty payload.
     */
    @Test
    public void testValidatePayloadWithSchemaString_EmptyPayload() {
        SIFJsonSchemaValidator.ValidationResult result = 
                SIFJsonSchemaValidator.validatePayloadWithSchemaString("", PERSON_SCHEMA);
        
        assertFalse("Empty payload should fail validation", result.isValid());
        assertFalse("Empty payload should have errors", result.getErrors().isEmpty());
        assertTrue("Error should mention empty payload", 
                result.getErrorSummary().toLowerCase().contains("empty"));
    }

    /**
     * Test validation with null schema.
     */
    @Test
    public void testValidatePayloadWithSchemaString_NullSchema() {
        SIFJsonSchemaValidator.ValidationResult result = 
                SIFJsonSchemaValidator.validatePayloadWithSchemaString(VALID_JSON_PAYLOAD, null);
        
        assertFalse("Null schema should fail validation", result.isValid());
        assertFalse("Null schema should have errors", result.getErrors().isEmpty());
        assertTrue("Error should mention null schema", 
                result.getErrorSummary().toLowerCase().contains("null"));
    }

    /**
     * Test validation with invalid schema.
     */
    @Test
    public void testValidatePayloadWithSchemaString_InvalidSchema() {
        SIFJsonSchemaValidator.ValidationResult result = 
                SIFJsonSchemaValidator.validatePayloadWithSchemaString(VALID_JSON_PAYLOAD, INVALID_SCHEMA);
        
        assertFalse("Invalid schema should fail validation", result.isValid());
        assertFalse("Invalid schema should have errors", result.getErrors().isEmpty());
        assertTrue("Error should mention schema. Actual: " + result.getErrorSummary(), 
                result.getErrorSummary().toLowerCase().contains("schema") ||
                result.getErrorSummary().toLowerCase().contains("parsing") ||
                result.getErrorSummary().toLowerCase().contains("error"));
    }

    /**
     * Test validation with schema file path.
     */
    @Test
    public void testValidatePayload_FilePathValid() {
        SIFJsonSchemaValidator.ValidationResult result = 
                SIFJsonSchemaValidator.validatePayload(VALID_JSON_PAYLOAD, tempSchemaFile.toString());
        
        assertTrue("Valid payload should pass validation with file path", result.isValid());
        assertTrue("Valid payload should have no errors", result.getErrors().isEmpty());
    }

    /**
     * Test validation with invalid schema file path.
     */
    @Test
    public void testValidatePayload_FilePathNotFound() {
        SIFJsonSchemaValidator.ValidationResult result = 
                SIFJsonSchemaValidator.validatePayload(VALID_JSON_PAYLOAD, "/nonexistent/path/schema.json");
        
        assertFalse("Non-existent file should fail validation", result.isValid());
        assertFalse("Non-existent file should have errors", result.getErrors().isEmpty());
        assertTrue("Error should mention file not found", 
                result.getErrorSummary().toLowerCase().contains("not found"));
    }

    /**
     * Test validation with URL - using a local file URL.
     */
    @Test
    public void testValidatePayload_URL() throws Exception {
        URL schemaUrl = tempSchemaFile.toUri().toURL();
        SIFJsonSchemaValidator.ValidationResult result = 
                SIFJsonSchemaValidator.validatePayload(VALID_JSON_PAYLOAD, schemaUrl);
        
        assertTrue("Valid payload should pass validation with URL", result.isValid());
        assertTrue("Valid payload should have no errors", result.getErrors().isEmpty());
    }

    /**
     * Test validation with invalid URL.
     */
    @Test
    public void testValidatePayload_InvalidURL() throws Exception {
        URL invalidUrl = new URL("file:///nonexistent/path/schema.json");
        SIFJsonSchemaValidator.ValidationResult result = 
                SIFJsonSchemaValidator.validatePayload(VALID_JSON_PAYLOAD, invalidUrl);
        
        assertFalse("Invalid URL should fail validation", result.isValid());
        assertFalse("Invalid URL should have errors", result.getErrors().isEmpty());
        assertTrue("Error should mention URL reading. Actual: " + result.getErrorSummary(), 
                result.getErrorSummary().toLowerCase().contains("url") || 
                result.getErrorSummary().toLowerCase().contains("reading") ||
                result.getErrorSummary().toLowerCase().contains("schema"));
    }

    /**
     * Test convenience method isValid with valid payload.
     */
    @Test
    public void testIsValid_Valid() {
        boolean result = SIFJsonSchemaValidator.isValidWithSchemaString(VALID_JSON_PAYLOAD, PERSON_SCHEMA);
        assertTrue("Valid payload should return true", result);
    }

    /**
     * Test convenience method isValid with invalid payload.
     */
    @Test
    public void testIsValid_Invalid() {
        boolean result = SIFJsonSchemaValidator.isValidWithSchemaString(INVALID_JSON_PAYLOAD, PERSON_SCHEMA);
        assertFalse("Invalid payload should return false", result);
    }

    /**
     * Test convenience method isValid with file path.
     */
    @Test
    public void testIsValid_FilePath() {
        boolean result = SIFJsonSchemaValidator.isValid(VALID_JSON_PAYLOAD, tempSchemaFile.toString());
        assertTrue("Valid payload should return true with file path", result);
    }

    /**
     * Test convenience method isValid with URL.
     */
    @Test
    public void testIsValid_URL() throws Exception {
        URL schemaUrl = tempSchemaFile.toUri().toURL();
        boolean result = SIFJsonSchemaValidator.isValid(VALID_JSON_PAYLOAD, schemaUrl);
        assertTrue("Valid payload should return true with URL", result);
    }

    /**
     * Test validation with payload missing required field.
     */
    @Test
    public void testValidatePayload_MissingRequiredField() {
        String payloadMissingName = "{\n" +
                "  \"age\": 30,\n" +
                "  \"email\": \"john@example.com\"\n" +
                "}";
        
        SIFJsonSchemaValidator.ValidationResult result = 
                SIFJsonSchemaValidator.validatePayloadWithSchemaString(payloadMissingName, PERSON_SCHEMA);
        
        assertFalse("Payload missing required field should fail validation", result.isValid());
        assertFalse("Payload missing required field should have errors", result.getErrors().isEmpty());
        
        List<String> errors = result.getErrors();
        // Just check that we have errors - the specific error message format may vary
        assertTrue("Should have validation errors for missing required field", !errors.isEmpty());
    }

    /**
     * Test validation with payload having additional properties when not allowed.
     */
    @Test
    public void testValidatePayload_AdditionalPropertiesNotAllowed() {
        String payloadWithExtra = "{\n" +
                "  \"name\": \"John Doe\",\n" +
                "  \"age\": 30,\n" +
                "  \"email\": \"john@example.com\",\n" +
                "  \"extraField\": \"not allowed\"\n" +
                "}";
        
        SIFJsonSchemaValidator.ValidationResult result = 
                SIFJsonSchemaValidator.validatePayloadWithSchemaString(payloadWithExtra, PERSON_SCHEMA);
        
        assertFalse("Payload with extra properties should fail validation", result.isValid());
        assertFalse("Payload with extra properties should have errors", result.getErrors().isEmpty());
        
        List<String> errors = result.getErrors();
        // Just check that we have errors - the specific error message format may vary
        assertTrue("Should have validation errors for additional properties", !errors.isEmpty());
    }
}