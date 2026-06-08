package com.artivisi.accountingfinance.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramPhotoSize {
    
    @JsonProperty("file_id")
    private String fileId;
    
    @JsonProperty("file_unique_id")
    private String fileUniqueId;
    
    @JsonProperty("width")
    private Integer width;
    
    @JsonProperty("height")
    private Integer height;
    
    @JsonProperty("file_size")
    private Long fileSize;

    /** Default constructor required for Jackson JSON deserialization. */
    public TelegramPhotoSize() {
        // Empty constructor for Jackson
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileUniqueId() {
        return fileUniqueId;
    }

    public void setFileUniqueId(String fileUniqueId) {
        this.fileUniqueId = fileUniqueId;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
}
