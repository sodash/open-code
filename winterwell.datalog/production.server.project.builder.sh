#!/bin/bash


# Production Server -- Project Builder
# VERSION=0.94

## Warning - This is a bare-bones template file.
##     There are no functions written in here to
##      preserve your config files, or uploaded media.
##      It is expected that you will modify this script
##      so as to have it meet these esoteric needs.

## USAGE -- human instructions for a how to use:
# Assumption -- you want to use this script to build
#     a project (eg. adserver, datalogger, portal, etc.)
#     on a production server.
#
# 01. ssh into the production server
#
# 02. Have this project builder script already on the server repo,
#     with this current version with all of the current edits.
#     You might have to `git pull` in order to get it's repo up
#     to date. And get this script up-to-date
#
# 03. Get yourself into a tmux session -- this will allow your
#     command(s) to be executed regardless of your ssh connection
#     status
#
# 04. as the user 'winterwell':
#     cd into your project's repository
#     ./production.server.project.builder.sh
#     and answer the yes/no prompt when you are SURE that you
#     have gotten all of your ducks in a row.

####### Goal -- create a project building script (using git, bob, jerbil, maven, npm, and webpack as the tools)
## which uses very similar, if not, the exact same, functions as the teamcity builder template script.
PROJECT_NAME='datalogger' #This is simply a human readable name
GIT_REPO_URL='github.com:/good-loop/open-code'
PROJECT_USES_BOB='yes'  #yes or no :: If 'yes', then you must also supply the name of the service which is used to start,stop,or restart the jvm
NAME_OF_SERVICE='lg' # This can be blank, but if your service uses a JVM, then you must put in the service name which is used to start,stop,or restart the JVM on the server.
PROJECT_USES_NPM='no' # yes or no
PROJECT_USES_WEBPACK='no' #yes or no
PROJECT_USES_JERBIL='no' #yes or no
PROJECT_USES_WWAPPBASE_SYMLINK='no'


#####  SPECIFIC SETTINGS
## This section should only be selectively edited - based on non-standardized needs
#####
PROJECT_ROOT_ON_SERVER="/home/winterwell/open-code/winterwell.datalog"
WWAPPBASE_REPO_PATH_ON_SERVER_DISK="/home/winterwell/wwappbase.js"
PROJECT_LOG_FILE="$PROJECT_ROOT_ON_SERVER/logs/datalog.log"

##### UNDENIABLY ESOTERIC SETTINGS
## This is the space where your project's settings make it completely non-standard
#####
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


# Git Cleanup Function -- More of a classic 'I type this too much, it should be a function', Function.
function git_hard_set_to_master {
    cd $1 && git gc --prune=now
    cd $1 && git pull origin master
    cd $1 && git reset --hard FETCH_HEAD
    cd $1 && git checkout -f master
    cd $1 && git pull
}

# Git change branch -- the hard way  - This Function's Version is 0.01
function git_change_branch {
    cd $1 && git checkout -f $BRANCH_NAME
    cd $1 && git gc --prune=now
    cd $1 && git pull origin $BRANCH_NAME
    cd $1 && git reset --hard FETCH_HEAD
}


# Dependency Check Function : Check if repo exists on the server('s) disk(s) - This Function's Version is 0.01
function check_repo_exists {
    if [[ ! -d $PROJECT_ROOT_ON_SERVER ]]; then
        printf '\ncloning the repo...\n'
        cd ~/
        git clone git@$GIT_REPO_URL
    fi
}

# Dependency Check Function - 'bob' is globally available - This Function's Version is 0.01
function check_bob_exists {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        if [[ $(which bob) = '' ]]; then
            printf "\nNo global installation of 'bob' was found. As 'root' please:\n\tnpm install -g java-bob\nAnd then as a regular user, please:\n\tbob\nand then retry this script\n"
            exit 0
        fi
    fi
}

