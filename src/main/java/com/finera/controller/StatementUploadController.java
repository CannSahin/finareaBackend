package com.finera.controller;

import com.finera.dto.FileUploadResponseDto;
import com.finera.exception.ResourceNotFoundException;
import com.finera.service.PdfProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody; // Swagger için doğru import
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Giriş yapan kullanıcıyı almak için
import org.springframework.security.core.userdetails.UserDetails; // UserDetails için
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.finera.service.ai.AiProvider; // Enum'u import et
import org.springframework.web.bind.annotation.RequestParam;


import java.util.UUID;

@RestController
@RequestMapping("/api/v1/statements")
@RequiredArgsConstructor
@Tag(name = "Statement Processing", description = "API for uploading and processing PDF statements")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class StatementUploadController {

    private final PdfProcessingService pdfProcessingService;
    private final com.finera.repository.UserRepository userRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and process a PDF bank statement",
            description = "Uploads a PDF, extracts text, sends to AI for analysis using the specified provider, and saves transactions.")
    @ApiResponse(responseCode = "200", description = "Statement processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file, user not found, or unsupported AI provider")
    @ApiResponse(responseCode = "500", description = "Internal processing error")
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
    public ResponseEntity<FileUploadResponseDto> uploadStatement(
            @Parameter(description = "PDF statement file to upload", required = true)
            @RequestParam("statementFile") MultipartFile file,
            @Parameter(description = "Optional prefix for the source name (e.g., 'Isbank Kredi Karti')", required = false)
            @RequestParam(value = "sourcePrefix", required = false) String sourcePrefix,
            @Parameter(description = "AI provider to use for extraction (GEMINI, OPENAI, etc.)", required = true, example = "GEMINI")
            @RequestParam(value = "provider") AiProvider provider, // <-- AI Provider parametresi
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("Unauthorized attempt to upload statement.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (file.isEmpty() || !MediaType.APPLICATION_PDF.toString().equals(file.getContentType())) {
            log.warn("Invalid file upload attempt by user {}: incorrect type or empty file.", userDetails.getUsername());
            return ResponseEntity.badRequest().body(new FileUploadResponseDto("Invalid file. Please upload a PDF.", null, 0));
        }

        try {
            String userEmail = userDetails.getUsername();
            com.finera.entities.User currentUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> {
                        log.error("User not found for email {} during statement upload.", userEmail);
                        return new ResourceNotFoundException("User not found with email: " + userEmail);
                    });
            UUID userId = currentUser.getUserId();

            log.info("Processing statement upload for user ID: {} with provider: {}", userId, provider);
            // Provider parametresini service metoduna ilet
            FileUploadResponseDto response = pdfProcessingService.processPdfStatement(file, userId, sourcePrefix, provider);
            log.info("Statement processing completed for user ID: {}. Response: {}", userId, response);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.error("User not found during upload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new FileUploadResponseDto(e.getMessage(), null, 0));
        } catch (IllegalArgumentException e) { // Geçersiz provider hatasını yakala
            log.warn("Invalid AI provider requested: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new FileUploadResponseDto(e.getMessage(), null, 0));
        } catch (Exception e) {
            log.error("Error processing uploaded statement for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileUploadResponseDto("Error processing file: " + e.getMessage(), null, 0));
        }
    }
}