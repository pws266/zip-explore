package study.task;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static study.task.RefInfo.MAIL_DOMAIN;

/**
 * Created by newbie on 27.06.16.
 */
public class SplitTest {
    public static void main(String[] args) {
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

                System.out.println("Split: ");

                StringBuilder phone = new StringBuilder();

                for (String s : line.split("[\\s;,-]+")) {
                    System.out.println(s);

                    if (s.indexOf("@") == -1) {
                        phone.append(s);

                        continue;
                    }
                }

                phone.insert(phone.indexOf("("), " ").insert(phone.indexOf(")") + 1, " ");
                System.out.println("Phone: " + phone.toString());

            }

            reader.close();
        } catch(IOException exc) {
            System.err.println("Error opening source file");
            System.exit(1);
        }

    }
}
