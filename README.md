# Crowd LDAP Server

Implementation of an LDAP server that delegates authentication to an Atlassian Crowd installation
using the Crowd REST API. 

This service allows your favourite SSO authentication source to be used from many legacy devices, appliances and systems.

The LDAP implementation is based on the Apache Directory Server v1.5.7,  which is distributed under the Apache v2.0 License.

## Build

	mvn package

## Configuration

### Crowd Server

![设置密码](doc/Password.png)

![添加IP白名单](doc/Remote_Addresses.png)

### Crowd LDAP Server
vi etc/crowd.properties

	#Crowd Server Configuration
	session.lastvalidation=session.lastvalidation
	session.isauthenticated=session.isauthenticated
	application.password=<crowd application password>
	application.name=crowd-openid-server
	session.validationinterval=0
	crowd.server.url=http://127.0.0.1/crowd/services/
	session.tokenkey=session.tokenkey
	application.login.url=http://127.0.0.1/crowd/console/



## Run

	./run

## Test

	ldapsearch -v -x -D 'uid=jira,ou=users,dc=crowd' -W -H ldap://localhost:10389 -b ou=users,dc=crowd uid=jira

	ldapsearch -v -x -D 'uid=jira,ou=users,dc=crowd' -W -H ldap://localhost:10389 -b ou=users,dc=crowd uid=*