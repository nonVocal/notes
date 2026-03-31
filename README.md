# Notes – Multi-Module Maven Project

A multi-module Maven project for a Notes application.  
**No framework dependencies** – plain Java 21, Jakarta Servlet 6, Lanterna, H2 and Jackson only.

---

## Project Structure

```
notes/
├── pom.xml                          # Parent POM (groupId: dev.nonvocal)
│
├── notes-core/                      # Business logic, entities, persistence
│   └── src/main/java/dev/nonvocal/notes/core/
│       ├── entity/Note.java
│       └── service/NoteService.java / NoteServiceImpl.java
│
├── notes-api/                       # Plain-Java controller layer
│   └── src/main/java/dev/nonvocal/notes/api/
│       └── controller/NoteController.java
│
├── notes-web/                       # WAR – deployed on Jetty
│   ├── src/main/java/dev/nonvocal/notes/web/servlet/
│   │   └── NotesApiServlet.java
│   └── src/main/webapp/
│       ├── WEB-INF/web.xml
│       ├── index.html
│       └── error/404.html / 500.html
│
├── notes-tui-client/                # Terminal UI (Lanterna)
│   └── src/main/java/dev/nonvocal/notes/tui/
│       ├── TuiClient.java
│       └── TuiMain.java
│
├── notes-vim-plugin/                # Vim/Neovim plugin (VimScript)
│   └── src/main/vim/
│       ├── plugin/notes.vim
│       ├── autoload/notes.vim
│       └── doc/notes.txt
│
├── notes-firefox-plugin/            # Firefox WebExtension (Manifest V2)
│   └── src/main/extension/
│       ├── manifest.json
│       ├── background/background.js
│       └── popup/popup.html / popup.js
│
├── notes-chrome-plugin/             # Chrome Extension (Manifest V3)
│   └── src/main/extension/
│       ├── manifest.json
│       ├── background/service_worker.js
│       └── popup/popup.html / popup.js
│
├── notes-idea-plugin/               # IntelliJ IDEA plugin
│   └── src/main/
│       ├── java/dev/nonvocal/notes/idea/
│       │   ├── NotesToolWindowFactory.java
│       │   └── action/NewNoteAction.java / ListNotesAction.java
│       └── resources/META-INF/plugin.xml
│
└── notes-osgi/                      # OSGi bundle with Declarative Services
    ├── bnd.bnd
    └── src/main/java/dev/nonvocal/notes/osgi/
        ├── api/NoteComponent.java
        └── impl/NoteComponentImpl.java
```

---

## Modules

| Module | Packaging | Purpose | Key Dependencies |
|---|---|---|---|
| `notes-core` | `jar` | Entities, service interfaces & implementations, persistence | H2, Jackson, JUnit 5 |
| `notes-api` | `jar` | Plain-Java controller layer | notes-core, JUnit 5 |
| `notes-web` | **`war`** | Jakarta Servlet REST API + static UI, deployed on Jetty | notes-api, Jakarta Servlet API 6 |
| `notes-tui-client` | `jar` | Terminal User Interface | notes-api, Lanterna 3.1.1 |
| `notes-vim-plugin` | `pom` | Vim/Neovim plugin (VimScript + autoload) | – (zip assembly) |
| `notes-firefox-plugin` | `pom` | Firefox WebExtension Manifest V2 | – (xpi assembly) |
| `notes-chrome-plugin` | `pom` | Chrome Extension Manifest V3 (Service Worker) | – (zip assembly) |
| `notes-idea-plugin` | `jar` | IntelliJ IDEA plugin (tool window + actions) | IntelliJ Platform SDK 2024.3 (provided) |
| `notes-osgi` | `jar` | OSGi bundle with DS annotations, processed by bnd | notes-core, OSGi DS Annotations (provided) |

---

## Technology Stack

