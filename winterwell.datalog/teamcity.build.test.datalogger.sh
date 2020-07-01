#!/bin/bash

# TeamCity Continuous Integration Builder Template

# Versions of this script are usually run by TeamCity, in response to a git commit.
# The script uses ssh remote commands to target a server -- it does not affect the local machine.
# For testing, the script can also be run from your local computer.
#Version 1.2.3
# Latest Change -- removed a piped command which prevented any and all webpacking errors to be detected

#####  GENERAL SETTINGS
## This section should be the most widely edited part of this script
## Set the Variables according to your project's name, directory path, git-checkout branches, etc.
## Set the preferences according to your project's needs
#####
PROJECT_NAME='open-code' #This name will be used to create/or/refer-to the directory of the project in /home/winterwell/
GIT_REPO_URL='github.com:/sodash/open-code' #
PROJECT_USES_BOB='yes'  #yes or no :: If 'yes', then you must also supply the name of the service which is used to start,stop,or restart the jvm
NAME_OF_SERVICE='lg' # This can be blank, but if your service uses a JVM, then you must put in the service name which is used to start,stop,or restart the JVM on the server.
PROJECT_USES_NPM='no' # yes or no
PROJECT_USES_WEBPACK='no' #yes or no
PROJECT_USES_JERBIL='no' #yes or no
PROJECT_USES_WWAPPBASE_SYMLINK='no'

# Where is the test server?
TARGET_SERVERS=(baker.good-loop.com)


#####  SPECIFIC SETTINGS
## This section should only be selectively edited - based on non-standardized needs
#####
PROJECT_ROOT_ON_SERVER="/home/winterwell/$PROJECT_NAME/winterwell.datalog"
WWAPPBASE_REPO_PATH_ON_SERVER_DISK="/home/winterwell/wwappbase.js"


##### UNDENIABLY ESOTERIC SETTINGS
## This is the space where your project's settings make it completely non-standard
#####
EMAIL_RECIPIENTS=(sysadmin@good-loop.com daniel@good-loop.com roscoe@good-loop.com)
BOB_ARGS='' #you can set bob arguments here, but they will run each and every time that the project is auto-built
BOB_BUILD_PROJECT_NAME='' #If the project name isn't automatically sensed by bob, you can set it explicitly here
NPM_CLEANOUT='no' #yes/no , will nuke the node_modules directory if 'yes', and then get brand-new packages.
NPM_I_LOGFILE="/home/winterwell/.npm/_logs/npm.i.for.$PROJECT_NAME.log"
NPM_RUN_COMPILE_LOGFILE="/home/winterwell/.npm/_logs/npm.run.compile.for.$PROJECT_NAME.log"

##### FUNCTIONS
## Do not edit these unless you know what you are doing
#####
ATTACHMENTS=()
function send_alert_email {
    for email in ${EMAIL_RECIPIENTS[@]}; do
        TIME=$(date +%Y-%m-%dT%H:%M:%S-%Z)
	    message="AutoBuild Detected a Failure Building $PROJECT_NAME during $BUILD_PROCESS_NAME"
	    body="Hi,\nThe AutoPublisher detected a failure when $BUILD_STEP"
	    title="TeamCity $message"
	    printf "$body" | mutt -s "$title" ${ATTACHMENTS[@]} -- $email
    done
}

# First-Run-check for repository : Check if repo exists on the server('s) disk(s)
function check_repo_exists {
    for server in ${TARGET_SERVERS[@]}; do
        ssh winterwell@$server "if [[ ! -d $PROJECT_ROOT_ON_SERVER ]]; then printf '\ncloning the repo...\n'; git clone git@$GIT_REPO_URL; fi"
    done
}

# First-Run-check for bob : Check if any of the target servers already have npm's bob
function check_bob_exists {
    BUILD_PROCESS_NAME='checking for bob'
    BUILD_STEP='checking for a global installation of "bob"'
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        for server in ${TARGET_SERVERS[@]}; do
            if [[ $(ssh winterwell@$server "which bob") = '' ]]; then
                printf "\nNo global installation of 'bob' was found. Sending Alert Emails and Breaking Operation\n"
                send_alert_email
                exit 0
            fi
        done
    fi
}

