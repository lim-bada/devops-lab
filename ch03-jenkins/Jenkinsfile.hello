// 3장 첫 Pipeline — 무엇이 어떤 순서로 도는지만 본다.
// Jenkins UI 의 Pipeline script 칸에 붙여 넣거나, repository 루트에 Jenkinsfile 로 둔다.
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo 'building the application...'
                sh 'echo "build start"'
            }
        }
        stage('Test') {
            steps {
                echo 'testing the application...'
                sh 'echo "test start"'
            }
        }
        stage('Deploy') {
            steps {
                echo 'deploying the application...'
                sh 'echo "deploy start"'
            }
        }
    }

    post {
        success { echo '모든 stage 성공' }
        failure { echo '실패한 stage 의 Console Output 을 확인한다' }
    }
}
