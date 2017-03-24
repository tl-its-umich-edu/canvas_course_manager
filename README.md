# Canvas Course Manager

## Project Description 
  
The Canvas Course Manager (CCM) is an application that will be able to be used by instructors and the service center alike. The current functionality searches all courses for which a given user has the 'teacher' role within a given semester and allows the combination of sections via drag-and-drop interface for Service Center or point-and-click interface for LTI users. The user can also determine if the course is used before the sections are moved. In addition, the functionality for adding friend accounts is available. Not only can a 'teacher' create friend accounts for non-umich accounts, but it will also be possible to add a created friend account to a course. The service center alone will have the ability to rename a course.

## Build Directions
### Dependencies:

<code>git clone https://github.com/tl-its-umich-edu/lti-utils.git</code>

<code>cd lti-utils</code>

<code>$mvn clean install</code>

<code>git clone https://github.com/tl-its-umich-edu/esbUtils.git</code>

<code>cd esbUtils</code>

<code>$mvn clean install</code>

<code>git clone https://github.com/tl-its-umich-edu/canvas_course_manager</code>

<code>cd canvas_course_manager</code>

### Build, deploy run:

1. sectionsTool <code>$mvn clean install</code>

2. Copy to tomcat/webapp

3. Add the property files on linux box <code>ccm.properties</code> and <code>ccm-secure.properties</code>, then in JAVA_OPTS add the  
<code>-DccmPropsPathSecure=file:/file-path/ccm-secure.properties</code>  
<code>-DccmPropsPath=file:/file-path/ccm.properties</code>

4. Add the following properties to ccm.properties:  
    
    In addition to the properties below, there will also be several regular expression properties that will be used for validating Canvas or ESB API calls.
    
	* <code>umich.friend.url= friend URL to be used</code>
	* <code>umich.friend.contactemail= Contact email used in friend template for users experiencing issues</code>
	* <code>umich.friend.referrer= URL to direct to Canvas</code>
	* <code>umich.friend.friendemail= File path to friend email template</code>
	* <code>umich.friend.requesteremail=File path to requester email</code>
	* <code>umich.friend.mailhost= always localhost</code>
	* <code>umich.friend.subjectline= Subject line for friend email</code>
	* <code>lti.url= URL to launch LTI tool</code>
	* <code>call.type= Either 'canvas' or 'esb'</code>
	* <code>stub.test= 'true' or 'false'</code>
    
5. Add the following 9 properties to ccm-secure.properties:<br/> 
	* <code>canvas.admin.token= Canvas Token </code>
	* <code>canvas.url= Canvas URL</code>
	* <code>use.test.url= Test user authentication </code>
	* <code>ldap.server.url= ldap URL </code>
	* <code>mcomm.group= MCommunity Group </code>
	* <code>umich.friend.ksfilename= file path to the keystore file ending in pkcs12 </code>
	* <code>umich.friend.kspassword= password to above keystore file </code>
	* <code>lti.key= Numeric key to launch LTI tool </code>
	* <code>lti.secret=alpha numeric secret to launch LTI tool</code>

6. Invoke the following URL in your browser:  
<code>http://localhost:PORT/canvasCourseManager/index-sc.vm?testUser=UNIQUENAME</code>  
	a. testUser parameter is not allowed in Prod and this is controlled by above property with value called <code>use.test.url=false</code>  
	b. We will enable the cosign for authentication the user so we will get remote user info through that.  
  
7. Enable application level logging using the log4j.properties files. Put this file in tomcat/lib directory and add the content between the 
 
	```
	log4j.rootLogger=INFO, A1
	log4j.appender.A1=org.apache.log4j.ConsoleAppender
	log4j.appender.A1.layout=org.apache.log4j.PatternLayout
	# Print the date in ISO 8601 format
	log4j.appender.A1.layout.ConversionPattern=%d [%t] %-5p %c - %m%n
	# umich
	#log4j.logger.edu.umich=INFO
	log4j.logger.edu.umich=DEBUG 
	```

8. For Adding Build information to the Project Release candidate populate src/main/webapps/build.txt with information about the current build (GIT commit, build time, etc.).
    If using Jenkins to build the application, the following script placed in the "Execute Shell" part of the "Pre Steps" section would do the job:

    
	``` 
	cd src/main/webapp
	if [ -f "build.txt" ]; then
	   echo "build.txt found."
	   rm build.txt
	   echo "Existing build.txt file removed."
	else
	   echo "No existing build.txt file found."
	fi
	  touch build.txt
	
	  echo "$JOB_NAME | Build: $BUILD_NUMBER | $GIT_URL | $GIT_COMMIT | $GIT_BRANCH | $BUILD_ID" >> build.txt
	```

9. In order to remote debug ccm application from Openshift, do `oc port-forward' <pod-name> 5009`. 5009 is the debug port opened for doing this. More Info https://docs.openshift.com/container-platform/3.4/dev_guide/port_forwarding.html
## Notes

If <code>use.test.url</code> is true, users will be able to execute the tool as if authenticated as the user specified in the URL parameter <code>?testUser=uniqname</code>. In Production this variable is  false. Based on this property property testUser is not allowed in Production.

ldap is used for authorizing the user and he needs to be part of particular Mcommunity group to be authorized to use the tool.

During development we received the following error:

	Failed to create input stream: Received fatal alert: handshake_failure

This error occurs when attempting to create a friend account with an unrecognized .pkc12 file which is required for the application. If this occurs, try getting a fresh copy of the .pkcs12 file.
