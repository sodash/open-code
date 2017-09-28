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


#########WRITE FUNCTIONS FOR FRONTEND,BACKEND,AND EVERYTHING PUBLISHES



echo -e "> ${HOSTNAME} says: 'Cleaning Cluster...'"
for server in ${TARGETS[*]}; do
	ssh winterwell@$server 'rm -rf /home/winterwell/lg.good-loop.com/lib/*.jar'
	ssh winterwell@$server 'rm -rf /home/winterwell/lg.good-loop.com/web'
	ssh winterwell@$server 'rm -rf /home/winterwell/lg.good-loop.com/package.json'
	ssh winterwell@$server 'rm -rf /home/winterwell/lg.good-loop.com/webpack.config.json'
	ssh winterwell@$server 'rm -rf /home/winterwell/lg.good-loop.com/src-js'
done

echo -e "> ${HOSTNAME} says: 'Syncing Directories and Files..."
parallel-rsync -h /tmp/target.list.txt --user=winterwell --recursive /home/winterwell/lg.good-loop.com/lib/ /home/winterwell/lg.good-loop.com/lib/
parallel-rsync -h /tmp/target.list.txt --user=winterwell --recursive /home/winterwell/lg.good-loop.com/web /home/winterwell/lg.good-loop.com/
parallel-rsync -h /tmp/target.list.txt --user=winterwell --recursive /home/winterwell/lg.good-loop.com/package.json /home/winterwell/lg.good-loop.com/
parallel-rsync -h /tmp/target.list.txt --user=winterwell --recursive /home/winterwell/lg.good-loop.com/webpack.config.json /home/winterwell/lg.good-loop.com/
parallel-rsync -h /tmp/target.list.txt --user=winterwell --recursive /home/winterwell/lg.good-loop.com/src-js /home/winterwell/lg.good-loop.com/