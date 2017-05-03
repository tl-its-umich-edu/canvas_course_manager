package edu.umich.ctools.sectionsUtilityTool;

/**
 * Created by pushyami on 4/11/17.
 */
public enum CourseUploadType {
	ADD_SECTIONS("addSections", "adding_sections_to_course"),
	ADD_USERS_TO_SECTIONS("addUsersToSections", "adding_enrollments_to_sections_in_course"),
	ADD_GROUPS_AND_USERS("addGroupsAndUsers","adding_groups_and_enrollments_to_course");

	private final String description;

	private String value;

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	CourseUploadType(String value, String description) {
		this.value = value;
		this.description = description;
	}
}
