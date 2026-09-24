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
| `POST` | `/api/v1/auth/login` | Autenticar coordinador o administrador |
| `POST` | `/api/v1/solicitudes-acceso` | Solicitar cuenta de coordinador (responde siempre `202` con un mensaje genérico) |
| `POST` | `/api/v1/solicitudes-acceso/verificar` | Confirmar el correo con el token del enlace (`{ "token" }`) |
| `POST` | `/api/v1/cuenta/activar` | Crear la contraseña con el enlace de aprobación o invitación (`{ "token", "contrasena" }`) |

### Protegidos (requieren `Authorization: Bearer <token>`)

| Método | URL | Descripción |
|---|---|---|
| `POST` | `/api/v1/coordinador/semilleros/iniciar` | Crear borrador de semillero |
| `GET` | `/api/v1/coordinador/semilleros/mis-semilleros` | Ver semilleros del coordinador |
| `GET` | `/api/v1/coordinador/semilleros/{id}` | Detalle de un semillero del coordinador |
| `GET` | `/api/v1/coordinador/semilleros/{id}/inscripciones/pendientes` | Listar solicitudes de inscripción pendientes |
| `POST` | `/api/v1/coordinador/semilleros/inscripciones/{idInscripcion}/aprobar` | Aprobar solicitud de inscripción |
| `POST` | `/api/v1/coordinador/semilleros/inscripciones/{idInscripcion}/rechazar` | Rechazar solicitud de inscripción |
| `POST` | `/api/v1/coordinador/semilleros/{id}/finalizar` | Finalizar caracterización |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/general` | Consultar / guardar pestaña General |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/produccion` | Consultar / guardar pestaña Producción Académica |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/organizacion` | Consultar / guardar pestaña Organización |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/relacionamiento` | Consultar / guardar pestaña Relacionamiento |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/actividades` | Consultar / guardar pestaña Actividades |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/dofa` | Consultar / guardar pestaña DOFA |
| `GET`/`PATCH` | `/api/v1/coordinador/semilleros/{id}/pestana/ods` | Consultar / guardar pestaña ODS |
| `GET`/`POST` | `/api/v1/coordinador/semilleros/{id}/sesiones` | Listar (`periodo`) / registrar actividad con su lista de asistencia |
| `GET`/`PUT`/`DELETE` | `/api/v1/coordinador/semilleros/sesiones/{idSesion}` | Detalle / corregir / eliminar actividad |
| `GET` | `/api/v1/coordinador/semilleros/{id}/asistencia/integrantes` | % de asistencia por integrante (`periodo`) |
| `GET` | `/api/v1/coordinador/reportes/dashboard` | Reportes calculados solo con los semilleros del coordinador |
| `GET` | `/api/v1/coordinador/reportes/rendimiento` | Rendimiento de los semilleros del coordinador |
| `GET` | `/api/v1/coordinador/reportes/semilleros` | Semilleros activos del coordinador para filtrar |

### Administrador (requieren token con rol `ADMIN`)

Todos aceptan los filtros `periodo` (`2025`, `2025-1`, `2025-2`), `tipoUnidad`
(`FACULTAD`, `ESCUELA`, `INSTITUTO`, `CORPORACION`, `SECCIONAL`), `idUnidad`, `idCampus` e `idSemillero`.

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/v1/admin/reportes/kpis` | KPIs globales con tendencia frente al período anterior |
| `GET` | `/api/v1/admin/reportes/dashboard` | Tablero completo: KPIs, unidades, campus, sexo, roles, top 5, evolución y actividades |
| `GET` | `/api/v1/admin/reportes/rendimiento` | Tabla por semillero (`pagina`, `tamano`, `orden`, `direccion`) |
| `GET` | `/api/v1/admin/reportes/semilleros` | Semilleros activos para el filtro |
| `GET` | `/api/v1/admin/reportes/exportar?formato=xlsx\|pdf\|csv` | Descarga `reporte_sigsi_AAAA-MM-DD_HHMM.<ext>` |
| `GET` | `/api/v1/admin/reportes/eventos` | Server-Sent Events: emite `datos-actualizados` cuando cambian los datos |

Asistencia: % = presentes / (presentes + ausentes) × 100. Las ausencias `EXCUSADO` se descuentan del total
esperado y los totales por unidad, campus o programa suman asistencias (no promedian porcentajes).

Usuarios y roles: las cuentas están en la tabla `usuario` (renombrada desde `coordinador` en la migración `V11`).
La columna `usuario.rol` admite `ADMIN` o `COORDINADOR` y viaja en el claim `rol` del JWT junto con `idUsuario`.
Para crear otro administrador: `UPDATE usuario SET rol = 'ADMIN' WHERE correo = '...'`.
Los reportes requieren sesión: `ADMIN` ve los generales y cada `COORDINADOR` solo los de sus semilleros.

#### Registro de coordinadores

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/v1/admin/solicitudes-acceso?estado=PENDIENTE` | Solicitudes con el correo ya confirmado (por defecto `PENDIENTE`) |
| `GET` | `/api/v1/admin/solicitudes-acceso/resumen` | Número de solicitudes por revisar |
| `POST` | `/api/v1/admin/solicitudes-acceso/{id}/aprobar` | Crea la cuenta inactiva y envía el enlace de activación (24 h) |
| `POST` | `/api/v1/admin/solicitudes-acceso/{id}/rechazar` | `{ "motivo", "bloquear" }`: notifica el motivo; `bloquear` impide volver a solicitar |
| `POST` | `/api/v1/admin/invitaciones` | `{ "nombres", "apellidos", "correo" }`: invita directamente (solo `@udea.edu.co`); reenviar invalida el enlace anterior |

Flujo: formulario en «Acceso SIGSI → Solicitar acceso» → correo de confirmación (1 h) → revisión del
administrador → enlace para crear la contraseña (24 h, un solo uso). Capas contra abuso: dominio
`@udea.edu.co`, captcha matemático, campo trampa, límite por IP (3 solicitudes/hora), una solicitud en curso por
correo y cédula, reenvíos cada 10 min (máx. 3/día), espera de 30 días tras un rechazo, respuestas genéricas que no
revelan si un correo existe, y limpieza horaria de solicitudes y enlaces vencidos. Los tokens solo se guardan como
hash SHA-256.

Propiedades (`application.properties`): `app.frontend.url` (variable `FRONTEND_URL`, base de los enlaces de los
correos), `app.accesos.verificacion-horas`, `app.accesos.activacion-horas`, `app.accesos.minutos-entre-envios`,
`app.accesos.max-envios-dia`, `app.accesos.espera-rechazo-dias`, `app.accesos.limpieza-cron` y
`app.accesos.resumen-cron` (resumen diario de pendientes a los administradores). En el perfil `dev`,
`app.mail.registrar-enlaces-sin-envio=true` escribe los enlaces en el log cuando no hay clave de SendGrid;
**nunca** debe activarse en producción.

---

## Seguridad

- **JWT** con firma HMAC-SHA256 (≥ 256 bits de secreto).
- **Dominio restringido**: solo `@udea.edu.co` para el login de coordinadores. La inscripción de
  estudiantes (`POST /api/v1/inscripciones`) no restringe el dominio del correo.
- **Anti-bot**: operación matemática requerida en login y en la solicitud de acceso.
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
