FROM tomcat:8-jre8

MAINTAINER Teaching and Learning <its.tl.dev@umich.edu>

RUN apt-get update \
 && apt-get install -y vim maven openjdk-8-jdk git

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

WORKDIR /tmp

# Build esbUtils, a dependency of the CCM.
RUN git clone --branch v2.0  https://github.com/tl-its-umich-edu/esbUtils \
 && cd esbUtils \
 && mvn clean install -Dmaven.test.skip=true

# Copy CCM code to local directory for building
COPY . /tmp

# Build CCM and place the resulting war in the tomcat dir.
RUN mvn clean install -Dmaven.test.skip=true \
 && mv ./target/canvasCourseManager.war /usr/local/tomcat/webapps

# Remove unnecessary build dependencies.
RUN apt-get remove -y maven openjdk-8-jdk git \
 && apt-get autoremove -y

# Download the YourKit to /usr/local for profiling. This will be commented out for production.
#WORKDIR /usr/local
#RUN wget https://www.yourkit.com/download/yjp-2017.02-b59.zip \
 #	&& unzip yjp-2017.02-b59.zip

WORKDIR /usr/local/tomcat/webapps

# Set Opts, including paths for the CCM properties.
ENV JAVA_OPTS="-server \
-Xmx1028m \
-Dorg.apache.jasper.compiler.Parser.STRICT_QUOTE_ESCAPING=false \
-Djava.awt.headless=true -Dcom.sun.management.jmxremote \
-Dsun.lang.ClassLoader.allowArraySyntax=true \
-Dfile.encoding=UTF-8 \
-DccmPropsPathSecure=file:$CATALINA_HOME/conf/ccm-secure.properties \
-DccmPropsPath=file:$CATALINA_HOME/conf/ccm.properties \
-Dlog4j.configuration=file:/usr/local/tomcat/conf/log4j.properties \
"
# When need for profiling the application this option goes in the JAVA_OPTS
#-agentpath:/usr/local/yjp-2017.02/bin/linux-x86-64/libyjpagent.so=delay=10000,sessionname=Tomcat \

#apache port
EXPOSE 8009
#debug port
EXPOSE 5009
ENV JPDA_ADDRESS="5009"
ENV JPDA_TRANSPORT="dt_socket"

### change directory owner, as openshift user is in root group.
RUN chown -R root:root /usr/local/tomcat/logs /var/lock /var/run/lock

### Modify perms for the openshift user, who is not root, but part of root group.
#RUN chmod 777 /usr/local/tomcat/conf /usr/local/tomcat/conf/webapps
RUN chmod g+rw /usr/local/tomcat/conf /usr/local/tomcat/logs /usr/local/tomcat/webapps \
        /usr/local/tomcat/conf/server.xml /var/lock /var/run/lock

# Launch Tomcat
#CMD cp /tmp/tomcat/* /usr/local/tomcat/conf/; cp /tmp/app/* /usr/local/tomcat/conf/; catalina.sh jpda run

### Start script incorporates config files and sends logs to stdout ###
COPY start.sh /usr/local/bin
RUN chmod 755 /usr/local/bin/start.sh
CMD /usr/local/bin/start.sh
