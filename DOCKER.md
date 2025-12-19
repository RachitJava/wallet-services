# Docker Deployment Guide

This guide explains how to run the Wallet Service using Docker and Docker Compose.

##  Prerequisites

- Docker installed (version 20.10+)
- Docker Compose installed (version 2.0+)

### Install Docker

**macOS:**
```bash
# Using Homebrew
brew install --cask docker

# Or download from: https://www.docker.com/products/docker-desktop
```

**Linux:**
```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
```

**Windows:**
- Download Docker Desktop from: https://www.docker.com/products/docker-desktop

---

##  Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/RachitJava/wallet-services.git
cd wallet-services
```

### 2. Configure Environment Variables
```bash
# Copy the template to create your .env file
cp env.template .env

# Edit .env file with your preferred configuration
nano .env
```

### 3. Start the Application
```bash
# Build and start all services
docker compose up -d

# View logs
docker compose logs -f

# View specific service logs
docker compose logs -f wallet-service
docker compose logs -f postgres
```

### 4. Verify Application is Running
```bash
# Check service status
docker compose ps

# Check application health
curl http://localhost:8080/actuator/health
```

---

##  Configuration

All configuration is done through the `.env` file. You can modify these values without rebuilding containers.

### Database Configuration

```bash
POSTGRES_DB=walletdb           # Database name
POSTGRES_USER=wallet_user      # Database username
POSTGRES_PASSWORD=wallet_pass  # Database password
POSTGRES_PORT=5432             # Database port (host machine)
```

### Application Configuration

```bash
APP_PORT=8080                  # Application port (host machine)
```

### JPA/Hibernate Configuration

```bash
JPA_DDL_AUTO=validate          # Options: validate, update, create, create-drop
JPA_SHOW_SQL=false             # Show SQL queries in logs
JPA_FORMAT_SQL=false           # Format SQL queries in logs
```

### Liquibase Configuration

```bash
LIQUIBASE_ENABLED=true         # Enable/disable Liquibase migrations
```

### Logging Configuration

```bash
LOG_LEVEL_ROOT=INFO            # Root logging level
LOG_LEVEL_APP=DEBUG            # Application logging level
LOG_LEVEL_SPRING=INFO          # Spring framework logging level
LOG_LEVEL_HIBERNATE=WARN       # Hibernate logging level
```

### Database Connection Pool

```bash
DB_POOL_SIZE=10                # Maximum pool size
DB_MIN_IDLE=5                  # Minimum idle connections
DB_CONNECTION_TIMEOUT=30000    # Connection timeout (ms)
DB_IDLE_TIMEOUT=600000         # Idle timeout (ms)
```

### Actuator Endpoints

```bash
ACTUATOR_ENDPOINTS=health,info,metrics    # Exposed actuator endpoints
ACTUATOR_HEALTH_DETAILS=when-authorized   # Health details visibility
```

---

##  Docker Commands

### Basic Operations

```bash
# Start services
docker compose up -d

# Stop services
docker compose stop

# Stop and remove containers
docker compose down

# Stop and remove containers, volumes, and images
docker compose down -v --rmi all

# Restart services
docker compose restart

# Restart specific service
docker compose restart wallet-service
```

### Viewing Logs

```bash
# View logs from all services
docker compose logs

# Follow logs in real-time
docker compose logs -f

# View logs from specific service
docker compose logs wallet-service
docker compose logs postgres

# View last 100 lines
docker compose logs --tail=100 wallet-service
```

### Rebuilding

```bash
# Rebuild and restart services
docker compose up -d --build

# Rebuild specific service
docker compose build wallet-service
docker compose up -d wallet-service

# Force rebuild without cache
docker compose build --no-cache wallet-service
```

### Database Operations

```bash
# Connect to PostgreSQL
docker compose exec postgres psql -U wallet_user -d walletdb

# Run SQL query
docker compose exec postgres psql -U wallet_user -d walletdb -c "SELECT * FROM wallets;"

# Backup database
docker compose exec postgres pg_dump -U wallet_user walletdb > backup.sql

