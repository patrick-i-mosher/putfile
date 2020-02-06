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

public class PutFile implements Runnable{

    public String TOKEN;
    public int putFileTaskState = 0;
    private static final String TAG = "PUTFILE";
    private static final int MAX_TIMEOUT = 120;
    static final int FILESYSTEM_ERROR = 1;
    static final int HTTP_CONNECTION_ERROR = 2;
    static final int FILE_DOWNLOAD_ERROR = 3;
    static final int WRITE_FILE_ERROR = 4;        
    private static boolean createDirs = false;
    private static boolean forceOverwrite = false;
    private static int waitRead = 30000; // 30 seconds
    private static int connTimeout = 30000; // 30 seconds
    private boolean argsValid = true;
    private String urlStr;
    private String filePath;
    
    // DEBUG
    public static void main( String[] args) throws Exception {
        // Usage: putfile <URL> <full path>
        // Small file (450KB):
        // https://homepages.cae.wisc.edu/~ece533/images/airplane.png
        // Medium file (16MB):
        // https://effigis.com/wp-content/uploads/2015/02/DigitalGlobe_WorldView1_50cm_8bit_BW_DRA_Bangkok_Thailand_2009JAN06_8bits_sub_r_1.jpg
        // "Large" file (39MB):
        // https://effigis.com/wp-content/uploads/2015/02/Airbus_Pleiades_50cm_8bit_RGB_Yogyakarta.jpg

        String[] args2 = {
                "https://effigis.com/wp-content/uploads/2015/02/Airbus_Pleiades_50cm_8bit_RGB_Yogyakarta.jpg",
                "/home/parsons/tmp/tmp/tmpb/output2.jpg",      
                "-p",
                "-f",                          
                "-w",
                "5"};
        PutFile putfile = new PutFile();        
        System.exit(putfile.entryPoint(args2));
    }
    //END DEBUG

    public int entryPoint(String[] args) throws NoSuchAlgorithmException {
        parseArgs(args);
        if (!argsValid) {
            return 1;
        }                
        return 0;
    }

@Override
    public void run(){
        // Test to make sure the write path is a valid location
        if (!testPath(filePath)) {
            putFileTaskState = FILESYSTEM_ERROR;
            return;
        }
        // Connect to URL
        HttpURLConnection urlConnection = doHttpConnect(urlStr);
        if (urlConnection == null) {
            putFileTaskState = HTTP_CONNECTION_ERROR;            
            return;
        }
        // Download File
        byte[] dlFile = null;
        try {
            dlFile = downloadFile(urlConnection);
        } catch (NoSuchAlgorithmException e) {
            // Unable to generate MD5, NBD
            Log.w(TAG, String.format("Unable to generate MD5 digest: %s",e));
        }
        if (dlFile == null) {
            putFileTaskState = FILE_DOWNLOAD_ERROR;            
            return;
        }
        // Write file to local storage
        if (!writeFile(filePath, dlFile)) {
            putFileTaskState = WRITE_FILE_ERROR;
            return;
        }
    }

