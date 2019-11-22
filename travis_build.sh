#!/bin/bash

set -ev
if [ "$TRAVIS_OS_NAME" == "osx" ]; then
	if [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then
		echo "Build on MacOSX: Pull Request"
		mvn -f edelta.parent/pom.xml clean verify $ADDITIONAL
	else
		echo "Skipping build on MacOSX for standard commit"
	fi
else
	echo "Build on Linux"
	mvn -f edelta.parent/pom.xml clean verify $ADDITIONAL
fi 

#  -Dtycho.disableP2Mirrors=true
