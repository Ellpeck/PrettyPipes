pipeline {
  agent any
  stages {
    stage('Clean') {
      steps {
        sh 'chmod +x ./gradlew'
        sh './gradlew clean --no-daemon'
      }
    }

    stage('Build') {
      steps {
        sh './gradlew build --no-daemon'
      }
    }

    stage('Upload Artifacts') {
      steps {
        archiveArtifacts 'build/libs/**.jar'
      }
    }

    stage('Publish') {
      when {
        branch 'master'
      }
      steps {
        sh './gradlew publish --no-daemon'
      }
    }
  }
  environment {
    local_maven = '/var/www/maven'
  }
}
