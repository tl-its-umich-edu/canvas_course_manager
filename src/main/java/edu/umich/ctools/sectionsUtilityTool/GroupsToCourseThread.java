package edu.umich.ctools.sectionsUtilityTool;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONObject;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Created by pushyami on 4/26/17.
 */
public class GroupsToCourseThread implements Runnable {
	private static Log M_log = LogFactory.getLog(GroupsToCourseThread.class);

	private final String csvGrpContent;
	private final String courseId;
	private final String adminEmail;
	Properties appExtPropertiesFile = SectionsUtilityToolServlet.appExtPropertiesFile;


	GroupsToCourseThread(String csvContent,String emailAddress, String courseId){
		this.csvGrpContent = csvContent;
		this.adminEmail=emailAddress;
		this.courseId=courseId;
	}

	@Override
	public void run() {
		M_log.info("Starting the Thread for group process in course:  "+courseId);
		String[] grpList = csvGrpContent.split(Utils.CONSTANT_LINE_FEED);

		//this class is holding data for reporting purpose
		GroupProcessReport grpReport = new GroupProcessReport();

		String grpSetName = getGrpSetName(grpList[1]);
		grpReport.setGrpSetName(grpSetName);

		Multimap<String, String> groupsToUserMap = getGroupsToUserMap(grpList,grpReport);
		if(groupsToUserMap.isEmpty()){
			sendEmailReportForGroups(grpReport);
			return;
		}
		grpReport.setUsersToGroupActual(groupsToUserMap);

		//Create group set before groups and enrollments
		ApiResultWrapper arw = createGroupSet(grpSetName);
		int status = arw.getStatus();
		if (status / 100 != 2) {
			grpReport.errMessages.add(String.format("The Group set \"%s\" creation failed with status code %s. " +
					"Check if this Group set already exist in course ", grpSetName, status,courseId));
			M_log.error(String.format("Group set %s creation for course %s failed with status code %s due to %s "
					, grpSetName, courseId, status, (arw.getApiResp().isEmpty()) ? arw.getMessage() : arw.getApiResp()));
			sendEmailReportForGroups(grpReport);
			return;
		}
		String response = arw.getApiResp();
		JSONObject grpResponse = new JSONObject(response);
		int grpSetId = (int) grpResponse.get("id");
		String grpSetIdStr = String.valueOf(grpSetId);
		grpReport.setGrpSetId(grpSetIdStr);
		// start the creation of groups and adding enrollments to groups
		createGroupAndAddUsers(groupsToUserMap,grpReport);
		sendEmailReportForGroups(grpReport);
	}

	private void createGroupAndAddUsers(Multimap<String, String> groupsToUserMap,  GroupProcessReport grpReport) {
		String grpSetId = grpReport.getGrpSetId();
		for (String grpName : groupsToUserMap.keySet()) {
			ApiResultWrapper arw = createGroup(grpName, grpSetId);
			int status = arw.getStatus();
			if (status / 100 != 2) {
				grpReport.errMessages.add(String.format("Group \"%s\" creation failed in Group Set \"%s\" with status code %s"
						, grpName, grpReport.getGrpSetName(), status));
				M_log.error(String.format("Group %s creation in GroupSet %s for course %s failed with status code %s due to %s "
						, grpName, grpSetId, courseId, status, (arw.getApiResp().isEmpty()) ? arw.getMessage() : arw.getApiResp()));
				continue;
			}

			String response = arw.getApiResp();
			JSONObject grpResponse = new JSONObject(response);
			int grpId = (int) grpResponse.get("id");
			String grpIdStr = String.valueOf(grpId);
			Collection<String> usersList = groupsToUserMap.get(grpName);
			grpReport.grpNameToIdMap.put(grpIdStr, grpName);
			addUsersToGroup(usersList, grpReport, grpIdStr, grpName);
		}

	}

	private void addUsersToGroup(Collection<String> userList, GroupProcessReport grpReport, String grpId,String grpName) {
		for (String user : userList) {
//			https://umich.test.instructure.com:443/api/v1/groups/41630/memberships?user_id=sis_user_id:1234555
			String url = Utils.urlConstructor("/groups/", grpId, "/memberships?user_id=sis_user_id:",user);
			ApiResultWrapper arw = Utils.makeApiCall(new HttpPost(url));
			int status = arw.getStatus();
			if (status / 100 != 2) {
				M_log.error(String.format("Adding user %s to a group %s in group set %s in course %s failed with status code %s due to %s "
						, user, grpName, grpReport.getGrpSetName(), courseId, status, (arw.getApiResp().isEmpty()) ? arw.getMessage() : arw.getApiResp()));
				if (status == HttpStatus.SC_NOT_FOUND) {
					grpReport.errMessages.add(String.format("The enrollment \"%s\" is not added to the group \"%s\" " +
									"in group set \"%s\" due to Users must already exist in course to be added to groups "
							, user, grpName, grpReport.getGrpSetName()));
					continue;
				}
				grpReport.errMessages.add(String.format("The enrollment \"%s\" is not added to the group \"%s\" in group set \"%s\" failed with status code %s "
						, user, grpName, grpReport.getGrpSetName(), status));
				continue;
			}
			grpReport.usersToGroup.put(grpName, user);
		}
	}

