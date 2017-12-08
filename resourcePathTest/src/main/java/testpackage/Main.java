package testpackage;

import java.io.File;
import java.net.URL;


public class Main {

    public static void main(String[] args) {
        File file = new File("");
        println("File path with '' = " + file.getAbsolutePath());

        File file2 = new File("/");
        println("File path with '/' = " + file2.getAbsolutePath());

        URL file3 = Main.class.getResource("");
        println("Resource path without '/' = " + file3.getPath());

        URL file4 = Main.class.getResource("/");
        println("Resource path with '/' = " + file4.getPath());

        URL file5 = Main.class.getClassLoader().getResource("");

        println("ClassLoader resource path without '/' = " + file5.getPath());

        URL file6 = Main.class.getClassLoader().getResource("/");

        println("ClassLoader resource path with '/' = " + file6.getPath());



    }


    public static void println(String string) {
        System.out.println(string);
    }
}
