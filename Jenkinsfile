def performRelease = false
def gradleOpts = "--info -s --build-cache -PlocalNexus=https://nexus.fincherhome.com/nexus/content/groups/public"

properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')), 
disableConcurrentBuilds(), pipelineTriggers([[$class: 'PeriodicFolderTrigger', interval: '1d']])])

node {
  publishHTML([allowMissing: false, 
      alwaysLinkToLastBuild: false, 
      keepAll: true, 
      reportDir: 'build/reports/tests/test', 
      reportFiles: 'index.html', 
      reportName: 'JUNIT HTML Report', 
      reportTitles: ''])
}

pipeline {
  agent any

  parameters {
      booleanParam(name: 'runSonarqube', defaultValue: false, description: 'Run SonarQube')
      string(defaultValue: '', description: 'Extra Gradle Options', name: 'extraGradleOpts')
      booleanParam(name: 'majorRelease', defaultValue: false, description: 'Perform a major release')
      booleanParam(name: 'minorRelease', defaultValue: false, description: 'Perform a minor release')
      booleanParam(name: 'patchRelease', defaultValue: false, description: 'Perform a patch release')
  }

  tools {
    jdk 'jdk17'
  }

  stages {
    stage('Prepare') {
      steps { script {
        if (!params.release.isEmpty()) {
          performRelease = true
        }                           
        if (!params.extraGradleOpts.isEmpty()) {
          gradleOpts = gradleOpts + " " + extraGradleOpts
        }
    }}
  }
		
    stage('Build') {
      steps {
        sh './gradlew clean build checkstyleMain ' + gradleOpts
      }
    }

    stage('Release') {
      when { expression { performRelease } }
      steps { script { 
        if (performRelease) {
          withCredentials([sshUserPrivateKey(credentialsId: "bfincher_git_private_key", keyFileVariable: 'keyfile')]) {
            sh 'echo keyfile = ${keyfile}'
            sh './gradlew finalizeRelease -PsshKeyFile=${keyfile} ' + gradleOpts
          }
         
          withCredentials([usernamePassword(credentialsId: 'nexus.fincherhome.com', usernameVariable: 'publishUsername', passwordVariable: 'publishPassword')]) {
            sh './gradlew publish -PpublishUsername=${publishUsername} -PpublishPassword=${publishPassword} ' + gradleOpts
          }
        }
      } }
    }
  }
}
