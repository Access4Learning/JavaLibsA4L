# HR Open Standards Schema Collection

This directory contains a clean extraction of the core JSON schemas for the HR Open Standards API specifications.

## Directory Structure

```
ExtractedSchemas/
├── common/                      # Common base type definitions
│   └── IdentifierType.json      # Core identifier schema
├── organizations/               # Organization-related schemas
│   └── OrganizationType.json    # Organization definition
├── workers/                     # Worker-related schemas
│   └── WorkerType.json          # Worker definition
├── workerCompensationReports/   # Worker compensation reporting schemas
│   └── WorkerCompensationReportType.json  # Compensation report structure
└── workerPaidHoursReports/      # Worker hours reporting schemas
    └── WorkerPaidHoursReportType.json     # Hours report structure
```

## Schema Hierarchy

The schemas follow a hierarchical structure where:

1. Base types are defined in the `common/` directory
2. Core entity types use these base types
3. Report types reference the entity types and add additional properties

## API Design

The APIs follow these patterns:

- Collection endpoints: `/organizations`, `/workers`, `/workerCompensationReports`, `/workerPaidHoursReports`
- Individual resource endpoints: `/organizations/organization`, `/workers/worker`, etc.
- Request schemas: `*-add-update-request-schema_v1.json`
- Response schemas: `*-read-response-schema_v1.json`

## Reference

These schemas are based on HR Open Standards (v4.3.0) and follow their naming conventions and data structures. The schemas provide a standardized model for HR data exchange focusing on:

- Organization information
- Worker details and relationships
- Compensation reporting
- Hours and time tracking