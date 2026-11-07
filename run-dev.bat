@echo off
REM CampusFind Runner - Dev Mode (In-Memory Database, Zero Setup)
echo ==========================================
echo    Starting CampusFind (Dev / Demo Mode)
echo    (Zero database setup required)
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

mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
