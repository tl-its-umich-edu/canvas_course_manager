package edu.umich.ctools.sectionsUtilityToolTest;

import edu.umich.ctools.sectionsUtilityTool.SISPollingThread;
import edu.umich.ctools.sectionsUtilityTool.SISUploadType;
import junit.framework.TestCase;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by pushyami on 4/12/17.
 */
public class SISPollingThreadTest extends TestCase {
    protected static final String PATH_SRC_TEST_JAVA_EDU_UMICH_CTOOLS = "/src/test/java/edu/umich/ctools/sectionsUtilityToolTest";
    String failedJson;
    String partialSuccessJson;
    String successJson;
    String progress;
    @Before
    public void setUp() throws Exception {
        Path importedFilePath = Paths.get(Paths.get(".").toAbsolutePath() + PATH_SRC_TEST_JAVA_EDU_UMICH_CTOOLS
                + "/imported.json");
        byte[] importedJson = Files.readAllBytes(importedFilePath);
        successJson = new String(importedJson);

        Path failedFilePath = Paths.get(Paths.get(".").toAbsolutePath() + PATH_SRC_TEST_JAVA_EDU_UMICH_CTOOLS +
                "/failed_with_messages.json");
        byte[] failedFileJson = Files.readAllBytes(failedFilePath);
        failedJson = new String(failedFileJson);

        Path importedWithMessagesPath = Paths.get(Paths.get(".").toAbsolutePath() + PATH_SRC_TEST_JAVA_EDU_UMICH_CTOOLS +
                "/imported_with_messages.json");
        byte[] importedWithMessagesJson = Files.readAllBytes(importedWithMessagesPath);
        partialSuccessJson = new String(importedWithMessagesJson);

        Path progressPath = Paths.get(Paths.get(".").toAbsolutePath() + PATH_SRC_TEST_JAVA_EDU_UMICH_CTOOLS +
                "/progress.json");
        byte[] progressBytes = Files.readAllBytes(progressPath);
        progress = new String(progressBytes);


    }
    @Test
    public void testMessageBodyForSISImportedWithErrors() {
        String actual = SISPollingThread.getBody(SISUploadType.ADD_SECTIONS, new JSONObject(this.partialSuccessJson));
        Path path = Paths.get(Paths.get(".").toAbsolutePath() + PATH_SRC_TEST_JAVA_EDU_UMICH_CTOOLS +
                "/msgBodyExpectedPartialSuccess.txt");
        byte[] expected = null;
        try {
            expected = Files.readAllBytes(path);
        } catch (IOException e) {
            fail("Problem reading the file");
        }

        assertEquals(new String(expected), actual);
    }

    @Test
    public void testMsessageBodyForSISImportFailure(){
        String actual = SISPollingThread.getBody(SISUploadType.ADD_SECTIONS, new JSONObject(this.failedJson));
        Path path = Paths.get(Paths.get(".").toAbsolutePath() + PATH_SRC_TEST_JAVA_EDU_UMICH_CTOOLS +
                "/msgBodyExpectedFailed.txt");
        byte[] expected = null;
        try {
            expected = Files.readAllBytes(path);
        } catch (IOException e) {
            fail("Problem reading the file");
        }

        assertEquals(new String(expected), actual);

    }

    @Test
    public void testMsgBodyForSISImportSuccess(){
        String actual = SISPollingThread.getBody(SISUploadType.ADD_SECTIONS, new JSONObject(this.successJson));
        System.out.println(actual);
        Path path = Paths.get(Paths.get(".").toAbsolutePath() + PATH_SRC_TEST_JAVA_EDU_UMICH_CTOOLS +
                "/msgBodyExpectedSuccess.txt");
        byte[] expected = null;
        try {
            expected = Files.readAllBytes(path);
        } catch (IOException e) {
            fail("Problem reading the file");
        }

        assertEquals(new String(expected).trim(), actual);


    }

    @Test
    public void testIsSISUploadDone(){
        //the test is based on canvas response param "progress =100"
        assertEquals(SISPollingThread.isSISUploadDone(this.successJson),true);
        assertEquals(SISPollingThread.isSISUploadDone(this.failedJson),true);
        assertEquals(SISPollingThread.isSISUploadDone(this.partialSuccessJson),true);
        assertEquals(SISPollingThread.isSISUploadDone(this.progress),false);

    }

}
