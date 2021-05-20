./mvnw -f edelta.parent/edelta.bom/pom.xml \
    '-P!development' \
	versions:set \
	-DgenerateBackupPoms=false \
	-DremoveSnapshot=true \
&& \
./mvnw -f edelta.parent/pom.xml '-P!development' \
    org.eclipse.tycho:tycho-versions-plugin:update-eclipse-metadata
