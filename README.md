# NovaBank Digital Services – Módulo 1

NovaBank Digital Services es una aplicación de consola desarrollada en Java que implementa un sistema básico de gestión bancaria.

En este módulo he desarrollado una primera versión funcional centrada en:

- El modelado del dominio
- La implementación de la lógica de negocio
- La validación mediante pruebas unitarias

La aplicación trabaja íntegramente en memoria, sin persistencia en base de datos, lo que permite centrarse en la 
estructura del código y en las reglas de negocio.

## Funcionalidades

El sistema permite:

- Gestión de clientes
- Creación y consulta de cuentas bancarias
- Operaciones financieras (depósitos, retiradas y transferencias)
- Consulta de saldo e historial de movimientos

## Arquitectura

El proyecto sigue una arquitectura en capas con separación clara de responsabilidades:

- Modelos (entidades del dominio)
- Repositorios en memoria (simulación de persistencia)
- Servicios (lógica de negocio y reglas del sistema)
- Menús (capa de presentación en consola)

La lógica de negocio está desacoplada de la interfaz de usuario, lo que permite su validación independiente mediante pruebas unitarias.


## Tecnologías utilizadas

- Java 17
- Apache Maven
- JUnit 5
- Mockito
- Git

---

## Requisitos

- Java 17 o superior
- Maven 3.8 o superior

Para comprobar las versiones instaladas:

#### java -version 
#### mvn -version

---

## Compilación

Desde la raíz del proyecto ejecutar:

mvn clean compile

---

## Ejecución

Para iniciar la aplicación:

#### mvn exec:java

---

## Ejecución de pruebas

Para ejecutar la batería completa de tests:

#### mvn test

La ejecución debe finalizar mostrando:

#### BUILD SUCCESS

---

## Estructura del proyecto

org.example
├── model        → Entidades del dominio (Cliente, Cuenta, Movimiento)
├── repository   → Almacenamiento en memoria
├── service      → Lógica de negocio y validaciones
├── menu         → Interfaz de usuario por consola
├── utilidades   → Clase auxiliar la cual se expandirá en la siguiente iteración
└── Main         → Punto de entrada de la aplicación

La lógica de negocio se encuentra encapsulada en la capa de servicios, lo que permite validar su comportamiento de forma
independiente mediante pruebas unitarias utilizando JUnit 5 y Mockito.

---

## Control de versiones

El desarrollo se ha gestionado mediante Git siguiendo una estrategia basada en ramas:

- main
- develop
- feature/*

Las funcionalidades se han desarrollado en ramas independientes y posteriormente integradas en develop.

---

## Autor

Carlos Torres León

