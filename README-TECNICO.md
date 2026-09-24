# 📐 README Técnico — Sistema de Semilleros UdeA

Documentación técnica detallada de arquitectura, patrones, decisiones de diseño y guías de desarrollo.

---

## Arquitectura Hexagonal

### Capas y responsabilidades

```
src/main/java/co/udea/semilleros/
│
├── domain/                          ← NÚCLEO DEL DOMINIO (sin dependencias externas)
│   ├── model/                       ← Entidades de dominio inmutables (Lombok @Builder + @With)
│   │   ├── Semillero.java
│   │   ├── Coordinador.java
│   │   ├── Inscripcion.java
│   │   ├── Campus.java
│   │   ├── UnidadAcademica.java
│   │   ├── AreaOcde.java
│   │   ├── SemilleroFiltro.java     ← Value Object para filtros
│   │   └── PageResult.java          ← Value Object genérico de paginación
│   ├── exception/                   ← Excepciones específicas de dominio
│   │   ├── SemillerosException.java          (base abstracta)
│   │   ├── RecursoNoEncontradoException.java
│   │   ├── DominioCorreoNoPermitidoException.java
│   │   ├── CredencialesInvalidasException.java
│   │   ├── AccesoNoAutorizadoException.java
│   │   ├── SemilleroYaExisteException.java
│   │   ├── InscripcionDuplicadaException.java
│   │   ├── CamposObligatoriosPendientesException.java
│   │   ├── ValidacionBotException.java
│   │   └── TokenInvalidoException.java
│   └── port/
│       ├── in/                      ← Puertos primarios (contratos de casos de uso)
│       │   ├── ConsultarSemillerosUseCase.java
│       │   ├── InscribirseASemilleroUseCase.java
│       │   ├── AutenticarCoordinadorUseCase.java
│       │   ├── GestionarSemilleroUseCase.java     ← Creación, pestañas y finalización
│       │   └── ConsultarFiltrosUseCase.java
│       └── out/                     ← Puertos secundarios (contratos de repositorios/servicios)
│           ├── SemilleroRepositoryPort.java
│           ├── InscripcionRepositoryPort.java
│           ├── CoordinadorRepositoryPort.java
│           ├── FiltrosRepositoryPort.java
│           ├── NotificacionEmailPort.java
│           ├── SemilleroIntegranteRepositoryPort.java  ← Pestañas de caracterización (Sprint 3)
│           ├── ProduccionAcademicaRepositoryPort.java
│           ├── OrganizacionSemilleroRepositoryPort.java
│           ├── RelacionamientoRepositoryPort.java
│           ├── ActividadesRepositoryPort.java
│           ├── DofaRepositoryPort.java
│           └── OdsRepositoryPort.java
│
├── application/                     ← CAPA DE APLICACIÓN (orquestación)
│   └── usecase/                     ← Implementaciones de los casos de uso
│       ├── ConsultarSemillerosUseCaseImpl.java
│       ├── InscribirseASemilleroUseCaseImpl.java
│       ├── AutenticarCoordinadorUseCaseImpl.java
│       ├── GestionarSemilleroUseCaseImpl.java     ← Borrador, 7 pestañas, finalización, inscripciones
│       └── ConsultarFiltrosUseCaseImpl.java
│
└── infrastructure/                  ← CAPA DE INFRAESTRUCTURA
    ├── adapter/
    │   ├── in/rest/                 ← Adaptadores de entrada (HTTP)
    │   │   ├── controller/          ← REST Controllers con Swagger
    │   │   │   ├── SemilleroController.java          (público)
    │   │   │   ├── InscripcionController.java        (público)
    │   │   │   ├── FiltrosController.java             (público)
    │   │   │   ├── AuthController.java                (público)
    │   │   │   └── CoordinadorSemilleroController.java (protegido, borrador + 7 pestañas)
    │   │   ├── dto/
    │   │   │   ├── request/         ← DTOs de entrada con Bean Validation
    │   │   │   └── response/        ← DTOs de salida
    │   │   └── mapper/              ← MapStruct: Domain ↔ DTO
    │   └── out/
    │       ├── persistence/         ← Adaptadores de salida (JPA)
    │       │   ├── entity/          ← Entidades JPA
    │       │   ├── mapper/          ← MapStruct: Entity ↔ Domain
    │       │   ├── repository/      ← Repositorios JPA (interfaces)
    │       │   ├── SemilleroRepositoryAdapter.java
    │       │   ├── InscripcionRepositoryAdapter.java
    │       │   ├── CoordinadorRepositoryAdapter.java
    │       │   ├── FiltrosRepositoryAdapter.java
    │       │   ├── SemilleroIntegranteRepositoryAdapter.java
    │       │   ├── ProduccionAcademicaRepositoryAdapter.java
    │       │   ├── OrganizacionSemilleroRepositoryAdapter.java
    │       │   ├── RelacionamientoRepositoryAdapter.java
    │       │   ├── ActividadesRepositoryAdapter.java
    │       │   ├── DofaRepositoryAdapter.java
    │       │   └── OdsRepositoryAdapter.java
    │       └── email/               ← Adaptador de correo (SendGrid + Spring Mail)
    │           └── NotificacionEmailAdapter.java
    ├── config/
    │   ├── SecurityConfig.java      ← Configuración Spring Security
    │   ├── OpenApiConfig.java       ← Configuración Swagger
    │   ├── CacheConfig.java         ← Caché de filtros (Spring Cache)
    │   ├── InputSanitizer.java      ← Sanitización de campos de texto libre
    │   └── GlobalExceptionHandler.java ← Manejo global de excepciones
    └── security/
        ├── jwt/
        │   └── JwtTokenProvider.java
        └── filter/
            ├── JwtAuthenticationFilter.java
            ├── RateLimitFilter.java  ← Rate limiting con Bucket4j
            └── CoordinadorPrincipal.java
```

