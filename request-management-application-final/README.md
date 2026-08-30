# Request Management Application

Spring Boot REST API for managing the complete lifecycle of business requests.

The application provides request creation, validation, state transitions, publishing, logical deletion, audit history, caching, pagination, authentication, and resilience controls.
---

## Architecture

```text
Client
  |
  v
REST Controller
  |
  v
Request Service
  |
  +---- Request State Machine
  |
  +---- Request Repository
  |
  +---- Audit Repository
  |
  +---- Publication Number Generator
  |
  v
Database
```

## Main layers

```text
Controller – REST endpoints, request validation and HTTP handling
Service – Business logic and transaction management
State Machine – Validates allowed request state transitions
Repository – Database access using Spring Data JPA
Audit – Maintains complete request history
Cache – Caches request reads and evicts entries after updates

================================================================

Request Flow:

                 +------------+
                 |  CREATED   |
                 +------------+
                  |          |
              verify       delete
                  |          |
                  v          v
            +----------+  +---------+
            | VERIFIED |  | DELETED |
            +----------+  +---------+
              |       |
           accept   reject
              |       |
              v       v
         +----------+ +---------+
         | ACCEPTED | |REJECTED|
         +----------+ +---------+
              |
           publish
              |
              v
         +-----------+
         | PUBLISHED |
         +-----------+

Valid transitions
From	To	Operation
CREATED	VERIFIED	Verify
VERIFIED	ACCEPTED	Accept
VERIFIED	REJECTED	Reject
ACCEPTED	PUBLISHED	Publish
ACCEPTED	REJECTED	Reject
CREATED	DELETED	Logical Delete

Important business rules
Content can only be modified in CREATED or VERIFIED.
Only VERIFIED requests can be accepted.
Only ACCEPTED requests can be published.
Rejection is allowed from VERIFIED and ACCEPTED.
Deletion is allowed only from CREATED.
Rejection requires a reason.
Deletion requires a reason.
Publishing generates a unique numeric publication number.
Published, rejected and deleted requests cannot be modified.
Every state-changing operation is audited.

```
---
## REST API
 ```text
 
Base path:
/api/v1/requests
Method	Endpoint	Description
POST	/requests	Create request
GET	/requests	Search/list requests
GET	/requests/{id}	Get request
PATCH	/requests/{id}/content	Update content
POST	/requests/{id}/verify	Verify request
POST	/requests/{id}/accept	Accept request
POST	/requests/{id}/reject	Reject request
POST	/requests/{id}/publish	Publish request
DELETE	/requests/{id}	Logically delete request
GET	/requests/{id}/audit	Get audit history

================================================================

Search, Pagination and Sorting
The search API supports:
Case-insensitive name filtering
state filtering
Pagination
Sorting

================================================================
Audit History

Every important lifecycle operation is recorded in REQUEST_AUDIT.
Audit information includes:
Request ID
Previous state
New state
Action
Reason
Actor
Timestamp
Example:
CREATED -> VERIFIED
VERIFIED -> ACCEPTED
ACCEPTED -> PUBLISHED

```

## Database
```text

The application uses different databases for different environments.

Environment	Database	Schema
DEV	H2 In-Memory	schema_dev.sql
PROD	Oracle	Existing Oracle schema
DEV

Development uses H2:

jdbc:h2:mem:reqdb;MODE=Oracle;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=true

Important points:

H2 is in-memory.
Oracle compatibility mode is enabled.
schema_dev.sql initializes the database.
Data is lost when the application stops.
H2 console is enabled.

H2 Console:

/h2-console
PROD

Production uses a real Oracle database.

Database configuration is provided through environment variables:

DB_URL
DB_USERNAME
DB_PASSWORD

Example:

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: oracle.jdbc.OracleDriver

Database Tables

Main tables:

REQUESTS
REQUEST_AUDIT

The REQUESTS table contains:

ID
NAME
CONTENT
STATE
PUBLICATION_NUMBER
TERMINATION_REASON
CREATED_AT
UPDATED_AT
VERSION

The REQUEST_AUDIT table contains:

ID
REQUEST_ID
FROM_STATE
TO_STATE
ACTION
REASON
ACTOR
OCCURRED_AT

Database sequences:

REQUEST_SEQ
REQUEST_AUDIT_SEQ
PUBLICATION_SEQ

PUBLICATION_SEQ starts from 100000 and generates unique publication numbers.

Optimistic Locking :
The REQUESTS table contains a VERSION column.

Caching :
Request retrieval is cached using Spring Cache.
@Cacheable(cacheNames = "requests", key = "#id")

Default configuration:
Maximum entries : 10,000
TTL             : 30 seconds

Configurable using:
REQUEST_CACHE_MAX_SIZE
REQUEST_CACHE_TTL_SECONDS


Connection Pool : 
The application uses HikariCP.

DEV
Maximum pool size : 10
Minimum idle      : 2
Connection timeout: 30 seconds
PROD
Maximum pool size : 20
Minimum idle      : 5
Connection timeout: 30 seconds

Production values can be configured using:
DB_POOL_MAX_SIZE
DB_POOL_MIN_IDLE
DB_POOL_CONNECTION_TIMEOUT
DB_POOL_VALIDATION_TIMEOUT

```
## Resilience
```text

The application supports configurable resilience controls.

Rate Limiting

Default:
100 requests/second

Configuration:
RATE_LIMIT_REQUESTS_PER_SECOND

========================================================================

Bulkhead
Default:
40 concurrent requests

Configuration:
BULKHEAD_MAX_CONCURRENT_REQUESTS

```
## Security

```text

The API uses HTTP Basic Authentication.

Configuration:

APP_SECURITY_USERNAME
APP_SECURITY_PASSWORD

Default development credentials:

Username: api-user
Password: change-me
```

## Graceful Shutdown
```text

The application supports graceful shutdown.
server:
  shutdown: graceful

Shutdown timeout:
30 seconds
```