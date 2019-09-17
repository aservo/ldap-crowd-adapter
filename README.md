Crowd LDAP Server
=================

This project is an LDAP server implementation based on the Apache Directory Server. It allows authentication and
authorization by delegating queries to an Atlassian Crowd installation using the Crowd REST-API.

### How to start

This project depends on the sbt build tool and the Java SE Development Kit version 8.

For starting the server the backend and server configuration files should be edited.

* etc/backend.properties (Crowd)
* etc/server.properties (LDAP server)
* etc/log4j.properties (Logging)

To run the server after configuration, simply run the following command:

	sbt run

Alternatively, the server can be run in a Docker container.

    make build
    make push
    
    docker run \
      --detach \
      --net=host \
      --name crowd-ldap-server \
      --env "CROWD_APP_NAME=crowd-openid-server" \
      --env "CROWD_APP_PASSWORD=password" \
      --env "CROWD_SERVER_URL=http://localhost:8095/crowd/services/" \
      --env "SERVER_BIND_ADDRESS=localhost:10389" \
      aservo/crowd-ldap-server:latest

An overview of all configuration keys can be found in the file "start.sh". To ensure that the boot process was
successful, the following command can be used:

    docker logs crowd-ldap-server

For detailed information about configuration keys read the comments in configuration files.

### Maintenance history

* 2012 Dieter Wimberger (dwimberger)
* 2016 Jan Zdarsky (OneB1t)
* 2017 Alik Kurdyukov (akurdyukov)
* 2019 Eric Löffler (brettaufheber)

### License

Copyright (c) 2019 ASERVO Software GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

_http://www.apache.org/licenses/LICENSE-2.0_

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