---

## Patrones de Diseño Aplicados

| Patrón | Dónde se aplica |
|---|---|
| **Ports & Adapters** | Toda la arquitectura hexagonal |
| **Repository** | `*RepositoryPort` → `*RepositoryAdapter` |
| **Use Case / Command** | `*UseCase` interfaces + `*UseCaseImpl` |
| **DTO (Data Transfer Object)** | `*Request` / `*Response` |
| **Mapper** | MapStruct en capas REST y persistencia |
| **Builder** | Todos los modelos de dominio (Lombok) |
| **Factory Method** | `ApiResponse.exito()` / `ApiResponse.error()` |
| **Chain of Responsibility** | Filtros de seguridad Spring Security |
| **Template Method** | `OncePerRequestFilter` en JWT filter |
| **Immutable Value Object** | `SemilleroFiltro`, `PageResult` |

---

## Decisiones de Diseño

### ¿Por qué Flyway y no Liquibase?

Flyway fue elegido por:
- Sintaxis SQL pura sin XML/YAML, más cercana al modelo de datos.
- Menor curva de aprendizaje para el equipo.
- Integración nativa con Spring Boot 3.x sin configuración adicional.
- Historial lineal de versiones (`V1__`, `V2__`, `V3__`).

### ¿Por qué modelos de dominio inmutables?

Los modelos usan `@Builder` y `@With` de Lombok para garantizar:
- Inmutabilidad en el dominio (principio de diseño limpio).
- Thread-safety sin sincronización.
- Facilidad de testing sin efectos secundarios.

### ¿Por qué excepciones específicas y no genéricas?

Cada excepción de dominio extiende `SemillerosException` y tiene su propio `errorCode`. Esto permite:
- Respuestas HTTP semánticas (404, 401, 403, 409, 422).
- Mensajes de error claros para el cliente.
- Manejo centralizado en `GlobalExceptionHandler`.
- Sin `catch (Exception e)` en la lógica de negocio.

### Seguridad anti-inyección SQL

- **Todo acceso a BD** usa JPA con parámetros nombrados (`@Param`).
- **Búsqueda por texto** usa `LIKE LOWER(CONCAT('%', :param, '%'))` con parámetro vinculado.
- **Sin concatenación** de strings en queries.
- **Bean Validation** en DTOs valida formatos antes de llegar a la BD.

---

## Variables de Entorno Requeridas

