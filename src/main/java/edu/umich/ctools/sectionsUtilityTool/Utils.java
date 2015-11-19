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
		
		String url;
		String queryString = request.getQueryString();
		
		if(queryString!=null) {
			url=pathInfo+"?"+queryString;
		}else {
			url=pathInfo;
		}
		
		File testFile = null;
		FileReader fr = null;

		if (url.matches(SectionsUtilityToolServlet.appExtPropertiesFile.getProperty(CANVAS_API_TERMS))){
			try{
				M_log.info("Courses call stub");
				testFile = new File( Utils.class.getResource("/stubs/canvas/termsSample.txt").toURI() );
				fr = new FileReader(testFile);  
			}
			catch(Exception e){
				M_log.error("I don't think it worked");
				e.printStackTrace();
			}
		}
		
		else if(pathInfo.equalsIgnoreCase("/api/v1/courses/2222/sections")){
			try{
				M_log.info("Courses call stub");
				testFile = new File( Utils.class.getResource("/stubs/canvas/mySecondSections.txt").toURI() );
				fr = new FileReader(testFile);  
			}
			catch(Exception e){
				M_log.error("I don't think it worked");
				e.printStackTrace();
			}
		}
		
		else if(pathInfo.equalsIgnoreCase("/api/v1/courses/1111/sections")){
			try{
				M_log.info("Courses call stub");
				testFile = new File( Utils.class.getResource("/stubs/canvas/myFirstSections.txt").toURI() );
				fr = new FileReader(testFile);  
			}
			catch(Exception e){
				M_log.error("I don't think it worked");
				e.printStackTrace();
			}
		}
		
		else if(pathInfo.equalsIgnoreCase("/api/v1/courses")){
			try{
				M_log.info("Courses call stub");
				testFile = new File( Utils.class.getResource("/stubs/canvas/coursesSample.txt").toURI() );
				fr = new FileReader(testFile);  
			}
			catch(Exception e){
				M_log.error("I don't think it worked");
				e.printStackTrace();
			}
		}
		
		else if(url.matches(SectionsUtilityToolServlet.appExtPropertiesFile.getProperty(CANVAS_API_GET_COURSE))){
			try{
				M_log.info("Specific courses call stub");
				testFile = new File( Utils.class.getResource("/stubs/canvas/myCoursesSample.txt").toURI() );
				fr = new FileReader(testFile);  
			}
			catch(Exception e){
				M_log.error("I don't think it worked");
				e.printStackTrace();
			}
		}
		
		// canvas.api.getsection.per.course.regex
		else if(url.matches(SectionsUtilityToolServlet.appExtPropertiesFile.getProperty(CANVAS_API_GETALLSECTIONS_PER_COURSE))){
			try{
				M_log.info("Sections call stub");
				testFile = new File( Utils.class.getResource("/stubs/canvas/sectionsSample.txt").toURI() );
				fr = new FileReader(testFile);  
			}
			catch(Exception e){
				M_log.error("I don't think it worked");
				e.printStackTrace();
			}
		}
		
		else if(pathInfo.equalsIgnoreCase(SectionsUtilityToolServlet.MPATHWAYS_PATH_INFO)){
			try{
				M_log.info("MPathways call stub");
				testFile = new File( Utils.class.getResource("/stubs/esb/mpathwaysSample.txt").toURI() );
				fr = new FileReader(testFile);  
			}
			catch(Exception e){
				M_log.error("I don't think it worked");
				e.printStackTrace();
			}
		}
		else{
			M_log.debug("Hello, I'm not ready yet! " + url);
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
			M_log.debug("I think it worked");
		}
		catch(Exception e){
			M_log.error("I don't think it worked");
			e.printStackTrace();
		}
		
		
	}
}