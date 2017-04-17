package edu.umich.ctools.sectionsUtilityToolTest;

import edu.umich.ctools.sectionsUtilityTool.Utils;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by pushyami on 4/17/17.
 */
public class UtilsTest extends TestCase {
	@Before
	public void setUp() throws Exception {
		Utils.canvasURL="https://howdy.test.com";

	}
	@Test
	public void testURLFormatter(){
		String actual = Utils.urlConstructor(Utils.URL_CHUNK_ACCOUNTS, "12345");
		assertEquals("https://howdy.test.com/api/v1/accounts/12345", actual);

		actual = Utils.urlConstructor(Utils.URL_CHUNK_COURSES, "45667");
		assertEquals("https://howdy.test.com/api/v1/courses/45667",(actual));

		actual = Utils.urlConstructor(Utils.URL_CHUNK_COURSES,"34567",Utils.URL_CHUNK_COURSE_SIS_COURSE_ID,"cmmS12121");
		assertEquals("https://howdy.test.com/api/v1/courses/34567/?course[sis_course_id]=cmmS12121",actual);
	}


}
