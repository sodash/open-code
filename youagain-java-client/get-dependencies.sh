#!/bin/bash

#youagain-java-client dependency JAR grabber

SHORTHAND_WGET="wget -cO - $1"

printf "\nGetting Commons Codec...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/commons-codec/commons-codec/1.11/commons-codec-1.11.jar >> dependencies/commons-codec.jar

printf "\nGetting Commons Codec Sources...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/commons-codec/commons-codec/1.11/commons-codec-1.11-sources.jar >> dependencies/commons-codec-sources.jar

printf "\nGetting Jackson Annotations...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.9.0/jackson-annotations-2.9.0.jar >> dependencies/jackson-annotations.jar

printf "\nGetting Jackson Annotations Sources...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.9.0/jackson-annotations-2.9.0-sources.jar >> dependencies/jackson-annotations-sources.jar

printf "\nGetting Jackson Core...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.9.6/jackson-core-2.9.6.jar >> dependencies/jackson-core.jar

printf "\nGetting Jackson Core Sources...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.9.6/jackson-core-2.9.6-sources.jar >> dependencies/jackson-core-sources.jar

printf "\nGetting Jackson Databind...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.9.6/jackson-databind-2.9.6.jar >> dependencies/jackson-databind.jar

printf "\nGetting Jackson Databind Sources...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.9.6/jackson-databind-2.9.6-sources.jar >> dependencies/jackson-databind-sources.jar

printf "\nGetting Java JWT...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/com/auth0/java-jwt/3.4.0/java-jwt-3.4.0.jar >> dependencies/java-jwt.jar

printf "\nGetting Java JWT Sources...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/com/auth0/java-jwt/3.4.0/java-jwt-3.4.0-sources.jar >> dependencies/java-jwt-sources.jar

printf "\nGetting jose4j...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/org/bitbucket/b_c/jose4j/0.5.2/jose4j-0.5.2.jar >> dependencies/jose4j.jar

printf "\nGetting jose4j Sources...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/org/bitbucket/b_c/jose4j/0.5.2/jose4j-0.5.2-sources.jar >> dependencies/jose4j-sources.jar

printf "\nGetting slf4j API...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/org/slf4j/slf4j-api/1.7.21/slf4j-api-1.7.21.jar >> dependencies/slf4j-api.jar

printf "\nGetting slf4j API Sources...\n"
$SHORTHAND_WGET http://central.maven.org/maven2/org/slf4j/slf4j-api/1.7.21/slf4j-api-1.7.21-sources.jar >> dependencies/slf4j-api-sources.jar

printf "\nAll JAR dependencies have been gotten\n"