

pipeline {
    agent any

    parameters {
        string(
            name: 'TARGET_IP',
            defaultValue: '',
            description: 'Enter target EC2 public IP address'
        )
    }

    environment {
        APP_NAME     = "recruitmentsystem"
        DEPLOY_PATH  = "/home/ubuntu/workspace/recruitmentsystem"
        GIT_REPO     = "https://github.com/ChaitraBiradar10/Recruitment-system.git"
        GIT_BRANCH   = "main"

        // Jenkins SSH private key credential ID
        SSH_CRED_ID  = "e9ef2a50-d9e9-481e-bf01-09b8cec27689"

        // Jenkins Secret File credential ID for .env.recruitmentsystem
        ENV_CRED_ID  = "recruitment-env-file"
    }

    stages {

        stage('Validate Target IP') {
            steps {
                sh """
                    echo "Target EC2 IP: ${params.TARGET_IP}"

                    if [ -z "${params.TARGET_IP}" ]; then
                        echo "ERROR: TARGET_IP is empty"
                        echo "Use Build with Parameters and enter the EC2 public IP."
                        exit 1
                    fi
                """
            }
        }

        stage('Test SSH Connection') {
            steps {
                sshagent(credentials: ["${SSH_CRED_ID}"]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@${params.TARGET_IP} '
                            echo "SSH connection successful"
                            echo "Hostname: \$(hostname)"
                            echo "User: \$(whoami)"
                        '
                    """
                }
            }
        }

        stage('Prepare Target EC2') {
            steps {
                sshagent(credentials: ["${SSH_CRED_ID}"]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@${params.TARGET_IP} '
                            echo "Preparing target EC2 server..."

                            sudo apt-get update -y

                            if ! command -v git >/dev/null 2>&1; then
                                echo "Installing Git..."
                                sudo apt-get install -y git
                            else
                                echo "Git already installed"
                            fi

                            if ! command -v docker >/dev/null 2>&1; then
                                echo "Installing Docker..."
                                sudo apt-get install -y docker.io
                                sudo systemctl start docker
                                sudo systemctl enable docker
                                sudo usermod -aG docker ubuntu
                            else
                                echo "Docker already installed"
                            fi

                            if ! docker compose version >/dev/null 2>&1; then
                                echo "Installing Docker Compose v2..."
                                sudo apt-get install -y docker-compose-v2
                            else
                                echo "Docker Compose already installed"
                            fi

                            sudo systemctl start docker
                            sudo systemctl enable docker

                            mkdir -p /home/ubuntu/workspace

                            echo "Target EC2 preparation completed"
                        '
                    """
                }
            }
        }

        stage('Clone or Update Repository') {
            steps {
                sshagent(credentials: ["${SSH_CRED_ID}"]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@${params.TARGET_IP} '
                            if [ ! -d ${DEPLOY_PATH}/.git ]; then
                                echo "Cloning repository..."
                                git clone -b ${GIT_BRANCH} ${GIT_REPO} ${DEPLOY_PATH}
                            else
                                echo "Updating existing repository..."
                                cd ${DEPLOY_PATH}
                                git fetch origin
                                git reset --hard origin/${GIT_BRANCH}
                            fi

                            echo "Repository ready at ${DEPLOY_PATH}"
                        '
                    """
                }
            }
        }

        stage('Copy ENV File and Update IP') {
            steps {
                sshagent(credentials: ["${SSH_CRED_ID}"]) {
                    withCredentials([file(credentialsId: "${ENV_CRED_ID}", variable: 'ENV_FILE_PATH')]) {
                        sh """
                            echo "Copying Jenkins secret env file to target EC2..."

                            scp -o StrictHostKeyChecking=no "\$ENV_FILE_PATH" ubuntu@${params.TARGET_IP}:${DEPLOY_PATH}/.env.recruitmentsystem

                            ssh -o StrictHostKeyChecking=no ubuntu@${params.TARGET_IP} '
                                cd ${DEPLOY_PATH}

                                chmod 600 .env.recruitmentsystem

                                ENV_FILE="${DEPLOY_PATH}/.env.recruitmentsystem"

                                if [ ! -f "\$ENV_FILE" ]; then
                                    echo "ERROR: .env.recruitmentsystem not found at \$ENV_FILE"
                                    exit 1
                                fi

                                echo "ENV file copied successfully"

                                echo "Updating HOST_IP..."

                                if grep -q "^HOST_IP=" "\$ENV_FILE"; then
                                    sed -i "s|^HOST_IP=.*|HOST_IP=${params.TARGET_IP}|" "\$ENV_FILE"
                                else
                                    echo "HOST_IP=${params.TARGET_IP}" >> "\$ENV_FILE"
                                fi

                                echo "Updating APP_FRONTEND_URL..."

                                if grep -q "^APP_FRONTEND_URL=" "\$ENV_FILE"; then
                                    sed -i "s|^APP_FRONTEND_URL=.*|APP_FRONTEND_URL=http://${params.TARGET_IP}|" "\$ENV_FILE"
                                else
                                    echo "APP_FRONTEND_URL=http://${params.TARGET_IP}" >> "\$ENV_FILE"
                                fi

                                echo "Updating APP_CORS_ALLOWED_ORIGINS..."

                                if grep -q "^APP_CORS_ALLOWED_ORIGINS=" "\$ENV_FILE"; then
                                    sed -i "s|^APP_CORS_ALLOWED_ORIGINS=.*|APP_CORS_ALLOWED_ORIGINS=http://${params.TARGET_IP}|" "\$ENV_FILE"
                                else
                                    echo "APP_CORS_ALLOWED_ORIGINS=http://${params.TARGET_IP}" >> "\$ENV_FILE"
                                fi

                                echo "Updating GRAFANA_ROOT_URL..."

                                if grep -q "^GRAFANA_ROOT_URL=" "\$ENV_FILE"; then
                                    sed -i "s|^GRAFANA_ROOT_URL=.*|GRAFANA_ROOT_URL=http://${params.TARGET_IP}:3001/|" "\$ENV_FILE"
                                else
                                    echo "GRAFANA_ROOT_URL=http://${params.TARGET_IP}:3001/" >> "\$ENV_FILE"
                                fi

                                echo "Final IP-based env values:"
                                grep "^HOST_IP=" "\$ENV_FILE" || true
                                grep "^APP_FRONTEND_URL=" "\$ENV_FILE" || true
                                grep "^APP_CORS_ALLOWED_ORIGINS=" "\$ENV_FILE" || true
                                grep "^GRAFANA_ROOT_URL=" "\$ENV_FILE" || true
                            '
                        """
                    }
                }
            }
        }

        stage('Verify ENV File') {
            steps {
                sshagent(credentials: ["${SSH_CRED_ID}"]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@${params.TARGET_IP} '
                            ENV=${DEPLOY_PATH}/.env.recruitmentsystem

                            if [ ! -f "\$ENV" ]; then
                                echo "ERROR: .env.recruitmentsystem file missing"
                                exit 1
                            else
                                echo ".env.recruitmentsystem file found"
                            fi

                            REQUIRED_VARS="APP_FRONTEND_URL APP_CORS_ALLOWED_ORIGINS APP_JWT_SECRET SPRING_MAIL_USERNAME SPRING_MAIL_PASSWORD APP_MAIL_FROM MYSQL_ROOT_PASSWORD MYSQL_DATABASE MYSQL_USER MYSQL_PASSWORD HOST_IP"

                            for VAR in \$REQUIRED_VARS; do
                                VALUE=\$(grep "^\${VAR}=" "\$ENV" | cut -d "=" -f2- | tr -d "[:space:]")
                                if [ -z "\$VALUE" ]; then
                                    echo "ERROR: \$VAR is missing or empty in .env.recruitmentsystem"
                                    exit 1
                                fi
                                echo "\$VAR is set"
                            done

                            echo "All required ENV variables verified."
                        '
                    """
                }
            }
        }

        stage('Build and Deploy') {
            steps {
                sshagent(credentials: ["${SSH_CRED_ID}"]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@${params.TARGET_IP} '
                            cd ${DEPLOY_PATH}

                            echo "Stopping old containers, keeping volumes to preserve DB data..."
                            sudo docker compose --env-file .env.recruitmentsystem down --remove-orphans || true

                            echo "Building Docker images..."
                            sudo docker compose --env-file .env.recruitmentsystem build --no-cache

                            echo "Starting containers..."
                            sudo docker compose --env-file .env.recruitmentsystem up -d

                            echo "Pruning dangling images..."
                            sudo docker image prune -f

                            echo "Running containers:"
                            sudo docker compose --env-file .env.recruitmentsystem ps
                        '
                    """
                }
            }
        }

        stage('Health Check') {
            steps {
                sshagent(credentials: ["${SSH_CRED_ID}"]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@${params.TARGET_IP} '
                            cd ${DEPLOY_PATH}

                            echo "Waiting for backend to be ready..."
                            max_attempts=30
                            attempt=0

                            while [ \$attempt -lt \$max_attempts ]; do
                                STATUS=\$(curl -s -o /dev/null -w "%{http_code}" \\
                                    http://localhost:8080/actuator/health 2>/dev/null || echo "000")

                                if [ "\$STATUS" = "200" ]; then
                                    echo "Backend is healthy - HTTP status: \$STATUS"
                                    break
                                fi

                                attempt=\$((attempt+1))
                                echo "Attempt \$attempt/\$max_attempts - HTTP \$STATUS, retrying in 5s..."
                                sleep 5
                            done

                            if [ \$attempt -eq \$max_attempts ]; then
                                echo "ERROR: Backend health check failed after \$max_attempts attempts"
                                sudo docker compose --env-file .env.recruitmentsystem logs backend --tail=50
                                exit 1
                            fi

                            echo "Final container status:"
                            sudo docker compose --env-file .env.recruitmentsystem ps
                        '
                    """
                }
            }
        }

        stage('Monitoring Check') {
            steps {
                sshagent(credentials: ["${SSH_CRED_ID}"]) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@${params.TARGET_IP} '
                            echo "Checking monitoring stack locally on EC2..."

                            curl -fsS http://localhost:9090/-/ready  >/dev/null && echo "Prometheus: OK"
                            curl -fsS http://localhost:9100/metrics  >/dev/null && echo "Node Exporter: OK"
                            curl -fsS http://localhost:3001/api/health >/dev/null && echo "Grafana: OK"

                            echo "All monitoring services are reachable locally."
                        '
                    """
                }
            }
        }
    }

    post {
        success {
            echo "========================================="
            echo "Deployment SUCCESSFUL — Build #${BUILD_NUMBER}"
            echo "Deployed Server: ${params.TARGET_IP}"
            echo "Frontend  : http://${params.TARGET_IP}"
            echo "Backend   : http://${params.TARGET_IP}:8080"
            echo "Grafana   : http://${params.TARGET_IP}:3001"
            echo "Prometheus: http://${params.TARGET_IP}:9090"
            echo "========================================="
        }

        failure {
            echo "Deployment FAILED at build #${BUILD_NUMBER}. Check stage logs above."
        }

        always {
            echo "Pipeline finished."
        }
    }
}