package com.opscopilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

// Enables component scanning, auto-configuration, and Java-based configuration for this application.
@SpringBootApplication
@ConfigurationPropertiesScan
public class OpsCopilotApplication {

    // Java entry point; Spring Boot creates and starts the application context from this class.
    static void main(String[] args) {
        SpringApplication.run(OpsCopilotApplication.class, args);
    }

}
