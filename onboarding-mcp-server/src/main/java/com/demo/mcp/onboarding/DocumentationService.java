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

    private boolean isInsideDocsRoot(Path candidate) {
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        Path normalizedRoot = docsRoot.toAbsolutePath().normalize();
        return normalizedCandidate.startsWith(normalizedRoot);
    }

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
