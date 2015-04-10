#! /bin/sh
### BEGIN INIT INFO
# Provides:          codekvast-server
# Required-Start:
# Required-Stop:
# Should-Start:      $network
# Should-Stop:       $network
# X-Start-Before:
# X-Stop-After:
# Default-Start:     3 4 5
# Default-Stop:      1 2
# Short-Description: Codekvast Server
# Description:       The database and visualisation layer for Codekvast
### END INIT INFO

# Author: olle.hallin@crisp.se

APP=/opt/codekvast-server/bin/codekvast-server
NAME=CodekvastServerApplication
DESC="Codekvast Server"
SCRIPTNAME=/etc/init.d/codekvast-server.sh
USER=root

# Exit if the package is not installed
test -x $APP || exit 0

# Define LSB log_* functions.
# Depend on lsb-base (>= 3.2-14) to ensure that this file is present
# and status_of_proc is working.
. /lib/lsb/init-functions

# Get the timezone set.
if [ -z "$TZ" -a -e /etc/timezone ]; then
    TZ=`cat /etc/timezone`
    export TZ
fi

doStart() {
    cd $(dirname $APP)/..
    mkdir -p log
    chown $USER:$USER log
    su $USER -c "exec $APP" > log/codekvast-server.out 2>&1 &
    chown -R $USER:$USER log
}

doStop() {
    jps | grep $NAME | awk '{print $1}' | xargs kill
}

case "$1" in
  start)
	log_daemon_msg "Starting $DESC" "$NAME"
    doStart
	status=$?
	log_end_msg $status
	;;

  stop)
	log_daemon_msg "Stopping $DESC" "$NAME"
	doStop
	status=$?
	log_end_msg $status
	;;

  restart)
	log_daemon_msg "Restarting $DESC" "$NAME"
	doStop
	doStart
	status=$?
	log_end_msg $status
	;;

  status)
	jps | grep $NAME
	;;
  *)
	echo "Usage: $SCRIPTNAME {start|stop|restart|status}" >&2
	exit 3
	;;
esac

exit 0
