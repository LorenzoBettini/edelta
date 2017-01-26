node {
   def mvnHome
   stage('Preparation') { // for display purposes
      // Get some code from a GitHub repository
      git 'git@bitbucket.org:lbettini/edelta.git'
      // Get the Maven tool.
      // ** NOTE: This 'M3' Maven tool must be configured
      // **       in the global configuration.           
      mvnHome = tool 'M3'
   }
   stage('Build') {
      wrap([$class: 'Xvfb', displayNameOffset: 100]) {
        // Run the maven build
        sh "'${mvnHome}/bin/mvn' -f edelta.parent/pom.xml clean verify"
      }
   }
   stage('Results') {
      junit '**/target/surefire-reports/TEST-*.xml'
      archive '**/target/repository/'
   }
}
