pipeline {

	agent any
	
	stages {
		stage('Build') {
			steps {
				sh './gradlew --no-daemon clean build -x checkstyleMain -x checkstyleTest'
			}
		}
		
		stage('Checkstyle') {
		    steps {
		        sh './gradlew --no-daemon checkstyleMain checkstyleTest'
		    }
		}
		
		stage('Publish') {
		    steps {
		        sh './gradlew --no-daemon publish'
		    }
		}

		stage('Sonarqube') {
		    steps {
			sh """
		        ./gradlew sonarqube \
			-Dsonar.projectKey=java-thread \
		  	-Dsonar.host.url=http://sonarqube:9000 \
			-Dsonar.login=c52f06414e05db27d93a294d7ee60c601d2675b0
			"""
		    }
		}
	}
}
