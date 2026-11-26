package com.opscopilot.application;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class KnowledgeBaseIngestionServiceTest {

    @Test
    void shouldSplitAndStoreLoadedDocuments() {
        VectorStore vectorStore = mock(VectorStore.class);
        KnowledgeBaseIngestionService ingestionService = new KnowledgeBaseIngestionService(vectorStore);

        String sourceText = "Restart the service after updating its configuration. ".repeat(1_000);
        ingestionService.ingest(List.of(new Document(sourceText)));

        verify(vectorStore).add(org.mockito.ArgumentMatchers.argThat(chunks -> chunks.size() > 1));
    }

    @Test
    void shouldNotWriteWhenThereAreNoDocuments() {
        VectorStore vectorStore = mock(VectorStore.class);
        KnowledgeBaseIngestionService ingestionService = new KnowledgeBaseIngestionService(vectorStore);

        ingestionService.ingest(List.of());

        verify(vectorStore, never()).add(anyList());
    }
}