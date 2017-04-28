package edu.umich.ctools.sectionsUtilityTool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Created by pushyami on 4/7/17.
 */
public class SISPollingThread implements Runnable {
	private static Log M_log = LogFactory.getLog(SISPollingThread.class);

	volatile boolean flag = true;
	Properties appExtPropertiesFile = SectionsUtilityToolServlet.appExtPropertiesFile;
	private int pollingAttempts = getPollingAttempt();
	private int sleepTimeForPolling = getSleepTimeForPolling();


	/* Here we grab all the canvasId for polling to send a report to the admins of the sis process once uploaded to canvas.
	canvas is pretty quick in getting the sis_upload done, but some reason it's processing is slow, then after X attempts we would
	take it out of the polling list. All this tracking is for reporting so if we take the item out of the list that doesn't mean
	sis upload is interrupted by CCM tool.
	*/
	@Override
	public void run() {
		while (flag) {
			M_log.debug("***************************** Starting the thread check loop");
			synchronized (this) {
				try {
				    List<SISDataHolderForEmail> pollingIds = SectionsUtilityToolServlet.canvasPollingIds;
					int addedPollingIdCount = SectionsUtilityToolServlet.addedPollingIdCount;
					int removedPollingIdCount = SectionsUtilityToolServlet.removedPollingIdCount;

					M_log.debug("+++ Polling Id added " + addedPollingIdCount);
					M_log.debug("--- Polling Id removed " + removedPollingIdCount);

					//this check is made for keeping track of Threading issues
					didThreadingIssuesOccur(pollingIds, addedPollingIdCount, removedPollingIdCount);
					Iterator<SISDataHolderForEmail> iterator = pollingIds.iterator();
					printListOfPollsToDebugLog(pollingIds);
					while (iterator.hasNext()) {
						SISDataHolderForEmail emailData = iterator.next();
						M_log.debug(String.format("Number of attempts by for job %s are %s "
								, emailData.getPollingId(), emailData.getNumberOfTries()));

						// Well the check is to ensure polling to canvas don't take forever due to slow processing on canvas end
						//after certain attempt decided logistically we take the polling Id out of the list and report this to user.
						if (emailData.getNumberOfTries() >= pollingAttempts) {
							emailData.setSISUploadVerySlow(true);
							sendEmailReport(emailData, null);
							iterator.remove();
							M_log.warn(String.format("The SIS Request %s for request type %s for course %s took longer than expected, REMOVING the request from THREAD",
									emailData.getPollingId(), emailData.getSisProcessType(), emailData.getCourseId()));
							removedPollingIdCount();
							continue;
						}
						ApiResultWrapper arw = apiCallCheckingSISJobStatus(emailData);
						int status = arw.getStatus();
						//if polling to canvas failed we keep track of this for X attempts and after that
						// if still response is unsuccessful we take it out of the list
						if (status != HttpStatus.SC_OK) {
							M_log.warn(String.format("SIS polling call failed with status code %s due to %s but will try in next poll"
									, status, arw.getMessage()));
							emailData.incrementNumberOfTries(1);
							continue;
						}

						String apiResp = arw.getApiResp();
						// sis process is done(for success/failure/partial_success cases) only when we see progress =100,
						// Unit test written testing this.
						if (!isSISUploadDone(apiResp)) {
							emailData.incrementNumberOfTries(1);
							M_log.info(String.format("The SIS request %s of request type %s for course %s ,No# of (polling)attempts made for status %s "
									, emailData.getPollingId(), emailData.getSisProcessType(), emailData.getCourseId(), emailData.getNumberOfTries()));
							continue;
						}
						// sending email report to user saying what canvas did with the sisUpload request they submitted
						sendEmailReport(emailData, apiResp);
						M_log.info("Removing the Polling Id from the list" + emailData.getPollingId());
						iterator.remove();
						removedPollingIdCount();
					}
					printListOfPollsToDebugLog(pollingIds);
				} catch (RuntimeException e) {
					M_log.error("Some thing unexpected happened in the SISPollingThread due to "+e.getMessage());
					sendEmailReportingTheException(e);
				} catch (Exception e) {
					M_log.error("Some thing unexpected happened in the SISPollingThread due to "+e.getMessage());
					sendEmailReportingTheException(e);
				}
				M_log.debug("***************************** Finish the thread check loop");
				try {
					Thread.sleep(sleepTimeForPolling);
				} catch (InterruptedException e) {
					M_log.error("Canvas polling thread got Interrupted due to " + e.getMessage());
				} catch (Exception e) {
					M_log.error("Canvas polling thread got Interrupted due to " + e.getMessage());
				}

			}
		}

	}

	private void didThreadingIssuesOccur(List<SISDataHolderForEmail> pollingIds, int addedPollingIdCount, int removedPollingIdCount) {
		if ((addedPollingIdCount - removedPollingIdCount) == pollingIds.size()) {
			M_log.info("polling went fine");
			return;
		}
		M_log.info("canvas polling has some problems as number of things added/removed count" + (addedPollingIdCount - removedPollingIdCount) +
				"not equal to ids in the canvas polling id List: " + pollingIds.size());
	}

