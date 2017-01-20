node {
    stage('Prepare') {
        checkout scm
        sh """
        printenv
        # rm -fr ./.gradle
        # find product -name build -type d | grep -v node_modules | xargs rm -fr
        """
    }

    stage('Compile') {
        sh "./gradlew classes"
    }

    stage('Backend unit test') {
        sh "./gradlew test"
    }

    stage('Frontend unit test') {
        sh './gradlew frontendTest'
    }

    stage('Integration test') {
        sh './gradlew integrationTest'
    }

    stage('System test') {
        sh './gradlew systemTest'
    }

    stage('Assemble') {
        sh './gradlew assemble'
    }
}
