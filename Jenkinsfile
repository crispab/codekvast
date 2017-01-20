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

    stage('Unit test') {
        sh "./gradlew test"
    }

    stage('Integration test') {
        sh './gradlew integrationTest'
    }

    stage('System test') {
        sh './gradlew systemTest'
    }

    stage('Assemble') {
        sh './gradlew check assemble'
    }
}