# Dependency Check Function - 'jerbil' is globally available - This Function's Version is 0.01
function check_jerbil_exists {
    if [[ $PROJECT_USES_JERBIL = 'yes' ]]; then
        if [[ $(which jerbil) = '' ]]; then
            printf "\nNo global installation of 'jerbil' was found. As 'root' please:\n\tnpm install -g jerbil-website\nAnd then as a regular user, please:\n\tjerbil\nAnd then retry this script\n"
            exit 0
        fi
    fi
}

# Dependency Check Function - 'mvn' is globally available - This Function's Version is 0.01
function check_maven_exists {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        if [[ $(which mvn) = '' ]]; then
            printf "\nNo global installation of maven was found. As 'root' please:\n\tapt-get install maven\nAnd then retry this script\n"
            exit 0
        fi
    fi
}

# Dependency Check Function - 'JAVA_HOME' is set correctly in this CLI environment - This Function's Version is 0.01
function check_java_home {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        if [[ $(printf $JAVA_HOME) = '' ]]; then
            printf "\nYou must have an export in this user's .bashrc file which set's JAVA_HOME to the path where your java is installed\n\n\texport JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64\n\nand then\n\tsource ~/.bashrc\nAnd then retry this script\n"
            exit 0
        fi
        if [[ $(printf $JAVA_HOME) != '/usr/lib/jvm/java-11-openjdk-amd64' ]]; then
            printf "\nYour \$JAVA_HOME is not set to the correct path.  It should be /usr/lib/jvm/java-11-openjdk-amd64\n"
            exit 0
        fi
    fi
}

# Dependency Check Function - nodejs is at version 14.x - This Function's Version is 0.02
function check_nodejs_version {
    if [[ $PROJECT_USES_NPM = 'yes' ]]; then
        if [[ $(node -v | grep "v14") = '' ]]; then
            printf "Either nodejs is not installed, or it is not at version 14.x.x\n"
            exit 0
        fi
    fi
}

# Dependency Check Function - wwappbase.js is verified on disk - This Function's Version is 0.01
function check_for_wwappbasejs_location {
    if [[ $PROJECT_USES_WWAPPBASE_SYMLINK = 'yes' ]]; then
        if [[ ! -d $WWAPPBASE_REPO_PATH_ON_SERVER_DISK ]]; then
            printf "\nThe location of $WWAPPBASE_REPO_PATH_ON_SERVER_DISK could not be verified.  Please check this server's repositories and then check the variables regarding wwappbase.js at the top of this script.\n"
            exit 0
        fi
    fi
}

# Dependency Check Function - bobwarehouse directory has discrete 'code' repository nested inside of it. - This Function's Version is 0.01
function check_for_code_repo_in_bobwarehouse {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        if [[ ! -d $BOBWAREHOUSE_PATH/code ]]; then
            printf "\n\nNo 'code' repo found in $BOBWAREHOUSE_PATH.  Cloning now ...\n"
            cd $BOBWAREHOUSE_PATH && git clone git@git.winterwell.com:/winterwell-code code
            printf "\nContinuing without verifying successful cloning of the winterwell-code repo...\n"
        fi
    fi
}

# Cleanup Git -- Ensure a clean and predictable git repo for building - This Function's Version is 1.01
function cleanup_repo {
    printf "\nCleaning $HOSTNAME 's local repository...\n"
    git_hard_set_to_master $PROJECT_ROOT_ON_SERVER
    # If this is a node relient project, kill any existing package-lock.json
    if [[ $PROJECT_USES_NPM = 'yes' ]]; then
        # using reverse logic.  if package-lock.json does NOT exist, do nothing.  If it DOES exist, delete it.
        if [ ! -f $PROJECT_ROOT_ON_SERVER/package-lock.json ]; then
            printf ""
        else
            printf "\nFound a package-lock.json file.  Auto-Removing .\n"
            rm $PROJECT_ROOT_ON_SERVER/package-lock.json
        fi
    fi
}

