# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java-based configuration service project named "dos-config" built on the WeUP framework. The service manages various configurations related to data analysis, tagging systems, and enterprise information management.

## Build System

- **Build Tool**: Gradle (version 8.14.3)
- **Framework**: WeUP framework (internal microservice framework)
- **Main Build File**: `buildweup.gradle`
- **Project Configuration**: `project.gradle`
- **Settings File**: `settings.gradle`

## Key Components

### Architecture Layers
1. **Entity Layer**: Data models in `src/main/java/cn/webank/dosconfig/entity/`
2. **DAO Layer**: Database access objects using MyBatis
3. **Service Layer**: Business logic implementation
4. **Controller Layer**: REST API endpoints (based on WeUP framework)
5. **Configuration**: Property files in `src/main/resources/`

### Database
- **ORM Framework**: MyBatis
- **Mapper Files**: XML files in `src/main/resources/daoMapper/`
- **Database Scripts**: SQL files in `dbScript/` directory organized by version

### Testing
- **Test Framework**: JUnit
- **Test Location**: `src/test/java/cn/webank/dosconfig/`
- **Main Test Entry Point**: `TestWeupserverMain.java`

## Common Development Tasks

### Building the Project
```bash
# Using Gradle wrapper
./gradlew build

# Clean build
./gradlew clean build
```

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "cn.webank.dosconfig.service.impl.TagServiceImplTest"
```

### Local Development Setup
1. Main class for local startup: `cn.webank.weupserver.libs.WeUpServerApplication`
2. Local management console: http://localhost:27081/dos-config/
3. Application properties: `src/main/resources/application.properties`

### Database Migration
SQL scripts are organized in the `dbScript/` directory by version number. New database changes should follow the existing versioning pattern.

### Code Generation Tools
The project uses several WeUP-specific Gradle plugins for code generation:
- `weup-rmbprotocoldocuploadtool`: RMB protocol documentation upload
- `weup-rmbservicecodegentool`: RMB service code generation
- `weup-mybatisgeneratortool`: MyBatis generator
- `weup-rmbprotocolfieldfilltool`: RMB protocol field filling
- `weup-sqlUpdateTimeFillTool`: SQL update time filling

## Configuration Files

### Main Configuration
- `application.properties`: Core application configuration
- `weup.properties`: WeUP framework configuration
- `gns-api.properties`: GNS API configuration

### Security Configuration
- `um_client.properties`: UM (User Management) client configuration
- `sso.client.properties`: SSO client configuration

### External Services Configuration
- Database connection settings in `application.properties`
- FPS (Financial Processing Service) configuration
- LLM (Large Language Model) API configuration
- Milvus vector database configuration
- Tableau integration settings

## Important URLs

### Documentation
- WeUP framework documentation: http://10.107.99.84:53661/wego-rad/site/
- WeUP online documentation: http://tcftp.weoa.com/weup-admin-ui/

### Development Resources
- WeUP demo project: http://git.weoa.com/quickli/weupdemo-efsadm
- Internal WeUP core libraries (accessible internally):
  - http://git.weoa.com/Internal/weup-base
  - http://git.weoa.com/Internal/weup-rmb
  - http://git.weoa.com/Internal/weup-adm

### Monitoring & Tools
- APM monitoring (Matrix): http://mx.weoa.com/
- QMS quality platform: http://qms.weoa.com/#/
- CMDB: http://uat.cmdb.weoa.com/#/
- MSS message tracing: http://rmb-msg.weoa.com/mss/#/trace

## Local Debugging

1. Use Node.js version v12.14.1 for frontend builds
2. Frontend build process:
   ```bash
   cd dos-config-web
   npm install
   npm run build-pro
   ```
3. Copy built files to resources directory:
   ```bash
   rmdir ..\src\main\resources\webapp\
   cp -R .\docs\* ..\src\main\resources\webapp\
   ```

## Key Entities

The main data entities include:
- TagDistributionConfig: Configuration for tag distribution
- TagStatistic: Statistical information about tags
- TagValueRanges: Value ranges for tags
- Enterprise-related entities for business data management

## RMB Integration

The project integrates with RMB (Remote Message Bus) for inter-service communication:
- RMB protocol documentation generation
- Service code generation tools
- Request/response message classes in the entity package

## DAO Layer Structure

The DAO (Data Access Object) layer follows these patterns:

### DAO Interface Naming Convention
- DAO interfaces are named after the entity they manage, suffixed with "Dao"
- Example: `TagDao` for managing `TagDistributionConfig` entities
- Located in: `src/main/java/cn/webank/dosconfig/dao/`

### DAO Methods
- Standard CRUD operations: `insert`, `update`, `delete`, `selectByPrimaryKey`
- Query methods for specific business logic
- Batch operations for bulk data processing
- Pagination support for large datasets

## Mapper Layer (MyBatis)

### XML Mapper Files
- Located in: `src/main/resources/daoMapper/`
- Named after the corresponding DAO interface with "Mapper.xml" suffix
- Example: `TagDao` -> `TagDaoMapper.xml`

### Result Maps
- `BaseResultMap`: Maps basic fields (excluding BLOBs)
- `ResultMapWithBLOBs`: Extends BaseResultMap and includes BLOB fields
- Used to handle tables with large text fields efficiently

### SQL Fragments
- `Base_Column_List`: Lists basic columns (excluding BLOBs)
- `Blob_Column_List`: Lists BLOB columns separately
- Allows selective querying of large data fields

### Mapper Method Patterns
1. **Complete Record Operations**:
   - `selectByPrimaryKey`: Returns full record with BLOBs
   - `insert`: Inserts all fields including BLOBs
   - `updateByPrimaryKeyWithBLOBs`: Updates all fields including BLOBs

2. **Selective Operations**:
   - `insertSelective`: Inserts only non-null fields
   - `updateByPrimaryKeySelective`: Updates only provided fields
   - Uses `<if>` conditions in XML for dynamic SQL generation

3. **Batch Operations**:
   - `batchInsertList`: Bulk inserts using `<foreach>` iteration
   - More efficient for multiple records

4. **Pagination Support**:
   - `selectCountByPage`: Counts total records for pagination
   - `selectListByPage`: Retrieves paginated results
   - Uses `limit` clause with calculated offset

## Database Structure Rules

### Entity Class Patterns
1. **Standard Entities**:
   - Direct mapping to database tables
   - Contain all basic fields as Java properties
   - Follow JavaBean conventions with getters/setters

2. **WithBLOBs Entities**:
   - Extend standard entities
   - Add large text fields (BLOBs) as String properties
   - Used for tables with TEXT or LONGTEXT columns
   - Example: `TQueryHistory` (basic fields) and `TQueryHistoryWithBLOBs` (extends TQueryHistory with queryContent and extendInfo)

### Database Annotation Usage
- `@MySQLTable`: Defines table metadata (name, engine, charset, comments)
- `@MySQLColumn`: Maps Java fields to database columns with detailed metadata
- `@MySQLIndex`: Defines database indexes on tables
- Generated by WeUP MyBatis generator tools

### Field Mapping Conventions
- Column names in snake_case map to camelCase Java properties
- VARCHAR columns map to String
- TIMESTAMP columns map to java.util.Date
- TEXT/LONGTEXT columns map to String in WithBLOBs classes
- Primary keys are annotated with MyBatis `@id` in XML mappers

### DDL
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin

## Configuration Properties Usage

The project uses Spring's `@Value` annotation to inject configuration properties from property files:

### Basic Configuration Injection
```java
@Value("${property.key:default_value}")
private String propertyName;
```

### Common Patterns
1. **External Service URLs**:
   - `@Value("${llm.apiUrl:http://172.21.3.106:80/v1}")`
   - `@Value("${tableau.sign.ip:172.21.8.149}")`

2. **API Keys and Credentials**:
   - `@Value("${llm.apiKey:sk--JTBNjumkRmIRStpErBxKQ}")`
   - `@Value("${tableau.sign.password.rsa}")` (encrypted values)

3. **Feature Flags**:
   - `@Value("${llm.forceUse.model:NONE}")`

4. **Numeric Values**:
   - `@Value("${tableau.sign.port:9090}")`
   - `@Value("${tableau.restApi.version:3.21}")`

### Configuration Sources
- Primary: `src/main/resources/application.properties`
- Additional: `weup-adm.properties`, `weup.properties`, etc.
- Environment-specific overrides possible through external configuration

### Best Practices
1. Always provide sensible default values where appropriate
2. Use descriptive property keys that indicate their purpose
3. Group related properties with common prefixes (e.g., `llm.`, `tableau.`, `milvus.`)
4. Encrypt sensitive values using RSA encryption where applicable
5. Document new configuration properties in the appropriate `.properties` file