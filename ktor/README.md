# Native Push Ktor

## Overview

This is a Ktor-based server application that provides a native push notification service. It supports multiple user ID types (Long, UUID, and String) and various push notification systems such as Firebase, APNs, and Web Push. This service handles token management and notification sending for different user ID types, ensuring a flexible and scalable notification system.

## Features

- **Multi-ID Type Support**: Handles Long, UUID, and String user IDs.
- **Push System Integration**: Supports Firebase, APNs (P8 and P12), and Web Push systems.
- **Token Management**: Add, update, and delete notification tokens for users.
- **Notification Sending**: Send notifications with various customization options.

## Prerequisites

- Kotlin
- Ktor
- Gradle
- Java 11+

## Getting Started

### Configuration

Environment variables are used to configure the application. Below is a list of required environment variables:

- `ID_TYPE`: Type of user ID (`uuid`, `long`, `string`).
- `POSTGRES_HOST`: Hostname for the PostgreSQL database.
- `POSTGRES_DB`: Database name for the PostgreSQL database.
- `POSTGRES_USER`: Username for the PostgreSQL database.
- `POSTGRES_PASSWORD`: Password for the PostgreSQL database.
- `MYSQL_HOST`: Hostname for the MySQL database.
- `MYSQL_DB`: Database name for the MySQL database.
- `MYSQL_USER`: Username for the MySQL database.
- `MYSQL_PASSWORD`: Password for the MySQL database.
- `MARIA_HOST`: Hostname for the MariaDB database.
- `MARIA_DB`: Database name for the MariaDB database.
- `MARIA_USER`: Username for the MariaDB database.
- `MARIA_PASSWORD`: Password for the MariaDB database.
- `PUSH_SYSTEMS`: Comma-separated list of push systems (`FCM`, `APNS`, `WEBPUSH`).
- `FIREBASE_SERVICE_ACCOUNT_FILE`: Path to the Firebase service account file.
- `APNS_P8_FILE`: Path to the APNs P8 file.
- `APNS_KEY_ID`: APNs key ID.
- `APNS_TEAM_ID`: APNs team ID.
- `APNS_TOPIC`: APNs topic.
- `APNS_P12_FILE`: Path to the APNs P12 file.
- `APNS_P12_PASSWORD`: Password for the APNs P12 file.
- `WEB_PUSH_SUBJECT`: Subject for Web Push.
- `VAPID_KEYS_FILE`: Path to the VAPID keys file.
- `VAPID_PUBLIC_KEY`: VAPID public key.
- `VAPID_PRIVATE_KEY`: VAPID private key.
- `AUTHORIZATION_VALIDATION_URL`: URL for authorization validation.
- `DEVELOPMENT`: Set to `true` for development mode.

