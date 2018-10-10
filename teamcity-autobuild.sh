#!/bin/bash

#TeamCity Build $PROJECT script.

#GLOBAL RULES
#No matter which sub-directory has been altered, they are all inter-dependent for any and all good-loop/winterwell projects
#No matter which project, the build order must start with a new bob-all.jar creation
#Because the purpose of this script is to auto-publish, a new winterwell.webappbase.jar will have to be created, as it contains the methods for publishing.
#->winterwell.webappbase.jar needs freshly compiled elasticsearch-java-client.jar AND youagain-java-client.jar created first.



#DIRECTORY MAPS
OPEN_CODE='/home/winterwell/TeamCity/buildAgent/work/c7a16811424bee11'
ES_JAVA_CLIENT='/home/winterwell/TeamCity/buildAgent/work/ff7665b6f2ca318e'




#######################
### Step 01: Build a new bob-all.jar
#######################
printf "\nBuilding a new bob-all.jar\n"
cd winterwell.bob
java -jar bob-all.jar jobs.BuildBob
cd ../

########################
### Step 02: Build a new winterwell.webappbase.jar
########################
printf "\nBuilding a new winterwell.webappbase.jar\n"
printf "\n\tSub-Task: Building a new elasticsearch-java-client.jar\n"
cd $ES_JAVA_CLIENT
java -jar $OPEN_CODE/winterwell.bob/bob-all.jar

printf "\n\tSub-Task: Building a new youagain-java-client.jar\n"
cd $OPEN_CODE/youagain-java-client
java -jar $OPEN_CODE/winterwell.bob/bob-all.jar

printf "\nNOW Building a new winterwell.webappbase.jar\n"
cd $OPEN_CODE/winterwell.webappbase
java -cp $OPEN_CODE/junit-4.12.jar:$OPEN_CODE/winterwell.bob/bob-all.jar:$ES_JAVA_CLIENT/elasticsearch-java-client.jar:$OPEN_CODE/winterwell.webappbase/winterwell.webappbase.jar:$OPEN_CODE/youagain-java-client/youagain-java-client.jar org.junit.runner.JUnitCore com.winterwell.web.app.BuildWWAppBase



######NOW you need to build/handle the projects in a certain order, or maybe 'publish' them in a certain order.