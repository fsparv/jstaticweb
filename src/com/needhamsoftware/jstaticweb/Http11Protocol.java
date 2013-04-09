package com.needhamsoftware.jstaticweb;/*
 * Created with IntelliJ IDEA.
 * User: gus
 * Date: 4/5/13
 * Time: 3:40 PM
 */

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class Http11Protocol extends Thread {

    private static final int NEW = 0;
    private static final int REQ_HEADERS = 1;
    private static final int RESPONSE = 2;

    // The possible return codes from this server
    public static final String ERR_500 = "HTTP/1.1 500 Internal Server Error\n\n";
    public static final String ERR_400 = "HTTP/1.1 400 Bad Request\n\n";
    public static final String ERR_404 = "HTTP/1.1 404 Not Found\n\n";
    public static final String OK_200 = "HTTP/1.1 200 OK\n\n";


    private int state = NEW;
    private String resource;
    private Socket clientSocket;

    public Http11Protocol(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        OutputStream out = null;
        BufferedReader in = null;
        try {
            out = clientSocket.getOutputStream();
            in = new BufferedReader( new InputStreamReader( clientSocket.getInputStream()));
            String inputLine;
            byte[] output = null;

            while (output == null && (inputLine = in.readLine()) != null) {
                output = processInput(inputLine);
            }
            if(output == null) {
                out.write(ERR_500.getBytes());
            }
            out.write(output);
        } catch (Throwable e) {
            e.printStackTrace();
            if (out != null) {
                try {
                    out.write(ERR_500.getBytes());
                } catch (IOException e1) {
                    e1.printStackTrace(); // nothing else to do
                }
            }
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                clientSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public byte[] processInput(String line) {
        switch (state) {
            case NEW: {
                if (line.matches("^\\s*$")) {
                    return (ERR_400.getBytes());
                }
                int resourceStart = line.indexOf('/');
                int queryStart = line.indexOf('?');
                queryStart = queryStart < resourceStart ? Integer.MAX_VALUE : queryStart;
                int protocolStart = line.indexOf("HTTP/1.");
                if (resourceStart < 0 || protocolStart < 0) {
                    return (ERR_400.getBytes());
                }
                resource = line.substring(resourceStart, Math.min(queryStart, protocolStart)).trim();
                if (resource.charAt(0) != '/' || isUplevelRequest(resource)) {
                    return (ERR_400.getBytes());
                }
                state = REQ_HEADERS;
                break;
            }
            case REQ_HEADERS: {
                if (line.matches("^\\s*$")) {
                    state = RESPONSE;
                } else {
                    //headers are ignored
                    break;
                }
            }
            case RESPONSE: {
                if (resource.endsWith("/")) {
                    resource += "index.html";
                }
                File resourceFile = new File(System.getProperty("user.dir"), resource);
                if (!resourceFile.exists()) {
                    return (ERR_404.getBytes());
                } else {
                    try {
                        ReadableByteChannel rbc = Channels.newChannel(new FileInputStream(resourceFile));
                        ByteBuffer buf = ByteBuffer.allocate(1000);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
                        baos.write(OK_200.getBytes());

                        while (rbc.read(buf) > 0) {
                            buf.flip();
                            while (buf.hasRemaining()) {
                                baos.write(buf.get());
                            }
                            buf.clear();
                        }
                        return (baos.toByteArray());
                    } catch (Throwable e) {
                        e.printStackTrace();
                        return (ERR_500.getBytes());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check that the resource specified does not live outside of the directory we are serving.
     *
     * @param str the resource specification
     * @return true if the resource specified is outside of the current directory, false otherwise
     */
    public boolean isUplevelRequest(String str) {
        int downcount = 0;
        char lastchar = 'a';
        char lastchar2 = 'a';
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (c == '/' && lastchar != '/') {
                downcount++;
            }
            if (c == '/' && lastchar == '.' && lastchar2 == '.') {
                downcount -= 2;
            }
            lastchar2 = lastchar;
            lastchar = c;

            if (downcount <= 0) {
                return true;
            }
        }
        return downcount <= 0;
    }
}
