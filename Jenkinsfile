pipeline {

	agent any
	
	stages {
		stage('Build') {
			steps {
				sh 'gradle --no-daemon clean build'
			}
		}
		
		stage('Checkstyle') {
		    steps {
		        sh 'gradle --no-daemon checkstyleMain checkstyleTest'
		    }
		}
		
		stage('Publish') {
		    steps {
		        sh 'gradle --no-daemon publishToMavenLocal'
		    }
		}
	}
}