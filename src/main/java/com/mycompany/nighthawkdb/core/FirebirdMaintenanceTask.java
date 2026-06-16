package com.mycompany.nighthawkdb.core;
import com.mycompany.nighthawkdb.config.ConfigManager;
import javafx.concurrent.Task;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Task assíncrona responsável por executar os utilitários do Firebird (gbak, gfix)
 * sem travar a interface gráfica do JavaFX.
 */
public class FirebirdMaintenanceTask extends Task<Void>{
   private final String commandType; // Ex: "gbak", "gfix"
   private final List<String> arguments;

    public FirebirdMaintenanceTask(String commandType, List<String> arguments) {
        this.commandType = commandType;
        this.arguments = arguments;
    }
   
    @Override
    protected Void call() throws Exception{
        updateMessage("Iniciando processo...");
        updateProgress(0, 100);
        
        long startTime = System.currentTimeMillis(); // Inicialização do cronômetro
        
        // Monta o caminho correto do executável usando nosso ConfigManager
        String binFolder = ConfigManager.getFirebirdBinPath();
        String executable = binFolder.isEmpty() ? commandType : binFolder + File.separator + commandType;
        
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(executable);
        fullCommand.addAll(arguments);
        
        ProcessBuilder builder = new ProcessBuilder(fullCommand);
        builder.redirectErrorStream(true); // Redireciona erros para a saída padrão para capturarmos tudo
        
        Process process = builder.start();
        
        // Captura a saída de texto gerada pelo processo em tempo real (modo verboso '-v')
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String line;
            int estimatedLinesProcessed = 0;
            
            while ((line = reader.readLine()) != null){
                estimatedLinesProcessed++;
                // Calcula o tempo decorrido até o momento
                long elapsetTime = (System.currentTimeMillis() - startTime) / 1000;
                
                // Atualiza a mensagem na tela do JavaFx
                updateMessage(String.format("[%d] %s", elapsetTime, line));
                
                // Simulação de progresso com base em linhas lidas (ajustar conforme o tamanho do banco)
                double progress = Math.min(estimatedLinesProcessed / 200.0, 1.0);
                
                updateProgress(progress, 1.0);
            }
        }
        int exitCode = process.waitFor();
        long totalTime = (System.currentTimeMillis() - startTime) / 1000;
        
        if (exitCode == 0) {
            updateMessage("Concluído com sucesso em " +  totalTime +  "segundos!");
        } else {
            updateMessage("Falha na execução. Código de erro: " + exitCode);
        }
        return null;
    }
}
