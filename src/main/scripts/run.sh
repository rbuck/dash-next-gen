#!/usr/bin/env bash

DEBUG_MODE=false
DEBUG_PORT="8787"
SERVER_OPTS=""
while [ "$#" -gt 0 ]
do
    case "$1" in
      --debug)
          DEBUG_MODE=true
          shift
          if [ -n "$1" ] && [ "${1#*-}" = "$1" ]; then
              DEBUG_PORT=$1
          fi
          ;;
      --)
          shift
          break;;
      *)
          SERVER_OPTS="$SERVER_OPTS \"$1\""
          ;;
    esac
    shift
done

# resolve links - $0 may be a soft link
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "${PRG}"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "${PRG}"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "${PRG}"`

# Only set APPLICATION_HOME_DIR if not already set
[ -z "${APPLICATION_HOME_DIR}" ] && APPLICATION_HOME_DIR=`cd "${PRGDIR}/.." >/dev/null; pwd`

# Only set APPLICATION_CONF_DIR if not already set
[ -z "${APPLICATION_CONF_DIR}" ] && APPLICATION_CONF_DIR=${APPLICATION_HOME_DIR}/conf

# Only set APPLICATION_LOGGING_CONF_FILE if not already set
[ -z "${APPLICATION_LOGGING_CONF_FILE}" ] && APPLICATION_LOGGING_CONF_FILE=${APPLICATION_CONF_DIR}/logback.xml

# Only set APPLICATION_LOG_DIR if not already set
[ -z "${APPLICATION_LOG_DIR}" ] && APPLICATION_LOG_DIR=${APPLICATION_HOME_DIR}/log

# Only create log directory if it does not already exist
[ ! -d "${APPLICATION_LOG_DIR}" ] && `mkdir -p "${APPLICATION_LOG_DIR}"`

# Add on extra jar files to CLASSPATH
if [ ! -z "${CLASSPATH}" ] ; then
  CLASSPATH="${CLASSPATH}":"${APPLICATION_HOME_DIR}"/lib
fi
CLASSPATH=${CLASSPATH}:$(JARS=("${APPLICATION_HOME_DIR}"/lib/*.jar); IFS=:; echo "${JARS[*]}")
CLASSPATH=${CLASSPATH}:$(JARS=("${APPLICATION_HOME_DIR}"/lib/ext/*.jar); IFS=:; echo "${JARS[*]}")

# Read an optional running configuration file
if [ "x${RUN_CONF}" = "x" ]; then
    RUN_CONF="${PRGDIR}/run.conf"
fi
if [ -r "${RUN_CONF}" ]; then
    . "${RUN_CONF}"
fi

# Set debug settings if not already set
if [ "$DEBUG_MODE" = "true" ]; then
    DEBUG_OPT=`echo $JAVA_OPTS | $GREP "\-agentlib:jdwp"`
    if [ "x$DEBUG_OPT" = "x" ]; then
        JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=n"
    else
        echo "Debug already enabled in JAVA_OPTS, ignoring --debug argument"
    fi
fi

if [ "x${JAVA_HOME}" = "x" ]; then
    os_type=`uname -s`
    case $os_type in
        Darwin*)
            #export JAVA_HOME=`/usr/libexec/java_home -v 1.6`
            export JAVA_HOME=`/usr/libexec/java_home -v 1.7`
            #export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
        ;;
        Linux*)
            export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")
        ;;
    esac
fi

JAVA=""
if [ "x${JAVA}" = "x" ]; then
  JAVA=${JAVA_HOME}/bin/java
else
  echo "JAVA_HOME must be set."
  exit 1
fi

# Set the classpath.

if [ "${CLASSPATH}" != "" ]; then
  CLASSPATH=${CLASSPATH}:$JAVA_HOME/lib/tools.jar
else
  CLASSPATH=$JAVA_HOME/lib/tools.jar
fi

if [ "${DEBUG}" != "" ]; then
  DEBUG_ECHO=echo
else
  DEBUG_ECHO=true
fi

${DEBUG_ECHO} "========================================================================="
${DEBUG_ECHO} ""
${DEBUG_ECHO} "  Application Environment"
${DEBUG_ECHO} ""
${DEBUG_ECHO} "  APPLICATION_HOME_DIR: ${APPLICATION_HOME_DIR}"
${DEBUG_ECHO} ""
${DEBUG_ECHO} "  JAVA: ${JAVA}"
${DEBUG_ECHO} ""
${DEBUG_ECHO} "  JAVA_OPTS: ${JAVA_OPTS}"
${DEBUG_ECHO} ""
${DEBUG_ECHO} "  CLASSPATH: ${CLASSPATH}"
${DEBUG_ECHO} ""
${DEBUG_ECHO} "========================================================================="
${DEBUG_ECHO} ""

while true; do
    if [ "x${LAUNCH_DASH_IN_BACKGROUND}" = "x" ]; then
        # Execute the JVM in the foreground
        eval "${JAVA}" ${JAVA_OPTS} \
            -Ddash.application.home.dir=${APPLICATION_HOME_DIR} \
            -Ddash.application.conf.dir=${APPLICATION_CONF_DIR} \
            -Ddash.log.dir=${APPLICATION_LOG_DIR} \
            -Dlogback.configurationFile=file://${APPLICATION_LOGGING_CONF_FILE} \
            -classpath "${CLASSPATH}" com.github.rbuck.dash.Main "$SERVER_OPTS"

        APPLICATION_STATUS=$?
        # to redirect use this: > /var/tmp/github-demos.log 2>&1
    else
        # Execute the JVM in the background
        eval "${JAVA}" ${JAVA_OPTS} \
            \"-Ddash.application.home.dir=${APPLICATION_HOME_DIR}\" \
            \"-Ddash.application.conf.dir=${APPLICATION_CONF_DIR}\" \
            \"-Ddash.log.dir=${APPLICATION_LOG_DIR}\" \
            \"-Dlogback.configurationFile=file://${APPLICATION_LOGGING_CONF_FILE}\" \
            -classpath "${CLASSPATH}" \
            com.github.rbuck.dash.Main \
            "$SERVER_OPTS" "&"

        APPLICATION_PID=$!

        # Trap common signals and relay them to the application process
        trap "kill -HUP  ${APPLICATION_PID}" HUP
        trap "kill -TERM ${APPLICATION_PID}" INT
        trap "kill -QUIT ${APPLICATION_PID}" QUIT
        trap "kill -PIPE ${APPLICATION_PID}" PIPE
        trap "kill -TERM ${APPLICATION_PID}" TERM
        if [ "x${APPLICATION_PIDFILE}" != "x" ]; then
            echo ${APPLICATION_PID} > ${APPLICATION_PIDFILE}
        fi
        # Wait until the background process exits
        WAIT_STATUS=128
        while [ "${WAIT_STATUS}" -ge 128 ]; do
            wait ${APPLICATION_PID} 2>/dev/null
            WAIT_STATUS=$?
            if [ "${WAIT_STATUS}" -gt 128 ]; then
                SIGNAL=`expr ${WAIT_STATUS} - 128`
                SIGNAL_NAME=`kill -l ${SIGNAL}`
                echo "*** Application process (${APPLICATION_PID}) received ${SIGNAL_NAME} signal ***" >&2
            fi
        done
        if [ "${WAIT_STATUS}" -lt 127 ]; then
            APPLICATION_STATUS=${WAIT_STATUS}
        else
            APPLICATION_STATUS=0
        fi
        if [ "${APPLICATION_STATUS}" -ne 10 ]; then
            # Wait for a complete shudown
            wait ${APPLICATION_PID} 2>/dev/null
        fi
        if [ "x${APPLICATION_PIDFILE}" != "x" ]; then
            grep "${APPLICATION_PID}" ${APPLICATION_PIDFILE} && rm ${APPLICATION_PIDFILE}
        fi
    fi
    if [ "${APPLICATION_STATUS}" -eq 10 ]; then
        echo "Restarting the application..."
    else
        exit ${APPLICATION_STATUS}
    fi
done
