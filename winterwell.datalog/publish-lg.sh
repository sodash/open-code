#!/bin/bash

# Publish-lg.sh  a script to publish files to the LG production or test server(s)

####
#CHECKING TO SEE IF YOUR MACHINE HAS THE REQUIRED SOFTWARE
####
if [ $(which parallel-ssh) = "" ]; then
	echo -e "In order to use this publishing script, you will need Parallel-SSH installed on your machine"
	echo -e "install Parallel-SSH with 'sudo apt-get install pssh'"
	exit 2
fi
####
#END OF CHECKS
####



####
#SETTING THE ARRAYS OF POTENTIAL SERVERS
####
PRODUCTIONPUBLISHER=(gl-es-03.soda.sh)  #Can be changed to any machine in the production array.
PRODUCTION=(gl-es-03.soda.sh gl-es-04.soda.sh gl-es-05.soda.sh)
TEST=(hugh.soda.sh)
####
#END OF TARGET ARRAYS
####

####
#Is this a production pushout or a test pushout?
####
TYPEOFPUSHOUT=$1
LEVEL=$2
CLEANPUBLISH=$3
####
#ACCEPTING AND PROCESSING THE ARGUMENTS
####
case $1 in
	production|PRODUCTION)
	echo "this is a PRODUCTION pushout"
	TARGET=${PRODUCTION[@]}
	TYPEOFPUSHOUT='PRODUCTION'
	;;
	test|TEST)
	echo "this is a TEST pushout"
	TARGET=$TEST
	TYPEOFPUSHOUT='TEST'
	;;
	*)
	echo "The script couldn't discern if this was a production or a test pushout.  EXITING..."
	exit 1
	;;
esac

case $2 in
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

case $3 in
	clean|CLEAN)
		echo "this publishing process is going to clear out the target server's directories before syncing"
		CLEANPUBLISH='true'
	;;
	*)
		echo "this publishing process will only overwrite old files with new versions, all other files will not be changed"
		CLEANPUBLISH='false'
	;;
esac
####
#END OF ARGUMENTS
####

####
#PRECAUTIONARY REMOVAL OF A TMP FILE
####
rm /tmp/target.list.txt
####
#END OF PRECAUTIONARY REMOVAL OF A TMP FILE
####

####
#CREATING TEXT FILE ARRAY OF TARGETTED SERVERS
####
# Removing the name of the productionpublisher from the array of production servers
if [[ $TYPEOFPUSHOUT = 'PRODUCTION' ]]; then
	#echo ${TARGET[@]/$PRODUCTIONPUBLISHER}
	TARGET=( "${TARGET[@]/$PRODUCTIONPUBLISHER}" )
fi
#
printf '%s\n' ${TARGET[@]} >> /tmp/target.list.txt
echo "TARGETS ARE:"
cat /tmp/target.list.txt
####
#END OF CREATING TEXT FILE ARRAY OF TARGETTED SERVERS
####

####
#SETTING THE FUNCTIONS FOR THE LEVEL OF PUBLISH
####
function frontend_publish {
	echo -e "> Converting LESS files to CSS..."
	./build-less.sh
	echo ""
	if [[ $TYPEOFPUSHOUT = 'TEST' ]]; then
		echo -e "> Strictly Syncing the Frontend from YOUR localmachine to $TEST"
		rsync -rhP --delete-before web winterwell@$TEST:/home/winterwell/lg.good-loop.com/
		rsync -rhP --delete-before package.json winterwell@$TEST:/home/winterwell/lg.good-loop.com/
		rsync -rhP --delete-before webpack.config.js winterwell@$TEST:/home/winterwell/lg.good-loop.com/
		rsync -rhP --delete-before src-js winterwell@$TEST:/home/winterwell/lg.good-loop.com/
	else
		echo -e "> Strictly Syncing the Frontend from YOUR localmachine to $PRODUCTIONPUBLISHER"
		rsync -rhP --delete-before web winterwell@$PRODUCTIONPUBLISHER:/home/winterwell/lg.good-loop.com/
		rsync -rhP --delete-before package.json winterwell@$PRODUCTIONPUBLISHER:/home/winterwell/lg.good-loop.com/
		rsync -rhP --delete-before webpack.config.js winterwell@$PRODUCTIONPUBLISHER:/home/winterwell/lg.good-loop.com/
		rsync -rhP --delete-before src-js winterwell@$PRODUCTIONPUBLISHER:/home/winterwell/lg.good-loop.com/
		echo -e "> Sending list of targets to $PRODUCTIONPUBLISHER..."
		scp /tmp/target.list.txt winterwell@$PRODUCTIONPUBLISHER:/tmp/
		echo -e "> Telling $PRODUCTIONPUBLISHER to update the frontend..."
		ssh winterwell@$PRODUCTIONPUBLISHER 'bash /home/winterwell/lg.good-loop.com/cluster-sync.sh frontend'
	fi
	echo -e "> Satisfying NPM dependencies..."
	parallel-ssh -h /tmp/target.list.txt --user=winterwell 'cd /home/winterwell/lg.good-loop.com && npm i'
	echo ""
	echo -e "> Webpacking..."
	parallel-ssh -h /tmp/target.list.txt --user=winterwell 'cd /home/winterwell/lg.good-loop.com && webpack -p'
	echo ""
	echo -e "frontend(s) have been updated for..."
	cat /tmp/target.list.txt
	echo ""
}

