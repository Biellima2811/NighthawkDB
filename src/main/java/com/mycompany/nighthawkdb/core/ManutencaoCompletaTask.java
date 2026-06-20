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
            updateMessage("Banco não existe.");
            return null;
        }

        String semExt = dbPathStr.substring(0, dbPathStr.lastIndexOf('.'));
        String backup = semExt + ".fbk", temp = semExt + "_temp.fdb", old = semExt + "_Old.fdb";
        String bin = ConfigManager.getFirebirdBinPath();
        String gfix = bin.isEmpty() ? "gfix.exe" : bin + File.separator + "gfix.exe";
        String gbak = bin.isEmpty() ? "gbak.exe" : bin + File.separator + "gbak.exe";

        updateMessage("Etapa 1/5: Verificando (gfix -v)");
        executar(List.of(gfix, "-v", "-f", "-user", "sysdba", "-pass", "masterkey", dbPathStr));
        updateMessage("Etapa 2/5: Corrigindo (gfix -m)");
        executar(List.of(gfix, "-m", "-f", "-user", "sysdba", "-pass", "masterkey", dbPathStr));
        updateMessage("Etapa 3/5: Sweep");
        executar(List.of(gfix, "-sweep", "-user", "sysdba", "-pass", "masterkey", dbPathStr));
        updateMessage("Etapa 4/5: Backup");
        executar(List.of(gbak, "-b", "-v", "-user", "sysdba", "-pass", "masterkey", dbPathStr, backup));
        updateMessage("Etapa 5/5: Restore");
        executar(List.of(gbak, "-r", "-v", "-p", "8192", "-user", "sysdba", "-pass", "masterkey", "-REP", backup, temp));

        Path orig = Path.of(dbPathStr), oldPath = Path.of(old), tempPath = Path.of(temp);
        long tamOrig = Files.size(orig), tamNovo = Files.size(tempPath);
        updateMessage(String.format("CONFIRMAR_TROCA|%d|%d|%s|%s", tamOrig, tamNovo, orig.getFileName(), tempPath.getFileName()));

        while (!confirmacao.get()) {
            Thread.sleep(200);
        }
        if (cancelada) {
            Files.deleteIfExists(tempPath);
            updateMessage("Cancelado.");
            return null;
        }

        Files.deleteIfExists(oldPath);
        updateMessage("Renomeando original para " + oldPath.getFileName());
        Files.move(orig, oldPath, StandardCopyOption.ATOMIC_MOVE);
        updateMessage("Movendo novo banco para produção");
        Files.move(tempPath, orig, StandardCopyOption.ATOMIC_MOVE);
        updateMessage("Sucesso! Manutenção concluída.");
        updateProgress(100, 100);
        return null;
    }

    private void executar(List<String> cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                updateMessage(line);
            }
        }
        int exit = p.waitFor();
        if (exit != 0 && cmd.get(0).contains("gbak.exe")) {
            throw new RuntimeException("Falha no gbak, código " + exit);
        }
    }
}