| Concern | Choice |
|---|---|
| Language | Java 21 |
| Build | Apache Maven 3.9+ |
| Web runtime | Eclipse Jetty 12 (EE10 / Jakarta EE 10) |
| Servlet API | Jakarta Servlet 6.0 |
| Database | H2 2.3 (in-process) |
| JSON | Jackson 2.18 (databind + jsr310) |
| Terminal UI | Lanterna 3.1.1 |
| OSGi tooling | bnd-maven-plugin 7.0 |
| Testing | JUnit Jupiter 5.11 |
| IDE plugin SDK | IntelliJ Platform 2024.3 (ideaIC) |

---

## Build

```bash
# Build all modules
mvn clean package

# Build a single module (and its dependencies)
mvn clean package -pl notes-web -am

# Run tests for a specific module
mvn test -pl notes-core
```

## Run

```bash
# Start the web application on http://localhost:8080/notes
mvn jetty:run -pl notes-web -am

# Start the TUI client
mvn exec:java -pl notes-tui-client -Dexec.mainClass=dev.nonvocal.notes.tui.TuiMain -am
```

## Deploy (WAR on standalone Jetty)

```bash
# 1. Build the WAR
mvn clean package -pl notes-web -am
# → notes-web/target/notes.war

# 2. Copy to Jetty webapps directory
cp notes-web/target/notes.war $JETTY_HOME/webapps/

# 3. Start Jetty
cd $JETTY_HOME && java -jar start.jar
# → http://localhost:8080/notes
```

## Browser Extensions

```bash
# Firefox (.xpi)
mvn package -pl notes-firefox-plugin
# → notes-firefox-plugin/target/notes-firefox-1.0.0.zip  (rename to .xpi for sideloading)

# Chrome (.zip)
mvn package -pl notes-chrome-plugin
# → notes-chrome-plugin/target/notes-chrome-1.0.0.zip
```

## Vim Plugin

```bash
# Build zip
mvn package -pl notes-vim-plugin
# → notes-vim-plugin/target/notes-vim-plugin-1.0.0.zip

# Manual install (unzip into ~/.vim or ~/.config/nvim)
unzip notes-vim-plugin/target/notes-vim-plugin-1.0.0.zip -d ~/.vim
```

## IntelliJ IDEA Plugin

The plugin targets IntelliJ IDEA 2024.3+ (Community or Ultimate) and provides:

- A **Notes tool window** in the right sidebar (`View → Tool Windows → Notes`)
- A **New Note** action under `Tools → Notes → New Note…` (also bound to `Ctrl+Shift+N`)
- A **Show All Notes** action to focus the tool window

### Build

```bash
mvn package -pl notes-idea-plugin -am
# → notes-idea-plugin/target/notes-idea-plugin-1.0.0.jar
```

### Install

1. Open IntelliJ IDEA.
2. Go to **Settings → Plugins → ⚙ → Install Plugin from Disk…**
3. Select `notes-idea-plugin/target/notes-idea-plugin-1.0.0.jar`.
4. Restart the IDE.

### Build notes – IntelliJ Platform SDK

The IntelliJ Platform SDK (`com.jetbrains.intellij.idea:ideaIC`) is distributed as a
ZIP archive containing hundreds of JARs. Because Maven cannot place a ZIP on the
compile classpath automatically, and because IntelliJ 2024.3 distributes its platform
APIs across many module JARs in `lib/` and `lib/modules/`, the module uses a custom
two-step build:

1. **`maven-dependency-plugin`** unpacks `lib/*.jar` and `lib/modules/*.jar` from the
   `ideaIC` ZIP into `target/idea-sdk/` during the `initialize` phase.
2. **`maven-antrun-plugin`** replaces the default `maven-compiler-plugin` compile step
   with an Ant `<javac>` task that uses `<fileset>` wildcards to add every unpacked
   JAR to the classpath — no need to enumerate individual module JARs.

The `ideaIC` ZIP (~800 MB) is downloaded once and cached in the local Maven repository
(`~/.m2/repository/com/jetbrains/intellij/idea/ideaIC/`).

---

## Project Metadata

| Property | Value |
|---|---|
| GroupId | `dev.nonvocal` |
| Version | `1.0.0` |
| Java source/target | `21` |
| Encoding | `UTF-8` |
| Base package | `dev.nonvocal.notes` |
