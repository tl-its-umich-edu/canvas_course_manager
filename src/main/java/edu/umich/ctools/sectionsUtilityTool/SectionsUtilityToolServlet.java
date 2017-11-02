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

import edu.umich.ctools.esb.utils.WAPI;
import edu.umich.ctools.esb.utils.WAPIException;
import edu.umich.ctools.esb.utils.WAPIResultWrapper;
import edu.umich.its.lti.TcSessionData;
import edu.umich.its.lti.utils.OauthCredentials;
import edu.umich.its.lti.utils.OauthCredentialsFactory;
import edu.umich.its.lti.utils.RequestSignatureUtils;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.VelocityViewServlet;
import org.apache.velocity.tools.view.ViewToolContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;

public class SectionsUtilityToolServlet extends VelocityViewServlet {

	private static Log M_log = LogFactory.getLog(SectionsUtilityToolServlet.class);
	private static final long serialVersionUID = 7284813350014385613L;

	//Constants
	private static final String CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS = "canvas.api.getcourse.by.uniqname.no.sections.regex";
	private static final String CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS_PAGED = "canvas.api.getcourse.by.uniqname.no.sections.paged.regex";
	private static final String CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS_MASK = "canvas.api.getcourse.by.uniqname.no.sections.mask.regex";
	private static final String CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS_MASK_PAGED = "canvas.api.getcourse.by.uniqname.no.sections.mask.paged.regex";
	private static final String CANVAS_API_GETALLSECTIONS_PER_COURSE = "canvas.api.getallsections.per.course.regex";
	private static final String CANVAS_API_GETSECTION_PER_COURSE = "canvas.api.getsection.per.course.regex";
	private static final String CANVAS_API_GETSECTION_INFO = "canvas.api.getsection.info.regex";
	private static final String CANVAS_API_DECROSSLIST = "canvas.api.decrosslist.regex";
	private static final String CANVAS_API_CROSSLIST = "canvas.api.crosslist.regex";
	private static final String CANVAS_API_CROSSLIST_MASK = "canvas.api.crosslist.mask.regex";
	private static final String CANVAS_API_GETCOURSE_INFO = "canvas.api.getcourse.info.regex";
	private static final String CANVAS_API_RENAME_COURSE = "canvas.api.rename.course.regex";
	private static final String CANVAS_API_GETCOURSE_BY_UNIQNAME = "canvas.api.getcourse.by.uniqname.regex";
	private static final String CANVAS_API_GETCOURSE_BY_UNIQNAME_MASK = "canvas.api.getcourse.by.uniqname.mask.regex";
	private static final String CANVAS_API_ENROLLMENT = "canvas.api.enrollment.regex";
	private static final String CANVAS_API_TERMS = "canvas.api.terms.regex";
	private static final String CANVAS_API_SEARCH_COURSES = "canvas.api.search.courses.regex";
	private static final String CANVAS_API_SEARCH_USER = "canvas.api.search.user.regex";
	private static final String CANVAS_API_CREATE_USER = "canvas.api.create.user.regex";
	private static final String CANVAS_API_ADD_USER = "canvas.api.add.user.regex";
	private static final String CANVAS_API_GET_COURSE = "canvas.api.get.single.course.regex";
	private static final String CANVAS_API_GET_COURSE_ENROLL = "canvas.api.get.single.course.enrollment.regex";
	private static final String MPATHWAYS_API_GNERIC = "mpathways.api.get.generic";
	private static final String ROLE_CAN_ADD_TEACHER = "role.can.add.teacher";
	private static final String GET_GROUPS= "get.groups.regex";
	private static final String GET_GROUPSET= "get.groupset.regex";
	private static final String POST_GROUPSET= "post.groupset.regex";

	private static final String M_PATH_DATA = "mPathData";
	private static final String LTI_1P0_CONST = "LTI-1p0";
	private static final String LTI_VERSION = "lti_version";

	private static final String PARAMETER_TERMID = "termid";

	private static final String MANAGER_SERVLET_NAME = "/manager";
	protected static final String MPATHWAYS_PATH_INFO = "/mpathways/Instructors";

	private final static String CCM_PROPERTY_FILE_PATH = "ccmPropsPath";
	private final static String CCM_SECURE_PROPERTY_FILE_PATH = "ccmPropsPathSecure";

	private static final String LAUNCH_TYPE = "launchType";
	private static final String LTI = "lti";

	private static final String DESIGNER_ENROLLMENT = "DesignerEnrollment";
	private static final String TEACHER_ENROLLMENT = "TeacherEnrollment";
	private static final String TA_ENROLLMENT = "TaEnrollment";

	//Member variables
	private String callType = null;
	private String ltiUrl = null;
	private String ltiKey = null;
	private String ltiSecret = null;
	private boolean isStubTesting = false;
	private OauthCredentialsFactory oacf;

	protected static Properties appExtSecurePropertiesFile=null;
	protected static Properties appExtPropertiesFile=null;

	private static List<String> rolesThatCanAddTeacherList = null;

	protected static List<SISDataHolderForEmail> canvasPollingIds =Collections.synchronizedList(new ArrayList<>());
	protected static int addedPollingIdCount;
	protected static int removedPollingIdCount;

	private static final HashMap<String, Integer> enrollmentsMap = new HashMap<String, Integer>(){
		private static final long serialVersionUID = -1389517682290891890L;

		{
			//Those who have a TeacherEnrollment or a DesignerEnrollment can
			//add anyone. Those users with a TaEnrollment type can only add
			//users with type lower that TaEnrollment, i.e. Student and
			//Observer Enrollments.
			put("ObserverEnrollment", 0);
			put("StudentEnrollment", 0);
			put("TaEnrollment", 1);
			put("TeacherEnrollment", 2);
			put("DesignerEnrollment", 2);
		}
	};

