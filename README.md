# BankID Registrator

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Coverage](https://img.shields.io/badge/coverage-37%25-orange)
![Java](https://img.shields.io/badge/java-11-blue.svg)
![Backend](https://img.shields.io/badge/backend-Spring%20Boot-blue)
![Frontend](https://img.shields.io/badge/frontend-Thymeleaf-blue)
![License](https://img.shields.io/badge/license-GPL_3.0-blue.svg)

---

## ✅ Local Environment

Features:
- 🔄 Live Reload
- 🛠️ PhpMyAdmin for DB inspection

### 🚀 Setup with Docker

1. Create `.docker/local/.env` based on `.docker/local/.env.example`.
2. Prepare your local database (credentials, schema, etc.) as per the `.env`.
3. Start the containers:
   ```sh
   ./env.sh local up
   ```
4. Stop the containers:
   ```sh
   ./env.sh local down
   ```

---

### 🧪 Running Tests in Local Environment

1. Make sure `src/test/resources/tests.properties` exists.
   - If not, create it by copying from `src/test/resources/tests.properties.example`.
   - Fill in the required test credentials (e.g., for LDAP).
2. Run the app container:
   ```sh
   docker exec -it bankid-app bash
   ```
3. Run all tests inside the container:
   ```sh
   ./mvnw test
   ```

---

## 🧪 Testing Environment

Features:
- ❌ No Live Reload
- ❌ No PhpMyAdmin
- ⚠️ Database only accessible from within the container

### 🐳 Deploy with Docker

1. SSH into the testing server:
   ```sh
   ssh username@your-server
   ```
2. Navigate to the project directory. If not cloned yet:
   ```sh
   git clone https://github.com/your-username/bankid-registrator.git
   cd bankid-registrator
   ```
3. Pull the latest code (do this regularly):
   ```sh
   git pull
   ```
4. Create `.docker/testing/.env` based on `.docker/testing/.env.example`.
5. Start the containers:
   ```sh
   ./env.sh testing up
   ```
6. Stop the containers:
   ```sh
   ./env.sh testing down
   ```

---

### 🗃️ Checking the Database

1. List running containers:
   ```sh
   docker ps
   ```
2. Enter the DB container:
   ```sh
   docker exec -it bankid-database bash
   ```
3. Access MySQL:
   ```sh
   mysql -u root -p
   ```
4. Run your queries:
   ```sql
   SHOW DATABASES;
   ```

---

## 🚀 Production Deployment

> ⚠️ Ensure that sensitive `.env` files are securely stored and never committed to the repository.

### 🔐 Secure Setup & Deployment

1. SSH into the production server.
2. Clone the repo or pull latest changes:
   ```sh
   git pull
   ```
3. Create the environment file:
   ```sh
   cp .docker/production/.env.example .docker/production/.env
   ```
   - Fill in all production secrets: database credentials, JWT secret, BankID client ID/secret, etc.
4. Start the app:
   ```sh
   ./env.sh production up
   ```
5. To stop:
   ```sh
   ./env.sh production down
   ```

### 🔍 Post-Deployment Checklist

- Check logs: `docker logs bankid-app`
