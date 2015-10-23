package edu.umich.ctools.sectionsUtilityTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.json.Json;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.json.JSONObject;

import com.mashape.unirest.http.exceptions.UnirestException;

import edu.umich.ctools.esb.utils.WAPI;
import edu.umich.ctools.esb.utils.WAPIResultWrapper;
import edu.umich.its.lti.utils.RequestSignatureUtils;

public class SectionsUtilityToolServlet extends VelocityViewServlet {

	private static Log M_log = LogFactory.getLog(SectionsUtilityToolServlet.class);
	private static final long serialVersionUID = 7284813350014385613L;

	private static final String CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS = "canvas.api.getcourse.by.uniqname.no.sections.regex";
	private static final String CANVAS_API_GETALLSECTIONS_PER_COURSE = "canvas.api.getallsections.per.course.regex";
	private static final String CANVAS_API_GETSECTION_PER_COURSE = "canvas.api.getsection.per.course.regex";
	private static final String CANVAS_API_GETSECTION_INFO = "canvas.api.getsection.info.regex";
	private static final String CANVAS_API_DECROSSLIST = "canvas.api.decrosslist.regex";
	private static final String CANVAS_API_CROSSLIST = "canvas.api.crosslist.regex";
	private static final String CANVAS_API_GETCOURSE_INFO = "canvas.api.getcourse.info.regex";
	private static final String CANVAS_API_RENAME_COURSE = "canvas.api.rename.course.regex";
	private static final String CANVAS_API_GETCOURSE_BY_UNIQNAME = "canvas.api.getcourse.by.uniqname.regex";
	private static final String CANVAS_API_ENROLLMENT = "canvas.api.enrollment.regex";
	private static final String CANVAS_API_TERMS = "canvas.api.terms.regex";
	private static final String CANVAS_API_SEARCH_COURSES = "canvas.api.search.courses.regex";
	private static final String CANVAS_API_SEARCH_USER = "canvas.api.search.user.regex";
	private static final String CANVAS_API_CREATE_USER = "canvas.api.create.user.regex";
	private static final String CANVAS_API_ADD_USER = "canvas.api.add.user.regex";
	private static final String CANVAS_API_GET_COURSE = "canvas.api.get.single.course.regex";
	private static final String MPATHWAYS_API_GNERIC = "mpathways.api.get.generic";

	private static final String PARAMETER_INSTRUCTOR = "instructor";
	private static final String PARAMETER_TERMID = "termid";

	private static final String MANAGER_SERVLET_NAME = "/manager";
	private static final String MPATHWAYS_PATH_INFO = "/mpathways/Instructors";

	private static final String DELETE = "DELETE";
	private static final String POST = "POST";
	private static final String GET = "GET";
	private static final String PUT = "PUT";

	private String canvasToken;
	private String canvasURL;
	private String callType;
	private String ltiUrl;
	private String ltiKey;
	private String ltiSecret;

	private final static String CCM_PROPERTY_FILE_PATH = "ccmPropsPath";
	private final static String CCM_SECURE_PROPERTY_FILE_PATH = "ccmPropsPathSecure";	

	protected static Properties appExtSecurePropertiesFile=null;
	protected static Properties appExtPropertiesFile=null;	

	private static final HashMap<String,String> apiListRegexWithDebugMsg = new HashMap<String,String>(){
		private static final long serialVersionUID = -1389517682290891890L;
		{			
			put(CANVAS_API_TERMS, "for terms");
			put(CANVAS_API_CROSSLIST, "for crosslist");
			put(CANVAS_API_RENAME_COURSE, "for rename a course");
			put(CANVAS_API_GETCOURSE_BY_UNIQNAME, "for getting courses by uniqname");
			put(CANVAS_API_GETCOURSE_BY_UNIQNAME_NO_SECTIONS, "for getting courses by uniqname not including sections");
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
			put(MPATHWAYS_API_GNERIC, "for mpathways calls");
		}
	};

