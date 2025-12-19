# Wallet Service

---

## API Endpoints

### 1. Process Wallet Operation (Deposit/Withdraw)

**Endpoint**: `POST /api/v1/wallet`

**Request Body**:
```json
{
  "operationType": "DEPOSIT",
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 100.00
}
```

**Response**:
```json
{
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "balance": 100.00
}
```

### 2. Get Wallet Balance

**Endpoint**: `GET /api/v1/wallets/{walletId}`

**Response**:
```json
{
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "balance": 100.00
}
```

---

##  Postman API Screenshots

### Collection Variables Setup
Configure the following variables in your Postman collection:
- `baseUrl`: http://localhost:8080
- `walletId`: Your wallet ID (e.g., 550e8400-e29b-41d4-a716-446655440000)

![Postman Collection Variables](screenshots/postman-0.png)

### Deposit Operation
![Deposit API](screenshots/postman-1.png)

### Get Balance
![Withdraw API](screenshots/postman-2.png)

### Withdraw Operation
![Get Balance API](screenshots/postman-3.png)

### Deposit More Funds
![Wallet Not Found](screenshots/postman-4.png)

### Error Handling - Insufficient Funds
![Insufficient Funds](screenshots/postman-5.png)

### Error Handling - Wallet Not Found
![Invalid Amount](screenshots/postman-6.png)

### Concurrent Transactions Test
![Invalid Operation](screenshots/postman-7.png)
![Concurrent Test 1](screenshots/postman-8.png)
![Concurrent Test 2](screenshots/postman-9.png)

### All Test Cases Execution
![Test Cases](screenshots/tests.png)

---

##  Deployment Options

### Option 1: Docker Deployment (Recommended)


**Quick Start:**
```bash
# Clone the repository
git clone https://github.com/RachitJava/wallet-services.git
cd wallet-services

# Configure environment variables
cp env.template .env
# Edit .env file if needed

# Start the application (note: space between docker and compose)
docker compose up -d

# View logs
docker compose logs -f

# Application will be available at http://localhost:8080
```

### Docker Containers Running
![Docker Containers](screenshots/docker.png)

** For detailed Docker documentation, see [DOCKER.md](DOCKER.md)**

### Option 2: Local Development Setup

If you prefer to run the application directly on your machine, follow the detailed local setup guide below.

---

# Local Setup Guide

## Prerequisites & Software Installation

### 1. Install Java 17

#### macOS
```bash
# Check if Java 17 is installed
java -version

# Install using Homebrew
brew install openjdk@17

# Add to PATH (add to ~/.zshrc)
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
```

#### Windows
```powershell
# Check if Java 17 is installed
java -version

# Download and install from:
# https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
# OR download Amazon Corretto 17:
# https://corretto.aws/downloads/latest/amazon-corretto-17-x64-windows-jdk.msi

# After installation, verify:
java -version
# Should show: openjdk version "17.x.x"

# Set JAVA_HOME (System Properties > Environment Variables)
# Add: JAVA_HOME = C:\Program Files\Java\jdk-17
# Add to Path: %JAVA_HOME%\bin
```

---

### 2. Install PostgreSQL 16

#### macOS
```bash
# Install using Homebrew
brew install postgresql@16

# Start PostgreSQL service
brew services start postgresql@16

# Add to PATH (add to ~/.zshrc)
export PATH="/opt/homebrew/opt/postgresql@16/bin:$PATH"

# Verify installation
psql --version
```

#### Windows
```powershell
# Download PostgreSQL 16 installer from:
# https://www.postgresql.org/download/windows/
# Or direct download:
# https://www.enterprisedb.com/downloads/postgres-postgresql-downloads

# Run the installer (postgresql-16.x-windows-x64.exe)
# During installation:
# - Set password for postgres user (remember this!)
# - Port: 5432 (default)
# - Locale: Default
# - Install pgAdmin 4 (optional but recommended)

# After installation, add to PATH:
# System Properties > Environment Variables > Path
# Add: C:\Program Files\PostgreSQL\16\bin

# Verify installation (in Command Prompt or PowerShell)
psql --version

# Should show: psql (PostgreSQL) 16.x
```

**Start PostgreSQL Service on Windows:**
```powershell
# Option 1: Using Services
# Press Win + R, type 'services.msc'
# Find 'postgresql-x64-16' service
# Right-click > Start (or set to Automatic)

# Option 2: Using Command Prompt (as Administrator)
net start postgresql-x64-16

# Check if service is running
sc query postgresql-x64-16
```

---

### 3. Install IntelliJ IDEA (or any Java IDE)

**Both macOS & Windows:**
- Download from: https://www.jetbrains.com/idea/download/
- Choose Community Edition (free) or Ultimate Edition
- Follow installation wizard
- On first launch, configure JDK to point to Java 17

