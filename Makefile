IMAGE_NAME=crowd-ldap-server
IMAGE_VERSION=latest
IMAGE_TAG=aservo/$(IMAGE_NAME):$(IMAGE_VERSION)
DIST_VERSION=centos7

build:
	docker build \
		--build-arg DIST_VERSION=$(DIST_VERSION) \
		--tag $(IMAGE_TAG) \
		.

build-nc:
	docker build \
		--no-cache \
		--build-arg DIST_VERSION=$(DIST_VERSION) \
		--tag $(IMAGE_TAG) \
		.

push:
	docker push \
		$(IMAGE_TAG)

.PHONY: build build-nc push
