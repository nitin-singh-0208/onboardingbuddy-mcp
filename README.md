# Build an MCP server for RideFlex docs

You will build a small **Model Context Protocol (MCP)** server in **Java** with **Spring Boot** and **Spring AI**. It exposes the fictional **RideFlex** markdown in **`rideflex-docs/`** to an AI client so it can answer onboarding-style questions about the project.

After you **clone** this repository, you should see **`rideflex-docs/`** (sample documentation) and this **`README.md`**. You will **create** a new folder called **`onboarding-mcp-server/`** in Step 1 — it is not part of the clone, so you build it yourself as you follow along.

**What you will have at the end**

- A runnable **`onboarding-mcp-server/`** Maven project next to **`rideflex-docs/`**
- **Tools:** `list_topics`, `search_docs`
- **Resource:** `docs://{docId}` (full markdown for one document)
- **Prompt:** `onboarding_walkthrough` (suggested flow for the assistant)

**How to use this guide**

Work through **Step 0 → Step 11** in order. After steps that add code, run the **`mvn`** commands in the checkpoints so problems show up early.

---

## What you need installed

| Requirement | How to check |
|-------------|----------------|
| **JDK 17** | `java -version` |
| **Apache Maven 3.9+** | `mvn -version` |
| **Git** | `git --version` |
| **Editor or IDE** (optional) | IntelliJ, VS Code, Cursor, or any editor you like |

**Ideas that make the rest easier (you do not have to be an expert):**

- MCP can run over different transports; here you use **STDIO** (the client runs your JAR and talks over stdin/stdout).
- On STDIO, **stdout** carries the MCP protocol. Anything else printed there can break clients — that is why you will add a file-only logger.
- MCP exposes three big ideas you will implement: **tools** (functions the model can call), **resources** (addressable content like `docs://…`), and **prompts** (starter messages for the assistant).

---

## Step 0 — Clone the repo and verify docs

Use the Git URL for *this* repository (ask your host if you are not sure which fork to use):

```bash
git clone https://github.com/<owner>/onboardingbuddy-mcp-demo.git
cd onboardingbuddy-mcp-demo
git checkout main
ls rideflex-docs
```

You should see **`rideflex-docs/`** with numbered markdown files, for example `01-overview.md`, `05-high-level-architecture.md`.

At the **repository root**, list files:

```bash
ls
```

You should **not** see **`onboarding-mcp-server/`** yet. That is expected — you create it in Step 1.

**Checkpoint:** You can open a few `rideflex-docs/*.md` files and you are ready for Step 1.

---

## Step 1 — Create the `onboarding-mcp-server` project

You will add a **new** Maven/Spring project directory **next to** `rideflex-docs/` at the repository root.

```bash
mkdir -p onboarding-mcp-server/src/main/java/com/demo/mcp/onboarding
mkdir -p onboarding-mcp-server/src/main/resources
mkdir -p onboarding-mcp-server/src/test/java/com/demo/mcp/onboarding
```

Check that the folder exists:

```bash
ls onboarding-mcp-server
```

**Checkpoint:** You have **`onboarding-mcp-server/`** at the repo root. From here on, paths like `pom.xml` mean **`onboarding-mcp-server/pom.xml`**.

### Alternative — Spring Initializr

