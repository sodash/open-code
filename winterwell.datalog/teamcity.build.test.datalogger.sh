#!/bin/bash

# TeamCity Continuous Integration Builder Template

# Versions of this script are usually run by TeamCity, in response to a git commit.
# The script uses ssh remote commands to target a server -- it does not affect the local machine.
# For testing, the script can also be run from your local computer.
#Version 1.4.9
# Latest Change -- nodejs version checker now checks for version 14.x being present

#####  GENERAL SETTINGS
## This section should be the most widely edited part of this script
## Set the Variables according to your project's name, directory path, git-checkout branches, etc.
## Set the preferences according to your project's needs
#####
PROJECT_NAME='open-code' #This name will be used to create/or/refer-to the directory of the project in /home/winterwell/
GIT_REPO_URL='github.com:/good-loop/open-code' #
PROJECT_USES_BOB='yes'  #yes or no :: If 'yes', then you must also supply the name of the service which is used to start,stop,or restart the jvm
NAME_OF_SERVICE='lg' # This can be blank, but if your service uses a JVM, then you must put in the service name which is used to start,stop,or restart the JVM on the server.
PROJECT_USES_NPM='no' # yes or no
PROJECT_USES_WEBPACK='no' #yes or no
PROJECT_USES_JERBIL='no' #yes or no
PROJECT_USES_WWAPPBASE_SYMLINK='no'
BRANCH='master' # If target machine is NOT defined in BuildHacks.java, then it is treated as a 'test' device and will be LOCKED TO THE MASTER BRANCH.  
                # If changed -- you must also change the VCS settings for this project in teamcity

# Where is the test server?
TARGET_SERVERS=(baker.good-loop.com)


#####  SPECIFIC SETTINGS
## This section should only be selectively edited - based on non-standardized needs
#####
PROJECT_ROOT_ON_SERVER="/home/winterwell/$PROJECT_NAME/winterwell.datalog"
WWAPPBASE_REPO_PATH_ON_SERVER_DISK="/home/winterwell/wwappbase.js"
PROJECT_LOG_FILE="$PROJECT_ROOT_ON_SERVER/logs/$PROJECT_NAME.log"


##### UNDENIABLY ESOTERIC SETTINGS
## This is the space where your project's settings make it completely non-standard
#####
EMAIL_RECIPIENTS=(sysadmin@good-loop.com daniel@good-loop.com roscoe@good-loop.com)
BOB_ARGS='' #you can set bob arguments here, but they will run each and every time that the project is auto-built
BOB_BUILD_PROJECT_NAME='' #If the project name isn't automatically sensed by bob, you can set it explicitly here
NPM_CLEANOUT='no' #yes/no , will nuke the node_modules directory if 'yes', and then get brand-new packages.
NPM_I_LOGFILE="/home/winterwell/.npm/_logs/npm.i.for.$PROJECT_NAME.log"
NPM_RUN_COMPILE_LOGFILE="/home/winterwell/.npm/_logs/npm.run.compile.for.$PROJECT_NAME.log"
BOBWAREHOUSE_PATH='/home/winterwell/bobwarehouse'

##### FUNCTIONS
## Do not edit these unless you know what you are doing
#####

# Set empty array of attachments
ATTACHMENTS=()

# Email Function.  Sends emails to email addresses in $EMAIL_RECIPIENTS
function send_alert_email {
    for email in ${EMAIL_RECIPIENTS[@]}; do
        TIME=$(date +%Y-%m-%dT%H:%M:%S-%Z)
	    message="AutoBuild Detected a Failure Building $PROJECT_NAME during $BUILD_PROCESS_NAME"
	    body="Hi,\nThe AutoPublisher detected a failure when $BUILD_STEP"
	    title="TeamCity $message"
	    printf "$body" | mutt -s "$title" ${ATTACHMENTS[@]} -- $email
    done
}


