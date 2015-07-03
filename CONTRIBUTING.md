
## Project Local Repository

All dependencies are managed locally so that field folks can run a build
with no external dependencies, could compile without Maven, etc. The means
to import a new dependency is Maven-based however, and the command to do
so is as follows:

    mvn install:install-file
        -DlocalRepositoryPath=repo
        -DcreateChecksum=true
        -DgroupId=io.dropwizard.metrics
        -DartifactId=metrics-core
        -Dversion=3.1.2
        -Dpackaging=jar
        -Dfile=/Users/rbuck/Downloads/metrics-core-3.1.2.jar
        -Djavadoc=/Users/rbuck/Downloads/metrics-core-3.1.2-javadoc.jar
        -Dsources=/Users/rbuck/Downloads/metrics-core-3.1.2-sources.jar

This does require those importing artifacts to download the authoritative
artifacts from Maven Central located here:

    http://search.maven.org/

The current list of local dependencies are:

| Name | Group Id | Artifact Id | Version | License | Purpose |
| :-------------------- |:---------------------------------| ---------------------|
| Dropwizard Metrics | io.dropwizard.metrics | metrics-core | 3.1.2 | Apache 2.0 | Metrics Monitoring |