| Variable | Descripción | Ejemplo |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil activo | `dev`, `cert`, `pdn` |
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/semilleros_dev` |
| `DB_USERNAME` | Usuario de BD | `semilleros_user` |
| `DB_PASSWORD` | Contraseña de BD | (secreto) |
| `JWT_SECRET` | Clave HMAC-SHA256 (≥ 32 chars) | (secreto, mín. 256 bits) |
| `JWT_EXPIRATION_MS` | Expiración del token en ms | `86400000` (24h) |
| `MAIL_HOST` | Host SMTP | `smtp.udea.edu.co` |
| `MAIL_PORT` | Puerto SMTP | `587` |
| `MAIL_USERNAME` | Usuario SMTP | `correo@udea.edu.co` |
| `MAIL_PASSWORD` | Contraseña SMTP | (secreto) |
| `APP_ADMIN_CORREO` | Correo del administrador | `admin@udea.edu.co` |
| `REPORTES_INTERVALO_MS` | Cada cuánto se verifican cambios para actualizar los reportes en tiempo real | `60000` |

---

## Estándares de Código

### Convenciones de nombres

- **Clases de dominio**: sustantivos simples (`Semillero`, `Inscripcion`).
- **Puertos de entrada**: `*UseCase` (interfaces) → `*UseCaseImpl` (implementaciones).
- **Puertos de salida**: `*RepositoryPort` → `*RepositoryAdapter`.
- **DTOs**: `*Request` (entrada), `*Response` (salida).
- **Mappers REST**: `*RestMapper`, **Mappers JPA**: `*EntityMapper`.

### Reglas de capas

- El **dominio** no importa ninguna clase de `infrastructure` ni de `application`.
- La **aplicación** solo conoce puertos (interfaces), no implementaciones.
- La **infraestructura** implementa los puertos y conoce frameworks externos.
- Las **excepciones de dominio** se lanzan en la capa donde ocurre la violación de negocio, nunca se propagan como checked exceptions.

---

## Ejecución de Tests

```bash
# Todos los tests
mvn test

# Con reporte de cobertura JaCoCo
mvn verify

# Ver reporte HTML
open target/site/jacoco/index.html
```

### Cobertura mínima exigida: 85%

Las siguientes clases están excluidas de la métrica de cobertura (son boilerplate):
- `SemillerosApplication`
- Entidades JPA (`entity/**`)
- DTOs (`dto/**`)
- Modelos de dominio (`domain/model/**`)
- Configuraciones (`config/OpenApiConfig`)

---

## Configuración SonarCloud

1. Cree un proyecto en [sonarcloud.io](https://sonarcloud.io).
2. Configure los secretos en GitHub:
   - `SONAR_TOKEN`
   - `SONAR_PROJECT_KEY`
   - `SONAR_ORGANIZATION`
3. El pipeline `.github/workflows/ci.yml` analiza automáticamente en cada push a `main` o `develop`.

---

## Docker

### Construir imagen manualmente

```bash
docker build -t semilleros-udea:1.0.0 .
```

### Comandos útiles

```bash
# Levantar todo
docker-compose up -d

# Ver logs de la app
docker-compose logs -f app

# Reiniciar solo la app (tras cambios)
docker-compose restart app

# Conectar a PostgreSQL
docker exec -it semilleros-postgres psql -U semilleros_user -d semilleros_dev

# Ver UI de correos (MailHog)
open http://localhost:8025
```

---

## Flyway — Versionado de BD

| Versión | Archivo | Descripción |
|---|---|---|
| V1 | `V1__crear_tablas_base.sql` | Todas las tablas, constraints, índices |
| V2 | `V2__datos_referencia.sql` | Campus, facultades, ODS, OCDE, recursos |
| V3 | `V3__datos_ejemplo.sql` | Coordinadores y semilleros de prueba |
| V4 | `V4__extensiones.sql` | Extensiones de PostgreSQL requeridas |
| V5 | `V5__alter_semillero_nombre_nullable.sql` | Permite nombre nulo en semilleros (borradores) |
| V6 | `V6__actualizar_produccion_y_nuevas_tablas.sql` | Tablas de las pestañas de caracterización (Sprint 3) |

Para agregar una nueva migración:

```bash
# Crear archivo con el siguiente número de versión
touch src/main/resources/db/migration/V4__descripcion_cambio.sql
```

**Regla**: nunca modificar un archivo de migración ya ejecutado en producción.
