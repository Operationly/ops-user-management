# Operationly User Management Service

A Spring Boot microservice that handles user authentication and tenant management for the Operationly platform. This service integrates with WorkOS for authentication and manages user accounts and tenant information.

## Overview

This microservice provides:

- **User Authentication**: Integration with WorkOS for secure user authentication
- **User Account Management**: CRUD operations for user accounts
- **Tenant Management**: Multi-tenant support with tenant creation and management
- **Database Management**: PostgreSQL with Liquibase for schema versioning
- **Service Discovery**: Eureka integration for microservice discovery
- **Configuration Management**: Spring Cloud Config integration

## Architecture

The service follows a layered architecture:

```
controller/       - REST API endpoints
service/          - Business logic layer
repository/       - Data access layer (JPA)
entity/           - JPA entities
dto/              - Data Transfer Objects
exception/        - Custom exceptions
config/           - Configuration classes
constants/        - Application constants
```

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.5.9
- **Spring Cloud**: 2025.0.1
- **Database**: PostgreSQL
- **ORM**: JPA/Hibernate
- **Database Migration**: Liquibase 5.0.1
- **Authentication**: WorkOS 4.18.1
- **Service Discovery**: Netflix Eureka
- **Build Tool**: Maven

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- WorkOS account and API credentials

## Installation

1. **Clone the repository**:
```bash
git clone <repository-url>
cd ops-user-management
```

2. **Install dependencies**:
```bash
mvn clean install
```

## Configuration

### Environment Variables

The service uses environment variables for configuration. Set the following variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `CONFIG_SERVER_URL` | `http://localhost:9296` | Spring Cloud Config server URL |
| `DB_HOST` | `localhost:5432` | PostgreSQL host and port |
| `DB_NAME` | `operationly` | Database name |
| `DB_USER_NAME` | `postgres` | Database username |
| `DB_PASSWORD` | `root` | Database password |
| `DB_POOL_SIZE` | `20` | Database connection pool size |
| `WORKOS_API_KEY` | Test key provided | WorkOS API key |
| `WORKOS_CLIENT_ID` | Test ID provided | WorkOS Client ID |

### application.yaml

The service configuration is defined in `src/main/resources/application.yaml`:

- **Server Port**: 8181
- **Context Path**: `/operationly/api/v1/users`
- **Liquibase**: Enabled for automatic schema management

### Docker Compose Development

For local development with Docker:

```bash
# Update DB_HOST to 'postgres' when using docker-compose
docker-compose up -d
```

## Running the Application

### Development

```bash
mvn spring-boot:run
```

The service will start on `http://localhost:8181/operationly/api/v1/users`

### Production Build

```bash
mvn clean package
java -jar target/ops-user-management-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### Authentication

#### Sync User
Validates a WorkOS session token and syncs user data with the local database.

```http
GET /operationly/api/v1/users/auth/sync
Authorization: Bearer <sessionToken>
```

**Query Parameters**:
- `tenantId` (optional): UUID of the tenant. Can be omitted during initial signup.

**Response**:
```json
{
  "status": "success",
  "response": {
    "userId": "user_123",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "tenantId": "tenant_456"
  },
  "errors": []
}
```

**Status Codes**:
- `200 OK`: User synchronized successfully
- `400 Bad Request`: Missing or invalid session token
- `401 Unauthorized`: Invalid token
- `500 Internal Server Error`: Server error

## Database Schema

The database is managed by Liquibase with the following structure:

### Migration Files

- `001_initial_schema.xml`: Initial schema setup
- `002_new_column_in_user_account.xml`: User account table enhancements

### Main Tables

- **user_account**: Stores user account information
  - userId (WorkOS user ID)
  - email
  - firstName
  - lastName
  - tenantId (foreign key)
  - Timestamps (createdAt, updatedAt)

- **tenant**: Stores tenant/organization information
  - tenantId (primary key)
  - name
  - status
  - plan
  - Timestamps (createdAt, updatedAt)

## Security

- **Spring Security**: Configured for protecting endpoints
- **Bearer Token Authentication**: Uses WorkOS session tokens
- **HTTPS**: Recommended for production deployments

## Integration with WorkOS

The service integrates with WorkOS for:

- User authentication via OAuth/SSO
- User data validation and retrieval
- Session token validation

Configuration is handled through:
- `config/WorkOSConfig.java`: WorkOS client initialization
- `config/WorkOSProperties.java`: WorkOS properties binding
- `service/WorkOSService.java`: WorkOS integration logic

## Project Structure

```
src/
├── main/
│   ├── java/com/operationly/usermanagement/
│   │   ├── UserManagementApplication.java      # Spring Boot entry point
│   │   ├── config/                             # Configuration classes
│   │   ├── constants/                          # Application constants
│   │   ├── controller/                         # REST controllers
│   │   ├── dto/                                # Data Transfer Objects
│   │   ├── entity/                             # JPA entities
│   │   ├── exception/                          # Custom exceptions
│   │   ├── repository/                         # Data access layer
│   │   └── service/                            # Business logic layer
│   └── resources/
│       ├── application.yaml                    # Application configuration
│       └── liquibase/                          # Database migrations
└── test/
    └── java/                                   # Unit and integration tests
```

## Building and Deployment

### Maven Build

```bash
# Clean build
mvn clean build

# Build with tests
mvn clean package

# Build without tests
mvn clean package -DskipTests
```

### Docker Deployment

Create a `Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jre-slim
COPY target/ops-user-management-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

Build and run:

```bash
docker build -t operationly/user-management:latest .
docker run -p 8181:8181 \
  -e DB_HOST=postgres \
  -e WORKOS_API_KEY=<your-api-key> \
  operationly/user-management:latest
```

## Monitoring and Logging

The service uses SLF4J with Logback for logging. Configure logging levels in `application.yaml`:

```yaml
logging:
  level:
    com.operationly: DEBUG
    org.springframework: INFO
```

## Testing

Run tests with Maven:

```bash
mvn test
```

Test dependencies included:
- Spring Boot Test
- Spring Security Test
- JUnit 5

## Troubleshooting

### Connection Issues
- Verify PostgreSQL is running and accessible
- Check `DB_HOST`, `DB_USER_NAME`, and `DB_PASSWORD` environment variables
- Ensure database `operationly` exists

### WorkOS Integration Issues
- Validate `WORKOS_API_KEY` and `WORKOS_CLIENT_ID`
- Check WorkOS documentation for authentication flow
- Verify session token format

### Port Already in Use
Change the port in `application.yaml`:
```yaml
server:
  port: 8182
```

## Contributing

1. Create a feature branch
2. Make your changes
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

This project is part of the Operationly platform.

## Support

For issues or questions:
1. Check the troubleshooting section
2. Review WorkOS documentation
3. Contact the development team

## Version History

- **0.0.1-SNAPSHOT**: Initial release
  - Basic user management
  - Tenant management
  - WorkOS integration

