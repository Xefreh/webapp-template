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

## Tests

```sh
./gradlew check
```

`check` runs:

- `test`: JUnit tests under `app/src/test/java`. Unit tests, HTTP tests against the real server on a free port, and tests against PostgreSQL started with [Testcontainers](https://testcontainers.com/) (`@DatabaseTest`);
- `smokeTest`: starts the shadow jar against an empty PostgreSQL, to catch packaging problems;
- `testFrontend`: [Vitest](https://vitest.dev/) tests under `app/src/main/frontend/test`.

Tests that need PostgreSQL are skipped when Docker is not available, except on CI (`CI` environment variable set), where they fail instead. The GitHub Actions workflow `tests.yml` runs `./gradlew check` on every pull request.

## Endpoints

| Method | Path        | Description                                                 |
|--------|-------------|-------------------------------------------------------------|
| GET    | `/`         | Hello page rendered with FreeMarker (other paths: 404)      |
| GET    | `/users`    | List users as JSON                                          |
| POST   | `/users`    | Create a user (form parameters `name`, `email`, `password`) |
| GET    | `/assets/*` | Frontend assets built by Vite                               |

Request data is validated with [YAVI](https://github.com/making/yavi): each form has a record declaring its rules (see `CreateUserRequest`). Invalid requests get a 400 with the messages grouped by field:

```json
{"errors": {"email": ["\"email\" must be a valid email address"]}}
```

## Configuration

The whole configuration lives in a properties file; environment variables are not read. By default the application uses `app/src/main/resources/config.properties`, bundled in the jar and set up for local development. To run it elsewhere, pass another file as argument. It replaces the bundled file entirely, so it must define every key:

```sh
java -jar app/build/libs/app-all.jar /path/to/config.properties
```

The `app` service of `compose.yaml` mounts `docker/config.properties` this way. Its only difference from the bundled file is the PostgreSQL host.

## Project layout

```
app/src/
├── main/
│   ├── java/org/example/        # Server, servlets, repository, configuration
│   ├── resources/
│   │   ├── config.properties    # Default configuration
│   │   ├── migrations/          # Flyway SQL migrations
│   │   └── templates/           # FreeMarker templates
│   └── frontend/                # Vite project, built into static/assets
│       └── test/                # Vitest tests
└── test/java/org/example/       # JUnit tests
docker/config.properties         # Configuration of the app service in compose.yaml
```
