package task.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/**
 * This application repacks nested *.zip archives including inner *.gz archives according the rules specified in
 * Java Basics course task. The application consists of two classes:
 * - RefData - reference with constants using in application;
 * - ZipQueue - class for nested archives viewing and repacking.
 * @author Sergey Sokhnyshev
 * Created: 23.05.2016
 */

class RefInfo {
    private RefInfo() {
        throw new AssertionError();
    }

    public static final String zip_ext;
    public static final String gzip_ext;
    public static final String sorted_phones_filename;
    public static final String sorted_emails_filename;
    public static final String mail_domain;
    public static final String mail_separators;

    public static final String sep_dog;
    public static final String sep_hyphen;
    public static final String sep_space;
    public static final String sep_nosign;

    public static final String left_bracket;
    public static final String right_bracket;


    public static final Hashtable<String, String> phone_codes_sub;

    //static fields initialization
    static {
        zip_ext = ".zip";
        gzip_ext = ".gz";
        sorted_phones_filename = "phones.txt";
        sorted_emails_filename = "emails.txt";
        mail_domain = ".org";
        mail_separators = " \t,;";

        sep_dog = "@";
        sep_hyphen = "-";
        sep_space = " ";
        sep_nosign = "";

        left_bracket = "(";
        right_bracket = ")";

        phone_codes_sub = new Hashtable<String, String>();

        phone_codes_sub.put("101", "401");
        phone_codes_sub.put("202", "802");
        phone_codes_sub.put("301", "321");
    }
}

class ZipQueue {
    private int RecursionLevel = 0;      //нельзя делать static!
    private HashSet<String> mail_list = new HashSet<String>();
    private HashSet<String> phones = new HashSet<String>();


    private void repack_nested_zip(String zip_name, String new_zip_name) throws IOException{
        FileInputStream input = new FileInputStream(new File(zip_name));
        FileOutputStream output = new FileOutputStream(new File(new_zip_name));

        view_nested(input, output);

        input.close();
        output.close();
    }

    private void view_nested(InputStream input, OutputStream output) throws IOException {
        System.out.println("Recursion level: " + RecursionLevel);
        ++RecursionLevel;

        ZipInputStream zin = new ZipInputStream(input);
        ZipOutputStream zos = new ZipOutputStream(output);

        ZipEntry in_entry;

        while((in_entry = zin.getNextEntry()) != null) {
            if(in_entry.isDirectory()) {
                System.out.println("Repacked directory: " + in_entry.getName());

                zos.putNextEntry(new ZipEntry(in_entry.getName()));
            } else {
                System.out.println("Repacking file: " +  in_entry.getName());
                zos.putNextEntry(new ZipEntry(in_entry.getName()));

                if(in_entry.getName().endsWith(RefInfo.zip_ext)) {
                    view_nested(zin, zos);
                } else if (in_entry.getName().endsWith(RefInfo.gzip_ext)){
                    GZIPInputStream gzin = new GZIPInputStream(zin);
                    GZIPOutputStream gzos = new GZIPOutputStream(zos);

                    parseLines(gzin, gzos);
                    gzos.finish();

                } else {
                    parseLines(zin, zos);
                }

                zos.closeEntry();
            }
        }

        if(--RecursionLevel == 0) {
            saveSortedData(RefInfo.sorted_phones_filename, zos, phones);
            saveSortedData(RefInfo.sorted_emails_filename, zos, mail_list);

            zin.close();
            zos.close();
        } else {
            zos.finish();
        }
    }

    private void parseLines(InputStream is, OutputStream os) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            //System.out.println(line);
            //ищем почтовую "собаку"
            int dog_index = line.indexOf(RefInfo.sep_dog);
            if(dog_index > 0) {
                int mail_beg_index = line.lastIndexOf(RefInfo.sep_space, dog_index);      //разделителем может быть Tab, здесь лучше цифру поискать?

//                System.out.println("Phone: " + line.substring(0, mail_beg_index));

                //работаем с телефоном
                int left_br_index = line.indexOf(RefInfo.left_bracket);
                int right_br_index = line.indexOf(RefInfo.right_bracket);

                StringBuffer phone_str = new StringBuffer(line.substring(0, mail_beg_index));

                if(right_br_index > 0 && left_br_index > 0) {

                    String sub_code = RefInfo.phone_codes_sub.get(line.substring(left_br_index + 1, right_br_index).trim());
                    if(sub_code != null) {
                        phone_str.replace(left_br_index + 1, right_br_index, sub_code);
                    }
                }

                //сохраняем телефон и список e-mail в потоке
                writer.write(phone_str.toString(), 0, phone_str.length());
                writer.write(line, mail_beg_index, line.length() - mail_beg_index);

                //форматируем строку с телефоном
                String formatted_phone = phone_str.toString().replace(RefInfo.sep_space,
                                                                      RefInfo.sep_nosign).replace(RefInfo.sep_hyphen,
                                                                                                  RefInfo.sep_nosign);

                left_br_index = formatted_phone.indexOf(RefInfo.left_bracket);

                right_br_index = formatted_phone.indexOf(RefInfo.right_bracket);
                right_br_index += 1;    //потому что при вставке пробела перед левой скобкой размер индексов символов справа увеличится на 1

//                System.out.println("Formatted phone: " + (new StringBuffer(formatted_phone)).insert(left_br_index,
//                                    RefInfo.sep_space).insert(right_br_index + 1, RefInfo.sep_space));
                phones.add((new StringBuffer(formatted_phone)).insert(left_br_index,
                            RefInfo.sep_space).insert(right_br_index + 1, RefInfo.sep_space).toString());

                //работаем с почтой
                StringTokenizer stok = new StringTokenizer(line.substring(mail_beg_index), RefInfo.mail_separators);

                while(stok.hasMoreTokens()) {
                    String mail = stok.nextToken().trim();
                    if(mail.endsWith(RefInfo.mail_domain)) {
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

    private void saveSortedData(String filename, ZipOutputStream zos, HashSet<String> data) throws IOException {
        zos.putNextEntry(new ZipEntry(filename));

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));

        List<String> sorted = new ArrayList<String>(data);
        Collections.sort(sorted);

        System.out.println("File: " + filename + " Sorted list: ");
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
            ZipQueue zip_proc = new ZipQueue();
            zip_proc.repack_nested_zip("../../files/inputs.zip", "../../files/repacked.zip");
        }
        catch (IOException exc){
            //exc.printStackTrace();
            System.out.println("Error: " + exc.getMessage());
        }
    }
}
