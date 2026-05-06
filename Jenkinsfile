pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    parameters {
        string(name: 'APP_EC2_HOST', defaultValue: 'your-app-ec2-public-ip', description: 'Public IP or DNS of the application EC2 instance')
        string(name: 'APP_EC2_USER', defaultValue: 'ubuntu', description: 'SSH user for the application EC2 instance')
        string(name: 'DEPLOY_PATH', defaultValue: '/home/ubuntu/recruitment-system', description: 'Deployment directory on the application EC2 instance')
        string(name: 'REMOTE_ENV_FILE', defaultValue: '/home/ubuntu/.env', description: 'Production env file already present on the application EC2 instance')
    }

    environment {
        APP_NAME = 'recruitment-system'
        SSH_CREDENTIALS_ID = 'app-ec2-ssh-key'
        RELEASE_ARCHIVE = 'recruitment-system.tar.gz'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Package Deployment Files') {
            steps {
                sh '''
                    rm -f "${RELEASE_ARCHIVE}"
                    tar \
                      --exclude='.git' \
                      --exclude='backend/target' \
                      --exclude='frontend/node_modules' \
                      --exclude='.env' \
                      --exclude='.env.production' \
                      -czf "${RELEASE_ARCHIVE}" .
                '''
            }
        }

        stage('Prepare Application EC2') {
            steps {
                sshagent([env.SSH_CREDENTIALS_ID]) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no "${APP_EC2_USER}@${APP_EC2_HOST}" '
                            set -eu

                            if ! command -v docker >/dev/null 2>&1; then
                                sudo apt-get update -y
                                sudo apt-get install -y docker.io
                                sudo systemctl enable --now docker
                                sudo usermod -aG docker '"${APP_EC2_USER}"'
                            fi

                            if ! docker compose version >/dev/null 2>&1; then
                                sudo apt-get update -y
                                sudo apt-get install -y docker-compose-v2
                            fi

                            if [ ! -f '"${REMOTE_ENV_FILE}"' ]; then
                                echo "Missing production env file: '"${REMOTE_ENV_FILE}"'"
                                echo "Create it from .env.production.example before running deployment."
                                exit 1
                            fi

                            mkdir -p '"${DEPLOY_PATH}"'
                        '
                    '''
                }
            }
        }

        stage('Upload Release') {
            steps {
                sshagent([env.SSH_CREDENTIALS_ID]) {
                    sh '''
                        scp -o StrictHostKeyChecking=no "${RELEASE_ARCHIVE}" "${APP_EC2_USER}@${APP_EC2_HOST}:/tmp/${RELEASE_ARCHIVE}"

                        ssh -o StrictHostKeyChecking=no "${APP_EC2_USER}@${APP_EC2_HOST}" '
                            set -eu
                            mkdir -p '"${DEPLOY_PATH}"'
                            tar -xzf /tmp/'"${RELEASE_ARCHIVE}"' -C '"${DEPLOY_PATH}"'
                            cp '"${REMOTE_ENV_FILE}"' '"${DEPLOY_PATH}"'/.env.production
                        '
                    '''
                }
            }
        }

        stage('Build and Deploy') {
            steps {
                sshagent([env.SSH_CREDENTIALS_ID]) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no "${APP_EC2_USER}@${APP_EC2_HOST}" '
                            set -eu
                            cd '"${DEPLOY_PATH}"'

                            sudo docker compose --env-file .env.production down --remove-orphans
                            sudo docker compose --env-file .env.production build --pull
                            sudo docker compose --env-file .env.production up -d
                            sudo docker image prune -f
                            sudo docker compose --env-file .env.production ps
                        '
                    '''
                }
            }
        }

        stage('Health Check') {
            steps {
                sshagent([env.SSH_CREDENTIALS_ID]) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no "${APP_EC2_USER}@${APP_EC2_HOST}" '
                            set -eu
                            cd '"${DEPLOY_PATH}"'

                            echo "Waiting for backend health endpoint..."
                            for attempt in $(seq 1 30); do
                                status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || true)
                                if [ "$status" = "200" ]; then
                                    echo "Backend is healthy."
                                    exit 0
                                fi
                                echo "Attempt ${attempt}/30 returned HTTP ${status}; retrying..."
                                sleep 5
                            done

                            sudo docker compose --env-file .env.production logs backend --tail=80
                            exit 1
                        '
                    '''
                }
            }
        }

        stage('Monitoring Check') {
            steps {
                sshagent([env.SSH_CREDENTIALS_ID]) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no "${APP_EC2_USER}@${APP_EC2_HOST}" '
                            set -eu
                            curl -fsS http://localhost:9090/-/ready >/dev/null
                            curl -fsS http://localhost:9100/metrics >/dev/null
                            curl -fsS http://localhost:3001/api/health >/dev/null
                            echo "Prometheus, Grafana, and node-exporter are reachable locally."
                        '
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "Deployment successful."
            echo "Frontend  : http://${params.APP_EC2_HOST}"
            echo "Backend   : http://${params.APP_EC2_HOST}:8080"
            echo "Prometheus: http://${params.APP_EC2_HOST}:9090"
            echo "Grafana   : http://${params.APP_EC2_HOST}:3001"
        }
        failure {
            echo "Deployment failed. Check the failed stage logs above."
        }
        cleanup {
            sh 'rm -f "${RELEASE_ARCHIVE}"'
        }
    }
}
