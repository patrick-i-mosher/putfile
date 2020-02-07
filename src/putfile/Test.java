package putfile;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static final int TEST_PASS = 0;
    public static final int TEST_FAIL = 1;
    public static final List<UnitTest> unitTestList = new ArrayList<UnitTest>();
    private static String[] noOptionsTestArgs = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png",
        "/home/parsons/tmp/smallFileTest.png" };
    private static String[] mediumFileArgs = {
        "https://effigis.com/wp-content/uploads/2015/02/DigitalGlobe_WorldView1_50cm_8bit_BW_DRA_Bangkok_Thailand_2009JAN06_8bits_sub_r_1.jpg",
        "/home/parsons/tmp/mediumFileTest.png" };
    private static String[] largeFileArgs = {
        "https://effigis.com/wp-content/uploads/2015/02/Airbus_Pleiades_50cm_8bit_RGB_Yogyakarta.jpg",
        "/home/parsons/tmp/largeFileTest.jpg" };
    private static String[] createDirsSuccessArgs = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png",
        "/home/parsons/tmp/smallFileTest.png", "-p" };    
    private static String[] fileOverwriteSuccessArgs = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png",
        "/home/parsons/tmp/smallFileTest.png", "-f" };
    private static String[] setWaitTestSuccessArgs = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png",
        "/home/parsons/tmp/smallFileTest.png", "-w", "10" };
    private static String[] setTimeoutTestSuccessArgs = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png",
        "/home/parsons/tmp/smallFileTest.png", "-t", "10" };
    private static String[] badArgs1 = { "Some Garbage" };
    private static String[] badArgs2 = { "https://www.wickerpedia.com/basket.png, /home/parsons/tmp/badArgs.png" };
    private static String[] badArgs3 = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png",
        "/home/parsons/tmp/badArgsTest.png", "-w" };
    private static String[] badArgs4 = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png",
        "/home/parsons/tmp/badArgsTest.png", "-t" };
    private static String[] badArgs5 = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png",
        "/home/parsons/tmp/badArgsTest.png", "-w", "130" };

    public Test() {                  
        unitTestList.add(createTest("basicTestNoFlags", noOptionsTestArgs, 5));
        unitTestList.add(createTest("mediumFileTest", mediumFileArgs, 5));
        unitTestList.add(createTest("largeFileTest", largeFileArgs, 5));
        unitTestList.add(createTest("createDirsTestSuccess", createDirsSuccessArgs, 5));
        unitTestList.add(createTest("createDirsTestFail", noOptionsTestArgs, 1));
        unitTestList.add(createTest("fileOverwriteSuccess",fileOverwriteSuccessArgs, 5));


                
    }
    private UnitTest createTest(String name, String[] args, int expectedResults) {
        UnitTest ut = new UnitTest();
        ut.name = name;
        ut.args = args;
        ut.expectedResults = expectedResults;
        return ut;
    }
}