package com.opscopilot.application;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Loads Markdown knowledge-base documents from a local directory. */
@Component
public class KnowledgeBaseMarkdownLoader {

    /**
     * Load all Markdown files beneath the supplied directory in a stable path order.
     *
     * @param directory the root directory to scan recursively
     * @return the file contents as Spring AI documents with relative {@code source} metadata
     * @throws IOException if the directory cannot be read
     */
    public List<Document> load(Path directory) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IOException("Knowledge-base directory does not exist: " + directory);
        }

        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .sorted()
                    .map(path -> toDocument(root, path))
                    .toList();
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    private static Document toDocument(Path root, Path path) {
        try {
            String source = root.relativize(path).toString().replace('\\', '/');
            return new Document(Files.readString(path, StandardCharsets.UTF_8), Map.of("source", source));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}