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
                        -Dsonar.login=227a800ab5d6ea59657d22e7880f6e355f96b3c1
			"""
		    }
		}
	}
}
