
package com.lawfirm;

import com.lawfirm.controller.*;
import com.lawfirm.util.AuthGuard;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class Server {

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(8082), 0);

        server.createContext("/api/clients", new AuthGuard(new ClientHandler(), "ADMIN", "BRANCH_MANAGER"));

        server.createContext("/api/lawyers", new AuthGuard(new LawyerHandler(), "ADMIN", "BRANCH_MANAGER"));

        server.createContext("/api/cases", new AuthGuard(new CaseHandler(),"ADMIN", "BRANCH_MANAGER", "LAWYER"));

        server.createContext("/api/appointments", new AuthGuard(new AppointmentHandler(), "ADMIN", "BRANCH_MANAGER", "LAWYER", "CLIENT"));

        server.createContext("/api/documents", new AuthGuard(new DocumentHandler(),"ADMIN", "BRANCH_MANAGER", "LAWYER"));

        server.createContext("/api/notifications", new AuthGuard(new NotificationHandler(), "ADMIN", "BRANCH_MANAGER", "LAWYER", "CLIENT"));

        server.createContext("/api/branches", new AuthGuard(new BranchHandler(), "ADMIN", "BRANCH_MANAGER"));

        server.createContext("/api/recommendations", new AuthGuard(new RecommendationHandler(),"ADMIN", "BRANCH_MANAGER", "LAWYER", "CLIENT"));

        server.createContext("/api/auth/login", new AuthHandler());
        server.createContext("/api/auth/session", new SessionHandler());
        server.createContext("/api/client-accounts", new AuthGuard(new ClientAccountHandler(), "ADMIN"));
        server.createContext("/api/lawyer-accounts",
                new AuthGuard(new LawyerAccountHandler(), "ADMIN"));



        server.createContext(
                "/api/lawyer-directory",
                new AuthGuard(new LawyerDirectoryHandler(),
                        "ADMIN", "BRANCH_MANAGER", "LAWYER", "CLIENT")
        );

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
        System.out.println("Documents API: http://localhost:8082/api/documents");
        System.out.println("Notification API: http://localhost:8082/api/notifications");
        System.out.println("Branches API: http://localhost:8082/api/branches");
        System.out.println("Recommendations API: http://localhost:8082/api/recommendations");
        System.out.println("Login API: http://localhost:8082/api/auth/login");
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
