package com.opscopilot.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RAG_INTEGRATION_TESTS", matches = "true")
class KnowledgeBaseRetrievalIntegrationTest {

    @Autowired
    private KnowledgeBaseMarkdownLoader markdownLoader;

    @Autowired
    private KnowledgeBaseIngestionService ingestionService;

    @Autowired
    private VectorStore vectorStore;

    @Test
    void shouldRetrieveRelevantChunksFromProjectDocs() throws IOException {
        List<Document> documents = markdownLoader.load(Path.of("docs/runbooks"));
        assertFalse(documents.isEmpty(), "Expected Markdown documents in docs/runbooks/");

        ingestionService.ingest(documents);

        List<Document> matches = vectorStore.similaritySearch(SearchRequest.builder()
                .query("What does Kubernetes CrashLoopBackOff mean, and what safe checks should I run?")
                .topK(10)
                .build());

        assertTrue(matches.stream().anyMatch(document ->
                        document.getText().contains("CrashLoopBackOff")
                                && "kubernetes-crashloopbackoff.md"
                                .equals(document.getMetadata().get("source"))),
                "Expected pgvector retrieval to return a chunk from the CrashLoopBackOff runbook");
    }
}