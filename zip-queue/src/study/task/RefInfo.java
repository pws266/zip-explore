package study.task;

import java.util.HashMap;

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

    static final int cmdLineArgsNumber = 2;    //correct arguments number in command line

    static final String zipFileExtension = ".zip";    //extension of *.zip file
    static final String gzipFileExtension = ".gz";    //extension of *.gzip file
    static final String sortedPhonesFileName = "phones.txt";    //file name for unique phones storage
    static final String sortedEmailsFileName = "emails.txt";    //file name for unique e-mails storage
    static final String mailDomain = ".org";         //mail domain (org) for unique e-mails saving
    static final String mailSeparators = " \t,;";    //set of separators between e-mail entries in line

    static final String separatorAt = "@";           //the at sign in e-mail
    static final String separatorSpace = " ";        //space sign
    static final String separatorNoSign = "";        //empty string for symbol removing from string
    static final String separatorDot = ".";          //point sign

    static final String leftBracket = "(";    //left round bracket sign
    static final String rightBracket = ")";   //right round bracket sign

    //regular expression for replacing separators at the phone number end to spaces
    static final String regexpSeparatorsToSpace = "[" + mailSeparators.substring(1) + "]";
    //regular expression for removing all separators in phone number
    static final String regexpSeparatorsToNoSign = "[" + mailSeparators + "-]";

    static final String repackedFileNameSuffix = "v2";      //suffix for destination archive name

    //table with original phone codes and its substitutions
    //NOTE! Using "double braces" initialization via anonymous class and its static initialization block
    static final HashMap<String, String> phoneCodesSubstitution = new HashMap<String, String>() {{
        put("101", "401");
        put("202", "802");
        put("301", "321");
    }};
}
