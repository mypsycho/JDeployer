#!/bin/sh

# ---------------------------------------------------------------------------
# Entry point of mypsycho-rrun
#
# Environment Variable Prequisites
#
#
#   RRUN_OPTS   (Optional) Java runtime options used when 
#                   command is executed.
#
#   JAVA_HOME       Must point at your Java Development Kit installation.
#                   or your Java Runtime Environment installation
#
#
# ---------------------------------------------------------------------------

# Identification of installation directory
#   resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
RRUN_HOME=`dirname "$PRG"`/..


# Get standard environment variables
if [ -r "$RRUN_HOME/bin/setenv.sh" ]; then
  chmod u+x "$RRUN_HOME/bin/setenv.sh"
  . "$RRUN_HOME/bin/setenv.sh"
fi


# Make sure prerequisite environment variables are set
if [ -z "$JAVA_EXEC" ]; then
  if [ -z "$JAVA_HOME" ]; then
    JAVA_EXEC=java
  else
    JAVA_EXEC="$JAVA_HOME/bin/java"
  fi
fi

$JAVA_EXEC $RRUN_OPTS -jar "$RRUN_HOME/lib/org.mypsycho.rrun/rrun-proc-1.0.0-SNAPSHOT.jar" %*
