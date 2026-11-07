package com.example.campusfind.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactMessageForm {
    @NotBlank(message = "Write a message before sending.")
    @Size(max = 2000, message = "Message must be at most 2,000 characters.")
    private String body;

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
