# Sistema de Gestión de Semilleros de Investigación — Universidad de Antioquia

Sistema web para la gestión integral de los semilleros de investigación de la Universidad de Antioquia. Permite a visitantes consultar la oferta investigativa, a estudiantes inscribirse de forma guiada y a coordinadores caracterizar sus semilleros mediante un formulario estructurado.

---

## Tabla de Contenidos

- [Descripción](#descripción)
- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura)
- [Requisitos](#requisitos)
- [Inicio Rápido](#inicio-rápido)
- [Ambientes](#ambientes)
- [Endpoints Principales](#endpoints-principales)
- [Seguridad](#seguridad)
- [Sprints](#sprints)
- [Contribuir](#contribuir)

---

## Descripción

La ausencia de un sistema centralizado para los semilleros genera ineficiencias operativas que afectan la visibilidad, el crecimiento y el seguimiento de estos grupos. Este proyecto centraliza la gestión, habilitando:

- **Visitantes**: consultar y filtrar semilleros activos de forma paginada.
- **Estudiantes** (`@udea.edu.co`): inscribirse a cualquier semillero disponible.
- **Coordinadores** (`@udea.edu.co`): caracterizar su semillero mediante formulario por pestañas con guardado parcial.

---

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.3 |
| Seguridad | Spring Security + JWT (JJWT 0.12) |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL 16 |
| Migraciones | Flyway |
| Documentación API | SpringDoc OpenAPI 3 (Swagger UI) |
| Mapeo de objetos | MapStruct 1.5 |
| Envío de correo | Spring Mail + SendGrid |
| Rate limiting | Bucket4j |
| Gestión de proyecto | Maven |
| Calidad de código | SonarCloud + JaCoCo (≥ 85 %) |
| Contenedores | Docker + Docker Compose |
| CI/CD | GitHub Actions |

---

## Arquitectura

El proyecto sigue **Arquitectura Hexagonal (Ports & Adapters)** con principios de **Clean Architecture** y **SOLID**:

```
┌────────────────────────────────────────────────────┐
│                  INFRAESTRUCTURA                    │
│  ┌──────────────┐          ┌──────────────────────┐│
│  │ REST Controllers│        │ JPA Repositories     ││
│  │ DTOs / Mappers │        │ Email Adapter        ││
│  └──────┬───────┘          └─────────┬────────────┘│
│         │ Puerto de Entrada          │ Puerto Salida│
├─────────▼────────────────────────────▼─────────────┤
│                  APLICACIÓN                         │
│            Use Cases (Orchestrators)                │
├────────────────────────────────────────────────────┤
│                    DOMINIO                          │
│     Models │ Exceptions │ Port Interfaces           │
└────────────────────────────────────────────────────┘
```

---

## Requisitos

- Java 21+
- Maven 3.9+
- Docker & Docker Compose 2+
- (Opcional) PostgreSQL 16 local

---

## Inicio Rápido

### 1. Clonar el repositorio

```bash
git clone https://github.com/udea/semilleros-investigacion.git
cd semilleros-investigacion
```

### 2. Configurar variables de entorno

```bash
cp .env.example .env
# Edite .env con sus valores (especialmente JWT_SECRET)
```

### 3. Levantar con Docker Compose

```bash
docker-compose up -d
```

Esto inicia:
- **PostgreSQL** en `localhost:5432`
- **MailHog** (correo local) en `localhost:8025`
- **Aplicación** en `http://localhost:8080`

### 4. Verificar la API

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### 5. Ejecutar solo la base de datos (desarrollo local)

```bash
docker-compose up -d postgres mailhog
mvn spring-boot:run -Pdev
```

---

## Ambientes

| Perfil | Descripción | Activación |
|---|---|---|
| `dev` | Desarrollo local con H2/Postgres local y MailHog | Por defecto |
| `cert` | Certificación / QA | `-Pcert` o `SPRING_PROFILES_ACTIVE=cert` |
| `pdn` | Producción | `-Ppdn` o `SPRING_PROFILES_ACTIVE=pdn` |

---

## Endpoints Principales

### Públicos (sin autenticación)

| Método | URL | Descripción |
|---|---|---|
| `GET` | `/api/v1/semilleros` | Listar semilleros activos (paginado, max 15) |
| `GET` | `/api/v1/semilleros/{id}` | Detalle de un semillero |
| `POST` | `/api/v1/inscripciones` | Inscribirse a un semillero |
| `GET` | `/api/v1/filtros/unidades-academicas` | Filtros de unidades |
| `GET` | `/api/v1/filtros/campus` | Filtros de campus |
| `GET` | `/api/v1/filtros/areas-ocde` | Filtros de áreas OCDE |
| `GET` | `/api/v1/auth/captcha-math` | Obtener desafío anti-bot |
| `POST` | `/api/v1/auth/login` | Autenticar coordinador |

### Protegidos (requieren `Authorization: Bearer <token>`)

| Método | URL | Descripción |
|---|---|---|
| `POST` | `/api/v1/coordinador/semilleros/iniciar` | Crear borrador de semillero |
| `GET` | `/api/v1/coordinador/semilleros/mis-semilleros` | Ver semilleros del coordinador |
| `GET` | `/api/v1/coordinador/semilleros/{id}` | Detalle de un semillero del coordinador |
| `POST` | `/api/v1/coordinador/semilleros/{id}/finalizar` | Finalizar caracterización |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/general` | Consultar / guardar pestaña General |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/produccion` | Consultar / guardar pestaña Producción Académica |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/organizacion` | Consultar / guardar pestaña Organización |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/relacionamiento` | Consultar / guardar pestaña Relacionamiento |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/actividades` | Consultar / guardar pestaña Actividades |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/dofa` | Consultar / guardar pestaña DOFA |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/ods` | Consultar / guardar pestaña ODS |

---

## Seguridad

- **JWT** con firma HMAC-SHA256 (≥ 256 bits de secreto).
- **Dominio restringido**: solo `@udea.edu.co` para el login de coordinadores. La inscripción de
  estudiantes (`POST /api/v1/inscripciones`) no restringe el dominio del correo.
- **Anti-bot**: operación matemática requerida en login.
- **Contraseñas hasheadas** con BCrypt (factor 12).
- **Anti-inyección SQL**: uso exclusivo de JPA con parámetros nombrados (sin SQL concatenado).
- **CORS** configurable por ambiente.
- **Headers de seguridad** gestionados por Spring Security.

---

## Sprints

| Sprint | Historias | Estado |
|---|---|---|
| Sprint 1 | HU-1 Visualización, HU-2 Filtros, HU-3 Detalle, HU-4 Inscripción | ✅ Implementado |
| Sprint 2 | HU-5 Acceso formulario, HU-6 Código único, HU-7 Navegación pestañas, HU-8/9 Guardado parcial | ✅ Implementado |
| Sprint 3 | HU-11 Actividades, HU-12/13 Navegación/Finalización, HU-15 Persistencia borrador | ✅ Implementado |

---

## Contribuir

1. Cree una rama desde `develop`: `git checkout -b sprint/HU-XX-descripcion`
2. Implemente los cambios siguiendo las guías de código del proyecto.
3. Asegúrese de que los tests pasen: `mvn verify`
4. Abra un Pull Request hacia `develop`.
5. El pipeline de CI verificará cobertura ≥ 85 % y análisis SonarCloud.
