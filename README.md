# JavaLibsA4L

A collection of Java libraries to help implement the Schools Interoperability Framework (SIF) Specifications.

## Core Components and Functionality

### 1. SIF Version Handling
The library provides robust version handling through the `SIFVersion` class, which:
- Supports both SIF 2.x format (e.g., "2.0r1") and SIF 3.x format (e.g., "3.0.1")
- Implements wildcard matching for flexible version comparisons (like "*", "2.*", etc.)
- Provides immutable objects for thread safety
- Includes custom comparison methods optimized for SIF version semantics

### 2. Reference ID Management
The `SIFRefId` class manages unique identifiers within the SIF ecosystem:
- Generates and parses UUIDs that comply with SIF specifications
- Supports both standard UUID format and SIF-specific format (without dashes)
- Includes hardware address and timestamp components for guaranteed uniqueness
- Validates all components to ensure proper UUID compliance

### 3. XML-JSON Conversion
The XMLJSON package offers multiple approaches to XML-JSON conversion:
- `GoessnerNative`: Pure Java implementation of the Goessner algorithm
- `GoessnerReference`: JavaScript-based reference implementation via Java scripting
- `JacksonNative`: Advanced implementation with better type preservation and round-trip support
- `PESCNative`: Support for Postsecondary Electronic Standards Council JSON notation
- All implementations share the same interface for consistent usage

### 4. SIF Messaging
The library provides comprehensive messaging capabilities for different SIF versions:
- `SIF2MessageXML`: Handles SIF 2.x messaging with support for both HTTP and SOAP
- `SIF3Message`: Implements SIF 3.x messaging with REST-based approach
- Both support message creation, parsing, validation, and transport conversion
- Includes payload management with format-specific handling

### 5. XML Database Integration
The querying package enables interaction with XML databases, particularly eXist:
- `EXistXQuery`: XML:DB API-based implementation for traditional XML database access
- `EXistXQueryREST`: REST-based implementation for modern deployments
- Provides efficient result iteration and pagination for large datasets
- Includes resource management for collections, documents, and queries

### 6. Schema Processing
The schema package offers tools for working with XML schemas:
- XML schema traversal and analysis capabilities
- Visitor pattern implementations for different schema exploration needs
- Support for SIF-specific schema annotations and metadata
- Enhanced XPath functionality for complex queries

### 7. Utility Components
Various utility classes provide supporting functionality:
- File and URL operations specialized for SIF contexts
- XML manipulation with XOM library integration
- Authentication utilities for secure messaging
- Logging implementations for message tracking
- JSON structure creation and manipulation

## Build Commands
- Clean and build: `ant clean build`
- Run tests: `ant test`
- Run single test: `ant test-single -Dtest.includes=org/sifassociation/messaging/SIFVersionTest.java`
- Generate javadocs: `ant javadoc`