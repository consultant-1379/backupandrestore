def bob = './bob/bob -r service/ruleset.yaml'
def bobrelease = './bob/bob -r service/ruleset.release.yaml'

pipeline {
    agent {
        node {
            label SLAVE
        }
    }
    options {
        timestamps()
        timeout(time: 1, unit: 'HOURS')
    }
    environment {
        BRANCH=params.GERRIT_BRANCH.substring(GERRIT_BRANCH.lastIndexOf("-") + 1)
        HUB = credentials('eadphub-psw')
        API_TOKEN = credentials('hub-arm-rnd-ki-api-token')
        SEMANTIC_VERSION = "${sh(script:'cat fossa-config-bro.yml | sed -n -e \'s/^.*version: //p\'', returnStdout: true).trim()}"
        CODE_CHANGE = "${sh(script: "git diff --name-only HEAD HEAD~1 | grep -E '^service.*\\.java|^service/pom.xml' | wc -l", returnStdout: true).trim()}"
    }
    stages {
        stage('Prep') {
            parallel {
                stage("Init") {
                    when { expression { (!params.MANUAL) } }
                    steps {
                        buildName "#" + currentBuild.number + " | " + params.GERRIT_CHANGE_SUBJECT.substring(0, GERRIT_CHANGE_SUBJECT.indexOf(" ")) + " | ${GERRIT_BRANCH} | ${GERRIT_PATCHSET_UPLOADER_NAME}"
                    }
                }
                stage('Prepare bob') {
                    steps {
                        sh 'git clean -xdff --exclude=.m2 --exclude=settings.xml'
                        sh 'git submodule sync'
                        sh 'git submodule update --init --recursive'
                        sh "echo ${env.CODE_CHANGE}"
                    }
                }
                stage('Setup for characteristics report') {
                    when { expression { (params.CHAR_REPORT_GENERATE)}}
                    steps {
                        sh "mkdir -p .bob"
                        sh "echo ${params.CHAR_FRAGMENT_LINK} > .bob/var.char-fragment-url"
                    }
                }
            }
        }
        stage('Generate html from raml') {
            steps {
                sh "docker run --rm -v ${env.WORKSPACE}/service/SupportingDocumentation:/raml armdocker.rnd.ericsson.se/proj-planb/utils/raml2html -i /raml/rest_specification.raml -o /raml/rest_specification_v1v3.html"
                sh "docker run --rm -v ${env.WORKSPACE}/service/SupportingDocumentation:/raml armdocker.rnd.ericsson.se/proj-planb/utils/raml2html -i /raml/rest_specificationv2.raml -o /raml/rest_specification_v2.html"
                sh "docker run --user 1001:1001 --rm -v ${env.WORKSPACE}/service/SupportingDocumentation:/local armdocker.rnd.ericsson.se/proj-planb/openapitools/openapi-generator-cli generate -i /local/v4_spec.yaml -g html -o /local"
                sh "mv service/SupportingDocumentation/index.html service/SupportingDocumentation/rest_specification_v4.html"
            }
        }
        stage('Quality Checks') {
            parallel {
                stage('MVN Build') {
                    steps {
                        script {
                            if ( env.BRANCH != "master") {
                                sh "${bob} mvn:mvn-dev"
                            }
                            else {
                                sh "${bob} mvn:mvn-master"
                            }
                            if (!params.RELEASE && env.CODE_CHANGE != "0") {
                                withSonarQubeEnv('Sonarqube Server') {
                                    sh "${bob} mvn:package-test-sonar"
                                }
                                if ( env.BRANCH == "master") {
                                    sh "sed -i 1,2d service/newdeptree.txt"
                                    sh "diff service/currentdeptree.txt service/newdeptree.txt"
                                }
                            }
                            else {
                                sh "${bob} mvn:package"
                            }
                        }
                    }
                }
                stage('Build httpprobe') {
                    steps {
                        script {
                                sh "${bob} build-httpprobe"
                        }
                    }
                }
                stage('Generate sdif files from adoc') {
                    steps {
                        withCredentials([usernamePassword(credentialsId: 'eadphub-psw', usernameVariable: 'ERIDOC_USERNAME', passwordVariable: 'ERIDOC_PASSWORD')]) {
                          script {
                              sh "${bob} init"
                              sh './bob/bob -r eric-log4j2-socket-appender/ruleset.yaml generate-docs'
                              sh "${bob} generate-docs"
                              if (env.RELEASE == 'false') {
                                  sh "${bob} eridoc:dryrun"
                              }
                          }
                        }
                    }
                }
                stage('Helm v3 Lint') {
                    when { expression { (!params.RELEASE) } }
                    steps {
                        sh "${bob} lint"
                    }
                }
                stage('Validate license agreement') {
                    when { expression { (params.RELEASE) } }
                    steps {
                        sh "${bob} license-agreement"
                    }
                }
                stage('Validate interfaces json') {
                    when { expression { (!params.RELEASE) } }
                    steps {
                        sh "${bob} validate-interfaces"
                    }
                }
                stage('Pep8 + Pylint') {
                    when { expression { (!params.RELEASE) } }
                    steps {
                        sh "pep8 ./test/"
                        sh "python3 -m pylint --reports=n --rcfile=./testframework/.pylintrc ./test/*.py"
                    }
                }
                stage('validate pm_metrics') {
                    when { expression { (params.RELEASE) } }
                    steps {
                        sh "${bob} validate-pm-metrics"
                    }
                }
                stage('pm metrics dr checker local metric file') {
                    when { expression { (params.METRIC_DR_CHECKER) } }
                    steps {
                        sh "${bob} pm-metrics-checker"
                    }
                }
            }
        }
        stage('Build Image & Run DR Check') {
            parallel {
                stage('Build Snapshot Helm Chart/Docker Image') {
                    steps {
                        sh "${bob} init"
                        script {
                            if (env.RELEASE == 'false') {
                                // Set version build number to 0 as DRs fail regex if init:version generated build number used
                                sh "echo \"\$(cat .bob/var.semver)-0\" > .bob/var.version"
                            }
                            if ( env.BRANCH == "master") {
                                sh "${bob} setup-repo-paths:is-master"
                            }
                            else {
                                sh "${bob} setup-repo-paths:is-dev"
                            }
                            sh "${bob} image"
                            if (env.RELEASE == 'false') {
                                sh "${bob} image-dr-checker"
                            }
                        }
                    }
                }
            }
        }
        stage('Test security attributes fragment') {
            when { expression { (!params.RELEASE) } }
            steps {
                sh "${bob} security-attribute-fragment"
            }
        }
        stage('Generate, validate and upload security attributes fragment') {
            when { expression { (params.RELEASE) } }
            steps {
                sh "${bob} security-attribute-fragment:output-dir"
                sh "${bob} generate-security-attributes-json"
                sh "${bob} security-attribute-fragment:validate-security-attributes-json"
                sh "${bob} upload-security-attribute-fragment"
            }
        }
        stage('Helm DR Checker') {
            when { expression { (!params.RELEASE) } }
            steps {
                sh "${bob} check-helm-dr"
            }
        }
        stage('Push Snapshots of Docker and Helm') {
            when { expression { (params.RELEASE) } }
            steps {
                sh "${bob} push"
            }
        }
        stage('Int Test and prepare release image') {
            parallel {
                stage('Integration Test') {
                    when { expression { (params.RELEASE) && (!params.MANUAL) } }
                    environment {
                        TEST_CHART_VERSION = "${sh(script:'helm search devtest --versions | grep ${BRANCH} | awk \'{print $2}\' | head -n 1', returnStdout: true)}"
                        PYTHON_FILE = "nose_auto.py"
                    }
                    steps {
                        script {
                            def namespaces = ["cicd-bro", "cicd-bro-two", "cicd-bro-three"]
                            if (Jenkins.instance.getItem("${env.JOB_NAME}").builds[1..3].every { build-> build.result.toString() == 'FAILURE' || build.result.toString() == 'ABORTED'}) {
                                manualChoiceRequired = true
                            } else {
                                try {
                                    for (int i = 6; i >= 1; i--) {
                                        copyArtifacts(filter: 'namespace-used.txt', projectName: currentBuild.projectName, optional: true,
                                          selector: specific((currentBuild.number-i).toString()))
                                        if (fileExists('namespace-used.txt')) {
                                            if (Jenkins.instance.getItem("${env.JOB_NAME}").getBuildByNumber(currentBuild.number-i).result != hudson.model.Result.SUCCESS) {
                                                namespaces -= readFile(file: "namespace-used.txt")
                                            } else {
                                                namespaces += readFile(file: "namespace-used.txt")
                                                namespaces.unique()
                                            }
                                        }
                                    }
                                    if (namespaces) {
                                        writeFile file: "namespace-used.txt", text: namespaces[0]
                                        archiveArtifacts 'namespace-used.txt'
                                        env.NAMESPACE = readFile 'namespace-used.txt'
                                        manualChoiceRequired = false
                                        buildName "#" + currentBuild.number + " | " + params.GERRIT_CHANGE_SUBJECT.substring(0, GERRIT_CHANGE_SUBJECT.indexOf(" ")) + " | ${GERRIT_BRANCH} | ${GERRIT_PATCHSET_UPLOADER_NAME} | ${NAMESPACE}"
                                    } else {
                                        manualChoiceRequired = true
                                    }
                                } catch(err) {
                                    error("Failed to get a namespace")
                                }
                            }
                            if (manualChoiceRequired) {
                                def jobName = currentBuild.fullDisplayName
                                emailext body: '''${SCRIPT, template="groovy-html.template"}''',
                                mimeType: 'text/html',
                                subject: "${jobName} [PICK A NAMESPACE]",
                                to: '$GERRIT_CHANGE_OWNER_EMAIL',
                                replyTo: '$GERRIT_CHANGE_OWNER_EMAIL',
                                recipientProviders: [[$class: 'DevelopersRecipientProvider']]
                                def getNamespaceChoice
                                def userInput = input(
                                        id: 'userInput', message: 'Choose namespace:?',
                                        parameters: [
                                                choice(choices: 'cicd-bro\ncicd-bro-two\ncicd-bro-three',
                                                        description: 'Choose a namespace',
                                                        name: 'Namespace')
                                        ])
                                getNamespaceChoice = userInput?:''
                                writeFile file: "namespace-used.txt", text: "${getNamespaceChoice}"
                                env.NAMESPACE = readFile 'namespace-used.txt'
                                buildName "#" + currentBuild.number + " | " + params.GERRIT_CHANGE_SUBJECT.substring(0, GERRIT_CHANGE_SUBJECT.indexOf(" ")) + " | ${GERRIT_BRANCH} | ${GERRIT_PATCHSET_UPLOADER_NAME} | ${NAMESPACE}"
                                archiveArtifacts 'namespace-used.txt'
                            }
                        }
                        withCredentials([
                            file(credentialsId: 'hall060', variable: 'KUBECONFIG'),
                            file(credentialsId: 'sftp-users-yaml', variable: 'USERS_YAML'),
                            file(credentialsId: 'hub-armdocker-config-json', variable: 'DOCKER_CONFIG')
                        ]) {
                                sh '[[ "${TEST_CHART_VERSION}" != "" ]] && ./bob/bob -r service/ruleset.yaml setup-repo-paths:set-dev-test-repo || ./bob/bob -r service/ruleset.yaml setup-repo-paths:set-test-repo'
                                sh "${bob} deploy-to-k8s"
                        }
                    }
                }
                stage('Build Release Docker Image & Helm Chart') {
                    when { expression { (params.RELEASE) && (params.MANUAL) } }
                    steps {
                        sh "${bobrelease} init"
                        script {
                            if ( env.BRANCH == "master") {
                                sh "${bobrelease} setup-repo-paths:is-master"
                                sh "${bobrelease} generate-adp-artifacts"
                            }
                            else {
                                sh "${bobrelease} setup-repo-paths:is-dev"
                            }
                        }
                        sh "${bobrelease} setup-repo-paths:release-name"
                        sh "${bobrelease} image"
                    }
                }
            }
        }
        stage('Push Release of Docker and Helm') {
            when { expression { (params.RELEASE) && (params.MANUAL) } }
            steps {
                sh "${bobrelease} push"
            }
        }
        stage('Generate characteristics report formats') {
            when { expression { (params.CHAR_REPORT_GENERATE)}}
            steps {
                sh "${bobrelease} characteristics:pull"
                sh "${bobrelease} characteristics:generate-other-formats"
                archiveArtifacts artifacts: 'characteristics_report.html, characteristics_report.pdf, characteristics_report.md', allowEmptyArchive: true
            }
        }
        stage('Upload sdif files to Eridoc') {
            when { expression { (params.RELEASE) && (params.MANUAL) } }
            steps {
               withCredentials([usernamePassword(credentialsId: 'eadphub-psw', usernameVariable: 'ERIDOC_USERNAME', passwordVariable: 'ERIDOC_PASSWORD')]) {
                    sh "${bob} eridoc:upload"
               }
            }
        }
        stage('Package Marketplace Docs/Upload to ARM') {
            when { expression { (params.RELEASE) && (params.GERRIT_BRANCH=="master") && (params.MANUAL) } }
            steps {
                withCredentials([string(credentialsId: 'bro-marketplace-token', variable: 'MARKETPLACE_TOKEN')]) {
                    script {
                        try {
                            sh "${bobrelease} marketplace-upload"
                        } catch (err) {
                            echo err.getMessage()
                        }
                    }
                }
            }
        }
        stage('Tag the Git Repo') {
            when { expression { (params.RELEASE) && (params.MANUAL) } }
            steps {
                sh "${bobrelease} git-tag"
            }
        }
        stage('Munin Set/Create Version') {
            steps {
                withCredentials([string(credentialsId: 'munin_token', variable: 'MUNIN_TOKEN')]) {
                    script {
                        uplift = sh (script: "git log -1 | grep '.*\\[UPLIFT\\].*'", returnStatus: true)
                        if (uplift == 0) {
                            sh "${bob} munin-update-version"
                        }
                    }
                }
            }
        }
        stage('Trigger BRO_CMM') {
            when { expression { (params.RELEASE) && (!params.MANUAL) } }
            steps {
                 build job: 'BRO_CMM', parameters: [
                      [$class: 'StringParameterValue', name: 'GERRIT_BRANCH', value: "${params.GERRIT_BRANCH}", defaultValue: "master"],
                      [$class: 'StringParameterValue', name: 'GERRIT_CHANGE_SUBJECT', value: "${params.GERRIT_CHANGE_SUBJECT}", defaultValue: "Nightly run"],
                      [$class: 'StringParameterValue', name: 'GERRIT_PATCHSET_UPLOADER_NAME', value: "${params.GERRIT_PATCHSET_UPLOADER_NAME}", defaultValue: "Cron job"]
                 ], wait: false
            }
        }
        stage('Retrieve XRAY report') {
            when { expression { (params.RELEASE) && (!params.MANUAL) } }
            steps {
                withCredentials([string(credentialsId: 'xray-api-token', variable: 'XRAY_TOKEN')]) {
                    script {
                        try {
                            sh "${bob} get-xray-report"
                        } catch (err) {
                            echo err.getMessage()
                        }
                    }
                }
            }
        }
        stage('Trigger Vuln Hub Upload') {
            when { expression { (params.RELEASE) && (params.MANUAL) && (params.TRIGGER_NIGHTLY_VULN_HUB_UPLOAD) } }
            steps {
                build job: 'NightlyJobs/BRO_Security_Nightly', parameters: [
                      [$class: 'BooleanParameterValue', name: 'UPLOAD_VA_CONFIG', value: true],
                      [$class: 'StringParameterValue', name: 'UPLOAD_SCAN_RESULTS', value: "true"],
                      [$class: 'StringParameterValue', name: 'VERSION_FOR_VULN_HUB', value: "${env.SEMANTIC_VERSION}"]
                ], wait: false
            }
        }
    }
    post {
        always {
            script {
                if (params.RELEASE) {
                    archiveArtifacts artifacts: 'artifact.properties, output.html, pod_logs/*.*, pm_metric_check_report.html', allowEmptyArchive: true
                }
                archiveArtifacts artifacts: '.bob/eric-ctrl-bro-internal/eric-ctrl-bro*.tgz, service/currentdeptree.txt, service/newdeptree.txt, build/service/Documentation/**/*.*, .bob/XRAY-report.md, .bob/image-design-rule-check-report.html, .bob/DR-Checks/**/*.*, service/target/site/surefire-report.html, .bob/eridoc-upload-report.log, service/SupportingDocumentation/rest_specification_v1v3.html, service/SupportingDocumentation/rest_specification_v2.html, service/SupportingDocumentation/rest_specification_v4.html', allowEmptyArchive: true
                if (!params.RELEASE && env.CODE_CHANGE != "0") {
                    withSonarQubeEnv('Sonarqube Server') {
                        for (Integer i = 0; i < 20; i++) {
                            try {
                                timeout(time: 15, unit: 'SECONDS') {
                                    def qg = waitForQualityGate()
                                    if (qg.status != 'OK') {
                                        error "Pipeline aborted due to quality gate coverage failure: ${qg.status}"
                                    } else {
                                        i = 20
                                    }
                                }
                            } catch (Throwable e) {
                                if (i == 19) {
                                    throw e
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
