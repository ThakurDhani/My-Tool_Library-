def call(String configFile = "prod.yaml") {
    def yamlText = libraryResource("config/${configFile}")
    def config = readYaml text: yamlText

    pipeline {
        agent any
        stages {
            stage('Deploy') {
                steps {
                    script {
                        checkout scm

                        if (config.KEEP_APPROVAL_STAGE) {
                            timeout(time: 10, unit: 'MINUTES') {
                                input message: "Approve Ansible execution for environment: ${config.ENVIRONMENT}", ok: 'Approve'
                            }
                        }

                        dir("${config.CODE_BASE_PATH}") {
                            withCredentials([file(credentialsId: 'ansible-private-key', variable: 'PEM_FILE')]) {
                                withEnv(['ANSIBLE_HOST_KEY_CHECKING=False']) {
                                    sh '''
                                        ansible-playbook playbook.yml -i inventory.ini --private-key="$PEM_FILE"
                                    '''
                                }
                            }
                        }

                        slackSend channel: "${config.SLACK_CHANNEL_NAME}", message: "${config.ACTION_MESSAGE}"
                    }
                }
            }
        }
    }
}
