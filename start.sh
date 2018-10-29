#!/bin/bash -x

# copy app/tomcat config files from secret volumes to a location that can be written to.
if [ -e /secrets/app ];
then
    cp /secrets/app/* /usr/local/tomcat/conf/.
fi

if [ -e /secrets/tomcat ];
then
    cp /secrets/tomcat/* /usr/local/tomcat/conf/.
fi

# If it exists, include local.start.sh

if [ -f /secrets/start/local.start.sh ]
then
  /bin/sh /secrets/start/local.start.sh
fi

# Redirect logs to stdout and stderr for docker reasons.
ln -sf /dev/stdout /usr/local/tomcat/logs/access_log
ln -sf /dev/stderr /usr/local/tomcat/logs/error_log

/usr/local/tomcat/bin/catalina.sh jpda run
