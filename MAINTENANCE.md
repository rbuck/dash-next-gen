## Periodic Maintenance Tasks

This repository contains a local repository for dependencies. These
were added for field sales personnel could compile without Maven, etc.

As such all dependencies are manually managed, at least until such
time they move over to Maven. Some details on how to manage dependencies
are below.

Run this Maven goal to find out what dependencies are out of date:

    mvn versions:display-dependency-updates

Run this Maven goal to display the dependency tree:

    mvn dependency:tree

Run this Maven goal to list all dependencies:

    mvn dependency:resolve -o

To import a dependency into the local repository:

    mvn install:install-file \
        -DlocalRepositoryPath=repo \
        -DcreateChecksum=true \
        -Dpackaging=jar \
        -Dversion=3.1.2 \
        -DartifactId=metrics-core \
        -DgroupId=io.dropwizard.metrics \
        -Dfile=/Users/rbuck/Downloads/metrics-core-3.1.2.jar \
        -Djavadoc=/Users/rbuck/Downloads/metrics-core-3.1.2-javadoc.jar \
        -Dsources=/Users/rbuck/Downloads/metrics-core-3.1.2-sources.jar \
