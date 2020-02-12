package putfile;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static final int TEST_PASS = 0;
    public static final int TEST_FAIL = 1;
    public List<UnitTest> unitTestList = new ArrayList<UnitTest>();
    private static String[] noOptionsTestArgs = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png::/home/parsons/tmp/smallFileTest.png" };
    
    private static String[] mediumFileArgs = {
        "https://effigis.com/wp-content/uploads/2015/02/DigitalGlobe_WorldView1_50cm_8bit_BW_DRA_Bangkok_Thailand_2009JAN06_8bits_sub_r_1.jpg::/home/parsons/tmp/mediumFileTest.png" };
    
    private static String[] largeFileArgs = {
        "https://effigis.com/wp-content/uploads/2015/02/Airbus_Pleiades_50cm_8bit_RGB_Yogyakarta.jpg::/home/parsons/tmp/largeFileTest.jpg" };

    private static String[] multiFileSuccess = {"https://homepages.cae.wisc.edu/~ece533/images/airplane.png::/home/parsons/tmp/smallFileTest.png", 
        "https://effigis.com/wp-content/uploads/2015/02/Airbus_Pleiades_50cm_8bit_RGB_Yogyakarta.jpg::/home/parsons/tmp/largeFileTest.jpg",
        "https://effigis.com/wp-content/uploads/2015/02/DigitalGlobe_WorldView1_50cm_8bit_BW_DRA_Bangkok_Thailand_2009JAN06_8bits_sub_r_1.jpg::/home/parsons/tmp/mediumFileTest.png",
        "-f" };
    
    private static String[] createDirsSuccess = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png::/home/parsons/tmp/newDir/smallFileTest.png", "-p", "-f" };    
    
    private static String[] createDirsFail = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png::/home/parsons/tmp/newDir/smallFileTest.png"};

    private static String[] fileOverwriteSuccess = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png::/home/parsons/tmp/smallFileTest.png", "-f" };

    private static String[] setWaitSuccess = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png::/home/parsons/tmp/smallFileTest.png", "-w", "10", "-f" };

    private static String[] setTimeoutSuccess = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png::/home/parsons/tmp/smallFileTest.png", "-t", "10", "-f" };

    private static String[] badArgs1 = { "Some Garbage" };
    
    private static String[] notEnoughArgs = {"https://www.google.com"};
    
    private static String[] badUrl = { "https://www.wickerpedia.com/basket.png::/home/parsons/tmp/badArgs.png" };
    
    private static String[] wOptionNoArg = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png::/home/parsons/tmp/badArgsTest.png", "-w" };
    
    private static String[] tOptionNoArg = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png::/home/parsons/tmp/badArgsTest.png", "-t" };

    private static String[] setWaitFail = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png::/home/parsons/tmp/waitFail.png", "-w", "130" };

    private static String[] setTimeoutFail = { "https://homepages.cae.wisc.edu/~ece533/images/airplane.png::/home/parsons/tmp/waitFail.png", "-t", "130" };

    public Test() {                  
        
        int[] res = {5};
        
        unitTestList.add(createTest("basicTestNoFlags", noOptionsTestArgs, res));
              
        unitTestList.add(createTest("mediumFileTest", mediumFileArgs, res));    
            
        unitTestList.add(createTest("setTimeoutTestSuccess", setTimeoutSuccess, res));
        
        unitTestList.add(createTest("largeFileTest", largeFileArgs, res));
        unitTestList.add(createTest("createDirsTestSuccess", createDirsSuccess, res));        
        unitTestList.add(createTest("fileOverwriteSuccess",fileOverwriteSuccess, res));                
        unitTestList.add(createTest("setWaitTestSuccess", setWaitSuccess, res));                  
        
        int[] res2 = {1};        
        unitTestList.add(createTest("fileOeverwriteFail",noOptionsTestArgs, res2));                
        unitTestList.add(createTest("createDirsTestFail", createDirsFail, res2));
        
        int[] res3 = {4};
        unitTestList.add(createTest("setWaitTestFail", setWaitFail, res3));        
        unitTestList.add(createTest("setTimeoutTestFail", setTimeoutFail, res3));
        unitTestList.add(createTest("justBadArgs", badArgs1, res3));
        unitTestList.add(createTest("notEnoughArgs",notEnoughArgs, res3));                 
        unitTestList.add(createTest("WaitNoArg", wOptionNoArg, res3));
        unitTestList.add(createTest("TimeoutNoArg",tOptionNoArg, res3));
        int[] res4 = {3};
        unitTestList.add(createTest("badURL",badUrl, res4));               
        // mult-file tests
        int[] res5 = {5,5,5};
        unitTestList.add(createTest("multiFile", multiFileSuccess, res5));
                      
        

    }
    private UnitTest createTest(String name, String[] args, int[] expectedResults) {
        UnitTest ut = new UnitTest();
        ut.name = name;
        ut.args = args;
        ut.expectedResults = expectedResults;
        return ut;
    }
}