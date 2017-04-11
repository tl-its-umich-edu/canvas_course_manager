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

import edu.umich.its.lti.TcSessionData;
import edu.umich.its.lti.utils.PropertiesUtilities;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

public class Utils {

	public static final String LTI_PARAM_CANVAS_COURSE_ID = "custom_canvas_course_id";
	public static final String LTI_PARAM_CANVAS_ENROLLMENT_STATE = "custom_canvas_enrollment_state";
	public static final String LTI_PARAM_UNIQNAME = "custom_canvas_user_login_id";
	public static final String LTI_PARAM_CONTACT_EMAIL_PRIMARY = "lis_person_contact_email_primary";
	public static final String LTI_PARAM_LAST_NAME = "lis_person_name_family";
	public static final String LTI_PARAM_FIRST_NAME = "lis_person_name_given";
	public static final String LTI_PARAM_CANVAS_USER_ID = "custom_canvas_user_id";
	public static final String SESSION_ROLES_FOR_ADDING_TEACHER = "session_roles_for_adding_teacher";
	public static final String DELETE = "DELETE";
	public static final String POST = "POST";
	public static final String GET = "GET";
	public static final String PUT = "PUT";
	public static final String CSV_HEADERS_SECTIONS = "section_id,name,status,course_id";
	public static final String CONSTANT_MIME_TEXT_CSV = "text/csv";
	protected static final String MAIL_SMTP_HOST = "mail.smtp.host";
	protected static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
	protected static final String MAIL_SMTP_STARTTLS = "mail.smtp.starttls.enable";
	protected static final String MAIL_DEBUG = "mail.debug";
	protected static final String MAIL_HOST = "umich.friend.mailhost";
	protected static final String FRIEND_CONTACT_EMAIL = "umich.friend.contactemail";
	protected static final String SIS_REPORT_CC_ADDRESS = "sis.report.cc.address";
	protected static final String SIS_POLLING_ATTEMPTS = "sis.polling.attempts";
	protected static final String SIS_POLLING_FREQUENCY = "sis.polling.frequency";
	protected static final String IS_MAIL_DEBUG_ENABLED = "mail.debug.enabled";
	protected static final String JSON_PARAM_WORKFLOW_STATE = "workflow_state";
	protected static final String JSON_PARAM_FAILED_WITH_MESSAGES = "failed_with_messages";
	protected static final String JSON_PARAM_IMPORTED_WITH_MESSAGES = "imported_with_messages";
	protected static final String JSON_PARAM_IMPORTED = "imported";
	private static Log M_log = LogFactory.getLog(Utils.class);

	private static final String CANVAS_API_GETALLSECTIONS_PER_COURSE = "canvas.api.getallsections.per.course.regex";
	private static final String CANVAS_API_TERMS = "canvas.api.terms.regex";
	private static final String CANVAS_API_GET_COURSE = "canvas.api.get.single.course.regex";
	private static final String CANVAS_API_SEARCH_USER = "canvas.api.search.user.regex";
	public static final String TC_SESSION_DATA = "tcSessionData";
	public static final String IS_ACCOUNT_ADMIN = "isAccountAdmin" ;
	public static final String CANVAS_API_VERSION = "/api/v1" ;
	public static String canvasURL = null;
	public static String canvasToken = null;
	public final static int API_UNKNOWN_ERROR = 666;
	public final static int API_EXCEPTION_ERROR = 667;

	public static Properties loadProperties(String path){
		String propertiesFilePath = System.getProperty(path);
		M_log.debug(path + " : " + propertiesFilePath);
		if (!propertiesFilePath.isEmpty()) {
			return PropertiesUtilities.getPropertiesObjectFromURL(propertiesFilePath);
		}
		M_log.error("File path for (" + path + ") is not provided");
		return null;		
	}

