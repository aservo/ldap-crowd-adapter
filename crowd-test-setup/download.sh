#!/bin/bash

CROWD_VERSION="$1"

mkdir "$PWD/temp"

curl -sL "https://www.atlassian.com/software/crowd/downloads/binary/atlassian-crowd-$CROWD_VERSION.tar.gz" \
  | tar -C "$PWD/temp" -zxf -

ln -s "$PWD/temp/atlassian-crowd-$CROWD_VERSION" "$PWD/temp/atlassian-crowd"
