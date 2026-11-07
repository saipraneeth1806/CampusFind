# CampusFind Runner - Dev Mode (In-Memory Database, Zero Setup)
# Uses H2 database in MySQL compatibility mode with pre-seeded data

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "   Starting CampusFind (Dev / Demo Mode)  " -ForegroundColor Cyan
Write-Host "   (Zero database setup required)         " -ForegroundColor Cyan
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
}

Write-Host "Launching Spring Boot with 'dev' profile..." -ForegroundColor Green
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
