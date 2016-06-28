package study.task;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.zip.*;

import static study.task.RefInfo.*;

/**
 * This application repacks nested *.zip archives including inner *.gz archives according the rules specified in
 * Java Basics course task. The package consists of two classes:
 * - RefInfo - reference with constants using in application;
 * - ZipQueue - class for nested archives viewing and repacking.
 * @author Sergey Sokhnyshev
 * Created: 23.05.2016
 */

/** Class for nested archives viewing and repacking */
public class ZipQueue {
    private static final Logger log = Logger.getLogger(ZipQueue.class.getName());

    private final SortedSet<String> mailList;  // storage of sorted e-mails
    private final SortedSet<String> phones;    // storage of sorted phones


    private ZipQueue() {
        mailList = new TreeSet<>();
        phones = new TreeSet<>();
    }

    /** repacks archive with specified name including nested *.zip and *.gz to another archive according to
     *  Java Basics task statement
     * */
    private void repackZip(String zipFileName, String repackedPath) throws IOException{
        reset();

        // opening source *.zip - file
        File zipFile = new File(zipFileName);

        // forming name for repacked *.zip - file
        StringBuilder repackedNameFormer = new StringBuilder(repackedPath);

        if (!repackedPath.isEmpty() && !repackedPath.endsWith(File.separator)) {
            repackedNameFormer.append(File.separator);
        }

        repackedNameFormer.append(zipFile.getName());
        repackedNameFormer.insert(repackedNameFormer.lastIndexOf(SEPARATOR_DOT), REPACKED_FILE_NAME_SUFFIX);

        try ( FileInputStream input = new FileInputStream(zipFile);
              FileOutputStream output = new FileOutputStream(new File(repackedNameFormer.toString()))
            ){
                //performs on-fly archives repacking
                //invokes recursively
                processNestedZip(input, output, 0);
        } catch (FileNotFoundException exc) {
            log.log(Level.SEVERE, "Error: input/output file isn't found\nDescription: ", exc);
            System.exit(1);
        }
    }

    /** clears mail and phone storages before new archive repacking */
    private void reset() {
        mailList.clear();
        phones.clear();
    }

    /**
     * ZipInputStream wrapper. Provides compatibility with JDK 1.7
     * Prevents unauthorized closing by GZipInputStream.
     */
    private class UnclosedZipInputStream extends InputStream {
        private ZipInputStream zipIn;   // reference to external input stream

        UnclosedZipInputStream(ZipInputStream zipIn) {
            this.zipIn = zipIn;
        }

        @Override
        public int read() throws IOException {
            return zipIn.read();
        }

        // empty method preventing external stream closing
        @Override
        public void close() {}
    }

    /** recursive method for nested archives repacking */
    private void processNestedZip(InputStream input, OutputStream output, int recursionLevel) throws IOException {
        log.info(">> Nesting level: " + recursionLevel);

        ZipInputStream zin = new ZipInputStream(input);
        ZipOutputStream zos = new ZipOutputStream(output);

        ZipEntry inEntry;

        while((inEntry = zin.getNextEntry()) != null) {
            if(inEntry.isDirectory()) {
                log.info("Repacked directory: " + inEntry.getName());

                zos.putNextEntry(new ZipEntry(inEntry.getName()));
            } else {
                log.info("Repacking file: " +  inEntry.getName());

                zos.putNextEntry(new ZipEntry(inEntry.getName()));

                //nested *.zip archives processing
                if(inEntry.getName().endsWith(ZIP_FILE_EXTENSION)) {
                    //recursive invoking of method if we are in nested archive
                    processNestedZip(zin, zos, recursionLevel + 1);
                //processing *.gz archives
                } else if (inEntry.getName().endsWith(GZIP_FILE_EXTENSION)){
                    UnclosedZipInputStream unclosedZipIn = new UnclosedZipInputStream(zin);

                    GZIPInputStream gzIn = new GZIPInputStream(unclosedZipIn);
                    GZIPOutputStream gzOut = new GZIPOutputStream(zos);

                    parseLines(gzIn, gzOut);
                    //tells to output stream about the end of writing into it but doesn't close this stream
                    gzOut.finish();
                //processing ordinary data files
                } else {
                    parseLines(zin, zos);
                }

                zos.closeEntry();
            }
        }
        //saves unique e-mails and phones if all source archive entries are read
        if(recursionLevel == 0) {
            saveSortedData(SORTED_PHONES_FILE_NAME, zos, phones);
            saveSortedData(SORTED_EMAILS_FILE_NAME, zos, mailList);

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
            // it is valid line with phone and e-mails if line contains "@"
            if (line.contains(SEPARATOR_AT)) {
                // getting e-mails and phone without separators
                StringBuilder phoneToSort = new StringBuilder();

                for (String s : line.split(REGEXP_LINE)) {
                    if (!s.contains(SEPARATOR_AT)) {
                        phoneToSort.append(s);
                        continue;
                    }

                    // saving e-mails with *.org domain
                    if (s.endsWith(MAIL_DOMAIN)) {
                        mailList.add(s);
                    }
                }

                // getting bracket positions in trimmed and original phone
                int lbToSortIndex = phoneToSort.indexOf(LEFT_BRACKET);
                int rbToSortIndex = phoneToSort.indexOf(RIGHT_BRACKET);

                int lbIndex = line.indexOf(LEFT_BRACKET) + 1;
                int rbIndex = line.indexOf(RIGHT_BRACKET);

                //processing phone number: changing city code if necessary
                //getting substitution code instead of source city code
                String subCode = PHONE_CODES_SUBSTITUTION.get(line.substring(lbIndex, rbIndex).trim());

                if(subCode != null) {
                    phoneToSort.replace(lbToSortIndex + 1, rbToSortIndex, subCode);

                    writer.write(line, 0, lbIndex);
                    writer.write(subCode);
                    writer.write(line, rbIndex, line.length() - rbIndex);
                }
                else {
                    writer.write(line);
                }

                phoneToSort.insert(lbToSortIndex, SEPARATOR_SPACE).insert(rbToSortIndex + 2, SEPARATOR_SPACE);
                phones.add(phoneToSort.toString());
            }
            else {
                // copying line if it doesn't contain phones and e-mails or no substitution phone code was found
                writer.write(line);
            }

            writer.newLine();
        }

        writer.flush();
    }

