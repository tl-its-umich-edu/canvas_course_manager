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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.VelocityViewServlet;
import org.apache.velocity.tools.view.ViewToolContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mashape.unirest.http.exceptions.UnirestException;

import edu.umich.ctools.esb.utils.WAPI;
import edu.umich.ctools.esb.utils.WAPIResultWrapper;
import edu.umich.its.lti.TcSessionData;
import edu.umich.its.lti.utils.OauthCredentials;
import edu.umich.its.lti.utils.OauthCredentialsFactory;
import edu.umich.its.lti.utils.RequestSignatureUtils;

public class SectionsUtilityToolServlet extends VelocityViewServlet {

	private static Log M_log = LogFactory.getLog(SectionsUtilityToolServlet.class);
	private static final long serialVersionUID = 7284813350014385613L;

	//Constants
	private static final String CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS = "canvas.api.getcourse.by.uniqname.no.sections.regex";
	private static final String CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS_MASK = "canvas.api.getcourse.by.uniqname.no.sections.mask.regex";
	private static final String CANVAS_API_GETALLSECTIONS_PER_COURSE = "canvas.api.getallsections.per.course.regex";
	private static final String CANVAS_API_GETSECTION_PER_COURSE = "canvas.api.getsection.per.course.regex";
	private static final String CANVAS_API_GETSECTION_INFO = "canvas.api.getsection.info.regex";
	private static final String CANVAS_API_DECROSSLIST = "canvas.api.decrosslist.regex";
	private static final String CANVAS_API_CROSSLIST = "canvas.api.crosslist.regex";
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

	private static final String CUSTOM_CANVAS_COURSE_ID = "custom_canvas_course_id";
	private static final String CUSTOM_CANVAS_ENROLLMENT_STATE = "custom_canvas_enrollment_state";
	private static final String CUSTOM_CANVAS_USER_LOGIN_ID = "custom_canvas_user_login_id";
	private static final String LIS_PERSON_CONTACT_EMAIL_PRIMARY = "lis_person_contact_email_primary";
	private static final String LIS_PERSON_NAME_FAMILY = "lis_person_name_family";
	private static final String LIS_PERSON_NAME_GIVEN = "lis_person_name_given";

	private static final String TC_SESSION_DATA = "tcSessionData";
	private static final String M_PATH_DATA = "mPathData";
	private static final String LTI_1P0_CONST = "LTI-1p0";
	private static final String LTI_VERSION = "lti_version";

	private static final String PARAMETER_TERMID = "termid";

	private static final String MANAGER_SERVLET_NAME = "/manager";
	protected static final String MPATHWAYS_PATH_INFO = "/mpathways/Instructors";

	private final static String CCM_PROPERTY_FILE_PATH = "ccmPropsPath";
	private final static String CCM_SECURE_PROPERTY_FILE_PATH = "ccmPropsPathSecure";

	private static final String LAUNCH_TYPE = "launchType";

	private static final String DELETE = "DELETE";
	private static final String POST = "POST";
	private static final String GET = "GET";
	private static final String PUT = "PUT";

	//Member variables
	private String canvasToken;
	private String canvasURL;
	private String callType;
	private String ltiUrl;
	private String ltiKey;
	private String ltiSecret;
	private OauthCredentialsFactory oacf;

