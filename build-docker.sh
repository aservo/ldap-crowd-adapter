#!/bin/bash

set -euxo pipefail

VERSION="$(mvn --batch-mode help:evaluate -Dexpression=project.version | grep -v -E '^\[([A-Za-z])+\]')"

mvn --batch-mode clean package -DskipTests

docker build --no-cache --tag "aservo/ldap-crowd-adapter:$VERSION" .
docker build --no-cache --tag "aservo/ldap-crowd-adapter-rhel:$VERSION" --file "Dockerfile-rhel" .

exit 0
