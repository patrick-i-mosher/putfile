package putfile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;
//import javax.xml.bind.DatatypeConverter;

public class PutFile implements Runnable {

    // This field holds the result of the putFile operation    
    static final int FILESYSTEM_ERROR = 1;
    static final int HTTP_CONNECTION_ERROR = 2;
    static final int FILE_DOWNLOAD_ERROR = 3;    
    static final int INVALID_ARGUMENT_ERROR = 4;
    static final int PUTFILE_SUCCESS = 5;

    public String TOKEN;
    public boolean argsValid = true;
    
    private static final int MAX_TIMEOUT = 120;
    private boolean createDirs = false;
    private boolean forceOverwrite = false;
    private static int waitRead = 30000; // 30 seconds
    private static int connTimeout = 30000; // 30 seconds    
    private List<String[]> targetList = new ArrayList<>(1);
    // Result string is formatted "TargetURL:TargetPath:MD5"
    public List<PutFileTask> resultsList = new ArrayList<>(1);
    public StringBuffer ini = new StringBuffer("[INIT]\n");

    /*
     * usage =
     * "Usage: putfile [-p --parent] [-f --force] [-w --wait N] [-t --timeout N] <targetURL::savePath1> <targetURL::savePath2> ... \n"
     * +
     * "\t-p --parent    Create target directory structure if it doesn't exist.  Overrides default behavior.\n"
     * +
     * "\t-f --force     Overwrite file if a file of the same name already exists.  Overides default behavior.\n"
     * +
     * "\t-w --wait      Time, in seconds, to wait for additional data when connection is interrupted.  Default: 30, max 120.\n"
     * +
     * "\t-t --timeout   Time, in seconds, to wait for an initial connection to be established.  Defualt: 30, max 120.\n"
     * +
     * "Example: putfile -p --force -w 45 --timeout 45 www.website.com/something.png:/home/user/Downloads/something.png"
     * ;
     */

    public int entryPoint(String[] args) {
        String results = String.format("%s\n",parseArgs(args));
        appendToIni(results);
        if (!argsValid) {            
            PutFileTask pft = new PutFileTask("null", "null");
            pft.taskState = INVALID_ARGUMENT_ERROR;
            resultsList.add(pft);
            return 1;
        }        
        return 0;
    }

    @Override
    public void run() {
        appendToIni("[TARGETS]\n#URL=OUTPUT_PATH");
        for (String[] target : targetList) {            
            PutFileTask pft = new PutFileTask(target[0], target[1]);           
            appendToIni(String.format("%s=%s", pft.url, pft.path));
            // Test to make sure the write path is a valid location
            if (!testPath(pft)) {
                pft.taskState = FILESYSTEM_ERROR;
                resultsList.add(pft);            
                continue;
            }
            // Connect to URL            
            try {
                doHttpConnect(pft);
            } catch (IOException e) {
                putError(pft, String.format("Unable to establish connection: %s",e));
                pft.taskState = HTTP_CONNECTION_ERROR;
                resultsList.add(pft);            
                continue;            
            }        
            // Download and Write File
            try {
                downloadFile(pft);
            } catch (NoSuchAlgorithmException e) {
                // Unable to generate MD5, NBD
                putError(pft, String.format("Unable to generate MD5 digest: %s", e));
            } catch (IOException e1) {
                putError(pft, String.format("Error downloading file: %s", e1));
                pft.taskState = FILE_DOWNLOAD_ERROR;                            
            }
            resultsList.add(pft);            
        }
        appendToIni("\n");
        for (PutFileTask result : resultsList) {
            appendToIni(String.format("[%s]",result.url));
            appendToIni("# Path to file on target system");
            appendToIni(String.format("path=%s",result.path));
            appendToIni("# Size in megabytes");
            appendToIni(String.format("size=%.2f",result.size));
            appendToIni("# Status of operation");
            switch(result.taskState) {
                case PUTFILE_SUCCESS:
                    result.result = "SUCCESS";
                    break;
                default:
                    result.result = String.format("FAIL:Code=%d",result.taskState);
            }
            appendToIni(String.format("status=%s",result.result));
            appendToIni("#MD5 hash of target file (after download)");
            appendToIni(String.format("md5=%s",result.md5));            
            int i = 1;
            for (String error : result.errorList) {
                appendToIni(String.format("error%d=\"%s\"\n",i,error));
                i++;
            }
            appendToIni("");      
        }        
    } 

