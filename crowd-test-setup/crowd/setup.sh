#!/bin/bash

set -euo pipefail

docker run \
  --name crowd-test-container \
  --detach \
  --net=host \
  atlassian/crowd:latest

until $(curl -L --silent --output /dev/null --head --fail "http://localhost:8095/crowd/console"); do
  sleep 3
done

exit 0
