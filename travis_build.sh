#!/bin/bash

set -ev
if [ "$TRAVIS_OS_NAME" == "osx" ]; then
	if [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then
		echo "Build on MacOSX: Pull Request"
		mvn -f edelta.parent/pom.xml clean verify -Dtycho.disableP2Mirrors=true
	else
		echo "Skipping build on MacOSX for standard commit"
	fi
else
	echo "Build on Linux"
	mvn -f edelta.parent/pom.xml clean verify $ADDITIONAL -Dtycho.disableP2Mirrors=true
fi 

#  -Dtycho.disableP2Mirrors=true
