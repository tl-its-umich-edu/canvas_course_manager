/*
Copyright 2015-2016 University of Michigan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package edu.umich.ctools.sectionsUtilityTool;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SectionUtilityToolFilter implements Filter {

	private static Log M_log = LogFactory.getLog(SectionUtilityToolFilter.class);

	private static final String OU_GROUPS = "ou=Groups";
	private static final String LDAP_CTX_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
	protected static final String SYSTEM_PROPERTY_FILE_PATH_SECURE = "sectionsToolPropsPathSecure";
	protected static final String PROPERTY_CANVAS_ADMIN = "canvas.admin.token";
	protected static final String PROPERTY_CANVAS_URL = "canvas.url";
	protected static final String PROPERTY_USE_TEST_URL = "use.test.url";
	protected static final String PROPERTY_LDAP_SERVER_URL = "ldap.server.url";
	protected static final String PROPERTY_LTI_URL = "lti.url";
	protected static final String PROPERTY_LTI_KEY = "lti.key";
	protected static final String PROPERTY_LTI_SECRET = "lti.secret";
	protected static final String PROPERTY_CALL_TYPE = "call.type";
	protected static final String PROPERTY_AUTH_GROUP = "mcomm.group";
	protected static final String ESB_TOKEN_SERVER = "esb.token.server";
	protected static final String ESB_KEY = "esb.key";
	protected static final String ESB_SECRET = "esb.secret";
	protected static final String ESB_PREFIX = "esb.prefix";
	protected static final String PROPERTY_TEST_STUB = "stub.test";
	private static final String TEST_USER = "testUser";
	private static final String LAUNCH_TYPE = "launchType";

	private String providerURL = null;
	private String mcommunityGroup = null;
	private boolean isTestUrlEnabled=false;

	private final static String CCM_PROPERTY_FILE_PATH = "ccmPropsPath";
	private final static String CCM_SECURE_PROPERTY_FILE_PATH = "ccmPropsPathSecure";	

	protected static Properties appExtSecureProperties=null;
	protected static Properties appExtProperties=null;

	private static final String FALSE = "false";

	public static final String BASIC_LTI_LAUNCH_REQUEST = "basic-lti-launch-request";
	public static final String LTI_MESSAGE_TYPE = "lti_message_type";

	public static final String LTI_PAGE = "/index-lti.vm";
	public static final String SC_PAGE = "/index-sc.vm";
	public static final String STATUS_PAGE = "/status.html";
	public static final String PING_PAGE = "/status/ping.html";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		M_log.debug("Filter Init(): Called");
		appExtProperties = Utils.loadProperties(CCM_PROPERTY_FILE_PATH);
		appExtSecureProperties = Utils.loadProperties(CCM_SECURE_PROPERTY_FILE_PATH);
		getExternalAppProperties();
	}

	@Override
	public void destroy() {
		M_log.debug("destroy: Called");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		M_log.debug("doFilter: Called");
		HttpServletRequest useRequest = (HttpServletRequest) request;
		HttpServletResponse useResponse=(HttpServletResponse)response;

		if ( BASIC_LTI_LAUNCH_REQUEST.equals(request.getParameter(LTI_MESSAGE_TYPE))) {
			M_log.debug("new launch so invalidate any existing session");
			if (useRequest.getSession() != null) {
				M_log.info("session id to invalidate: " + useRequest.getSession().getId());
				useRequest.getSession().invalidate();
			}
		}

		HttpSession session= useRequest.getSession(true);
		M_log.debug("session id: " + session.getId());

		//Launch type for for the LTI part will be different from the launch
		//type for SC because LTI will only be launched within LMS and SC can 
		//be launched from a browser
		setLaunchType(request, session);

		M_log.debug("Launch Type: " + session.getAttribute(LAUNCH_TYPE));
		M_log.debug("Path Info: " + useRequest.getPathInfo() );

		if (session.getAttribute(LAUNCH_TYPE).equals("lti")){
			if( useRequest.getPathInfo() != null && useRequest.getPathInfo().equals(SC_PAGE)){
				useRequest.getSession().invalidate();
				useResponse.sendError(403);
				return;
			}
			chain.doFilter(useRequest, response);
		}
		else if(session.getAttribute(LAUNCH_TYPE).equals("sc")){
			if(!useRequest.getPathInfo().equals(STATUS_PAGE)  && !useRequest.getPathInfo().equals(PING_PAGE)){
				if(!checkForAuthorization(useRequest)) {
					useResponse.sendError(403);
					return;
				}
			}
			chain.doFilter(useRequest, response);
		}
	}

	public void setLaunchType(ServletRequest request, HttpSession session) {
		if ( BASIC_LTI_LAUNCH_REQUEST.equals(request.getParameter(LTI_MESSAGE_TYPE)) && session.getAttribute(LAUNCH_TYPE) == null) {
			session.setAttribute(LAUNCH_TYPE, "lti");
		}
		if (session.getAttribute(LAUNCH_TYPE) == null){
			session.setAttribute(LAUNCH_TYPE, "sc");
		}
	}

	protected void getExternalAppProperties() {
		M_log.debug("getExternalAppProperties(): called");
		if(appExtSecureProperties!=null) {
			isTestUrlEnabled = Boolean.parseBoolean(appExtSecureProperties.getProperty(SectionUtilityToolFilter.PROPERTY_USE_TEST_URL,FALSE));
			providerURL=appExtSecureProperties.getProperty(PROPERTY_LDAP_SERVER_URL);
			mcommunityGroup=appExtSecureProperties.getProperty(PROPERTY_AUTH_GROUP);
		}else {
			M_log.error("Failed to load secure application properties from sectionsToolPropsSecure.properties for SectionsTool");
		}
	}

	private boolean isEmpty(String value) {
		return (value == null) || (value.trim().equals(""));
	}

	/*
	 * User is authenticated using cosign and authorized using Ldap. For local development we are enabling
	 * "testUser" parameter. "testUser" also go through the Ldap authorization process. 
	 * we have use.test.url configured in the properties file in order to disable usage of "testUser" parameter in PROD.
	 * 
	 */
	private boolean checkForAuthorization(HttpServletRequest request) {
		M_log.debug("checkLdapForAuthorization(): called");
		String remoteUser = request.getRemoteUser();
		String testUser = request.getParameter(TEST_USER);
		boolean isAuthorized = false;
		String user=null;
		String testUserInSession = (String)request.getSession().getAttribute(TEST_USER);
		String sessionId = request.getSession().getId();

		if ( isTestUrlEnabled && testUser != null  ) { 
			user=testUser;
			request.getSession().setAttribute(TEST_USER, testUser);
		}
		else if ( isTestUrlEnabled && testUserInSession != null ){
			user=testUserInSession;
		} 
		M_log.debug("remote user: " + remoteUser);
		M_log.debug("test user: " + testUser);
		M_log.debug("session test user: " + testUserInSession);
		M_log.debug("User: " + user);
		if  ( !isAuthorized && remoteUser != null ) {
			user=remoteUser;
			M_log.info(String.format("The session id \"%s\" of Service Desk Person with uniqname  \"%s\" issuing the request" ,sessionId,remoteUser));
		}
		isAuthorized=ldapAuthorizationVerification(user); 
		return isAuthorized;

	}
	/*
	 * The Mcommunity group we have is a members-only group is one that only the members of the group can send mail to. 
	 * The group owner can turn this on or off.
	 * More info on Ldap configuration  http://www.itcs.umich.edu/itcsdocs/r1463/attributes-for-ldap.html#group.
	 */
	private boolean ldapAuthorizationVerification(String user)  {
		M_log.debug("ldapAuthorizationVerification(): called");
		boolean isAuthorized = false;
		DirContext dirContext=null;
		NamingEnumeration listOfPeopleInAuthGroup=null;
		NamingEnumeration allSearchResultAttributes=null;
		NamingEnumeration simpleListOfPeople=null;
		Hashtable<String,String> env = new Hashtable<String, String>();
		if(!isEmpty(providerURL) && !isEmpty(mcommunityGroup)) {
			env.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_CTX_FACTORY);
			env.put(Context.PROVIDER_URL, providerURL);
		}else {
			M_log.error(" [ldap.server.url] or [mcomm.group] properties are not set, review the sectionsToolPropsLessSecure.properties file");
			return isAuthorized;
		}
		try {
			dirContext = new InitialDirContext(env);
			String[] attrIDs = {"member"};
			SearchControls searchControls = new SearchControls();
			searchControls.setReturningAttributes(attrIDs);
			searchControls.setReturningObjFlag(true);
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String searchBase = OU_GROUPS;
			String filter = "(&(cn=" + mcommunityGroup + ") (objectclass=rfc822MailGroup))";
			listOfPeopleInAuthGroup = dirContext.search(searchBase, filter, searchControls);
			String positiveMatch = "uid=" + user + ",";
			outerloop:
				while (listOfPeopleInAuthGroup.hasMore()) {
					SearchResult searchResults = (SearchResult)listOfPeopleInAuthGroup.next();
					allSearchResultAttributes = (searchResults.getAttributes()).getAll();
					while (allSearchResultAttributes.hasMoreElements()){
						Attribute attr = (Attribute) allSearchResultAttributes.nextElement();
						simpleListOfPeople = attr.getAll();
						while (simpleListOfPeople.hasMoreElements()){
							String val = (String) simpleListOfPeople.nextElement();
							if(val.indexOf(positiveMatch) != -1){
								isAuthorized = true;
								break outerloop;
							}
						}
					}
				}
			return isAuthorized;
		} catch (NamingException e) {
			M_log.error("Problem getting attribute:" + e);
			return isAuthorized;
		}
		finally {
			try {
				if(simpleListOfPeople!=null) {
					simpleListOfPeople.close();
				}
			} catch (NamingException e) {
				M_log.error("Problem occurred while closing the NamingEnumeration list \"simpleListOfPeople\" list ",e);
			}
			try {
				if(allSearchResultAttributes!=null) {
					allSearchResultAttributes.close();
				}
			} catch (NamingException e) {
				M_log.error("Problem occurred while closing the NamingEnumeration \"allSearchResultAttributes\" list ",e);
			}
			try {
				if(listOfPeopleInAuthGroup!=null) {
					listOfPeopleInAuthGroup.close();
				}
			} catch (NamingException e) {
				M_log.error("Problem occurred while closing the NamingEnumeration \"listOfPeopleInAuthGroup\" list ",e);
			}
			try {
				if(dirContext!=null) {
					dirContext.close();
				}
			} catch (NamingException e) {
				M_log.error("Problem occurred while closing the  \"dirContext\"  object",e);
			}
		}

	}

}