# First-Run-check for Jerbil : Check if any of the target servers already have npm's jerbil
function check_jerbil_exists {
    BUILD_PROCESS_NAME='checking for jerbil'
    BUILD_STEP='checking for a global installation of "jerbil"'
    if [[ $PROJECT_USES_JERBIL = 'yes' ]]; then
        for server in ${TARGET_SERVERS[@]}; do
            if [[ $(ssh winterwell@$server "which jerbil") = '' ]]; then
                printf "\nNo global installation of 'jerbil' was found. Sending Alert Emails and Breaking Operation\n"
                send_alert_email
                exit 0
            fi
        done
    fi
}

function check_wwappbasejs_exists {
    BUILD_PROCESS_NAME='checking for wwappbase.js'
    BUILD_STEP='checking the path for the wwappbase.js repository on the servers disk'
    if [[ $PROJECT_USES_WWAPPBASE_SYMLINK = 'yes' ]]; then
        for server in ${TARGET_SERVERS[@]}; do
            if [[ $(ssh winterwell@$server "ls $WWAPPBASE_REPO_PATH_ON_SERVER_DISK") = "ls: cannot access '$WWAPPBASE_REPO_PATH_ON_SERVER_DISK': No such file or directory" ]]; then
                printf "\nThe Defined Path to wwappbase.js couldn't be validated. Sending Alert Emails and Breaking Operation\n"
                send_alert_email
                exit 0
            fi
        done
    fi
}


# Cleanup Git -- Ensure a clean and predictable git repo for building
function cleanup_repo {
    for server in ${TARGET_SERVERS[@]}; do
        printf "\nCleaning $server 's local repository...\n"
        ssh winterwell@$server "cd $PROJECT_ROOT_ON_SERVER && git gc --prune=now"
        ssh winterwell@$server "cd $PROJECT_ROOT_ON_SERVER && git pull origin master"
        ssh winterwell@$server "cd $PROJECT_ROOT_ON_SERVER && git reset --hard FETCH_HEAD"
    done
}

# Cleanup wwappbase.js 's repo -- Ensure that this repository is up to date and clean
function cleanup_wwappbasejs_repo {
    if [[ $PROJECT_USES_WWAPPBASE_SYMLINK = 'yes' ]]; then
        for server in ${TARGET_SERVERS[@]}; do
            printf "\nCleaning $server 's local wwappbase.js repository\n"
            ssh winterwell@$server "cd $WWAPPBASE_REPO_PATH_ON_SERVER_DISK && git gc --prune=now"
            ssh winterwell@$server "cd $WWAPPBASE_REPO_PATH_ON_SERVER_DISK && git pull origin master"
            ssh winterwell@$server "cd $WWAPPBASE_REPO_PATH_ON_SERVER_DISK && git reset --hard FETCH_HEAD"
        done
    fi
}

# Stopping the JVM Backend (if applicable)
function stop_service {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        for server in ${TARGET_SERVERS[@]}; do
            printf "\nStopping $NAME_OF_SERVICE on $server...\n"
            ssh winterwell@$server "sudo service $NAME_OF_SERVICE stop"
        done
    fi
}

# Bob -- Evaluate and Use
function use_bob {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        BUILD_PROCESS_NAME='bob'
        BUILD_STEP='bob was attempting to render jars'
        for server in ${TARGET_SERVERS[@]}; do
            printf "\ncleaning out old bob.log on $server ...\n"
            ssh winterwell@$server "rm -rf $PROJECT_ROOT_ON_SERVER/bob.log"
            printf "\n$server is updating bob...\n"
            ssh winterwell@$server "bob -update"
            printf "\n$server is building JARs...\n"
            ssh winterwell@$server "cd $PROJECT_ROOT_ON_SERVER && bob $BOB_ARGS $BOB_BUILD_PROJECT_NAME"
            printf "\nchecking bob.log for failures on $server\n"
            if [[ $(ssh winterwell@$server "grep -i 'Compile task failed' $PROJECT_ROOT_ON_SERVER/bob.log") = '' ]]; then
                printf "\nNo failures recorded in bob.log on $server.  JARs should be fine.\n"
            else
                printf "\nFailure or failures detected in latest bob.log. Sending Alert Emails and Breaking Operation\n"
                send_alert_email
                exit 0
            fi
        done
    fi
}

