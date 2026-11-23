# Snippet Searcher — PrintScript service

Servicio de lenguaje de PrintScript para Snippet Searcher. Valida, lintea, formatea y corre los tests
de los snippets escritos en PrintScript, y guarda las reglas de lint y de formato de cada usuario.
Usa [PrintScript](https://github.com/los-implementadores/PrintScript) como librería.

La arquitectura y cómo se comunica con los otros servicios están en
[snippet-searcher-infra](https://github.com/los-implementadores/snippet-searcher-infra).

Stack: Java 21, Spring Boot 4.1, Gradle (Kotlin DSL), Postgres + Flyway, JUnit 5. Mismas quality
gates que PrintScript y el snippets service: Spotless (google-java-format), Checkstyle y PMD.

Creado desde [snippet-searcher-service-template](https://github.com/los-implementadores/snippet-searcher-service-template).

## Qué trae

| Pieza | Dónde | Qué hace |
|---|---|---|
| Resource server de Auth0 | `auth/SecurityConfig` | Todo pide `Authorization: Bearer <jwt>` salvo `/actuator/health`. Valida firma, `iss`, `aud` y `exp`. Stateless, sin CSRF |
| CORS | `auth/CorsProperties` | Orígenes permitidos por `CORS_ALLOWED_ORIGINS` (el dev server de Vite). Detrás del proxy no hace falta |
| Correlation id | `observability/CorrelationIdFilter` | Lee o genera `X-Correlation-Id`, lo devuelve en la respuesta y lo pone en el MDC. Es el mismo header que usa el asset service de la cátedra |
| Usuario en los logs | `observability/UserMdcFilter` | Pone el `sub` del token en el MDC (`userId`) |
| Log por request | `observability/RequestLogFilter` | `GET /snippets -> 200 (12 ms)`: 5xx como error y 4xx como warning. Sin query string y sin health checks |
| Llamadas a otros servicios | `http/ServiceClientFactory` | `RestClient` que reenvía el JWT del usuario y el correlation id. Timeouts: 2 s para conectar y 10 s de lectura |
| Errores RFC 7807 | `shared/ApiExceptionHandler` | Las excepciones de `shared/` salen como `application/problem+json` (404, 403, 400, 502), igual que los errores de Spring |
| Logs en JSON | `application-production.yml` | En la imagen de Docker cada línea es un JSON (ECS) con el MDC adentro. Localmente, texto con `[correlationId,userId]` |
| New Relic | `newrelic/`, `Dockerfile` | El agente viene en la imagen, apagado hasta que el ambiente le pase `NEW_RELIC_AGENT_ENABLED=true` y la license key |
| Health | Actuator | `/actuator/health`, `/actuator/health/liveness` y `/actuator/health/readiness` (para el healthcheck de Swarm) |
| CI/CD | `.github/workflows/ci-cd.yml` | Ver [Ramas e imágenes](#ramas-e-imágenes) |
| Git hooks | `hooks/` | El primer build configura `core.hooksPath`. pre-commit: formato, Checkstyle y PMD; pre-push: tests |

## Correr local

```bash
cp .env.example .env    # completar AUTH_ISSUER_URI y AUTH_AUDIENCE
```

Con la app en la IDE o con Gradle, y la base en Docker:

```bash
docker compose up -d db
./gradlew bootRun
```

O todo en Docker, igual que en los ambientes:

```bash
docker compose up --build
```

Para conseguir un token y probar con `curl`, seguí el README de
[auth0-integration](https://github.com/austral-ingsis/auth0-integration#how-to-use) (Auth0 CLI:
`auth0 test token`).

## Variables de entorno

| Variable | Default | Para qué |
|---|---|---|
| `AUTH_ISSUER_URI` | obligatoria | Tenant de Auth0, con `https://` y `/` final |
| `AUTH_AUDIENCE` | obligatoria | Identifier de la API en Auth0 |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Postgres en `localhost:${DB_PORT}` | Base del servicio |
| `PORT` / `DB_PORT` | `8080` / `5432` | Puertos en tu máquina, para levantar varios servicios a la vez |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Separados por coma. Vacío: ningún origen externo |
| `NEW_RELIC_AGENT_ENABLED` / `NEW_RELIC_LICENSE_KEY` | `false` / vacía | Solo en la imagen de Docker |

## Convenciones

- **El usuario es el `sub` del token.** Se lee con `@AuthenticationPrincipal Jwt jwt` y `jwt.getSubject()`.
  Es el mismo id en todos los servicios.
- **Scopes vs. permisos de dominio.** El scope (`read:snippets`) se chequea en `SecurityConfig` con
  `hasAuthority("SCOPE_read:snippets")`. Quién puede hacer qué con cada recurso (dueño, compartido)
  se decide en el código del servicio.
- **404 vs. 403.** Si el usuario no puede ver un recurso, la respuesta es 404, para no revelar que existe
  (`ResourceNotFoundException`). Si lo ve pero no lo puede modificar, 403 (`ForbiddenOperationException`).
- **Llamadas a otros servicios** con `ServiceClientFactory.create(url)`. Para APIs de terceros (como la
  Management API de Auth0), un `RestClient.Builder` común: el token del usuario no sale de la plataforma.
  Si un servicio falla, se envuelve en `DownstreamServiceException` (502).
- **El schema es de Flyway.** Los cambios van en `src/main/resources/db/migration` y Hibernate solo
  valida (`ddl-auto: validate`).
- **Nunca loguear tokens, contraseñas ni secretos.** El correlation id y el usuario ya están en cada línea:
  el mensaje cuenta solo qué pasó (`"snippets listed, count=0"`).

## Ramas e imágenes

| Rama | Ambiente | Imagen |
|---|---|---|
| `feature/*` | — | PR a `dev`: solo build y tests |
| `dev` | dev | `ghcr.io/los-implementadores/<repo>:dev` y `:dev-<sha>` |
| `main` | prod | `ghcr.io/los-implementadores/<repo>:prod` y `:prod-<sha>` |

Los tags con sha no cambian: sirven para volver a una versión anterior. El repo del template no publica
imágenes.

Las imágenes de un repo privado son privadas. Para que las VMs las bajen, en cada VM:
`docker login ghcr.io` con un token personal con scope `read:packages`.

## Lo que no trae, a propósito

- **Redis Streams.** Solo lo usan algunos servicios (snippets publica y printscript consume los eventos de
  lint y formateo). La librería de la cátedra está en Kotlin y su `emit` es `inline reified`, así que
  no se puede llamar desde Java. Conviene copiar el patrón con `RedisTemplate.opsForStream()` en el
  servicio que lo necesite.
- **PrintScript desde GitHub Packages.** Solo lo necesita el servicio de PrintScript. GitHub Packages
  pide credenciales aunque el paquete sea público:

  ```kotlin
  repositories {
      mavenCentral()
      maven {
          url = uri("https://maven.pkg.github.com/los-implementadores/PrintScript")
          credentials {
              username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user") as String?
              password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.key") as String?
          }
      }
  }
  dependencies { implementation("org.printscript:runner:2.0.0-SNAPSHOT") }
  ```

  Localmente van en `~/.gradle/gradle.properties` (`gpr.user` y `gpr.key`, un token con
  `read:packages`). En el `Dockerfile` hay que pasarlos como build secret, no como `ARG`, para que no
  queden en la imagen.
- **Llamadas sin usuario.** Un consumer de Redis o un job no tienen el token de nadie, así que
  `ServiceClientFactory` no manda `Authorization`. Si tiene que llamar a otro servicio protegido,
  necesita un token propio de Auth0 (Client Credentials).