	protected static Properties appExtSecurePropertiesFile=null;
	protected static Properties appExtPropertiesFile=null;	

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
			put(CANVAS_API_RENAME_COURSE, "for rename a course");
			put(CANVAS_API_GETCOURSE_BY_UNIQNAME, "for getting courses by uniqname");
			put(CANVAS_API_GETCOURSE_BY_UNIQNAME_MASK, "for getting courses by masked uniqname");
			put(CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS, "for getting courses by uniqname not including sections");
			put(CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS_MASK, "for getting courses by masked uniqname not including sections");
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
		}
	};

	private static final ArrayList<String> allowedRoles = new ArrayList<String>(Arrays.asList("Primary Instructor",
			"Secondary Instructor",
			"Faculty grader",
			"Graduate Student Instructor"));

	public void init() throws ServletException {
		M_log.debug(" Servlet init(): Called");
		appExtPropertiesFile = Utils.loadProperties(CCM_PROPERTY_FILE_PATH);
		appExtSecurePropertiesFile = Utils.loadProperties(CCM_SECURE_PROPERTY_FILE_PATH);
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

		String oauth_consumer_key = appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.PROPERTY_LTI_KEY);
		ViewToolContext vtc = (ViewToolContext)context;
		HttpServletResponse response = vtc.getResponse();
		HttpSession session= request.getSession(true);
		M_log.debug("session id: "+session.getId());

		HashMap<String, Object> customValuesMap = new HashMap<String, Object>();

		customValuesMap.put(CUSTOM_CANVAS_COURSE_ID, request.getParameter(CUSTOM_CANVAS_COURSE_ID));
		customValuesMap.put(CUSTOM_CANVAS_ENROLLMENT_STATE, request.getParameter(CUSTOM_CANVAS_ENROLLMENT_STATE));
		customValuesMap.put(CUSTOM_CANVAS_USER_LOGIN_ID, request.getParameter(CUSTOM_CANVAS_USER_LOGIN_ID));
		customValuesMap.put(LIS_PERSON_CONTACT_EMAIL_PRIMARY, request.getParameter(LIS_PERSON_CONTACT_EMAIL_PRIMARY));
		customValuesMap.put(LIS_PERSON_NAME_FAMILY, request.getParameter(LIS_PERSON_NAME_FAMILY));
		customValuesMap.put(LIS_PERSON_NAME_GIVEN, request.getParameter(LIS_PERSON_NAME_GIVEN));

		TcSessionData tc = (TcSessionData) session.getAttribute(TC_SESSION_DATA);

		OauthCredentials oac = oacf.getOauthCredentials(oauth_consumer_key);

		if (tc == null) {
			tc = new TcSessionData(request, oac, customValuesMap);
		}

		session.setAttribute(TC_SESSION_DATA,tc);
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

		// Verify this is an LTI launch request and some of the required parameters.
		if ( ! SectionUtilityToolFilter.BASIC_LTI_LAUNCH_REQUEST.equals(request.getParameter(SectionUtilityToolFilter.LTI_MESSAGE_TYPE)) ||
				! LTI_1P0_CONST.equals(request.getParameter(LTI_VERSION)) ||
				oauth_consumer_key == null) {
			try {
				M_log.debug("LTI Message: " + request.getParameter(SectionUtilityToolFilter.LTI_MESSAGE_TYPE));
				M_log.debug("LTI Version: " + request.getParameter(LTI_VERSION));
				M_log.debug("LTI Key: " + oauth_consumer_key);
				doError(request, response, "Missing required parameter:  LTI Message Type, LTI Version, or Consumer Key is incorrect.");
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

		// Fill context with the required lti values.
		// The VelocityViewServlet will take care of sending the processing on
		// to the proper velocity template.
		fillCcmValuesForContext(ltiValues, request);

		context.put("ltiValues", ltiValues);
	}

	public void fillCcmValuesForContext(Map<String, String> ltiValues, HttpServletRequest request) {
		ltiValues.put(CUSTOM_CANVAS_COURSE_ID, request.getParameter(CUSTOM_CANVAS_COURSE_ID));
		ltiValues.put(CUSTOM_CANVAS_ENROLLMENT_STATE, request.getParameter(CUSTOM_CANVAS_ENROLLMENT_STATE));
		ltiValues.put(CUSTOM_CANVAS_USER_LOGIN_ID, request.getParameter(CUSTOM_CANVAS_USER_LOGIN_ID));
		ltiValues.put(LIS_PERSON_CONTACT_EMAIL_PRIMARY, request.getParameter(LIS_PERSON_CONTACT_EMAIL_PRIMARY));
		ltiValues.put(LIS_PERSON_NAME_FAMILY, request.getParameter(LIS_PERSON_NAME_FAMILY));
		ltiValues.put(LIS_PERSON_NAME_GIVEN, request.getParameter(LIS_PERSON_NAME_GIVEN));

		M_log.info("Course ID: " + ltiValues.get(CUSTOM_CANVAS_COURSE_ID));
		M_log.info("Enrollment State: " + ltiValues.get(CUSTOM_CANVAS_ENROLLMENT_STATE));
		M_log.info("Login ID: " + ltiValues.get(CUSTOM_CANVAS_USER_LOGIN_ID));
		M_log.info("Primary Email: " + ltiValues.get(LIS_PERSON_CONTACT_EMAIL_PRIMARY));
		M_log.info("Last Name: " + ltiValues.get(LIS_PERSON_NAME_FAMILY));
		M_log.info("First Name: " + ltiValues.get(LIS_PERSON_NAME_GIVEN));
	}

	public Boolean checkForValidMessage(HttpServletRequest request,
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
					M_log.info("NOT MANAGER");
					doRequest(request, response); //doRequest will always call fillContext()
				}
			}catch(Exception e) {
				M_log.error("GET request has some exceptions",e);
			}
		}
	}

	public void doPost(HttpServletRequest request,HttpServletResponse response){
		M_log.debug("doPOST: Called");
		try {
			//determine if this is an LTI Call or a browser call?
			if(request.getParameterMap().containsKey("oauth_consumer_key")){
				processLti(request, response);
				return;
			}
			canvasRestApiCall(request, response);
		}catch(Exception e) {
			M_log.error("POST request has some exceptions",e);
		}
	}

	public void processLti(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		for (Object e : request.getParameterMap().entrySet()) {
			Map.Entry<String, String[]> entry = (Map.Entry<String, String[]>) e;
			String name = entry.getKey();
			if(M_log.isDebugEnabled()){
				for (String value : entry.getValue()) {
					M_log.debug(name + " = " + value);
				}
			}
		}
		//Properties appExtSecureProperties = SectionUtilityToolFilter.appExtSecurePropertiesFile;
		if(appExtSecurePropertiesFile!=null) {
			ltiKey = appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.PROPERTY_LTI_KEY);
			ltiSecret = appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.PROPERTY_LTI_SECRET);
			if(appExtPropertiesFile.getProperty(SectionUtilityToolFilter.PROPERTY_LTI_URL) != null){
				ltiUrl = appExtPropertiesFile.getProperty(SectionUtilityToolFilter.PROPERTY_LTI_URL);
			}
			else{
				ltiUrl = request.getRequestURL().toString();
			}
			M_log.debug("ltiKey from props: "    + ltiKey);
			M_log.debug("ltiSecret from props: " + ltiSecret);
			M_log.debug("ltiUrl from props: "    + ltiUrl);
		}
		else {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			M_log.error("Failed to load system properties(sectionsToolProps.properties) for SectionsTool");
			return;
		}
		//method verifySignature is used to verify LTI oauth authorization
		if(RequestSignatureUtils.verifySignature(request, ltiKey, ltiSecret, ltiUrl)){
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
		Properties appExtSecureProperties = SectionUtilityToolFilter.appExtSecureProperties;
		if(appExtSecureProperties!=null) {
			canvasToken = appExtSecureProperties.getProperty(SectionUtilityToolFilter.PROPERTY_CANVAS_ADMIN);
			canvasURL = appExtSecureProperties.getProperty(SectionUtilityToolFilter.PROPERTY_CANVAS_URL);
		}
		else {
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
		PrintWriter out = response.getWriter();
		String isStubTesting = appExtPropertiesFile.getProperty(SectionUtilityToolFilter.PROPERTY_TEST_STUB);
		M_log.debug("isStubTesting: " + isStubTesting);
		if(Boolean.valueOf(isStubTesting)){
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
		TcSessionData tc = (TcSessionData) request.getSession().getAttribute(TC_SESSION_DATA);
		if( tc != null){
			uniqname = (String) tc.getCustomValuesMap().get("custom_canvas_user_login_id");
		}
		M_log.debug("WAPI uniqname: " + uniqname);
		String mpathwaysTermId = request.getParameter(PARAMETER_TERMID);
		if(uniqname == null || 
				mpathwaysTermId == null){
			response.setStatus(400);
			wrappedResult = new WAPIResultWrapper(400, "Parameter missing in Instructors request", new JSONObject());
			M_log.error("Error in mpathwaysCall(), missing parameter in Instuctors request");
		}
		else{
			if(appExtSecurePropertiesFile!=null) {
				HashMap<String, String> wapiValuesMap = new HashMap<String, String>();
				wapiValuesMap.put("tokenServer", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_TOKEN_SERVER));
				wapiValuesMap.put("apiPrefix", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_PREFIX));
				wapiValuesMap.put("key", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_KEY));
				wapiValuesMap.put("secret", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_SECRET));
				WAPI wapi = new WAPI(wapiValuesMap);
				try {
					String url = wapi.getApiPrefix() + uniqname + "/Terms/" + mpathwaysTermId + "/Classes";
					M_log.info("WAPI URL: " + url);
					wrappedResult = wapi.getRequest(url);
					addMpathwayDataToSession(request, wrappedResult, mpathwaysTermId);
				} catch (UnirestException e) {
					M_log.error("MPathways API call did not complete successfully", e);
				}	
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
			url = canvasURL+pathInfo+"?"+queryString;
		}else {
			url = canvasURL+pathInfo;
		}

		TcSessionData tc = (TcSessionData) request.getSession().getAttribute(TC_SESSION_DATA);
		M_log.debug("TC Session Data: " + tc);

		//useful for debugging
		//displaySessionAttributes(request);
		//displayKeyValuePairs(tc);

		String stringToReplaceUser = "user=self";
		String stringToReplaceCourse = "course_id";

		//Retrieve Canvas Data from TC Session Data in order to mask user. 
		//This API is being masked because a uniqname is considered sensitive data.
		String originalUrl = url;
		url = unmaskUrl(url, tc, stringToReplaceUser, stringToReplaceCourse);

		String sessionId = request.getSession().getId();
		String loggingApiWithSessionInfo = String.format("Canvas API request with Session Id \"%s\" for URL \"%s\"", sessionId,url);
		M_log.info(loggingApiWithSessionInfo);
		HttpUriRequest clientRequest = null;
		if(request.getMethod().equals(GET)) {
			clientRequest = new HttpGet(url);
		}else if (request.getMethod().equals(POST)) {
			clientRequest = new HttpPost(url);
		}else if(request.getMethod().equals(PUT)) {
			clientRequest=new HttpPut(url);
		}else if(request.getMethod().equals(DELETE)) {
			clientRequest=new HttpDelete(url);
		}
		BufferedReader rd = processApiCall(clientRequest);
		String line = "";
		StringBuilder sb = new StringBuilder();
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}

		if( originalUrl.substring(originalUrl.indexOf("/api")).matches(appExtPropertiesFile.getProperty(CANVAS_API_GET_COURSE_ENROLL)) && request.getSession().getAttribute(LAUNCH_TYPE).equals("lti")){
			addEnrollmentsToSession(request, sb);
		}

		out.print(sb.toString());
		out.flush();
	}

	private void addEnrollmentsToSession(HttpServletRequest request,
			StringBuilder sb) {
		M_log.debug("ENROLLMENTS: " + sb.toString());
		HashMap<Integer, String> enrollmentsFound =  new HashMap<Integer, String>();
		JSONArray enrollmentsArray = new JSONArray(sb.toString());
		for(int i = 0; i < enrollmentsArray.length(); i++){
			JSONObject childJSONObject = enrollmentsArray.getJSONObject(i);
			M_log.debug("ENROLLMENT RECORD: " + childJSONObject.get("course_id") + " " + childJSONObject.get("course_section_id") + " " + childJSONObject.get("type"));
			//if the key is already in the map, check and see if the new value is greater for that key
			//if it is greater then replace, if not then skip.

			if( enrollmentsFound.containsKey(childJSONObject.getInt("course_id"))                          ){
				String oldType = enrollmentsFound.get(childJSONObject.getInt("course_id"));
				String newType = childJSONObject.getString("type");
				M_log.debug("OLD TYPE: " + oldType);
				M_log.debug("NEW TYPE: " + newType);
				//Only want to overwrite the key,value pair if the value is greater
				if(enrollmentsMap.get(newType) > enrollmentsMap.get(oldType)){
					enrollmentsFound.put(childJSONObject.getInt("course_id"), childJSONObject.getString("type"));
				}
			}
			else{
				enrollmentsFound.put(childJSONObject.getInt("course_id"), childJSONObject.getString("type"));
			}

		}
		request.getSession().setAttribute("enrollments", enrollmentsFound);
		M_log.debug("SESSION ENROLLMENTS: " + request.getSession().getAttribute("enrollments"));
	}

	//Canvas adds custom parameters for lti launches. These custom paramerers 
	//include user_login and course_id. We use these parameters to unmask the
	//API calls.
	public String unmaskUrl(String url, TcSessionData tc,
			String stringToReplaceUser, String stringToReplaceCourse) {
		if(tc == null){
			return url;
		}

		String uniqname = (String) tc.getCustomValuesMap().get(CUSTOM_CANVAS_USER_LOGIN_ID);
		String courseId = (String) tc.getCustomValuesMap().get(CUSTOM_CANVAS_COURSE_ID);
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

		if( url.matches(appExtPropertiesFile.getProperty(CANVAS_API_CROSSLIST)) && session.getAttribute(LAUNCH_TYPE).equals("lti")){
			isAllowedRequest = isCrosslistAllowed(request, session, url);
		}

		if( url.matches(appExtPropertiesFile.getProperty(CANVAS_API_ADD_USER)) && request.getSession().getAttribute(LAUNCH_TYPE).equals("lti")){
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
		String sectionsApiCall = canvasURL + "/api/v1/sections/" + sectionString;
		M_log.debug("SECTIONS API CALL: " + sectionsApiCall);

		//string built, time to make call
		String sessionId = request.getSession().getId();
		String loggingApiWithSessionInfo = String.format("Canvas API request with Session Id \"%s\" for URL \"%s\"", sessionId,sectionsApiCall);
		M_log.info(loggingApiWithSessionInfo);
		HttpUriRequest clientRequest = null;

		clientRequest = new HttpGet(sectionsApiCall);

		BufferedReader rd = processApiCall(clientRequest);

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
			return false;
		}

		M_log.debug("ENROLLMENT FROM REQUEST: " + enrollmentTypeFromRequest);

		if(enrollmentsMap.containsKey(enrollmentTypeFromRequest) == false){
			return false;
		}

		isCallAllowed = compareRanks(enrollmentTypeFromRequest,
				enrollmentTypeFromSession);
		
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
		String crosslistApiCall = canvasURL + url.substring(0, url.indexOf("/crosslist"));
		M_log.debug("crosslist API call: " + crosslistApiCall);

		//String is built, time to make the call
		String sessionId = request.getSession().getId();
		String loggingApiWithSessionInfo = String.format("Canvas API request with Session Id \"%s\" for URL \"%s\"", sessionId,crosslistApiCall);
		M_log.info(loggingApiWithSessionInfo);
		HttpUriRequest clientRequest = null;

		clientRequest = new HttpGet(crosslistApiCall);

		BufferedReader rd = processApiCall(clientRequest);

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

	private BufferedReader processApiCall(HttpUriRequest clientRequest) {
		HttpClient client = new DefaultHttpClient();
		final ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();
		nameValues.add(new BasicNameValuePair("Authorization", "Bearer"+ " " +canvasToken));
		nameValues.add(new BasicNameValuePair("content-type", "application/json"));
		for (final NameValuePair h : nameValues)
		{
			clientRequest.addHeader(h.getName(), h.getValue());
		}
		BufferedReader rd = null;
		long startTime = System.currentTimeMillis();
		try {
			rd = new BufferedReader(new InputStreamReader(client.execute(clientRequest).getEntity().getContent()));
		} catch (IOException e) {
			M_log.error("Canvas API call did not complete successfully", e);
		}
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		M_log.info(String.format("CANVAS Api response took %sms",elapsedTime));
		return rd;
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
