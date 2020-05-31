#!/bin/bash

CROWD_INSTALL="$PWD/temp/atlassian-crowd"

( cd "$CROWD_INSTALL" ; "./stop_crowd.sh" )
