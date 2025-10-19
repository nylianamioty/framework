To setup the initial project using `maven`
```bash
mvn archetype:generate \
    -DgroupId=com.giga.spring \
    -DartifactId=gigaspring-lab \
    -DarchetypeArtifactId=maven-archetype-webapp \
    -DinteractiveMode=false
```

After `docker compose` and `Dockerfile` are set up the **latest** project can be deployed to `apache tomcat`:
```bash
./run.sh
```