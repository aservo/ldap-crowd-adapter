#!/bin/bash

set -euo pipefail

docker stop crowd-test-container
docker rm crowd-test-container

exit 0
