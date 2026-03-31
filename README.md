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
│       ├── index.html               # Sidebar layout (mirrors TUI)
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
│       │   └── action/NewNoteAction.java / ListNotesAction.java / NotesDialogAction.java
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
| `notes-web` | **`war`** | Jakarta Servlet REST API + sidebar SPA, deployed on Jetty | notes-api, Jakarta Servlet API 6 |
| `notes-tui-client` | `jar` | Full-featured Terminal UI with sidebar, search, mouse & command bar | notes-api, Lanterna 3.1.1 |
| `notes-vim-plugin` | `pom` | Vim/Neovim plugin with NERDTree-style sidebar | – (zip assembly) |
| `notes-firefox-plugin` | `pom` | Firefox WebExtension Manifest V2 | – (xpi assembly) |
| `notes-chrome-plugin` | `pom` | Chrome Extension Manifest V3 (Service Worker) | – (zip assembly) |
| `notes-idea-plugin` | `jar` | IntelliJ IDEA plugin (tool window + actions) | IntelliJ Platform SDK 2024.3 (provided) |
| `notes-osgi` | `jar` | OSGi bundle with DS annotations, processed by bnd | notes-core, OSGi DS Annotations (provided) |

---

## Shared UI Layout

All three interactive clients follow the same two-panel layout concept:

```
┌─ Sidebar ──────┬──────────────────────────────┐
│  All Notes     │                              │
│  New Note      │   Content / main area        │
│  Search        │   (updates on selection)     │
│────────────────│                              │
│  Exit / Close  │                              │
└────────────────┴──────────────────────────────┘
```

| Feature | TUI | Web | Vim |
|---|---|---|---|
| Persistent sidebar | ✅ `BorderLayout.LEFT` | ✅ CSS flexbox `<aside>` | ✅ `topleft vsplit` |
| Content area | ✅ swapped via `removeAllComponents` | ✅ CSS `.view` sections | ✅ scratch buffer in right window |
| Ctrl+F → Search | ✅ `WindowListenerAdapter` | ✅ `keydown` listener | ✅ buffer-local `<C-f>` |
| Mouse support | ✅ `MouseCaptureMode.CLICK_RELEASE` | ✅ native browser | ✅ `set mouse=a` + `<2-LeftMouse>` |
| ESC → command input | ✅ Vim-style command bar at bottom | – | ✅ Vim's native `:` command line |

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

## TUI Client

The terminal UI uses Lanterna 3.1.1 and runs in any xterm-compatible terminal.

```
┌─[ Notes ]────────┬──────────────────────────────────────┐
│  All Notes       │  Welcome to Notes TUI Client         │
│  New Note…       │  Select an option from the sidebar.  │
│  Search…         │                                      │
│──────────────────│                                      │
│  Exit            │                                      │
└──────────────────┴──────────────────────────────────────┘
: _                                    ← Vim-style command bar (ESC)
```

| Shortcut | Action |
|---|---|
| `Ctrl+F` | Open search dialog |
| `ESC` | Open Vim-style command bar at bottom |
| `:q` / `:quit` / `:exit` | Quit |
| `:new` / `:n` | New note dialog |
| `:search` / `:s` | Search dialog |
| `:all` / `:list` / `:ls` | All notes view |
| Scroll wheel | Move sidebar focus up / down |
| Mouse click | Activate any button |

```bash
mvn exec:java -pl notes-tui-client -Dexec.mainClass=dev.nonvocal.notes.tui.TuiMain -am
```

## Web UI

The single-page web application mirrors the TUI layout with a persistent left sidebar.

```
┌─ 📝 Notes ───────┬──────────────────────────────────────┐
│  🗒  All Notes   │  All Notes                           │
│  ✏️  New Note    │  ┌─────────────────────────────────┐ │
│  🔍  Search      │  │ Note title              [Delete]│ │
│                  │  └─────────────────────────────────┘ │
│  Ctrl+F — Search │                                      │
└──────────────────┴──────────────────────────────────────┘
```

| Shortcut | Action |
|---|---|
| `Ctrl+F` | Jump to Search view and focus input |
| `Enter` (in title field) | Move focus to content textarea |

After saving a note the view automatically switches to **All Notes**.  
Search filters notes client-side as you type.

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

The plugin provides a NERDTree-style persistent sidebar that mirrors the TUI layout.