function backend_publish {
	if [[ $TYPEOFPUSHOUT = 'TEST' ]]; then
		echo -e "> Strictly Syncing JARs from your localmachine to $TEST"
		rsync -rhP --delete-before tmp-lib/*.jar --exclude 'tmp-lib/lucene-*-4.8.0.jar' --exclude 'tmp-lib/elasticsearch-1.2.1.jar' winterwell@$TEST:/home/winterwell/lg.good-loop.com/lib/
		echo -e "> Strictly Syncing config from YOUR localmachine to $TEST"
		rsync -rhP --delete-before config/*.properties winterwell@$TEST:/home/winterwell/lg.good-loop.com/config/
	fi
	if [[ $TYPEOFPUSHOUT = 'PRODUCTION' ]]; then
		echo -e "> Strictly Syncing JARs from YOUR localmachine to $PRODUCTIONPUBLISHER"
		rsync -rhP --delete-before tmp-lib/*.jar --exclude 'tmp-lib/lucene-*-4.8.0.jar' --exclude 'tmp-lib/elasticsearch-1.2.1.jar' winterwell@$PRODUCTIONPUBLISHER:/home/winterwell/lg.good-loop.com/lib/
		echo -e "> Strictly Syncing config from YOUR localmachine to $PRODUCTIONPUBLISHER"
		rsync -rhP --delete-before config/*.properties winterwell@$PRODUCTIONPUBLISHER:/home/winterwell/lg.good-loop.com/config/
		scp cluster-sync.sh winterwell@$PRODUCTIONPUBLISHER:/home/winterwell/lg.good-loop.com/
		echo -e "> Sending list of targets to $PRODUCTIONPUBLISHER..."
		scp /tmp/target.list.txt winterwell@$PRODUCTIONPUBLISHER:/tmp/
		echo -e "> Telling $PRODUCTIONPUBLISHER to update the backend..."
		ssh winterwell@$PRODUCTIONPUBLISHER 'bash /home/winterwell/lg.good-loop.com/cluster-sync.sh backend'
		ssh winterwell@$PRODUCTIONPUBLISHER 'sudo service lg restart'
	fi
	echo ""
	echo -e "> Restarting the lg process on target(s)"
	parallel-ssh -h /tmp/target.list.txt --user=winterwell 'sudo service lg restart'
	echo ""
	echo -e "backend(s) have been updated for..."
	if [[ $TYPEOFPUSHOUT = 'PRODUCTION' ]]; then
		echo -e "$PRODUCTIONPUBLISHER and..."
	fi
	cat /tmp/target.list.txt
	echo ""
}
####
#END OF SETTING THE FUNCTIONS FOR THE LEVEL OF PUBLISH
####

####
#SETTING THE 'CLEANPUBLISH' FUNCTION
####
function clean_publish {
	if [[ $PUBLISHLEVEL != 'everything' ]]; then
		echo -e "Silly goose! you cannot perform a 'clean-out' unless you are publishing both the frontend AND the backend"
		echo -e "If you really want to 'clean' for this publish, please use the 'everything' argument for the publishlevel"
		exit 3
	fi
	echo -e "***WARNING***"
	echo -e "This Script will now clean/clear out all files/directories before syncing"
	echo -e "You now have 5 seconds to interupt this process if you do not want this to happen"
	sleep 1
	CURRENTCOUNT='5'
	while  [[ $CURRENTCOUNT != '1' ]]; do
		echo -e "$CURRENTCOUNT"
		sleep 1
		CURRENTCOUNT=$((CURRENTCOUNT-1))
	done
	ssh winterwell@${TARGET[0]} 'rm -rf /home/winterwell/lg.good-loop.com/lib/*'
	ssh winterwell@${TARGET[0]} 'rm -rf /home/winterwell/lg.good-loop.com/web/*'
	ssh winterwell@${TARGET[0]} 'rm -rf /home/winterwell/lg.good-loop.com/config/*'
	echo -e "Cleaning of the lg.good-loop.com directory is now complete for all targets."
}
####
#END OF SETTING THE 'CLEANPUBLISH'S' FUNCTION
####



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
		if [[ $CLEANPUBLISH = 'true' ]]; then
			clean_publish
		fi
		backend_publish
		frontend_publish
	;;
esac
