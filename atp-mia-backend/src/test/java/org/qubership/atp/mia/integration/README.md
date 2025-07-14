## to run integration tests
### Prerequisites 
1. Installed Docker locally which version is higher 1.6.8
2. Mark recourses folder as Test Recourses Root (in IDEA)
### Locally with Docker Desktop
Run test with JAVA_OPTS: 
`
    -DLOCAL_DOCKER_START=true 
    -Dspring.profiles.active=it
`

If you wan to connect to ssh use this credentials:
`
    user = root` `password = test_password
`
If you run Mongo test then you can do it with your own local base (not container).
In case if you changed DB credentials for this run don't forget to add parameters:
`
    -DMIA_DB_ENABLE=true
    -DMIA_DB_USER=
    -DMIA_DB_PASSWORD=
    -DMIA_DB=
    -DMONGO_DB_ADDR=
    -DMONGO_DB_PORT=
`
In BaseIntegrationTestConfiguration you should set username and password for test:
`
@TestPropertySource(value = "classpath:application.properties",
        properties = {"management.server.port=0","gridfs.enable=true", "gridfs.username=", "gridfs.password="})
`        

### Locally with manual start of Docker containers (e.g. WSL Docker)
1) You need to start Postgres, Cassandra and SshServer containers (Mongo is not necessary).
You can find them for db: `atp-mia-backend/src/test/resources/db`, 
and for ssh: `atp-mia-backend/src/test/resources/sshserver`
 
For adding container you can use command:
```
cd <dir with Dockerfile>
sudo docker build -t <container name> .
```
For starting container use:
```sudo docker run -d -p 5432:5432 <container name>```

2) You need to get eth0 address from `ifconfig`, it will look like this:
```
eth0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
inet 127.0.0.1 netmask 0.0.0.0  broadcast 127.0.0.1
...
```
You need inet address `127.0.0.1` and run this command,

```netsh interface portproxy add v4tov4 listenport=${YOUR_PORT} listenaddress=0.0.0.0 connectport=${YOUR_PORT} connectaddress=${WSL_INSTANCE_IP}```

where replace `${WSL_INSTANCE_IP}` with `127.0.0.1` and `${YOUR_PORT}` with your port (for Postgres 5432).

This step is taken from here: [WSL + Docker setup guide](./docs/WSL_Docker_Guide.md)
3) Run test with JAVA_OPTS: 
`
    -DLOCAL_DOCKER_START=true 
    -Dspring.profiles.active=it
    -DPOSTGRES_JDBC_URL=jdbc:postgresql://localhost:5432/mia
    -DSSHSERVER_IP=127.0.0.1
    -DCASSANDRA_IP=127.0.0.1
    -DJDBC_URL=jdbc:postgresql://localhost:5432/mia
    -DMIA_USER=mia
    -DMIA_PASSWORD=mia
`
