package edu.umich.ctools.sectionsUtilityTool;

import edu.umich.its.lti.TcSessionData;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;

/**
 * Created by pushyami on 3/29/17.
 */
public class SISSupportProcess {

    private static Log M_log = LogFactory.getLog(SectionsUtilityToolServlet.class);

    private final HttpServletRequest request;
    private final HttpServletResponse response;


    public SISSupportProcess(HttpServletRequest request, HttpServletResponse response) {
        this.request=request;
        this.response=response;
    }

    protected void handleSISImportProcess() throws IOException {
        PrintWriter out;
        String csvFileContent = getAttachmentContent();
        String pathInfo = request.getPathInfo();
        TcSessionData tc = (TcSessionData) request.getSession().getAttribute(Utils.TC_SESSION_DATA);
        HashMap<String, Object> customValuesMap = tc.getCustomValuesMap();
        String emailAddress = (String)customValuesMap.get(Utils.LTI_PARAM_CONTACT_EMAIL_PRIMARY);
        String courseId = (String)customValuesMap.get(Utils.LTI_PARAM_CANVAS_COURSE_ID);

        if (pathInfo.contains(SISUploadType.ADD_SECTIONS.getValue())) {
            String courseID = (String) customValuesMap.get(Utils.LTI_PARAM_CANVAS_COURSE_ID);
            String completeCsvFile = makeSectionsCSVForSISUpload(csvFileContent, courseID);
            ApiResultWrapper arw = sisUploadProcess(completeCsvFile);

            int status = arw.getStatus();
            response.setStatus(status);
            out = response.getWriter();
            // send the bad response to UI
            if (status == Utils.API_EXCEPTION_ERROR || status == Utils.API_UNKNOWN_ERROR) {
                out.print(arw.getMessage());
                out.flush();
                return;
            }

            if (status != HttpStatus.SC_OK) {
                out.print(arw.getApiResp());
                out.flush();
                return;
            }
            // if good response then add the details to thread for later polling canvas status of job for reporting
            String apiResp = arw.getApiResp();
            JSONObject sisRes = new JSONObject(apiResp);
            int id = (int) sisRes.get("id");
            SISDataHolderForEmail emailData = new SISDataHolderForEmail
                    (id, courseId, emailAddress, SISUploadType.ADD_SECTIONS, completeCsvFile);
            SectionsUtilityToolServlet.canvasPollingIds.add(emailData);
            //send the good response to UI
            out.print(arw.getApiResp());
            out.flush();
        }
    }

    private String getAttachmentContent() {
        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload();
        StringBuilder csv=new StringBuilder();
        try {
             // Parse the request
            FileItemIterator iter = upload.getItemIterator(request);
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                InputStream inputStream = item.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    String csvContent = strLine.trim();
                    csv.append(csvContent);
                    csv.append("\n");
                }
                M_log.info(csv);

            }
        } catch (FileUploadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csv.toString();
    }

    private String makeSectionsCSVForSISUpload(String csvSectionsData, String courseID) {
        M_log.debug("makeSectionsCSVForSISUpload() call");
        // course_sis_id is needed as part of creating sections via sis_upload
        String courseSisId = getCourseSisId(courseID);
        String[] sections = csvSectionsData.split("\n");
        StringBuilder completeCsvFile = new StringBuilder();
        completeCsvFile.append(Utils.CSV_HEADERS_SECTIONS);
        completeCsvFile.append("\n");
        String sisSectionIdChunk = Utils.getSisSectionIDChunk(request);
        for (int i = 1; i < sections.length; i++) {
            String[] aSectionInfo = sections[i].split(",");
            String section = aSectionInfo[0].replace(aSectionInfo[0], sisSectionIdChunk + aSectionInfo[0]) + ","
                    + aSectionInfo[1] + "," + aSectionInfo[2] + "," + courseSisId;
            completeCsvFile.append(section);
            completeCsvFile.append("\n");
        }
        M_log.info(completeCsvFile);
        return completeCsvFile.toString();
    }

    private ApiResultWrapper sisUploadProcess(String csvFileContent){
        String url = Utils.urlConstructor("/accounts/1/sis_imports?batch_mode=0&override_sis_stickiness=0&extension=csv");
        HttpPost post = new HttpPost(url);
        post.setEntity(new StringEntity(csvFileContent, ContentType.create(Utils.CONSTANT_MIME_TEXT_CSV, "UTF-8")));
        ApiResultWrapper arw = Utils.makeApiCall(post);
        M_log.debug("API response: "+arw.getApiResp());
        return arw;
    }

    private String getCourseSisId(String courseId) {
        M_log.debug("getCourseSisId() call");
        String course_account_id;
        String url = Utils.urlConstructor("/courses/", courseId);
        ApiResultWrapper arw = Utils.makeApiCall(new HttpGet(url));
        String msg = String.format("couldn't get course_sis_id for course %s, with Status code %s, due to: %s", courseId,
                arw.getStatus(), (arw.getApiResp().isEmpty()) ? arw.getMessage() : arw.getApiResp());

        if(!Utils.didApiReturedWithOutErrors(msg, arw)){
            return null;
        }
        JSONObject courseJson = new JSONObject(arw.getApiResp());
        if (courseJson.isNull("sis_course_id")) {
            return createACourseSISId(courseId);
        }
        course_account_id = (String) courseJson.get("sis_course_id");
        return course_account_id;
    }

    private String createACourseSISId(String courseId) {
        M_log.debug("createACourseSISId() call");
        String course_sis_id = Utils.generateSisCourseId();
        //https://umich.test.instructure.com/api/v1/courses/183787/?course[sis_course_id]=pu_sis_id_0329_3
        String url = Utils.urlConstructor("/courses/", courseId, "/?course[sis_course_id]=" + course_sis_id);
        ApiResultWrapper arw = Utils.makeApiCall(new HttpPut(url));
        String msg = String.format("couldn't create course_sis_id for course %s, with status code %s, due to: %s", courseId,
                arw.getStatus(), (arw.getApiResp().isEmpty()) ? arw.getMessage() : arw.getApiResp());

         if(!Utils.didApiReturedWithOutErrors(msg, arw)){
             return null;
         }
        return course_sis_id;

    }

}
