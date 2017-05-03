package edu.umich.ctools.sectionsUtilityTool;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by pushyami on 4/26/17.
 */
public class GroupProcessReport {
	// this map keeps track of all the users-group after creation/addition to canvas
	public Multimap<String, String> usersToGroup = HashMultimap.create();
	//this map holds actual content that we got csv upload from UI
	@Setter
	@Getter
	private Multimap<String, String> usersToGroupActual = HashMultimap.create();
	public HashMap<String, String> grpNameToIdMap = new HashMap<>();
	@Getter
	@Setter
	private String grpSetName;
	@Setter
	@Getter
	private String grpSetId;
	public List<String> errMessages = new ArrayList<>();

}
