#!/bin/bash

exec java $JAVA_OPTS -Dfile.encoding=UTF-8 -classpath "/app/lib/*" "de.aservo.ldap.adapter.Main" "$@"
