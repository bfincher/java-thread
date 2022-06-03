def performRelease = false
def gradleOpts = '--no-daemon --info -s -PlocalNexus=https://www.fincherhome.com/nexus/content/groups/public -PpublishUsername=upload -PpublishPassword=upload -PpublishSnapshotUrl=https://www.fincherhome.com/nexus/nexus/content/repositories/snapshots -PpublishReleaseUrl=https://www.fincherhome.com/nexus/nexus/content/repositories/releases'

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
        string(defaultValue: '', description: 'Perform a release with the given version', name: 'release')
        booleanParam(name: 'runSonarqube', defaultValue: true, description: 'Run SonarQube')
        string(defaultValue: '', description: 'Extra Gradle Options', name: 'extraGradleOpts')
    }

    tools {
        jdk 'jdk11'
    }

    stages {
        stage('Prepare') {
            steps {
                script {
                   if (!params.release.isEmpty()) {
                       performRelease = true
                   }                           
		   if (!params.extraGradleOpts.isEmpty()) {
		       gradleOpts = gradleOpts + extraGradleOpts
		   }
               }
            }
        }
		
        stage('Build') {
            steps {
                sh './gradlew clean build checkstyleMain ' + gradleOpts
            }
        }

        stage('Sonarqube') {
            when { expression { params.runSonarqube }}

            steps {
                sh """
                ./gradlew sonarqube \
                -Dsonar.projectKey=java-thread \
                -Dsonar.host.url=http://192.168.1.2:9000 \
                -Dsonar.login=3ae1d2fcd4be3b3081d66f33b74cf9298279c721 \
                $gradleOpts
                """
            }
        }

        stage('Release') {
            when { expression { performRelease } }
            steps {
                sh "./gradlew release -Prelease.releaseVersion=${params.release} -Prelease.newVersion=${params.release}-SNAPSHOT " + gradleOpts
            }
        }
		
        stage('Publish') {
            steps {
                sh './gradlew publish ' + gradleOpts
            }
        }

    }
}
