package com.example.campusfind.controller;

import com.example.campusfind.exception.ReportNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public String forbidden(AccessDeniedException exception, Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("message", "You do not have permission to view or change this conversation or report.");
        return "error/403";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String unavailable(IllegalStateException exception, Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("message", exception.getMessage());
        return "error/400";
    }

    @ExceptionHandler(ReportNotFoundException.class)
    public String notFound(ReportNotFoundException exception, Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("message", "We could not find that report. It may have been removed.");
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String unexpected(Exception exception, Model model, HttpServletResponse response) {
        log.error("Unexpected error while handling a CampusFind request", exception);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("message", "Something went wrong. Please try again in a moment.");
        return "error/500";
    }
}
