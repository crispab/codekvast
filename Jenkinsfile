slackNotification 'gray', 'Build Started'
try {

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
                    sh "./gradlew foobar classes testClasses integrationTestClasses systemTestClasses"
                }

                stage('Java unit test') {
                    sh "./gradlew test"
                    junit '**/build/test-results/test/*.xml'
                }

                stage('TypeScript unit test') {
                    sh "./gradlew :product:warehouse:frontendTest"
                    junit '**/build/frontendTest-results/*.xml'
                    archiveArtifacts 'product/warehouse/build/frontendTest-coverage/**'
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

                stage('Documentation & reports') {
                    sh './gradlew :product:docs:build :product:aggregateJavadoc'

                    archiveArtifacts '**/build/asciidoc/html5/*.html'
                    step([$class: 'JavadocArchiver', javadocDir: 'product/build/docs/javadoc', keepAll: false])

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
                        maximumInstructionCoverage: '50',
                        minimumInstructionCoverage: '40',
                        maximumLineCoverage: '80',
                        minimumLineCoverage: '70',
                        maximumMethodCoverage: '70',
                        minimumMethodCoverage: '60',
                        ])

                }

            }
        }
    }
    slackNotification 'green', 'Build Finished'
} catch(err) {
    slackNotification 'red', "Build Failed: $err"
}

def slackNotification(color, message) {
    slackSend color: color, message: "${java.time.LocalDateTime.now()} ${message} ${env.BUILD_URL}", teamDomain: 'codekvast', channel: '#builds', tokenCredentialId: 'codekvast.slack.com'
}
