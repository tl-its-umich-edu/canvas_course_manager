package edu.umich.ctools.sectionsUtilityTool;

/**
 * Created by pushyami on 4/10/17.
 */
public class SISDataHolderForEmail {

	private final String courseId;
	private String emailAddress;
	private int pollingId;
	private CourseUploadType uploadType;
	private String sisEmailData;
	private int numberOfTries;
	private boolean isSISUploadVerySlow = false;

	public CourseUploadType getUploadType() {
		return uploadType;
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
	public boolean isSISUploadVerySlow() {
		return isSISUploadVerySlow;
	}

	public void setSISUploadVerySlow(boolean SISUploadVerySlow) {
		isSISUploadVerySlow = SISUploadVerySlow;
	}


	public SISDataHolderForEmail(int pollingId, String courseId, String emailAddress, CourseUploadType sisProcessType, String sisEmailData){
		this.pollingId=pollingId;
		this.courseId=courseId;
		this.emailAddress = emailAddress;
		this.uploadType =sisProcessType;
		this.sisEmailData=sisEmailData;
	}

}