# Git Cleanup Function -- More of a classic 'I type this too much, it should be a function', Function. This Function's Version is 1.01
function git_hard_set_to_master {
    ssh winterwell@$server "cd $1 && git gc --prune=now"
    ssh winterwell@$server "cd $1 && git checkout -f $BRANCH"
    ssh winterwell@$server "cd $1 && git pull origin $BRANCH"
    ssh winterwell@$server "cd $1 && git reset --hard FETCH_HEAD"
}


# Dependency Check Function : Check if repo exists on the server('s) disk(s) - This Function's Version is 0.01
function check_repo_exists {
    for server in ${TARGET_SERVERS[@]}; do
        ssh winterwell@$server "if [[ ! -d $PROJECT_ROOT_ON_SERVER ]]; then printf '\ncloning the repo...\n'; git clone git@$GIT_REPO_URL; fi"
    done
}

# Dependency Check Function - 'bob' is globally available - This Function's Version is 0.01
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

# Dependency Check Function - 'jerbil' is globally available - This Function's Version is 0.01
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

# Dependency Check Function - 'mvn' is globally available - This Function's Version is 0.01
function check_maven_exists {
    BUILD_PROCESS_NAME='checking for maven'
    BUILD_STEP='looking for globally available "mvn" from the command line interface'
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        for server in ${TARGET_SERVERS[@]}; do
            if [[ $(ssh winterwell@$server "which mvn") = '' ]]; then
                printf "\nNo global installation of maven was found. As 'root' please:\n\tapt-get install maven\nAnd then retry this script\n"
                send_alert_email
                exit 0
            fi
        done
    fi
}

# Dependency Check Function - nodejs is at version 14.x - This Function's Version is 0.02
function check_nodejs_version {
    BUILD_PROCESS_NAME='verifying nodejs version'
    BUILD_STEP='verifying that nodejs is at version 14.x.x'
    if [[ $PROJECT_USES_NPM = 'yes' ]]; then
        for server in ${TARGET_SERVERS[@]}; do
            if [[ $(ssh winterwell@$server 'node -v | grep "v14"') = '' ]]; then
                printf "Either nodejs is not installed, or it is not at version 14.x.x\n"
                send_alert_email
                exit 0
            fi
        done
    fi
}

