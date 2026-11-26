package com.opscopilot.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeBaseMarkdownLoaderTest {

    private final KnowledgeBaseMarkdownLoader markdownLoader = new KnowledgeBaseMarkdownLoader();

    @Test
    void shouldLoadMarkdownRecursivelyWithRelativeSourceMetadata(@TempDir Path directory) throws IOException {
        Path runbook = Files.createDirectories(directory.resolve("runbooks")).resolve("restart.md");
        Files.writeString(runbook, "Restart guidance");
        Files.writeString(directory.resolve("ignore.txt"), "not a Markdown document");

        List<Document> documents = markdownLoader.load(directory);

        assertEquals(1, documents.size());
        assertEquals("Restart guidance", documents.getFirst().getText());
        assertEquals("runbooks/restart.md", documents.getFirst().getMetadata().get("source"));
    }

    @Test
    void shouldFailWhenDirectoryDoesNotExist(@TempDir Path directory) {
        assertThrows(IOException.class, () -> markdownLoader.load(directory.resolve("missing")));
    }

    @Test
    void shouldLoadProjectRunbookForRetrieval() throws IOException {
        List<Document> documents = markdownLoader.load(Path.of("docs"));

        assertTrue(documents.stream().anyMatch(document ->
                document.getMetadata().get("source").equals("runbooks/kubernetes-crashloopbackoff.md")));
    }
}