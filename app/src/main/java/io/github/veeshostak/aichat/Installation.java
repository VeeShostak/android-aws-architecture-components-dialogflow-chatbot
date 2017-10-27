package io.github.veeshostak.aichat;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by vladshostak on 8/22/17.
 */

public class Installation {
    private static String sID = null;
    private static final String INSTALLATION = "INSTALLATION";

    public synchronized static String id(Context context) {
        if (sID == null) {
            File installation = new File(context.getFilesDir(), INSTALLATION);
            try {
                if (!installation.exists()) {
                    // if file doesnt exist, creat the file, and write to it the ID
                    writeInstallationFile(installation);
                }
                // get the ID from the file
                sID = readInstallationFile(installation);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID;
    }

    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);


        //String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        // returns: 10/23/17 8:22AM
        String currentDateTimeString = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US).format(new Date());

        // format to 102317822AM
        currentDateTimeString = currentDateTimeString.replace("/", "").replace(" ", "").replace(":", "");

        // uuid + datetime
        String id = UUID.randomUUID().toString() + currentDateTimeString;

        out.write(id.getBytes());
        out.close();
    }
}
