@echo off
REM ============================================================
REM  compilar.bat — Genera informe_unidad4.pdf con LaTeX local
REM  Requiere: MiKTeX o TeX Live instalado en PATH
REM  Ejecutar desde la carpeta docs\
REM ============================================================
setlocal

set DOC=informe_unidad4

echo [1/4] Primera pasada pdflatex...
pdflatex -interaction=nonstopmode %DOC%.tex
if errorlevel 1 goto error

echo [2/4] Biber (referencias APA)...
biber %DOC%
if errorlevel 1 goto error

echo [3/4] Segunda pasada pdflatex...
pdflatex -interaction=nonstopmode %DOC%.tex
if errorlevel 1 goto error

echo [4/4] Tercera pasada pdflatex (numeracion y cruces finales)...
pdflatex -interaction=nonstopmode %DOC%.tex
if errorlevel 1 goto error

echo.
echo PDF generado: %DOC%.pdf
goto fin

:error
echo.
echo ERROR en la compilacion. Revisa %DOC%.log
exit /b 1

:fin
endlocal