You can scaffold the same module with **[Spring Initializr](https://start.spring.io)** instead of empty `mkdir` steps:

| Field | Value |
|--------|--------|
| **Project** | Maven |
| **Language** | Java |
| **Spring Boot** | 3.4.2 (or latest **3.4.x** offered) |
| **Group** | `com.demo.mcp` |
| **Artifact** | `onboarding-mcp-server` |
| **Java** | 17 |
| **Packaging** | Jar |

**Dependencies:** do **not** add **Spring Web** (this app has no HTTP port; MCP uses **STDIO**). Add only what you need for a minimal app — for example search **“Spring Boot”** / core starters if available, or start with **no extra dependencies** and rely on the next step to fix the `pom.xml`.

1. Click **Generate**, unzip the archive, and move/rename the folder so it sits next to **`rideflex-docs/`** as **`onboarding-mcp-server/`** (same layout as above).
2. In **Step 2**, use the **`pom.xml`** from this guide as the source of truth: either **replace** your generated `pom.xml` with that file, or **merge** in the **`spring-ai-bom` (1.1.7)** `dependencyManagement` block and the **`spring-ai-starter-mcp-server`** + **`spring-boot-starter-test`** dependencies. Initializr does not know about MCP server starters, so that merge is required.
3. If Initializr created a main class under another package (for example `com.demo.mcp.onboardingmcpserver`), **delete it** and follow **Step 5** to add **`OnboardingMcpServerApplication`** under **`com.demo.mcp.onboarding`**, or repackage to match.

Then continue from **Step 2** (or Step 3 if your `pom.xml` already matches Step 2).

---

## Step 2 — Add `pom.xml`

If you used **[Spring Initializr](https://start.spring.io)** in Step 1, treat the `pom.xml` below as the definition of this lab: **replacing** your generated file is the least error-prone option. If you prefer to merge, keep your Initializr coordinates and copy over **`dependencyManagement`** (`spring-ai-bom` **1.1.7**) and the **`dependencies`** block from this snippet.

Create or replace **`onboarding-mcp-server/pom.xml`** with this content:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.2</version>
        <relativePath/>
    </parent>

    <groupId>com.demo.mcp</groupId>
    <artifactId>onboarding-mcp-server</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>onboarding-mcp-server</name>
    <description>RideFlex docs MCP server</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>1.1.7</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-mcp-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**Why `spring-ai-bom` 1.1.7:** It matches **Spring Boot 3.4.x** and includes the MCP pieces this project uses. Do not switch this tutorial to Spring AI **2.x** / Spring Boot **4** without updating everything.

**Checkpoint:**

```bash
cd onboarding-mcp-server
mvn -q -DskipTests compile
```

---

## Step 3 — Application configuration (`application.yml`)

Create **`onboarding-mcp-server/src/main/resources/application.yml`**:

```yaml
spring:
  main:
    web-application-type: none
    banner-mode: off
  ai:
    mcp:
      server:
        name: onboarding-mcp-server
        version: 0.0.1
        type: SYNC
        capabilities:
          tool: true
          resource: true
          prompt: true

app:
  docs:
    path: ./rideflex-docs

logging:
  pattern:
    console: ""
  file:
    name: /tmp/onboarding-mcp-server.log
```

- **`web-application-type: none`** — No web server; MCP runs over **STDIO**.
- **`app.docs.path`** — Default **`./rideflex-docs`** is correct when you run commands from **`onboarding-mcp-server/`** (one level below the repo root, where `rideflex-docs` sits). You can override at runtime with **`-Dapp.docs.path=/absolute/path/to/rideflex-docs`**.

**Checkpoint:** File saved. You do not need to run the app yet.

---

## Step 4 — Logging must not corrupt STDIO (`logback-spring.xml`)

MCP on STDIO uses **stdout** for JSON-RPC. If Spring logs to the console, many MCP clients break. Send logs to a **file** only.

Create **`onboarding-mcp-server/src/main/resources/logback-spring.xml`**:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>/tmp/onboarding-mcp-server.log</file>
        <append>true</append>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

**Checkpoint:** You understand that **quiet stdout** matters for STDIO MCP (you will see the effect when you connect a client in Step 11).

---

## Step 5 — Spring Boot entrypoint

Create **`onboarding-mcp-server/src/main/java/com/demo/mcp/onboarding/OnboardingMcpServerApplication.java`**:

```java
package com.demo.mcp.onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OnboardingMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnboardingMcpServerApplication.class, args);
    }
}
```

**Checkpoint:**

```bash
cd onboarding-mcp-server
mvn -q -DskipTests package
java -jar target/onboarding-mcp-server-0.0.1-SNAPSHOT.jar
```

You should see **no** normal Spring banner or log lines on the terminal. Open **`/tmp/onboarding-mcp-server.log`** and look for `Started OnboardingMcpServerApplication`. Stop the process with **Ctrl+C**.

---

## Step 6 — Documentation service (index + tools + resource)

Spring AI’s MCP starter discovers annotated beans. The annotations you import are in **`org.springaicommunity.mcp.annotation`** (they come in through **`spring-ai-starter-mcp-server`**).

Create **`onboarding-mcp-server/src/main/java/com/demo/mcp/onboarding/DocumentationService.java`**. The comments in the file explain behavior and the read-only safety check.

```java
package com.demo.mcp.onboarding;

import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DocumentationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentationService.class);

    private static final Pattern LEADING_NUMBER_PREFIX = Pattern.compile("^\\d+-");

    private final Path docsRoot;
    private Map<String, Path> docPathsById = Map.of();

    public DocumentationService(@Value("${app.docs.path:./rideflex-docs}") String docsPath) {
        this.docsRoot = Paths.get(docsPath).toAbsolutePath().normalize();
    }

    /**
     * Build slug → file path map once at startup. Slug example: {@code 05-high-level-architecture.md}
     * → {@code high-level-architecture}. Missing dirs or empty folders: warn and continue (empty map).
     */
    @PostConstruct
    public void loadDocuments() {
        if (!Files.isDirectory(docsRoot)) {
            log.warn("Documentation directory does not exist or is not a directory: {}", docsRoot);
            this.docPathsById = Map.of();
            return;
        }

        Map<String, Path> discovered = new LinkedHashMap<>();
        try (Stream<Path> stream = Files.list(docsRoot)) {
            List<Path> mdFiles =
                    stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".md")).sorted().toList();

            if (mdFiles.isEmpty()) {
                log.warn("No .md files found under documentation path: {}", docsRoot);
                this.docPathsById = Map.of();
                return;
            }

            for (Path file : mdFiles) {
                String filename = file.getFileName().toString();
                String slug = slugFromFilename(filename);
                if (slug == null || slug.isBlank()) {
                    log.warn("Skipping file with unusable slug: {}", filename);
                    continue;
                }
                Path previous = discovered.put(slug, file);
                if (previous != null) {
                    log.warn("Duplicate documentation id '{}' for files '{}' and '{}'; using '{}'", slug, previous.getFileName(), filename, file.getFileName());
                }
            }

            this.docPathsById = Collections.unmodifiableMap(discovered);
            log.info("Loaded {} markdown documents from {}", this.docPathsById.size(), docsRoot);
        } catch (IOException ex) {
            log.warn("Failed to scan documentation directory '{}': {}", docsRoot, ex.toString());
            this.docPathsById = Map.of();
        }
    }

    static String slugFromFilename(String filename) {
        if (filename == null || !filename.endsWith(".md")) {
            return null;
        }
        String base = filename.substring(0, filename.length() - ".md".length());
        String withoutPrefix = LEADING_NUMBER_PREFIX.matcher(base).replaceFirst("");
        return withoutPrefix.toLowerCase(Locale.ROOT);
    }

    /** Only allow reads inside the configured docs root (defense in depth). */
    private boolean isInsideDocsRoot(Path candidate) {
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        Path normalizedRoot = docsRoot.toAbsolutePath().normalize();
        return normalizedCandidate.startsWith(normalizedRoot);
    }

    /**
     * MCP Resource: clients read {@code docs://overview}, {@code docs://flows}, etc.
     * Returns markdown as {@code text/markdown}, or a plain-text error message (not an exception).
     */
    @McpResource(
            uri = "docs://{docId}",
            name = "Project Documentation",
            description =
                    "Read the full content of a specific RideFlex project document by its id. Call list_topics first to see valid ids.",
            mimeType = "text/markdown")
    public McpSchema.ReadResourceResult readDocument(String docId) {
        Objects.requireNonNull(docId, "docId");
        String id = docId.trim().toLowerCase(Locale.ROOT);
        Path path = docPathsById.get(id);

        if (path == null) {
            String message =
                    "No document with id '"
                            + docId
                            + "'. Call the list_topics tool to see valid ids, then read docs://{docId} using one of those ids.";
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents("docs://" + id, "text/plain", message)));
        }

        if (!isInsideDocsRoot(path)) {
            String message = "Resolved path is outside the configured documentation root; refusing to read.";
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents("docs://" + id, "text/plain", message)));
        }

        try {
            String markdown = Files.readString(path, StandardCharsets.UTF_8);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents("docs://" + id, "text/markdown", markdown)));
        } catch (IOException ex) {
            String message = "Failed to read document '" + id + "': " + ex.getMessage();
            log.warn(message);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents("docs://" + id, "text/plain", message)));
        }
    }

    @McpTool(
            name = "list_topics",
            description =
                    "List all available RideFlex project documents, with their id and title, so you know what you can read or search.",
            annotations =
                    @McpTool.McpAnnotations(
                            readOnlyHint = true,
                            destructiveHint = false,
                            idempotentHint = true))
    public String listTopics() {
        if (docPathsById.isEmpty()) {
            return "No RideFlex markdown documents were found. Verify the configured documentation directory (app.docs.path / -Dapp.docs.path) points to a folder containing .md files.";
        }

        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Path> entry : docPathsById.entrySet()) {
            String title = readTitle(entry.getValue());
            lines.add(entry.getKey() + " — " + title);
        }
        return String.join("\n", lines);
    }

    private String readTitle(Path path) {
        try {
            String head = Files.readString(path, StandardCharsets.UTF_8);
            for (String rawLine : head.split("\\R", 200)) {
                String line = rawLine.stripLeading();
                if (line.startsWith("# ")) {
                    return line.substring(2).strip();
                }
            }
        } catch (IOException ex) {
            log.warn("Unable to read title from '{}': {}", path, ex.toString());
        }
        return path.getFileName().toString();
    }

    @McpTool(
            name = "search_docs",
            description =
                    "Search across all RideFlex documentation for a keyword or phrase and return matching excerpts with their source document id.",
            annotations =
                    @McpTool.McpAnnotations(
                            readOnlyHint = true,
                            destructiveHint = false,
                            idempotentHint = true))
    public String searchDocs(
            @McpToolParam(description = "Keyword or phrase to search for", required = true) String query,
            @McpToolParam(description = "Optional document id to restrict search to a single file", required = false) String topic) {

        if (query == null || query.isBlank()) {
            return "Please provide a non-empty query string. You can also call list_topics to see available document ids.";
        }

        String needle = query.toLowerCase(Locale.ROOT);
        Map<String, Path> targets = docPathsById;

        if (topic != null && !topic.isBlank()) {
            String topicId = topic.trim().toLowerCase(Locale.ROOT);
            Path only = docPathsById.get(topicId);
            if (only == null) {
                return "Unknown topic id '"
                        + topic
                        + "'. Call list_topics for valid ids, or omit topic to search all documents.";
            }
            if (!isInsideDocsRoot(only)) {
                return "Refusing to search outside the configured documentation root.";
            }
            targets = Map.of(topicId, only);
        }

        if (targets.isEmpty()) {
            return "No documents are loaded. Check app.docs.path / -Dapp.docs.path and ensure the folder contains .md files.";
        }

        List<String> matches = new ArrayList<>();
        int maxMatches = 8;
        int maxChars = 6000;

        outer:
        for (Map.Entry<String, Path> entry : targets.entrySet()) {
            String docId = entry.getKey();
            Path path = entry.getValue();
            if (!isInsideDocsRoot(path)) {
                continue;
            }

            String content;
            try {
                content = Files.readString(path, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                log.warn("search_docs: failed to read '{}': {}", path, ex.toString());
                continue;
            }

            for (String paragraph : splitParagraphs(content)) {
                if (paragraph.toLowerCase(Locale.ROOT).contains(needle)) {
                    String excerpt = trimExcerpt(paragraph, 900);
                    matches.add(docId + ": " + excerpt);
                    if (matches.size() >= maxMatches) {
                        break outer;
                    }
                }
            }
        }

        if (matches.isEmpty()) {
            return "No paragraphs matched '"
                    + query
                    + "'. Try a shorter keyword, call list_topics to pick a specific document id, or rephrase your search.";
        }

        StringBuilder builder = new StringBuilder();
        for (String match : matches) {
            if (builder.length() + match.length() + 1 > maxChars) {
                break;
            }
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append(match);
        }
        return builder.toString();
    }

    static List<String> splitParagraphs(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        String[] parts = normalized.split("\\n\\s*\\n");
        return Stream.of(parts).map(String::strip).filter(p -> !p.isEmpty()).collect(Collectors.toList());
    }

    private static String trimExcerpt(String paragraph, int maxLen) {
        String singleLine = paragraph.replace('\n', ' ').replaceAll("\\s+", " ").strip();
        if (singleLine.length() <= maxLen) {
            return singleLine;
        }
        return singleLine.substring(0, maxLen) + "…";
    }
}
```

**What this class is doing**

1. **`@PostConstruct`** — Scans `app.docs.path` once at startup and fills a map from **slug** (from the filename) to **file path**. Read-only.
2. **`@McpTool`** — Exposes `list_topics` and `search_docs` with documented parameters via **`@McpToolParam`**.
3. **`@McpResource`** — URI template **`docs://{docId}`**; returns **`McpSchema.ReadResourceResult`** from the MCP Java SDK.
4. **`isInsideDocsRoot`** — Ensures you never read files outside the configured docs directory.

**Checkpoint:** Package, run briefly, and confirm documents loaded:

```bash
cd onboarding-mcp-server
mvn -q -DskipTests package
java -Dapp.docs.path="$(pwd)/../rideflex-docs" -jar target/onboarding-mcp-server-0.0.1-SNAPSHOT.jar
```

Stop with **Ctrl+C**. In **`/tmp/onboarding-mcp-server.log`**, find a line like **`Loaded N markdown documents`**. On Windows, prefer an absolute **`-Dapp.docs.path=...`** if relative paths are awkward.

---

## Step 7 — MCP prompt (guided onboarding script)

A **prompt** returns **messages** for the assistant. Here you return one **user-role** message that tells the model how to use your tools and resources.

Create **`onboarding-mcp-server/src/main/java/com/demo/mcp/onboarding/OnboardingPrompts.java`**:

```java
package com.demo.mcp.onboarding;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

@Component
public class OnboardingPrompts {

    @McpPrompt(
            name = "onboarding_walkthrough",
            description = "Start a guided walkthrough of the RideFlex project for a new joiner.")
    public McpSchema.GetPromptResult onboardingWalkthrough(
            @McpArg(
                    name = "focusArea",
                    description = "Optional focus area (e.g., architecture, modules, flows, requirements)",
                    required = false)
                    String focusArea) {

        StringBuilder instructions = new StringBuilder();
        instructions.append("You are helping a new joiner learn the fictional RideFlex project from local markdown documentation exposed via MCP.\n\n");
        instructions.append("First, call the list_topics tool so you know the exact document ids.\n\n");
        instructions.append(
                "Then walk the person through the documentation in this order, reading full documents via the docs://{docId} resource as needed:\n");
        instructions.append("1) overview (doc id: overview)\n");
        instructions.append("2) high-level architecture (doc id: high-level-architecture)\n");
        instructions.append("3) modules (doc id: modules)\n");
        instructions.append("4) key flows (doc id: flows)\n\n");
        instructions.append("After each major section, pause and ask whether they have questions before continuing.\n");
        instructions.append("If they seem lost, use search_docs to find relevant excerpts.\n\n");

        if (focusArea != null && !focusArea.isBlank()) {
            instructions.append("The joiner asked to focus on: ")
                    .append(focusArea.strip())
                    .append(
                            ". Spend extra time on that theme: pull additional excerpts with search_docs and read related docs (for example requirements, features, low-level-architecture, tech-stack, glossary) when helpful.\n");
        } else {
            instructions.append("No specific focus was provided—cover the end-to-end story at a sensible depth for a first day.\n");
        }

        return new McpSchema.GetPromptResult(
                "RideFlex onboarding walkthrough",
                List.of(new McpSchema.PromptMessage(
                        McpSchema.Role.USER, new McpSchema.TextContent(instructions.toString()))));
    }
}
```

**Checkpoint:** Run **`mvn -q -DskipTests package`** again. In **`/tmp/onboarding-mcp-server.log`**, at startup you should see MCP-related lines such as **registered prompts** (wording can vary slightly by Spring AI version).

---

## Step 8 — (Optional) small unit test

Create **`onboarding-mcp-server/src/test/java/com/demo/mcp/onboarding/DocumentationServiceTest.java`**:

```java
package com.demo.mcp.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentationServiceTest {

    @Test
    void slugFromFilename_stripsNumericPrefixAndMdExtension() {
        assertThat(DocumentationService.slugFromFilename("05-high-level-architecture.md"))
                .isEqualTo("high-level-architecture");
        assertThat(DocumentationService.slugFromFilename("01-overview.md")).isEqualTo("overview");
    }

    @Test
    void splitParagraphs_splitsOnBlankLines() {
        String input = "First paragraph.\n\nSecond paragraph.\n\n\nThird.";
        assertThat(DocumentationService.splitParagraphs(input)).containsExactly("First paragraph.", "Second paragraph.", "Third.");
    }
}
```

Run:

```bash
cd onboarding-mcp-server
mvn test
```

---

## Step 9 — `.gitignore` (optional)

Create **`onboarding-mcp-server/.gitignore`**:

```gitignore
/target/
/.idea/
*.iml
.DS_Store
```

---

## Step 10 — Build and run the server (STDIO)

```bash
cd onboarding-mcp-server
mvn clean package -DskipTests
```

Run with an **absolute** path to `rideflex-docs` (works from any working directory):

```bash
java -Dspring.main.web-application-type=none \
  -Dapp.docs.path=/ABSOLUTE/PATH/TO/onboardingbuddy-mcp-demo/rideflex-docs \
  -jar target/onboarding-mcp-server-0.0.1-SNAPSHOT.jar
```

Replace **`/ABSOLUTE/PATH/TO/...`** with the real path on your machine.

Expect **no** useful logs on the terminal — read **`/tmp/onboarding-mcp-server.log`** instead.

---

## Step 11 — Connect your MCP client (e.g. Cursor)

Menus differ by tool and version; the idea is always the same:

1. Add an **MCP server** with transport **stdio**.
2. **Command:** your **`java`** (JDK 17).
3. **Arguments:** `-Dspring.main.web-application-type=none`, `-Dapp.docs.path=...`, `-jar`, and the **absolute** path to **`onboarding-mcp-server-0.0.1-SNAPSHOT.jar`**.
4. **Working directory:** keep it consistent with how you set **`app.docs.path`**.

Then in the MCP UI, try:

- Tool **`list_topics`**
- Resource **`docs://overview`**
- Prompt **`onboarding_walkthrough`**

---

## If something goes wrong

| What you see | Likely cause | What to try |
|--------------|----------------|-------------|
| Client errors or garbled protocol on connect | Logs going to **stdout** | Confirm **`logback-spring.xml`** exists and only uses the **FILE** appender |
| Log says **`Loaded 0 markdown documents`** | Wrong **`app.docs.path`** or wrong cwd | Use an absolute **`-Dapp.docs.path=...`** pointing at **`rideflex-docs`** |
| Compile errors on **`McpSchema`** | Version skew | Keep **`spring-ai-bom` 1.1.7** and Spring Boot **3.4.2** as in this guide |
| IDE cannot resolve **`org.springaicommunity.mcp.annotation`** | Stale IDE index | Run **`mvn -q -DskipTests compile`**, then reimport the Maven project |

**Windows:** Use a quoted or Windows-style absolute path for **`-Dapp.docs.path`** if spaces or drive letters cause issues. WSL paths and Windows paths are easy to mix up — pick one environment and stay consistent.

---

## You are done when…

You can tick these off for yourself:

- [ ] You can explain **tool** vs **resource** vs **prompt** in your own words.
- [ ] Your server **only reads** files under **`app.docs.path`**.
- [ ] You kept **stdout** clean for STDIO MCP.
- [ ] Your client can run **`list_topics`**, read **`docs://…`**, and call **`search_docs`**.
- [ ] Your client can fetch the **`onboarding_walkthrough`** prompt.

---

## What to try next

- Change **`search_docs`** (more matches, different snippet size, or simple ranking).
- Add another **`@McpTool`** (for example: return word counts per doc).
- Read the **Model Context Protocol** specification and Spring AI MCP docs to compare transports (**stdio** vs **SSE** / HTTP).

Good luck — you now have a working MCP server wired to a real documentation folder.
