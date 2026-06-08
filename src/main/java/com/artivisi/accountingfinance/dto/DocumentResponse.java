package com.artivisi.accountingfinance.dto;

import com.artivisi.accountingfinance.entity.Document;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String originalFilename,
        String contentType,
        Long fileSize,
        String fileSizeFormatted,
        String checksumSha256,
        LocalDateTime uploadedAt,
        String uploadedBy
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getFileSize(),
                document.getFileSizeFormatted(),
                document.getChecksumSha256(),
                document.getUploadedAt(),
                document.getUploadedBy()
        );
    }
}
