package task.example;

import java.io.*;
import java.net.URI;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.zip.*;

/**
 * Created by newbie on 23.05.16.
 */
public class ZipQueue {
    private static int RecursionLevel = 0;      //нельзя делать static!

    public static void pack(File pack_dir, String zip_name) throws IOException{
        URI base = pack_dir.toURI();
        Deque<File> queue = new LinkedList<File>();

        queue.push(pack_dir);

        OutputStream out = new FileOutputStream(new File(zip_name));
        Closeable res = out;

        try{
            ZipOutputStream zout = new ZipOutputStream(out);
            res = zout;

            while (!queue.isEmpty()) {
                pack_dir = queue.pop();

                for (File child : pack_dir.listFiles()) {
                    String name = base.relativize(child.toURI()).getPath();
                    System.out.println("name: " + name);

                    if(child.isDirectory()) {
                        queue.push(child);
                        name = name.endsWith("/") ? name : name + "/";
                        zout.putNextEntry(new ZipEntry(name));
                    }
                    else {
                        zout.putNextEntry(new ZipEntry(name));
                        InputStream in = new FileInputStream(child);

                        try {
                            byte[] buffer = new byte[1024];

                            while(true) {
                                int readCount = in.read(buffer);

                                if(readCount < 0) {
                                    break;
                                }
                                zout.write(buffer, 0, readCount);
                            }

                        } finally {
                            in.close();
                        }

                        zout.closeEntry();
                    }
                }
            }
        }
        finally{
            res.close();
        }
    }

    public static void unpack(String zip_name, String unpack_dir) throws IOException {
        ZipFile zip = new ZipFile(zip_name);
        Enumeration entries = zip.entries();
        LinkedList<ZipEntry> zfiles = new LinkedList<ZipEntry>();

        while (entries.hasMoreElements()){
            ZipEntry entry = (ZipEntry) entries.nextElement();

            if (entry.isDirectory()) {
                new File(unpack_dir + "/" + entry.getName()).mkdir();
            }
            else{
                zfiles.add(entry);
            }
        }

        for (ZipEntry entry : zfiles) {
            InputStream in = zip.getInputStream(entry);
            OutputStream out = new FileOutputStream(unpack_dir + "/" + entry.getName());

            byte[] buffer = new byte[1024];
            int len;

            while ((len = in.read(buffer)) >= 0){
                out.write(buffer, 0, len);
            }

            in.close();
            out.close();
        }

        zip.close();
    }

    public static void repack(String zip_name, String new_zip_name) throws IOException {
        ZipFile zip = new ZipFile(zip_name);
        Enumeration entries = zip.entries();
        LinkedList<ZipEntry> zfiles = new LinkedList<ZipEntry>();

        FileOutputStream zfos = new FileOutputStream(new File(new_zip_name));
        Closeable res = zfos;

        try {
            ZipOutputStream zos = new ZipOutputStream(zfos);
            res = zos;

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                if (entry.isDirectory()) {
                    System.out.println("Added directory: " + entry.getName());
                    System.out.println("Extra: " + entry.getExtra());

                    zos.putNextEntry(new ZipEntry(entry.getName()));
                    zos.closeEntry();
                    //new File(unpack_dir + "/" + entry.getName()).mkdir();
                } else {
                    zfiles.add(entry);
                }
            }

            for (ZipEntry entry : zfiles) {
                System.out.println("Added file: " + entry.getName());
                zos.putNextEntry(new ZipEntry(entry.getName()));

                InputStream in = zip.getInputStream(entry);
                //OutputStream out = new FileOutputStream(unpack_dir + "/" + entry.getName());

                try {
                    byte[] buffer = new byte[1024];

                    while (true) {
                        int readCount = in.read(buffer);

                        if (readCount < 0) {
                            break;
                        }
                        zos.write(buffer, 0, readCount);
                    }

                } finally {
                    in.close();
                }

                zos.closeEntry();

            }
        } finally {
            res.close();
            zip.close();
        }
    }

    public static void repack_nested_zip(String zip_name, String new_zip_name) throws IOException{
        FileInputStream input = new FileInputStream(new File(zip_name));
        FileOutputStream output = new FileOutputStream(new File(new_zip_name));

        view_nested(input, output);

        input.close();
        output.close();
    }

    public static void view_nested(InputStream input, OutputStream output) throws IOException {
        System.out.println("Recursion level: " + RecursionLevel);
        ++RecursionLevel;

        Closeable res_in = input, res_out = output;
//        try {
            ZipInputStream zin = new ZipInputStream(input);
            res_in = zin;

            ZipOutputStream zos = new ZipOutputStream(output);
            if(RecursionLevel == 0) {
                zos.setMethod(ZipOutputStream.DEFLATED);
            }
            res_out = zos;

            ZipEntry in_entry = null;

            while((in_entry = zin.getNextEntry()) != null ) {
                if(in_entry.isDirectory()) {
                    System.out.println("Repacked directory: " + in_entry.getName());

                    zos.putNextEntry(new ZipEntry(in_entry.getName()));
                } else {
                    System.out.println("Repacking file: " +  in_entry.getName());
                    zos.putNextEntry(new ZipEntry(in_entry.getName()));

                    if(in_entry.getName().endsWith(".zip")) {
                        view_nested(zin, zos);
                    } else if (in_entry.getName().endsWith(".gz")){
                        GZIPInputStream gzin = new GZIPInputStream(zin);
                        GZIPOutputStream gzos = new GZIPOutputStream(zos);

                        copyContent(gzin, gzos);
/*
                        byte[] buffer = new byte[1024];

                        while (true) {
                            int readCount = gzin.read(buffer, 0, 1024);

                            if (readCount < 0) {
                                break;
                            }
                            gzos.write(buffer, 0, readCount);
                        }
*/
                        gzos.finish();

                    } else {
                        copyContent(zin, zos);
/*
                        byte[] buffer = new byte[1024];

                        while (true) {
                            int readCount = zin.read(buffer, 0, 1024);

                            if (readCount < 0) {
                                break;
                            }
                            zos.write(buffer, 0, readCount);
                        }
*/
                    }

                    zos.closeEntry();
                }
            }

            if(--RecursionLevel == 0) {
                zin.close();
                zos.close();
            } else {
                zos.finish();
            }


//        } finally {
//            if(--RecursionLevel == 0) {
//                res_in.close();
//                res_out.close();
//            }
//        }
    }

    public static void copyContent(InputStream is, OutputStream os) throws IOException{
        byte[] buffer = new byte[1024];

        while (true) {
            int readCount = is.read(buffer, 0, 1024);

            if (readCount < 0) {
                break;
            }
            os.write(buffer, 0, readCount);
        }
    }

    public static void parseLines(InputStream is, OutputStream os) {


    }

    public static void main(String[] args) {
        try {
            repack_nested_zip("../../files/inputs.zip", "../../files/repacked.zip");

//            repack("../../files/inputs.zip", "../../files/repacked.zip");

//            unpack("../../files/inputs.zip", "../../files/unpack/");
//            File dir_to_zip = new File("../../files/unpack/inputs");
//            pack(dir_to_zip, "../../files/new_inputs.zip");
        }
        catch (IOException exc){
            //можно или так, или так
            exc.printStackTrace();
            System.out.println("Error: " + exc.getMessage());
        }
    }
}
