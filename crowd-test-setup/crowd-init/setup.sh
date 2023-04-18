#!/bin/bash

set -euo pipefail

SELF_PATH="$(dirname "$(readlink -f "$0")")"

CROWD_SERVER_ID="$(
  docker exec crowd-test-container /bin/bash -c 'cat /var/atlassian/application-data/crowd/shared/crowd.cfg.xml' |
    grep -oPm1 '(?<=<property name="crowd.server.id">)[^<]+'
)"

docker build \
  --tag crowd-init:latest \
  "$SELF_PATH/docker"

docker run \
  --name crowd-init-test-container \
  --net=host \
  --env "CROWD_COOKIES=/tmp/crowd-cookies.txt" \
  --env "CROWD_SERVER_ID=$CROWD_SERVER_ID" \
  --env "CROWD_LICENSE_KEY=$CROWD_LICENSE_KEY" \
  --env "CROWD_HOST=http://localhost:8095" \
  crowd-init:latest

exit 0
