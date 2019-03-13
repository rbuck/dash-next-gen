FROM maven:3.6.0-jdk-8-alpine AS build

COPY README.md /tmp/
COPY LICENSE /tmp/
COPY versions-maven-plugin-rules.xml /tmp/
COPY pom.xml /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN mvn versions:display-dependency-updates -Dversions.outputFile=target/outdated.txt
RUN mvn package
RUN tar xzf target/dash-1.10-SNAPSHOT-distribution.tar.gz

FROM openjdk:8-alpine AS release

ARG VERSION
ARG JDBC_VERSION

LABEL "name"="rbuck/dash" \
    "maintainer"="Robert Buck" \
    "version"="${VERSION}" \
    "release"="${VERSION}" \
    "vendor"="Robert Buck" \
    "summary"="Database Benchmark Framework" \
    "description"="A database benchmark framework."

ENV DRIVER_HOME /app

COPY docker-entrypoint.sh /bin

RUN set -eux && \
    apk add --no-cache bash curl && \
    mkdir -p /app

COPY --from=build /tmp/dash-ng-1.10-SNAPSHOT/ /app/

ENTRYPOINT [ "docker-entrypoint.sh" ]

CMD [ "run" ]
