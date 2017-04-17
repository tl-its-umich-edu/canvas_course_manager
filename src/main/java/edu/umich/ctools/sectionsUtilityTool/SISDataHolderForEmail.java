package edu.umich.ctools.sectionsUtilityTool;

/**
 * Created by pushyami on 4/10/17.
 */
public class SISDataHolderForEmail {

	private final String courseId;
	private String emailAddress;
	private int pollingId;
	private SISUploadType sisProcessType;
	private String sisEmailData;
	private int numberOfTries;
	private boolean isSISUploadFailed = false;

	public SISUploadType getSisProcessType() {
		return sisProcessType;
	}

	public String getEmailAddress() {
		return emailAddress;
	}
	public String getCourseId() {
		return courseId;
	}
	public void incrementNumberOfTries(int numberOfTries) {
		this.numberOfTries+= numberOfTries;
	}
	public int getNumberOfTries() {
		return numberOfTries;
	}

	public int getPollingId() {
		return pollingId;
	}

	public String getSisEmailData() {
		return sisEmailData;
	}
	public boolean isSISUploadFailed() {
		return isSISUploadFailed;
	}

	public void setSISUploadFailed(boolean SISUploadFailed) {
		isSISUploadFailed = SISUploadFailed;
	}


	public SISDataHolderForEmail(int pollingId, String courseId,String emailAddress, SISUploadType sisProcessType, String sisEmailData){
		this.pollingId=pollingId;
		this.courseId=courseId;
		this.emailAddress = emailAddress;
		this.sisProcessType=sisProcessType;
		this.sisEmailData=sisEmailData;
	}

}
