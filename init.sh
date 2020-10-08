#!/bin/bash

if [ -n "$VAULT_TOKEN" ] && [ -n "$VAULT_TOKEN_TYPE" ]; then
  VAULT_TOKEN="$VAULT_TOKEN_TYPE $VAULT_TOKEN"
fi

if [ -n "$LOGLEVEL" ]; then
  JAVA_OPTS="-Dloglevel=$LOGLEVEL $JAVA_OPTS"
fi

if [ -n "$XMS" ]; then
  JAVA_OPTS="-Xms$XMS $JAVA_OPTS"
fi

if [ -n "$XMX" ]; then
  JAVA_OPTS="-Xmx$XMX $JAVA_OPTS"
fi

if [ -n "$XSS" ]; then
  JAVA_OPTS="-Xss$XSS $JAVA_OPTS"
fi

if [ -n "$CROWD_APP_NAME" ]; then
  JAVA_OPTS="-Dapplication.name=$CROWD_APP_NAME $JAVA_OPTS"
fi

if [ -n "$VAULT_HEADER" ] && [ -n "$VAULT_TOKEN" ] && [ -n "$VAULT_URI_CROWD_APP_PASSWORD" ]; then
  CROWD_APP_PASSWORD="$(curl -sSL -H "$VAULT_HEADER: $VAULT_TOKEN" -XGET "$VAULT_URI_CROWD_APP_PASSWORD" | jq -r '.data.value')"
fi

if [ -n "$CROWD_APP_PASSWORD" ]; then
  JAVA_OPTS="-Dapplication.password=$CROWD_APP_PASSWORD $JAVA_OPTS"
fi

if [ -n "$CROWD_SERVER_URL" ]; then
  JAVA_OPTS="-Dcrowd.server.url=$CROWD_SERVER_URL $JAVA_OPTS"
fi

if [ -n "$CROWD_VALIDATION_INTERVAL" ]; then
  JAVA_OPTS="-Dsession.validationinterval=$CROWD_VALIDATION_INTERVAL $JAVA_OPTS"
fi

if [ -n "$SERVER_CACHE_DIR" ]; then
  JAVA_OPTS="-Dds-cache-directory=$SERVER_CACHE_DIR $JAVA_OPTS"
fi

if [ -n "$SERVER_BIND_ADDRESS" ]; then
  JAVA_OPTS="-Dbind.address=$SERVER_BIND_ADDRESS $JAVA_OPTS"
fi

if [ -n "$SERVER_ENTRY_CACHE_ENABLED" ]; then
  JAVA_OPTS="-Dentry-cache.enabled=$SERVER_ENTRY_CACHE_ENABLED $JAVA_OPTS"
fi

if [ -n "$SERVER_ENTRY_CACHE_MAX_SIZE" ]; then
  JAVA_OPTS="-Dentry-cache.max-size=$SERVER_ENTRY_CACHE_MAX_SIZE $JAVA_OPTS"
fi

if [ -n "$SERVER_ENTRY_CACHE_MAX_AGE" ]; then
  JAVA_OPTS="-Dentry-cache.max-age=$SERVER_ENTRY_CACHE_MAX_AGE $JAVA_OPTS"
fi

if [ -n "$SERVER_READINESS_CHECK" ]; then
  JAVA_OPTS="-Dreadiness-check=$SERVER_READINESS_CHECK $JAVA_OPTS"
fi

if [ -n "$SERVER_SSL_ENABLED" ]; then
  JAVA_OPTS="-Dssl.enabled=$SERVER_SSL_ENABLED $JAVA_OPTS"
fi

if [ -n "$SERVER_SSL_KEY_STORE_FILE" ]; then
  JAVA_OPTS="-Dssl.key-store-file=$SERVER_SSL_KEY_STORE_FILE $JAVA_OPTS"
fi

if [ -n "$SERVER_SSL_KEY_STORE_PASSWORD" ]; then
  JAVA_OPTS="-Dssl.key-store-password=$SERVER_SSL_KEY_STORE_PASSWORD $JAVA_OPTS"
fi

if [ -n "$SERVER_MODE_FLATTENING" ]; then
  JAVA_OPTS="-Dmode.flattening=$SERVER_MODE_FLATTENING $JAVA_OPTS"
fi

if [ -n "$VAULT_HEADER" ] && [ -n "$VAULT_TOKEN" ] && [ -n "$VAULT_URI_SSL_CRT" ] && [ -n "$VAULT_URI_SSL_KEY" ]; then
  curl -sSL -H "$VAULT_HEADER: $VAULT_TOKEN" -XGET "$VAULT_URI_SSL_CRT" | jq -r '.data.value' > "local.crt"
  curl -sSL -H "$VAULT_HEADER: $VAULT_TOKEN" -XGET "$VAULT_URI_SSL_KEY" | jq -r '.data.value' > "local.key"
  openssl pkcs12 -export -name "servercert" \
    -in "local.crt" -inkey "local.key" \
    -out local.keystore.p12 -passout "pass:changeit"
  keytool -importkeystore -srcstorepass "changeit" --deststorepass "changeit" \
    -srckeystore local.keystore.p12 -destkeystore local.keystore.jks \
    -srcstoretype pkcs12 -alias "servercert"
fi

export JAVA_OPTS

exec "$@"
