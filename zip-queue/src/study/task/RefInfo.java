package study.task;

import java.util.Hashtable;

/**
 * This class is a reference for class ZipQueue.
 * It contains string constants for line parsing.
 * @author Sergey Sokhnyshev
 * Created: 31.05.2016
 */

/** Reference with string constants */
class RefInfo {
    //private constructor for preventing class instances creation
    private RefInfo() {
        throw new AssertionError();
    }

    static final String zip_ext;                //extension of *.zip file
    static final String gzip_ext;               //extension of *.gzip file
    static final String sorted_phones_filename; //file name for unique phones storage
    static final String sorted_emails_filename; //file name for unique e-mails storage
    static final String mail_domain;            //mail domain (org) for unique e-mails saving
    static final String mail_separators;        //set of separators between e-mail entries in line

    static final String sep_dog;        //the at sign in e-mail
    static final String sep_hyphen;     //hyphen sign
    static final String sep_space;      //space sign
    static final String sep_no_sign;    //empty string for symbol removing from string
    static final String sep_dot;        //point sign
    static final String sep_slash;      //linux slash in path definition
    static final String sep_back_slash; //windows slash in path definition


    static final String left_bracket;       //left round bracket sign
    static final String right_bracket;      //right round bracket sign
    static final String left_sqbracket;     //left square bracket sign
    static final String right_sqbracket;    //right square bracket sign

    static final String name_suffix;    //suffix for destination archive name

    static final Hashtable<String, String> phone_codes_sub;     //table with original and substitutional phone codes

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

        name_suffix = "v2";

        phone_codes_sub = new Hashtable<>();

        phone_codes_sub.put("101", "401");
        phone_codes_sub.put("202", "802");
        phone_codes_sub.put("301", "321");
    }
}
