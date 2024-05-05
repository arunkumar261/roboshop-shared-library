pipeline {
    agent {
        node {
            label 'Agent-1'
        }
    }
    environment {
        packageVersion = ''
        nexusURL = '172.31.25.31:8081'
    }

    options {
        timeout(time:1, unit: 'HOURS')
        disableConcurrentBuilds()
    }

     parameters {
        booleanParam(name: 'Deploy', defaultValue: 'false', description: 'Toggle the value')
        
    }


    stages {
        stage('Get the version') {
            steps {
                script {
                    def packageJson = readJSON file: 'package.json'
                    packageVersion = packageJson.version
                    echo "applicationversion : $packageVersion"
                }
            }
        }

        stage('install dependencies') {
            steps {
                sh """
                    npm install
                """
            }
        }
        stage('unit test') {
            steps {
                 echo "this is testing stage"
            }
        }
        stage('Sonar-scan') {
            steps {
                 sh """
                    sonar-scanner
                 """
            }
        }
        stage('Build') {
            steps {
                sh """
                    ls -la
                    zip -q -r catalogue.zip ./* -x ".git" -x "*.zip"
                    ls -ltr

                """
            }
        }
        stage('Publish to nexus') {
            steps {
                 nexusArtifactUploader(
                 nexusVersion: 'nexus3',
                 protocol: 'http',
                 nexusUrl: "${nexusURL}",
                 groupId: 'com.roboshop',
                 version: "${packageVersion}",
                 repository: 'catalogue',
                 credentialsId: 'nexus-auth',
                 artifacts: [
                        [artifactId: 'catalogue',
                        classifier: '',
                        file: 'catalogue.zip',
                        type: 'zip']
                    ]
                )
            }
        }
        stage('trigger catalogue-deploy2 pipeline once ci success yi.e., deploy stage') {

            when {
                expression {
                    params.Deploy == 'true'
                }
            }
            steps {
                build job: 'catalogue-deploy2', wait: true,
                parameters: [
                    string(name: 'version', value: "${packageVersion}"),
                    string(name: 'environment', value: "dev"),
                ]
            }
        }

        
    }
//post always executes even if success or failed
    post {
        always {
            echo "I will always say Hello"
            deleteDir()
        }
        failure {
            echo "I will say Hello only pipeline fails"
        }
        success {
            echo "I will say Hello only pipeline success"
        }
    }
}