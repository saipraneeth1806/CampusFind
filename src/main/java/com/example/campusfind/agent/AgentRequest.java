package com.example.campusfind.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AgentRequest {
    @NotBlank(message = "Enter what you are looking for.")
    @Size(max = 400, message = "Keep your question under 400 characters.")
    private String question;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}
