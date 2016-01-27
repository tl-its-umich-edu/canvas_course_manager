/*
Copyright 2015-2016 University of Michigan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package edu.umich.ctools.sectionsUtilityTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.umich.its.lti.utils.PropertiesUtilities;

public class Utils {

	private static Log M_log = LogFactory.getLog(Utils.class);

	private static final String CANVAS_API_GETALLSECTIONS_PER_COURSE = "canvas.api.getallsections.per.course.regex";
	private static final String CANVAS_API_TERMS = "canvas.api.terms.regex";
	private static final String CANVAS_API_GET_COURSE = "canvas.api.get.single.course.regex";
	private static final String CANVAS_API_SEARCH_USER = "canvas.api.search.user.regex";

	public static Properties loadProperties(String path){
		String propertiesFilePath = System.getProperty(path);
		M_log.debug(path + " : " + propertiesFilePath);
		if (!propertiesFilePath.isEmpty()) {
			return PropertiesUtilities.getPropertiesObjectFromURL(propertiesFilePath);
		}
		M_log.error("File path for (" + path + ") is not provided");
		return null;		
	}

	public static void openFile(HttpServletRequest request,
			HttpServletResponse response, PrintWriter out) {
		M_log.debug("Stub method called");
		String pathInfo = request.getPathInfo();
		M_log.debug("Path Info: " + pathInfo);

		String url = request.getPathInfo();
		String queryString = request.getQueryString();

		if(queryString!=null) {
			url=pathInfo+"?"+queryString;
		}

		FileReader fr = null;

		//CANVAS_API_SEARCH_USER
		if (url.matches(SectionsUtilityToolServlet.appExtPropertiesFile.getProperty(CANVAS_API_SEARCH_USER))){
			fr = retrieveTestFile(url, fr, "Courses call stub", "/stubs/canvas/users.txt");
		}
		
		else if (url.matches(SectionsUtilityToolServlet.appExtPropertiesFile.getProperty(CANVAS_API_TERMS))){
			fr = retrieveTestFile(url, fr, "Courses call stub", "/stubs/canvas/termsSample.txt");
		}

		else if(pathInfo.equalsIgnoreCase("/api/v1/courses/2222/sections")){
			fr = retrieveTestFile(url, fr, "Sections call for course 2222 stub", "/stubs/canvas/mySecondSections.txt");
		}

		else if(pathInfo.equalsIgnoreCase("/api/v1/courses/1111/sections")){
			fr = retrieveTestFile(url, fr, "Sections call for course 1111 stub", "/stubs/canvas/myFirstSections.txt");
		}

		else if(pathInfo.equalsIgnoreCase("/api/v1/courses")){
			fr = retrieveTestFile(url, fr, "Courses call stub", "/stubs/canvas/coursesSample.txt");
		}

		else if(url.matches(SectionsUtilityToolServlet.appExtPropertiesFile.getProperty(CANVAS_API_GET_COURSE))){
			fr = retrieveTestFile(url, fr, "Specific courses call stub", "/stubs/canvas/myCoursesSample.txt");
		}

		else if(url.matches(SectionsUtilityToolServlet.appExtPropertiesFile.getProperty(CANVAS_API_GETALLSECTIONS_PER_COURSE))){
			fr = retrieveTestFile(url, fr, "Sections call stub", "/stubs/canvas/sectionsSample.txt");
		}

		else if(pathInfo.equalsIgnoreCase(SectionsUtilityToolServlet.MPATHWAYS_PATH_INFO)){
			fr = retrieveTestFile(url, fr, "MPathways call stub", "/stubs/esb/mpathwaysSample.txt");
		}

		else{
			M_log.debug("Unrecognized call: " + url);
		}

		try{
			BufferedReader rd = new BufferedReader(fr);;
			String line = "";
			StringBuilder sb = new StringBuilder();
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			out.print(sb.toString());
			out.flush();
			M_log.debug("SUCCESS");
		}
		catch(Exception e){
			M_log.error("FAILURE");
			M_log.error("Exception: ", e);
		}
	}

	public static FileReader retrieveTestFile(String url, FileReader fr, String msg, String path) {
		File testFile;
		try{
			M_log.info(msg);
			testFile = new File( Utils.class.getResource(path).toURI() );
			fr = new FileReader(testFile);  
		}
		catch(Exception e){
			M_log.error("Call Failed: " + url);
			M_log.error("Exception: ", e);
		}
		return fr;
	}
}