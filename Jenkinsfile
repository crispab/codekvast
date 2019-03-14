slackNotification null, 'Build Started', null
def startedAt = java.time.Instant.now()

node {
    try {
        timestamps {
            stage('Prepare') {
                checkout scm
                sh """
                printenv | sort
                tools/install-compilers.sh
                # tools/real-clean-workspace.sh
                pwd
                """
            }

            stage('Compile Java') {
                sh "./.gradlew classes testClasses integrationTestClasses"
            }

            stage('Java unit test') {
                try {
                    sh "./.gradlew test --exclude-task :product:system-test:test"
                } finally {
                    // Prevent junit publisher to fail if Gradle has skipped the test
                    sh "find . -name '*.xml' | grep '/build/test-results/test/' | xargs --no-run-if-empty touch"
                    junit '**/build/test-results/test/*.xml'
                }
            }

            stage('Integration test') {
                try {
                    sh "./.gradlew integrationTest"
                } finally {
                    // Prevent junit publisher to fail if Gradle has skipped the test
                    sh "find . -name '*.xml' | grep '/build/test-results/integrationTest/' | xargs --no-run-if-empty touch"
                    junit '**/build/test-results/integrationTest/*.xml'
                }
            }

            stage('Frontend webpack') {
                lock("Codekvast-${env.BRANCH_NAME}-webpack") {
                    try {
                        sh "./.gradlew :product:server:dashboard:frontendWebpack"
                    } finally {
                        // Prevent junit publisher to fail if Gradle has skipped the test
                        sh "find . -name '*.xml' | grep '/build/test-results/frontendTest/' | xargs --no-run-if-empty touch"
                        junit '**/build/test-results/frontendTest/*.xml'

                        publishHTML([allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'product/dashboard/build/reports/frontend-coverage',
                            reportFiles: 'index.html',
                            reportName: 'Frontend Coverage Report'])
                    }
                }
            }

            stage('System test') {
                try {
                    sh "./.gradlew :product:system-test:test"
                } finally {
                    archiveArtifacts '**/system-test/build/*.log'

                    // Prevent junit publisher to fail if Gradle has skipped the test
                    sh "find . -name '*.xml' | grep '/build/test-results/test/' | xargs --no-run-if-empty touch"
                    junit '**/build/test-results/test/*.xml'
                }
            }

            stage('Documentation & reports') {
                sh "./.gradlew -Dorg.gradle.configureondemand=false :product:docs:build :product:aggregateJavadoc"

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

// TODO:          step([$class: 'JacocoPublisher',
//                    classPattern: 'product/**/build/classes/main',
//                    execPattern: '**/build/jacoco/*.exec',
//                    buildOverBuild: true,
//                    changeBuildStatus: true,
//                    deltaBranchCoverage: '10',
//                    deltaClassCoverage: '10',
//                    deltaComplexityCoverage: '10',
//                    deltaInstructionCoverage: '10',
//                    deltaLineCoverage: '10',
//                    deltaMethodCoverage: '10',
//                    maximumBranchCoverage: '30',
//                    minimumBranchCoverage: '20',
//                    maximumClassCoverage: '90',
//                    minimumClassCoverage: '80',
//                    maximumComplexityCoverage: '40',
//                    minimumComplexityCoverage: '30',
//                    maximumInstructionCoverage: '50',
//                    minimumInstructionCoverage: '40',
//                    maximumLineCoverage: '80',
//                    minimumLineCoverage: '65',
//                    maximumMethodCoverage: '70',
//                    minimumMethodCoverage: '60',
//                    ])

                echo "Running tools/uptodate-report.sh"
                sh 'tools/uptodate-report.sh'
                archiveArtifacts 'build/reports/**'
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
    // Transform e.g., "PT3M42.8934S" to "3m 42s"
    d.toString().replaceFirst("^PT", "").replaceAll("([A-Z])", "\$1 ").replaceAll("\\.[0-9]+", "").toLowerCase()
}

def slackNotification(color, message, startedAt) {
    def duration = startedAt == null ? "" : " in ${prettyDuration(java.time.Duration.between(startedAt, java.time.Instant.now()))}"
    def console = "${env.BUILD_URL}/console".replace('//console', '/console')
    slackSend color: color, message: "${java.time.LocalDateTime.now()} ${message}${duration} ${console}", teamDomain: 'codekvast', channel: '#builds', tokenCredentialId: 'codekvast.slack.com'
}
