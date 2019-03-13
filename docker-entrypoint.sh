#!/bin/sh

set -e

: ${DRIVER_HOME:="/app"}

if [ "${1:0:1}" = '-' ]; then
  set -- run "$@"
fi

if [ "$1" = 'run' ]; then
    shift
    set -- "${DRIVER_HOME}/bin/run.sh" "$@"
fi

exec "$@"