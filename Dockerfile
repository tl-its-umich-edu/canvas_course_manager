FROM tomcat:8-jre8

MAINTAINER Teaching and Learning <its.tl.dev@umich.edu>

RUN apt-get update \
 && apt-get install -y vim maven openjdk-8-jdk git

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

WORKDIR /tmp

# Build esbUtils, a dependency of the CCM.
RUN git clone --branch v1.1  https://github.com/tl-its-umich-edu/esbUtils \
 && cd esbUtils \
 && mvn clean install

# Build lti-utils, a dependency of the CCM.
#RUN git clone --branch 1.5 https://github.com/tl-its-umich-edu/lti-utils \
RUN git clone --branch 1.6 https://github.com/tl-its-umich-edu/lti-utils \
 && cd lti-utils \
 && mvn clean install

# Copy CCM code to local directory for building
COPY . /tmp

# Build CCM and place the resulting war in the tomcat dir.
RUN mvn clean install \
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

#tomcat port
EXPOSE 8080
#apache port
EXPOSE 8009
#debug port
EXPOSE 5009
ENV JPDA_ADDRESS="5009"
ENV JPDA_TRANSPORT="dt_socket"

# Launch Tomcat
CMD cp /usr/share/ccm-props/* /usr/local/tomcat/conf/; catalina.sh jpda run
