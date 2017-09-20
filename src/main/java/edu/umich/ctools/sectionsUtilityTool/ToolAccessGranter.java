package edu.umich.ctools.sectionsUtilityTool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;


public class ToolAccessGranter {
	private static Log m_log = LogFactory.getLog(ToolAccessGranter.class);
	private final String courseId;
	private final String canvasUserId;

	public ToolAccessGranter(String courseId, String canvasUserId){
		this.courseId=courseId;
		this.canvasUserId=canvasUserId;
	}


	public boolean isAllowedAccess(boolean isAdmin) {

		// access is given to the admin's in course account hierarchy and super admins.
		if(isAdmin){
			return true;
		}
		return checkIfUserHasPermissionToManageStudentsInCourse();


	}
	/*
	 This call is checking if the user role in the course like instructor, student, TA or any other has manage student access
	 if he does then the user is granted access to the tool.
	 */

	private boolean checkIfUserHasPermissionToManageStudentsInCourse() {
		boolean canManageStudents = false;
		String url = Utils.urlConstructor(Utils.URL_CHUNK_COURSES, courseId, "/permissions?as_user_id=", canvasUserId);
		ApiResultWrapper arw = Utils.makeApiCall(new HttpGet(url));
		String errMsgString = "The Permission check to grant access to the tool failed with status code %s due to %s in the course %s for the user %s";
		String apiResp = arw.getApiResp();
		String errMsg = String.format(errMsgString, arw.getStatus(), (apiResp.isEmpty()) ? arw.getMessage() : apiResp, courseId, canvasUserId);
		if (!Utils.didApiReturnedWithOutErrors(errMsg, arw)) {
			return canManageStudents;
		}
		JSONObject permissionJson = new JSONObject(apiResp);
		if (!permissionJson.isNull("manage_students")) {
			canManageStudents = (boolean) permissionJson.get("manage_students");
		}

		return canManageStudents;
	}
}
