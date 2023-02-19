@echo off
set JAR=demo1-jar-with-dependencies.jar
java -Dkotlin.script.classpath=%JAR% -jar %JAR%
