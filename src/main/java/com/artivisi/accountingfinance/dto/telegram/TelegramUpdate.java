package com.artivisi.accountingfinance.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simplified Telegram Update DTO for webhook reception.
 * Only includes fields we actually use.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramUpdate {
    
    @JsonProperty("update_id")
    private Long updateId;
    
    @JsonProperty("message")
    private TelegramMessage message;

    /** Default constructor required for Jackson JSON deserialization. */
    public TelegramUpdate() {
        // Empty constructor for Jackson
    }

    public Long getUpdateId() {
        return updateId;
    }

    public void setUpdateId(Long updateId) {
        this.updateId = updateId;
    }

    public TelegramMessage getMessage() {
        return message;
    }

    public void setMessage(TelegramMessage message) {
        this.message = message;
    }
}
