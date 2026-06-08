package com.artivisi.accountingfinance.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Simplified Telegram Message DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramMessage {
    
    @JsonProperty("message_id")
    private Long messageId;
    
    @JsonProperty("from")
    private TelegramUser from;
    
    @JsonProperty("chat")
    private TelegramChat chat;
    
    @JsonProperty("date")
    private Long date;
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("photo")
    private List<TelegramPhotoSize> photo;
    
    @JsonProperty("document")
    private TelegramDocument document;

    /** Default constructor required for Jackson JSON deserialization. */
    public TelegramMessage() {
        // Empty constructor for Jackson
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public TelegramUser getFrom() {
        return from;
    }

    public void setFrom(TelegramUser from) {
        this.from = from;
    }

    public TelegramChat getChat() {
        return chat;
    }

    public void setChat(TelegramChat chat) {
        this.chat = chat;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<TelegramPhotoSize> getPhoto() {
        return photo;
    }

    public void setPhoto(List<TelegramPhotoSize> photo) {
        this.photo = photo;
    }

    public TelegramDocument getDocument() {
        return document;
    }

    public void setDocument(TelegramDocument document) {
        this.document = document;
    }

    public boolean hasText() {
        return text != null && !text.isEmpty();
    }

    public boolean hasPhoto() {
        return photo != null && !photo.isEmpty();
    }

    public boolean hasDocument() {
        return document != null;
    }
}
