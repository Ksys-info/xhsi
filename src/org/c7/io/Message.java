/**
 * Message.java
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.c7.io;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Message
 */
public class Message extends Thread {

    /**
     * DEBUG
     */
    private final static boolean DEBUG = System.getProperty("message.debug") != null;

    /**
     * The default server name
     */
    private final static String DEFAULT_SERVER;
    static {
        String str = System.getProperty("message.server", System.getenv("MESSAGESERVER"));
        String host = (str == null) ? "localmsg.net" : str;
        try {
             InetAddress.getByName(host);
        } catch (UnknownHostException ex) {
            if (DEBUG) {
                System.out.println("*************** Message: cannot access host " + host + " using localhost");
            }
            host = "localhost";
        } catch (Exception ex) {
        }
        DEFAULT_SERVER = host;
    }

    /**
     * parse
     */
    public static String parse(String str, String tag) {
        if (str != null) {
            String startTag = "<"+tag+">";
            int start = str.indexOf(startTag);
            if (start > 0) {
                String endTag = "</"+tag+">";
                //int end = tag.equals("data") ? str.lastIndexOf(endTag) : str.indexOf(endTag, start);
                int end = str.indexOf(endTag, start);
                if (end >= 0) {
                    start += startTag.length();
                    if (start >= end) {
                        return null;     // Zero length XML nodes should not occur here, but...
                    } else if (tag.equals("data")) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = start ; i < end ; i++) {
                            char ch = str.charAt(i);
                            if (ch == '&') {
                                if (next(str, i+1, "lt;")) {
                                    sb.append('<');
                                    i += 3;
                                } else if (next(str, i+1, "gt;")) {
                                    sb.append('>');
                                    i += 3;
                                } else if (next(str, i+1, "amp;")) {
                                    sb.append('&');
                                    i += 4;
                                } else {
                                    sb.append(ch);  // Some error....
                                }
                            } else {
                                sb.append(ch);
                            }
                        }
                        return sb.toString();
                    } else {
                        return str.substring(start,  end);
                        //return (res.length() == 0) ? null : res;
                    }
                }
            }
        }
        return null;
    }

    /**
     * get
     */
    private static boolean next(String str, int i, String txt) {
        int j = 0;
        try {
            switch (txt.length()) {
                default : throw new Error();
                case 4:   if (str.charAt(i++) != txt.charAt(j++)) return false;
                case 3:   if (str.charAt(i++) != txt.charAt(j++)) return false;
                          if (str.charAt(i++) != txt.charAt(j++)) return false;
                          if (str.charAt(i++) != txt.charAt(j++)) return false;
                          return true;
            }
        } catch (Exception ex) {
        }
        return false;
    }

    /**
     * get
     */
    public static String get(String url) throws IOException {
        return get(url, false);
    }

    /**
     * get
     */
    public static String get(String url, boolean debug) throws IOException {
        String[] parts = url.split("\\?", 2);
        StringBuffer sb = new StringBuffer(getServer(parts[0]));
        if (parts.length == 2) {
            String op = "?";
            boolean hasData = false;
            boolean hasUuid = false;
            for (String parm : parts[1].split("\\&")) {
                String[] kv = parm.split("\\=", 2);
                sb.append(op);
                sb.append(kv[0]);
                sb.append("=");
                sb.append(URLEncoder.encode(kv[1], "UTF-8"));
                op = "&";
                hasData |= kv[0].equals("data");
                hasUuid |= kv[0].equals("uuid");
            }
            if (hasData && !hasUuid) {
                sb.append("&uuid=");
                sb.append(getUuid());
            }
        }
        String str = sb.toString();
        if (debug) {
            System.err.println("URL = " + str);
        }
        return get0(str);
    }


    /**
     * get0
     */
    private static String get0(String urlstr) throws IOException {
        if (DEBUG) {
            System.out.println(""+new Date()+" ----------------------- get0() url="+urlstr);
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlstr);
            connection = (HttpURLConnection)url.openConnection();
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setReadTimeout(75000);
            connection.setConnectTimeout(75000);
            return new Scanner(connection.getInputStream(), "UTF-8").useDelimiter("\\A").next();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
        //return new Scanner(new URL(url).openStream(), "UTF-8").useDelimiter("\\A").next();
    }

    /**
     * getServer
     */
    private static String getServer(String url) {
        if (url.startsWith("http://")) {
            return url;
        } else {
            String end = "";
            if (!url.startsWith("/message/")) {         //    /message/foo
                if (url.startsWith("message/")) {
                    end = "/";                          //    message/foo
                } else if (url.startsWith("/")) {
                    end = "/message";                   //    /foo
                } else {
                    end = "/message/";                  //    foo
                }
            }
            return "http://"+ DEFAULT_SERVER + end + url;
        }
    }


    /**
     * fixupUrl
     */
    private static void fixupUrl(StringBuilder sb, String url) {
        if (url.startsWith("http://")) {
            sb.append(url);
        } else {
            String end = "";
            if (!url.startsWith("/message/")) {         //    /message/foo
                if (url.startsWith("message/")) {
                    end = "/";                          //    message/foo
                } else if (url.startsWith("/")) {
                    end = "/message";                   //    /foo
                } else {
                    end = "/message/";                  //    foo
                }
            }
            sb.append("http://");
            sb.append(DEFAULT_SERVER);
            sb.append(end);
            sb.append(url);
        }
    }

    /**
     * readData
     */
    public static String readData(String url) throws IOException {
        return parse(read(url), "data");
    }

    /**
     * writeData
     */
    public static String writeData(String url, String data) {
        return parse(writeTo(url, "", data), "data");
    }

    /**
     * read
     */
    public static String read(String url) {
        try {
            return get(url);
        } catch (Exception ex) {
           throw new RuntimeException(ex);
        }
    }

    /**
     * write
     */
    public static String write(String url, String data) {
        return writeTo(url, "", data);
    }

    /**
     * writeTo
     */
    public static String writeTo(String url, String to, String data) {
        try {
            if (to == null || to.length() == 0) {
                return send(url, data, null);
            } else {
                Properties p = new Properties();
                p.setProperty("to", to);
                return send(url, data, p);
            }
        } catch (Exception ex) {
           throw new RuntimeException(ex);
        }
    }

    /**
     * createChannel
     */
    public static String createChannel(String chan) throws IOException {
        Properties p = new Properties();
        p.setProperty("create", "true");
        return send(chan, null, p);
    }

    /**
     * send
     */
    public static String send(String url, String data, Properties p) throws IOException {
        return send(url, data, p, false);
    }

    /**
     * send
     */
    public static String send(String url, String data, Properties p, boolean debug) throws IOException {
        StringBuilder sb = new StringBuilder();
        fixupUrl(sb, url);
        boolean seenUuid = false;
        char op = '?';
        if (p != null) {
            for (String k : p.stringPropertyNames()) {
                String v = p.getProperty(k);
                sb.append(op);
                sb.append(k);
                sb.append('=');
                sb.append(URLEncoder.encode(v, "UTF-8"));
                seenUuid |= k.equals("uuid");
                op = '&';
            }
        }
        if (data != null && data.length() > 0) {
            if (!seenUuid) {
                sb.append(op);
                sb.append("uuid=");
                sb.append(getUuid());
            }
            sb.append("&data=");
            sb.append(URLEncoder.encode(data, "UTF-8"));
        }
        String str = sb.toString();
        if (debug) {
            System.err.println("URL = " + str);
        }
        return get0(str);
    }


    // ------------------------------ Async loop ------------------------------

    /**
     * addNotifier
     */
    public static Message addNotifier(String channel, Notify client) {
        return addNotifier(channel, null, client);
    }

    /**
     * addNotifier
     */
    public static Message addNotifier(String channel, String filter, Notify client) {
        return addNotifier(channel, filter, false, client);
    }

    /**
     * addNotifier
     */
    public static Message addNotifier(String channel, boolean notifyRaw, Notify client) {
        return addNotifier(channel, null, notifyRaw, client);
    }

    /**
     * addNotifier
     */
    public static Message addNotifier(String channel, String filter, boolean notifyRaw, Notify client) {
        Message m = new Message(channel, filter, notifyRaw, client);
        m.start();
        return m;
    }

    /**
     * running
     */
    private boolean running = true;

    /**
     * channel
     */
    private final String channel;

    /**
     * filter
     */
    private final String filter;

    /**
     * notifyRaw
     */
    private final boolean notifyRaw;

    /**
     * client
     */
    private final Notify client;

    /**
     * Message
     */
    private Message(String channel, String filter, boolean notifyRaw, Notify client) {
        this.channel = channel;
        this.filter = filter;
        this.notifyRaw = notifyRaw;
        this.client = client;
        if (DEBUG) {
            System.out.println("Message() channel="+channel+" filter="+filter+" notifyRaw="+notifyRaw);
        }
        setDaemon(true);
    }

    /**
     * run
     */
    public void run() {
        int next = 0;
        //try {
        //    String raw = get(channel); // Get the newest posting
        //    next = notify(raw);
        //} catch (IOException ex) {
        //}
        while (running) {
            long time = System.currentTimeMillis();
            for (int i = 0 ; i < 25 ; i++) {
                try {
                    String url = channel + ((next == 0) ? "/+1" : "/" + next) + "?create=true"; // Either get a specific posting or the next posting
                    String raw = get(url);
                    next = notify(raw);
                } catch (IOException ex) {
                    pause(1000); // Wait for 1 second
                }
            }

            // The following should ensure that no matter what, this
            // code will never cause more that 25 requests / second

            long delta = System.currentTimeMillis() - time;
            pause(1000L - delta); // Wait for upto one second
        }
    }


    /**
     * notify
     */
    private int notify(String raw) {
        if (raw != null) {
            String data = parse(raw, "data");
            String to   = parse(raw, "to");
            if (DEBUG) {
                System.out.println(""+new Date()+" ----------------------- notify() raw="+raw+ " data="+data+" filter="+filter+" to="+to+" notifyRaw="+notifyRaw);
            }
            boolean forClient = (data != null || notifyRaw) && (filter == null || to == null || filter.equals(to));
            if (forClient) {
                try {
                    String str = notifyRaw ? raw : data;
                    if (data != null) {
                        if (DEBUG) {
                            System.out.println(""+new Date()+" ----------------------- notify() str="+str);
                        }
                        client.notify(str);
                    }
                } catch (Exception ex) {
                    System.out.println("Exception calling notify() = " + ex);
                    ex.printStackTrace();
                }
            }
            try {
               return Integer.parseInt(parse(raw, (data != null) ? "number" : "highest")) + 1;
            } catch (Exception ex) {
            }
        }
        return 0;
    }

    /**
     * shutdown
     */
    public void shutdown() {
        running = false;
    }

    /**
     * pause
     */
    private void pause(long time) {
        if (time > 0) {
            try {
                Thread.sleep(time);
            } catch (Exception ex) {
            }
        }
    }


    /**
     * Notify
     */
    public interface Notify {
        public void notify(String str);
    }




    // ================================================================================
    //                                Mini Base64 encoder
    // ================================================================================


  //private static String charSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-";  // Normal
    private static String charSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789$@";  // Ours
    private static byte[] encodeData;

    static {
        encodeData = new byte[64];
        for (int i = 0 ; i < 64 ; i++) {
            encodeData[i] = (byte) charSet.charAt(i);
        }
    }

    /**
     * b64encode
     */
    private static String b64encode(byte[] src, int start, int length, boolean pad) {
        byte[] dst = new byte[(length+2)/3 * 4 + length/54];
        int x = 0;
        int dstIndex = 0;
        int state = 0;  // which char in pattern
        int old = 0;    // previous byte
        int len = 0;    // length decoded so far
        int max = length + start;
        for (int srcIndex = start ; srcIndex < max ; srcIndex++) {
            x = src[srcIndex];
            switch (++state) {
            case 1:
                dst[dstIndex++] = encodeData[(x>>2) & 0x3f];
                break;
            case 2:
                dst[dstIndex++] = encodeData[((old<<4)&0x30) | ((x>>4)&0xf)];
                break;
            case 3:
                dst[dstIndex++] = encodeData[((old<<2)&0x3C) | ((x>>6)&0x3)];
                dst[dstIndex++] = encodeData[x&0x3F];
                state = 0;
                break;
            }
            old = x;
        }

       if (pad) {
            /*
             * now clean up the end bytes
             */
            switch (state) {
                case 1: dst[dstIndex++] = encodeData[(old<<4) & 0x30];
                   dst[dstIndex++] = (byte) '=';
                   dst[dstIndex++] = (byte) '=';
                   break;
                case 2: dst[dstIndex++] = encodeData[(old<<2) & 0x3c];
                   dst[dstIndex++] = (byte) '=';
                   break;
            }
        }
        return new String(dst, 0, dstIndex);
    }

    /**
     * place
     */
    private static void place(long val, byte[] buf, int pos) {
        buf[pos++] = (byte)(val & 0xff) ; val >>= 8;
        buf[pos++] = (byte)(val & 0xff) ; val >>= 8;
        buf[pos++] = (byte)(val & 0xff) ; val >>= 8;
        buf[pos++] = (byte)(val & 0xff) ; val >>= 8;
        buf[pos++] = (byte)(val & 0xff) ; val >>= 8;
        buf[pos++] = (byte)(val & 0xff) ; val >>= 8;
        buf[pos++] = (byte)(val & 0xff) ; val >>= 8;
        buf[pos++] = (byte)(val & 0xff) ;;
    }

    /**
     * getUuid()
     */
    private static String getUuid() {
        UUID uuid = UUID.randomUUID();
        byte[] buf = new byte[16];
        place(uuid.getMostSignificantBits(), buf, 0);
        place(uuid.getLeastSignificantBits(), buf, 8);
        return b64encode(buf, 0, buf.length, false);
    }
}





/*



      //String u1 = uuid.toString();
      //String u2 = Long.toString(uuid.getMostSignificantBits(), Character.MAX_RADIX) +
      //            Long.toString(uuid.getLeastSignificantBits(), Character.MAX_RADIX);

&uuid=90RO8Xaors5aPnDQi5Lhk

//Base64.Encoder
//Base64.Encoder  withoutPadding()
//encoder encodeToString(byte[] src)


//Base64.Encoder enc = Base64.getEncoder();
//enc = enc.withoutPadding();
//String u3 = enc.encodeToString(buf);

//System.err.println("u1="+u1);
//System.err.println("u2="+u2);
//System.err.println("u3="+u3);


u1=34a8af0c-7a3a-4866-9b99-9024867f86f4
u2=sttx6l9gdjl2-1iyqp4ktjj8jw
u3=Zkg6egyvqDT0hn_GJJCZm
Kk@NHMaagZqF7U5FIRtNp


u1=

68e674d6-5ef1-4ac8-bc2b-3d935d6ad939
68e674d65ef14ac8bc2b3d935d6ad939              // 32

u2=
1lffjcnzux3ns-114up3drsyo5j                   // 26-28
*/



