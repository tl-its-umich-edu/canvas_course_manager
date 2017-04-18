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
import java.util.Map;

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
		PrintWriter out=response.getWriter();
		String csvFileContent = getAttachmentContent();

		if (csvFileContent == null) {
			response.setStatus(Utils.API_UNKNOWN_ERROR);
			String errMsg = "{\"errorMsg\":\"Failed to grab the attachment content\"}";
			out.print(errMsg);
			out.flush();
			return;
		}
		String pathInfo = request.getPathInfo();

		TcSessionData tc = (TcSessionData) request.getSession().getAttribute(Utils.TC_SESSION_DATA);
		HashMap<String, Object> customValuesMap = tc.getCustomValuesMap();
		String emailAddress = (String) customValuesMap.get(Utils.LTI_PARAM_CONTACT_EMAIL_PRIMARY);
		String courseId = (String) customValuesMap.get(Utils.LTI_PARAM_CANVAS_COURSE_ID);

		if (pathInfo.contains(SISUploadType.ADD_SECTIONS.getValue())) {
			String courseID = (String) customValuesMap.get(Utils.LTI_PARAM_CANVAS_COURSE_ID);
			// step to construct csv file for sis upload.
			HashMap<Integer, String> csvContentWithStatus = makeSectionsCSVForSISUpload(csvFileContent, courseID);
			Map.Entry<Integer, String> csvFile = csvContentWithStatus.entrySet().iterator().next();

			int status = csvFile.getKey();
			String csvFinalContent = csvFile.getValue();
			//things went wrong in making the csv file
			if (status != HttpStatus.SC_OK) {
				response.setStatus(status);
				out.print(csvFinalContent);
				out.flush();
				return;
			}
			//So lets do the sisupload with constructed csv file
			ApiResultWrapper arw = sisUploadCall(csvFinalContent);

			status = arw.getStatus();
			response.setStatus(status);
			// send the response to UI stating the cause for failure
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
			savingPollingIdsForReporting(csvFinalContent, emailAddress, courseId, arw, SISUploadType.ADD_SECTIONS);
			//send the good response to UI
			out.print(arw.getApiResp());
			out.flush();
		}
	}

	private synchronized void savingPollingIdsForReporting(String csvFileContent, String emailAddress, String courseId,
														   ApiResultWrapper arw, SISUploadType type) {
		String apiResp = arw.getApiResp();
		JSONObject sisRes = new JSONObject(apiResp);
		int id = (int) sisRes.get("id");
		SISDataHolderForEmail emailData = new SISDataHolderForEmail(id, courseId, emailAddress, type, csvFileContent);
		M_log.info("Adding the Polling Id to List: "+id);
		SectionsUtilityToolServlet.canvasPollingIds.add(emailData);
		addingPollingIdCount();
	}

	private synchronized void addingPollingIdCount(){
		SectionsUtilityToolServlet.addedPollingIdCount += 1;
		M_log.info(" @&*@&* Number of POLLING ID's Added are " + SectionsUtilityToolServlet.addedPollingIdCount);
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
				M_log.debug(csv);

			}
		} catch (FileUploadException | IOException e) {
			M_log.error("Error getting attachment content from the UI due to "+e.getMessage());
			return null;
		} catch (Exception e) {
			M_log.error("Error getting attachment content from the UI due to "+e.getMessage());
			return null;
		}
		return csv.toString();
	}

	private HashMap<Integer, String> makeSectionsCSVForSISUpload(String csvSectionsData, String courseID) {
		M_log.debug("makeSectionsCSVForSISUpload() call");
		// course_sis_id is needed as part of creating sections via sis_upload
		HashMap<Integer, String> csvMap = new HashMap<>();
		ApiResultWrapper arw = getCourseSisId(courseID);

		int status = arw.getStatus();
		if (status == Utils.API_EXCEPTION_ERROR || status == Utils.API_UNKNOWN_ERROR) {
			csvMap.put(status, arw.getMessage());
			return csvMap;
		}
		if (status != HttpStatus.SC_OK) {
			csvMap.put(status, arw.getApiResp());
			return csvMap;
		}
		JSONObject courseJson = new JSONObject(arw.getApiResp());
		String courseSisId = (String) courseJson.get("sis_course_id");

		String[] sections = csvSectionsData.split("\n");
		StringBuilder completeCsvFile = new StringBuilder();
		completeCsvFile.append(Utils.CSV_HEADERS_SECTIONS);
		completeCsvFile.append("\n");
		String sisSectionIdChunk = Utils.getSisSectionIDChunk(request);
		for (int i = 1; i < sections.length; i++) {
			String[] aSectionInfo = sections[i].split(",");
			String section = aSectionInfo[0].replace(aSectionInfo[0], sisSectionIdChunk + aSectionInfo[0]) + ","
					+ aSectionInfo[1] + "," + "active" + "," + courseSisId;
			completeCsvFile.append(section);
			completeCsvFile.append("\n");
		}
		M_log.debug(completeCsvFile);
		csvMap.put(status, completeCsvFile.toString());
		return csvMap;
	}

	private ApiResultWrapper sisUploadCall(String csvFileContent){
		String url = Utils.urlConstructor(Utils.URL_CHUNK_ACCOUNTS_1_SIS_IMPORTS+Utils.URL_CHUNK_SIS_IMPORT_WITH_BATCH_MODE_STICKINESS_DISABLED);
		HttpPost post = new HttpPost(url);
		post.setEntity(new StringEntity(csvFileContent, ContentType.create(Utils.CONSTANT_MIME_TEXT_CSV, "UTF-8")));
		ApiResultWrapper arw = Utils.makeApiCall(post);
		M_log.debug("API response: "+arw.getApiResp());
		return arw;
	}

	private ApiResultWrapper getCourseSisId(String courseId) {
		M_log.debug("getCourseSisId() call");
		String course_account_id;
		String url = Utils.urlConstructor(Utils.URL_CHUNK_COURSES, courseId);
		ApiResultWrapper arw = Utils.makeApiCall(new HttpGet(url));
		String msg = String.format("couldn't get course_sis_id for course %s, with Status code %s, due to: %s", courseId,
				arw.getStatus(), (arw.getApiResp().isEmpty()) ? arw.getMessage() : arw.getApiResp());

		if(!Utils.didApiReturnedWithOutErrors(msg, arw)){
			return arw;
		}
		JSONObject courseJson = new JSONObject(arw.getApiResp());
		if (courseJson.isNull("sis_course_id")) {
			return createACourseSISId(courseId);
		}
		return arw;
	}

	private ApiResultWrapper createACourseSISId(String courseId) {
		M_log.debug("createACourseSISId() call");
		String courseSISId = Utils.generateSisCourseId();
		//https://umich.test.instructure.com/api/v1/courses/183787/?course[sis_course_id]=pu_sis_id_0329_3
		String url = Utils.urlConstructor(Utils.URL_CHUNK_COURSES, courseId, Utils.URL_CHUNK_COURSE_SIS_COURSE_ID, courseSISId);
		ApiResultWrapper arw = Utils.makeApiCall(new HttpPut(url));
		String msg = String.format("couldn't create course_sis_id for course %s, with status code %s, due to: %s", courseId,
				arw.getStatus(), (arw.getApiResp().isEmpty()) ? arw.getMessage() : arw.getApiResp());

		 if(!Utils.didApiReturnedWithOutErrors(msg, arw)){
			 return arw;
		 }
		return arw;

	}

}
