pipeline {
    agent any

    tools {
        jdk 'JDK-21'
        nodejs 'Node-20'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam(name: 'RUN_KARATE_TESTS', defaultValue: true, description: 'Execute Karate REST API Integration Tests')
        booleanParam(name: 'BUILD_DOCKER_IMAGES', defaultValue: true, description: 'Build Docker container images for frontend and backend')
        booleanParam(name: 'PUBLISH_IMAGES', defaultValue: false, description: 'Push built images to configured container registry')
        booleanParam(name: 'DEPLOY_K8S', defaultValue: false, description: 'Deploy manifests to Kubernetes cluster')
        string(name: 'DOCKER_REGISTRY', defaultValue: 'registry.hub.docker.com', description: 'Target Docker Registry')
        string(name: 'DOCKER_IMAGE_PREFIX', defaultValue: 'smartinventory', description: 'Image repository prefix')
    }

    environment {
        JAVA_HOME = tool 'JDK-21'
        NODE_HOME = tool 'Node-20'
        PATH = "${JAVA_HOME}/bin:${NODE_HOME}/bin:${env.PATH}"
        BACKEND_IMAGE = "${params.DOCKER_REGISTRY}/${params.DOCKER_IMAGE_PREFIX}/backend:${BUILD_NUMBER}"
        FRONTEND_IMAGE = "${params.DOCKER_REGISTRY}/${params.DOCKER_IMAGE_PREFIX}/frontend:${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend Unit & Integration Tests') {
            steps {
                dir('backend') {
                    sh './mvnw clean test'
                }
            }
            post {
                always {
                    junit testResults: 'backend/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Backend Package') {
            steps {
                dir('backend') {
                    sh './mvnw package -DskipTests'
                }
            }
        }

        stage('Frontend Build & Verification') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Karate API Tests') {
            when {
                expression { return params.RUN_KARATE_TESTS }
            }
            steps {
                dir('tests/karate') {
                    sh './mvnw test -Dtest=KarateTestRunner'
                }
            }
            post {
                always {
                    junit testResults: 'tests/karate/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Build Docker Images') {
            when {
                expression { return params.BUILD_DOCKER_IMAGES }
            }
            steps {
                script {
                    echo "Building Backend Docker image: ${env.BACKEND_IMAGE}"
                    sh "docker build -t ${env.BACKEND_IMAGE} -t ${params.DOCKER_REGISTRY}/${params.DOCKER_IMAGE_PREFIX}/backend:latest ./backend"

                    echo "Building Frontend Docker image: ${env.FRONTEND_IMAGE}"
                    sh "docker build -t ${env.FRONTEND_IMAGE} -t ${params.DOCKER_REGISTRY}/${params.DOCKER_IMAGE_PREFIX}/frontend:latest ./frontend"
                }
            }
        }

        stage('Publish Docker Images') {
            when {
                allOf {
                    expression { return params.BUILD_DOCKER_IMAGES }
                    expression { return params.PUBLISH_IMAGES }
                }
            }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-registry-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh "echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin ${params.DOCKER_REGISTRY}"
                        sh "docker push ${env.BACKEND_IMAGE}"
                        sh "docker push ${params.DOCKER_REGISTRY}/${params.DOCKER_IMAGE_PREFIX}/backend:latest"
                        sh "docker push ${env.FRONTEND_IMAGE}"
                        sh "docker push ${params.DOCKER_REGISTRY}/${params.DOCKER_IMAGE_PREFIX}/frontend:latest"
                        sh "docker logout ${params.DOCKER_REGISTRY}"
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            when {
                expression { return params.DEPLOY_K8S }
            }
            steps {
                script {
                    withCredentials([file(credentialsId: 'kubeconfig-credentials', variable: 'KUBECONFIG')]) {
                        sh "kubectl apply -f k8s/00-namespace.yaml"
                        sh "kubectl apply -f k8s/01-configmap.yaml"
                        sh "kubectl apply -f k8s/02-secret.yaml"
                        sh "kubectl apply -f k8s/03-postgres-pvc.yaml"
                        sh "kubectl apply -f k8s/04-postgres-deployment.yaml"
                        sh "kubectl apply -f k8s/05-backend-deployment.yaml"
                        sh "kubectl apply -f k8s/06-frontend-deployment.yaml"
                        sh "kubectl apply -f k8s/07-ingress.yaml"
                        sh "kubectl rollout status deployment/backend-deployment -n smart-inventory --timeout=180s"
                        sh "kubectl rollout status deployment/frontend-deployment -n smart-inventory --timeout=120s"
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully."
        }
        failure {
            echo "Pipeline failed! Inspect console output and test reports."
        }
    }
}
