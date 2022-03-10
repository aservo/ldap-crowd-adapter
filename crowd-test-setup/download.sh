#!/bin/bash

set -euo pipefail

CROWD_VERSION="$1"
INSTALL_DIR="$PWD/tmp/crowd-it-test-installation"

rm -r "$INSTALL_DIR" || true
mkdir -p "$INSTALL_DIR"

curl -sL "https://www.atlassian.com/software/crowd/downloads/binary/atlassian-crowd-$CROWD_VERSION.tar.gz" |
  tar -C "$INSTALL_DIR" -zxf -

ln -s "$INSTALL_DIR/atlassian-crowd-$CROWD_VERSION" "$INSTALL_DIR/atlassian-crowd"

exit 0