	private String getBody(GroupProcessReport grpReport) {
		StringBuffer status = new StringBuffer();
		status.append("Group Set: " + grpReport.getGrpSetName());
		status.append(Utils.CONSTANT_LINE_FEED);
		Multimap<String, String> usersToGroupActual = grpReport.getUsersToGroupActual();
		if (grpReport.getGrpSetId() == null) {
			status.append("The Groups and Enrollment failed due to ");
			status.append(Utils.CONSTANT_LINE_FEED);
			status.append(getErrorMessageForGroupsFailure(grpReport));
			return status.toString();
		}
		if (!usersToGroupActual.equals(grpReport.usersToGroup)) {
			if (grpReport.usersToGroup.isEmpty()) {
				status.append("The Groups and Enrollment failed due to");
				status.append(Utils.CONSTANT_LINE_FEED);
				status.append(getErrorMessageForGroupsFailure(grpReport));
				return status.toString();
			}
			status.append("The Groups and Enrollment to groups has some errors");
			status.append(Utils.CONSTANT_LINE_FEED);
			status.append(getErrorMessageForGroupsFailure(grpReport));
			return status.toString();
		}
		status.append("Groups creations and adding Enrollments to groups are successful!");
		return status.toString();
	}

	private String getErrorMessageForGroupsFailure(GroupProcessReport grpReport) {
		StringBuffer errMsgs = new StringBuffer();
		List<String> errMessages = grpReport.errMessages;
		errMessages.forEach(msg -> errMsgs.append(msg+Utils.CONSTANT_LINE_FEED));
		return errMsgs.toString();
	}


	private void sendEmailReportForGroups(GroupProcessReport grpReport){
		M_log.debug("sendEmailReportForGroups(): Called");
		String errmsg = "Problem in sending the email when %s in course %s due to %s";
		String fromAddress = appExtPropertiesFile.getProperty(Utils.FRIEND_CONTACT_EMAIL);
		String ccmSupportAddress = appExtPropertiesFile.getProperty(Utils.SIS_REPORT_CCM_SUPPORT_ADDRESS);
		Properties properties = Utils.getMailProperties();
		Session session = Session.getInstance(properties);
		MimeMessage message = new MimeMessage(session);
		try {
			message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccmSupportAddress));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(adminEmail));
			message.setFrom(new InternetAddress(fromAddress));
			message.setSubject(String.format(CourseUploadType.ADD_GROUPS_AND_USERS.getDescription() + " " + courseId));
			Multipart multipart = new MimeMultipart();
			BodyPart body = new MimeBodyPart();
			String msgBody = getBody(grpReport);
			M_log.debug(msgBody);
			body.setText(msgBody);
			multipart.addBodyPart(body);
			message.setContent(multipart);
			M_log.info(String.format("Sending Email for the Groups set %s in course %s ", grpReport.getGrpSetName(), courseId));
			Transport.send(message);
		} catch (MessagingException e) {
			M_log.error(String.format(errmsg, CourseUploadType.ADD_GROUPS_AND_USERS,courseId, e.getMessage()));
		} catch (Exception e) {
			M_log.error(String.format(errmsg, CourseUploadType.ADD_GROUPS_AND_USERS,courseId, e.getMessage()));
		}
	}

	private ApiResultWrapper createGroupSet(String grpSetName) {
		String url;
//		https://umich.test.instructure.com:443/api/v1/courses/106629/group_categories?name=pushyami_test
		try {
			url = Utils.urlConstructor(Utils.URL_CHUNK_COURSES, courseId, Utils.URL_CHUNK_GRP_CATEGORIES,
					"?name=" + URLEncoder.encode(grpSetName, Utils.CONSTANT_UTF_8));
		} catch (UnsupportedEncodingException e) {
			String errMsg = String.format("{\"errors\":\"Groups set creation failed for course due to %s\"}",courseId,e.getMessage());
			return new ApiResultWrapper(Utils.API_EXCEPTION_ERROR, errMsg, "");
		}
		ApiResultWrapper arw = Utils.makeApiCall(new HttpPost(url));
		return arw;
	}

	private ApiResultWrapper createGroup(String grpName,String grpSetId) {
//		https://umich.test.instructure.com:443/api/v1/group_categories/8017/groups
		String url;
		try {
			url = Utils.urlConstructor(Utils.URL_CHUNK_GRP_CATEGORIES, grpSetId, "/groups?course_id=",courseId,"&name=" ,
					URLEncoder.encode(grpName, Utils.CONSTANT_UTF_8));

		} catch (UnsupportedEncodingException e) {
			String errMsg = String.format("{\"errors\":\"Groups creation for %s failed due to %s\"}", grpName,e.getMessage());
			return new ApiResultWrapper(Utils.API_EXCEPTION_ERROR, errMsg, "");
		}
		ApiResultWrapper arw = Utils.makeApiCall(new HttpPost(url));
		return arw;
	}

    // getting the group content from the attachment
	public Multimap<String, String> getGroupsToUserMap(String[] grpList,GroupProcessReport report) {
		StringBuffer groupsWithUsers = new StringBuffer();
		Multimap<String, String> groupToUserMap = HashMultimap.create();
		try {
			for (int i = 1; i < grpList.length; i++) {
				String[] grpItems = grpList[i].split(",");
				groupsWithUsers.append(grpItems[1] + "," + grpItems[2]);
				groupsWithUsers.append(Utils.CONSTANT_LINE_FEED);
			}
			String s = groupsWithUsers.toString();
			String[] grpToUserConcat = s.split("\n");
			for (String grp : grpToUserConcat) {
				String[] split = grp.split(",");

				groupToUserMap.put(split[0], split[1]);
			}
		} catch (RuntimeException e) {
			// This case may not likely to happen as UI is validating thoroughly won't post unless the requirements met
			String msg = "Groups Process failed, the input groups data may be Bad ";
			report.errMessages.add(msg);
			M_log.error(msg+e.getMessage());
		}
		return groupToUserMap;
	}

	// 1st element is actually the data and 0th element is the header name.
	public String getGrpSetName(String groupsFirstRow) {
		String[] groupsFirstRowList = groupsFirstRow.split(",");
		String grpSetName = groupsFirstRowList[0];
		return grpSetName;
	}

}
