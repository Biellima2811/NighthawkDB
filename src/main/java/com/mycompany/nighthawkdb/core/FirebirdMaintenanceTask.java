package com.mycompany.nighthawkdb.core;

import com.mycompany.nighthawkdb.config.ConfigManager;
import javafx.concurrent.Task;
import java.io.*;
import java.util.*;

public class FirebirdMaintenanceTask extends Task<Void> {

    private final String commandType;
    private final List<String> arguments;

    public FirebirdMaintenanceTask(String commandType, List<String> arguments) {
        this.commandType = commandType;
        this.arguments = arguments;
    }

    @Override
    protected Void call() {
        updateMessage("Iniciando " + commandType);
        updateProgress(0, 100);
        long start = System.currentTimeMillis();
        try {
            String bin = ConfigManager.getFirebirdBinPath();
            if (bin.isEmpty()) {
                updateMessage("[ERRO] Pasta BIN não encontrada.");
                return null;
            }
            String executable = bin + File.separator + commandType;
            List<String> full = new ArrayList<>();
            full.add(executable);
            full.addAll(arguments);
            ProcessBuilder pb = new ProcessBuilder(full).redirectErrorStream(true);
            Process p = pb.start();

            List<String> linhas = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                int count = 0;
                while ((line = br.readLine()) != null) {
                    linhas.add(line);
                    long elapsed = (System.currentTimeMillis() - start) / 1000;
                    updateMessage(String.format("[%ds] %s", elapsed, line));
                    if (line.contains("% done")) {
                        try {
                            double perc = Double.parseDouble(line.replaceAll(".*?(\\d+)% done.*", "$1")) / 100.0;
                            updateProgress(perc, 1.0);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    double prog = Math.min(count++ / 200.0, 1.0);
                    updateProgress(prog, 1.0);
                }
            }
            int exit = p.waitFor();
            long total = (System.currentTimeMillis() - start) / 1000;
            if (exit == 0) {
                updateMessage("[SUCESSO] Concluído em " + total + "s");
            } else {
                updateMessage("[ALERTA] Código de saída: " + exit);
            }

            int erros = 0, corr = 0;
            for (String l : linhas) {
                if (l.toLowerCase().contains("error") || l.contains("corrupt")) {
                    erros++;
                }
                if (l.contains("mend") || l.contains("repaired")) {
                    corr++;
                }
            }
            updateMessage(String.format("RESUMO_GFIX|%d|%d|%d", exit, erros, corr));
        } catch (Exception e) {
            updateMessage("[ERRO] " + e.getMessage());
        }
        return null;
    }
}
