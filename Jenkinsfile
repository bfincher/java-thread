def performRelease = false
def gradleOpts = '--no-daemon --info -s'

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

		stage('Sonarqube') {
		    steps {
			sh """
		        ./gradlew sonarqube \
			-Dsonar.projectKey=java-thread \
		  	-Dsonar.host.url=http://sonarqube:9000 \
			-Dsonar.login=7afd7c7c928867ca548ea2324cff59e492e50266 \
                        gradleOpts
			"""
		    }
		}
	}
}