    public void parseArgs(String[] args){
        /*
        String usage = "Usage: putfile <targetURL> <savePath> [-p --parent] [-f --force] [-w --wait N] [-t --timeout N]\n" +
                "\t-p --parent    Create target directory structure if it doesn't exist.  Overrides default behavior.\n" +
                "\t-f --force     Overwrite file if a file of the same name already exists.  Overides default behavior.\n" +
                "\t-w --wait      Time, in seconds, to wait for additional data when connection is interrupted.  Default: 30, max 120.\n" +
                "\t-t --timeout   Time, in seconds, to wait for an initial connection to be established.  Defualt: 30, max 120.\n" +
                "Example: putfile www.website.com/something.png /home/user/Downloads/something.png -p --force -w 45 --timeout 45";
        */        
        urlStr = args[0];
        filePath = args[1];
        int duration = 0;
        if (args.length > 2) {
            for(int i = 2; i < args.length; i++) {
                switch(args[i].charAt(0)) {
                    case '-':
                        if (args[i].length() < 2) {
                            Log.e(TAG, String.format("Invalid argument: %s",args[i])); 
                            argsValid = false;
                            return;                       
                        }
                        switch(args[i].charAt(1)) {
                            case '-':
                                if (args[i].length() < 3) {
                                    Log.e(TAG, String.format("Invalid argument: %s", args[i]));
                                    argsValid = false;
                                    return;
                                }
                                switch(args[i].replaceAll("[^a-zA-Z]", "")){
                                    case "parent":
                                        createDirs = true;
                                        break;
                                    case "force":
                                        forceOverwrite = true;
                                        break;
                                    case "wait":                                    
                                        duration = checkIntArg(args[i + 1]);                                
                                        if (duration == -1) {
                                            argsValid = false;
                                            return;
                                        }
                                        connTimeout = duration * 1000;
                                        i++;
                                        break;
                                    case "timeout":                                    
                                        duration = checkIntArg(args[i + 1]);
                                        if (duration == -1) {
                                            argsValid = false;
                                            return;
                                        }
                                        waitRead = duration * 1000;
                                        i++;
                                        break;
                                    default:
                                        Log.e(TAG, "Unrecognized option provided");
                                        argsValid = false;
                                        return;
                                }
                                break;
                            case 'p':
                                if (args[i].length() > 2) {
                                    Log.e(TAG, String.format("Unrecognized option: %s", args[i]));
                                    argsValid = false;
                                    return;
                                }
                                createDirs = true;
                                break;
                            case 'f':
                            if (args[i].length() > 2) {
                                Log.e(TAG, String.format("Unrecognized option: %s", args[i]));
                                argsValid = false;
                                return;
                            }    
                                forceOverwrite = true;
                                break;
                            case 'w':
                                if (args[i].length() > 2) {
                                    Log.e(TAG, String.format("Unrecognized option: %s", args[i]));
                                    argsValid = false;
                                    return;
                                }
                                duration = checkIntArg(args[i + 1]);                                
                                if (duration == -1) {
                                    argsValid = false;
                                    return;
                                }
                                waitRead = duration * 1000;   
                                i++;                         
                                break;
                            case 't':
                                if (args[i].length() > 2) {
                                    Log.e(TAG, String.format("Unrecognized option: %s", args[i]));
                                    argsValid = false;
                                    return;
                                }
                                duration = checkIntArg(args[i + 1]);                                
                                if (duration == -1) {
                                    argsValid = false;
                                    return;
                                }
                                connTimeout = duration * 1000;                        
                                i++;
                                break;
                            default:
                                Log.e(TAG, String.format("Unrecognized option %s",args[i]));
                                argsValid = false;
                                return;
                        }
                        break;
                    default:
                        Log.e(TAG, String.format("Unrecognized option %s",args[i]));
                        argsValid = false;
                        return;
                        
                }
            }
        }
    }
    private int checkIntArg(String arg){
        int duration = -1;
        try {
            duration = Integer.parseInt(arg);
        }
        catch (NumberFormatException e) {
            Log.e(TAG, String.format("The value %s provided to the option is invalid.", arg + 1));
        }
        if (duration > MAX_TIMEOUT) {
            Log.e(TAG, String.format("The value %d provided to the option exceeds 120 seconds.",duration));            
            duration = -1;            
        }
        return duration;
    }

    public boolean testPath( String path) {
        File tmpDir = new File(path);
        // Check to see if this file already exists
        if (tmpDir.exists()) {
            Log.i(TAG, String.format("%s already exists.", path));
            if (!forceOverwrite) {
                return false;
            }
        }
        // Check to see whether parent directory structure exists
        String dir = tmpDir.getParent();
        File tmpDir2 = new File(dir);
        if (!tmpDir2.exists()) {            
            Log.i(TAG, "Target directory does not exist.");            
            // Try to create target directory
            if (!createDirs) {
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

    public HttpURLConnection doHttpConnect(String urlStr){
        HttpURLConnection urlConnection;
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
            return null;          
        }
        urlConnection.setConnectTimeout(connTimeout);
        urlConnection.setReadTimeout(waitRead);      
        Log.i(TAG, String.format("Successfully connected to %s, proceeding with download", urlStr));
        return urlConnection;
    }

    public byte[] downloadFile(HttpURLConnection urlConnection) throws NoSuchAlgorithmException {
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

    public boolean writeFile(String path, byte[] file){        
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