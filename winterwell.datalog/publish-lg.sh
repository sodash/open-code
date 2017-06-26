#!/bin/bash

# Publish-lg.sh  a script to publish files to the LG production or test server(s)

PRODUCTION=(gl-es-03.soda.sh gl-es-04.soda.sh gl-es-05.soda.sh)
TEST='hugh.soda.sh'
LOCALPUBLISH='localmachine'
SSHPARAMS='-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null'

##Is this a production pushout or a test pushout?
TYPEOFPUSHOUT=$1
CLEANPUBLISH=$2







case $1 in
	production)
	echo "this is a PRODUCTION pushout"
	TARGET=${PRODUCTION[*]}
	;;
	PRODUCTION)
	echo "this is a PRODUCTION pushout"
	TARGET=${PRODUCTION[*]}
	;;
	test)
	echo "this is a TEST pushout"
	TARGET=$TEST
	;;
	TEST)
	echo "this is a TEST pushout"
	TARGET=$TEST
	;;
	*)
	echo "The script couldn't discern if this was a production or a test pushout.  EXITING..."
	exit 1
	;;
esac

case $2 in
	frontend)
		echo "This publishing process will only sync the FRONTEND files, and will not sync backend files"
		PUBLISHLEVEL='frontend'
	;;
	FRONTEND)
		echo "This publishing process will only sync the FRONTEND files, and will not sync backend files"
		PUBLISHLEVEL='frontend'
	;;
	backend)
		echo "This publishing process will only sync the BACKEND files, and will not sync the frontend files"
		PUBLISHLEVEL='backend'
	;;
	BACKEND)
		echo "This publishing process will only sync the BACKEND files, and will not sync the frontend files"
		PUBLISHLEVEL='backend'
	;;
	everything)
		echo "This publishing process will publish BOTH the FRONTEND and BACKEND files"
		PUBLISHLEVEL='everything'
	;;
	EVERYTHING)
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
	clean)
		echo "this publishing process is going to clear out the target server's directories before syncing"
		CLEANPUBLISH='true'
	;;
	CLEAN)
		echo "this publishing process is going to clear out the target server's directories before syncing"
		CLEANPUBLISH='true'
	;;
	*)
		echo "this publishing process will only overwrite old files with new versions, all other files will not be changed"
		CLEANPUBLISH='false'
	;;
esac


for server in ${TARGET[*]}; do
	if [[ $CLEANPUBLISH = 'true' ]]; then
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
		ssh $SSHPARAMS winterwell@$server 'rm -rf /home/winterwell/lg.good-loop.com/lib/*'
		ssh $SSHPARAMS winterwell@$server 'rm -rf /home/winterwell/lg.good-loop.com/web/*'
		ssh $SSHPARAMS winterwell@$server 'rm -rf /home/winterwell/lg.good-loop.com/config/*'
		echo -e "Cleaning of $server 's lg.good-loop.com directory is now complete."
	fi


	function frontend_publish {
		echo -e "Converting LESS files to CSS..."
		for file in web/style/*.less; do
			if [ -e "$file" ]; then
				lessc "$file" "${file%.less}.css"
			else
				echo "no less files found"
			exit 2
			fi
		done
		rsync -rhP web winterwell@$server:/home/winterwell/lg.good-loop.com/
		rsync -rhP package.json winterwell@$server:/home/winterwell/lg.good-loop.com/
		rsync -rhP webpack.config.js winterwell@$server:/home/winterwell/lg.good-loop.com/
		rsync -rhP src-js winterwell@$server:/home/winterwell/lg.good-loop.com/
		echo -e "Satisfying NPM dependencies..."
		ssh winterwell@$server 'cd /home/winterwell/lg.good-loop.com && npm i'
		echo -e "Webpacking..."
		ssh winterwell@$server 'cd /home/winterwell/lg.good-loop.com && webpack -p'
		echo -e "$server 's frontend has been updated and published"
	}

	function backend_publish {
		rsync -rhP tmp-lib/* winterwell@$server:/home/winterwell/lg.good-loop.com/lib/
		echo -e "Restarting the lg process on $server..."
		if [[ ${TARGET[*]} = 'hugh.soda.sh' ]]; then
			ssh winterwell@$server 'service lg restart'
		else
			ssh winterwell@$server 'sudo service lg restart'
		fi
	}

	case $PUBLISHLEVEL in
		frontend)
			frontend_publish
			exit 2
		;;
		FRONTEND)
			frontend_publish
			exit 2
		;;
		backend)
			backend_publish
			exit 2
		;;
		BACKEND)
			backend_publish
			exit 2
		;;
		everything)
			frontend_publish
			backend_publish
			exit 2
		;;
		EVERYTHING)
			frontend_publish
			backend_publish
			exit 2
		;;
	esac
done