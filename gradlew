#!/bin/sh
#
# Gradle start up script for UN*X
#
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
APP_HOME="`pwd -P`"
GRADLE_OPTS=""
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () { echo "$*"; }
die () { echo; echo "$*"; echo; exit 1; }

OS="`uname`"
case "$OS" in
  Darwin*) OS=darwin;;
  Linux*) OS=linux;;
  CYGWIN*) OS=cygwin;;
  MSYS*) OS=msys;;
  MINGW*) OS=mingw;;
esac

GRADLE_USER_HOME="${GRADLE_USER_HOME:-"$HOME/.gradle"}"
WRAPPER_JAR="$GRADLE_USER_HOME/wrapper/dists"
JAVACMD="${JAVACMD:-java}"

exec "$JAVACMD" \
  $DEFAULT_JVM_OPTS \
  $JAVA_OPTS \
  $GRADLE_OPTS \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
