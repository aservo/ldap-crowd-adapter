#!/bin/bash

export CROWD_INSTALL="$PWD/temp/atlassian-crowd"
CROWD_INSTANCE="$CROWD_INSTALL-instance"

echo "crowd.home=$CROWD_INSTANCE/data" >> "$CROWD_INSTALL/crowd-webapp/WEB-INF/classes/crowd-init.properties"

( cd "$CROWD_INSTALL" ; "./start_crowd.sh" & )