    public String parseArgs(String[] args){    
        
        appendToIni("");    
        appendToIni("[Argument Parsing Errors]");
        if(args.length < 1) {
            argsValid = false;
            return "Invalid number of arguments provided.";
        }        
        int duration = 0;
        for(int i = 0; i < args.length; i++) {
            // Check to see if the current arg is a valid URL:path combination            
            if (args[i].contains("::")) {
                try {
                    String[] targets = args[i].split("::");
                    targetList.add(targets);
                    continue;
                }
                catch (PatternSyntaxException e) {
                    // This exception is fine
                }
            }            
            switch(args[i].charAt(0)) {
                case '-':
                    if (args[i].length() < 2) {                         
                        argsValid = false;                        
                        return String.format("argError=Invalid argument: %s",args[i]);                       
                    }
                    switch(args[i].charAt(1)) {
                        case '-':
                            if (args[i].length() < 3) {                                
                                argsValid = false;
                                return String.format("argError=Invalid argument: %s", args[i]);
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
                                        return String.format("argError=Invalid duration: %s.",args[i+1]);
                                    }
                                    connTimeout = duration * 1000;
                                    i++;
                                    break;
                                case "timeout":                                    
                                    duration = checkIntArg(args[i + 1]);
                                    if (duration == -1) {
                                        argsValid = false;
                                        return String.format("argError=Invalid duration: %s.",args[i+1]);
                                    }
                                    waitRead = duration * 1000;
                                    i++;
                                    break;
                                default:                                
                                    argsValid = false;
                                    return "argError=Unrecognized option provided";
                            }
                            break;
                        case 'p':
                            if (args[i].length() > 2) {                                
                                argsValid = false;
                                return String.format("argError=Unrecognized option: %s", args[i]);
                            }
                            createDirs = true;
                            break;
                        case 'f':
                        if (args[i].length() > 2) {                            
                            argsValid = false;
                            return String.format("argError=Unrecognized option: %s", args[i]);
                        }    
                            forceOverwrite = true;
                            break;
                        case 'w':
                            if (args[i].length() > 2) {                                
                                argsValid = false;
                                return String.format("argError=Unrecognized option: %s", args[i]);
                            }
                            try{
                                duration = checkIntArg(args[i + 1]);                                
                            } 
                            catch (ArrayIndexOutOfBoundsException e) {                                
                                argsValid = false;
                                return String.format("argError=Missing expected argument to -w: %s",e);
                            }
                            if (duration == -1) {
                                argsValid = false;
                                return String.format("argError=Invalid duraction: %s",args[i+1]);
                            }
                            waitRead = duration * 1000;   
                            i++;                         
                            break;
                        case 't':
                            if (args[i].length() > 2) {
                                argsValid = false;
                                return String.format("argError=Unrecognized option: %s", args[i]);
                            }
                            try {
                                duration = checkIntArg(args[i + 1]);                                
                            }
                            catch (ArrayIndexOutOfBoundsException e) {                                
                                argsValid = false;
                                return String.format("argError=Missing expected argument to -t option: %s",e);
                            }
                            if (duration == -1) {
                                argsValid = false;
                                return String.format("argError=Invalid duraction: %s",args[i+1]);
                            }
                            connTimeout = duration * 1000;                        
                            i++;
                            break;
                        default:                        
                            argsValid = false;
                            return String.format("argError=Unrecognized option %s",args[i]);
                    }
                    break;
                default:                    
                    argsValid = false;
                    return String.format("argError=Unrecognized option %s",args[i]);                        
            }
        }
        return "argError=NONE";
    }

    private int checkIntArg(String arg){
        int duration = -1;
        try {
            duration = Integer.parseInt(arg);
        }
        catch (NumberFormatException e) {
            appendToIni(String.format("error=\"The value %s provided to the option is invalid.\"", arg + 1));
        }
        if (duration > MAX_TIMEOUT) {
            appendToIni(String.format("The value %d provided to the option exceeds 120 seconds.",duration));            
            duration = -1;            
        }
        return duration;
    }

    public boolean testPath(PutFileTask pft) {
        File tmpDir = new File(pft.path);
        // Check to see if this file already exists
        if (tmpDir.exists()) {            
            if (!forceOverwrite) {
                return false;
            }
        }
            // Check to see whether parent directory structure exists
            String dir = tmpDir.getParent();
            File tmpDir2 = new File(dir);
            if (!tmpDir2.exists()) {                        
                // Try to create target directory
                if (!createDirs) {
                    return false;
                }
                if (!tmpDir2.mkdirs()) {
                    putError(pft, String.format("Unable to creat directory %s", dir));
                    return false;
                }            
            }                        
        // make sure we can write to the target directory        
        if (!tmpDir2.canWrite()) {
            putError(pft, String.format("Insufficient write priveleges to %s", dir));
            return false;
        }        
        return true;
    }

    public void doHttpConnect(PutFileTask gft) throws MalformedURLException, IOException {        
        URL url = new URL(gft.url);        
        gft.urlConnection = (HttpURLConnection) url.openConnection();        
        gft.urlConnection.setConnectTimeout(connTimeout);
        gft.urlConnection.setReadTimeout(waitRead);              
        return;
    }

    public void downloadFile(PutFileTask pft) throws NoSuchAlgorithmException, IOException {
        FileOutputStream out = new FileOutputStream(pft.path);
        InputStream in = new BufferedInputStream(pft.urlConnection.getInputStream());                        
        int nRead = 0;            
        int totalRead = 0;
        byte[] data = new byte[4096];        
        MessageDigest md = MessageDigest.getInstance("MD5");        
        while (nRead != -1) {
            nRead = in.read(data, 0, data.length);
            if(nRead == -1) {
                break;
            }
            out.write(data, 0, nRead);  
            totalRead += nRead;
            md.update(data, 0, nRead);
        }        
        double downloaded = (double) totalRead / 1048576;        
        pft.size = downloaded;
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02X", b&0xff));
        }
        pft.md5 = sb.toString();
        out.close();
        pft.taskState = PUTFILE_SUCCESS;
        return;
    }
    public void putError(PutFileTask pft, String msg) {
        pft.errorList.add(String.format("ERROR: %tc: %s", System.currentTimeMillis(), msg));
    }

    public void appendToIni(String str) {
        ini.append(String.format("%s\n",str));        
    }

    static class PutFileTask {
        HttpURLConnection urlConnection;
        String url;
        String path;
        String result;        
        String md5;
        double size; // in MB
        int taskState;        
        List<String> errorList = new ArrayList<String>(1);

        public PutFileTask(String urlStr, String pathStr) {
            url = urlStr;
            path = pathStr;
        }        
    }
}
