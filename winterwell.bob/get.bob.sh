#!/bin/bash


if [[ -f bob-all.jar ]]; then
	rm bob-all.jar
fi

printf "\nDownloading bob-all.jar from the winterwell.com software repo (https://www.winterwell.com/software/downloads)\n"
wget -cO - "https://www.winterwell.com/software/downloads/bob-all.jar" >> bob-all.jar
