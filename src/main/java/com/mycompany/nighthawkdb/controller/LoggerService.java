package com.mycompany.nighthawkdb.core;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerService {
    private static final String LOG_DIR = "logs";

    public static void log(String message) {
        try {
            Files.createDirectories(Paths.get(LOG_DIR)); // Cria a pasta "logs" se não existir
            String dataAtual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String horaAtual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            File logFile = new File(LOG_DIR, "nighthawkdb_" + dataAtual + ".log");
            
            // "true" para não apagar o ficheiro, apenas adicionar no final
            try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
                out.println("[" + horaAtual + "] " + message);
            }
        } catch (IOException e) {
            System.err.println("Erro crítico ao gravar log físico: " + e.getMessage());
        }
    }
}