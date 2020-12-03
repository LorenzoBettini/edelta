#!/bin/sh

./mvnw -f edelta.parent/pom.xml -P!development -Psonatype-oss-release clean deploy

