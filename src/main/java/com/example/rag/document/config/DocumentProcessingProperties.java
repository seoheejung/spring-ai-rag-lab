package com.example.rag.document.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.document")
public record DocumentProcessingProperties(
        boolean enabled,
        Path sourcePdf
) {
}