#!/bin/bash

set -euxo pipefail

HOSTNAME="$1"
PASSWORD="$2"
INSTALL_DIR="$3"

# shellcheck disable=SC2015
[[ -d "$INSTALL_DIR" ]] && rm -r "$INSTALL_DIR" || true
mkdir -p "$INSTALL_DIR"

cp "./ssl-test-setup/req.conf" "$INSTALL_DIR/req.conf"
echo "CN=$HOSTNAME" >> "$INSTALL_DIR/req.conf"

openssl req \
  -config "$INSTALL_DIR/req.conf" \
  -new \
  -keyout "$INSTALL_DIR/local.key" \
  -out "$INSTALL_DIR/local.csr" \
  -passout "pass:$PASSWORD"

openssl x509 -req -sha256 -days 7 \
  -signkey "$INSTALL_DIR/local.key" \
  -in "$INSTALL_DIR/local.csr" \
  -out "$INSTALL_DIR/local.crt" \
  -passin "pass:$PASSWORD"

openssl pkcs12 -export -name "servercert" \
  -inkey "$INSTALL_DIR/local.key" \
  -in "$INSTALL_DIR/local.crt" \
  -out "$INSTALL_DIR/local.keystore.p12" \
  -password "pass:$PASSWORD" \
  -passin "pass:$PASSWORD"

keytool -importkeystore \
  -srcstorepass "$PASSWORD" -deststorepass "$PASSWORD" \
  -srckeystore "$INSTALL_DIR/local.keystore.p12" -destkeystore "$INSTALL_DIR/local.keystore.jks" \
  -srcstoretype pkcs12 -alias "servercert"

exit 0
