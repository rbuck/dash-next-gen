FROM openjdk:8-alpine AS staging

ARG VERSION

COPY build/distributions/dash-${VERSION}.tar.gz /tmp
WORKDIR /tmp

RUN set -eux && \
    tar xzf /tmp/dash-${VERSION}.tar.gz

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

COPY --from=staging /tmp/dash-${VERSION}/ /app/

ENTRYPOINT [ "docker-entrypoint.sh" ]

CMD [ "run" ]
