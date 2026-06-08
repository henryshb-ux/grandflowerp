package com.artivisi.accountingfinance.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramChat {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("username")
    private String username;

    /** Default constructor required for Jackson JSON deserialization. */
    public TelegramChat() {
        // Empty constructor for Jackson
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
