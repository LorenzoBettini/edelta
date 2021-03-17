node {
   def mavenProfiles = ""
   def mavenArguments = "clean verify"
   def hasToDeploye = false
   def ideTests = false
   def mavenOnlyProfile = "-P!development"
   def isSnapshot = false
   if (env.JOB_NAME.endsWith("release-site")) {
     mavenProfiles = "-Prelease-composite"
     hasToDeploye = true
   } else if (env.JOB_NAME.endsWith("release-snapshot")) {
     mavenProfiles = "-Prelease-composite,release-snapshots"
     hasToDeploye = true
     isSnapshot = true
   } else if (env.JOB_NAME.endsWith("release")) {
     mavenProfiles = "-Prelease-composite,release-ide"
     hasToDeploye = true
   } else {
     mavenProfiles = "-Pjacoco,test-ide"
     ideTests = true
   }
   properties([
     [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '30']]
   ])
   stage('Preparation') { // for display purposes
      checkout scm
   }
   if (!hasToDeploye) {
      stage('Build and Test') {
         wrap([$class: 'Xvfb', autoDisplayName: true]) {
           if (ideTests) {
             sh "mutter --replace --sm-disable 2> mutter.err &"
           }
           // Run the maven build
           // don't make the build fail in case of test failures...
           sh (script:
             "./mvnw -f edelta.parent/pom.xml -Dmaven.test.failure.ignore=true -fae ${mavenProfiles} ${mavenArguments}",
           )
         }
      }
      stage('JUnit Results') {
         // ... JUnit archiver will set the build as UNSTABLE in case of test failures
         junit '**/target/surefire-reports/TEST-*.xml'
         archive '**/target/repository/'
         publishHTML(target: [
           allowMissing: false,
           alwaysLinkToLastBuild: true,
           keepAll: true,
           reportDir: 'edelta.parent/edelta.tests.report/target/site/jacoco-aggregate',
           reportFiles: 'index.html',
           reportName: 'Jacoco HTML Report'
         ])
      }
      stage('Code Coverage') {
        step([$class: 'JacocoPublisher',
          // disabled for https://issues.jenkins-ci.org/browse/JENKINS-43225
          // changeBuildStatus: true,
          minimumClassCoverage: '90',
          minimumInstructionCoverage: '90',
          minimumLineCoverage: '90',
          maximumClassCoverage: '100',
          maximumInstructionCoverage: '100',
          maximumLineCoverage: '100',
          execPattern: '**/**.exec',
          sourcePattern: '**/edelta/src,**/edelta.ui/src,**/edelta.lib/src',
          classPattern: '**/edelta/**/classes,**/edelta.lib/**/classes,**/edelta.ui/**/classes',
          exclusionPattern: '**/*Test*.class,**/edelta/edelta/**/*.class,**/antlr/**/*.class,**/serializer/*.class,**/*Abstract*RuntimeModule.class,**/*StandaloneSetup*.class,**/*Abstract*Validator.class,**/*GrammarAccess*.class,**/*Abstract*UiModule.class,**/**ExecutableExtensionFactory.class,**/*Abstract*ProposalProvider.class,**/internal/*.class,**/*ProjectTemplate.class'])
      }
   } else {
      stage('Build and Deploy P2 Artifacts') {
         sh (script:
           "./mvnw -f edelta.parent/pom.xml ${mavenProfiles} ${mavenArguments}",
         )
      }
      if (!isSnapshot) {
         stage('Remove SNAPSHOT') {
            // Since edelta.parent has edelta.bom as parent, but the aggregator
            // is edelta.parent, the versions:set must be run only on the BOM
            // (to avoid the error "Project version is inherited from parent")
            // this will remove the -SNAPSHOT.
            // Then we run tycho-versions-plugin on the edelta.parent (the aggregator)
            // to remove the .qualifier
            sh (script:
              "./mvnw -f edelta.parent/edelta.bom/pom.xml ${mavenOnlyProfile} \
                    versions:set -DgenerateBackupPoms=false -DremoveSnapshot=true \
               && \
               ./mvnw -f edelta.parent/pom.xml ${mavenOnlyProfile} \
                    org.eclipse.tycho:tycho-versions-plugin:update-eclipse-metadata",
            )
         }
      }
      stage('Build and Deploy Maven Artifacts') {
         sh (script:
           "./mvnw -f edelta.parent/pom.xml -Dmaven.repo.local='${env.WORKSPACE}'/.repository ${mavenOnlyProfile} -Psonatype-oss-release clean deploy",
         )
      }
   }
}
