#!/bin/sh

# link configuration files for apache and cosign
# from volume from preloaded secret.
ln -sf /usr/local/apache2/local/conf/httpd.conf /usr/local/apache2/conf/httpd.conf
ln -s /usr/local/apache2/local/conf/httpd-cosign.conf /usr/local/apache2/conf/extra/httpd-cosign.conf

# Rehash command needs to be run before starting apache.
c_rehash /usr/local/apache2/local/certs

# Redirect logs to stdout and stderr for docker reasons.
ln -sf /dev/stdout /usr/local/apache2/logs/access_log
ln -sf /dev/stderr /usr/local/apache2/logs/error_log

/usr/local/apache2/bin/httpd -DFOREGROUND 