# Cleanup wwappbase.js 's repo -- Ensure that this repository is up to date and clean - This Function's Version is 0.01
function cleanup_wwappbasejs_repo {
    if [[ $PROJECT_USES_WWAPPBASE_SYMLINK = 'yes' ]]; then
        printf "\nCleaning $HOSTNAME 's local wwappbase.js repository\n"
        git_hard_set_to_master $WWAPPBASE_REPO_PATH_ON_SERVER_DISK
    fi
}

# Cleanup the repos nested inside of bobwarehouse  - This Function's Version is 0.02
function cleanup_bobwarehouse_repos {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        printf "\nEnsuring that the repos inside of bobwarehouse are up-to-date...\n"
        for repo in $BOBWAREHOUSE_PATH/*/; do
            if [[ $repo = '/home/winterwell/bobwarehouse/flexi-gson/' ]]; then
                printf "\e[30;107mFound and skipped flex-gson -- BOB controls this repo\e[0m\n"
            elif [[ $repo = '/home/winterwell/bobwarehouse/jtwitter/' ]]; then
                printf "\e[30;107mFound and skipped jtwitter -- BOB controls this repo\e[0m\n"
            elif [[ $repo = '/home/winterwell/bobwarehouse/juice/' ]]; then
                printf "\e[30;107mFound and skipped juice -- BOB controls this repo\e[0m\n"
            else
                git_hard_set_to_master $repo
            fi
        done
    fi
}

# Checkout git branch on all repos for this release - This Function's Version is 0.02
function git_checkout_release_branch {
    printf "\nSwitching to your specified release branch ...\n"
    git_change_branch $PROJECT_ROOT_ON_SERVER
    if [[ $PROJECT_USES_WWAPPBASE_SYMLINK = 'yes' ]]; then
        git_change_branch $WWAPPBASE_REPO_PATH_ON_SERVER_DISK
    fi
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        for repo in $BOBWAREHOUSE_PATH/*/; do
            if [[ $repo = '/home/winterwell/bobwarehouse/flexi-gson/' ]]; then
                printf "\e[30;107mFound and skipped flex-gson -- BOB controls this repo\e[0m\n"
            elif [[ $repo = '/home/winterwell/bobwarehouse/jtwitter/' ]]; then
                printf "\e[30;107mFound and skipped jtwitter -- BOB controls this repo\e[0m\n"
            elif [[ $repo = '/home/winterwell/bobwarehouse/juice/' ]]; then
                printf "\e[30;107mFound and skipped juice -- BOB controls this repo\e[0m\n"
            else
                git_change_branch $repo
            fi
        done
    fi
}

# Stopping the JVM Backend (if applicable) - This Function's Version is 0.01
function stop_service {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        printf "\nStopping $NAME_OF_SERVICE on $HOSTNAME...\n"
        sudo service $NAME_OF_SERVICE stop
    fi
}

# Bob -- Evaluate and Use - This Function's Version is 0.02
function use_bob {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        printf "\ncleaning out old bob.log ...\n"
        rm -rf $PROJECT_ROOT_ON_SERVER/bob.log
        printf "\nupdating bob...\n"
        bob -update
        printf "\nCleaning out old bobhistory.csv...\n"
        rm $BOBWAREHOUSE_PATH/bobhistory.csv
        printf "\nbuilding JARs...\n"
        cd $PROJECT_ROOT_ON_SERVER && bob $BOB_ARGS $BOB_BUILD_PROJECT_NAME
        printf "\nchecking bob.log for failures\n"
        if [[ $(grep -i 'Compile task failed' $PROJECT_ROOT_ON_SERVER/bob.log) = '' ]]; then
            printf "\nNo failures recorded in bob.log on $HOSTNAME in first bob.log sweep.\n"
        else
            printf "\nFailure or failures detected in latest bob.log. Breaking Operation\n"
            printf "\nAttempting to turn $NAME_OF_SERVICE back on...\n"
            sudo service $NAME_OF_SERVICE start
            printf "\nCheck the file $PROJECT_ROOT_ON_SERVER/bob.log for the failure, and check to see if\n"
            printf "this server's old build is running.\n"
            printf "\n\n\t\e[37;41mATTENTION: YOUR BUILD IS INCOMPLETE AND YOUR SERVICE/SITE MIGHT BE DOWN\e[0m\n"
            exit 0
        fi
        if [[ $(grep -i 'ERROR EXIT' $PROJECT_ROOT_ON_SERVER/bob.log) = '' ]]; then
            printf "\nBob reported a clean exit from it's process.  Continuing to next task.\n"
        else
            printf "\nFailure or failures detected in latest bob.log. Breaking Operation\n"
            printf "\nAttempting to turn $NAME_OF_SERVICE back on...\n"
            sudo service $NAME_OF_SERVICE start
            printf "\nCheck the file $PROJECT_ROOT_ON_SERVER/bob.log for the failure, and check to see if\n"
            printf "this server's old build is running.\n"
            printf "\n\n\t\e[37;41mATTENTION: YOUR BUILD IS INCOMPLETE AND YOUR SERVICE/SITE MIGHT BE DOWN\e[0m\n"
            exit 0
        fi
    fi
}

