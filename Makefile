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
all: clean release

#:help: package     | Creates a `package` Docker image
.PHONY: package
package:
	docker build --build-arg VERSION=$(VERSION) --build-arg JDBC_VERSION=20.0.0 -f Dockerfile -t nuodb/autobahn:$(VERSION) .
	docker tag nuodb/autobahn:$(VERSION) nuodb/autobahn:latest

#:help: publish     | Publishes a Docker image
.PHONY: publish
publish:
	docker push nuodb/autobahn:$(VERSION)
	docker push nuodb/autobahn:latest

#:help: clean       | Cleans up any build artifacts
.PHONY: clean
clean:
	-docker rm $(docker ps --all -q -f status=exited)
	-docker rm $(docker ps --all -q -f status=created)
	-docker rmi -f nuodb/autobahn:$(VERSION)
	-docker image prune -f
	-rm -fr build node_modules