package com.example.campusfind.controller;

import com.example.campusfind.agent.AgentRequest;
import com.example.campusfind.agent.CampusFindAgent;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AssistantController {
    private final CampusFindAgent agent;

    public AssistantController(CampusFindAgent agent) { this.agent = agent; }

    @GetMapping("/assistant")
    public String assistant(Model model) {
        model.addAttribute("agentRequest", new AgentRequest());
        return "assistant/index";
    }

    @PostMapping("/assistant")
    public String ask(@Valid @ModelAttribute("agentRequest") AgentRequest request,
                      BindingResult errors, Model model) {
        if (errors.hasErrors()) return "assistant/index";
        model.addAttribute("response", agent.ask(request.getQuestion()));
        return "assistant/index";
    }
}
