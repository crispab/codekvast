node {
    concurrent: false

    timestamps {
        stage('Prepare') {
            checkout scm
            sh """
            printenv | sort
            rm -fr ./.gradle
            find product -name build -type d | grep -v node_modules | xargs rm -fr
            ./gradlew --stop
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
            // echo "JavaScript unit tests currently disabled due to Jenkins problems"
            sh """
            node --version
            npm --version
            product/warehouse/src/webapp/node_modules/phantomjs-prebuilt/bin/phantomjs --version
            """

            sh "./gradlew :product:warehouse:frontendTest"
            // TODO: publish Jasmine report
        }

        stage('Integration test') {
            sh './gradlew integrationTest'
            junit '**/build/integrationTest-results/*.xml'
        }

        stage('System test') {
            sh './gradlew systemTest -x :product:warehouse:frontendTest'
            junit '**/build/systemTest-results/*.xml'
        }

        stage('Assemble') {
            sh './gradlew check assemble'
        }
    }
}
