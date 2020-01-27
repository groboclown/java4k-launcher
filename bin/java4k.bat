@echo off
if exist "%JAVA_HOME%" goto JAVAHOME
set JAVAEXE=javaw
goto TESTJAVA

:JAVAHOME
REM set JAVAEXE=%JAVA_HOME%\bin\javaw
set JAVAEXE=%JAVA_HOME%\bin\java


:TESTJAVA
%JAVAEXE% -version > NUL 2>&1 || (echo "Could not find javaw.exe" & goto END)

pushd %~dp0 > NUL 2>&1 || (echo "Could not find %~dp0" & goto END)
%JAVAEXE% -Dlauncher.dir=. -jar java4klauncher.jar %*
popd > NUL 2>&1


:END