# Dependency Check Function - wwappbase.js is verified on disk - This Function's Version is 0.01
function check_for_wwappbasejs_location {
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

# Dependency Check Function - bobwarehouse directory has discrete 'code' repository nested inside of it. - This Function's Version is 1.01
function check_for_code_repo_in_bobwarehouse {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        for server in ${TARGET_SERVERS[@]}; do
            if ssh winterwell@$server "[ ! -d $BOBWAREHOUSE_PATH/code ]"; then
                printf "\n\nNo 'code' repo found in $BOBWAREHOUSE_PATH on $server.  Cloning now ...\n"
                ssh winterwell@$server "cd $BOBWAREHOUSE_PATH && git clone git@git.winterwell.com:/winterwell-code code"
                printf "\nContinuing without verifying successful cloning of the winterwell-code repo...\n"
            fi
        done
    fi
}

# Cleanup Git -- Ensure a clean and predictable git repo for building - This Function's Version is 1.01
function cleanup_repo {
    for server in ${TARGET_SERVERS[@]}; do
        printf "\nCleaning $server 's local repository...\n"
        git_hard_set_to_master $PROJECT_ROOT_ON_SERVER
        # If this is a node relient project, kill any existing package-lock.json
        if [[ $PROJECT_USES_NPM = 'yes' ]]; then
            for server in ${TARGET_SERVERS[@]}; do
                printf "\nGetting rid of any package-lock.json files\n"
                # using reverse logic.  if package-lock.json does NOT exist, do nothing.  If it DOES exist, delete it.
                if ssh winterwell@$server "[ ! -f $PROJECT_ROOT_ON_SERVER/package-lock.json ]"; then
                    printf "\nno package-lock.json found.  No need to remove it.\n"
                else
                    ssh winterwell@$server "rm $PROJECT_ROOT_ON_SERVER/package-lock.json"
                fi
            done
        fi
    done
}

# Cleanup wwappbase.js 's repo -- Ensure that this repository is up to date and clean - This Function's Version is 1.00
function cleanup_wwappbasejs_repo {
    if [[ $PROJECT_USES_WWAPPBASE_SYMLINK = 'yes' ]]; then
        for server in ${TARGET_SERVERS[@]}; do
            printf "\nCleaning $server 's local wwappbase.js repository\n"
            git_hard_set_to_master $WWAPPBASE_REPO_PATH_ON_SERVER_DISK
        done
    fi
}

# Cleanup the repos nested inside of bobwarehouse  - This Function's Version is 1.10
function cleanup_bobwarehouse_repos {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
	    for server in ${TARGET_SERVERS[@]}; do
		    printf "\nEnsuring that the repos inside of bobwarehouse are up-to-date...\n"
        	ssh winterwell@$server "for repo in $BOBWAREHOUSE_PATH/*/; do cd \$repo; git gc --prune=now; git pull origin master; git reset --hard FETCH_HEAD; git checkout $BRANCH; done"
        done
    fi
}

# Stopping the JVM Backend (if applicable) - This Function's Version is 0.01
function stop_service {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        for server in ${TARGET_SERVERS[@]}; do
            printf "\nStopping $NAME_OF_SERVICE on $server...\n"
            ssh winterwell@$server "sudo service $NAME_OF_SERVICE stop"
        done
    fi
}

# Bob -- Evaluate and Use - This Function's Version is 0.03
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
                printf "\nNo failures recorded in bob.log on $server in first bob.log sweep.\n"
            else
                printf "\nFailure or failures detected in latest bob.log. Sending Alert Emails and Breaking Operation\n"
                # Get the bob.log
                scp winterwell@$server:$PROJECT_ROOT_ON_SERVER/bob.log .
                ATTACHMENTS+=("-a bob.log")
                send_alert_email
                # remove bob.log from the teamcity server's disk
                rm bob.log
                exit 0
            fi
            if [[ $(ssh winterwell@$server "grep -i 'ERROR EXIT' $PROJECT_ROOT_ON_SERVER/bob.log") = '' ]]; then
                printf "\nBob reported a clean exit from it's process.  Continuing to next task.\n"
            else
                printf "\nFailure or failures detected in latest bob.log. Sending Alert Emails and Breaking Operation\n"
                # Get the bob.log
                scp winterwell@$server:$PROJECT_ROOT_ON_SERVER/bob.log .
                ATTACHMENTS+=("-a bob.log")
                send_alert_email
                # remove bob.log from the teamcity server's disk
                rm bob.log
                exit 0
            fi
        done
    fi
}

# NPM -- Evaluate and Use - This Function's Version is 0.02
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
            if [[ $(ssh winterwell@$server "grep -i 'error ' $NPM_I_LOGFILE") = '' ]]; then
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

# Webpack -- Evaluate and Use - This Function's Version is 0.02
function use_webpack {
    if [[ $PROJECT_USES_WEBPACK = 'yes' ]]; then
        BUILD_PROCESS_NAME='webpack'
        BUILD_STEP='npm was running a weback process'
        for server in ${TARGET_SERVERS[@]}; do
            printf "\nNPM is now running a Webpack process on $server\n"
            ssh winterwell@$server "cd $PROJECT_ROOT_ON_SERVER && npm run compile &> $NPM_RUN_COMPILE_LOGFILE"
            printf "\nChecking for errors that occurred during Webpacking process on $server ...\n"
            if [[ $(ssh winterwell@$server "cat $NPM_RUN_COMPILE_LOGFILE | grep -i 'error ' | grep -iv 'ErrorAlert.jsx'") = '' ]]; then
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

# Jerbil -- Evaluate and Use - This Function's Version is 0.01
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

# Starting the JVM Backend (if applicable) - This Function's Version is 0.01
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
check_maven_exists
check_nodejs_version
check_for_wwappbasejs_location
check_for_code_repo_in_bobwarehouse
cleanup_repo
cleanup_wwappbasejs_repo
cleanup_bobwarehouse_repos
stop_service
use_bob
use_npm
use_webpack
use_jerbil
start_service

