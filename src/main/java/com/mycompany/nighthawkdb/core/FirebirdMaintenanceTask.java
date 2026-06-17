package com.mycompany.nighthawkdb.core;

import com.mycompany.nighthawkdb.config.ConfigManager;
import javafx.concurrent.Task;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FirebirdMaintenanceTask extends Task<Void> {
    private final String commandType;
    private final List<String> arguments;

    public FirebirdMaintenanceTask(String commandType, List<String> arguments) {
        this.commandType = commandType;
        this.arguments = arguments;
    }

    @Override
    protected Void call() {
        updateMessage("Iniciando processo: " + commandType + "...");
        updateProgress(0, 100);
        long startTime = System.currentTimeMillis();

        try {
            String binFolder = ConfigManager.getFirebirdBinPath();
            
            // MENTORIA: Interrupção preventiva. Se a pasta for vazia, nem tentamos 
            // iniciar o processo, poupando a aplicação de um "crash".
            if (binFolder.isEmpty()) {
                updateMessage("[ERRO CRÍTICO] Pasta BIN do Firebird não encontrada automaticamente.");
                updateMessage("Por favor, configure o caminho manualmente nas opções do sistema.");
                return null; 
            }
            
            // Se a pasta bin não estiver configurada, tenta rodar o comando puro 
            // assumindo que os técnicos configuraram o PATH do Windows.
            String executable = binFolder.isEmpty() ? commandType : binFolder + File.separator + commandType;

            List<String> fullCommand = new ArrayList<>();
            fullCommand.add(executable);
            fullCommand.addAll(arguments);

            ProcessBuilder builder = new ProcessBuilder(fullCommand);
            builder.redirectErrorStream(true);

            updateMessage("[SISTEMA] Executando: " + String.join(" ", fullCommand));
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int estimatedLinesProcessed = 0;

                while ((line = reader.readLine()) != null) {
                    estimatedLinesProcessed++;
                    long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                    
                    updateMessage(String.format("[%ds] %s", elapsedTime, line));
                    
                    double progress = Math.min(estimatedLinesProcessed / 200.0, 1.0);
                    updateProgress(progress, 1.0);
                }
            }

            int exitCode = process.waitFor();
            long totalTime = (System.currentTimeMillis() - startTime) / 1000;

            if (exitCode == 0) {
                updateMessage("[SUCESSO] Operação concluída em " + totalTime + " segundos.");
            } else {
                updateMessage("[ALERTA] Processo finalizado com código de erro: " + exitCode);
            }

        } catch (java.io.IOException e) {
            updateMessage("[ERRO CRÍTICO] Executável não encontrado. Verifique se o Firebird está instalado ou configure a pasta BIN.");
            updateMessage("Detalhe técnico: " + e.getMessage());
        } catch (Exception e) {
            updateMessage("[ERRO FATAL] Falha inesperada na Thread de manutenção.");
            updateMessage("Detalhe técnico: " + e.getMessage());
        }
        
        return null;
    }
}