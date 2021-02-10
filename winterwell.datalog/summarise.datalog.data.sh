#!/bin/bash

######
# A bash wrapper for a java process which summarises (or compresses) datalog indices
####

USAGE="\n\n./summarise.datalog.data.sh full-name-of-target-index\n\n"

####
# Check if name of index was passed in
###
if [[ $# -eq 0 ]]; then
	printf $USAGE
	exit 0
fi

####
# Check if human user has already build jars
# and then process answer along with intended action
####
printf "\n\tHave you already built this project using 'bob'?\n\n\tyes/no\n"
read answer
ANSWER=$answer
case $ANSWER in
	yes|YES)
		printf "\n\n\tContinuing to launch JVM process to summarise $1\n"
		sleep 2
		java -cp build-lib/datalog.compressor.jar:build-lib/* com.winterwell.datalog.server.CompressDataLogIndexMain $1
	;;
	no|NO)
		printf "\n\n\tAttempting to launch 'bob' to build this project\n"
		bob
		printf "\n\n\tIf bob built your project successfully, please re-run this script.\n\n\tIf bob was unsucessful, you'll need to fix underlying issues first, then re-'bob', and then re-run this script\n"
	;;
	*)
		printf "\n\n\tI couldn't understand your answer.  Exiting\n."
		exit 0
	;;
esac
