
package com.lawfirm;

import com.lawfirm.controller.AppointmentHandler;
import com.lawfirm.controller.CaseHandler;
import com.lawfirm.controller.ClientHandler;
import com.lawfirm.controller.LawyerHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.lawfirm.controller.DocumentHandler;
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
        server.createContext("/api/lawyers", new LawyerHandler());
        server.createContext("/api/cases", new CaseHandler());
        server.createContext("/api/appointments", new AppointmentHandler());
        server.createContext("/api/documents", new DocumentHandler());


        Path frontendRoot = Path.of(
                System.getProperty("user.dir"), "frontend"
        ).toAbsolutePath().normalize();

        System.out.println("Frontend directory: " + frontendRoot);

        server.createContext("/", exchange -> serveFrontend(exchange, frontendRoot));

        server.setExecutor(null);
        server.start();

        System.out.println("Law Firm Platform: http://localhost:8082/");
        System.out.println("Client API: http://localhost:8082/api/clients");
        System.out.println("Lawyer API: http://localhost:8082/api/lawyers");
        System.out.println("Case API: http://localhost:8082/api/cases");
        System.out.println("Appointment API: http://localhost:8082/api/appointments");
    }

    private static void serveFrontend(
            HttpExchange exchange, Path frontendRoot
    ) throws IOException {

        String requestPath = exchange.getRequestURI().getPath();

        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        Path file = frontendRoot.resolve(
                requestPath.substring(1)
        ).normalize();

        // Prevent requests from accessing files outside frontendRoot.
        if (!file.startsWith(frontendRoot)
                || !Files.isRegularFile(file)) {
            byte[] notFound = "Frontend file not found".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8
            );

            exchange.sendResponseHeaders(404, notFound.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(notFound);
            }
            return;
        }

        String filename = file.getFileName().toString();

        String contentType = filename.endsWith(".css")
                ? "text/css; charset=UTF-8"
                : filename.endsWith(".js")
                  ? "application/javascript; charset=UTF-8"
                  : "text/html; charset=UTF-8";

        byte[] content = Files.readAllBytes(file);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, content.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }
}
