#!/bin/sh
JAR=target/crowd-ldap-server-2.0.1-SNAPSHOT.jar
rm -rf work
jar -xvf  $JAR work/schema