	// if calling from other than synchronized block this method needs to be synchronized keyword
	private void printListOfPollsToDebugLog(List<SISDataHolderForEmail> pollingId) {
		for (SISDataHolderForEmail data: pollingId){
		M_log.debug("PollingId " +data.getPollingId());
		}
	}

	// if calling from other than synchronized block this method needs to be synchronized keyword
	private void removedPollingIdCount(){
		SectionsUtilityToolServlet.removedPollingIdCount += 1;
		M_log.info("*#$*#$*#$ Number of Polling Ids removed are " + SectionsUtilityToolServlet.removedPollingIdCount);
	}

	public static boolean isSISUploadDone(String apiResp){
		M_log.debug("isSISUploadDone() call");
		JSONObject resp = new JSONObject(apiResp);
		int progress = (int) resp.get("progress");
		if (progress == 100) {
			return true;
		}
		return false;

	}

	private ApiResultWrapper apiCallCheckingSISJobStatus(SISDataHolderForEmail data) {
		String url = Utils.urlConstructor(Utils.URL_CHUNK_ACCOUNTS_1_SIS_IMPORTS, String.valueOf(data.getPollingId()));
		ApiResultWrapper arw = Utils.makeApiCall(new HttpGet(url));
		return arw;
	}

	public void sendEmailReport(SISDataHolderForEmail data, String apiResp) {
		M_log.debug("sendEmailReport(): Called");
		String fromAddress = appExtPropertiesFile.getProperty(Utils.FRIEND_CONTACT_EMAIL);
		String ccmSupportAddress = appExtPropertiesFile.getProperty(Utils.SIS_REPORT_CCM_SUPPORT_ADDRESS);

		Properties properties = getMailProperties();

		Session session = Session.getInstance(properties);
		MimeMessage message = new MimeMessage(session);
		String errmsg = "Problem in sending the email for course %s due to %s";
		try {
			message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccmSupportAddress));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(data.getEmailAddress()));
			message.setFrom(new InternetAddress(fromAddress));
			message.setSubject(String.format(data.getSisProcessType().getDescription() + " " + data.getCourseId()));

			//if canvas taking long time to process the sis request, after X attempts intentionally we take the request
			// out of the thread pool that's when apiResp is null and we send a email to user about this situation.
			if ((apiResp == null) && data.isSISUploadVerySlow()) {
				Multipart multipart = new MimeMultipart();
				BodyPart bodyParts = new MimeBodyPart();
				//BSA might come up with better wording, so keeping it simple for now
				bodyParts.setText((data.getSisProcessType().getDescription() + " " +
						"canvas is taking too long to process this request"));
				message.setContent(multipart);
				Transport.send(message);
				return;
			}

			JSONObject resp = new JSONObject(apiResp);
			String workflowState = (String) resp.get(Utils.JSON_PARAM_WORKFLOW_STATE);

			Multipart multipart = new MimeMultipart();
			String msgBody = getBody(data.getSisProcessType(), resp);
			//msgBody of the email
			BodyPart body = new MimeBodyPart();
			body.setText(msgBody);
			multipart.addBodyPart(body);
			//Attachment of sisData
			if (workflowState.equals(Utils.JSON_PARAM_IMPORTED)) {
				multipart.addBodyPart(getAttachment(data));
			}

			message.setContent(multipart);
			M_log.info(String.format("SIS request %s of type %s for course %s Finished! Sending email report  " ,
					data.getPollingId(),data.getSisProcessType(),data.getCourseId()));
			Transport.send(message);
		} catch (MessagingException | JSONException e) {
			M_log.error(String.format(errmsg, data.getCourseId(), e.getMessage()));
		} catch (Exception e) {
			M_log.error(String.format(errmsg, data.getCourseId(), e.getMessage()));
		}

	}
    /*
	 We are sending an email to the ccm admin support group in case of exceptions that we wouldn't anticipate to see
     like out of memory error, threading errors etc. This way atleast we know if an error need an attention
     rather get buried in logs
     */
	private void sendEmailReportingTheException(Exception exp) {
		StringBuffer bodyMsg = new StringBuffer();
		bodyMsg.append("Background Polling has some Exceptions due to "+exp.getMessage());
		bodyMsg.append(Utils.CONSTANT_LINE_FEED);
		StringWriter errors = new StringWriter();
		exp.printStackTrace(new PrintWriter(errors));
		bodyMsg.append(errors.toString());
		M_log.debug(bodyMsg.toString());

		Properties properties = getMailProperties();
		Session session = Session.getInstance(properties);
		MimeMessage message = new MimeMessage(session);

		try {
			String ccmSupportAddress = appExtPropertiesFile.getProperty(Utils.SIS_REPORT_CCM_SUPPORT_ADDRESS);
			String fromAddress = appExtPropertiesFile.getProperty(Utils.FRIEND_CONTACT_EMAIL);
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(ccmSupportAddress));
			message.setFrom(new InternetAddress(fromAddress));
			message.setSubject("Canvas Course Manage: Polling Thread Exceptions");
			Multipart multipart = new MimeMultipart();
			BodyPart body = new MimeBodyPart();
			body.setText(bodyMsg.toString());
			multipart.addBodyPart(body);
			message.setContent(multipart);
			Transport.send(message);

		} catch (MessagingException e) {
			M_log.error("Email failed due to " + e.getMessage());
		} catch (Exception e) {
			M_log.error("Email failed due to " + e.getMessage());
		}
	}

	private Properties getMailProperties() {
		Properties properties = System.getProperties();
		properties.put(Utils.MAIL_SMTP_AUTH, "false");
		properties.put(Utils.MAIL_SMTP_STARTTLS, "true");
		properties.put(Utils.MAIL_SMTP_HOST, appExtPropertiesFile.getProperty(Utils.MAIL_HOST));
		//if enabled will print out raw email body to logs.
		properties.put(Utils.MAIL_DEBUG, appExtPropertiesFile.getProperty(Utils.IS_MAIL_DEBUG_ENABLED));
		return properties;
	}

	public static String getBody(SISUploadType sisProcessType, JSONObject apiResp) throws JSONException {
		String workflowState = (String) apiResp.get(Utils.JSON_PARAM_WORKFLOW_STATE);
		String startTime = (String) apiResp.get("started_at");
		String endTime = (String) apiResp.get("ended_at");

		StringBuilder msgBody = new StringBuilder();
		msgBody.append("StartTime: " + startTime);
		msgBody.append(Utils.CONSTANT_LINE_FEED);
		msgBody.append("EndTime: " + endTime);
		msgBody.append(Utils.CONSTANT_LINE_FEED);

		// canvas success/failure/partial success response is stated with 3 Json attribute
		// imported = success; failed_with_message = Failure of SIS process;
		// imported_with_message = partial success, we report to the user about these conditions

		if (workflowState.equals(Utils.JSON_PARAM_IMPORTED_WITH_MESSAGES)) {
			msgBody.append("SIS upload imported with some errors");
			msgBody.append(Utils.CONSTANT_LINE_FEED);
			JSONArray processing_warnings = apiResp.getJSONArray("processing_warnings");
			for (int i = 0; i < processing_warnings.length(); i++) {
				msgBody.append(processing_warnings.get(i));
				msgBody.append(Utils.CONSTANT_LINE_FEED);
			}
			return msgBody.toString();

		}

		if (workflowState.equals(Utils.JSON_PARAM_FAILED_WITH_MESSAGES)) {
			msgBody.append("SIS upload Failed due to ");
			JSONArray processingErrors = apiResp.getJSONArray("processing_errors");
			for (int i = 0; i < processingErrors.length(); i++) {
				msgBody.append(processingErrors.get(i));
				msgBody.append(Utils.CONSTANT_LINE_FEED);
			}
			return msgBody.toString();

		}

		if (workflowState.equals(Utils.JSON_PARAM_IMPORTED)) {
			if (sisProcessType.equals(SISUploadType.ADD_SECTIONS)) {
				int sectionAddedCount = (int) apiResp.getJSONObject("data").getJSONObject("counts").get("sections");
				//giving the count from canvas how many section added to course, user could match up to one in the
				//attachment since as part of finished sis response canvas is not providing any more detail.
				msgBody.append("Sections added: " + sectionAddedCount);
			}
			if(sisProcessType.equals(SISUploadType.ADD_USERS_TO_SECTIONS)){
				int enrollmentsToSection = (int) apiResp.getJSONObject("data").getJSONObject("counts").get("enrollments");
				msgBody.append("Enrollments added: " + enrollmentsToSection);
			}
		}
		return msgBody.toString();
	}

	public BodyPart getAttachment(SISDataHolderForEmail data) throws MessagingException {
		BodyPart attachment = new MimeBodyPart();
		attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(data.getSisEmailData().getBytes(),
				Utils.CONSTANT_MIME_TEXT_CSV)));
		attachment.setFileName(data.getSisProcessType().getDescription() + "_" + data.getCourseId() + ".csv");

		return attachment;

	}

	// defaults to 5 attempts (if property goes missing/bad in ccm.properties) to retry polling before giving up
	private int getPollingAttempt() {
		return Utils.getIntegerValueOfProperty(Utils.SIS_POLLING_ATTEMPTS);
	}

	//defaulting to every minute polling if the property goes missing/bad in ccm.properties
	private Integer getSleepTimeForPolling() {
		return Utils.getIntegerValueOfProperty(Utils.SIS_POLLING_SLEEPTIME);
	}


}
