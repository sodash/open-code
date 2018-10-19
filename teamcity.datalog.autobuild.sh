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
WWAPPBASE_DIR='/home/winterwell/TeamCity/buildAgent/work/9307b27f248c307'




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


###########################
## Step 03: Alter the PublishDataServer.java file 
## so that it wants to publish to the 'localhost' target
###########################
printf "\nSetting the 'KPubType' to a 'local' string, so that this is a localhost publish of the jars\n"
cd $OPEN_CODE/winterwell.datalog
sed -i -e 's/typeOfPublish = KPubType.test;/typeOfPublish = KPubType.local;/g' winterwell.datalog/builder/com/winterwell/datalog/PublishDataServer.java
sed -i -e 's/typeOfPublish = KPubType.production;/typeOfPublish = KPubType.local;/g' winterwell.datalog/builder/com/winterwell/datalog/PublishDataServer.java

############################
## Step 04: Update the wwappbase.js repo
## because there might be a change
## to the publishing script
############################
printf "\nEnsuring that the teamcity wwappbase.js repo is up-to-date\n"
cd $WWAPPBASE_DIR
git gc --prune=now
git pull origin master
git reset --hard FETCH_HEAD

##############################
## Step 05: Have Bob render a datalog.jar
##############################
printf "\nRendering a winterwell.datalog.jar file\n"
cd $OPEN_CODE/winterwell.datalog
java -jar ../winterwell.bob/bob-all.jar jobs.BuildDataLog

###############################
## Step 06: Tell Bob that it's time to publish datalog
###############################
printf "\nHaving Bob run the PublishDataServer task as a JUnit test\n"
cd $OPEN_CODE
java -cp junit-4.12.jar:winterwell.bob/bob-all.jar:winterwell.webappbase/winterwell.webappbase.jar:winterwell.datalog/winterwell.datalog.jar org.junit.runner.JUnitCore com.winterwell.datalog.PublishDataServer

################################
## Step 07: Run the project-publisher.sh script in order to publish
## all of these new jars to the test server
################################
printf "\nUsing the 'project-publisher.sh' script to publish datalog to the test server\n"
cd $WWAPPBASE_DIR
bash project-publisher.sh datalog test