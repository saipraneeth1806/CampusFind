package com.example.campusfind.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller serving the interactive REST API Documentation and Live Test Explorer.
 */
@Controller
public class ApiDocsController {

    @GetMapping("/api/docs")
    public String apiDocs() {
        return "api/docs";
    }
}