    /** sorts data in specified containter and puts it in file with given name in archive defined by output stream */
    private void saveSortedData(String filename, ZipOutputStream zos, SortedSet<String> data) throws IOException {
        zos.putNextEntry(new ZipEntry(filename));

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));

        //writing data to entry in archive
        log.info("File: " + filename + " Sorted list: ");

        for(String s : data) {
            log.info(s);
            writer.write(s);
            writer.newLine();
        }

        writer.flush();
        zos.closeEntry();
    }

    /**
     * Switches on logging and tries to create folder for *.log - files
     */
    private static void createLoggingFolder() {
        // switching on logging
        // resource file should be in the same folder with package folders. Using "absolute" path
        try {
            LogManager.getLogManager().readConfiguration(
                    study.task.ZipQueue.class.getResourceAsStream(LOG_RESOURCE_FILE_PATH));
        } catch (IOException exc) {
            log.log(Level.SEVERE, "Error: unable to read logging configuration file", exc);
            System.exit(1);
        }

        // creating folder for logging
        try {
            if (log.getParent().getLevel() != Level.OFF) {
                //getting name of log folder
                Properties logTraits = new Properties();
                logTraits.load(study.task.ZipQueue.class.getResourceAsStream(LOG_RESOURCE_FILE_PATH));

                String logPattern = logTraits.getProperty("java.util.logging.FileHandler.pattern");

                if (logPattern != null) {
                    String logPath = new File(logPattern).getParent();

                    File logFolder = new File(logPath);

                    if (!logFolder.exists() && !logFolder.mkdirs()) {
                        throw new Exception("Unable to create folder for *.log files specified in resources");
                    }
                }
            }
        } catch (IOException exc) {
            log.log(Level.SEVERE, "Error: unable to read/load logging configuration file", exc);
            System.exit(1);
        } catch (Exception exc) {
            log.log(Level.SEVERE, "Error: no log folder was created: ", exc);
            System.exit(1);
        }
    }

    /**
     * Logging information about JRE and OS in
     */

    private static void logSystemInfo() {
        // saving information to log about OS and JRE
        log.info(LOG_SEPARATOR);

        log.info("OS:\n - name: " + System.getProperty("os.name") + "\n - platform: " +
                System.getProperty("os.arch") + "\n - version: " + System.getProperty("os.version") + "\n");

        log.info("JRE:\n - vendor: " + System.getProperty("java.specification.vendor") + "\n - name: " +
                System.getProperty("java.specification.name") + "\n - version: " +
                System.getProperty("java.specification.version") + "\n");

        log.info(LOG_SEPARATOR);
    }

    public static void main(String[] args) {
        // getting program start time
        long startTime = System.currentTimeMillis();

        //checking command line arguments number
        if(args.length != CMD_LINE_ARGS_NUMBER) {
            log.info(CMD_ANNOTATION);
            System.exit(1);
        }

        // switching on logging, creating folder for *.log - files
        createLoggingFolder();

        // saving information to log about OS and JRE
        logSystemInfo();

        try {
            //starting for archives repacking
            ZipQueue zipProcessor = new ZipQueue();
            zipProcessor.repackZip(args[0], args[1]);

            // saving information to log about elapsed time
            long elapsedTime = System.currentTimeMillis() - startTime;

            log.fine("Elapsed time: " + elapsedTime + " ms");
            log.fine("Successfully repacked!");

            log.info(LOG_SEPARATOR);
        } catch (ZipException exc) {
            log.log(Level.SEVERE, "Error: error in zip/gzip data processing: \nDescription: ", exc);
            System.exit(1);
        } catch (IOException exc) {
            log.log(Level.SEVERE, "Error: problems in I/O\nDescription: ", exc);
            System.exit(1);
        }
    }
}
