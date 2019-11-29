IMAGE_NAME=crowd-ldap-server
IMAGE_VERSION=latest
IMAGE_TAG=aservo/$(IMAGE_NAME):$(IMAGE_VERSION)

build:
	docker build \
		--tag $(IMAGE_TAG) \
		.

build-nc:
	docker build \
		--no-cache \
		--tag $(IMAGE_TAG) \
		.

push:
	docker push \
		$(IMAGE_TAG)

.PHONY: build build-nc push
