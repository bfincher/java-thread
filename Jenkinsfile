def performRelease = false

properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')), 
disableConcurrentBuilds(), pipelineTriggers([[$class: 'PeriodicFolderTrigger', interval: '1d']])])

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
				sh 'gradle --no-daemon clean build checkstyleMain checkstyleTest'
			}
		}

		stage('Sonarqube') {
			when { expression { params.runSonarqube }}

		    steps {
			sh """
		        gradle --no-daemon sonarqube \
			-Dsonar.projectKey=java-thread \
		  	-Dsonar.host.url=http://192.168.1.2:9000 \
			-Dsonar.login=c52f06414e05db27d93a294d7ee60c601d2675b0
			"""
		    }
		}

		stage('Release') {
		    when { expression { performRelease } }
		    steps {
		        sh "gradle release -Prelease.releaseVersion=${params.release} -Prelease.newVersion=${params.release}-SNAPSHOT"
		    }
		}
		
		stage('Publish') {
		    steps {
		        sh 'gradle --no-daemon publish'
		    }
		}
	}
}
