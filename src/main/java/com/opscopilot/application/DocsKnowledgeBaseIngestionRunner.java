package com.opscopilot.application;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Runs an explicitly requested local Markdown ingestion when the dev application starts. */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.knowledge-base", name = "ingest-docs-on-startup", havingValue = "true")
public class DocsKnowledgeBaseIngestionRunner implements ApplicationRunner {

    private final KnowledgeBaseMarkdownLoader markdownLoader;
    private final KnowledgeBaseIngestionService ingestionService;
    private final Path docsDirectory;

    public DocsKnowledgeBaseIngestionRunner(KnowledgeBaseMarkdownLoader markdownLoader,
                                           KnowledgeBaseIngestionService ingestionService,
                                           @Value("${app.knowledge-base.docs-path:docs}") String docsPath) {
        this.markdownLoader = markdownLoader;
        this.ingestionService = ingestionService;
        this.docsDirectory = Path.of(docsPath);
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        List<Document> documents = markdownLoader.load(docsDirectory);
        if (documents.isEmpty()) {
            throw new IllegalStateException("No Markdown documents found under " + docsDirectory);
        }
        ingestionService.ingest(documents);
    }
}