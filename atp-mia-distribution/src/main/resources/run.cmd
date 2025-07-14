@echo off
setlocal enabledelayedexpansion

::set default values
if not defined MAX_RAM set MAX_RAM=1024m
if not defined GRAYLOG_ON set GRAYLOG_ON=false
if not defined GRAYLOG_LEVEL set GRAYLOG_LEVEL=INFO
if not defined JDBC_URL set JDBC_URL=jdbc:postgresql://localhost:5432/mia
if not defined SPRING_CONFIG_LOCATION set SPRING_CONFIG_LOCATION=./config/application.properties

::check mandatory parameters
if not defined JDBC_URL echo Must provide JDBC_URL && exit /b

::build run options
set JAVA_OPTIONS=!JAVA_OPTIONS! -XX:+PrintFlagsFinal
set JAVA_OPTIONS=!JAVA_OPTIONS! -Dlog.graylog.on=!GRAYLOG_ON!
set JAVA_OPTIONS=!JAVA_OPTIONS! -Dspring.cloud.bootstrap.location=./config/bootstrap.properties!
set JAVA_OPTIONS=!JAVA_OPTIONS! -Dnashorn.args="-strict --no-java --no-syntax-extensions"
set JAVA_OPTIONS=!JAVA_OPTIONS! -Dlog.graylog.host=!GRAYLOG_HOST!
set JAVA_OPTIONS=!JAVA_OPTIONS! -Dlog.graylog.port=!GRAYLOG_PORT!
set JAVA_OPTIONS=!JAVA_OPTIONS! -Dlog.graylog.level=!GRAYLOG_LEVEL!
set JAVA_OPTIONS=!JAVA_OPTIONS! -Dspring.datasource.url=!JDBC_URL!
set JAVA_OPTIONS=!JAVA_OPTIONS! -Dspring.config.location=!SPRING_CONFIG_LOCATION!

java --add-opens java.base/java.lang=ALL-UNNAMED -XX:MaxRAM=!MAX_RAM! !JAVA_OPTIONS! -cp "config/;lib/*" org.qubership.atp.mia.Main
