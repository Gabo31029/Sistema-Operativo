@echo off
echo Iniciando servidor Spring Boot...
cd /d "%~dp0"
call mvnw.cmd spring-boot:run
if errorlevel 1 (
    echo.
    echo Error: No se pudo ejecutar Maven Wrapper.
    echo Intentando con Maven global...
    mvn spring-boot:run
)