Only on of the database parameter set must be provided.
The parameters for a push system must only be specified if 
the system is used. APNS does either need APNS_P8_FILE,
APNS_KEY_ID, APNS_TEAM_ID and APNS_TOPIC or APNS_P12_FILE,
APNS_P12_PASSWORD and APNS_TOPIC. Web Push does either need
VAPID_KEYS_FILE or VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY.
The AUTHORIZATION_VALIDATION_URL does specify are url where
the users will be validated. The server will send a request
with two headers: `Authorization` and `User-Id` containing
the forwarded `Authorization` header and the user id. The
server aspects a `SuccessResult` (see
[Request and Response Formats](#request-and-response-formats))
This parameter can also be omitted to skip all validation.

All environment can also be supplied as paths by appending
`_PATH` to the variable name (except the ones which are
already paths).

See the
[initialization section of library](https://github.com/Native-Push/native_push_server/tree/main/lib/README.md#initialization-example)
for further information

### Running the Application

1. Download the jar from the releases

    ```bash
   wget https://github.com/svenopdehipt/native_push_server/releases/latest/download/native-push.jar
    ```

2. Set up the environment variables:

    ```bash
    export ID_TYPE=uuid
    export POSTGRES_HOST=localhost
    export POSTGRES_DB=opdehipt
    export POSTGRES_USER=postgres
    export POSTGRES_PASSWORD=postgres
    # Set other environment variables as needed
    ```

3. Build and run the application using Gradle:

    ```bash
    java -jar native-push.jar
    ```

The application will start on port 80 by default.

### Running the Application with Docker

You can use Docker and Docker Compose to run the application in a containerized environment. Below is an example `docker-compose.yml` file:

```yaml
# Example Docker Compose configuration file for Native Push Notification Service

services:
  # Nginx service to act as a reverse proxy for the Ktor service
  nginx:
    image: nginx:alpine
    restart: always
    networks:
      - nginx
    ports:
      - "0.0.0.0:80:80/tcp"  # Expose port 80
      - "0.0.0.0:443:443/tcp"  # Expose port 443
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro  # Nginx configuration
      - ./nginx/ssl:/ssl:ro  # SSL certificates
      - ./nginx/example:/www/example:ro  # Example website content
    depends_on:
      - ktor  # Ensure Ktor service is started before Nginx

  # Ktor service running the Opdehipt application
  ktor:
    image: eclipse-temurin:22-jre  # Use Eclipse Temurin JRE image
    restart: always
    networks:
      - nginx
      - postgres
    volumes:
      - ./ktor/app.jar:/app/app.jar:ro  # Application JAR file
    entrypoint: ["java", "-jar", "/app/app.jar"]  # Command to run the application
    depends_on:
      db:
        condition: service_healthy  # Wait for DB service to be healthy before starting
    environment:
      ID_TYPE: UUID
      POSTGRES_HOST: db
      POSTGRES_DB: native_push
      POSTGRES_USER: native_push
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password  # Use secrets for sensitive data
      FIREBASE_SERVICE_ACCOUNT_FILE: /run/secrets/service_account
      APNS_P8_FILE: /run/secrets/apns_p8_file
      APNS_KEY_ID_FILE: /run/secrets/apns_key_id
      APNS_TEAM_ID_FILE: /run/secrets/apns_team_id
      APNS_TOPIC: com.opdehipt.nativepushclient.example
      WEB_PUSH_SUBJECT_FILE: /run/secrets/web_push_subject
      VAPID_KEYS_FILE: /run/secrets/vapid_keys
      DEVELOPMENT: true  # Enable development mode
    secrets:
      - db_password
      - service_account
      - apns_p8_file
      - apns_key_id
      - apns_team_id
      - web_push_subject
      - vapid_keys

  # PostgreSQL database service
  db:
    image: postgres:alpine  # Use Alpine-based PostgreSQL image
    restart: always
    networks:
      - postgres
    volumes:
      - ./db:/var/lib/postgresql/data  # Persist database data
    environment:
      POSTGRES_DB: native_push
      POSTGRES_USER: native_push
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password  # Use secrets for sensitive data
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready" ]  # Health check command
      interval: 5s
      timeout: 5s
      retries: 5
    secrets:
      - db_password

# Define networks for services
networks:
  nginx:
    driver: bridge
  postgres:
    driver: bridge

# Define secrets for sensitive data
secrets:
  db_password:
    file: ./secrets/db_password
  service_account:
    file: ./secrets/service_account_key.json
  apns_p8_file:
    file: ./secrets/apns_key.p8
  apns_key_id:
    file: ./secrets/apns_key_id
  apns_team_id:
    file: ./secrets/apns_team_id
  web_push_subject:
    file: ./secrets/web_push_subject
  vapid_keys:
    file: ./secrets/vapid_keys
```

1. Create the necessary secret files and place them in the `./secrets` directory.

2. Create the necessary configuration files for nginx in the `./nginx` directory.

3. Build and start the services:

    ```bash
    docker-compose up --build
    ```

The application will be accessible on port 80 and 443 via nginx.

## Project Structure

- **main.kt**: Entry point of the application. Configures the server environment and starts the embedded server.
- **plugins**: Contains Ktor plugin configurations for monitoring, serialization, native push notifications, and routing.
- **native_push**: Implements the native push notification logic.
- **IdType.kt**: Defines the abstract class `IdType` and its implementations for different user ID types.

## API Endpoints

- **POST /{userId}/token**: Add a new notification token.
- **PUT /{userId}/token/{id}**: Update an existing notification token.
- **DELETE /{userId}/token/{id}**: Delete a notification token.
- **POST /{userId}/send-notification**: Send a notification to the user.

### Request and Response Formats

- **TokenRequest**:
    ```json
    {
        "token": "notification_token",
        "system": "FCM"
    }
    ```

- **NewTokenResponse**:
    ```json
    {
        "id": "token_id"
    }
    ```

- **SendNotificationRequest**:
    ```json
    {
        "title": "Notification Title",
        "body": "Notification Body",
        "imageUrl": "http://example.com/image.png",
        "channelId": "channel_id",
        "sound": "default",
        "icon": "http://example.com/icon.png",
        "collapseKey": "collapse_key",
        "priority": "HIGH",
        "data": {
            "key1": "value1",
            "key2": "value2"
        }
    }
    ```

- **SuccessResult**:
    ```json
    {
        "success": true
    }
    ```
