#!/bin/sh

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
  JAVA_OPTS="-Dcache-directory=$SERVER_CACHE_DIR $JAVA_OPTS"
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

if [ -n "$SERVER_SSL_ENABLED" ]; then
  JAVA_OPTS="-Dssl.enabled=$SERVER_SSL_ENABLED $JAVA_OPTS"
fi

if [ -n "$SERVER_SSL_KEY_STORE_FILE" ]; then
  JAVA_OPTS="-Dssl.key-store-file=$SERVER_SSL_KEY_STORE_FILE $JAVA_OPTS"
fi

if [ -n "$SERVER_SSL_KEY_STORE_PASSWORD" ]; then
  JAVA_OPTS="-Dssl.key-store-password=$SERVER_SSL_KEY_STORE_PASSWORD $JAVA_OPTS"
fi

if [ -n "$SERVER_SUPPORT_MEMBER_OF" ]; then
  JAVA_OPTS="-Dsupport.member-of=$SERVER_SUPPORT_MEMBER_OF $JAVA_OPTS"
fi

export JAVA_OPTS

sbt run
