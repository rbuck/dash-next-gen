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
	docker build --build-arg VERSION=$(VERSION) --build-arg JDBC_VERSION=20.0.0 -f Dockerfile -t rbuck/dash:$(VERSION) .
	docker tag rbuck/dash:$(VERSION) rbuck/dash:latest

#:help: publish     | Publishes a Docker image
.PHONY: publish
publish:
	docker push rbuck/dash:$(VERSION)
	docker push rbuck/dash:latest

#:help: clean       | Cleans up any build artifacts
.PHONY: clean
clean:
	-docker rm $(docker ps --all -q -f status=exited)
	-docker rm $(docker ps --all -q -f status=created)
	-docker rmi -f rbuck/dash:$(VERSION)
	-docker rmi -f rbuck/dash:latest
	-docker image prune -f
	-rm -fr build node_modules