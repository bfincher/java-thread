def performRelease = false
def gradleOpts = '--no-daemon --info -s -PlocalNexus=http://nexus:8081/nexus/content//groups/public -PpublishUsername=upload -PpublishPassword=upload -PpublishSnapshotUrl=http://nexus:8081/nexus/content/repositories/snapshots -PpublishReleaseUrl=http://nexus:8081/nexus/content/repositories/releases'

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
	}

	
	stages {
		stage('Prepare') {
            steps {
                script {
                   if (!params.release.isEmpty()) {
                       performRelease = true
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
