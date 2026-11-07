package com.example.campusfind.exception;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(Long id) {
        super("Report not found: " + id);
    }
}
