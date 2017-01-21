node {
    concurrent: false

    timestamps {
        stage('Prepare') {
            checkout scm
            sh """
            printenv | sort
            rm -fr ./.gradle
            find product -name build -type d | grep -v node_modules | xargs rm -fr
            """
        }

        stage('Compile Java') {
            sh "./gradlew classes"
        }

        stage('Java unit test') {
            sh "./gradlew test"
            junit '**/build/test-results/test/*.xml'
        }

        stage('JavaScript unit test') {
            sh "./gradlew --no-daemon :product:warehouse:frontendTest"
            // TODO: publish Jasmine report
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
}
