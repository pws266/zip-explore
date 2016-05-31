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
    public static final String sep_no_sign;
    public static final String sep_dot;
    public static final String sep_slash;
    public static final String sep_back_slash;


    public static final String left_bracket;
    public static final String right_bracket;
    public static final String left_sqbracket;
    public static final String right_sqbracket;



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
        sep_no_sign = "";
        sep_dot = ".";
        sep_slash = "/";
        sep_back_slash = "\\";

        left_bracket = "(";
        right_bracket = ")";

        left_sqbracket = "[";
        right_sqbracket = "]";

        phone_codes_sub = new Hashtable<>();

        phone_codes_sub.put("101", "401");
        phone_codes_sub.put("202", "802");
        phone_codes_sub.put("301", "321");
    }
}

class ZipQueue {
    private int recursion_level;      //нельзя делать static!
    private HashSet<String> mail_list;
    private HashSet<String> phones;

    public ZipQueue() {
        recursion_level = 0;
        mail_list = new HashSet<>();
        phones = new HashSet<>();
    }

    public void repack_nested_zip(String zip_name, String new_zip_name) throws IOException{
        reset();

        FileInputStream input = new FileInputStream(new File(zip_name));
        FileOutputStream output = new FileOutputStream(new File(new_zip_name));

        view_nested(input, output);

        input.close();
        output.close();
    }

    private void reset() {
        recursion_level = 0;
        mail_list.clear();
        phones.clear();
    }

    private void view_nested(InputStream input, OutputStream output) throws IOException {
        System.out.println("Recursion level: " + recursion_level);
        ++recursion_level;

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
                    GZIPInputStream gz_in = new GZIPInputStream(zin);
                    GZIPOutputStream gz_os = new GZIPOutputStream(zos);

                    parseLines(gz_in, gz_os);
                    gz_os.finish();

                } else {
                    parseLines(zin, zos);
                }

                zos.closeEntry();
            }
        }

        if(--recursion_level == 0) {
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
                //тут все хуже
                int mail_beg_index = line.substring(0, dog_index).replaceAll(RefInfo.left_sqbracket +
                                        RefInfo.mail_separators.substring(1) + RefInfo.right_sqbracket,
                                        RefInfo.sep_space).lastIndexOf(RefInfo.sep_space);
//                int mail_beg_index = line.lastIndexOf(RefInfo.sep_space, dog_index);      //разделителем может быть Tab, здесь лучше цифру поискать?

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
                String formatted_phone = phone_str.toString().replaceAll(RefInfo.left_sqbracket +
                                            RefInfo.mail_separators + RefInfo.sep_hyphen + RefInfo.right_sqbracket,
                                            RefInfo.sep_no_sign);
//                String formatted_phone = phone_str.toString().replace(RefInfo.sep_space,
//                                                                      RefInfo.sep_no_sign).replace(RefInfo.sep_hyphen,
//                                                                                                  RefInfo.sep_no_sign);

                left_br_index = formatted_phone.indexOf(RefInfo.left_bracket);

                right_br_index = formatted_phone.indexOf(RefInfo.right_bracket);
                right_br_index += 1;    //потому что при вставке пробела перед левой скобкой размер индексов символов справа увеличится на 1

//                System.out.println("Formatted phone: " + (new StringBuffer(formatted_phone)).insert(left_br_index,
//                                    RefInfo.sep_space).insert(right_br_index + 1, RefInfo.sep_space));
                phones.add((new StringBuffer(formatted_phone)).insert(left_br_index,
                            RefInfo.sep_space).insert(right_br_index + 1, RefInfo.sep_space).toString());

                //работаем с почтой
                StringTokenizer token_set = new StringTokenizer(line.substring(mail_beg_index), RefInfo.mail_separators);

                while(token_set.hasMoreTokens()) {
                    String mail = token_set.nextToken().trim();
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

        List<String> sorted = new ArrayList<>(data);
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

            if(args.length < 1) {
                System.out.println("ZipQueue utility for nested *.zip and *.gz archives repacking");
                System.out.println("Usage: java -jar utility_name.jar [path/archive.zip] [result_path/]");
                System.out.println("Result: utility puts the repacked archive \"archivev2.zip\" in user specified folder");
                return;
            }

            int dot_index = args[0].lastIndexOf(RefInfo.sep_dot);
            int name_begin = args[0].replace(RefInfo.sep_back_slash, RefInfo.sep_slash).lastIndexOf(RefInfo.sep_slash);

            String repacked_name = args[1];
            repacked_name += (args[1].replace(RefInfo.sep_back_slash, RefInfo.sep_slash).lastIndexOf(RefInfo.sep_slash) !=
                                (args[1].length() - 1)) ? RefInfo.sep_slash : RefInfo.sep_no_sign;
            repacked_name += args[0].substring(((name_begin != -1) ? name_begin + 1 : 0), dot_index) + "v2" + RefInfo.zip_ext;

            //zip_proc.repack_nested_zip("../../files/inputs.zip", "../../files/repacked.zip");
            zip_proc.repack_nested_zip(args[0], repacked_name);
        }
        catch (IOException exc){
            //exc.printStackTrace();
            System.out.println("Error: " + exc.getMessage());
        }
    }
}
