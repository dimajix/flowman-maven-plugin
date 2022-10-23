# Flowman Maven Plugin

## Building

You can build your own version via Maven with
```shell
mvn clean install
```


## Using
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>kaya</groupId>
    <artifactId>quickstart</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>flowman</packaging>

    <name>Flowman Quickstart Example Project</name>

    <properties>
        <!-- Encoding related settings -->
        <encoding>UTF-8</encoding>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    </properties>

    <build>
        <plugins>
            <plugin>
                <!-- 2. Process project resources -->
                <groupId>com.dimajix.flowman</groupId>
                <artifactId>flowman-maven-plugin</artifactId>
                <version>0.1.0-SNAPSHOT</version>
                <extensions>true</extensions>
                <configuration>
                    <deploymentDescriptor>deployment.yml</deploymentDescriptor>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

The `deployment.yml` file looks as follows:
```yaml
flowman:
  version: 0.29.0
  
flows:
  - flow

deployments:
  dist:
    kind: dist
    installPath: /tmp/flowman-dist

  jar:
    kind: jar
    applicationPath: /tmp/flowman-jar/jar
    projectPath: /tmp/flowman-jar/flows
```


## Running

Building packages:
```shell
mvn clean compile
```

Executing Tests:
```shell
mvn test
```

Artifact deployment:
```shell
mvn deploy
```

```shell
mvn flowman:build -Dflowman.deployment=<deployment-name>
```

```shell
mvn flowman:test -Dflowman.deployment=<deployment-name>
```

```shell
mvn flowman:shell -Dflowman.deployment=<deployment-name> -Dflowman.flow=<flow>
```
