#!/bin/sh
# Converts Fly's DATABASE_URL (postgres://user:pass@host:port/db?params) into the JDBC settings Spring reads.
set -e

if [ -n "$DATABASE_URL" ] && [ -z "$JDBC_DATABASE_URL" ]; then
  rest=${DATABASE_URL#*://}          # user:pass@host:port/db?params
  creds=${rest%%@*}                  # user:pass
  hostpart=${rest#*@}                # host:port/db?params
  export JDBC_DATABASE_USERNAME="${creds%%:*}"
  export JDBC_DATABASE_PASSWORD="${creds#*:}"
  export JDBC_DATABASE_URL="jdbc:postgresql://${hostpart}"
fi

exec java $JAVA_OPTS -jar /app/app.jar
