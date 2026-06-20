package com.mycompany.nighthawkdb.core;

import com.mycompany.nighthawkdb.config.ConfigManager;
import javafx.concurrent.Task;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ManutencaoCompletaTask extends Task<Void> {

    private final String dbPathStr;
    private final AtomicBoolean confirmacao = new AtomicBoolean(false);
    private boolean cancelada = false;

    public ManutencaoCompletaTask(String dbPathStr) {
        this.dbPathStr = dbPathStr;
    }

    public void confirmarTroca() {
        confirmacao.set(true);
    }

    public void cancelarOperacao() {
        cancelada = true;
        confirmacao.set(true);
    }

    @Override
    protected Void call() throws Exception {
        File dbFile = new File(dbPathStr);
        if (!dbFile.exists()) {
            updateMessage("[ERRO CRÍTICO] Banco de dados original não localizado.");
            return null;
        }

        String semExt = dbPathStr.substring(0, dbPathStr.lastIndexOf('.'));
        String backup = semExt + ".fbk", temp = semExt + "_temp.fdb", old = semExt + "_Old.fdb";
        String bin = ConfigManager.getFirebirdBinPath();
        String gfix = bin.isEmpty() ? "gfix.exe" : bin + File.separator + "gfix.exe";
        String gbak = bin.isEmpty() ? "gbak.exe" : bin + File.separator + "gbak.exe";

        updateProgress(5, 100);
        updateMessage("[ETAPA 1/5] Verificação de Integridade Física (gfix -v)");
        executar(List.of(gfix, "-v", "-f", "-user", "sysdba", "-pass", "masterkey", dbPathStr));

        updateProgress(20, 100);
        updateMessage("[ETAPA 2/5] Correção de Transações Órfãs (gfix -m)");
        executar(List.of(gfix, "-m", "-f", "-user", "sysdba", "-pass", "masterkey", dbPathStr));

        updateProgress(40, 100);
        updateMessage("[ETAPA 3/5] Garbage Collection (Sweep)");
        executar(List.of(gfix, "-sweep", "-user", "sysdba", "-pass", "masterkey", dbPathStr));

        updateProgress(60, 100);
        updateMessage("[ETAPA 4/5] Backup Compactado de Segurança (gbak -b)");
        executar(List.of(gbak, "-b", "-v", "-user", "sysdba", "-pass", "masterkey", dbPathStr, backup));

        updateProgress(85, 100);
        updateMessage("[ETAPA 5/5] Restauração e Reorganização de Índices (gbak -r)");
        executar(List.of(gbak, "-r", "-v", "-p", "8192", "-user", "sysdba", "-pass", "masterkey", "-REP", backup, temp));

        updateProgress(90, 100);
        Path orig = Path.of(dbPathStr), oldPath = Path.of(old), tempPath = Path.of(temp);
        long tamOrig = Files.size(orig), tamNovo = Files.size(tempPath);
        updateMessage(String.format("CONFIRMAR_TROCA|%d|%d|%s|%s", tamOrig, tamNovo, orig.getFileName(), tempPath.getFileName()));

        while (!confirmacao.get()) {
            Thread.sleep(200);
        }

        if (cancelada) {
            Files.deleteIfExists(tempPath);
            updateMessage("[SISTEMA] Troca de banco cancelada pelo usuário.");
            updateProgress(100, 100);
            return null;
        }

        updateProgress(95, 100);
        Files.deleteIfExists(oldPath);
        updateMessage("[SISTEMA] Arquivo original movido para " + oldPath.getFileName());
        Files.move(orig, oldPath, StandardCopyOption.ATOMIC_MOVE);

        updateMessage("[SISTEMA] Nova base restaurada movida para produção.");
        Files.move(tempPath, orig, StandardCopyOption.ATOMIC_MOVE);

        updateMessage("[SUCESSO] Manutenção Automática finalizada com sucesso!");
        updateProgress(100, 100);
        return null;
    }

    private void executar(List<String> cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Removemos o "updateMessage" linha a linha no log para não travar a barra de progresso principal
                // Deixamos isso para rodar silenciosamente ou logar apenas fisicamente
                com.mycompany.nighthawkdb.core.LoggerService.log("[CMD] " + line);
            }
        }
        int exit = p.waitFor();
        if (exit != 0 && cmd.get(0).contains("gbak.exe")) {
            throw new RuntimeException("Falha na execução do GBAK, código " + exit);
        }
    }
}
