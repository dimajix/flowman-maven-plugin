# Flowman Maven Utilities

The Flowman Maven utilities simplifies building deployable artifacts of [Flowman](https://flowman.io) projects by using
Apache Maven. Flowman is a build tool for data based on Apache Spark for creating both simple and complex data
transformation applications using a purely declarative approach.

By using Apache Maven as the build tool of choice, Flowman projects can be easily integrated in existing DevOps
pipelines, which normally provide a working Maven environment together with an artifact store like Nexus.

The Flowman Maven utilities consist of different parts:
* The Flowman Maven plugin for simplifying the build itself
* Additional Flowman providers for automating the deployment process to cloud environments like AWS S3, Azure Blob 
 storage, etc
* Maven Archetypes for quickly setting up a new Flowman project


## Maven Archetypes 

This project contains two Maven archetypes:

* `com.dimajix.flowman.maven:flowman-archetype-quickstart`: This archetype will create a small Flowman project using
Maven as the build system and using the Flowman Maven plugin. This is the preferred archetype as it simplifies creating
appropriate artifacts like Flowman distributions or fat jars.

* `com.dimajix.flowman.maven:flowman-archetype-assembly`: The second archetype will also create a small Flowman project
using Maven, but it only uses standard Maven plugins like the maven-dependency-plugin and the maven-assembly-plugin.
This archetype will only create a Flowman distribution `tar.gz` and cannot be used out of the box for creating a fat 
jar. Of course, explicitly using standard Maven plugins offers more flexibility, but also requires more expertise for
working with Maven.

### Creating a new Project from an Archetype

You can easily create a new Flowman project with one of the archetypes via the following Maven command:
```shell
mvn archetype:generate \
  -DarchetypeGroupId=com.dimajix.flowman.maven \
  -DarchetypeArtifactId=flowman-archetype-quickstart \
  -DgroupId=my.company.name \ 
  -DartifactId=my-artifact-name
```
This example will create a new Maven project `my-artifact-name` within the Maven group `my.company.name`. Since
the project is created from the archetype `flowman-archetype-quickstart`, it will use the Flowman Maven plugin
instead of standard Maven plugins.

The command will create a new directory `my-artifact-bane`, which looks as follows:
```
├── conf
│   ├── default-namespace.yml
│   └── flowman-env.sh
├── flow
│   ├── config
│   │   ├── aws.yml
│   │   ├── config.yml
│   │   ├── connections.yml
│   │   └── environment.yml
│   ├── documentation.yml
│   ├── job
│   │   └── main.yml
│   ├── mapping
│   │   └── measurements.yml
│   ├── model
│   │   ├── measurements-raw.yml
│   │   └── measurements.yml
│   ├── project.yml
│   ├── schema
│   │   └── measurements.json
│   ├── target
│   │   ├── documentation.yml
│   │   └── measurements.yml
│   └── test
│       └── test-measurements.yml
├── deployment.xml
├── pom.xml
└── README.md
```
You then can immediately start tinkering with the Flowman project defined in the `flow` directory.


## Using the Flowman Maven plugin  

The Flowman Maven plugin will greatly simplify packaging Flowman projects for deployment. A package will typically
contain both all required Flowman jar files and all the YAML files of your project(s). The Flowman Maven plugin currently
supports two different package types, either a *distribution* inside a `tar.gz` file or a *fat jar* inside a single
`jar` file.

Which of the two variants is better for your use case, depends on the infrastructure. A distribution inside a `tar.gz`
file contains all shell scripts and binaries and provides a more flexible user experience — but it requires a working
shell environment (i.e. SSH access to a Hadoop edge node). On the other hand, a fat jar is much simpler to use as a
complete, self-contained and directly executable artifact in managed cloud environments like AWS EMR or Azure Synapse.


### Maven `pom.xml` file

In order to use the Flowman Maven plugin, you have to create a corresponding `pom.xml` for Maven:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>my.company.name</groupId>
    <artifactId>my-artifact-name</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>Flowman Quickstart Example Project</name>

    <properties>
        <!-- Encoding related settings -->
        <encoding>UTF-8</encoding>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>com.dimajix.flowman.maven</groupId>
                <artifactId>flowman-maven-plugin</artifactId>
                <version>0.1.0</version>
                <extensions>true</extensions>
                <configuration>
                    <deploymentDescriptor>deployment.yml</deploymentDescriptor>
                </configuration>

                <!-- Additional optional plugin dependencies for specific deployment targets -->
                <dependencies>
                    <!-- Support for deploying to S3 storage -->
                    <dependency>
                      <groupId>com.dimajix.flowman.maven</groupId>
                      <artifactId>flowman-provider-aws</artifactId>
                      <version>0.1.0</version>
                    </dependency>
                    <!-- Support for deploying to Azure Blob Storage -->
                    <dependency>
                      <groupId>com.dimajix.flowman.maven</groupId>
                      <artifactId>flowman-provider-azure</artifactId>
                      <version>0.1.0</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

### Flowman `deployment.yml` file

The Flowman Maven plugin relies on an additional file. The `deployment.yml` provides detailed information about the
artifacts to be built and the settings to be used. You can define multiple different deployment packages, for example
for different execution environments (i.e. on premise Cloudera plus AWS EMR):

```yaml
flowman:
  # Specify the Flowman version to use
  version: 0.30.0-oss-spark3.3-hadoop3.3
  # Optional: Specify the full artifact to use
  distribution: com.dimajix.flowman:flowman-dist:bin:0.30.0-oss-spark3.3-hadoop3.3
  # Optional: Specify plugins  
  plugins:
    - flowman-avro
    - flowman-aws
    - flowman-mariadb
    - flowman-mysql
    # Other possible notations are groupId:artifactId[[[:classifier]:type]:version], i.e.
    #- com.dimajix.flowman:flowman-plugin-aws
    #- com.dimajix.flowman:flowman-plugin-aws:0.29.0-SNAPSHOT
    #- com.dimajix.flowman:flowman-plugin-aws:jar:0.29.0-SNAPSHOT
    #- groupId:artifactId
    #- groupId:artifactId:version
    #- groupId:artifactId:type:version
    #- groupId:artifactId:type:classifier:version
  # Optional: Specify Flowman config settings, which will be stored in the default-namespace.yml
  config:
    - flowman.execution.mapping.parallelism=4
  # Optional: Specify Flowman environment variables, which will be stored in the default-namespace.yml
  environment:
    - src_dir=s3a://dimajix-training/data/weather

# List of subdirectories containing Flowman projects
projects:
  - flow

packages:
  dist:
    kind: dist
    # Optional: Modify global Flowman settings, which will be stored in the default-namespace.yml
    flowman:
      config:
        - flowman.execution.mapping.parallelism=6
      environment:
        - src_dir=s3a://dimajix-training/data/weather

  jar:
    kind: fatjar
```

### Running

Now with all the files in place, you can start using Maven for building the project. The Flowman Maven plugin
supports the classical Maven commands like `compile`, `test`, `package`, `install`, `deploy`:

* Building packages:
```shell
mvn clean compile
```

* Executing all tests:
```shell
mvn test
```

* Deploying artifacts to a repository:
```shell
mvn deploy
```

Additionally, the Flowman Maven plugin supports some additional commands
```shell
# Only build a single package
mvn flowman:build -Dflowman.package=<package-name>
# Only test a single package
mvn flowman:test -Dflowman.package=<package-name>
# Run the Flowman shell for a single package
mvn flowman:shell -Dflowman.package=<package-name> -Dflowman.flow=<flow>
```
