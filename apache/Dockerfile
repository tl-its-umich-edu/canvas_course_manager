FROM httpd:2.4

### Cosign Pre-requisites ###
WORKDIR /usr/local/apache2
ENV COSIGN_URL http://downloads.sourceforge.net/project/cosign/cosign/cosign-3.2.0/cosign-3.2.0.tar.gz
ENV CPPFLAGS="-I/usr/kerberos/include"
ENV OPENSSL_VERSION 1.0.2k-1~bpo8+1

RUN apt-get update \
	&& apt-get install -y wget gcc libssl-dev=$OPENSSL_VERSION make openssl

### Build Cosign ###
RUN wget "$COSIGN_URL" \
	&& mkdir -p src/cosign \
	&& tar -xvf cosign-3.2.0.tar.gz -C src/cosign --strip-components=1 \
	&& rm cosign-3.2.0.tar.gz \
	&& cd src/cosign \
	&& ./configure --enable-apache2=/usr/local/apache2/bin/apxs \
	&& sed -i 's/remote_ip/client_ip/g' ./filters/apache2/mod_cosign.c \
	&& make \
	&& make install \
	&& cd ../../ \
	&& rm -r src/cosign \
	&& mkdir -p /var/cosign/filter \
	&& chmod 777 /var/cosign/filter
	

### Not needed unless communicating with tomcat via AJP ###
### Build mod_jk ###
#RUN wget http://mirrors.koehn.com/apache/tomcat/tomcat-connectors/jk/tomcat-connectors-1.2.42-src.tar.gz \
#	&& mkdir -p src/mod_jk \
#	&& tar -xvf tomcat-connectors-1.2.42-src.tar.gz -C src/mod_jk --strip-components=1 \
#	&& rm tomcat-connectors-1.2.42-src.tar.gz \
#	&& cd src/mod_jk/native \
#	&& ./configure -with-apxs=/usr/local/apache2/bin/apxs \
#	&& make \
#	&& make install \
#	&& rm -r src/mod_jk

### Remove pre-reqs ###
RUN apt-get remove -y make wget \
	&& apt-get autoremove -y

EXPOSE 443
#EXPOSE 80

### Start script incorporates config files and sends logs to stdout ###
COPY start.sh .
RUN chmod +x start.sh

CMD /usr/local/apache2/start.sh