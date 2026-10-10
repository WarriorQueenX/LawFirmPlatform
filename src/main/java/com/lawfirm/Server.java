
package com.lawfirm;

import com.lawfirm.controller.ClientHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class Server {

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(
                new InetSocketAddress(8082), 0
        );

        server.createContext("/api/clients", new ClientHandler());

        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {

                String path = exchange.getRequestURI().getPath();

                if (!path.equals("/") && !path.equals("/index.html")
                        && !path.equals("/style.css")
                        && !path.equals("/app.js")) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }

                String filename = path.equals("/") ? "index.html"
                        : path.substring(1);

                Path file = Path.of("frontend", filename);

                if (!Files.exists(file)) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }

                String contentType = filename.endsWith(".css")
                        ? "text/css; charset=UTF-8"
                        : filename.endsWith(".js")
                          ? "application/javascript; charset=UTF-8"
                          : "text/html; charset=UTF-8";

                byte[] content = Files.readAllBytes(file);

                exchange.getResponseHeaders().set(
                        "Content-Type", contentType
                );
                exchange.sendResponseHeaders(200, content.length);

                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(content);
                }
            }
        });

        server.setExecutor(null);
        server.start();

        System.out.println("Law Firm Platform: http://localhost:8082");
        System.out.println("Client API: http://localhost:8082/api/clients");
    }
}
