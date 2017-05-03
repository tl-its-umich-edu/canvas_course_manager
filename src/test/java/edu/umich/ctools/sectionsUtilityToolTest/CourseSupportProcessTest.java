package edu.umich.ctools.sectionsUtilityToolTest;

import edu.umich.ctools.sectionsUtilityTool.CourseSupportProcess;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by pushyami on 4/21/17.
 */
public class CourseSupportProcessTest extends TestCase {
	protected static final String PATH_SRC_TEST_JAVA_EDU_UMICH_CTOOLS = "/src/test/java/edu/umich/ctools/sectionsUtilityToolTest";
	private String csv;
	private String swappedColumnCVS;

	@Before
	public void setUp() throws Exception {
		Path csvFilePath = Paths.get(Paths.get(".").toAbsolutePath() + PATH_SRC_TEST_JAVA_EDU_UMICH_CTOOLS
				+ "/csvfile.txt");
		byte[] csvBytes = Files.readAllBytes(csvFilePath);
		csv = new String(csvBytes);

		Path csvFilePath1 = Paths.get(Paths.get(".").toAbsolutePath() + PATH_SRC_TEST_JAVA_EDU_UMICH_CTOOLS
				+ "/csvfile1.txt");
		byte[] csv1Bytes = Files.readAllBytes(csvFilePath1);
		swappedColumnCVS = new String(csv1Bytes);
	}
	@Test
	public void testSwapCSVFileContent(){
		String actual = CourseSupportProcess.swapCSVFileContent(swappedColumnCVS);
		assertEquals(csv,actual);
	}

	@Test
	public void testHeaderStartingElement(){
		assertEquals(true, CourseSupportProcess.isHeaderStartWithIdPrefix(csv));
		assertEquals(false, CourseSupportProcess.isHeaderStartWithIdPrefix(swappedColumnCVS));
	}
}
