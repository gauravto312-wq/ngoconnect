package com.ngoconnect.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service managing physical upload storage location, ensuring consistent,
 * CWD-independent path resolution and persistence across application lifecycles.
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDirConfig;

    private Path resolvedUploadPath;
    private Path resolvedPrivateDocumentsPath;

    @PostConstruct
    public void init() {
        Path configured = Paths.get(uploadDirConfig);
        if (configured.isAbsolute()) {
            resolvedUploadPath = configured.normalize();
        } else {
            // When started from workspace root, backend/uploads exists
            Path rootBackendUploads = Paths.get("backend", uploadDirConfig).toAbsolutePath().normalize();
            // When started from backend directory, uploads exists
            Path localUploads = Paths.get(uploadDirConfig).toAbsolutePath().normalize();

            if (Files.exists(rootBackendUploads) && Files.isDirectory(rootBackendUploads)) {
                resolvedUploadPath = rootBackendUploads;
            } else if (Files.exists(localUploads) && Files.isDirectory(localUploads)) {
                resolvedUploadPath = localUploads;
            } else {
                if (Files.exists(Paths.get("backend"))) {
                    resolvedUploadPath = rootBackendUploads;
                } else {
                    resolvedUploadPath = localUploads;
                }
            }
        }

        try {
            if (!Files.exists(resolvedUploadPath)) {
                Files.createDirectories(resolvedUploadPath);
            }
            log.info("Physical file storage initialized at: {}", resolvedUploadPath);

            // Private documents directory: isolated outside the public static uploads directory
            resolvedPrivateDocumentsPath = resolvedUploadPath.resolveSibling("private_documents").toAbsolutePath().normalize();
            if (!Files.exists(resolvedPrivateDocumentsPath)) {
                Files.createDirectories(resolvedPrivateDocumentsPath);
            }
            log.info("Private documents storage initialized at: {}", resolvedPrivateDocumentsPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create storage directory: " + resolvedUploadPath, e);
        }
    }

    public Path getUploadDirectory() {
        if (resolvedUploadPath == null) {
            init();
        }
        return resolvedUploadPath;
    }

    public Path getPrivateDocumentsDirectory() {
        if (resolvedPrivateDocumentsPath == null) {
            init();
        }
        return resolvedPrivateDocumentsPath;
    }
}
