#!/bin/bash

set -euo pipefail

# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} \
  -Dfile.encoding=UTF-8 \
  -classpath "/opt/app/lib/*" \
  "de.aservo.ldap.adapter.Main" "$@"
