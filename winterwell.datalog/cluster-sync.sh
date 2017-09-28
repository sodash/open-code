#!/bin/bash

######
##DO NOT ATTEMPT TO RUN THIS FROM YOUR LOCAL DEVBOX.
##THIS SCRIPT IS MEANT TO BE ONLY RUN FROM THE CURRENT
##GOOD-LOOP DATALOGGER 'PUBLISH' SERVER. ANY SERVER
##IN THE CLUSTER CAN SERVE AS THE 'PUBLISH' SERVER.
######

######
##PROCESSING TARGET LIST
######
TARGETS=$(cat /tmp/target.list.txt)
TARGETS=($TARGETS)

TYPEOFPUSHOUT=$1
case $1 in
	frontend|FRONTEND)
		echo "This publishing process will only sync the FRONTEND files, and will not sync backend files"
		PUBLISHLEVEL='frontend'
	;;
	backend|BACKEND)
		echo "This publishing process will only sync the BACKEND files, and will not sync the frontend files"
		PUBLISHLEVEL='backend'
	;;
	everything|EVERYTHING)
		echo "This publishing process will publish BOTH the FRONTEND and BACKEND files"
		PUBLISHLEVEL='everything'
	;;
	*)
		echo -e "I couldn't understand if you wanted the Frontend, Backend, or Everything to be published to ${TARGET[*]}"
		echo -e ""
		echo -e "I am now exiting this publishing process for your own safety."
		exit 2
	;;
esac

PARSYNC='parallel-rsync -h /tmp/target.list.txt --user=winterwell --recursive'
PASSH='parallel-ssh -h /tmp/target.list.txt --user=winterwell'
LGHOME='/home/winterwell/lg.good-loop.com'

#########WRITE FUNCTIONS FOR FRONTEND,BACKEND,AND EVERYTHING PUBLISHES

function frontend_publish {
	echo -e "> ${HOSTNAME} says: 'Cleaning Cluster's Frontend...'"
	$PASSH 'rm -rf /home/winterwell/lg.good-loop.com/web'
	$PASSH 'rm -rf /home/winterwell/lg.good-loop.com/webpack.config.json'
	$PASSH 'rm -rf /home/winterwell/lg.good-loop.com/src-js'
	$PASSH 'rm -rf /home/winterwell/lg.good-loop.com/package.json'
	echo -e "> ${HOSTNAME} says: 'Syncing Frontend...'"
	$PARSYNC $LGHOME/web $LGHOME
	$PARSYNC $LGHOME/webpack.config.json $LGHOME
	$PARSYNC $LGHOME/src-js $LGHOME
	$PARSYNC $LGHOME/package.json $LGHOME
}


function backend_publish {
	echo -e "> ${HOSTNAME} says: 'Cleaning Cluster's Backend...'"
	$PASSH 'rm -rf /home/winterwell/lg.good-loop.com/lib/*.jar'
	echo -e "> ${HOSTNAME} says: 'Syncing Backend..."
	$PARSYNC $LGHOME/lib $LGHOME/lib
}

function everything_publish {
	frontend_publish
	backend_publish
}

####
#PERFORMING THE PUBLISH
####
case "$PUBLISHLEVEL" in
	frontend)
		frontend_publish
	;;
	backend)
		backend_publish
	;;
	everything)
		everything_publish
esac