#!/bin/bash

INSTALL_DIR="$PWD/tmp/crowd-it-test-installation"
export CROWD_INSTALL="$INSTALL_DIR/atlassian-crowd"

( cd "$CROWD_INSTALL" ; "./stop_crowd.sh" -force )
