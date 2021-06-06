#!/bin/bash

# basic settings

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

# SSL settings

if [ -n "$SERVER_SSL_ENABLED" ]; then
  JAVA_OPTS="-Dssl.enabled=$SERVER_SSL_ENABLED $JAVA_OPTS"
fi

if [ -n "$SERVER_SSL_KEY_STORE_FILE" ]; then
  JAVA_OPTS="-Dssl.key-store-file=$SERVER_SSL_KEY_STORE_FILE $JAVA_OPTS"
fi

if [ -n "$SERVER_SSL_KEY_STORE_PASSWORD" ]; then
  JAVA_OPTS="-Dssl.key-store-password=$SERVER_SSL_KEY_STORE_PASSWORD $JAVA_OPTS"
fi

if [ -n "$SSL_CRT" ] && [ -n "$SSL_KEY" ]; then
  if [ -z "$SERVER_KEYSTORE_PASSWORD" ]; then
    SERVER_KEYSTORE_PASSWORD="$(openssl rand -hex 42)"
  fi
  JAVA_OPTS="-Dssl.key-store-password=$SERVER_KEYSTORE_PASSWORD $JAVA_OPTS"
  echo "$SSL_CRT" > "local.crt"
  echo "$SSL_KEY" | openssl pkcs12 -export -name "servercert" \
    -in "local.crt" -inkey "/dev/stdin" \
    -out "local.keystore.p12" -passout "pass:$SERVER_KEYSTORE_PASSWORD"
  keytool -importkeystore -srcstorepass "$SERVER_KEYSTORE_PASSWORD" --deststorepass "$SERVER_KEYSTORE_PASSWORD" \
    -srckeystore "local.keystore.p12" -destkeystore "local.keystore.jks" \
    -srcstoretype pkcs12 -alias "servercert"
fi

# server settings

if [ -n "$SERVER_CACHE_DIR" ]; then
  JAVA_OPTS="-Dds-cache-directory=$SERVER_CACHE_DIR $JAVA_OPTS"
fi

if [ -n "$SERVER_BIND_ADDRESS" ]; then
  JAVA_OPTS="-Dbind.address=$SERVER_BIND_ADDRESS $JAVA_OPTS"
fi

if [ -n "$SERVER_MODE_FLATTENING" ]; then
  JAVA_OPTS="-Dmode.flattening=$SERVER_MODE_FLATTENING $JAVA_OPTS"
fi

if [ -n "$SERVER_RESPONSE_MAX_SIZE_LIMIT" ]; then
  JAVA_OPTS="-Dmode.response.max-size-limit=$SERVER_RESPONSE_MAX_SIZE_LIMIT $JAVA_OPTS"
fi

if [ -n "$SERVER_RESPONSE_MAX_TIME_LIMIT" ]; then
  JAVA_OPTS="-Dmode.response.max-time-limit=$SERVER_RESPONSE_MAX_TIME_LIMIT $JAVA_OPTS"
fi

if [ -n "$SERVER_CONNECTION_BACK_LOG" ]; then
  JAVA_OPTS="-Dmode.connection.back-log=$SERVER_CONNECTION_BACK_LOG $JAVA_OPTS"
fi

if [ -n "$SERVER_RESPONSE_ACTIVE_THREADS" ]; then
  JAVA_OPTS="-Dmode.connection.active-threads=$SERVER_RESPONSE_ACTIVE_THREADS $JAVA_OPTS"
fi

if [ -n "$SERVER_DIRECTORY_BACKEND_PERMANENT" ]; then
  JAVA_OPTS="-Ddirectory-backend.permanent=$SERVER_DIRECTORY_BACKEND_PERMANENT $JAVA_OPTS"
fi

if [ -n "$SERVER_DIRECTORY_BACKEND_SESSION" ]; then
  JAVA_OPTS="-Ddirectory-backend.session=$SERVER_DIRECTORY_BACKEND_SESSION $JAVA_OPTS"
fi

if [ -n "$SERVER_ABBREVIATE_SN_ATTRIBUTE" ]; then
  JAVA_OPTS="-Dattribute.sn.abbreviate=$SERVER_ABBREVIATE_SN_ATTRIBUTE $JAVA_OPTS"
fi

if [ -n "$SERVER_ABBREVIATE_GN_ATTRIBUTE" ]; then
  JAVA_OPTS="-Dattribute.gn.abbreviate=$SERVER_ABBREVIATE_GN_ATTRIBUTE $JAVA_OPTS"
fi

# backend settings for Crowd client

if [ -n "$CROWD_APP_NAME" ]; then
  JAVA_OPTS="-Dapplication.name=$CROWD_APP_NAME $JAVA_OPTS"
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

# backend settings for class CrowdDirectoryBackend

if [ -n "$BACKEND_READINESS_CHECK" ]; then
  JAVA_OPTS="-Dreadiness-check=$BACKEND_READINESS_CHECK $JAVA_OPTS"
fi

# backend settings for class CachedInMemoryDirectoryBackend

if [ -n "$BACKEND_ENTRY_CACHE_ENABLED" ]; then
  JAVA_OPTS="-Dentry-cache.enabled=$BACKEND_ENTRY_CACHE_ENABLED $JAVA_OPTS"
fi