# Restore database
docker compose exec -T postgres psql -U wallet_user -d walletdb < backup.sql
```

### Container Management

```bash
# View running containers
docker compose ps

# View container resource usage
docker compose top

# Execute command in container
docker compose exec wallet-service sh

# View container configuration
docker compose config
```

---

##  Updating Configuration

To change configuration **without rebuilding containers**:

1. **Edit `.env` file**
   ```bash
   nano .env
   ```

2. **Restart services**
   ```bash
   docker compose restart
   ```

Example: Change application port
```bash
# Edit .env
APP_PORT=9090

# Restart
docker compose restart wallet-service
```

---

##  Architecture

### Container Structure

```
┌─────────────────────────────────┐
│     wallet-service              │
│  (Spring Boot Application)      │
│  Port: 8080 (configurable)      │
└──────────────┬──────────────────┘
               │
               │ JDBC Connection
               ▼
┌─────────────────────────────────┐
│     postgres                    │
│  (PostgreSQL 16)                │
│  Port: 5432 (configurable)      │
│  Volume: postgres_data          │
└─────────────────────────────────┘
```

### Network

- All services run on `wallet-network` (bridge network)
- Services communicate using service names
- Ports are mapped to host machine

### Volumes

- `postgres_data`: Persists PostgreSQL data
- Data survives container restarts
- Located at: `/var/lib/docker/volumes/wallet-service_postgres_data`

---

##  Troubleshooting

### Application Won't Start

```bash
# Check logs
docker compose logs wallet-service

# Check if database is ready
docker compose logs postgres | grep "database system is ready"

# Check container status
docker compose ps

# Restart services
docker compose restart
```

### Database Connection Issues

```bash
# Check if postgres is healthy
docker compose ps

# Check postgres logs
docker compose logs postgres

# Test database connection
docker compose exec postgres psql -U wallet_user -d walletdb -c "SELECT 1;"
```

### Port Already in Use

```bash
# Change ports in .env
APP_PORT=9090
POSTGRES_PORT=5433

# Restart services
docker compose down
docker compose up -d
```

### Clean Start (Remove all data)

```bash
# Stop and remove everything
docker compose down -v

# Start fresh
docker compose up -d
```

### View Container Resource Usage

```bash
# CPU, Memory usage
docker stats

# Specific container
docker stats wallet-service
```

---

##  Security Best Practices

1. **Change default passwords** in `.env`
2. **Never commit `.env`** file to git
3. **Use strong passwords** for production
4. **Limit exposed ports** (comment out in docker compose.yml if not needed)
5. **Use secrets management** for production (Docker Swarm secrets, Kubernetes secrets)

---

##  Testing

### API Testing with curl

```bash
# Health check
curl http://localhost:8080/actuator/health

# Process wallet operation
curl -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{
    "operationType": "DEPOSIT",
    "walletId": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 100.00
  }'

# Get wallet balance
curl http://localhost:8080/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000
```

### Using Postman

Import the collection:
```bash
# The collection is available at: Wallet-Service-Postman-Collection.json
# Update the base URL to: http://localhost:8080
```

---

##  Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Available Actuator Endpoints

Configured via `ACTUATOR_ENDPOINTS` in `.env`:
- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

---

##  Production Deployment

For production deployment, consider:

1. **Use environment-specific .env files**
   - `.env.production`
   - `.env.staging`

2. **Add reverse proxy** (nginx)
   ```yaml
   nginx:
     image: nginx:alpine
     ports:
       - "80:80"
       - "443:443"
   ```

3. **Enable HTTPS/TLS**

4. **Add monitoring** (Prometheus, Grafana)

5. **Configure backup strategy** for PostgreSQL

6. **Use Docker Swarm or Kubernetes** for orchestration

---

##  Notes

- First startup takes longer due to Maven dependency download and compilation
- Liquibase migrations run automatically on first startup
- Database data persists in Docker volume even after container restart
- Configuration changes require service restart but NOT rebuild
- Health checks ensure services start in correct order

---

##  Support

For issues or questions:
- Check logs: `docker compose logs -f`
- GitHub Issues: https://github.com/RachitJava/wallet-service/issues
- Review configuration in `.env` file