	private static final HashMap<String,String> apiListRegexWithDebugMsg = new HashMap<String,String>(){
		private static final long serialVersionUID = -1389517682290891890L;

		{
			put(CANVAS_API_TERMS, "for terms");
			put(CANVAS_API_CROSSLIST, "for crosslist");
			put(CANVAS_API_CROSSLIST_MASK, "for crosslist mask");
			put(CANVAS_API_RENAME_COURSE, "for rename a course");
			put(CANVAS_API_GETCOURSE_BY_UNIQNAME, "for getting courses by uniqname");
			put(CANVAS_API_GETCOURSE_BY_UNIQNAME_MASK, "for getting courses by masked uniqname");
			put(CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS, "for getting courses by uniqname not including sections");
			put(CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS_PAGED, "for getting pages of courses by uniqname not including sections");
			put(CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS_MASK, "for getting courses by masked uniqname not including sections");
			put(CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS_MASK_PAGED, "for getting pages of courses by masked uniqname not including sections");
			put(CANVAS_API_ENROLLMENT, "for enrollment");
			put(CANVAS_API_GETCOURSE_INFO, "for getting course info");
			put(CANVAS_API_DECROSSLIST,"for decrosslist");
			put(CANVAS_API_GETSECTION_INFO, "for getting section info");
			put(CANVAS_API_GETSECTION_PER_COURSE, "for getting section info for a given course");
			put(CANVAS_API_GETALLSECTIONS_PER_COURSE, "for getting all sections info for a given course");
			put(CANVAS_API_SEARCH_COURSES, "for searching courses");
			put(CANVAS_API_SEARCH_USER, "for searching for users");
			put(CANVAS_API_CREATE_USER, "for creating a user");
			put(CANVAS_API_ADD_USER, "for adding a user to a section");
			put(CANVAS_API_GET_COURSE, "for getting a single course");
			put(CANVAS_API_GET_COURSE_ENROLL,"for getting the enrollments of a user in a course");
			put(MPATHWAYS_API_GNERIC, "for mpathways calls");
			put(GET_GROUPS, "for getting the groups of the current course");
			put(GET_GROUPSET, "for getting the groupset of the current course");

		}
	};

	private static final ArrayList<String> allowedRoles = new ArrayList<String>(Arrays.asList("Primary Instructor",
			"Secondary Instructor",
			"Faculty Grader",
			"Graduate Student Instructor"));

	public void init() throws ServletException {
		M_log.debug(" Servlet init(): Called");
		configurePropertyValues();
		configureOauthCredentials();
		SISPollingThread polling=new SISPollingThread();
		Thread thread = new Thread(polling);
		thread.start();
		M_log.info("************* Thread Started"+thread.getId());
	}

