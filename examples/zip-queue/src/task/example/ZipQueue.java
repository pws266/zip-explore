package task.example;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/**
 * Created by newbie on 23.05.16.
 */
public class ZipQueue {
    private static int RecursionLevel = 0;      //нельзя делать static!
    private static HashSet<String> mail_list = new HashSet<String>();
    private static HashSet<String> phones = new HashSet<String>();

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

                        //copyContent(gzin, gzos);
                        parseLines(gzin, gzos);
                        gzos.finish();

                    } else {
                        //copyContent(zin, zos);
                        parseLines(zin, zos);
                    }

                    zos.closeEntry();
                }
            }

            if(--RecursionLevel == 0) {
                saveSortedData("phones.txt", zos, phones);
                saveSortedData("emails.txt", zos, mail_list);

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

    public static void copyContent(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];

        while (true) {
            int readCount = is.read(buffer, 0, 1024);

            if (readCount < 0) {
                break;
            }
            os.write(buffer, 0, readCount);
        }
    }

    public static void parseLines(InputStream is, OutputStream os) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            //System.out.println(line);
            //ищем почтовую "собаку"
            int dog_index = line.indexOf("@");
            if(dog_index > 0) {
                int mail_beg_index = line.lastIndexOf(" ", dog_index);      //разделителем может быть Tab, здесь лучше цифру поискать?

                System.out.println("Phone: " + line.substring(0, mail_beg_index));

                //работаем с телефоном
                int left_br_index = line.indexOf("(");
                int right_br_index = line.indexOf(")");

                StringBuffer phone_str = new StringBuffer(line.substring(0, mail_beg_index));

                if(right_br_index > 0 && left_br_index > 0) {
                    int code = (new Integer(line.substring(left_br_index + 1, right_br_index).trim())).intValue();
                    //int code = new Integer(line.substring(left_br_index + 1, right_br_index));

                    System.out.println("Code: " + code);
                    System.out.println("Phone before: " + phone_str);

                    //переделать через перечисления
                    switch(code) {
                        case 101: phone_str.replace(left_br_index + 1, right_br_index, "401"); break;
                        case 202: phone_str.replace(left_br_index + 1, right_br_index, "802"); break;
                        case 301: phone_str.replace(left_br_index + 1, right_br_index, "321"); break;
                    }

                    System.out.println("Phone after: " + phone_str);
                }

                //сохраняем телефон и список e-mail в потоке
                writer.write(phone_str.toString(), 0, phone_str.length());
                writer.write(line, mail_beg_index, line.length() - mail_beg_index);
                //writer.write(line.substring(mail_beg_index), 0, line.substring(mail_beg_index).length());

                //форматируем строку с телефоном
                String formatted_phone = phone_str.toString().replace(" ", "").replace("-", "");

                left_br_index = formatted_phone.indexOf("(");

                right_br_index = formatted_phone.indexOf(")");
                right_br_index += 1;    //потому что при вставке пробела перед левой скобкой размер индексов символов справа увеличится на 1

                System.out.println("Formatted phone: " + (new StringBuffer(formatted_phone)).insert(left_br_index, " ").insert(right_br_index + 1, " "));
                phones.add((new StringBuffer(formatted_phone)).insert(left_br_index, " ").insert(right_br_index + 1, " ").toString());

                //работаем с почтой
                StringTokenizer stok = new StringTokenizer(line.substring(mail_beg_index), " \t,;");

                while(stok.hasMoreTokens()) {
                    String mail = stok.nextToken().trim();
                    if(mail.endsWith(".org")) {
                        //System.out.println("E-mail: " + mail);
                        mail_list.add(mail);
                        //sorted.add(mail);
                    }
                }
            } else {
                writer.write(line, 0, line.length());
            }

            writer.newLine();
        }

        //reader.close();

        writer.flush();
        //writer.close();
    }

    public static void saveSortedData(String filename, ZipOutputStream zos, HashSet<String> data) throws IOException {
        zos.putNextEntry(new ZipEntry(filename));

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));

        List<String> sorted = new ArrayList<String>(data);
        Collections.sort(sorted);

        System.out.println("File: " + filename + "Sorted list: ");
        for(String s : sorted) {
            System.out.println(s);

            writer.write(s, 0, s.length());
            writer.newLine();
        }

        writer.flush();
        zos.closeEntry();
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
