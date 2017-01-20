node {
    stage('Prepare') {
        checkout scm
        sh """
        printenv | sort
        rm -fr ./.gradle
        find product -name build -type d | grep -v node_modules | xargs rm -fr
        """
    }

    stage('Compile') {
        sh "./gradlew classes"
    }

    stage('Unit test') {
        sh "./gradlew test"
        junit '**/build/test-results/test/*.xml'
    }

    stage('Integration test') {
        sh './gradlew integrationTest'
        junit '**/build/integrationTest-results/*.xml'
    }

    stage('System test') {
        sh './gradlew systemTest'
        junit '**/build/systemTest-results/*.xml'
    }

    stage('Assemble') {
        sh './gradlew check assemble'
    }
}
