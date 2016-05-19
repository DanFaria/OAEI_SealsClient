package eu.sealsproject.omt.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class Helper {

    public static void copyFiles(File src, File dest) throws IOException {
        //Check to ensure that the source is valid...
        if (!src.exists()) {
            throw new IOException("copyFiles: Can not find source: " + src.getAbsolutePath() + ".");
        } else if (!src.canRead()) { //check to ensure we have rights to the source...
            throw new IOException("copyFiles: No right to source: " + src.getAbsolutePath() + ".");
        }
        //is this a directory copy?
        if (src.isDirectory()) {
            if (!dest.exists()) { //does the destination already exist?
                //if not we need to make it exist if possible (note this is mkdirs not mkdir)
                if (!dest.mkdirs()) {
                    throw new IOException("copyFiles: Could not create direcotry: " + dest.getAbsolutePath() + ".");
                }
            }
            //get a listing of files...
            String list[] = src.list();
            //copy all the files in the list.
            for (int i = 0; i < list.length; i++) {
                File dest1 = new File(dest, list[i]);
                File src1 = new File(src, list[i]);
                copyFiles(src1, dest1);
            }
        } else {
            //This was not a directory, so lets just copy the file
            FileInputStream fin = null;
            FileOutputStream fout = null;
            byte[] buffer = new byte[4096]; //Buffer 4K at a time (you can change this).
            int bytesRead;
            try {
                //open the files for input and output
                fin = new FileInputStream(src);
                fout = new FileOutputStream(dest);
                //while bytesRead indicates a successful read, lets write...
                while ((bytesRead = fin.read(buffer)) >= 0) {
                    fout.write(buffer, 0, bytesRead);
                }

                dest.setExecutable(true);

            } catch (IOException e) { //Error copying file... 
                IOException wrapper = new IOException("copyFiles: Unable to copy file: "
                        + src.getAbsolutePath() + "to" + dest.getAbsolutePath() + ".");
                wrapper.initCause(e);
                wrapper.setStackTrace(e.getStackTrace());
                throw wrapper;
            } finally { //Ensure that the files are closed (if they were open).
                if (fin != null) {
                    fin.close();
                }
                if (fout != null) {
                    fout.close();
                }
            }
        }
    }

    public static void stopProgram(String message) {
        KeyboardInput key = new KeyboardInput();
        System.out.print(message);
        String answer = key.readString();
        if (!(answer.startsWith("y"))) {
            System.out.println("Program interrupted!");
            System.exit(0);
        }
    }

    static public void deleteDirectory(File path, int depth) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i], depth + 1);
                } else {
                    files[i].delete();
                }
            }
        }
        if (depth > 0) {
            path.delete();
        }
    }

    public static String deployPackage(String packageLocation) {
        Map<String, String> env = System.getenv();
        String sealsHome = env.get("SEALS_HOME");
        if (sealsHome != null) {
            File sealsHomeDir = new File(env.get("SEALS_HOME"));
            File workingDir = new File(System.getProperty("user.dir"));
            if (!sealsHomeDir.equals(workingDir)) {
                System.out.println(">>> You have to change your current directory to SEALS_HOME!");
                System.out.println(">>> You are in: " + workingDir.getAbsolutePath() + ".");
                System.out.println(">>> Please go to: " + sealsHomeDir.getAbsolutePath() + ".");
                System.exit(1);
            }

            System.out.println(">>> Preparing environment ...");
            File from1 = new File(packageLocation + System.getProperty("file.separator") + "lib");
            try {
                copyFiles(from1, sealsHomeDir);
                File from2 = new File(packageLocation + System.getProperty("file.separator") + "conf");
                copyFiles(from2, sealsHomeDir);
            } catch (IOException e) {
                System.out.println(">>> Could not copy required files to " + sealsHomeDir.getAbsolutePath() + " check permissions");
                System.out.println(">>> Caught Exception: " + e);

                System.exit(1);
            }



        } else {
            System.out.println(">>> You have define the system variable SEALS_HOME first!");
            System.out.println(">>> Let it point to an empty directory where you have read,");
            System.out.println(">>> write and execution rights.");
            System.exit(1);
        }
        return sealsHome;

    }
}