	public static void openFile(HttpServletRequest request,
			HttpServletResponse response, PrintWriter out) {
		M_log.debug("Stub method called");
		String pathInfo = request.getPathInfo();
		M_log.debug("Path Info: " + pathInfo);

		String url = request.getPathInfo();
		String queryString = request.getQueryString();

		if(queryString!=null) {
			url=pathInfo+"?"+queryString;
		}

		FileReader fr = null;

		//CANVAS_API_SEARCH_USER
		if (url.matches(SectionsUtilityToolServlet.appExtPropertiesFile.getProperty(CANVAS_API_SEARCH_USER))){
			fr = retrieveTestFile(url, fr, "Users call stub", "/stubs/canvas/users.txt");
		}
		
		else if (url.matches(SectionsUtilityToolServlet.appExtPropertiesFile.getProperty(CANVAS_API_TERMS))){
			fr = retrieveTestFile(url, fr, "Terms call stub", "/stubs/canvas/termsSample.txt");
		}

		else if(pathInfo.equalsIgnoreCase("/api/v1/courses/2222/sections")){
			fr = retrieveTestFile(url, fr, "Sections call for course 2222 stub", "/stubs/canvas/mySecondSections.txt");
		}

		else if(pathInfo.equalsIgnoreCase("/api/v1/courses/1111/sections")){
			fr = retrieveTestFile(url, fr, "Sections call for course 1111 stub", "/stubs/canvas/myFirstSections.txt");
		}

		else if(pathInfo.equalsIgnoreCase("/api/v1/courses")){
			fr = retrieveTestFile(url, fr, "Courses call stub", "/stubs/canvas/coursesSample.txt");
		}

		else if(url.matches(SectionsUtilityToolServlet.appExtPropertiesFile.getProperty(CANVAS_API_GET_COURSE))){
			fr = retrieveTestFile(url, fr, "Specific courses call stub", "/stubs/canvas/myCoursesSample.txt");
		}

		else if(url.matches(SectionsUtilityToolServlet.appExtPropertiesFile.getProperty(CANVAS_API_GETALLSECTIONS_PER_COURSE))){
			fr = retrieveTestFile(url, fr, "Sections call stub", "/stubs/canvas/sectionsSample.txt");
		}

		else if(pathInfo.equalsIgnoreCase(SectionsUtilityToolServlet.MPATHWAYS_PATH_INFO)){
			fr = retrieveTestFile(url, fr, "MPathways call stub", "/stubs/esb/mpathwaysSample.txt");
		}

		else{
			M_log.error("Unrecognized call: " + url);
		}

		try{
			M_log.info("fr: " + fr);
			BufferedReader rd = new BufferedReader(fr);
			String line = "";
			StringBuilder sb = new StringBuilder();
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			out.print(sb.toString());
			out.flush();
			M_log.debug("SUCCESS");
		}
		catch(Exception e){
			M_log.error("FAILURE");
			M_log.error("Exception in openFile call: ", e);
		}
	}

	public static FileReader retrieveTestFile(String url, FileReader fr, String msg, String path) {
		File testFile;
		try{
			M_log.info(msg);
			testFile = new File( Utils.class.getResource(path).toURI() );
			fr = new FileReader(testFile);  
		}
		catch(Exception e){
			M_log.error("Call Failed: " + url);
			M_log.error("Exception in retrieveTestFile call: ", e);
		}
		return fr;
	}

	public static void logApiCall(HttpServletRequest request) {
		String uniqname = null;
		String queryString = request.getQueryString();
		String pathInfo = request.getPathInfo();
		String url = pathInfo;
		if (queryString != null) {
			url = url + "?" + queryString;
		}

		TcSessionData tc = (TcSessionData) request.getSession().getAttribute(TC_SESSION_DATA);
		M_log.debug("TC Session Data: " + tc);

		String baseString = "FRIEND API request with Uniqname \"%s\" for URL \"%s\"";

		if (tc != null) {
			uniqname = (String) tc.getCustomValuesMap().get("custom_canvas_user_login_id");
		}
		logMsg(uniqname, url, request, baseString);
	}

	public static void logApiCall(String uniqname, String originalUrl, HttpServletRequest request) {
		String baseString = "CANVAS API request with Uniqname \"%s\" for URL \"%s\"";
		logMsg(uniqname, originalUrl, request, baseString);
	}


