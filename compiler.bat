@echo off
@title Package builder compiler
"C:/Program Files/Java/jdk1.8.0_91/bin/javac.exe" -cp libs/* -d bin src/com/cryo/iOSBuilder.java
pause