#!/bin/bash

set -euo pipefail

VERSION="$(mvn --batch-mode help:evaluate -Dexpression=project.version | grep -v -E '^\[([A-Za-z])+\]')"

echo "IMAGE_VERSION=$VERSION" >> $GITHUB_ENV

exit 0
