package org.apache.log4j;

public class Logger {

    public static Logger getLogger(Class cls) {
       return new Logger();
    }

    public void error(Object str) {
        System.err.println(str);
    }

    public void error(Object str, Object obj) {
        System.err.print(str);
        System.err.print(' ');
        System.err.println(obj);
    }

    public void error(Object str, Object[] vals) {
        System.err.print(str);
        for (Object val : vals) {
            System.err.print(' ');
            System.err.print(val);

        }
        System.err.println();
    }

    public void info(Object str) {
        System.err.println(str);
    }

    public void debug(Object str) {
        System.err.println(str);
    }
}
