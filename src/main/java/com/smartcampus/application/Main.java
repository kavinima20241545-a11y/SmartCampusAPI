package com.smartcampus.application;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.logging.Logger;

/**
 main class
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

// Localhost URL
    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1";

    public static void main(String[] args) throws Exception {
        ResourceConfig config = new ResourceConfig()
                .packages("com.smartcampus")   // scans resource, exception, filter sub-packages
                .register(JacksonFeature.class);

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI), config);

        LOGGER.info("Smart Campus API started at: http://localhost:8080/api/v1");
        LOGGER.info("Press CTRL+C to stop...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down server...");
            server.shutdownNow();
        }));

        Thread.currentThread().join();
    }
}