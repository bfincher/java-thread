pipeline {

	agent any
	
	stages {
		stage('Build') {
			steps {
				sh 'gradle --no-daemon clean build -x checkstyleMain -x checkstyleTest'
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

		stage('Sonarqube') {
		    steps {
			sh """
		        gradle sonarqube \
			-Dsonar.projectKey=java-thread \
		  	-Dsonar.host.url=http://192.168.1.2:9000 \
			-Dsonar.login=c52f06414e05db27d93a294d7ee60c601d2675b0
			"""
		    }
		}
	}
}