# NPM -- Evaluate and Use - This Function's Version is 0.01
function use_npm {
    if [[ $PROJECT_USES_NPM = 'yes' ]]; then
        if [[ $NPM_CLEANOUT = 'yes' ]]; then
            printf "\nDeleting the existing node_modules...\n"
            rm -rf $PROJECT_ROOT_ON_SERVER/node_modules
        fi
        # Ensuring that there are no residual npm error/debug logs in place
        rm -rf /home/winterwell/.npm/_logs/*.log
        printf "\nEnsuring all NPM Packages are in place for $PROJECT_NAME ...\n"
        cd $PROJECT_ROOT_ON_SERVER && npm i &> $NPM_I_LOGFILE
        printf "\nChecking for errors while npm was attempting to get packages ...\n"
        if [[ $(grep -i 'error' $NPM_I_LOGFILE) = '' ]]; then
            printf "\nNPM package installer check : No mention of 'error' in $NPM_I_LOGFILE\n"
        else
            printf "\nNPM encountered one or more errors while attempting to get node packages in $PROJECT_ROOT_ON_SERVER. Breaking Operation\n"
            printf "\n\n\t\e[37;41mATTENTION: YOUR BUILD IS INCOMPLETE AND YOUR SERVICE/SITE MIGHT BE DOWN\e[0m\n"
            exit 0
        fi
        if [[ $(grep -i 'is not in the npm registry' $NPM_I_LOGFILE) = '' ]]; then
            printf "\nNPM package installer check : No mention of packages which could not be found in $NPM_I_LOGFILE\n"
        else
            printf "\nNPM encountered one or more errors while attempting to get node packages. Breaking Operation\n"
            printf "\n\n\t\e[37;41mATTENTION: YOUR BUILD IS INCOMPLETE AND YOUR SERVICE/SITE MIGHT BE DOWN\e[0m\n"
            exit 0 
        fi
    fi
}

# Webpack -- Evaluate and Use - This Function's Version is 0.01
function use_webpack {
    if [[ $PROJECT_USES_WEBPACK = 'yes' ]]; then
        printf "\nNPM is now running a Webpack process for $PROJECT_NAME\n"
        cd $PROJECT_ROOT_ON_SERVER && npm run compile &> $NPM_RUN_COMPILE_LOGFILE
        printf "\nChecking for errors that occurred during Webpacking process ...\n"
        if [[ $(cat $NPM_RUN_COMPILE_LOGFILE | grep -i 'error' | grep -iv 'ErrorAlert.jsx') = '' ]]; then
            printf "\nNo Webpacking errors detected\n"
        else
            printf "\nOne or more errors were recorded during the webpacking process. Breaking Operation\n"
            printf "\n\n\t\e[37;41mATTENTION: YOUR BUILD IS INCOMPLETE AND YOUR SERVICE/SITE MIGHT BE DOWN\e[0m\n"
            exit 0
        fi
    fi
}

# Jerbil -- Evaluate and Use - This Function's Version is 0.01
function use_jerbil {
    if [[ $PROJECT_USES_JERBIL = 'yes' ]]; then
        printf "\nEnsuring that jerbil is up to date\n"
        jerbil -update
        printf "\nConverting markdown to html..\n"
        cd $PROJECT_ROOT_ON_SERVER && jerbil
        ### Is there a way to check for errors?  I'd like to check to check for errors
    fi
}

# Starting the JVM Backend (if applicable) - This Function's Version is 0.01
function start_service {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        printf "\nStarting $NAME_OF_SERVICE...\n"
        sudo service $NAME_OF_SERVICE start
    fi
}


################
### Interactive Portion -- Have the Human User type in the name of the git branch that
###                         will be used to build the project. Also, give them once last
###                         chance to back out if this was executed accidentally.
################
function get_branch_and_print_warning {
    printf "\n\e[34;107mWhat branch would you like to use for this production build?\033[0m\n"
    read branch
    BRANCH_NAME=$branch
    printf "\n\e[34;107mAre you absolutely certain that you want to build and release $PROJECT_NAME on this Production Server\033[0m\n\e[34;107mBased on your specified branch of $BRANCH_NAME ?\033[0m"
    if [[ $PROJECT_USES_WWAPPBASE_SYMLINK = 'yes' ]]; then
        printf "\n\t\e[34;107mFurther, are you certain that the branch $BRANCH_NAME exists in the wwappbase.js repo?\033[0m\n"
    fi
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        printf "\n\t\e[34;107mFurther, are you certain that the branch $BRANCH_NAME exists in the open-code, elasticsearch-java-client, and winterwell-code repos?\033[0m\n"
    fi
    printf "\n\nyes/no\n"
    read answer
    ANSWER=$answer
    case $ANSWER in
        yes|YES)
            printf "\n\n\t\tContinuing Production Build\n\n"
        ;;
        no|NO)
            printf "\n\n\t\tAborting Operation\n\n"
            exit 0
        ;;
        *)
            printf "\n\n\t\tAnswer not understood.  Aborting Operation\n\n"
            exit 0
        ;;
    esac
}

## Checking the immediate logged output for errors or warnings - This Function's Version is 0.02
function catch_JVM_success_or_error {
    if [[ $PROJECT_USES_BOB = 'yes' ]]; then
        INITIAL_LOG_NUM_LINES=$(wc -l $PROJECT_LOG_FILE | awk '{print $1}')
        while read -t 10 line; do
            case "$line" in
                *"AMain Running"* )
                    printf "\n\t$PROJECT_NAME 's JVM reports a successful startup\n"
                    exit
                ;;
                *"ES.init To reindex"* )
                    printf "\n\t\e[30;41m$PROJECT_NAME reports that at least one ES index will need to be re-indexed and re-aliased\e[0m\n"
                    printf "You'll need to read the logged output of the JVM in order to see what exactly needs to be changed\n"
                    exit
                ;;
            esac
        done < <(tail --lines=+$INITIAL_LOG_NUM_LINES -f $PROJECT_LOG_FILE)
        RETVAL=$?
        case $RETVAL in
            0)
                echo ""
            ;;
            *)
                printf "The JVM was given 10 seconds to report either success or that an elasticsearch index requires a re-index and re-aliasing before it could start. No such indication was received and parsed.  Please check your service and the log file for this project\n"
            ;;
        esac
    fi
}



################
### Run the Functions in Order
################
get_branch_and_print_warning
check_bob_exists
check_jerbil_exists
check_maven_exists
check_java_home
check_nodejs_version
check_for_wwappbasejs_location
check_for_code_repo_in_bobwarehouse
cleanup_repo
cleanup_wwappbasejs_repo
cleanup_bobwarehouse_repos
git_checkout_release_branch
stop_service
use_bob
use_npm
use_webpack
use_jerbil
start_service
catch_JVM_success_or_error
