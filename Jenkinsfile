node {
    stage('Debug') {
        sh """
        pwd
        ls -l
        """
    }
    stage('Compile') {
        sh "./gradlew clean classes"
    }
    stage('Unit test') {
        sh "./gradlew test"
    }
    stage('Integration test') {
        sh './gradlew integrationTest'
    }
    stage('Frontend test') {
        sh './gradlew frontendTest'
    }
    stage('System test') {
        sh './gradlew systemTest'
    }
    stage('Assemble') {
        sh './gradlew assemble'
    }
}
