node {
   def mavenProfiles = ""
   def mavenArguments = "clean verify"
   def mavenDeploy = false
   def ideTests = false
   if (env.JOB_NAME.endsWith("release-site")) {
     mavenProfiles = "-Prelease-composite"
     mavenDeploy = true
   } else if (env.JOB_NAME.endsWith("release")) {
     mavenProfiles = "-Pbuild-ide,release-ide-composite,deploy-ide-composite"
     mavenDeploy = true
   } else {
     mavenProfiles = "-Pjacoco,build-ide,test-ide"
     ideTests = true
   }
   properties([
     [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '30']]
   ])
   stage('Preparation') { // for display purposes
      checkout scm
   }
   if (!mavenDeploy) {
     // temporary experiment
     stage('Remove SNAPSHOT') {
        sh (script:
          "./mvnw -f edelta.parent/pom.xml \
                  versions:set -DgenerateBackupPoms=false -DremoveSnapshot=true \
                  org.eclipse.tycho:tycho-versions-plugin:update-eclipse-metadata",
        )
     }
   }
   stage('Build') {
      wrap([$class: 'Xvfb', autoDisplayName: true]) {
        if (ideTests) {
          sh "mutter --replace --sm-disable 2> mutter.err &"
        }
        // Run the maven build
        // returnStatus: true here will ensure the build stays yellow
        // when test cases are failing
        sh (script:
          "./mvnw -f edelta.parent/pom.xml -fae ${mavenProfiles} ${mavenArguments}",
          returnStatus: true
        )
      }
   }
   if (!mavenDeploy) {
      stage('JUnit Results') {
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
          exclusionPattern: '**/*Test*.class,**/edelta/edelta/**/*.class,**/antlr/**/*.class,**/serializer/*.class,**/*Abstract*RuntimeModule.class,**/*StandaloneSetup*.class,**/*Abstract*Validator.class,**/*GrammarAccess*.class,**/*Abstract*UiModule.class,**/**ExecutableExtensionFactory.class,**/*Abstract*ProposalProvider.class,**/internal/*.class,**/*NewProjectWizard.class,**/*ProjectCreator.class'])
      }
   }

}
