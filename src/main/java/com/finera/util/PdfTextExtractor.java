package com.finera.util;

import lombok.extern.slf4j.Slf4j; // Loglama için
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

@Component
@Slf4j // Loglama ekleyelim
public class PdfTextExtractor {

    // Kontrol karakterlerini temizlemek için Regex Pattern'i
    private static final Pattern CONTROL_CHARS_PATTERN = Pattern.compile("[\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]");

    public String extractText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is null or empty");
        }

        byte[] pdfBytes;
        try (InputStream inputStream = file.getInputStream()) {
            pdfBytes = inputStream.readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read input stream from file: {}", file.getOriginalFilename(), e);
            throw new IOException("Failed to read input stream from file: " + e.getMessage(), e);
        }

        String rawText;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                // İsteğe bağlı: Tablo yapısını korumak için deneyebilirsiniz
                // stripper.setSortByPosition(true);
                rawText = stripper.getText(document);
            } else {
                throw new IOException("Cannot process encrypted PDF without password.");
            }
        } catch (Exception e) {
            log.error("Failed to parse PDF content for file: {}", file.getOriginalFilename(), e);
            throw new IOException("Failed to parse PDF content: " + e.getMessage(), e);
        }

        // Metin çıkarma sonrası TEMİZLEME işlemini burada yap
        String cleanedText = cleanRawText(rawText);
        log.info("Extracted and cleaned text from {}. Original length: {}, Cleaned length: {}",
                file.getOriginalFilename(), rawText.length(), cleanedText.length());
        return cleanedText;
    }

    /**
     * PDF'ten çıkarılan ham metindeki olası sorunlu karakterleri temizler.
     */
    private String cleanRawText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        log.debug("Cleaning raw text (first 100 chars): {}", text.substring(0, Math.min(text.length(), 100)).replace('\n',' '));

        // 1. Görünmeyen kontrol ve format karakterlerini kaldır
        String cleaned = CONTROL_CHARS_PATTERN.matcher(text).replaceAll(" "); // Boşlukla değiştir ki kelimeler birleşmesin

        // 2. Birden fazla boşluğu tek boşluğa indirge ve satır başı/sonu boşlukları temizle
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        // 3. JSON yapısını bozabilecek karakterleri değiştirme (Yorumda bırakıldı - Riskli)
        // cleaned = cleaned.replace("{", "[_ocb_]");
        // cleaned = cleaned.replace("}", "[_ccb_]");
        // cleaned = cleaned.replace("`", "'");

        log.debug("Cleaned text (first 100 chars): {}", cleaned.substring(0, Math.min(cleaned.length(), 100)));
        return cleaned;
    }
}