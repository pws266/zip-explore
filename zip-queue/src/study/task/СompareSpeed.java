package study.task;

import java.util.*;

/**
 * Comparision HashSet and TreeSet execution speed via elements addition and sorting
 * Created by newbie on 23.06.16.
 */
interface Lambda {
    void apply();
}

public class СompareSpeed {
    public static void main(String[] args) {

        final String[] strings = prepareStrings();

        long forSet = timeIt(new Lambda() {
            @Override
            public void apply() {
                Set<String> set = new TreeSet<String>();

                for (String str : strings) {
                    set.add(str);
                }
            }
        });

        System.out.println("red-black tree: " + forSet);

        long forHash = timeIt(new Lambda() {
            @Override
            public void apply() {
                Set<String> set = new HashSet<String>();

                for (String str : strings) {
                    set.add(str);
                }

                final List<String> sorted = new ArrayList<String>(set);
                Collections.sort(sorted);
            }
        });

        System.out.println("sorted HashMap: " + forHash);

        final List<String> list = new ArrayList<String>(Arrays.asList(strings));

        long forList = timeIt(new Lambda() {
            @Override
            public void apply() {
                Collections.sort(list);
            }
        });

        System.out.println("merge stable sort: " + forList);
    }

    private static String[] prepareStrings() {
        int len = 1000000;
        final String[] strings = new String[len];

        for (int i = 0; i < len; ++i) {
            strings[i] = UUID.randomUUID().toString();
        }
        return strings;
    }

    static long timeIt(Lambda lambda) {
        long start = System.nanoTime();
        lambda.apply();
        long end = System.nanoTime();
        return end - start;
    }
}
