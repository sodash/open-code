#!/bin/bash

#######
# Update open-code jars
#######

BOB_BUILD="java -jar ../winterwell.bob/bob-all.jar"

###
#01. Get latest built 'bob-all.jar' JAR package.
###
wget -cO - https://www.winterwell.com/software/downloads/bob-all.jar >> winterwell.bob/bob-all.jar

###
#02. Get latest 'open-code' updates from github
###
git pull

###
#03. Build project JAR files based on this latest 'open-code'
###
#Datalog
cd winterwell.datalog
$BOB_BUILD jobs.BuildDataLog
cd ../
#Depot
cd winterwell.depot
$BOB_BUILD jobs.BuildDepot
cd ../
#Maths
cd winterwell.maths
$BOB_BUILD jobs.BuildMaths
cd ../
#NLP
cd winterwell.nlp
$BOB_BUILD jobs.BuildNLP
cd ../
#Optimization
cd winterwell.optimization
$BOB_BUILD jobs.BuildOptimization
cd ../
#Utils
cd winterwell.utils
$BOB_BUILD jobs.BuildUtils
cd ../
#Web
cd winterwell.web
$BOB_BUILD jobs.BuildWeb
cd ../
#Webappbase
cd winterwell.webappbase
$BOB_BUILD jobs.BuildWebappbase
cd ../