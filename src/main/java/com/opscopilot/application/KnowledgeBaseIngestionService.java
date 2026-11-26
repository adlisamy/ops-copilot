package com.opscopilot.application;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Prepares loaded knowledge-base documents for storage in the configured vector store.
 *
 * <p>Source loading is intentionally kept outside this service: callers can use any Spring AI
 * {@code DocumentReader}, attach source metadata to its documents, and pass those documents here.
 * The service splits them into smaller chunks before the {@link VectorStore} creates and stores
 * embeddings. The same embedding model is then used by retrieval when answering RAG requests.</p>
 *
 * <p>Useful metadata such as {@code source}, {@code runbook}, or {@code type} should be added
 * to each input document by the loader so it is available for filtering and traceability later.</p>
 *
 * @see VectorStore
 */
@Service
public class KnowledgeBaseIngestionService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;

    /**
     * Create the ingestion service using the application-wide vector store.
     *
     * @param vectorStore the storage abstraction used for document chunks
     */
    public KnowledgeBaseIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.textSplitter = TokenTextSplitter.builder().build();
    }

    /**
     * Split loaded source documents and store the resulting chunks.
     *
     * <p>An empty input is a no-op. Metadata on source documents is carried into their chunks
     * by Spring AI's transformer and can be used by retrieval filters.</p>
     *
     * @param documents source documents produced by a document reader
     */
    public void ingest(List<Document> documents) {
        Objects.requireNonNull(documents, "documents must not be null");
        if (documents.isEmpty()) {
            return;
        }

        List<Document> chunks = textSplitter.apply(documents);
        if (!chunks.isEmpty()) {
            vectorStore.add(chunks);
        }
    }
}