# NPM -- Evaluate and Use
function use_npm {
    if [[ $PROJECT_USES_NPM = 'yes' ]]; then
        BUILD_PROCESS_NAME='npm'
        BUILD_STEP='npm was downloading packages'
        NPM_LOG_DATE=$(date +%Y-%m-%d)
        for server in ${TARGET_SERVERS[@]}; do
            if [[ $NPM_CLEANOUT = 'yes' ]]; then
                printf "\nDeleting the existing node_modules...\n"
                ssh winterwell@$server "rm -rf $PROJECT_ROOT_ON_SERVER/node_modules"
            fi
            # Ensuring that there are no residual npm error/debug logs in place
            ssh winterwell@$server "rm -rf /home/winterwell/.npm/_logs/*.log"
            printf "\nEnsuring all NPM Packages are in place on $server ...\n"
            ssh winterwell@$server "cd $PROJECT_ROOT_ON_SERVER && npm i &> $NPM_I_LOGFILE"
            printf "\nChecking for errors while npm was attempting to get packages on $server ...\n"
            if [[ $(ssh winterwell@$server "grep -i 'error' $NPM_I_LOGFILE") = '' ]]; then
                printf "\nNPM package installer check : No mention of 'error' in $NPM_I_LOGFILE on $server\n"
            else
                printf "\nNPM encountered one or more errors while attempting to get node packages. Sending Alert Emails, but Continuing Operation\n"
                # Get the NPM_I_LOGFILE
                scp winterwell@$server:$NPM_I_LOGFILE .
                # Add it to the Attachments
                ATTACHMENTS+=("-a npm.i.for.$PROJECT_NAME.log")
                # Send the email
                send_alert_email
            fi
            if [[ $(ssh winterwell@$server "grep -i 'is not in the npm registry' $NPM_I_LOGFILE") = '' ]]; then
                printf "\nNPM package installer check : No mention of packages which could not be found in $NPM_I_LOGFILE on $server\n"
            else
                printf "\nNPM encountered one or more errors while attempting to get node packages. Sending Alert Emails, but Continuing Operation\n"
                # Get the NPM_I_LOGFILE
                scp winterwell@$server:$NPM_I_LOGFILE .
                # Add it to the Attachments
                ATTACHMENTS+=("-a npm.i.for.$PROJECT_NAME.log")
                # Send the email
                send_alert_email
            fi
        done
    fi
}

# Webpack -- Evaluate and Use
function use_webpack {
    if [[ $PROJECT_USES_WEBPACK = 'yes' ]]; then
        BUILD_PROCESS_NAME='webpack'
        BUILD_STEP='npm was running a weback process'
        for server in ${TARGET_SERVERS[@]}; do
            printf "\nNPM is now running a Webpack process on $server\n"
            ssh winterwell@$server "cd $PROJECT_ROOT_ON_SERVER && npm run compile &> $NPM_RUN_COMPILE_LOGFILE"
            printf "\nChecking for errors that occurred during Webpacking process on $server ...\n"
            if [[ $(ssh winterwell@$server "cat $NPM_RUN_COMPILE_LOGFILE | grep -i 'error' | grep -iv 'ErrorAlert.jsx'") = '' ]]; then
                printf "\nNo Webpacking errors detected on $server\n"
            else
                printf "\nOne or more errors were recorded during the webpacking process. Sending Alert Emails, but Continuing Operation\n"
                # Get the NPM_RUN_COMPILE_LOGFILE
                scp winterwell@$server:$NPM_RUN_COMPILE_LOGFILE .
                # Add it to the Attachments
                ATTACHMENTS+=("-a npm.run.compile.for.$PROJECT_NAME.log")
                # Send the email
                send_alert_email
            fi
        done
    fi
}

# Jerbil -- Evaluate and Use
function use_jerbil {
    if [[ $PROJECT_USES_JERBIL = 'yes' ]]; then
        BUILD_PROCESS_NAME='jerbil'
        BUILD_STEP='jerbil was attempting to render markdown to html'
        for server in ${TARGET_SERVERS[@]}; do
            printf "\n$server is ensuring that jerbil is up to date\n"
            ssh winterwell@$server "jerbil -update"
            printf "\n$server is converting markdown to html..\n"
            ssh winterwell@$server "cd $PROJECT_ROOT_ON_SERVER && jerbil"
            ### Is there a way to check for errors?  I'd like to check to check for errors
        done
    fi
}

# Starting the JVM Backend (if applicable)
function start_service {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        for server in ${TARGET_SERVERS[@]}; do
            printf "\nStarting $NAME_OF_SERVICE on $server...\n"
            ssh winterwell@$server "sudo service $NAME_OF_SERVICE start"
        done
    fi
}


################
### Run the Functions in Order
################
check_repo_exists
check_bob_exists
check_jerbil_exists
check_wwappbasejs_exists
cleanup_repo
cleanup_wwappbasejs_repo
stop_service
use_bob
use_npm
use_webpack
use_jerbil
start_service
