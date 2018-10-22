#!/bin/bash

#TeamCity Build $PROJECT script.

#######TODO : Read latest build log on TC and (probably) see that building the elasticsearch-java-client just fails outright.
########CURRENT THEORY TO TEST:
# I believe that bob is genuinely confused about the directory structure that it is being asked to build in.
# Therefore, to fully test this theory, I will rsync the Teamcity 'work/$PROJECT' directories, to /home/winterwell/$PROJECT so
# that bob can run in a more normal directory structure.   This essentially turns TeamCity into a "Git-Trigger -> Build Now" system.
# Which makes this 1 degree off of a traditional TeamCity Setup.

#GLOBAL RULES
#No matter which sub-directory has been altered, they are all inter-dependent for any and all good-loop/winterwell projects
#No matter which project, the build order must start with a new bob-all.jar creation
#Because the purpose of this script is to auto-publish, a new winterwell.webappbase.jar will have to be created, as it contains the methods for publishing.
#->winterwell.webappbase.jar needs freshly compiled elasticsearch-java-client.jar AND youagain-java-client.jar created first.




#DIRECTORY MAPS
OPEN_CODE='/home/winterwell/open-code'
ES_JAVA_CLIENT='/home/winterwell/elasticsearch-java-client'
WWAPPBASE_DIR='/home/winterwell/TeamCity/buildAgent/work/9307b27f248c307'


#########################
### Pull on Flexi-Gson
#########################
printf "\nPulling on Flexi-Gson repo\n"
cd /home/winterwell/TeamCity/buildAgent/work/80e533dc8a610115/ && \
git gc --prune=now && \
git pull origin master && \
git reset --hard FETCH_HEAD


############################
## Update the wwappbase.js repo
## because there might be a change
## to the publishing script
############################
printf "\nEnsuring that the teamcity wwappbase.js repo is up-to-date\n"
cd $WWAPPBASE_DIR && \
git gc --prune=now && \
git pull origin master && \
git reset --hard FETCH_HEAD



#RSYNC the projects on the disk to mimic a semi-traditional devbox directory structure.
## SUBTASK: Clean out the existing contents of the /home/winterwell/$PROJECT structure.
rm -rf /home/winterwell/elasticsearch-java-client/*
rm -rf /home/winterwell/flexi-gson/*
rm -rf /home/winterwell/open-code/*
rm -rf /home/winterwell/wwappbase.js/*
## NOW do the rsync
rsync -ravz /home/winterwell/TeamCity/buildAgent/work/ff7665b6f2ca318e/.[^.]* /home/winterwell/elasticsearch-java-client/
rsync -ravz /home/winterwell/TeamCity/buildAgent/work/ff7665b6f2ca318e/* /home/winterwell/elasticsearch-java-client/

rsync -ravz /home/winterwell/TeamCity/buildAgent/work/80e533dc8a610115/.[^.]* /home/winterwell/flexi-gson/
rsync -ravz /home/winterwell/TeamCity/buildAgent/work/80e533dc8a610115/* /home/winterwell/flexi-gson/

rsync -ravz /home/winterwell/TeamCity/buildAgent/work/c7a16811424bee11/.[^.]* /home/winterwell/open-code/
rsync -ravz /home/winterwell/TeamCity/buildAgent/work/c7a16811424bee11/* /home/winterwell/open-code/

rsync -ravz /home/winterwell/TeamCity/buildAgent/work/9307b27f248c307/.[^.]* /home/winterwell/wwappbase.js/
rsync -ravz /home/winterwell/TeamCity/buildAgent/work/9307b27f248c307/* /home/winterwell/wwappbase.js/




########################
### Step 00: Get existing bob-all.jar
########################
if [[ -f $OPEN_CODE/winterwell.bob/bob-all.jar ]]; then
    rm $OPEN_CODE/winterwell.bob/bob-all.jar
fi

wget -cO - 'https://www.winterwell.com/software/downloads/bob-all.jar' >> $OPEN_CODE/winterwell.bob/bob-all.jar


##########################
### QuarterStep: Build Flexi-Gson
##########################
printf "\nBuilding flexi-gson.jar\n"
cd /home/winterwell/flexi-gson
java -jar $OPEN_CODE/winterwell.bob/bob-all.jar jobs.BuildFlexiGson



#######################
### Step 01: Build a new bob-all.jar
#######################
printf "\nBuilding a new bob-all.jar\n"
cd $OPEN_CODE/winterwell.bob
java -jar bob-all.jar jobs.BuildBob


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
sed -i -e 's/typeOfPublish = KPubType.test;/typeOfPublish = KPubType.local;/g' builder/com/winterwell/datalog/PublishDataServer.java
sed -i -e 's/typeOfPublish = KPubType.production;/typeOfPublish = KPubType.local;/g' builder/com/winterwell/datalog/PublishDataServer.java


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