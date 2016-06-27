package study.task;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trying "Scanner" usage instead of "StringTokenizer"
 * Created by newbie on 24.06.16.
 */
interface Parser {
    void apply(String str);
}

public class ScannerTest {
    public static void main(String[] args) {
        String mail_regex = "[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.org)";
        Pattern pat = Pattern.compile(mail_regex);

        // testing StringTokenizer
        System.out.println("----- StringTokenizer -----");
        long tokenizerTime = timeIt(new Parser() {
           @Override
            public void apply(String str) {
                StringTokenizer tokenSet = new StringTokenizer(str, "[\t;,]");

                while(tokenSet.hasMoreTokens()) {
                    String mail = tokenSet.nextToken().trim();
                    System.out.println(mail);
                }
           }
        });
        System.out.println("----------------------------");

        // testing Scanner
        System.out.println("----- Scanner -----");
        long scannerTime = timeIt(new Parser() {
            @Override
            public void apply(String str) {
//                Scanner scan = new Scanner(str.replaceAll("[\t;,]", " ")).useDelimiter("\\s+");
//                Scanner scan = new Scanner(str).useDelimiter("\\t*\\s+\\t*|\\s*\\t+\\s*|" +
//                        "\\s*;+\\s*|\\s*,+\\s*|\\t*;+\\s*|\\t*,+\\s*|\\s*;+\\t*|\\s*,+\\t*|\\t*;+\\t*|\\t*,+\\t*");
//                Scanner scan = new Scanner(str).useDelimiter(
//                        "((\\s*)(\\t*))[;+,+]((\\s*)(\\t*))");
//                Scanner scan = new Scanner(str);
//                scan.findInLine("\\w(.*)@(.*)\\.org");

 //               Scanner scan = new Scanner(str).useDelimiter("[\\s\\t;,]+");

 //               scan.forEachRemaining(System.out::println);

/*
                Matcher mat = pat.matcher(str);
                while (mat.find()) {
                    System.out.println(mat.group());
                }
*/
                Scanner scan = new Scanner(str);

                String s;
                while ((s = scan.findInLine(pat)) != null) {
                    System.out.println(s);
                }
            }
        });
        System.out.println("----------------------------");

        // testing String.split
        System.out.println("----- String.split -----");
        long splitTime = timeIt(new Parser() {
            @Override
            public void apply(String str) {
                //for (String s : str.replaceAll("[\t;,]", " ").split("\\s+")) {
                for (String s : str.split("[\\s\\t;,]+")) {
                   //System.out.println(s.trim());
                    System.out.println(s);
                }
            }
        });
        System.out.println("----------------------------");

        System.out.println("String tokenizer time: " + tokenizerTime);
        System.out.println("Scanner time: " + scannerTime);
        System.out.println("Split time: " + splitTime);
    }

    static long timeIt(Parser parser) {
        long sumTime = 0;

        try {
            FileInputStream is = new FileInputStream(new File("../files/for_parser.txt"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);

                int dog_index = line.indexOf("@");
                if (dog_index < 0) {
                    //а на самом деле просто копируем такую строку в архив назначения
                    continue;
                }

                //слабое место: ищем пробел, посмотреть в сторону isSpace
                int mail_beg_index = line.substring(0, dog_index).replaceAll("[\t;,]", " ").lastIndexOf(" ");

                System.out.println("Mail string: " + line.substring(mail_beg_index + 1));
                System.out.println();

                System.out.println("Mailing list: ");

                long start = System.nanoTime();
                parser.apply(line.substring(mail_beg_index + 1));
                long end = System.nanoTime();

                sumTime += end - start;

                System.out.println();
            }

            reader.close();
        } catch(IOException exc) {
            System.err.println("Error opening source file");
            System.exit(1);
        }

        return sumTime;
    }
}
