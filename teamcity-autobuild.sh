#!/bin/bash

#TeamCity Build $PROJECT script.

#RULES
#No matter which sub-directory has been altered, they are all inter-dependent for any and all good-loop/winterwell projects
#No matter which project, the build order must start with a new bob-all.jar creation
#Because the purpose of this script is to auto-publish, a new winterwell.webappbase.jar will have to be created, as it contains the methods for publishing.

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
cd winterwell.web