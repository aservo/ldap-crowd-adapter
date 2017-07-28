# Crowd LDAP Server

Implementation of an LDAP server that delegates authentication to an Atlassian Crowd installation
using the Crowd REST API. 

This service allows your favourite SSO authentication source to be used from many legacy devices, appliances and systems.

The LDAP implementation is based on the Apache Directory Server v1.5.7,  which is distributed under the Apache v2.0 License.

## Configuration

Crowd-LDAP-Server searcher for file `crowd-ldap-server.properties` in `etc` directory relative to current working directory.

Available properties:
`listener.port`
`ssl.enabled`
`ssl.keystore`
`ssl.certificate.password`

`emulate.ad.memberof`
`emulate.ad.include.nested`
`map.member.cn`
`map.member.ou`
`map.member.dc`
`map.member.gid`
