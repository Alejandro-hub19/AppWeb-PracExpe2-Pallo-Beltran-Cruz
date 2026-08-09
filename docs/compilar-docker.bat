@echo off
REM ============================================================
REM  compilar-docker.bat — Genera informe_unidad4.pdf via Docker
REM  Requiere: Docker Desktop en ejecucion
REM  Ejecutar desde la RAIZ del repositorio
REM ============================================================
setlocal

set DOC=informe_unidad4
set DOCS_PATH=%~dp0

echo Compilando %DOC%.pdf con texlive/texlive via Docker...
echo (La primera vez descarga la imagen, puede tardar varios minutos)
echo.

docker run --rm ^
  -v "%DOCS_PATH%:/workdir" ^
  -w /workdir ^
  texlive/texlive:latest ^
  sh -c "pdflatex -interaction=nonstopmode %DOC%.tex && biber %DOC% && pdflatex -interaction=nonstopmode %DOC%.tex && pdflatex -interaction=nonstopmode %DOC%.tex"

if errorlevel 1 (
  echo.
  echo ERROR. Revisa la salida anterior o el archivo %DOC%.log dentro de docs\
  exit /b 1
)

echo.
echo PDF generado en docs\%DOC%.pdf
endlocal
