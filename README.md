# NovaBank Digital Services – Módulo 2

NovaBank Digital Services es una aplicación de consola desarrollada en Java que implementa un sistema de gestión
bancaria.

En esta segunda iteración del proyecto, la aplicación evoluciona desde una versión en memoria a una versión con
persistencia real utilizando PostgreSQL. Para ello se ha refactorizado la estructura del proyecto, incorporando una
arquitectura por capas, repositorios JDBC, gestión de transacciones y una suite de pruebas ampliada.

## Funcionalidades

El sistema permite:

- Gestión de clientes

    - creación de clientes

    - búsqueda por ID

    - búsqueda por DNI/NIF

    - listado de clientes

- Gestión de cuentas

    - creación de cuentas asociadas a un cliente

    - consulta de cuentas de un cliente

    - consulta de información de una cuenta

- Operaciones financieras

    - depósitos

    - retiradas

    - transferencias entre cuentas

- Consultas

    - saldo actual

    - historial completo de movimientos

    - movimientos por rango de fechas

## Arquitectura por capas

El proyecto sigue una arquitectura en capas con separación clara de responsabilidades:

- **domain**

    - entidades del dominio (`Cliente`, `Cuenta`, `Movimiento`, `TipoMovimiento`)

    - factorías del dominio como `MovimientoFactory`

- **persistence**

    - `repository`: contratos de persistencia

    - `jdbc`: implementaciones JDBC reales contra PostgreSQL

- **service**

    - lógica de negocio

    - validaciones

    - coordinación de operaciones complejas y transacciones

- **presentation.menu**

    - interacción por consola con el usuario

- **config**

    - configuración de conexión y factoría de repositorios

- **exception**

    - excepciones personalizadas del sistema

- **util**

    - utilidades auxiliares

- **Main**

    - punto de entrada de la aplicación

La lógica de negocio se mantiene desacoplada tanto de la presentación como del acceso a datos, permitiendo pruebas
unitarias con Mockito y pruebas de integración contra PostgreSQL.

## Persistencia y transacciones

A diferencia del módulo 1, esta versión utiliza PostgreSQL como sistema de persistencia.

La capa JDBC se encarga de:

- insertar y consultar clientes, cuentas y movimientos

- mapear resultados SQL a objetos del dominio

- mantener la atomicidad de operaciones críticas como la transferencia entre cuentas

La operación de transferencia utiliza una única conexión JDBC compartida durante toda la transacción para garantizar
que, si se produce un fallo, el `rollback` revierta todos los cambios.

## Patrones de diseño aplicados

En esta iteración se aplican los siguientes patrones y decisiones de diseño:

- **Singleton** en `DatabaseConnectionManager` para centralizar la configuración y apertura de conexiones JDBC.

- **Repository** mediante interfaces de persistencia (`ClienteRepository`, `CuentaRepository`, `MovimientoRepository`)
  para desacoplar los servicios de la tecnología de acceso a datos.

- **Factory** en `MovimientoFactory` para centralizar la creación de movimientos financieros.

- **Factory simple** en `RepositoryFactory` para decidir qué implementaciones de repositorio utiliza la aplicación.

- **Builder** mediante Lombok en las entidades del dominio para facilitar la construcción de objetos.

- **Strategy** para encapsular la generación del número de cuenta siguiendo el formato funcional exigido por el sistema.

## Tecnologías utilizadas

- Java 17

- Apache Maven

- PostgreSQL

- JDBC

- JUnit 5

- Mockito

- Lombok

- Git

- GitHub

---

## Requisitos

- Java 17 o superior

- Maven 3.8 o superior

- PostgreSQL disponible y accesible

Para comprobar las versiones instaladas:

```bash

java -version

mvn -version

```

---

## Variables de entorno

La conexión a PostgreSQL se configura mediante variables de entorno:

- `NOVABANK_DB_URL`

- `NOVABANK_DB_USER`

- `NOVABANK_DB_PASSWORD`

Ejemplo en PowerShell:

```powershell

$env:NOVABANK_DB_URL="jdbc:postgresql://localhost:5432/NovaBank"

$env:NOVABANK_DB_USER="postgres"

$env:NOVABANK_DB_PASSWORD="tu_password"

```

---

## Compilación

Desde la raíz del proyecto ejecutar:

```bash

mvn clean compile

```

---

## Ejecución

Para iniciar la aplicación interactiva por consola:

```bash

mvn exec:java

```

---

## Ejecución de pruebas

Para ejecutar la batería completa de tests:

```bash

mvn clean test

```

La ejecución debe finalizar mostrando:

```bash

BUILD SUCCESS

```

### Tipos de pruebas

El proyecto incluye:

- **Tests unitarios**, centrados en la lógica de negocio, usando Mockito para aislar dependencias.

- **Tests de integración**, centrados en la capa JDBC y en el comportamiento real con PostgreSQL.

Los tests de integración requieren PostgreSQL activo y configuran limpieza de tablas antes y después de su ejecución
para evitar residuos persistentes.



---

## Estructura del proyecto

```text

com.novabank

├── config

├── domain

│   ├── factory

│   └── model

├── exception

├── persistence

│   ├── jdbc

│   └── repository

├── presentation

│   └── menu

├── service

│   └── strategy

├── util

└── Main

```

---

## Control de versiones

El desarrollo se ha gestionado mediante Git siguiendo una estrategia de ramas basada en:

- `main`

- `develop`

- `feature/*`

- `refactor/*`

- `test/*`

Las funcionalidades, refactorizaciones y mejoras de testing se han desarrollado en ramas independientes y posteriormente
integradas.



---

## Repositorio

Enlace al repositorio público de GitHub:

https://github.com/CroquetaConQueso/NovaBank

---

## Autor

Carlos Torres León

