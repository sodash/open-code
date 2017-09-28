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
PRODUCTION=(gl-es-03.soda.sh gl-es-04.soda.sh gl-es-05.soda.sh)
TEST='hugh.soda.sh'
####
#END OF TARGET ARRAYS
####

####
#Is this a production pushout or a test pushout?
####
TYPEOFPUSHOUT=$1
CLEANPUBLISH=$2


####
#ACCEPTING AND PROCESSING THE ARGUMENTS
####
case $1 in
	production|PRODUCTION)
	echo "this is a PRODUCTION pushout"
	TARGET=${PRODUCTION[@]}
	;;
	test|TEST)
	echo "this is a TEST pushout"
	TARGET=$TEST
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
	build-less.sh
	echo ""
	echo -e "> Syncing the 'web' directory..."
	parallel-rsync -h /tmp/target.list.txt --user=winterwell --recursive web /home/winterwell/lg.good-loop.com/
	echo ""
	echo -e "> Syncing the 'package.json' file..."
	parallel-rsync -h /tmp/target.list.txt --user=winterwell package.json /home/winterwell/lg.good-loop.com/
	echo ""
	echo -e "> Syncing the 'webpack.config.js' file..."
	parallel-rsync -h /tmp/target.list.txt --user=winterwell webpack.config.js /home/winterwell/lg.good-loop.com/
	echo ""
	echo -e "> Syncing the 'src-js' directory..."
	parallel-rsync -h /tmp/target.list.txt --user=winterwell --recursive src-js /home/winterwell/lg.good-loop.com/
	echo ""
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
	echo -e "> Syncing fresh JARs..."
	rm tmp-lib/elasticsearch-1*.jar
    echo "parallel-rsync -h /tmp/target.list.txt --user=winterwell --recursive tmp-lib/ /home/winterwell/lg.good-loop.com/lib/"
	parallel-rsync -h /tmp/target.list.txt --user=winterwell --recursive tmp-lib/ /home/winterwell/lg.good-loop.com/lib/
	echo ""
	echo -e "> Restarting the lg process on target(s)"
	parallel-ssh -h /tmp/target.list.txt --user=winterwell 'sudo service lg restart'
	echo ""
	echo -e "backend(s) have been updated for..."
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
	parallel-ssh -h /tmp/target.list.txt --user=winterwell 'rm -rf /home/winterwell/lg.good-loop.com/lib/*'
	parallel-ssh -h /tmp/target.list.txt --user=winterwell 'rm -rf /home/winterwell/lg.good-loop.com/web/*'
	parallel-ssh -h /tmp/target.list.txt --user=winterwell 'rm -rf /home/winterwell/lg.good-loop.com/config/*'
	echo -e "Cleaning of the lg.good-loop.com directory is now complete for all targets."
}
####
#END OF SETTING THE 'CLEANPUBLISH'S' FUNCTION
####


#function force_jar_parity {
	#if $TYPEOFPUSHOUT==PRODUCTION, rsync --delete command to gl-es-03, then tell gl-es-03 to perform an 'rsync --delete' sync to rest of cluster.
#}








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
