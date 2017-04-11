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
    private int pollingAttempts =Integer.valueOf(appExtPropertiesFile.getProperty(Utils.SIS_POLLING_ATTEMPTS));
    private int pollingFrequency =Integer.valueOf(appExtPropertiesFile.getProperty(Utils.SIS_POLLING_FREQUENCY));


    @Override
    public void run() {
        while (flag) {
            M_log.debug("***************************** Starting polling Thread");
            List<SISDataHolderForEmail> pollingId = SectionsUtilityToolServlet.canvasPollingIds;
            listOFPolls(pollingId);
            Iterator<SISDataHolderForEmail> iterator = pollingId.iterator();
            while (iterator.hasNext()) {
                SISDataHolderForEmail emailData = iterator.next();
                M_log.debug(String.format("Number of attempts by for job %s are %s "
                        , emailData.getPollingId(), emailData.getNumberOfTries()));
                if (emailData.getNumberOfTries() >= pollingAttempts) {
                    emailData.setSISUploadFailed(true);
                    sendEmailReport(emailData, null);
                    iterator.remove();
                }
                ApiResultWrapper arw = sisApiCallCheckingJobStatus(emailData);
                int status = arw.getStatus();

                if (status != HttpStatus.SC_OK) {
                    M_log.warn(String.format("SIS polling call failed with status code %s due to %s"
                            , status, arw.getMessage()));
                    emailData.setNumberOfTries(1);
                    continue;
                }

                String apiResp = arw.getApiResp();
                if (!isSISUploadDone(apiResp)) {
                    emailData.setNumberOfTries(1);
                    continue;
                }
                sendEmailReport(emailData, apiResp);
                iterator.remove();

            }
            listOFPolls(pollingId);
            M_log.debug("***************************** Finish polling Thread");

            try {
                Thread.sleep(pollingFrequency);
            } catch (InterruptedException e) {
                M_log.error("Canvas polling thread got Interrupted due to "+e.getMessage());
            } catch (Exception e) {
                M_log.error("Canvas polling thread got Interrupted due to "+e.getMessage());
            }

        }

    }

    private void listOFPolls(List<SISDataHolderForEmail> pollingId) {
        for (SISDataHolderForEmail daa: pollingId){
        M_log.debug("PollingList" +daa.getPollingId());
        }
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

    private ApiResultWrapper sisApiCallCheckingJobStatus(SISDataHolderForEmail data) {
        String url = Utils.urlConstructor("/accounts/1/sis_imports/", String.valueOf(data.getPollingId()));
        ApiResultWrapper arw = Utils.makeApiCall(new HttpGet(url));
        return arw;
    }

    public void sendEmailReport(SISDataHolderForEmail data, String apiResp) {
        M_log.debug("sendEmailReport(): Called");
        String fromAddress = appExtPropertiesFile.getProperty(Utils.FRIEND_CONTACT_EMAIL);
        String ccAddress = appExtPropertiesFile.getProperty(Utils.SIS_REPORT_CC_ADDRESS);

        Properties properties = System.getProperties();
        properties.put(Utils.MAIL_SMTP_AUTH, "false");
        properties.put(Utils.MAIL_SMTP_STARTTLS, "true");
        properties.put(Utils.MAIL_SMTP_HOST, appExtPropertiesFile.getProperty(Utils.MAIL_HOST));
        //if enabled will print out raw email body to logs.
        properties.put(Utils.MAIL_DEBUG, appExtPropertiesFile.getProperty(Utils.IS_MAIL_DEBUG_ENABLED));

        Session session = Session.getInstance(properties);
        MimeMessage message = new MimeMessage(session);
        String errmsg = "Problem in sending the email for course %s due to %s";
        try {
            message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccAddress));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(data.getEmailAddress()));
            message.setFrom(new InternetAddress(fromAddress));
            message.setSubject(String.format(data.getSisProcessType().getDescription() + " " + data.getCourseId()));

            //if canvas taking long time to process the sis request, after X attempts intentionally we take the request
            // out of the thread pool that's when apiResp is null and we send a email to user about this situation.
            if ((apiResp == null) && data.isSISUploadFailed()) {
                Multipart multipart = new MimeMultipart();
                BodyPart bodyParts = new MimeBodyPart();
                //BSA might come up with better wording, so keeping it simple for now
                bodyParts.setText((data.getSisProcessType().getDescription() + " " +
                        "request either failed or canvas is taking too long to process this request"));
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
            if (workflowState == Utils.JSON_PARAM_IMPORTED) {
                multipart.addBodyPart(getAttachment(data));
            }

            message.setContent(multipart);
            M_log.info("Sending email for course " + data.getCourseId());
            Transport.send(message);
        } catch (MessagingException | JSONException e) {
            M_log.error(String.format(errmsg, data.getCourseId(), e.getMessage()));
        } catch (Exception e) {
            M_log.error(String.format(errmsg, data.getCourseId(), e.getMessage()));
        }

    }

    public static String getBody(SISUploadType sisProcessType, JSONObject apiResp) throws JSONException {
        String workflowState = (String) apiResp.get(Utils.JSON_PARAM_WORKFLOW_STATE);
        String startTime = (String) apiResp.get("started_at");
        String endTime = (String) apiResp.get("ended_at");

        StringBuilder msgBody = new StringBuilder();
        msgBody.append("StartTime: " + startTime);
        msgBody.append("\n");
        msgBody.append("EndTime: " + endTime);
        msgBody.append("\n");

        if (workflowState.equals(Utils.JSON_PARAM_IMPORTED_WITH_MESSAGES)) {
            msgBody.append("SIS upload imported with some errors");
            msgBody.append("\n");
            JSONArray processing_warnings = apiResp.getJSONArray("processing_warnings");
            for (int i = 0; i < processing_warnings.length(); i++) {
                msgBody.append(processing_warnings.get(i));
                msgBody.append("\n");
            }
            return msgBody.toString();

        }

        if (workflowState.equals(Utils.JSON_PARAM_FAILED_WITH_MESSAGES)) {
            msgBody.append("SIS upload Failed due to ");
            JSONArray processingErrors = apiResp.getJSONArray("processing_errors");
            for (int i = 0; i < processingErrors.length(); i++) {
                msgBody.append(processingErrors.get(i));
                msgBody.append("\n");
            }
            return msgBody.toString();

        }

        if (workflowState.equals(Utils.JSON_PARAM_IMPORTED)) {
            if (sisProcessType.equals(SISUploadType.ADD_SECTIONS)) {
                int sectionAddedCount = (int) apiResp.getJSONObject("data").getJSONObject("counts").get("sections");
                //giving the count from canvas how many section added to course. user could match up to one in the
                //attachment since as part of finished sis response canvas is not providing any more detail.
                msgBody.append("Sections added: " + sectionAddedCount);
            }
        }
        return msgBody.toString();
    }

    public BodyPart getAttachment(SISDataHolderForEmail data) throws MessagingException {
        BodyPart attachment = new MimeBodyPart();
        attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(data.getSisEmailData().getBytes(),
                Utils.CONSTANT_MIME_TEXT_CSV)));
        if (data.getSisProcessType().equals(SISUploadType.ADD_SECTIONS)) {
            attachment.setFileName(SISUploadType.ADD_SECTIONS.getDescription() + "_"+ data.getCourseId()+".csv");
        }
        return attachment;

    }
}