	public void init() throws ServletException {
		M_log.debug(" Servlet init(): Called");
		appExtPropertiesFile = Utils.loadProperties(CCM_PROPERTY_FILE_PATH);
		appExtSecurePropertiesFile = Utils.loadProperties(CCM_SECURE_PROPERTY_FILE_PATH);		
	}

	public void fillContext(Context context, HttpServletRequest request) {
		M_log.debug("fillContext() called");		
		ViewToolContext vtc = (ViewToolContext)context;
		//test code
		//vtc.put("variable", "happiness");
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

	public void doGet(HttpServletRequest request,HttpServletResponse response){
		M_log.debug("doGet: Called");
		try {
			if(request.getServletPath().equals(MANAGER_SERVLET_NAME)){
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
			for (String value : entry.getValue()) {
				M_log.debug(name + " = " + value);
			}
		}
		//Properties appExtSecureProperties = SectionUtilityToolFilter.appExtSecurePropertiesFile;
		if(appExtSecurePropertiesFile!=null) {
			ltiKey = appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.PROPERTY_LTI_KEY);
			ltiSecret = appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.PROPERTY_LTI_SECRET);
			ltiUrl = appExtPropertiesFile.getProperty(SectionUtilityToolFilter.PROPERTY_LTI_URL);
			M_log.debug("ltiKey: " + ltiKey);
			M_log.debug("ltiSecret: " + ltiSecret);
			M_log.debug("ltiUrl: " + ltiUrl);
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
		doError(request, response, "Missing required parameter:  Launch type or version is incorrect.");
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
	 */


	private void apiConnectionLogic(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		PrintWriter out = response.getWriter();
		if(request.getPathInfo().equalsIgnoreCase(MPATHWAYS_PATH_INFO)){
			realMpathwaysCall(request, response, out);
			//testMpathwaysCall(out);
		}
		else{
			getCanvasResponse(request, response, out);
		}
	}

	private void realMpathwaysCall(HttpServletRequest request, HttpServletResponse response, PrintWriter out){
		WAPIResultWrapper wrappedResult = null;
		String mpathwaysInstructor = request.getParameter(PARAMETER_INSTRUCTOR);
		String mpathwaysTermId = request.getParameter(PARAMETER_TERMID);
		if(mpathwaysInstructor == null || 
				mpathwaysTermId == null){
			response.setStatus(400);
			wrappedResult = new WAPIResultWrapper(400, "Parameter missing in Instructors request", new JSONObject());
		}
		else{
			if(appExtSecurePropertiesFile!=null) {
				HashMap<String, String> value = new HashMap<String, String>();
				value.put("tokenServer", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_TOKEN_SERVER));
				value.put("apiPrefix", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_PREFIX));
				value.put("key", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_KEY));
				value.put("secret", appExtSecurePropertiesFile.getProperty(SectionUtilityToolFilter.ESB_SECRET));
				WAPI wapi = new WAPI(value);
				try {
					wrappedResult = wapi.getRequest(wapi.getApiPrefix() + mpathwaysInstructor + "/Terms/" + mpathwaysTermId + "/Classes");
				} catch (UnirestException e) {
					e.printStackTrace();
				}	
			}
		}
		out.print(wrappedResult.toJson());
		out.flush();
	}

	private void getCanvasResponse(HttpServletRequest request,
			HttpServletResponse response, PrintWriter out) throws IOException {
		String queryString = request.getQueryString();
		String pathInfo = request.getPathInfo();
		String url;
		if(queryString!=null) {
			url= canvasURL+pathInfo+"?"+queryString;
		}else {
			url=canvasURL+pathInfo;
		}
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
		String line = "";
		StringBuilder sb = new StringBuilder();
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		out.print(sb.toString());
		out.flush();
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
		return isAllowedRequest;
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
			if(url.matches(appExtPropertiesFile.getProperty(api))) {
				M_log.debug(prefixDebugMsg+apiListRegexWithDebugMsg.get(api));
				isMatch= true;
				break;
			}

		}
		return isMatch;
	}

}