---

### 4. Install Postman

**Both macOS & Windows:**
- Download from: https://www.postman.com/downloads/
- Install and launch
- Create free account (optional)
- We can import Wallet-Service-Postman-Collection.json in postman this collection is at same location where we have pom.xml

---

## Database Setup

### Create Production Database

#### macOS
```bash
# Create database
createdb walletdb

# Create user and grant privileges
psql walletdb -c "CREATE USER wallet_user WITH PASSWORD 'wallet_pass';"
psql walletdb -c "GRANT ALL PRIVILEGES ON DATABASE walletdb TO wallet_user;"
psql walletdb -c "ALTER DATABASE walletdb OWNER TO wallet_user;"
```

#### Windows (PowerShell or CMD)
```powershell
# Login as postgres user (enter password when prompted)
psql -U postgres

# Inside psql prompt:
CREATE DATABASE walletdb;
CREATE USER wallet_user WITH PASSWORD 'wallet_pass';
GRANT ALL PRIVILEGES ON DATABASE walletdb TO wallet_user;
ALTER DATABASE walletdb OWNER TO wallet_user;

# Exit psql
\q
```

**OR using single command (Windows):**
```powershell
# Create database
psql -U postgres -c "CREATE DATABASE walletdb;"

# Create user and grant privileges
psql -U postgres -d walletdb -c "CREATE USER wallet_user WITH PASSWORD 'wallet_pass';"
psql -U postgres -d walletdb -c "GRANT ALL PRIVILEGES ON DATABASE walletdb TO wallet_user;"
psql -U postgres -d walletdb -c "ALTER DATABASE walletdb OWNER TO wallet_user;"
```

---

### Verify Databases

#### macOS
```bash
# List databases
psql -l

# You should see:
# walletdb      | wallet_user
```

#### Windows
```powershell
# List databases
psql -U postgres -l

# OR connect and list
psql -U postgres
\l

# You should see:
# walletdb      | wallet_user

# Exit
\q
```

---

### Set Password Environment Variable (Optional - for easier access)

#### macOS
```bash
# Add to ~/.zshrc or ~/.bash_profile
export PGPASSWORD='wallet_pass'

# Or create .pgpass file
echo "localhost:5432:*:wallet_user:wallet_pass" > ~/.pgpass
chmod 600 ~/.pgpass
```

#### Windows
```powershell
# Option 1: Set environment variable
# System Properties > Environment Variables
# Add User Variable: PGPASSWORD = wallet_pass

# Option 2: Create pgpass.conf file
# Location: %APPDATA%\postgresql\pgpass.conf
# Content: localhost:5432:*:wallet_user:wallet_pass

# In PowerShell (one-time for session):
$env:PGPASSWORD = "wallet_pass"
```

---

## Running the Application

### In IntelliJ IDEA

1. **Open Project**
   - File → Open → Select `/Users/rachit/wallet-service`

2. **Wait for Maven to Download Dependencies**
   - IntelliJ will automatically download dependencies from `pom.xml`

3. **Run Application**
   - Navigate to `src/main/java/com/wallet/WalletServiceApplication.java`
   - Right-click → Run 'WalletServiceApplication'
   - OR click the green play button ▶

4. **Application Started**
   - Watch console for: `Started WalletServiceApplication`
   - Liquibase will run automatically and create tables
   - Application runs on: `http://localhost:8080`

### Verify Liquibase Migrations
Check the console logs for:
```
Liquibase: Table wallets created
Liquibase: Index idx_wallets_wallet_id created
Liquibase: Successfully released change log lock
```

Check database:
```bash
export PGPASSWORD='wallet_pass'
psql -U wallet_user -d walletdb -c "\dt"
```

You should see:
- `wallets` - Main wallet table
- `databasechangelog` - Liquibase tracking
- `databasechangeloglock` - Liquibase locking

---

## Project Architecture & Flow

### High-Level Architecture

```
┌─────────────────┐
│   Postman/UI    │
└────────┬────────┘
         │ HTTP REST
         ▼
┌─────────────────────────────────────┐
│       WalletController              │  ← REST Layer
│  - POST /api/v1/wallet              │
│  - GET  /api/v1/wallets/{id}        │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│       WalletService                 │  ← Business Logic Layer
│  - processOperation()               │
│  - getWalletBalance()               │
│  - Concurrency handling             │
│  - Transaction management           │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│     WalletRepository                │  ← Data Access Layer
│  - findById()                       │
│  - findByIdWithLock()  (PESSIMISTIC)│
│  - save()                           │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│       PostgreSQL Database           │  ← Persistence Layer
│  Tables:                            │
│  - wallets                          │
│  - databasechangelog (Liquibase)    │
└─────────────────────────────────────┘


