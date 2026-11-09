When you want to use SNAPSHOTs of `xtext-build-utils` (e.g., `xtext-tycho-parent`), add these lines to the POM

```xml
  <repositories>
    <repository>
      <id>sonatype-snapshots</id>
      <url>https://central.sonatype.com/repository/maven-snapshots/</url>
      <releases><enabled>false</enabled></releases>
      <snapshots><enabled>true</enabled></snapshots>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>sonatype-snapshots</id>
      <url>https://central.sonatype.com/repository/maven-snapshots/</url>
      <releases><enabled>false</enabled></releases>
      <snapshots><enabled>true</enabled></snapshots>
    </pluginRepository>
  </pluginRepositories>
```