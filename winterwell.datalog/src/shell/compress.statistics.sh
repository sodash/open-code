#!/bin/bash

if [ $# -eq 0 ]; then
    printf "\nNo month and year specified\nexiting...\n"
    exit 1
fi

cd ../
java -cp build-lib/winterwell.datalog.jar:build-lib/* com.winterwell.datalog.server.CompressDataLogIndexMain $1
