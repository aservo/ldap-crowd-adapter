Crowd LDAP Server
=================

Implementation of an LDAP server that delegates authentication to an Atlassian Crowd installation using the Crowd
REST API. The LDAP implementation is based on the Apache Directory Server.

### Configuration

* etc/backend.properties (Crowd)
* etc/log4j.properties (Logging)
* etc/server.properties (Crowd-LDAP-Adapter)

### Run

	sbt run

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
