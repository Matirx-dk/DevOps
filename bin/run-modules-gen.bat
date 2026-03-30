@echo off
echo.
echo [Ϣ] ʹJarModules-Gen̡
echo.

cd %~dp0
cd ../aidevops-modules/aidevops-gen/target

set JAVA_OPTS=-Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m

java -Dfile.encoding=utf-8 %JAVA_OPTS% -jar aidevops-modules-gen.jar

cd bin
pause