	private void configurePropertyValues() {
		appExtPropertiesFile = Utils.loadProperties(CCM_PROPERTY_FILE_PATH);
		appExtSecurePropertiesFile = Utils.loadProperties(CCM_SECURE_PROPERTY_FILE_PATH);

		if(appExtSecurePropertiesFile!=null) {
			Utils.canvasToken = appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.PROPERTY_CANVAS_ADMIN);
			Utils.canvasURL = appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.PROPERTY_CANVAS_URL);
			isStubTesting = Boolean.valueOf(appExtPropertiesFile.getProperty(SectionUtilityToolFilter.PROPERTY_TEST_STUB));
			M_log.debug("isStubTesting: " + isStubTesting);
		}
		else {
			M_log.error("Failed to load system properties(sectionsToolProps.properties) for SectionsTool");
		}
	}

	protected void configureOauthCredentials() {

		// The oauth properties might have been injected on startup, so only set if
		// the value isn't yet set.

		if (appExtSecurePropertiesFile != null) {
			M_log.debug("oauthCredentials were injected");
		}
		else{
			// Try getting the oauth credentials properties file for the URL.  They should
			// not be in the main properties file they contain secrets.
			M_log.debug("try to set oauth credentials from properties url");
			appExtSecurePropertiesFile = Utils.loadProperties(CCM_SECURE_PROPERTY_FILE_PATH);
		}

		// if there still isn't a properties object then use a default set of properties.
		if (appExtSecurePropertiesFile == null) {
			M_log.debug("try to set oauth credentials from default.");
			appExtSecurePropertiesFile = new Properties();
			appExtSecurePropertiesFile.put("lmsng.school.edu.secret", "secret");
		}

		// Now turn the properties in a factory for creating specific
		// credentials.
		oacf = new OauthCredentialsFactory(appExtSecurePropertiesFile);
	}

	public void fillContext(Context context, HttpServletRequest request) {
		M_log.debug("fillContext() called");

		if ( SectionUtilityToolFilter.BASIC_LTI_LAUNCH_REQUEST.equals(request.getParameter(SectionUtilityToolFilter.LTI_MESSAGE_TYPE))) {
			storeContext(context, request);
		}
	}

	public void storeContext(Context context, HttpServletRequest request) {
		Map<String, String> ltiValues = new HashMap<String, String>();

		ViewToolContext vtc = (ViewToolContext)context;
		HttpServletResponse response = vtc.getResponse();
		HttpSession session= request.getSession(true);
		M_log.debug("session id: "+session.getId());
		CanvasAccountAdminFinder adminFinder = new CanvasAccountAdminFinder();
		boolean admin = adminFinder.isAccountAdmin(request);
		if (admin) {
			M_log.info(String.format("The user \"%s\" is account admin in the course %s",
					request.getParameter(Utils.LTI_PARAM_UNIQNAME), request.getParameter(Utils.LTI_PARAM_CANVAS_COURSE_ID)));
		}
		ToolAccessGranter accessGranter = new ToolAccessGranter(request.getParameter(Utils.LTI_PARAM_CANVAS_COURSE_ID),
				request.getParameter(Utils.LTI_PARAM_CANVAS_USER_ID));
		boolean allowedToolAccess=accessGranter.isAllowedAccess(admin);
		if (allowedToolAccess) {
			M_log.info(String.format("The user \"%s\" is granted access to the tool in the course %s",
					request.getParameter(Utils.LTI_PARAM_UNIQNAME), request.getParameter(Utils.LTI_PARAM_CANVAS_COURSE_ID)));
		}

		HashMap<String, Object> customValuesMap = new HashMap<>();

		customValuesMap.put(Utils.LTI_PARAM_CANVAS_COURSE_ID, request.getParameter(Utils.LTI_PARAM_CANVAS_COURSE_ID));
		customValuesMap.put(Utils.LTI_PARAM_CANVAS_ENROLLMENT_STATE, request.getParameter(Utils.LTI_PARAM_CANVAS_ENROLLMENT_STATE));
		customValuesMap.put(Utils.LTI_PARAM_UNIQNAME, request.getParameter(Utils.LTI_PARAM_UNIQNAME));
		customValuesMap.put(Utils.LTI_PARAM_CONTACT_EMAIL_PRIMARY, request.getParameter(Utils.LTI_PARAM_CONTACT_EMAIL_PRIMARY));
		customValuesMap.put(Utils.LTI_PARAM_LAST_NAME, request.getParameter(Utils.LTI_PARAM_LAST_NAME));
		customValuesMap.put(Utils.LTI_PARAM_FIRST_NAME, request.getParameter(Utils.LTI_PARAM_FIRST_NAME));
		customValuesMap.put(Utils.LTI_PARAM_CANVAS_USER_ID, request.getParameter(Utils.LTI_PARAM_CANVAS_USER_ID));
		customValuesMap.put(Utils.SESSION_ROLES_FOR_ADDING_TEACHER, appExtPropertiesFile.getProperty(ROLE_CAN_ADD_TEACHER));
		customValuesMap.put(Utils.IS_ACCOUNT_ADMIN, String.valueOf(admin));
		customValuesMap.put(Utils.IS_TOOL_ACCESS_ALLOWED, String.valueOf(allowedToolAccess));

		TcSessionData tc = (TcSessionData) session.getAttribute(Utils.TC_SESSION_DATA);

		OauthCredentials oac = oacf.getOauthCredentials(ltiKey);

		if (tc == null) {
			tc = new TcSessionData(request, oac, customValuesMap);
		}

		session.setAttribute(Utils.TC_SESSION_DATA,tc);
		M_log.debug("TC Session Data: " + tc.getUserId());

		// sanity check the result
		if (tc.getUserId() == null || tc.getUserId().length() == 0) {
			String msg = "Canvas Course Manager: tc session data is bad - userId is empty.";
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			M_log.error(msg);
			try {
				doError(request, response, "Canvas Course Manager LTI: tc session data is bad: userId is empty.");
			} catch (IOException e) {
				M_log.error("fillContext: IOException: ",e);
			}
			return;
		}

		if(customValuesMap.containsValue(null)){
			String msg = "Canvas Course Manager: Found launch parameters null.";
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			M_log.error(msg);
			try {
				doError(request, response, "Canvas Course Manager LTI: null Launch parameter found.");
			} catch (IOException e) {
				M_log.error("fillContext: IOException: ",e);
			}
			return;
		}

		// Verify this is an LTI launch request and some of the required parameters (if not stub testing)
		if( !isStubTesting ){
			if ( ! SectionUtilityToolFilter.BASIC_LTI_LAUNCH_REQUEST.equals(request.getParameter(SectionUtilityToolFilter.LTI_MESSAGE_TYPE)) ||
					!LTI_1P0_CONST.equals(request.getParameter(LTI_VERSION)) ||
					ltiKey == null) {
				try {
					M_log.debug("LTI request Message: " + request.getParameter(SectionUtilityToolFilter.LTI_MESSAGE_TYPE));
					M_log.debug("LTI request Version: " + request.getParameter(LTI_VERSION));
					M_log.debug("LTI Key: " + ltiKey);
					doError(request, response, "Missing required parameter:	LTI Message Type, LTI Version, or Consumer Key is incorrect.");
				} catch (IOException e) {
					M_log.error("fillContext: IOException: ",e);
				}
				return;
			}

			OauthCredentials oc = tc.getOauthCredentials();

			Boolean validMessage = checkForValidMessage(request, oc);
			if (!validMessage) {
				String msg = "Launch data does not validate";
				M_log.error(msg);
				return;
			}
		}

		// Fill context with the required lti values.
		// The VelocityViewServlet will take care of sending the processing on
		// to the proper velocity template.
		fillCcmValuesForContext(ltiValues,tc);

		context.put("ltiValues", ltiValues);
	}

	private void fillCcmValuesForContext(Map<String, String> ltiValues, TcSessionData tc) {
		HashMap<String, Object> customValuesMap = tc.getCustomValuesMap();
		M_log.info("**** CCM Values in Context are ****");
		for (String ltiparam : customValuesMap.keySet()) {
			ltiValues.put(ltiparam,(String)customValuesMap.get(ltiparam));
			M_log.info(String.format(ltiparam + "=" + customValuesMap.get(ltiparam)));
		}
	}


	private Boolean checkForValidMessage(HttpServletRequest request,
			OauthCredentials oc) {
		Boolean errorReturn = RequestSignatureUtils.validateMessage(request, oc);
		return !errorReturn;
	}

	// Deal nicely with error conditions.
	public void doError(HttpServletRequest request, HttpServletResponse response, String s)
			throws java.io.IOException {

		StringBuilder return_url = new StringBuilder();
		if (return_url != null && return_url.length() > 1) {
			return_url.append((return_url.indexOf("?") > 1) ? "&" : "?");
			return_url.append("lti_msg=").append(URLEncoder.encode(s,"UTF-8"));
			response.sendRedirect(return_url.toString());
			return;
		}
		PrintWriter out = response.getWriter();
		out.println(s);
	}

	public void doGet(HttpServletRequest request,HttpServletResponse response)
			throws IOException{
		M_log.debug("doGet: Called");
		M_log.info("request.getPathInfo(): " + request.getPathInfo());
		if( request.getPathInfo().equals(SectionUtilityToolFilter.LTI_PAGE) ){
			response.sendError(403);
			return;
		}
		else{
			try {
				if(request.getServletPath().equals(MANAGER_SERVLET_NAME)){
					M_log.debug("Manager Servlet invoked");
					canvasRestApiCall(request, response);
				}
				else{
					M_log.debug("NOT MANAGER");
					doRequest(request, response); //doRequest will always call fillContext()
				}
			}catch(Exception e) {
				M_log.error("GET request has some exceptions",e);
			}
		}
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)  {
		M_log.debug("doPOST: Called");
		try {
			//determine if this is an LTI Call or a browser call?
			if (request.getParameterMap().containsKey("oauth_consumer_key")) {
				processLti(request, response);
				return;
			}
			boolean isMultipart = ServletFileUpload.isMultipartContent(request);

			if (isMultipart) {
				CourseSupportProcess csp = new CourseSupportProcess(request, response);
				csp.getDataAndStartCourseSupportProcess();
				return;
			}
			canvasRestApiCall(request, response);
		} catch (Exception e) {
			M_log.error("POST request has some exceptions", e);
		}
	}

	private void processLti(HttpServletRequest request,
							HttpServletResponse response) throws IOException {
		for (Object e : request.getParameterMap().entrySet()) {
			Map.Entry<String, String[]> entry = (Map.Entry<String, String[]>) e;
			String name = entry.getKey();
			if (M_log.isDebugEnabled()) {
				for (String value : entry.getValue()) {
					M_log.debug(name + " = " + value);
				}
			}
		}

		ltiKey = request.getParameter("oauth_consumer_key");
		ltiSecret = appExtSecurePropertiesFile.getProperty(ltiKey + ".secret");

		// Verify valid LTI key & secret
		if( ltiKey == null || ltiSecret == null ) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			M_log.error("No LTI key and secret defined in ccmSecure.properties file");
			return;
		}
		if(ltiUrl == null) {
			ltiUrl = request.getRequestURL().toString();
		}
		// if not isStubTesting, call verifySignature to verify LTI oauth authorization
		if( isStubTesting || RequestSignatureUtils.verifySignature(request, ltiKey, ltiSecret, ltiUrl)){
			doRequest(request, response);
			return;
		}

		doError(request, response, "Missing required parameter:  Key, secret, or URL is incorrect.");
		return;
	}

	protected void doPut(HttpServletRequest request,HttpServletResponse response){
		M_log.debug("doPut: Called");
		try {
			canvasRestApiCall(request, response);
		}catch(Exception e) {
			M_log.error("PUT request has some exceptions",e);
		}
	}

	protected void doDelete(HttpServletRequest request,HttpServletResponse response) {
		M_log.debug("doDelete: Called");
		try {
			canvasRestApiCall(request, response);
		}catch(Exception e) {
			M_log.error("DELETE request has some exceptions",e);
		}
	}

	/*
	 * This method is handling all the different Api request like PUT, POST etc to canvas.
	 * We are using canvas admin token stored in the secure properties file to handle the request.
	 */
	private void canvasRestApiCall(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		request.setCharacterEncoding("UTF-8");
		M_log.debug("canvasRestApiCall(): called");
		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		if ( Utils.canvasToken == null || Utils.canvasURL == null ) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out = response.getWriter();
			out.print(appExtPropertiesFile.getProperty("property.file.load.error"));
			out.flush();
			M_log.error("Failed to load system properties(sectionsToolProps.properties) for SectionsTool");
			return;
		}
		if(isAllowedApiRequest(request)) {
			callType = appExtPropertiesFile.getProperty(SectionUtilityToolFilter.PROPERTY_CALL_TYPE);
			if(callType.equals("canvas")){
				apiConnectionLogic(request,response);
			}
			else{
				esbRestApiCall(request,response);
			}
		}else {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out = response.getWriter();
			out.print(appExtPropertiesFile.getProperty("api.not.allowed.error"));
			out.flush();
		}
	}

	private void esbRestApiCall(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		M_log.debug("esbRestApiCall() called");
		return;
		//Stub: to be implemented later when ESB to canvas call is ready
	}

	/*
	 * This function has logic that execute client(i.e., browser) request and get results from the canvas
	 * using Apache Http client library
	 * If the property for stub testing is set to true, stub testing will be performed. Stub testing may
	 * be done during load testing or other kinds of testing. The reason for stubbing is so that we load
	 * test the application and none of its dependencies.
	 */

	private void apiConnectionLogic(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		if(M_log.isDebugEnabled()){
			displayRequestHeaders(request);
		}

		PrintWriter out = response.getWriter();
		if( isStubTesting ){
			Utils.openFile(request, response, out);
			return;
		}
		if(request.getPathInfo().equalsIgnoreCase(MPATHWAYS_PATH_INFO)){
			mpathwaysCall(request, response, out);
		}
		else{
			getCanvasResponse(request, response, out);
		}
	}

	private void mpathwaysCall(HttpServletRequest request, HttpServletResponse response, PrintWriter out){
		WAPIResultWrapper wrappedResult = null;
		String uniqname = null;
		TcSessionData tc = (TcSessionData) request.getSession().getAttribute(Utils.TC_SESSION_DATA);
		if( tc != null){
			uniqname = (String) tc.getCustomValuesMap().get(Utils.LTI_PARAM_UNIQNAME);
		}
		M_log.debug("WAPI uniqname: " + uniqname);
		String originalUrl = request.getRequestURI();
		if(request.getQueryString() != null){
			originalUrl = originalUrl + "?" + request.getQueryString();
		}
		Utils.logApiCall(uniqname, originalUrl, request);
		String mpathwaysTermId = request.getParameter(PARAMETER_TERMID);
		//Friend accounts for Canvas will have a "+" in place of an "@" in their uniqnames
		if(uniqname == null || uniqname.contains("+") ||
				mpathwaysTermId == null){
			response.setStatus(400);
			String message = "Parameter missing in Instructors request OR you are using a friend account";
			wrappedResult = new WAPIResultWrapper(400, message, new JSONObject());
			M_log.error("Error in mpathwaysCall() " + message);
		}
		else{
			if(appExtSecurePropertiesFile!=null) {
				HashMap<String, String> wapiValuesMap = new HashMap<String, String>();
				wapiValuesMap.put("tokenServer", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_TOKEN_SERVER));
				wapiValuesMap.put("apiPrefix", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_PREFIX));
				wapiValuesMap.put("key", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_KEY));
				wapiValuesMap.put("secret", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_SECRET));
				wapiValuesMap.put("scope", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_SCOPE_INSTRUCTORS));
				wapiValuesMap.put("grant_type", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_GRANT_TYPE));
				WAPI wapi = new WAPI(wapiValuesMap);

				HashMap<String,String> headers = new HashMap<String,String>();
				headers.put("x-ibm-client-id",appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_IBM_CLIENT_ID));

				long startTime = System.currentTimeMillis();
				String errMsg = "MPATHWAYS API call did not complete successfully";
				try {
					String url = wapi.getApiPrefix() + uniqname + "/Terms/" + mpathwaysTermId + "/Classes";
					M_log.info("WAPI URL: " + url);
					wrappedResult = wapi.doRequest(url, headers);
					addMpathwayDataToSession(request, wrappedResult, mpathwaysTermId);
				} catch (WAPIException e) {
					M_log.error(errMsg, e);
				} catch (Exception e) {
					M_log.error(errMsg, e);
				} finally {
					long stopTime = System.currentTimeMillis();
					long elapsedTime = stopTime - startTime;
					M_log.info(String.format("MPATHWAYS API response took %sms", elapsedTime));
				}
			}
			if (wrappedResult == null) {
				response.setStatus(500);
				wrappedResult = new WAPIResultWrapper(500, "Unexpected error", new JSONObject());
			} else {
				response.setStatus(wrappedResult.getStatus());
			}
		}

		out.print(wrappedResult.toJson());
		out.flush();
	}

	public void addMpathwayDataToSession(HttpServletRequest request,
			WAPIResultWrapper wrappedResult, String mpathwaysTermId) {
		try{
			M_log.debug("Mapth Wrapped Result: " + wrappedResult.toJson().toString());
			//Add mpathways info to session data
			ArrayList<String> mPathwayData = new ArrayList<String>();
			JSONArray mPathJsonArray;
			//pluck result from json
			JSONObject result = new JSONObject(wrappedResult.toJson());
			mPathJsonArray = result.getJSONObject("Result").getJSONObject("getInstrClassListResponse").getJSONArray("InstructedClass");
			M_log.debug("mPathJsonArray: " + mPathJsonArray);

			for(int i = 0; i < mPathJsonArray.length(); i++){
				JSONObject childJSONObject = mPathJsonArray.getJSONObject(i);
				if(allowedRoles.contains(childJSONObject.get("InstructorRole"))){
					M_log.debug("Class Number: " + childJSONObject.get("ClassNumber"));
					mPathwayData.add(mpathwaysTermId + childJSONObject.get("ClassNumber").toString());
				}
			}

			if(M_log.isDebugEnabled()){
				for(String course : mPathwayData){
					M_log.debug("Course: " + course);
				}
			}

			HttpSession session = request.getSession(true);
			session.setAttribute(M_PATH_DATA,mPathwayData);
		}
		catch(JSONException e){
			M_log.error(e.getMessage());
			return;
		}
	}

	private void getCanvasResponse(HttpServletRequest request,
			HttpServletResponse response, PrintWriter out) throws IOException {
		String queryString = request.getQueryString();
		String pathInfo = request.getPathInfo();
		String url;
		if(queryString!=null) {
			url = Utils.canvasURL+pathInfo+"?"+queryString;
		}else {
			url = Utils.canvasURL+pathInfo;
		}

		String uniqname = null;

		TcSessionData tc = (TcSessionData) request.getSession().getAttribute(Utils.TC_SESSION_DATA);
		M_log.debug("TC Session Data: " + tc);

		if( tc != null){
			uniqname = (String) tc.getCustomValuesMap().get(Utils.LTI_PARAM_UNIQNAME);
		}

		Utils.logApiCall(uniqname, url, request);

		//useful for debugging
		//displaySessionAttributes(request);
		//displayKeyValuePairs(tc);

		String stringToReplaceUser = "user=self";
		String stringToReplaceCourse = "course_id";

		String originalUrl = url;

		//Retrieve Canvas Data from TC Session Data in order to mask user.
		//This API is being masked because a uniqname is considered sensitive data.
		url = unmaskUrl(url, tc, stringToReplaceUser, stringToReplaceCourse);

		HttpUriRequest clientRequest = null;
		if(request.getMethod().equals(Utils.GET)) {
			clientRequest = new HttpGet(url);
		}else if (request.getMethod().equals(Utils.POST)) {
			clientRequest = new HttpPost(url);
		}else if(request.getMethod().equals(Utils.PUT)) {
			clientRequest=new HttpPut(url);
		}else if(request.getMethod().equals(Utils.DELETE)) {
			clientRequest=new HttpDelete(url);
		}

		HttpResponse canvasResponse = Utils.executeApiCall(clientRequest);
		if(canvasResponse == null){
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			M_log.error(String.format("The Api call %s failed with errors", url));
			return;
		}
		int statusCode = canvasResponse.getStatusLine().getStatusCode();
		BufferedReader rd = new BufferedReader(new InputStreamReader(canvasResponse.getEntity().getContent()));

		String linkValue = null;

		linkValue = extractNextLink(canvasResponse, request.getSession());

		if(linkValue != null){
			response.addHeader("Next", linkValue);
		}

		String line = "";
		StringBuilder sb = new StringBuilder();
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}

		if( originalUrl.substring(originalUrl.indexOf("/api")).matches(appExtPropertiesFile.getProperty(CANVAS_API_GET_COURSE_ENROLL)) && request.getSession().getAttribute(LAUNCH_TYPE).equals(LTI)){
			addEnrollmentsToSession(request, sb);
		}
		response.setStatus(statusCode);
		if(statusCode / 100 != 2){
			M_log.error(String.format("Canvas Api \"%s\" call failed with status code %s due to %s ",url,statusCode,sb.toString()));
		}
		out.print(sb.toString());
		out.flush();
	}

	//When there is too much data to retrieve at once data will be paged.
	//When data is paged Link Headers will be send in http response. We are
	//only concerned with the Next Link Header. This method will extract header
	//if one exists.
	private String extractNextLink(HttpResponse canvasResponse, HttpSession session) {
		String linkValueString = null;
		String linkValueNext = null;
		String searchPhrase = "rel=\"next\"";
		M_log.debug("SEARCH PHRASE: " + searchPhrase);
		HashMap<String, String> links = new HashMap<String, String>();
		Header[] headers = canvasResponse.getHeaders("Link");
		for (Header header : headers) {
			M_log.debug("Key : " + header.getName()
					+ " ,Value : " + header.getValue());
			if(header.getName().equals("Link")){
				linkValueString = header.getValue();
				String[] headerPairs = linkValueString.split(",");
				for(String headerPair : headerPairs){
					M_log.debug("String: " + headerPair);
					String[] splitPairs = headerPair.split(";");
					splitPairs[0] = splitPairs[0].replace("<","");
					splitPairs[0] = splitPairs[0].replace(">","");
					links.put(splitPairs[1].trim(), splitPairs[0]);
				}
				break;
			}
		}

		if(links.containsKey(searchPhrase)){
			M_log.debug("LINKS CONTAINS NEXT: " + links.get(searchPhrase));
			linkValueNext = "/canvasCourseManager/manager" + links.get(searchPhrase).substring(links.get(searchPhrase).indexOf("/api"), links.get(searchPhrase).length());
			if(linkValueNext.contains("as_user_id") && session.getAttribute(SectionUtilityToolFilter.LAUNCH_TYPE) == LTI){
				linkValueNext = linkValueNext.replaceAll("as_user_id=sis_login_id.*?&", "user=self&");
			}
		}

		M_log.debug("LINKS; " + links);
		M_log.debug("NEXT LINK VALUE: " + linkValueNext);
		return linkValueNext;
	}

	private void addEnrollmentsToSession(HttpServletRequest request,
			StringBuilder sb) {
		if(rolesThatCanAddTeacherList == null){
			rolesThatCanAddTeacherList = generateAuthorizedRolesList();
		}

		M_log.debug("ENROLLMENTS: " + sb.toString());
		HashMap<Integer, String> enrollmentsFound =  new HashMap<Integer, String>();
		JSONArray enrollmentsArray = new JSONArray(sb.toString());

		//iterate through new enrollments
		//if enrollment type == teacherEnrollment or (DesignerEnrollment AND Designer Role) then add it to enrollments found and break
		//Else enrollment type is TaEnrollment
		//This is because Teachers and Designers can add friends at teacher level, but Librarians (a type of Designer) can only add students
		for(int i = 0; i < enrollmentsArray.length(); i++){
			JSONObject childJSONObject = enrollmentsArray.getJSONObject(i);
			M_log.debug("ENROLLMENT RECORD: " + childJSONObject.get("course_id") + " " + childJSONObject.get("course_section_id") + " " + childJSONObject.get("type") + " " + childJSONObject.get("role"));

			JSONObject enrollmentRecord = new JSONObject();
			enrollmentRecord.put("role", childJSONObject.get("role"));

			M_log.debug("ENROLLMENT RECORD TO COMPARE: " + enrollmentRecord);

			String newRole = childJSONObject.getString("role");

			boolean canAddTeacher = false;

			if(rolesThatCanAddTeacherList.contains(newRole)){
				M_log.debug("USER ROLE FOUND IN AUTHORIZED ROLES LIST");
				canAddTeacher = true;
			}

			if(canAddTeacher){
				//Here the user will be given a teacher enrollment status even if the user is a designer with a designer role
				//as the only time this is consulted is when adding friend accounts
				enrollmentsFound.put(childJSONObject.getInt("course_id"), TEACHER_ENROLLMENT);
				break;
			}
			else{
				enrollmentsFound.put(childJSONObject.getInt("course_id"), TA_ENROLLMENT);
			}

		}
		request.getSession().setAttribute("enrollments", enrollmentsFound);
		M_log.debug("SESSION ENROLLMENTS: " + request.getSession().getAttribute("enrollments"));
	}

	private List<String> generateAuthorizedRolesList() {
		String roleCanAddTeacherString = appExtPropertiesFile.getProperty(ROLE_CAN_ADD_TEACHER);

		M_log.debug("ROLE CAN ADD TEACHER STRING: " + roleCanAddTeacherString);

		//convert string to json object
		JSONObject roleCanAddTeacherJson = new JSONObject(roleCanAddTeacherString);
		M_log.debug("ROLE CAN ADD TEACHER JSON OBJECT: " + roleCanAddTeacherJson);

		JSONArray roleCanAddTeacherJsonArray = roleCanAddTeacherJson.getJSONArray("roles");

		//implement lists to make compare easier
		List<String> rolesThatCanAddTeacherList = new ArrayList<String>();
		for(int i = 0; i < roleCanAddTeacherJsonArray.length(); i++){
			rolesThatCanAddTeacherList.add(roleCanAddTeacherJsonArray.getJSONObject(i).getString("role"));
		}
		return rolesThatCanAddTeacherList;
	}

	//Canvas adds custom parameters for lti launches. These custom paramerers
	//include user_login and course_id. We use these parameters to unmask the
	//API calls.
	public String unmaskUrl(String url, TcSessionData tc,
			String stringToReplaceUser, String stringToReplaceCourse) {
		if(tc == null){
			return url;
		}

		String uniqname = (String) tc.getCustomValuesMap().get(Utils.LTI_PARAM_UNIQNAME);
		String courseId = (String) tc.getCustomValuesMap().get(Utils.LTI_PARAM_CANVAS_COURSE_ID);
		M_log.debug("uniqname: " + uniqname);
		String replaceUserIdValue = "as_user_id=sis_login_id:" + uniqname;
		if(url.toLowerCase().contains(stringToReplaceUser.toLowerCase())){
			url = url.replace(stringToReplaceUser.toLowerCase(), replaceUserIdValue);
		}
		if(url.toLowerCase().contains(stringToReplaceCourse.toLowerCase())){
			url = url.replace(stringToReplaceCourse.toLowerCase(), courseId);
		}
		M_log.debug("New URL: " + url);

		return url;
	}

	public void displayKeyValuePairs(TcSessionData tc) {
		Iterator tcIterator = tc.getCustomValuesMap().entrySet().iterator();
		while(tcIterator.hasNext()){
			Map.Entry pair = (Map.Entry)tcIterator.next();
			M_log.debug(pair.getKey() + " = " + pair.getValue());
		}
	}

	public void displaySessionAttributes(HttpServletRequest request) {
		Enumeration attrs = request.getSession().getAttributeNames();
		while(attrs.hasMoreElements()){
			M_log.debug("Attribute: " + attrs.nextElement());
		}
	}

	public void displayRequestHeaders(HttpServletRequest request) {
		Enumeration headerNames = request.getHeaderNames();
		while(headerNames.hasMoreElements()){
			M_log.debug("HEADER NAME: " + headerNames.nextElement());
		}
	}

	public void testMpathwaysCall(PrintWriter out) {
		try{
			M_log.info("MPathways call stub");
			//Sample File for strict use of testing esb calls made from front end application
			File testFile = new File( this.getClass().getResource("/mpathwaysSample.txt").toURI() );
			FileReader fr = new FileReader(testFile);
			BufferedReader rd = new BufferedReader(fr);;
			String line = "";
			StringBuilder sb = new StringBuilder();
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			out.print(sb.toString());
			out.flush();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	/*
	 * This method control canvas api request allowed. If a particular request from UI is not in the allowed list then it will not process the request
	 * and sends an error to the UI. Using regex to match the incoming request
	 */

	private boolean isAllowedApiRequest(HttpServletRequest request) {
		M_log.debug("isAllowedApiRequest(): called");
		HttpSession session = request.getSession(true);
		String url;
		String queryString = request.getQueryString();
		String pathInfo = request.getPathInfo();
		boolean isAllowedRequest=false;
		if(queryString!=null) {
			url=pathInfo+"?"+queryString;
			isAllowedRequest=isApiFoundIntheList(url);
		}else {
			url=pathInfo;
			isAllowedRequest=isApiFoundIntheList(url);
		}

		if(!session.getAttribute(LAUNCH_TYPE).equals(LTI)){
			return isAllowedRequest;
		}

		//typical crosslist call is not allowed in the LTI version
		if( url.matches(appExtPropertiesFile.getProperty(CANVAS_API_CROSSLIST))){
			isAllowedRequest = false;
		}

		//only the masked crosslist call is allowed in the LTI version
		if( url.matches(appExtPropertiesFile.getProperty(CANVAS_API_CROSSLIST_MASK))){
			isAllowedRequest = isCrosslistAllowed(request, session, url);
		}

		if( url.matches(appExtPropertiesFile.getProperty(CANVAS_API_ADD_USER))){
			isAllowedRequest = isAddUserAllowed(request, url);
		}

		return isAllowedRequest;
	}

	private boolean isAddUserAllowed(HttpServletRequest request, String url) {
		boolean isCallAllowed = false;
		//when using the substring method, the +9, +17, +x, the int is the
		//number of characters in the length of the string to skip.
		String sectionString = url.substring(url.indexOf("sections/")+9, url.indexOf("/enrollments"));
		String enrollmentTypeFromRequest = url.substring(url.indexOf("enrollment[type]=")+17, url.length());
		M_log.debug("SECTION_STRING: " + sectionString);
		M_log.debug("ENROLLMENT_TYPE: " + enrollmentTypeFromRequest);

		//build api call
		String sectionsApiCall = Utils.canvasURL + "/api/v1/sections/" + sectionString;
		M_log.debug("SECTIONS API CALL: " + sectionsApiCall);

		//string built, time to make call
		String uniqname = null;
		boolean isAdmin = false;
		TcSessionData tc = (TcSessionData) request.getSession().getAttribute(Utils.TC_SESSION_DATA);
		if( tc != null){
			uniqname = (String) tc.getCustomValuesMap().get(Utils.LTI_PARAM_UNIQNAME);
			String isAdminStrg = (String) tc.getCustomValuesMap().get(Utils.IS_ACCOUNT_ADMIN);
			isAdmin=Boolean.valueOf(isAdminStrg);
		}

		Utils.logApiCall(uniqname, sectionsApiCall, request);

		HttpUriRequest clientRequest = null;

		clientRequest = new HttpGet(sectionsApiCall);

		HttpResponse canvasResponse = Utils.executeApiCall(clientRequest);
		BufferedReader rd = null;
		try {
			rd = new BufferedReader(new InputStreamReader(canvasResponse.getEntity().getContent()));
		} catch (IOException e) {
			M_log.error("Canvas API call did not complete successfully", e);
			return false;
		} catch (NullPointerException e){
			M_log.error("Canvas API call did not complete successfully", e);
			return false;
		}


		String line = "";
		StringBuilder sb = new StringBuilder();

		try {
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			M_log.error("Canvas API call did not complete successfully", e);
			return false;
		}
		M_log.debug("RESPONSE TO isAddUserAllowed: " + sb.toString());

		Integer courseIdFromRequest = null;
		try{
			//If JSON isn't properly formed catch exception
			JSONObject sectionsCallResponse = new JSONObject( sb.toString() );
			courseIdFromRequest = sectionsCallResponse.getInt("course_id");
		}
		catch(JSONException e){
			M_log.error("JSONException found attempting to process sectionsCallResponse");
			return false;
		}
		M_log.debug("COURSE ID FROM REQUEST: " + courseIdFromRequest);

		HashMap<Integer, String> enrollmentsFound = (HashMap<Integer, String>) request.getSession().getAttribute("enrollments");
		M_log.debug("SESSION ENROLLMENTS FOUND: " + request.getSession().getAttribute("enrollments"));

		String enrollmentTypeFromSession = enrollmentsFound.get(courseIdFromRequest);
		M_log.debug("ENROLLMENT FOUND: " + enrollmentTypeFromSession);

		if(enrollmentTypeFromSession == null){
			// while adding friend account only teacher/observer/ta can add enrollments to course that is ruled
			// by login in the addEnrollmentsToSession(), but some cases admins need to do this so this check will enforce
			// they can do it other wise they have to add them self to course or masquerade to add users.
			if(isAdmin){
				M_log.info(String.format("Account Admin \"%s\" adding user to Course %s ",uniqname,courseIdFromRequest));
				return true;
			}
			return false;
		}

		M_log.debug("ENROLLMENT FROM REQUEST: " + enrollmentTypeFromRequest);

		if(enrollmentsMap.containsKey(enrollmentTypeFromRequest) == false){
			return false;
		}

		isCallAllowed = compareRanks(enrollmentTypeFromRequest,
				enrollmentTypeFromSession);

		M_log.debug("IS CALL ALLOWED: " + isCallAllowed);

		return isCallAllowed;
	}

	private boolean compareRanks(String enrollmentTypeFromRequest,
			String enrollmentTypeFromSession) {
		boolean isCallAllowed;

		int teacherDesignerEnrollmentRank = 2;
		int taEnrollmentRank = 1;

		int enrollmentValueFromRequest = enrollmentsMap.get(enrollmentTypeFromRequest);
		int enrollmentValueFromSession = enrollmentsMap.get(enrollmentTypeFromSession);

		M_log.debug("ENROLLMENT TYPE VALUE: " + enrollmentValueFromRequest);
		M_log.debug("ENROLLMENT FOUND VALUE: " + enrollmentValueFromSession);

		//Enrollment types are ordered by rank. If you are of the same or
		//higher rank, then you can add user with rank in request, otherwise
		//fail.
		if(enrollmentValueFromSession == teacherDesignerEnrollmentRank){
			M_log.debug("NON UMICH USER ADD ALLOWED BY TEACHER OR DESIGNER");
			isCallAllowed = true;
		}
		else if(enrollmentValueFromSession == taEnrollmentRank && enrollmentValueFromSession > enrollmentValueFromRequest){
			M_log.debug("NON UMICH USER ADD ALLOWED BY TEACHER OR DESIGNER");
			isCallAllowed = true;
		}
		else{
			M_log.debug("API CALL REJECTED DUE TO INSUFFICIENT PERMISSION");
			isCallAllowed = false;
		}
		return isCallAllowed;
	}

	private boolean isCrosslistAllowed(HttpServletRequest request,
			HttpSession session, String url) {
		boolean isSectionMatch = false;

		//build api call
		String crosslistApiCall = Utils.canvasURL + url.substring(0, url.indexOf("/crosslist"));
		M_log.debug("crosslist API call: " + crosslistApiCall);

		String uniqname = null;
		TcSessionData tc = (TcSessionData) request.getSession().getAttribute(Utils.TC_SESSION_DATA);
		if( tc != null){
			uniqname = (String) tc.getCustomValuesMap().get(Utils.LTI_PARAM_UNIQNAME);
		}

		Utils.logApiCall(uniqname, url, request);
		HttpUriRequest clientRequest = null;

		clientRequest = new HttpGet(crosslistApiCall);

		HttpResponse canvasResponse = Utils.executeApiCall(clientRequest);
		BufferedReader rd = null;
		try {
			rd = new BufferedReader(new InputStreamReader(canvasResponse.getEntity().getContent()));
		} catch (IOException e) {
			M_log.error("Canvas API call did not complete successfully", e);
		} catch(NullPointerException e){
			M_log.error("Canvas API call did not complete successfully", e);
		}

		String line = "";
		StringBuilder sb = new StringBuilder();

		try {
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			M_log.error("Canvas API call did not complete successfully", e);
		}
		M_log.debug("RESPONSE TO isCrosslistAllowed: " + sb.toString());

		String sisSectionId = null;
		try{
			JSONObject crosslistSectionResponse = new JSONObject( sb.toString() );
			sisSectionId = crosslistSectionResponse.getString("sis_section_id");
		}
		catch(JSONException e){
			M_log.error("JSONException found attempting to process sectionsCallResponse");
			return false;
		}
		M_log.debug("SIS SECTION ID: " + sisSectionId);

		M_log.debug("session id: "+session.getId());
		ArrayList<String> courses = (ArrayList<String>) session.getAttribute(M_PATH_DATA);
		if(M_log.isDebugEnabled()){
			if(courses != null){
				for(String section : courses){
					M_log.debug("CrossSection: " + section);
				}
			}
		}

		if(courses.contains(sisSectionId)){
			M_log.info("SECTION MATCH FOUND - CROSSLIST CALL ALLOWED");
			isSectionMatch = true;
		}
		else{
			M_log.info("API CALL REJECTED DUE TO CROSSLIST MISMATCH");
			isSectionMatch = false;
		}

		return isSectionMatch;
	}

	/*
	 * This helper method iterate through the list of api's that sections tool have and if a match is found then logs associated debug message.
	 */
	private boolean isApiFoundIntheList(String url) {
		M_log.debug("isApiFoundIntheList(): called");
		String prefixDebugMsg="The canvas api request ";
		boolean isMatch=false;
		Set<String> apiListRegex = apiListRegexWithDebugMsg.keySet();
		for (String api : apiListRegex) {
			M_log.debug("URL: " + url);
			M_log.debug("API: " + appExtPropertiesFile.getProperty(api));
			if(url.matches(appExtPropertiesFile.getProperty(api))) {
				M_log.debug(prefixDebugMsg+apiListRegexWithDebugMsg.get(api));
				isMatch= true;
				break;
			}
		}
		return isMatch;
	}

}
