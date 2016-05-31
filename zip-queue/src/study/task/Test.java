package study.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by newbie on 25.05.16.
 */
public class Test {
    public static void main(String[] args) {
        //List<String> mail_list = new ArrayList<String>();
        HashSet<String> mail_list = new HashSet<String>();
        //TreeSet<String> sorted = new TreeSet<String>();

        try {
            FileInputStream is = new FileInputStream(new File("../../files/for_parser.txt"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);

                int dog_index = line.indexOf("@");
                if(dog_index < 0) {
                    //а на самом деле просто копируем такую строку в архив назначения
                    continue;
                }

                //слабое место: ищем пробел, посмотреть в сторону isSpace
//                int mail_beg_index = line.lastIndexOf(" ", dog_index);
//                int mail_beg_index = line.substring(0, dog_index).replace("\t", " ").replace(",", " ").replace(";", " ").lastIndexOf(" ");
                int mail_beg_index = line.substring(0, dog_index).replaceAll("[\t;,]", " ").lastIndexOf(" ");

                System.out.println("Phone: " + line.substring(0, mail_beg_index));

                //работаем с телефоном
                int left_br_index = line.indexOf("(");
                int right_br_index = line.indexOf(")");

                StringBuffer phone_str = new StringBuffer(line.substring(0, mail_beg_index));

                if(right_br_index > 0 && left_br_index > 0) {
                    int code = (new Integer(line.substring(left_br_index + 1, right_br_index).trim())).intValue();

                    System.out.println("Code: " + code);
                    System.out.println("Phone before: " + phone_str);

                    //неправильно, переделать
                    switch(code) {
                        case 101: phone_str.replace(left_br_index + 1, right_br_index, "401"); break;
                        case 202: phone_str.replace(left_br_index + 1, right_br_index, "802"); break;
                        case 301: phone_str.replace(left_br_index + 1, right_br_index, "321"); break;
                    }

                    System.out.println("Phone after: " + phone_str);
                }

                //здесь запихнуть "phone_str.toString()" в файл, за ним подстроку с почтами
                //форматируем строку с телефоном
                //String formatted_phone = phone_str.toString().replace(" ", "").replace("-", "");
                String formatted_phone = phone_str.toString().replaceAll("[ \t;,-]", "");

                left_br_index = formatted_phone.indexOf("(");

                right_br_index = formatted_phone.indexOf(")");
                right_br_index += 1;


                System.out.println("Formatted phone: " + (new StringBuffer(formatted_phone)).insert(left_br_index, " ").insert(right_br_index + 1, " "));

                //работаем с почтой
                StringTokenizer stok = new StringTokenizer(line.substring(mail_beg_index), " \t,;");

                while(stok.hasMoreTokens()) {
                    String mail = stok.nextToken().trim();
                    if(mail.endsWith(".org")) {
                        System.out.println("E-mail: " + mail);
                        mail_list.add(mail);
                        //sorted.add(mail);
                    }
                }

                //List<String> sorted = new ArrayList<String>(new HashSet<String>(mail_list));
                List<String> sorted = new ArrayList<String>(mail_list);
                Collections.sort(sorted);

                System.out.println("Sorted mail list: ");
                for(String s : sorted) {
                    System.out.println(s);
                }
            }

            reader.close();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
