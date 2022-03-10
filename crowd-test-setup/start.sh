#!/bin/bash

set -euo pipefail

INSTALL_DIR="$PWD/tmp/crowd-it-test-installation"
export CROWD_INSTALL="$INSTALL_DIR/atlassian-crowd"
CROWD_INSTANCE="$CROWD_INSTALL-instance"

echo "crowd.home=$CROWD_INSTANCE/data" >>"$CROWD_INSTALL/crowd-webapp/WEB-INF/classes/crowd-init.properties"

(
  cd "$CROWD_INSTALL"
  "./start_crowd.sh"
)

exit 0
