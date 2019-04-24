VERSION:=$(shell jq -r .version package.json)

#:help: help        | Displays the GNU makefile help
.PHONY: help
help: ; @sed -n 's/^#:help://p' Makefile

#:help: version     | Displays the current release version (see package.json)
.PHONY: version
version:
	@echo $(VERSION)

#:help: all         | Runs the `clean` and `release` targets
.PHONY: all
all: clean package

#:help: package     | Creates a `package` Docker image
.PHONY: package
package:
#	mkdir -p .m2
	mkdir -p .gradle
	docker run -it --rm -v ${PWD}:/home/gradle -v ${PWD}/.gradle:/home/gradle/.gradle -w /home/gradle gradle:jdk8-alpine gradle assemble
	docker build --build-arg VERSION=$(VERSION) --build-arg JDBC_VERSION=20.0.0 -f Dockerfile -t rbuck/dash:$(VERSION) .
#	docker run -it --rm -v ${PWD}:/tmp -v ${PWD}/.m2:/root/.m2 -w /tmp maven:3.6.0-jdk-8-alpine mvn clean package
#	docker build --build-arg VERSION=$(VERSION) --build-arg JDBC_VERSION=20.0.0 -f Dockerfile -t rbuck/dash:$(VERSION) .
	docker tag rbuck/dash:$(VERSION) rbuck/dash:latest

#:help: publish     | Publishes a Docker image
.PHONY: publish
publish:
	docker push rbuck/dash:$(VERSION)
	docker push rbuck/dash:latest

#:help: clean       | Cleans up any build artifacts
.PHONY: clean
clean:
	-docker container prune --filter "until=24h" --force
	-docker rmi -f rbuck/dash:$(VERSION)
	-docker rmi -f rbuck/dash:latest
	-docker image prune -f
	-rm -fr build node_modules target