# AWS EC2 Deployment With Jenkins and Monitoring

This repo is configured for two EC2 instances:

- Jenkins EC2: runs Jenkins in a container and executes the pipeline.
- Application EC2: runs the recruitment system containers, Prometheus, Grafana, and node-exporter.

## Application EC2 Setup

Create the production env file on the application EC2:

```bash
cp .env.production.example /home/ubuntu/.env.recruitment
nano /home/ubuntu/.env.recruitment
```

Set real values for `HOST_IP`, database passwords, mail credentials, `APP_JWT_SECRET`, and Grafana admin credentials.

Open these inbound ports in the application EC2 security group:

- `22` from the Jenkins EC2 security group or Jenkins EC2 private IP.
- `80` for the frontend.
- `8080` for backend API access, if direct API access is required.
- `3001` for Grafana.
- `9090` for Prometheus, only from trusted IPs.
- `9100` for node-exporter, preferably only from trusted IPs or keep private.

## Jenkins Setup

In Jenkins, create an SSH private key credential for the application EC2:

- Credential ID: `app-ec2-ssh-key`
- Username: `ubuntu`
- Private key: key that can SSH into the application EC2

Create a Pipeline job from SCM and point Jenkins at the root `Jenkinsfile`.

Set build parameters:

- `APP_EC2_HOST`: public IP or DNS of the application EC2.
- `APP_EC2_USER`: usually `ubuntu`.
- `DEPLOY_PATH`: `/home/ubuntu/recruitment-system`.
- `REMOTE_ENV_FILE`: `/home/ubuntu/.env.recruitment`.

The pipeline packages the checked-out repository, uploads it to the application EC2, installs Docker and Compose if needed, starts the app stack, and verifies:

- Backend: `http://localhost:8080/actuator/health`
- Prometheus: `http://localhost:9090/-/ready`
- node-exporter: `http://localhost:9100/metrics`
- Grafana: `http://localhost:3001/api/health`

## Service URLs

After a successful deployment:

- Frontend: `http://APP_EC2_HOST`
- Backend: `http://APP_EC2_HOST:8080`
- Prometheus: `http://APP_EC2_HOST:9090`
- Grafana: `http://APP_EC2_HOST:3001`

## Monitoring

Prometheus configuration is in `monitoring/prometheus.yml`.

Grafana provisioning is in `monitoring/grafana/provisioning` and includes:

- Prometheus datasource
- Recruitment System Overview dashboard
