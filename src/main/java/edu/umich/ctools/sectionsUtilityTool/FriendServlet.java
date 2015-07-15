//do ctrl+f to find all "dovek" instances before committing.
package edu.umich.ctools.sectionsUtilityTool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.json.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FriendServlet extends HttpServlet {

	private static Log M_log = LogFactory.getLog(FriendServlet.class);
	private static final long serialVersionUID = 7284813350014385613L;

	private static final String FRIEND_CHECK_EXISTS = "some.regex";
	private static final String FRIEND_CREATE = "some.regex";
	private static final String FRIEND_SEND_EMAIL = "some.regex";
	private static final String DELETE = "DELETE";
	private static final String POST = "POST";
	private static final String GET = "GET";
	private static final String PUT = "PUT";
	ResourceBundle props = ResourceBundle.getBundle("sectiontool");

	public void init() throws ServletException {
		M_log.debug(" Servlet init(): Called");
	}

	protected void doGet(HttpServletRequest request,HttpServletResponse response){
		M_log.debug("doGet: Called");
		try {
			friendRestApiCall(request, response);
		}catch(Exception e) {
			M_log.error("GET request has some exceptions",e);
			e.printStackTrace();
		}
	}

	//I think all requests are going to be posts - ignore otherss?
	protected void doPost(HttpServletRequest request,HttpServletResponse response){
		M_log.debug("doPOST: Called");
		try {
			friendRestApiCall(request, response);
		}catch(Exception e) {
			M_log.error("POST request has some exceptions",e);
		}

	}
	protected void doPut(HttpServletRequest request,HttpServletResponse response){
		M_log.debug("doPut: Called");
		try {
			friendRestApiCall(request, response);
		}catch(Exception e) {
			M_log.error("PUT request has some exceptions",e);
		}
	}
	protected void doDelete(HttpServletRequest request,HttpServletResponse response) {
		M_log.debug("doDelete: Called");
		try {
			friendRestApiCall(request, response);
		}catch(Exception e) {
			M_log.error("DELETE request has some exceptions",e);
		}
	}

	/*
	 * This method is handling all the different Api request like PUT, POST etc to canvas.
	 * We are using canvas admin token stored in the properties file to handle the request. 
	 */
	private void friendRestApiCall(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		request.setCharacterEncoding("UTF-8");
		M_log.debug("friendRestApiCall(): called");
		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		Properties appExtSecureProperties = SectionUtilityToolFilter.appExtSecurePropertiesFile;
		if(appExtSecureProperties==null) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out = response.getWriter();
			out.print(props.getString("property.file.load.error"));
			out.flush();
			M_log.error("Failed to load system properties(sectionsToolProps.properties) for SectionsTool");
			return;
		}
		friendApiConnectionLogic(request,response);
	}	
	
	/*
	 * This function has logic that execute client(i.e., browser) request and get results from the canvas  
	 * using Apache Http client library
	 */
	private void friendApiConnectionLogic(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		String pathInfo = request.getPathInfo();
		PrintWriter out = response.getWriter();
		JsonObject json;
		Friend myFriend = new Friend();
		switch (pathInfo){
		case "/friendCreate":
			String friendInviteEmail = request.getParameter("id");
			String friendInstructorEmail = request.getParameter("inst_email");
			String friendInstructorFirstName = request.getParameter("inst_first_name");
			String friendInstructorLastName = request.getParameter("inst_last_name");
			json = doFriendCreate(myFriend, 
					friendInviteEmail, 
					friendInstructorEmail, 
					friendInstructorFirstName, 
					friendInstructorLastName);
			break;
		default:
			json = Json.createObjectBuilder()
					.add("message", "request error")
					.build();
			break;
		}
		out.print(json.toString());
		out.flush();
	}
	
	private JsonObject doFriendCreate(Friend myFriend, 
			String inviteEmail, 
			String instructorEmail, 
			String instructorFirstName, 
			String instructorLastName){
		JsonObject json;
		String instructorName = instructorFirstName + " " + instructorLastName;
		String detailedMessage;
		myFriend.init();
		//Step 1: determine if friend exists
		int friendExists = Friend.checkAccountExist(inviteEmail);
		switch(friendExists){
		case -1:
			//If friend email is invalid, send message for invalid email in json response.
			M_log.warn(" friendCreate: invalid email address " + inviteEmail);
			detailedMessage = "Account " + inviteEmail + " is not a valid email address";
			json = Json.createObjectBuilder()
					.add("message", "false")
					.add("detailedMessage", detailedMessage)
					.build();
			break;
		case 0:
			//If friend has valid email but does not have a friend account, attempt to create one.
			M_log.info(" friendCreate: " + inviteEmail + " does not have an account. One will attempt to be created.");
			int friendCreate = Friend.doSendInvite(inviteEmail, instructorEmail, instructorName);
			//Step 2: attempt to create friend account
			switch(friendCreate){
			case 1:
				//If friend account is successful, send json response back saying so
				M_log.info(" friendCreate: successfully created account for  " + inviteEmail);
				detailedMessage = "Friend Account created for " + inviteEmail;
				json = Json.createObjectBuilder()
						.add("message", "true")
						.add("detailedMessage", detailedMessage)
						.build();
				
				//Step 3: send notification to instructor indicating that a friend account has been created.
				Friend.notifyCurrentUser(instructorName, instructorEmail, inviteEmail);
				break;
			case -1:
				//If attempt to create friend account fails, then send so in response
				M_log.warn(" friendCreate: invalid email address " + inviteEmail);
				detailedMessage = "Friend Account NOT created for " + inviteEmail;
				json = Json.createObjectBuilder()
						.add("message", "false")
						.add("detailedMessage", detailedMessage)
						.build();
				break;
			default:
				//Default is that nothing be done and send back json response.
				M_log.warn(" friendCreate: invalid email address " + inviteEmail);
				detailedMessage = "Friend Account NOT created for " + inviteEmail;
				json = Json.createObjectBuilder()
						.add("message", "false")
						.add("detailedMessage", detailedMessage)
						.build();
				break;
			}
			break;	
		case 1:
			//If friend already has friend account, send so back in json response
			detailedMessage = "Account " + inviteEmail + " already exist";
			json = Json.createObjectBuilder()
					.add("message", "false")
					.add("detailedMessage", detailedMessage)
					.build();
			break;
		default:
			//Default is that nothing be done and send back json response.
			M_log.warn(" friendCreate: invalid email address " + inviteEmail);
			detailedMessage = "Friend Account NOT created for " + inviteEmail;
			json = Json.createObjectBuilder()
					.add("message", "false")
					.add("detailedMessage", detailedMessage)
					.build();
			break;
		}
		myFriend.destroy();
		return json;
	}

}