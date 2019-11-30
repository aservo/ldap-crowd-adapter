#!/bin/sh

if [ -n "$LOGLEVEL" ]; then
  ARGS="-Dloglevel=$LOGLEVEL $ARGS"
fi

if [ -n "$CROWD_APP_NAME" ]; then
  ARGS="-Dapplication.name=$CROWD_APP_NAME $ARGS"
fi

if [ -n "$CROWD_APP_PASSWORD" ]; then
  ARGS="-Dapplication.password=$CROWD_APP_PASSWORD $ARGS"
fi

if [ -n "$CROWD_SERVER_URL" ]; then
  ARGS="-Dcrowd.server.url=$CROWD_SERVER_URL $ARGS"
fi

if [ -n "$CROWD_VALIDATION_INTERVAL" ]; then
  ARGS="-Dsession.validationinterval=$CROWD_VALIDATION_INTERVAL $ARGS"
fi

if [ -n "$SERVER_CACHE_DIR" ]; then
  ARGS="-Dcache-directory=$SERVER_CACHE_DIR $ARGS"
fi

if [ -n "$SERVER_BIND_ADDRESS" ]; then
  ARGS="-Dbind.address=$SERVER_BIND_ADDRESS $ARGS"
fi

if [ -n "$SERVER_ENTRY_CACHE_ENABLED" ]; then
  ARGS="-Dentry-cache.enabled=$SERVER_ENTRY_CACHE_ENABLED $ARGS"
fi

if [ -n "$SERVER_ENTRY_CACHE_MAX_SIZE" ]; then
  ARGS="-Dentry-cache.max-size=$SERVER_ENTRY_CACHE_MAX_SIZE $ARGS"
fi

if [ -n "$SERVER_ENTRY_CACHE_MAX_AGE" ]; then
  ARGS="-Dentry-cache.max-age=$SERVER_ENTRY_CACHE_MAX_AGE $ARGS"
fi

if [ -n "$SERVER_SSL_ENABLED" ]; then
  ARGS="-Dssl.enabled=$SERVER_SSL_ENABLED $ARGS"
fi

if [ -n "$SERVER_SSL_KEY_STORE_FILE" ]; then
  ARGS="-Dssl.key-store-file=$SERVER_SSL_KEY_STORE_FILE $ARGS"
fi

if [ -n "$SERVER_SSL_KEY_STORE_PASSWORD" ]; then
  ARGS="-Dssl.key-store-password=$SERVER_SSL_KEY_STORE_PASSWORD $ARGS"
fi

if [ -n "$SERVER_SUPPORT_MEMBER_OF" ]; then
  ARGS="-Dsupport.member-of=$SERVER_SUPPORT_MEMBER_OF $ARGS"
fi

sbt $ARGS run
