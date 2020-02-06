package putfile;

//import android.util.Log;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;

public class PutFile {

    private static final String TAG = "PUTFILE";
    private static boolean CREATE_DIRS = false;
    private static boolean FORCE_OVERWRITE = false;
    private static int WAIT_READ = 30000;
    private static int CONN_TIMEOUT = 30000;
    public String TOKEN;

    // DEBUG
    public static void main(String[] args) throws Exception {
        // Usage: putfile <URL> <full path>
        // Small file (450KB):
        // https://homepages.cae.wisc.edu/~ece533/images/airplane.png
        // Medium file (16MB):
        // https://effigis.com/wp-content/uploads/2015/02/DigitalGlobe_WorldView1_50cm_8bit_BW_DRA_Bangkok_Thailand_2009JAN06_8bits_sub_r_1.jpg
        // "Large" file (39MB):
        // https://effigis.com/wp-content/uploads/2015/02/Airbus_Pleiades_50cm_8bit_RGB_Yogyakarta.jpg

        String[] args2 = {
                "https://effigis.com/wp-content/uploads/2015/02/Airbus_Pleiades_50cm_8bit_RGB_Yogyakarta.jpg",
                "/home/parsons/tmp/output2.jpg" };
        System.exit(entryPoint(args2));
    }
    //END DEBUG

    public static int entryPoint(String[] args) throws NoSuchAlgorithmException {
        /*
            TODO: Args
            Usage: putfile [-p --parent] [-f --force] [-w --wait N] [-t --timeout N]
                -p --parent    Create target directory structure if it doesn't exist.  Overrides default behavior.
                -f --force     Overwrite file if a file of the same name already exists.  Overides default behavior.
                -w --wait      Time, in seconds, to wait for additional data when connection is interrupted.  Default: 30
                -t --timeout   Time, in seconds, to wait for an initial connection to be established.  Defualt: 30
        */        
        HttpURLConnection urlConnection = null;
        String urlStr = args[0];
        String filePath = args[1];
        // Test to make sure the write path is a valid location
        if (!testPath(filePath)) {
            return 1;
        }
        // Connect to URL
        urlConnection = doHttpConnect(urlStr, urlConnection);
        if (urlConnection == null) {
            return 1;
        }
        // Set timeout values for connection and for read operation
        // Default 30 seconds
        urlConnection.setConnectTimeout(CONN_TIMEOUT);
        urlConnection.setReadTimeout(WAIT_READ);
        // Download File
        byte[] dlFile = downloadFile(urlConnection);
        if (dlFile == null) {
            return 1;
        }
        // Write file to local storage
        if (!writeFile(filePath, dlFile)) {
            return 1;
        }
        return 0;
    }

    public static boolean testPath(String path) {
        File tmpDir = new File(path);
        // Check to see if this file already exists
        if (tmpDir.exists()) {
            Log.i(TAG, String.format("%s already exists.", path));
            if (!FORCE_OVERWRITE) {
                return false;
            }
        }
        // Check to see whether parent directory structure exists
        String dir = tmpDir.getParent();
        File tmpDir2 = new File(dir);
        if (!tmpDir2.exists()) {            
            Log.i(TAG, "Target directory does not exist.");            
            // Try to create target directory
            if (!CREATE_DIRS) {
                return false;
            }
            if (!tmpDir2.mkdirs()) {
                Log.e(TAG, String.format("Unable to creat directory %s", dir));
                return false;
            }
            Log.i(TAG, String.format("Created directory at %s", dir));
        }                        
        // make sure we can write to the target directory        
        if (!tmpDir2.canWrite()) {
            Log.e(TAG, String.format("Insufficient write priveleges to %s", dir));
            return false;
        }
        Log.i(TAG, String.format("%s is a viable path.", path));
        return true;
    }

    public static HttpURLConnection doHttpConnect(String urlStr, HttpURLConnection urlConnection){
        URL url = null;                
        try {
            url = new URL(urlStr);
        }
        catch (MalformedURLException e) {
            Log.e(TAG, String.format("Invalid URL provided: %s", e));                        
        }
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
        }
        catch (IOException e) {
            Log.e(TAG, String.format("Unable to establish connection to %s: %s", urlStr, e));            
        }
        Log.i(TAG, String.format("Successfully connected to %s, proceeding with download", urlStr));
        return urlConnection;
    }

    public static byte[] downloadFile(HttpURLConnection urlConnection) throws NoSuchAlgorithmException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        InputStream in = null;
        int nRead = 0;            
        int totalRead = 0;
        byte[] data = new byte[4096];
        try { 
            in = new BufferedInputStream(urlConnection.getInputStream());
        }
        catch (IOException e) {
            Log.e(TAG, String.format("Error getting input stream: %s",e));
            return null;
        }        
        // DEBUG
        /*
        long startTime = System.currentTimeMillis();
        */
        // END DEBUG
        while (nRead != -1) {
            try {
                nRead = in.read(data, 0, data.length);
            }
            catch(IOException e) {
                Log.e(TAG, String.format("Error reading from input stream: %s",e));
                return null;
            }
            if(nRead == -1) {
                break;
            }
            buffer.write(data, 0, nRead);  
            totalRead += nRead;
            /*
            // DEBUG
            long currentTime = System.currentTimeMillis();
            double elapsedSeconds = (currentTime - startTime) / 1000;
            double bps = totalRead / elapsedSeconds;
            System.out.println(String.format("Received %d bytes at %.2f MBps", totalRead, bps / 1048576));
            // END DEBUG 
            */
        }
        double downloaded = (double) totalRead / 1048576;
        Log.i(TAG, String.format("Retreived %.2f MB from target", downloaded));
        // Calculate MD5 hash of the file.  This might not be necessary but I was bored.
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data);
        byte[] digest = md.digest();
        Log.i(TAG, String.format("File MD5 hash is %s", DatatypeConverter.printHexBinary(digest)));
        return buffer.toByteArray();
    }

    public static boolean writeFile(String path, byte[] file){        
        FileOutputStream out = null;
        boolean success = true;
        try {
            out = new FileOutputStream(path);
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, String.format("Unable to write file: %s", e));            
            success = false;
        }
        try {      
            out.write(file);
        }
        catch (IOException e) {
            Log.e(TAG, String.format("Unable to write file: %s", e));
            success = false;
        }        
        try {
            out.close();
        }
        catch (IOException e) {
            Log.w(TAG, "Unable to close file descriptor after write operation.");
        }
        Log.i(TAG, String.format("Wrote %.2f MB to %s", (float) file.length / 1048576, path));
        return success;
    }      
}

/*
 * public int entrypoint(String[] args)
 * public void parseArgs(String [] args)
 * public void run 
 * public String TOKEN --> stores token for payload execution instance 
 */