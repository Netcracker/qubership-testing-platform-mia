# Qubership Testing Platform MIA

For requirement that uses Business Solution and Billing System to perform operations such as Create Customers, Generate events, Create sales order, Rating, Billing, Invoice Generation, Revenue, Post billing financials etc. on different environments and on different databases. For these requirements, user needs to execute SSH and SQL commands. User needs to upload and download required files and at times extract data from SQL and use it as input in commands to execute. User needs to send REST and SOAP commands.

MIA provides users to offer inputs in various types such as text, list, checkbox, checkbox group, mask, date, memo, ace and attachment. User has option to refer to another inputs and make them mandatory or not.

MIA provides users to validate SQL query results post execution of command. User has option to refer to another commands and also provide markers

MIA provides users to execute pre-requisites of type-SSH, SQL and Command on host. User has option to refer to another inputs and another commands

MIA provides user to execute current statement[SQL commands] and use this as inputs in commands to execute.

## Requirements

### Service Requirements

| Service   | Component | vCPU | RAM   | HDD   | Version |
|-----------|-----------|------|-------|-------|---------|
| ATP MIA   | system    | 2    | 3 GB  | 2 GB  | JAVA 21 |

### Postgres Requirements

| Component | vCPU | RAM   | HDD   | Version |
|-----------|------|-------|-------|---------|
| Postgres  | 1    | 3 GB  | 10 GB | 9.6+    |
| system    | 1    | 2 GB  | 2 GB  | JAVA 21 |

## Non-Functional Requirements

- **Max number of Process/Compound executions at the same time for all projects:** 30
- **Max time response for simple SQL execution (including DB connection):** 30 seconds
- **Max number of Rate Matrix (process with test data file) executions at the same time for all projects:** 5

### Test Data Constraints

- Excel file size: **1 MB**
- Max number of rows on Main sheet: **7200**

# How to start Backend
 
 
(in some case with flag -DskipTests) 

1. Main class: org.qubership.atp.mia.Main
2. VM options: 
```
-DconfigPath=test
-Dspring.datasource.url=jdbc:postgresql://localhost:5432/mia
-Dspring.datasource.username=mia
-Dspring.datasource.password=mia
-Dspring.config.location=src\main\config\application.properties
-Dspring.cloud.bootstrap.location=src\main\config\bootstrap.properties
-Djavax.net.ssl.keyStore=src\main\config\keystore.p12
-Djavax.net.ssl.keyStorePassword=123456
-DFEIGN_ATP_ENVIRONMENTS_URL=http://environments:8080
-Dspring.liquibase.change-log=file:src/main/config/db/changelog/changelog-main.xml
```

3. Working directory: $MODULE_WORKING_DIR$
4. Use classpath or module: atp-mia-backend
5. In application.properties, Make Sure you have provided connection parameters of Running Postgres DataBase

Just run Main#main with args from step above

# How to deploy tool

1. Build snaphot (artifacts and docker image) of https://github.com/Netcracker/qubership-testing-platform-mia in GitHub
2. Clone repository to a place, available from your openshift/kubernetes where you need to deploy the tool to
3. Navigate to <repository-root>/deployments/charts/atp-mia folder
4. Check/change configuration parameters in the ./values.yaml file according to your services installed
5. Execute the command: helm install qstp-mia
6. After installation is completed, check deployment health
