package edu.umich.ctools.sectionsUtilityTool;

/**
 * Created by pushyami on 4/11/17.
 */
public enum SISUploadType {
	ADD_SECTIONS("addSections", "adding_sections_to_course"),
	ADD_USERS_TO_SECTIONS("addUsersToSections", "adding_user_to_sections_in_course");

	private final String description;

	private String value;

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	SISUploadType(String value, String description) {
		this.value = value;
		this.description = description;
	}
}
