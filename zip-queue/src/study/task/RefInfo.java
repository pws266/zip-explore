package study.task;

import java.util.HashMap;

/**
 * This class is a reference for class ZipQueue.
 * It contains string constants for line parsing.
 * @author Sergey Sokhnyshev
 * Created: 31.05.2016
 */

/** Reference with string constants */
public class RefInfo {
    //private constructor for preventing class instances creation
    private RefInfo() {
        throw new AssertionError();
    }

    public static final int CMD_LINE_ARGS_NUMBER = 2;    //correct arguments number in command line

    public static final String ZIP_FILE_EXTENSION = ".zip";    //extension of *.zip file
    public static final String GZIP_FILE_EXTENSION = ".gz";    //extension of *.gzip file
    public static final String SORTED_PHONES_FILE_NAME = "phones.txt";    //file name for unique phones storage
    public static final String SORTED_EMAILS_FILE_NAME = "emails.txt";    //file name for unique e-mails storage
    public static final String MAIL_DOMAIN = ".org";         //mail domain (org) for unique e-mails saving

    public static final String SEPARATOR_AT = "@";           //the at sign in e-mail
    public static final String SEPARATOR_SPACE = " ";        //space sign
    public static final String SEPARATOR_DOT = ".";          //point sign

    public static final String LEFT_BRACKET = "(";    //left round bracket sign
    public static final String RIGHT_BRACKET = ")";   //right round bracket sign

    // regular expression for phone/e-mails line separation
    public static final String REGEXP_LINE = "[\\s;,-]+";
    public static final String REPACKED_FILE_NAME_SUFFIX = "v2";      //suffix for destination archive name

    //table with original phone codes and its substitutions
    //NOTE! Using "double braces" initialization via anonymous class and its static initialization block
    public static final HashMap<String, String> PHONE_CODES_SUBSTITUTION = new HashMap<String, String>() {{
        put("101", "401");
        put("202", "802");
        put("301", "321");
    }};

    // "absolute" path to logging resource properties file
    public static final String LOG_RESOURCE_FILE_PATH = "/res/logging.properties";
    // separator in *.log - file
    public static final String LOG_SEPARATOR =
                               "--------------------------------------------------------------------------";
    // annotation
    public static final String CMD_ANNOTATION = "ZipQueue utility for nested *.zip and *.gz archives repacking\n" +
                               "Usage: \n" +
                               "       java -jar utility_name.jar [path/archive_name.zip] [result_path/]\n" +
                               "         or\n" +
                               "       java -classpath ./[path_to_package_folder] study.task.ZipQueue " +
                               "[path/archive_name.zip] [result_path/]\n" +
                               "Result: utility puts the repacked archive \"<archive_name>v2.zip\" " +
                               "in user specified folder\n";
}
