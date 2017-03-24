slackNotification null, 'Build Started'
def startedAt = java.time.Instant.now()
node {
    try {
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

                stage('TypeScript unit test') {
                    sh "./gradlew :product:warehouse:frontendTest"

                    junit '**/build/test-results/frontendTest/*.xml'

                    publishHTML([allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'product/warehouse/build/reports/frontend-coverage',
                        reportFiles: 'index.html',
                        reportName: 'Frontend Coverage Report'])
                }

                stage('Integration test') {
                    sh './gradlew integrationTest'
                    junit '**/build/test-results/integrationTest/*.xml'
                }

                stage('Build Docker image') {
                    sh './gradlew :product:warehouse:buildDockerImage'
                }

                stage('System test') {
                    sh './gradlew systemTest -x :product:warehouse:frontendTest'
                    junit '**/build/test-results/systemTest/*.xml'
                }

                stage('Documentation & reports') {
                    sh './gradlew :product:docs:build :product:aggregateJavadoc'

                    publishHTML([allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'product/docs/build/asciidoc/html5',
                        reportFiles: 'CodekvastUserManual.html',
                        reportName: 'User Manual'])

                    publishHTML([allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'product/build/docs/javadoc',
                        reportFiles: 'index.html',
                        reportName: 'API docs'])

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

                    sh 'tools/uptodate-report.sh'
                    archiveArtifacts 'build/reports/**'
                }

            }
        }
        def duration = java.time.Duration.between(startedAt, java.time.Instant.now())
        slackNotification 'good', "Build finished in $duration"
    } catch(err) {
        def duration = java.time.Duration.between(startedAt, java.time.Instant.now())
        slackNotification 'danger', "Build failed in $duration: $err"
        throw err
    } finally {
        stage('Cleanup') {
            sh 'tools/jenkins-cleanup.sh'
        }
    }
}

def slackNotification(color, message) {
    slackSend color: color, message: "${java.time.LocalDateTime.now()} ${message} ${env.BUILD_URL}", teamDomain: 'codekvast', channel: '#builds', tokenCredentialId: 'codekvast.slack.com'
}
