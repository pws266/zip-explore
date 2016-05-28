package task.example;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * Created by newbie on 25.05.16.
 */
public class Test {
    public static void main(String[] args) {
        try {
            FileOutputStream f = new FileOutputStream("../../files/test.zip");
            ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(f));
            zip.putNextEntry(new ZipEntry("xml/"));
            zip.closeEntry();

            ZipEntry ent = new ZipEntry("xml/xml/");
            zip.putNextEntry(ent);
            zip.closeEntry();
            zip.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
