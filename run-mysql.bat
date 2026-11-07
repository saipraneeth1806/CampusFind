@echo off
REM CampusFind Runner - MySQL Mode
echo ==========================================
echo    Starting CampusFind (MySQL Mode)
echo ==========================================

if exist .env (
    echo Loading settings from .env...
    for /f "usebackq tokens=1* delims==" %%A in (".env") do (
        set "key=%%A"
        set "val=%%B"
        if not "!key:~0,1!"=="#" (
            set "!key!=!val!"
        )
    )
)

mvn spring-boot:run