	private static void logMsg(String uniqname, String originalUrl, HttpServletRequest request, String baseString) {
		String remoteUser = request.getRemoteUser();
		String testUser = (String) request.getSession().getAttribute("testUser");
		String user;
		if (uniqname != null) {
			user = uniqname;
		} else if (remoteUser != null) {
			user = remoteUser;
		} else {
			user = testUser;
		}
		M_log.info(String.format(baseString, user, originalUrl));
	}

	public static ApiResultWrapper makeApiCall(HttpUriRequest clientRequest){
		HttpResponse response;
		response = Utils.executeApiCall(clientRequest);

		if (response == null) {
			String errMsg = String.format("{\"errorMsg\":\"The request %s failed with errors\"}",
					clientRequest.getURI().toString());
			return new ApiResultWrapper(Utils.API_UNKNOWN_ERROR, errMsg, "");
		}

		int statusCode = response.getStatusLine().getStatusCode();

		String apiResponse = "";
		String errMsg = "{\"errorMsg\":\"The request %s has failed to extract the response due to %s\"}";
		try {
			apiResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (IOException e) {
			return new ApiResultWrapper(Utils.API_EXCEPTION_ERROR,
					String.format(errMsg, clientRequest.getURI().toString(), e.getMessage()), "");
		} catch (Exception e) {
			return new ApiResultWrapper(Utils.API_EXCEPTION_ERROR,
					String.format(errMsg, clientRequest.getURI().toString(), e.getMessage()), "");
		}

		return new ApiResultWrapper(statusCode, "", apiResponse);
	}

	public static String urlConstructor(String... urlChunks) {
		String url = canvasURL + CANVAS_API_VERSION;
		for (String chunk : urlChunks) {
			url += chunk;
		}
		return url;
	}

	public static HttpResponse executeApiCall(HttpUriRequest clientRequest) {
		HttpClient client = HttpClients.createDefault();
		final ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();
		nameValues.add(new BasicNameValuePair("Authorization", "Bearer" + " " + canvasToken));
		nameValues.add(new BasicNameValuePair("content-type", "application/json"));
		for (final NameValuePair h : nameValues) {
			clientRequest.addHeader(h.getName(), h.getValue());
		}
		HttpResponse response = null;
		long startTime = System.currentTimeMillis();
		String errMsg = "Canvas API %s call did not complete successfully due to %s";
		try {
			response = client.execute(clientRequest);
		} catch (IOException e) {
			M_log.error(String.format(errMsg, clientRequest.getURI().toString(),e.getMessage()));
		} catch (Exception e) {
			M_log.error(String.format(errMsg, clientRequest.getURI().toString(),e.getMessage()));
		} finally {
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			M_log.info(String.format("CANVAS Api response took %sms", elapsedTime));
		}
		return response;
	}

	public static boolean didApiReturedWithOutErrors(String errMsg, ApiResultWrapper arw) {
        if (arw.getStatus() != HttpStatus.SC_OK) {
            M_log.error(errMsg);
			return false;
        }
		return true;
    }

	public static String getLTICustomParam(HttpServletRequest request, String ltiParam){
		HashMap<String, Object> customValuesMap = getLTICustomParams(request);
		for (String LtiParamKey : customValuesMap.keySet()) {
			if (LtiParamKey == ltiParam) {
				return (String) customValuesMap.get(ltiParam);
			}
		}
		return null;
	}

	public static String generateSisCourseId() {
        Random rand = new Random();
        int ranNum = rand.nextInt(40) + 1;
        long epoch = System.currentTimeMillis();
        String courseSisId="ccm"+epoch+"-"+ranNum;
        return courseSisId;
    }

	public static String getSisSectionIDChunk(HttpServletRequest request){
        String courseId = getLTICustomParam(request, LTI_PARAM_CANVAS_COURSE_ID);
        return "ccmS"+courseId+"-";
    }

	private static HashMap<String, Object> getLTICustomParams(HttpServletRequest request) {
		TcSessionData tc = (TcSessionData) request.getSession().getAttribute(Utils.TC_SESSION_DATA);
		return tc.getCustomValuesMap();
	}
}