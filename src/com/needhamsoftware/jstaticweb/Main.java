package com.needhamsoftware.jstaticweb;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is meant to serve as a start point for a really really stupidly dumb web server. The whole point
 * is to serve up pages with as little overhead as possible. All request headers are ignored, there is no support
 * for virtual hosts, and no modified checking. You ask for the bytes, and we give em to you, with no fuss and
 * no frills, no questions asked. Files are served from the current working directory, and below, but it should
 * not be possible to request files above that point.
 */

public class Main {
    public static void main(String[] args) throws IOException {

        int port = 8008;
        try {
            if (args.length > 0 && args[0] != null) {
                port = Integer.parseInt(args[0]);
            }
        } catch (NumberFormatException nfe) {
            System.err.println("Could not parse port number " + args[0] + ".");
            System.exit(1);
        }
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 8008.");
            System.exit(1);
        }

        while (true) {
            Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
                new Http11Protocol(clientSocket).start();

            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }
        }
    }
}