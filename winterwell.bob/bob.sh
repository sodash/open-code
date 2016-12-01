#!/bin/bash
# Quick n dirty script to run Bob

if [ -z $WINTERWELL_HOME ]; then
    echo "WINTERWELL_HOME is unset"
    exit 1
fi

BOB_DIR=$WINTERWELL_HOME/code/winterwell.bob

SCRIPTNAME=$2
BOB_CLASSPATH=$1
if [ $# -ne 2 ]; then
	echo "Bob the Builder"
    echo "Usage: CLASSPATH SCRIPTNAME"
    echo "E.g. bob.sh bin:../winterwell.utils/winterwell.utils.jar jobs.BuildUtils"
    exit 1
fi

#echo "SCRIPTNAME: $SCRIPTNAME"
#echo "BOB_CLASSPATH: $BOB_CLASSPATH"

CLASSPATH="$BOB_CLASSPATH:$BOB_DIR/bob.jar:."

echo java -ea -classpath $CLASSPATH winterwell.bob.Bob $SCRIPTNAME
java -ea -classpath $CLASSPATH winterwell.bob.Bob $SCRIPTNAME
