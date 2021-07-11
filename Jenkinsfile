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
                        -Dsonar.login=3ae1d2fcd4be3b3081d66f33b74cf9298279c721
			"""
		    }
		}
	}
}
