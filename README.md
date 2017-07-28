# Crowd LDAP Server

Implementation of an LDAP server that delegates authentication to an Atlassian Crowd installation
using the Crowd REST API. 

This service allows your favourite SSO authentication source to be used from many legacy devices, appliances and systems.

The LDAP implementation is based on the Apache Directory Server v1.5.7,  which is distributed under the Apache v2.0 License.

## Configuration

Crowd-LDAP-Server searcher for file `crowd-ldap-server.properties` in `etc` directory relative to current working directory.
See example in the distro.

Available properties:
| Name                        | Description | Default |
|-----------------------------| ------------|---------|
| `listener.port`             | Port the server listens to | 10389 |
| `ssl.enabled`               | LDAPS enabled? | false |
| `ssl.keystore`              | Path to keystore file | `etc/crowd-ldap-server.keystore` |
| `ssl.certificate.password`  | Certificate password | `changeit` |
| `emulate.ad.memberof`       | emulate ActiveDirectory | false |
| `emulate.ad.include.nested` | emulate nested groups for ActiveDirectory | false |
| `map.member.cn`             | | |
| `map.member.ou`             | | |
| `map.member.dc`             | | |
| `map.member.gid`            | | |

