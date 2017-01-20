node {
    stage('Prepare') {
        checkout scm
        sh """
        rm -fr ./.gradle
        ./gradlew --no-daemon clean
        """
    }
    stage('Compile') {
        sh "./gradlew --no-daemon classes"
    }
    stage('Unit test') {
        sh "./gradlew --no-daemon test"
    }
    stage('Integration test') {
        sh './gradlew --no-daemon integrationTest'
    }
    stage('Frontend test') {
        sh './gradlew --no-daemon frontendTest'
    }
    stage('System test') {
        sh './gradlew --no-daemon systemTest'
    }
    stage('Assemble') {
        sh './gradlew --no-daemon assemble'
    }
}
