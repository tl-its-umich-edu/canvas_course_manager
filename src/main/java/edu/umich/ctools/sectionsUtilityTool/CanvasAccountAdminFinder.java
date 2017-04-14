package edu.umich.ctools.sectionsUtilityTool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by pushyami on 4/15/17.
 */
public class CanvasAccountAdminFinder {

	private static Log M_log = LogFactory.getLog(SectionsUtilityToolServlet.class);

	protected boolean isAccountAdmin(HttpServletRequest request) {
		M_log.debug("isAdmin() call");
		String courseId = request.getParameter(Utils.LTI_PARAM_CANVAS_COURSE_ID);
		int userId = Integer.parseInt(request.getParameter(Utils.LTI_PARAM_CANVAS_USER_ID));

		int courseAccountId = getCourseAccountId(courseId);
		if (courseAccountId == 0) {
			return false;
		}
		M_log.debug("*** Course Account ID " + courseAccountId);
		return determineIfUserIsAdminInHierarchy(userId, courseAccountId);
	}

	private boolean determineIfUserIsAdminInHierarchy(int userCanvasId, int courseAccountId) {
		M_log.debug("determineIfUserIsAdminInHierarchy() call");
		String url = Utils.urlConstructor("/accounts/", String.valueOf(courseAccountId),
				"/admins/?user_id[]=", String.valueOf(userCanvasId));
		ApiResultWrapper arw = Utils.makeApiCall(new HttpGet(url));
		String errMsg = String.format("check user %s is an admin in the course account %s had null response, with status %s, due to:%s"
				, userCanvasId, courseAccountId, arw.getStatus(), (arw.getApiResp().isEmpty()) ? arw.getMessage() : arw.getApiResp());
		if (!Utils.didApiReturnedWithOutErrors(errMsg, arw)) {
			return false;
		}

		JSONArray subAccountAdminsJson = new JSONArray(arw.getApiResp());
		if (subAccountAdminsJson.length() > 0) {
			//user is a some kind of admin in the course
			return true;
		}
		//find out if the user is a subaccount admin one level up in the hierarchy
		int parentAccount = getParentAccount(courseAccountId);
		// 0 means cannot get the desired response or trying to get parent of root account which is not applicable
		if (parentAccount == 0) {
			return false;
		}
		return determineIfUserIsAdminInHierarchy(userCanvasId, parentAccount);
	}

	public int getParentAccount(int accountId) {
		M_log.debug("getParentAccount() call");
		// https://umich.test.instructure.com:443/api/v1/accounts/78
		String url = Utils.urlConstructor("/accounts/", String.valueOf(accountId));
		ApiResultWrapper arw = Utils.makeApiCall(new HttpGet(url));
		int parentAccountId = 0;
		String errMsg = String.format("getting parent for account %s for SubAccountAdmin check failed with status code " +
				"%s, due to: %s ", accountId, arw.getStatus(), (arw.getApiResp().isEmpty()) ? arw.getMessage() : arw.getApiResp());
		if (!Utils.didApiReturnedWithOutErrors(errMsg, arw)) {
			return 0;
		}
		JSONObject parentAccountJson = new JSONObject(arw.getApiResp());
		if (!parentAccountJson.isNull("parent_account_id")) {
			parentAccountId = (int) parentAccountJson.get("parent_account_id");
		}

		return parentAccountId;

	}

	private int getCourseAccountId(String courseId) {
		M_log.debug("getCourseAccountId() call");
		int course_account_id = 0;
		String url = Utils.urlConstructor("/courses/", courseId);
		ApiResultWrapper arw = Utils.makeApiCall(new HttpGet(url));
		String errMsg = String.format("As part of SubAccountAdmin check, couldn't get accountId the course %s" +
						" belong to, with status code %s, due to: %s",
				courseId, arw.getStatus(), (arw.getApiResp().isEmpty()) ? arw.getMessage() : arw.getApiResp());
		if (!Utils.didApiReturnedWithOutErrors(errMsg, arw)) {
			return 0;
		}
		JSONObject courseJson = new JSONObject(arw.getApiResp());
		if (!courseJson.isNull("account_id")) {
			course_account_id = (int) courseJson.get("account_id");
		}
		return course_account_id;
	}


}
