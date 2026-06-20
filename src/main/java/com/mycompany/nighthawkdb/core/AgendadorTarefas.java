package com.mycompany.nighthawkdb.core;

import com.mycompany.nighthawkdb.config.ConfigManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgendadorTarefas {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void agendarBackup(String dbPath, int hora, int minuto) {
        long initialDelay = calcularDelay(hora, minuto);
        scheduler.scheduleAtFixedRate(() -> {
            // Chamar gbak via linha de comando
            ProcessBuilder pb = new ProcessBuilder(
                ConfigManager.getFirebirdBinPath() + "\\gbak.exe",
                "-b", "-v", "-user", "sysdba", "-pass", "masterkey",
                dbPath, dbPath + ".fbk");
            try {
                Process p = pb.start();
                p.waitFor();
                System.out.println("Backup agendado concluído.");
            } catch (Exception e) { e.printStackTrace(); }
        }, initialDelay, 24, TimeUnit.HOURS);
    }

    private static long calcularDelay(int hora, int minuto) {
        // Implementar cálculo do delay até a próxima execução
        return 1; // simplificado para testes
    }
}