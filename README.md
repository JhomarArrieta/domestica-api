# Sistema de Organización de Tareas Domésticas

Backend desarrollado con Spring Boot para la gestión de tareas domésticas en grupos familiares. El sistema permite la autenticación de usuarios y la consulta de su perfil, incluyendo los grupos (hogares) a los que pertenece.

---

## Tecnologías utilizadas

- Java 21
- Spring Boot
- Spring Security
- JWT (JSON Web Token)
- PostgreSQL
- Maven
- JPA / Hibernate

---

## Arquitectura

El sistema utiliza una arquitectura en capas, organizada por módulos de dominio.

Estructura principal:

com.domesticas
│
├── auth
│   ├── controller
│   ├── service
│   └── dto
│       ├── request
│       └── response
│
├── usuario
│   ├── controller
│   ├── service
│   ├── repository
│   ├── model
│   └── dto
│       └── response
│
├── hogar
│   ├── model
│   └── repository
│
├── security
├── config
└── exception

Cada módulo contiene sus propias capas:
- controller: expone endpoints
- service: lógica de negocio
- repository: acceso a datos
- model: entidades JPA
- dto: entrada y salida de datos

Se implementa como un monolito modular, lo que permite escalar hacia microservicios en futuros sprints.

---

## Funcionalidades implementadas (Sprint 1)

### 1. Registro de usuario

Endpoint:
POST /api/auth/register

Descripción:
Permite registrar un usuario con nombre, correo electrónico y contraseña.

Validaciones:
- El correo no debe existir previamente
- La contraseña se encripta antes de almacenarse

Ejemplo de request:

{
  "nombre": "Juan",
  "email": "juan@gmail.com",
  "password": "12345678"
}

Respuesta:

{
  "id": 1,
  "nombre": "Juan",
  "email": "juan@gmail.com",
  "mensaje": "Usuario registrado exitosamente"
}

---

### 2. Inicio de sesión

Endpoint:
POST /api/auth/login

Descripción:
Permite autenticar un usuario y generar un token JWT.

Ejemplo de request:

{
  "email": "juan@gmail.com",
  "password": "12345678"
}

Respuesta:

{
  "id": 1,
  "nombre": "Juan",
  "email": "juan@gmail.com",
  "token": "JWT_TOKEN",
  "mensaje": "Inicio de sesión exitoso"
}

---

### 3. Perfil de usuario

Endpoint:
GET /api/usuarios/perfil

Requiere:
Authorization: Bearer TOKEN

Descripción:
Obtiene la información del usuario autenticado junto con los grupos (hogares) a los que pertenece.

Ejemplo de respuesta:

{
  "id": 1,
  "nombre": "Juan",
  "email": "juan@gmail.com",
  "grupos": [],
  "mensaje": "Perfil obtenido correctamente"
}

---

## Seguridad

El sistema implementa seguridad basada en JWT.

Componentes:

- JwtService: generación y validación de tokens
- JwtAuthenticationFilter: filtro que intercepta las peticiones
- CustomUserDetailsService: carga de usuario desde base de datos
- SecurityConfig: configuración de seguridad

Flujo:

1. El usuario se autentica en login
2. Se genera un token JWT
3. El cliente envía el token en cada petición protegida
4. El backend valida el token antes de procesar la solicitud

---

## Manejo de errores

Se implementa un manejador global de excepciones:

- BadRequestException → 400
- UnauthorizedException → 401
- Errores internos → 500

Respuesta estándar:

{
  "status": 401,
  "message": "Credenciales inválidas"
}

---

## Base de datos

Modelo relacional diseñado para soportar múltiples hogares por usuario.

Tablas principales:

- usuarios
- roles
- hogares
- miembros_hogar
- tareas
- asignaciones_tareas
- historial_tareas
- reportes

### Relación clave

La tabla miembros_hogar permite:

- asociar usuarios con hogares
- asignar un rol por hogar
- definir si es administrador

Ejemplo:

usuario_id | hogar_id | rol_id | es_administrador

Esto permite que un usuario sea:
- Padre en un hogar
- Hijo en otro
- Tutor en otro

---

## Estado actual del proyecto

- Sprint 1 completado
- Autenticación funcional
- JWT implementado
- Perfil de usuario funcional
- Arquitectura modular implementada
- Base de datos preparada para expansión


## Sprint 2


Este proyecto corresponde al backend de un sistema para la gestión de tareas domésticas, donde los usuarios pueden crear hogares, unirse a ellos y administrar tareas asignadas.

En este Sprint 2 se evolucionó el sistema hacia una arquitectura más robusta, se ampliaron las funcionalidades del dominio y se implementó integración continua y despliegue en la nube.

## Arquitectura

El proyecto está construido como un monolito modular, organizado por dominios:

com.domesticas
│
├── auth/
├── usuario/
├── hogar/
├── tarea/
│
├── security/
├── config/
├── exception/

Cada módulo contiene:

controller
service
repository
model
dto

## Seguridad

Autenticación mediante JWT
Protección de endpoints
Filtro de seguridad personalizado (JwtAuthenticationFilter)

## Funcionalidades Implementadas

# Usuario
Registro
Login
Obtener perfil autenticado

# Hogar
Crear hogar
Unirse a hogar
Salirse del hogar
Ver miembros del hogar
Gestión de roles dentro del hogar

# Solicitudes
Enviar solicitud de ingreso
Aceptar solicitud
Rechazar solicitud

# Tareas
Crear tarea
Asignar tarea
Cambiar estado de tarea
Consultar tareas por usuario
Consultar tareas del hogar
 
## HATEOAS

Se implementó HATEOAS para enriquecer las respuestas de la API, permitiendo navegar entre recursos mediante enlaces incluidos en las respuestas.

# Documentación

Se integró Swagger (OpenAPI) para la documentación interactiva de la API:

/swagger-ui/index.html
 Integración Continua (CI)

Se configuró GitHub Actions para automatizar:

Build del proyecto con Maven
Validación automática en cada push y pull request

Archivo:

.github/workflows/ci.yml

# Despliegue

La aplicación fue desplegada en la nube usando Render.

URL del servicio:

https://domestica-api.onrender.com

Nota:
El endpoint raíz (/) puede devolver 404 Not Found, ya que la API está diseñada para trabajar con rutas específicas.

Contenerización

Se utilizó Docker para facilitar el despliegue en Render.

Archivo:

Dockerfile
 Variables de Entorno

Configuradas en Render:

DB_URL
DB_USER
DB_PASSWORD
JWT_SECRET
PORT

# Tecnologías Utilizadas

Java 21
Spring Boot
Spring Security
JWT
Maven
PostgreSQL (Supabase)
Swagger (OpenAPI)
GitHub Actions
Docker
Render

# Resultados del Sprint 2

En este sprint se logró:

Evolucionar la arquitectura a un modelo modular
Implementar múltiples endpoints funcionales
Incorporar navegación con HATEOAS
Documentar la API con Swagger
Automatizar el build con CI
Desplegar la aplicación en producción
Estado del Proyecto

Backend funcional
Seguridad implementada
API documentada
CI configurado
Deploy en producción
