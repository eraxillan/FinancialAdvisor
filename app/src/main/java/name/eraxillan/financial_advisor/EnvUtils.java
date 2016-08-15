package name.eraxillan.financial_advisor;


import android.os.Environment;

import java.io.File;

public class EnvUtils {
    // Checks if external storage is available for read and write
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    // Checks if external storage is available to at least read
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    static File getSharedDocumentsDirectory() {
        if (!isExternalStorageReadable()) return null;

        // NOTE: DIRECTORY_DOCUMENTS field appears only in API level 19;
        //       our minimal API version is much lower, so we are forced to hardcode directory name
//        File sharedDocsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File sharedDocsDir = new File(Environment.getExternalStorageDirectory() + "/Documents");
        boolean isDirPresent = true;
        if (!sharedDocsDir.exists()) {
            isDirPresent = sharedDocsDir.mkdirs();
        }
        return (isDirPresent ? sharedDocsDir : null);
    }
}
