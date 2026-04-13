package com.uhn.pmb.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class FileController {

    private static final String UPLOADS_DIR = "uploads/admission-forms";

    /**
     * Serve admission form files (photos, certificates, etc.)
     */
    @GetMapping("/admission-forms/{studentId}/{fileName}")
    public ResponseEntity<Resource> getAdmissionFile(
            @PathVariable Long studentId,
            @PathVariable String fileName) {
        try {
            // Build safe file path
            Path filePath = Paths.get(UPLOADS_DIR, String.valueOf(studentId), fileName);
            File file = filePath.toFile();

            // Validate file exists and is within the uploads directory
            if (!file.exists() || !file.isFile()) {
                log.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // Prevent path traversal attacks
            String realPath = file.getCanonicalPath();
            String uploadsPath = new File(UPLOADS_DIR).getCanonicalPath();
            if (!realPath.startsWith(uploadsPath)) {
                log.warn("Attempted path traversal: {}", filePath);
                return ResponseEntity.status(403).build();
            }

            // Determine media type
            MediaType mediaType = determineMediaType(fileName);

            // Return file
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error serving file: {} for student: {}", fileName, studentId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Download admission form files
     */
    @GetMapping("/download/admission-forms/{studentId}/{fileName}")
    public ResponseEntity<Resource> downloadAdmissionFile(
            @PathVariable Long studentId,
            @PathVariable String fileName) {
        try {
            Path filePath = Paths.get(UPLOADS_DIR, String.valueOf(studentId), fileName);
            File file = filePath.toFile();

            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            // Prevent path traversal
            String realPath = file.getCanonicalPath();
            String uploadsPath = new File(UPLOADS_DIR).getCanonicalPath();
            if (!realPath.startsWith(uploadsPath)) {
                return ResponseEntity.status(403).build();
            }

            MediaType mediaType = determineMediaType(fileName);
            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading file: {} for student: {}", fileName, studentId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Determine media type based on file extension
     */
    private MediaType determineMediaType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        
        switch (extension) {
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            case "gif":
                return MediaType.IMAGE_GIF;
            case "pdf":
                return MediaType.APPLICATION_PDF;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
