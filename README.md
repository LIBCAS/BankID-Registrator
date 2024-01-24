# BankID-Registrator

## Local environment

- Live Reload
- PhpMyAdmin

### Setup with Docker

1. Create `.docker/local/.env` based on `.docker/local/.env.example`
2. Prepare your local DB based on `.docker/local/.env`
3. Start containers using `./env.sh local up`
4. Stop containers using `./env.sh local down`

## Testing environment

- No Live Reload
- No PhpMyAdmin
- DB running only in the container

### Deploy with Docker

1. `ssh` into the server
2. `cd` into the project directory and `git clone` the repo (skip these steps if already cloned)
3. `cd` into the project directory and `git pull` (pull often to keep the code updated)
4. Create `.docker/testing/.env` based on `.docker/testing/.env.example`
5. Start containers using `./env.sh testing up`
6. Stop containers using `./env.sh testing down`

### Checking the DB

1. `docker ps` and find the DB's container ID
2. Run `docker exec -it <_DB_container_id_> bash` to enter the container terminal of the DB
3. In the opened container terminal run `mysql -u root -p` and enter the password configured in `.docker/testing/.env`
4. Now u can run queries like `SHOW DATABASES;` etc.