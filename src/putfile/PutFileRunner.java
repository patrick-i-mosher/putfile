package putfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PutFileRunner {
    /*
     * Useful files for testing: Small file (450KB):
     * https://homepages.cae.wisc.edu/~ece533/images/airplane.png Medium file
     * (16MB): https://effigis.com/wp-content/uploads/2015/02/
     * DigitalGlobe_WorldView1_50cm_8bit_BW_DRA_Bangkok_Thailand_2009JAN06_8bits_sub_r_1
     * .jpg "Large" file (39MB): https://effigis.com/wp-content/uploads/2015/02/
     * Airbus_Pleiades_50cm_8bit_RGB_Yogyakarta.jpg
     */

    // Usage: putfile <targetURL> <savePath> [-p --parent] [-f --force] [-w --wait
    // N] [-t --timeout N]\n"
    

    public static void main(String[] args) {
        preTest();
        Test test = new Test();
        int i = 1;
        int passed = 0;
        int failed = 0;
        List<String> failedList = new ArrayList<String>();
        for (UnitTest ut : test.unitTestList) {
            System.out.println(String.format("Running test %d of %d...", i, Test.unitTestList.size()));
            PutFile pf = new PutFile();
            pf.entryPoint(ut.args);
            if (pf.argsValid) {
                pf.run();
            }
            if (pf.putFileTaskState != ut.expectedResults) {
                failed++;
                System.out.println(String.format("Test %d FAILED",i));
                System.out.println(String.format("Expected result %d, got result %d\n.",ut.expectedResults, pf.putFileTaskState));
                failedList.add(ut.name);
            }
            else {
                passed++;
                System.out.println(String.format("Test %d PASSED\n",i));
            }
            i++;
        }
        System.out.println(String.format("Final Results:\n\tRan %d test\n\tPASSED: %d\n\tFAILED: %d", Test.unitTestList.size(), passed, failed));
        if (failedList.size() > 0) {
            System.out.println("The following tests failed:");
            for (String name : failedList) {
                System.out.println(name);
            }
        }
        
    }

    private static void preTest() {
        String strPath = "/home/parsons/tmp";
        Path path = FileSystems.getDefault().getPath(strPath);
        try {
            Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException e) {
            System.out.println(String.format("Unable to clean up tmp directory before tests: ",e));            
        }
        File dir = new File(strPath);
        if (!dir.mkdir()) {
            System.out.println("unable to create directory structure during pre-test.");
        }        
    } 
}