pipeline {
  agent any
  stages {
    stage('pull') {
      agent any
      steps {
        git(url: 'https://github.com/sakthisarans/GPS_Tracker_Gateway.git', branch: 'main')
      }
    }

    stage('echo') {
      steps {
        echo 'done'
      }
    }

  }
}