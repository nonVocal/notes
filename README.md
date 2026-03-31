# Notes Multi-Module Maven Project

Ein Multi-Module Maven-Projekt für eine Notes-Anwendung mit den folgenden Modulen:

## Projektstruktur

```
notes/
├── pom.xml                 # Parent POM
├── notes-core/             # Core Modul (Business Logic)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/notes/core/
│       │   ├── entity/
│       │   └── service/
│       └── test/java/com/example/notes/core/
├── notes-api/              # API Modul (REST Endpoints)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/notes/api/
│       │   └── controller/
│       └── test/java/com/example/notes/api/
└── notes-web/              # Web Modul (Main Application)
    ├── pom.xml
    └── src/
        ├── main/java/com/example/notes/web/
        │   └── controller/
        └── test/java/com/example/notes/web/
```

## Module

### 1. notes-core
- **Zweck**: Enthält die Business-Logik und Entities
- **Abhängigkeiten**: JUnit
- **Abhängigkeiten von anderen Modulen**: Keine

### 2. notes-api
- **Zweck**: Bietet REST API Endpoints
- **Abhängigkeiten**: notes-core, Spring Boot Web
- **Abhängigkeiten von anderen Modulen**: notes-core

### 3. notes-web
- **Zweck**: Hauptanwendung mit Web UI
- **Abhängigkeiten**: notes-api, Spring Boot Thymeleaf
- **Abhängigkeiten von anderen Modulen**: notes-api (transitiv auch notes-core)

## Build

```bash
# Alle Module bauen
mvn clean package

# Nur ein bestimmtes Modul bauen
mvn clean package -pl notes-core

# Web-Anwendung starten
mvn spring-boot:run -pl notes-web
```

## Versioning

- **Projekt-Version**: 1.0.0
- **Java Version**: 11
- **Spring Boot Version**: 2.7.0

## Konfiguration

- **GroupId**: com.example
- **Encoding**: UTF-8
- **Compiler Source/Target**: 11