```
┌─ Notes ──────────┬──────────────────────────────────┐
│  All Notes       │  [scratch buffer – notes list,   │
│  New Note        │   Markdown editor, or search      │
│  Search          │   results]                        │
│──────────────────│                                   │
│  q Close  ? Help │                                   │
└──────────────────┴──────────────────────────────────┘
```

### Build & install

```bash
mvn package -pl notes-vim-plugin
# → notes-vim-plugin/target/notes-vim-plugin-1.0.0.zip

# Unzip into Vim's runtime path
unzip notes-vim-plugin/target/notes-vim-plugin-1.0.0.zip -d ~/.vim
# or for Neovim
unzip notes-vim-plugin/target/notes-vim-plugin-1.0.0.zip -d ~/.config/nvim
```

### Configuration (`vimrc`)

```vim
let g:notes_api_url       = 'http://localhost:8080/api/notes'
let g:notes_sidebar_width = 26   " sidebar column width
let g:notes_mouse         = 1    " set mouse=a (0 to disable)

" Optional: global Ctrl+F → search (sidebar has it buffer-locally by default)
nnoremap <C-f> :NotesSearch<CR>
```

### Commands & mappings

| Mapping | Command | Action |
|---|---|---|
| `<Leader>no` | `:NotesOpen` | Open / focus sidebar |
| `<Leader>nt` | `:NotesToggle` | Toggle sidebar |
| `<Leader>nn` | `:NotesNew` | New note (prompts for title) |
| `<Leader>nl` | `:NotesList` | List all notes |
| `<Leader>ns` | `:NotesSearch` | Search notes |
| `<Leader>nd` | `:NotesDelete` | Delete note by ID |

**Inside the sidebar:**

| Key | Action |
|---|---|
| `<CR>` / double-click | Activate item |
| `<C-f>` | Search |
| `q` | Close sidebar |
| `?` | Quick help |

## IntelliJ IDEA Plugin

The plugin targets IntelliJ IDEA 2024.3+ (Community or Ultimate) and provides:

- A **Notes tool window** in the right sidebar (`View → Tool Windows → Notes`)
- A **Notes dialog** that opens the same two-panel UI in a floating, non-modal window
  (`Tools → Notes → Open Notes Dialog…`, bound to `Ctrl+Alt+N`)
- A **New Note** action under `Tools → Notes → New Note…` (also bound to `Ctrl+Shift+N`)
- A **Show All Notes** action to focus the tool window

Both the tool window and the dialog reuse the same `NotesToolWindowFactory.buildNotesPanel()`
component, so layout and behaviour are always in sync.

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
ZIP archive containing hundreds of JARs. The module uses a two-step build to handle this:

1. **`maven-dependency-plugin`** unpacks `lib/*.jar` and `lib/modules/*.jar` from the
   `ideaIC` ZIP into `target/idea-sdk/` during the `initialize` phase.
2. **`maven-antrun-plugin`** replaces the default `maven-compiler-plugin` compile step
   with an Ant `<javac>` task that uses `<fileset>` wildcards to add every unpacked
   JAR to the classpath — no need to enumerate individual module JARs.

The `ideaIC` ZIP (~800 MB) is downloaded once and cached in the local Maven repository
(`~/.m2/repository/com/jetbrains/intellij/idea/ideaIC/`).

#### IDE symbol resolution

IntelliJ IDEA's Maven importer does not place ZIP-type dependencies on the module
classpath, so without extra configuration the editor shows *"Cannot resolve symbol
'intellij'"* errors even though compilation succeeds.

To fix this, the `pom.xml` declares two additional `system`-scope dependencies that
point directly to the JARs extracted by the unpack step:

| `systemPath` | Contains |
|---|---|
| `target/idea-sdk/lib/app-client.jar` | `ToolWindowFactory`, `ToolWindow`, `Content`, `ContentFactory`, `AnAction`, `WindowManager`, … |
| `target/idea-sdk/lib/util-8.jar` | `Project`, … |

After running `mvn initialize` (or any full build) once, click **Load Maven Changes**
in IntelliJ IDEA to add these JARs to the module classpath and clear all red errors.

---

## Project Metadata

| Property | Value |
|---|---|
| GroupId | `dev.nonvocal` |
| Version | `1.0.0` |
| Java source/target | `21` |
| Encoding | `UTF-8` |
| Base package | `dev.nonvocal.notes` |
