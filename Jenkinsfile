pipeline {
    agent { label 'Recruitment-Server' }
    environment {
        APP_NAME    = "recruitmentsystem"
        DEPLOY_PATH = "/home/ubuntu/workspace/recruitmentsystem"
    }
    stages {
        stage('Git Cloning') {
            steps {
                echo "Cloning repository from GitHub..."
                sh "mkdir -p ${DEPLOY_PATH}"
                dir("${DEPLOY_PATH}") {
                    git branch: 'main',
                        url: 'https://github.com/ChaitraBiradar10/Recruitment-system.git'
                }
            }
        }
        stage('Prepare ENV File') {
            steps {
                withCredentials([file(credentialsId: 'recruitment-env-file', variable: 'ENV_FILE_PATH')]) {
                    sh """
                        cp "\$ENV_FILE_PATH" ${DEPLOY_PATH}/.env.recruitmentsystem
                        chmod 600 ${DEPLOY_PATH}/.env.recruitmentsystem

                        # Auto-detect the current public IP of this EC2 instance
                        # This handles the case where the instance was restarted and got a new IP
                        CURRENT_IP=\$(curl -sf --max-time 5 http://169.254.169.254/latest/meta-data/public-ipv4 || \
                                      curl -sf --max-time 5 https://api.ipify.org || \
                                      echo "")

                        if [ -z "\$CURRENT_IP" ]; then
                            echo "WARNING: Could not auto-detect public IP. Using HOST_IP from env file as-is."
                        else
                            echo "Detected public IP: \$CURRENT_IP"

                            # Update HOST_IP in the env file
                            sed -i "s|^HOST_IP=.*|HOST_IP=\$CURRENT_IP|" ${DEPLOY_PATH}/.env.recruitmentsystem

                            # Update the URL fields that depend on the IP
                            sed -i "s|^APP_FRONTEND_URL=.*|APP_FRONTEND_URL=http://\$CURRENT_IP|" ${DEPLOY_PATH}/.env.recruitmentsystem
                            sed -i "s|^APP_CORS_ALLOWED_ORIGINS=.*|APP_CORS_ALLOWED_ORIGINS=http://\$CURRENT_IP|" ${DEPLOY_PATH}/.env.recruitmentsystem
                            sed -i "s|^GRAFANA_ROOT_URL=.*|GRAFANA_ROOT_URL=http://\$CURRENT_IP:3001/|" ${DEPLOY_PATH}/.env.recruitmentsystem

                            echo "ENV file updated with current IP: \$CURRENT_IP"
                        fi

                        echo ".env.recruitmentsystem ready"
                    """
                }
            }
        }
        stage('Verify ENV File') {
            steps {
                sh """
                    if [ ! -f ${DEPLOY_PATH}/.env.recruitmentsystem ]; then
                        echo "ERROR: .env.recruitmentsystem file missing"
                        exit 1
                    else
                        echo ".env.recruitmentsystem file found"
                    fi

                    # Validate all required variables are present and non-empty
                    for VAR in APP_FRONTEND_URL APP_CORS_ALLOWED_ORIGINS APP_JWT_SECRET \
                               SPRING_MAIL_USERNAME SPRING_MAIL_PASSWORD APP_MAIL_FROM \
                               MYSQL_ROOT_PASSWORD MYSQL_DATABASE MYSQL_USER MYSQL_PASSWORD; do
                        VALUE=\$(grep "^\${VAR}=" ${DEPLOY_PATH}/.env.recruitmentsystem | cut -d '=' -f2 | tr -d '[:space:]')
                        if [ -z "\$VALUE" ]; then
                            echo "ERROR: \$VAR is missing or empty in .env.recruitmentsystem"
                            exit 1
                        fi
                        echo "\$VAR is set"
                    done

                    HOST_IP=\$(grep '^HOST_IP=' ${DEPLOY_PATH}/.env.recruitmentsystem | cut -d '=' -f2 | tr -d '[:space:]')
                    echo "HOST_IP=\$HOST_IP"
                    echo "All required ENV variables verified."
                """
            }
        }
        stage('Setup EC2') {
            steps {
                sh """
                    echo "Checking Docker..."
                    if ! command -v docker &> /dev/null; then
                        echo "Installing Docker..."
                        sudo apt-get update -y
                        sudo apt-get install -y docker.io
                        sudo systemctl start docker
                        sudo systemctl enable docker
                        sudo usermod -aG docker ubuntu
                        echo "Docker installed"
                    else
                        echo "Docker already present"
                    fi
                    if ! docker compose version &> /dev/null; then
                        echo "Installing Docker Compose..."
                        sudo apt-get install -y docker-compose-v2
                        echo "Docker Compose installed"
                    else
                        echo "Docker Compose already present"
                    fi
                """
            }
        }
        stage('Build and Deploy') {
            steps {
                dir("${DEPLOY_PATH}") {
                    sh """
                        echo "Stopping old containers (keeping volumes to preserve DB data)..."
                        sudo docker compose --env-file .env.recruitmentsystem down --remove-orphans || true
                        echo "Building Docker images..."
                        sudo docker compose --env-file .env.recruitmentsystem build --no-cache
                        echo "Starting containers..."
                        sudo docker compose --env-file .env.recruitmentsystem up -d
                        echo "Pruning dangling images..."
                        sudo docker image prune -f
                        echo "Running containers:"
                        sudo docker compose --env-file .env.recruitmentsystem ps
                    """
                }
            }
        }
        stage('Health Check') {
            steps {
                sh """
                    echo "Waiting for backend to be ready..."
                    max_attempts=30
                    attempt=0
                    while [ \$attempt -lt \$max_attempts ]; do
                        STATUS=\$(curl -s -o /dev/null -w "%{http_code}" \
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
                        sudo docker compose --env-file ${DEPLOY_PATH}/.env.recruitmentsystem -f ${DEPLOY_PATH}/docker-compose.yml logs backend --tail=50
                        exit 1
                    fi
                    echo "Final container status:"
                    sudo docker compose --env-file ${DEPLOY_PATH}/.env.recruitmentsystem -f ${DEPLOY_PATH}/docker-compose.yml ps
                """
            }
        }
        stage('Monitoring Check') {
            steps {
                sh """
                    echo "Checking monitoring stack..."
                    curl -fsS http://localhost:9090/-/ready  >/dev/null && echo "Prometheus: OK"
                    curl -fsS http://localhost:9100/metrics  >/dev/null && echo "Node Exporter: OK"
                    curl -fsS http://localhost:3001/api/health >/dev/null && echo "Grafana: OK"
                    echo "All monitoring services are reachable."
                """
            }
        }
    }
    post {
        success {
            script {
                def ip = sh(script: "grep '^HOST_IP=' ${DEPLOY_PATH}/.env.recruitmentsystem | cut -d '=' -f2 | tr -d '[:space:]'", returnStdout: true).trim()
                echo "Deployment successful - Build #${BUILD_NUMBER}"
                echo "Frontend  : http://${ip}"
                echo "Backend   : http://${ip}:8080"
                echo "Grafana   : http://${ip}:3001"
                echo "Prometheus: http://${ip}:9090"
            }
        }
        failure {
            echo "Deployment failed at build #${BUILD_NUMBER}. Check stage logs above."
        }
        always {
            echo "Pipeline finished."
        }
    }
}