slackNotification null, 'Build Started', null
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
                    sh './gradlew systemTest'
                    junit '**/build/test-results/systemTest/*.xml'
                }

                stage('Documentation & reports') {
                    sh './gradlew -Dorg.gradle.configureondemand=false :product:docs:build :product:aggregateJavadoc'

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

// JacocoPublisher is broken, it hangs
//                    step([$class: 'JacocoPublisher',
//                        classPattern: 'product/**/build/classes/main',
//                        execPattern: '**/build/jacoco/*.exec',
//                        buildOverBuild: true,
//                        changeBuildStatus: true,
//                        deltaBranchCoverage: '10',
//                        deltaClassCoverage: '10',
//                        deltaComplexityCoverage: '10',
//                        deltaInstructionCoverage: '10',
//                        deltaLineCoverage: '10',
//                        deltaMethodCoverage: '10',
//                        maximumBranchCoverage: '30',
//                        minimumBranchCoverage: '20',
//                        maximumClassCoverage: '90',
//                        minimumClassCoverage: '80',
//                        maximumComplexityCoverage: '40',
//                        minimumComplexityCoverage: '30',
//                        maximumInstructionCoverage: '50',
//                        minimumInstructionCoverage: '40',
//                        maximumLineCoverage: '80',
//                        minimumLineCoverage: '70',
//                        maximumMethodCoverage: '70',
//                        minimumMethodCoverage: '60',
//                        ])
//
                    echo "Running tools/uptodate-report.sh"
                    sh 'tools/uptodate-report.sh'
                    archiveArtifacts 'build/reports/**'
                }

            }
        }
        slackNotification 'good', "Build finished", startedAt
    } catch(err) {
        slackNotification 'danger', "Build failed", startedAt
        throw err
    } finally {
        stage('Cleanup') {
            sh 'tools/jenkins-cleanup.sh'
        }
    }
}

def prettyDuration(java.time.Duration d) {
    // Transform e.g., "PT3M42S" to "3m 42s"
    d.toString().replaceFirst("^PT", "").replaceAll("([A-Z])", "\$1 ").toLowerCase()
}

def slackNotification(color, message, startedAt) {
    def duration = startedAt == null ? "" : " in ${prettyDuration(java.time.Duration.between(startedAt, java.time.Instant.now()))}"
    def console = "${env.BUILD_URL}/console".replace('//console', '/console')
    slackSend color: color, message: "${java.time.LocalDateTime.now()} ${message}${duration} ${console}", teamDomain: 'codekvast', channel: '#builds', tokenCredentialId: 'codekvast.slack.com'
}
