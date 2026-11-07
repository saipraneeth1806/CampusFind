# CampusFind Runner - MySQL Mode
# Loads .env variables and launches CampusFind connected to MySQL

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "   Starting CampusFind (MySQL Mode)       " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

if (Test-Path .env) {
    Write-Host "Loading configuration from .env..." -ForegroundColor Green
    Get-Content .env | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $varName = $parts[0].Trim()
            $varVal = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($varName, $varVal, "Process")
        }
    }
} else {
    Write-Host "No .env found. Using default environment settings." -ForegroundColor Yellow
}

Write-Host "Launching Spring Boot with MySQL..." -ForegroundColor Green
mvn spring-boot:run
