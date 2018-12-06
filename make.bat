@ECHO OFF

call mvn clean dependency:copy-dependencies package

java win64-ressources\windows-package.java
