# webapp-template

A minimal Java web application template without a framework: embedded Jetty, servlets, FreeMarker templates, PostgreSQL with Flyway and Jdbi, and a Vite frontend bundled into the jar.

## Stack

- **Java 25** (Gradle toolchain), built with Gradle and packaged as a fat jar with Shadow
- **Jetty 12** (EE11 servlets), request handling on virtual threads
- **FreeMarker** for server-side HTML templates
- **PostgreSQL** with **HikariCP**, **Flyway** migrations and **Jdbi**
- **Jackson** for JSON responses
- **Argon2** password hashing (Spring Security Crypto + Bouncy Castle)
- **SLF4J / Logback** for logging
- **Vite**, **Lit** and **Web Awesome** for the frontend

## Getting started

Requirements: a JDK to run Gradle (it downloads the Java 25 toolchain and Node.js itself) and Docker.

```sh
# Start PostgreSQL
docker compose up -d

# Run the application
./gradlew run
```

The server listens on <http://localhost:8080/>. Flyway migrations run at startup.

To build a standalone jar:

```sh
./gradlew shadowJar
java -jar app/build/libs/app-all.jar
```

To build the Docker image and run it with PostgreSQL:

```sh
docker compose --profile app up -d --build
```

## Endpoints

| Method | Path        | Description                                                 |
|--------|-------------|-------------------------------------------------------------|
| GET    | `/`         | Hello page rendered with FreeMarker                         |
| GET    | `/users`    | List users as JSON                                          |
| POST   | `/users`    | Create a user (form parameters `name`, `email`, `password`) |
| GET    | `/assets/*` | Frontend assets built by Vite                               |

## Configuration

Defaults live in `app/src/main/resources/config.properties`. Every key can be overridden by an environment variable: uppercase, dots replaced by underscores (e.g. `database.url` → `DATABASE_URL`).

## Project layout

```
app/src/main/
├── java/org/example/        # Server, servlets, repository, configuration
├── resources/
│   ├── config.properties    # Default configuration
│   ├── migrations/          # Flyway SQL migrations
│   └── templates/           # FreeMarker templates
└── frontend/                # Vite project, built into static/assets
```
