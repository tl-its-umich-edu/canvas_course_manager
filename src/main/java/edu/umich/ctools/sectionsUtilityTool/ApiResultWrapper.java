package edu.umich.ctools.sectionsUtilityTool;


/**
 * Created by pushyami on 3/29/17.
 */

public class ApiResultWrapper {

	private int status;
	private String message;
	private String apiResp;

	public String getApiResp() {
		return apiResp;
	}

	public String getMessage() {
		return message;
	}

	public int getStatus() {
		return status;
	}

	public ApiResultWrapper(int status, String message, String apiResp) {
		this.status = status;
		this.message = message;
		this.apiResp = apiResp;
	}

}
