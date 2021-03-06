pipeline {
    agent any
    stages {
        stage('cloning spring3hibernate and compiling the code') {
            steps {
                git 'https://github.com/opstree/spring3hibernate.git'
                sh '''
                mvn clean
                mvn compile
                '''
            }
        }
        stage('Testing the code') {
            steps {
                sh "mvn test"
            }
        }
        stage('sending test analysis to SonarQube') {
            steps {
                withSonarQubeEnv('sonar') {
                    sh "mvn sonar:sonar"
                }
            }
        }
        stage('Creating package') {
            steps {
                sh "mvn package"
            }
        }
        stage('ArchiveArtifact'){
            steps {
                    sh "mvn surefire-report:report"
                    archiveArtifacts 'target/*.war'   
            }
        }
       stage("Uploading Artifact"){
            steps{
                rtServer (
                id: 'key',
                url: 'http://6322371616d0.ngrok.io/artifactory',
                // If you're using username and password:
                username: 'admin',
                password: '6DE@Dpool9',
                // If you're using Credentials ID:
                //credentialsId: 'ccrreeddeennttiiaall',
                // Configure the connection timeout (in seconds).
                // The default value (if not configured) is 300 seconds:
                timeout: 300
                )
                
                rtUpload (
                serverId: 'key',
                spec: '''{
                      "files": [
                        {
                          "pattern": "target/*.war",
                          "target": "key/Build_${BUILD_NUMBER}/"
                        }
                     ]
                }'''
                )
                /*rtDownload (
                serverId: 'key',
                spec: '''{
                        "files": [
                        {
                            "pattern": "key/Build_${BUILD_NUMBER}/",
                            "target": "aditya/Build_${BUILD_NUMBER}"
                        }
                    ]
                }''' 
                )
            }
        }*/
    }
    post {
        success {
            notifySuccessful()
        }
        failure{
            notifyFailed()
        }
        always{
            publishHTML(target : [allowMissing: false, alwaysLinkToLastBuild: true, keepAll: false, reportDir: '/var/lib/jenkins/workspace/pi/target/site', reportFiles: 'surefire-report.html', reportName: 'HTML Report', reportTitles: ''])
        }
    }
}
def notifyFailed() {
    slackSend (color: '#00FF00', message: "Failed: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
}
def notifySuccessful() {
    slackSend (color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
    build job: 'upload',
        parameters: [
            string(name: 'BUILD_NU', value: 'Build_' + String.valueOf(env.BUILD_NUMBER))
        ]
}
