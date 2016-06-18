package study.task;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.zip.*;

/**
 * Custom exception class for logs storage folder verification
 *
 * @author Sergey Sokhnyshev
 * Created on 18.06.16.
 */

class LoggingFolderException extends Exception {
    private String msg;

    LoggingFolderException(String msg) {
        // using superclass constructor for correct detailed message output
        super(msg);
        this.msg = msg;
    }

    public String toString() {
        return "LoggingFolderException: " + msg;
    }
}

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
    private static final Logger log = Logger.getLogger(ZipQueue.class.getName());

    private int recursionLevel;        //nesting level counter
    private final HashSet<String> mailList;  //storage of sorted e-mails
    private final HashSet<String> phones;     //storage of sorted phones

    private ZipQueue() {
        recursionLevel = 0;
        mailList = new HashSet<>();
        phones = new HashSet<>();
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
        repackedNameFormer.insert(repackedNameFormer.lastIndexOf(RefInfo.separatorDot), RefInfo.repackedFileNameSuffix);

        try ( FileInputStream input = new FileInputStream(zipFile);
              FileOutputStream output = new FileOutputStream(new File(repackedNameFormer.toString()))
            ){
                //performs on-fly archives repacking
                //invokes recursively
                processNestedZip(input, output);
        } catch (FileNotFoundException exc) {
            System.err.println("Error in ZipQueue.repackZip: input/output file isn't found");
            System.err.println("Error description: " + exc.getMessage());

            log.log(Level.SEVERE, "Error: input/output file isn't found\nDescription: ", exc);
            System.exit(1);
        }
    }

    /** clears mail and phone storages before new archive repacking */
    private void reset() {
        recursionLevel = 0;
        mailList.clear();
        phones.clear();
    }

    /** recursive method for nested archives repacking */
    private void processNestedZip(InputStream input, OutputStream output) throws IOException {
        System.out.println(">> Nesting level: " + recursionLevel);
        log.info(">> Nesting level: " + recursionLevel);

        ++recursionLevel;

        ZipInputStream zin = new ZipInputStream(input);
        ZipOutputStream zos = new ZipOutputStream(output);

        ZipEntry inEntry;

        while((inEntry = zin.getNextEntry()) != null) {
            if(inEntry.isDirectory()) {
                System.out.println("Repacked directory: " + inEntry.getName());
                log.info("Repacked directory: " + inEntry.getName());

                zos.putNextEntry(new ZipEntry(inEntry.getName()));
            } else {
                System.out.println("Repacking file: " +  inEntry.getName());
                log.info("Repacking file: " +  inEntry.getName());

                zos.putNextEntry(new ZipEntry(inEntry.getName()));

                //nested *.zip archives processing
                if(inEntry.getName().endsWith(RefInfo.zipFileExtension)) {
                    //recursive invoking of method if we are in nested archive
                    processNestedZip(zin, zos);
                //processing *.gz archives
                } else if (inEntry.getName().endsWith(RefInfo.gzipFileExtension)){
                    GZIPInputStream gzIn = new GZIPInputStream(zin);
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
        if(--recursionLevel == 0) {
            saveSortedData(RefInfo.sortedPhonesFileName, zos, phones);
            saveSortedData(RefInfo.sortedEmailsFileName, zos, mailList);

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
            int atIndex = line.indexOf(RefInfo.separatorAt);
            if(atIndex > 0) {
                //separating phone number from e-mails set
                //using regular expression due to lines without spaces between phone number and first e-mail in line
                int mailBeginIndex = line.substring(0, atIndex).replaceAll(RefInfo.regexpSeparatorsToSpace,
                                        RefInfo.separatorSpace).lastIndexOf(RefInfo.separatorSpace);

                //processing phone number: changing city code if necessary
                int leftBracketIndex = line.indexOf(RefInfo.leftBracket);
                int rightBracketIndex = line.indexOf(RefInfo.rightBracket);

                StringBuilder phoneStr = new StringBuilder(line.substring(0, mailBeginIndex));

                if(rightBracketIndex > 0 && leftBracketIndex > 0) {
                    //getting substitution code instead of source city code
                    String sub_code = RefInfo.phoneCodesSubstitution.get(line.substring(leftBracketIndex + 1,
                                                                         rightBracketIndex).trim());
                    if(sub_code != null) {
                        phoneStr.replace(leftBracketIndex + 1, rightBracketIndex, sub_code);
                    }
                }

                //saving modified line in stream
                String savedPhone = phoneStr.toString();

                writer.write(savedPhone, 0, savedPhone.length());
                writer.write(line, mailBeginIndex, line.length() - mailBeginIndex);

                //formatting phone number according task statement
                String formattedPhone = savedPhone.replaceAll(RefInfo.regexpSeparatorsToNoSign,
                                                               RefInfo.separatorNoSign);

                leftBracketIndex = formattedPhone.indexOf(RefInfo.leftBracket);

                rightBracketIndex = formattedPhone.indexOf(RefInfo.rightBracket);
                rightBracketIndex += 1;    //increasing index due to space insertion in previous line

                //saving unique phone number
                phones.add((new StringBuffer(formattedPhone)).insert(leftBracketIndex,
                            RefInfo.separatorSpace).insert(rightBracketIndex + 1, RefInfo.separatorSpace).toString());

                //processing e-mails set in line
                StringTokenizer tokenSet = new StringTokenizer(line.substring(mailBeginIndex),
                                                                RefInfo.mailSeparators);

                while(tokenSet.hasMoreTokens()) {
                    String mail = tokenSet.nextToken().trim();
                    if(mail.endsWith(RefInfo.mailDomain)) {
                        mailList.add(mail);
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
            // getting program start time
            long startTime = System.currentTimeMillis();

            //checking command line arguments number
            if(args.length != RefInfo.cmdLineArgsNumber) {
                System.out.println("ZipQueue utility for nested *.zip and *.gz archives repacking");
                System.out.println("Usage: ");
                System.out.println("       java -jar utility_name.jar [path/archive_name.zip] [result_path/]");
                System.out.println("         or");
                System.out.println("       java -classpath ./[path_to_package_folder] study.task.ZipQueue " +
                                   "[path/archive_name.zip] [result_path/]");
                System.out.println("Result: utility puts the repacked archive \"<archive_name>v2.zip\" " +
                                   "in user specified folder");

                System.exit(1);
            }

            // switching on logging
            // resource file should be in the same folder with package folders. Using "absolute" path
            LogManager.getLogManager().readConfiguration(
                    study.task.ZipQueue.class.getResourceAsStream("/res/logging.properties"));

            // creating folder for logging
            if(log.getParent().getLevel() != Level.OFF) {
                //getting name of log folder
                Properties logTraits = new Properties();
                logTraits.load(study.task.ZipQueue.class.getResourceAsStream("/res/logging.properties"));

                String logPattern = logTraits.getProperty("java.util.logging.FileHandler.pattern");

                if(logPattern != null) {
                    String logPath = new File(logPattern).getParent();

                    File logFolder = new File(logPath);

                    if (!logFolder.exists() && !logFolder.mkdirs()) {
                        throw new LoggingFolderException("Unable to create folder for *.log files specified in resources");
                    }
                }
            }

            // saving information to log about OS and JRE
            log.info("--------------------------------------------------------------------------");

            log.info("OS:\n - name: " + System.getProperty("os.name") + "\n - platform: " +
                    System.getProperty("os.arch") + "\n - version: " + System.getProperty("os.version") + "\n");

            log.info("JRE:\n - vendor: " + System.getProperty("java.specification.vendor") + "\n - name: " +
                    System.getProperty("java.specification.name") + "\n - version: " +
                    System.getProperty("java.specification.version") + "\n");

            //starting for archives repacking
            ZipQueue zipProcessor = new ZipQueue();
            zipProcessor.repackZip(args[0], args[1]);

            // saving information to log about elapsed time
            long elapsedTime = System.currentTimeMillis() - startTime;

            System.out.println("Elapsed time: " + elapsedTime + " ms");

            log.fine("Elapsed time: " + elapsedTime + " ms");
            log.fine("Successfully repacked!");

            log.info("--------------------------------------------------------------------------");
        } catch (ZipException exc) {
            System.err.println("Error: error in zip/gzip data processing: " + exc.getMessage());

            log.log(Level.SEVERE, "Error: error in zip/gzip data processing: \nDescription: ", exc);
            System.exit(1);
        } catch (IOException exc) {
            System.err.println("Error: problems in I/O: " + exc.getMessage());

            log.log(Level.SEVERE, "Error: problems in I/O\nDescription: ", exc);
            System.exit(1);
        } catch (LoggingFolderException exc) {
            System.err.println("Error: no log folder was created: " + exc.getMessage());

            log.log(Level.SEVERE, "Error: no log folder was created: ", exc);
            System.exit(1);
        }
    }
}
