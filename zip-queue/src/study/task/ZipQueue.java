package study.task;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/**
 * This application repacks nested *.zip archives including inner *.gz archives according the rules specified in
 * Java Basics course task. The package consists of two classes:
 * - RefData - reference with constants using in application;
 * - ZipQueue - class for nested archives viewing and repacking.
 * @author Sergey Sokhnyshev
 * Created: 23.05.2016
 */

/** Class for nested archives viewing and repacking */
class ZipQueue {
    private int recursion_level;        //nesting level counter
    private final HashSet<String> mail_list;  //storage of sorted e-mails
    private final HashSet<String> phones;     //storage of sorted phones

    private ZipQueue() {
        recursion_level = 0;
        mail_list = new HashSet<>();
        phones = new HashSet<>();
    }

    /** repacks archive with specified name including nested *.zip and *.gz to another archive according to
     *  Java Basics task statement
     * */
    private void repack_nested_zip(String zip_name, String new_zip_name) throws IOException{
        reset();

        FileInputStream input = new FileInputStream(new File(zip_name));
        FileOutputStream output = new FileOutputStream(new File(new_zip_name));

        //performs on-fly archives repacking
        //invokes recursively
        view_nested(input, output);

        input.close();
        output.close();
    }

    /** clears storages before new archive repacking*/
    private void reset() {
        recursion_level = 0;
        mail_list.clear();
        phones.clear();
    }

    /** recursive method for nested archives repacking */
    private void view_nested(InputStream input, OutputStream output) throws IOException {
        System.out.println(">> Nesting level: " + recursion_level);
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

                //nested *.zip archives processing
                if(in_entry.getName().endsWith(RefInfo.zip_ext)) {
                    //recursive invoking of method if we are in nested archive
                    view_nested(zin, zos);
                //processing *.gz archives
                } else if (in_entry.getName().endsWith(RefInfo.gzip_ext)){
                    GZIPInputStream gz_in = new GZIPInputStream(zin);
                    GZIPOutputStream gz_os = new GZIPOutputStream(zos);

                    parseLines(gz_in, gz_os);
                    //tells to output stream about the end of writing into it but doesn't close this stream
                    gz_os.finish();
                //processing ordinary data files
                } else {
                    parseLines(zin, zos);
                }

                zos.closeEntry();
            }
        }
        //saves unique e-mails and phones if all source archive entries are read
        if(--recursion_level == 0) {
            saveSortedData(RefInfo.sorted_phones_filename, zos, phones);
            saveSortedData(RefInfo.sorted_emails_filename, zos, mail_list);

            zin.close();
            zos.close();
        } else {
            //we can't close output stream due to nested invoking of repacking method
            zos.finish();
        }
    }

    /** parses lines in data files according to task statement, saves modified lines to destination archive,
     * gets unique phone numbers and e-mails and stores its for sorting */
    private void parseLines(InputStream is, OutputStream os) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            //defining is it "suitable" line
            int dog_index = line.indexOf(RefInfo.sep_dog);
            if(dog_index > 0) {
                //separating phone number from e-mails set
                //using regular expression due to lines without spaces between phone number and first e-mail in line
                int mail_beg_index = line.substring(0, dog_index).replaceAll(RefInfo.left_sqbracket +
                                        RefInfo.mail_separators.substring(1) + RefInfo.right_sqbracket,
                                        RefInfo.sep_space).lastIndexOf(RefInfo.sep_space);

                //processing phone number: changing city code if necessary
                int left_br_index = line.indexOf(RefInfo.left_bracket);
                int right_br_index = line.indexOf(RefInfo.right_bracket);

                StringBuilder phone_str = new StringBuilder(line.substring(0, mail_beg_index));

                if(right_br_index > 0 && left_br_index > 0) {
                    //getting substitution code instead of source city code
                    String sub_code = RefInfo.phone_codes_sub.get(line.substring(left_br_index + 1,
                                                                                 right_br_index).trim());
                    if(sub_code != null) {
                        phone_str.replace(left_br_index + 1, right_br_index, sub_code);
                    }
                }

                //saving modified line in stream
                writer.write(phone_str.toString(), 0, phone_str.length());
                writer.write(line, mail_beg_index, line.length() - mail_beg_index);

                //formatting phone number according task statement
                String formatted_phone = phone_str.toString().replaceAll(RefInfo.left_sqbracket +
                                            RefInfo.mail_separators + RefInfo.sep_hyphen + RefInfo.right_sqbracket,
                                            RefInfo.sep_no_sign);

                left_br_index = formatted_phone.indexOf(RefInfo.left_bracket);

                right_br_index = formatted_phone.indexOf(RefInfo.right_bracket);
                right_br_index += 1;    //increasing index due to space insertion in previous line

                //saving unique phone number
                phones.add((new StringBuffer(formatted_phone)).insert(left_br_index,
                            RefInfo.sep_space).insert(right_br_index + 1, RefInfo.sep_space).toString());

                //processing e-mails set in line
                StringTokenizer token_set = new StringTokenizer(line.substring(mail_beg_index), RefInfo.mail_separators);

                while(token_set.hasMoreTokens()) {
                    String mail = token_set.nextToken().trim();
                    if(mail.endsWith(RefInfo.mail_domain)) {
                        mail_list.add(mail);
                    }
                }
            //simply copying line to output data file if it doesn't contain with phones and mails
            } else {
                writer.write(line, 0, line.length());
            }

            //starting new line in processing data file
            writer.newLine();
        }

        writer.flush();
    }

    /** sorts data in specified containter and puts it in file with given name in archive defined by output stream */
    private void saveSortedData(String filename, ZipOutputStream zos, HashSet<String> data) throws IOException {
        zos.putNextEntry(new ZipEntry(filename));

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));

        //sorting data in list
        List<String> sorted = new ArrayList<>(data);
        Collections.sort(sorted);

        //writing data to entry in archive
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

            //checking command line arguments number
            if(args.length < 1) {
                System.out.println("ZipQueue utility for nested *.zip and *.gz archives repacking");
                System.out.println("Usage: java -jar utility_name.jar [path/archive_name.zip] [result_path/]");
                System.out.println("Result: utility puts the repacked archive \"<archive_name>v2.zip\" in user specified folder");
                return;
            }

            //getting source archive name and creating resulting archive name according to specified destination folder
            int dot_index = args[0].lastIndexOf(RefInfo.sep_dot);
            int name_begin = args[0].replace(RefInfo.sep_back_slash, RefInfo.sep_slash).lastIndexOf(RefInfo.sep_slash);

            String repacked_name = args[1];
            repacked_name += (args[1].replace(RefInfo.sep_back_slash, RefInfo.sep_slash).lastIndexOf(RefInfo.sep_slash) !=
                                (args[1].length() - 1)) ? RefInfo.sep_slash : RefInfo.sep_no_sign;
            repacked_name += args[0].substring(((name_begin != -1) ? name_begin + 1 : 0), dot_index);
            repacked_name += RefInfo.name_suffix + RefInfo.zip_ext;

            //starting for archives repacking
            zip_proc.repack_nested_zip(args[0], repacked_name);
        }
        catch (IOException exc){
            System.out.println("Error: " + exc.getMessage());
        }
    }
}
