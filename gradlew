#!/bin/sh
set -e
if [ -d "$(dirname "$0")/gradle/wrapper" ]; then
  GRADLE_WRAPPER="$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar"
  if [ ! -f "$GRADLE_WRAPPER" ]; then
    echo "Gradle Wrapper not found at $GRADLE_WRAPPER"
    exit 1
  fi
  exec java -jar "$GRADLE_WRAPPER" "$@"
else
  echo "Gradle Wrapper directory not found"
  exit 1
fi

