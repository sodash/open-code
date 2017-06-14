#!/bin/bash

# restart the lg process

TARGETSERVER=$1

case $TARGETSERVER in
	lg.good-loop.com)
		echo -e "Telling $TARGETSERVER to restart the lg service"
		ssh -o StrictHostKeyChecking=no winterwell@$TARGETSERVER 'sudo service lg restart'
		echo -e "lg service has been restarted"
	;;
	testlg.good-loop.com)
		echo -e "Telling $TARGETSERVER to restart the lg service"
		ssh -o StrictHostKeyChecking=no winterwell@$TARGETSERVER 'service lg restart'
		echo -e "lg service has been restarted"
	;;
	*)
		echo -e "I don't know who or what $TARGETSERVER is, so I'm unable to tell $TARGETSERVER to restart the lg service"
		exit 1
	;;
esac