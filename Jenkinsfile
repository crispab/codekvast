node {
    timestamps {
        withEnv(['PHANTOMJS_BIN=/usr/local/lib/node_modules/phantomjs-prebuilt/bin/phantomjs']) {
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
                sh "./gradlew classes testClasses integrationTestClasses systemTestClasses"
            }

            stage('Java unit test') {
                sh "./gradlew test"
                junit '**/build/test-results/test/*.xml'
            }

            stage('JavaScript unit test') {
                sh "./gradlew :product:warehouse:frontendTest"
                junit '**/build/frontendTest-results/*.xml'
            }

            stage('Integration test') {
                sh './gradlew integrationTest'
                junit '**/build/integrationTest-results/*.xml'
            }

            stage('Build Docker image') {
                sh './gradlew :product:warehouse:buildDockerImage'
            }

            stage('System test') {
                sh './gradlew systemTest -x :product:warehouse:frontendTest'
                junit '**/build/systemTest-results/*.xml'
            }

            stage('Documentation') {
                sh './gradlew :product:docs:build'
                archiveArtifacts '**/build/asciidoc/html5/*.html'

                step([$class: 'JacocoPublisher',
                    classPattern: 'product/**/build/classes/main',
                    execPattern: '**/build/jacoco/*.exec',
                    changeBuildStatus: true,
                    maximumBranchCoverage: '30',
                    minimumBranchCoverage: '20',
                    maximumClassCoverage: '90',
                    minimumClassCoverage: '80',
                    maximumComplexityCoverage: '40',
                    minimumComplexityCoverage: '30',
                    maximumInstructionCoverage: '60',
                    minimumInstructionCoverage: '50',
                    maximumLineCoverage: '80',
                    minimumLineCoverage: '70',
                    maximumMethodCoverage: '70',
                    minimumMethodCoverage: '60',
                    ])
            }

        }
    }
}