if [ -n "$BACKEND_ENTRY_CACHE_MAX_SIZE" ]; then
  JAVA_OPTS="-Dentry-cache.max-size=$BACKEND_ENTRY_CACHE_MAX_SIZE $JAVA_OPTS"
fi

if [ -n "$BACKEND_ENTRY_CACHE_MAX_AGE" ]; then
  JAVA_OPTS="-Dentry-cache.max-age=$BACKEND_ENTRY_CACHE_MAX_AGE $JAVA_OPTS"
fi

# backend settings for class CachedInMemoryDirectoryBackend

if [ -n "$BACKEND_REST_USERNAME" ]; then
  JAVA_OPTS="-Drest.username=$BACKEND_REST_USERNAME $JAVA_OPTS"
fi

if [ -n "$BACKEND_REST_USER_PASSWORD" ]; then
  JAVA_OPTS="-Drest.user-password=$BACKEND_REST_USER_PASSWORD $JAVA_OPTS"
fi

if [ -n "$BACKEND_REST_BASE_URL" ]; then
  JAVA_OPTS="-Drest.base-url=$BACKEND_REST_BASE_URL $JAVA_OPTS"
fi

if [ -n "$BACKEND_MIRROR_SYNC_PAGE_SIZE" ]; then
  JAVA_OPTS="-Dmirror.sync.page-size=$BACKEND_MIRROR_SYNC_PAGE_SIZE $JAVA_OPTS"
fi

if [ -n "$BACKEND_MIRROR_AUDIT_LOG_PAGE_SIZE" ]; then
  JAVA_OPTS="-Dmirror.audit-log.page-size=$BACKEND_MIRROR_AUDIT_LOG_PAGE_SIZE $JAVA_OPTS"
fi

if [ -n "$BACKEND_MIRROR_AUDIT_LOG_PAGE_LIMIT" ]; then
  JAVA_OPTS="-Dmirror.audit-log.page-limit=$BACKEND_MIRROR_AUDIT_LOG_PAGE_LIMIT $JAVA_OPTS"
fi

if [ -n "$BACKEND_MIRROR_FORCE_FULL_SYNC_ON_BOOT" ]; then
  JAVA_OPTS="-Dmirror.force-full-sync-on-boot=$BACKEND_MIRROR_FORCE_FULL_SYNC_ON_BOOT $JAVA_OPTS"
fi

# backend settings for class CachedWithPersistenceDirectoryBackend

if [ -n "$BACKEND_JDBC_DRIVER" ]; then
  JAVA_OPTS="-Ddatabase.jdbc.driver=$BACKEND_JDBC_DRIVER $JAVA_OPTS"
fi

if [ -n "$BACKEND_JDBC_URL" ]; then
  JAVA_OPTS="-Ddatabase.jdbc.connection.url=$BACKEND_JDBC_URL $JAVA_OPTS"
fi

if [ -n "$BACKEND_JDBC_USER" ]; then
  JAVA_OPTS="-Ddatabase.jdbc.connection.user=$BACKEND_JDBC_USER $JAVA_OPTS"
fi

if [ -n "$BACKEND_JDBC_PASSWORD" ]; then
  JAVA_OPTS="-Ddatabase.jdbc.connection.password=$BACKEND_JDBC_PASSWORD $JAVA_OPTS"
fi

if [ -n "$BACKEND_JDBC_CON_MIN_IDLE" ]; then
  JAVA_OPTS="-Ddatabase.jdbc.connection.min-idle=$BACKEND_JDBC_CON_MIN_IDLE $JAVA_OPTS"
fi

if [ -n "$BACKEND_JDBC_CON_MAX_IDLE" ]; then
  JAVA_OPTS="-Ddatabase.jdbc.connection.max-idle=$BACKEND_JDBC_CON_MAX_IDLE $JAVA_OPTS"
fi

if [ -n "$BACKEND_JDBC_CON_MAX_TOTAL" ]; then
  JAVA_OPTS="-Ddatabase.jdbc.connection.max-total=$BACKEND_JDBC_CON_MAX_TOTAL $JAVA_OPTS"
fi

if [ -n "$BACKEND_JDBC_CON_MAX_OPEN_STMT" ]; then
  JAVA_OPTS="-Ddatabase.jdbc.connection.max-open-prepared-statements=$BACKEND_JDBC_CON_MAX_OPEN_STMT $JAVA_OPTS"
fi

if [ -n "$BACKEND_JDBC_ISO_LEVEL" ]; then
  JAVA_OPTS="-Ddatabase.jdbc.connection.isolation-level=$BACKEND_JDBC_ISO_LEVEL $JAVA_OPTS"
fi

if [ -n "$BACKEND_PASS_ACTIVE_USERS_ONLY" ]; then
  JAVA_OPTS="-Dpass-active-users-only=$BACKEND_PASS_ACTIVE_USERS_ONLY $JAVA_OPTS"
fi

# wait for database

if [ "$WAIT_DATABASE" = "true" ] && [ -n "$BACKEND_JDBC_URL" ]; then
  while ! nc -z $(echo "$BACKEND_JDBC_URL" | cut -d "/" -f 3 | tr ':' ' '); do
  echo "Waiting another 5 seconds for the database to come up."
  sleep 5
  done
fi

# start from here

export JAVA_OPTS

exec "$@"
