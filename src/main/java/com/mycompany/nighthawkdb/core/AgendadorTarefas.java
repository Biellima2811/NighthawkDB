package com.mycompany.nighthawkdb.core;

import com.mycompany.nighthawkdb.config.ConfigManager;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgendadorTarefas {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void agendarBackup(String dbPath, int hora, int minuto) {
        long initialDelay = calcularDelayAte(hora, minuto);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    ConfigManager.getFirebirdBinPath() + "\\gbak.exe",
                    "-b", "-v", "-user", "sysdba", "-pass", "masterkey",
                    dbPath, dbPath + ".fbk");
                Process p = pb.start();
                p.waitFor();
                System.out.println("Backup agendado concluído.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, initialDelay, 24, TimeUnit.HOURS);
    }

    private static long calcularDelayAte(int hora, int minuto) {
        LocalTime now = LocalTime.now();
        LocalTime target = LocalTime.of(hora, minuto);
        long diffSeconds = target.toSecondOfDay() - now.toSecondOfDay();
        if (diffSeconds < 0) diffSeconds += 24 * 3600; // se já passou, vai para amanhã
        return diffSeconds;
    }
}