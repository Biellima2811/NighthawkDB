package com.mycompany.nighthawkdb.core;

import com.mycompany.nighthawkdb.config.ConfigManager;
import javafx.concurrent.Task;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Task assíncrona responsável por gerenciar a manutenção completa do banco de
 * dados. Executa sequencialmente os utilitários de checagem lixeira, backup e
 * restauração. Ao final, realiza a substituição segura dos arquivos físicos do
 * cliente.
 */
public class ManutencaoCompletaTask extends Task<Void> {

    private final String dbPathStr;

    public String getDbPathStr() {
        return dbPathStr;
    }

    /**
     * Construtor da Task de manutenção macro.
     *
     * @param dbPathStr O caminho absoluto do banco selecionado (Ex:
     * C:\Bancos\CLIENTE_AC_123.fdb)
     */
    public ManutencaoCompletaTask(String dbPathStr) {
        this.dbPathStr = dbPathStr;
    }

    @Override
    protected Void call() throws Exception {
        File dbFile = new File(dbPathStr);
        if (!dbFile.exists()) {
            updateMessage("Erro Crítico: O arquivo de banco de dados original não existe.");
            updateProgress(0, 100);
            return null;
        }

        // Descobre dinamicamente os nomes dos caminhos derivados sem a extensão .fdb
        String absolutoSemExtensao = dbFile.getAbsolutePath().substring(0, dbFile.getAbsolutePath().lastIndexOf("."));
        String backupPath = absolutoSemExtensao + ".fbk";
        String tempRestorePath = absolutoSemExtensao + "_temp.fdb";
        String oldDbPath = absolutoSemExtensao + "_Old.fdb";

        // Localiza a pasta BIN configurada de forma persistente pelo usuário
        String binFolder = ConfigManager.getFirebirdBinPath();
        String gfix = binFolder.isEmpty() ? "gfix.exe" : binFolder + File.separator + "gfix.exe";
        String gbak = binFolder.isEmpty() ? "gbak.exe" : binFolder + File.separator + "gbak.exe";

        // ETAPA 1: Verificar Erros de Integridade Física
        updateMessage("Etapa 1/5: Verificando integridade física das páginas (gfix -v)...");
        updateProgress(10, 100);
        executarSubProcesso(List.of(gfix, "-v", "-f", "-user", "sysdba", "-pass", "masterkey", dbPathStr));

        // ETAPA 2: Liberar Liberação e Marcar Correções de Páginas Mutiladas
        updateMessage("Etapa 2/5: Corrigindo e liberando amarrações de transações (gfix -m)...");
        updateProgress(30, 100);
        executarSubProcesso(List.of(gfix, "-m", "-f", "-user", "sysdba", "-pass", "masterkey", dbPathStr));

        // ETAPA 3: Limpeza Completa da Lixeira de Transações (Garbage Collection)
        updateMessage("Etapa 3/5: Executando varredura e limpeza de lixeira ativa (gfix -sweep)...");
        updateProgress(50, 100);
        executarSubProcesso(List.of(gfix, "-sweep", "-user", "sysdba", "-pass", "masterkey", dbPathStr));

        // ETAPA 4: Geração do Arquivo de Dump/Backup Compactado
        updateMessage("Etapa 4/5: Compactando dados e gerando arquivo de segurança (gbak -b)...");
        updateProgress(70, 100);
        executarSubProcesso(List.of(gbak, "-b", "-v", "-user", "sysdba", "-pass", "masterkey", dbPathStr, backupPath));

        // ETAPA 5: Restauração Estrutural no Arquivo Temporário
        updateMessage("Etapa 5/5: Reorganizando índices e reconstruindo tabelas (gbak -r)...");
        updateProgress(90, 100);
        executarSubProcesso(List.of(gbak, "-r", "-v", "-p", "8192", "-user", "sysdba", "-pass", "masterkey", "-REP", backupPath, tempRestorePath));

        // =========================================================================
        // FASE CRÍTICA DA OPERAÇÃO: SAFE FILE SWAP (SUBSTITUIÇÃO DE ARQUIVOS)
        // =========================================================================
        updateMessage("Fase Final: Sincronizando substituição segura de arquivos no disco...");

        Path caminhoOriginal = Path.of(dbPathStr);
        Path caminhoBancoAntigoOld = Path.of(oldDbPath);
        Path caminhoTemporarioRestaurado = Path.of(tempRestorePath);

        // 1. Remove uma sobra de arquivo _Old caso o usuário já tenha rodado a manutenção antes
        Files.deleteIfExists(caminhoBancoAntigoOld);

        // 2. Transforma o banco atual de produção em arquivo histórico _Old
        updateMessage("Renomeando base atual para " + caminhoBancoAntigoOld.getFileName() + "...");
        Files.move(caminhoOriginal, caminhoBancoAntigoOld, StandardCopyOption.ATOMIC_MOVE);

        // 3. Move o banco temporário reestruturado para assumir o nome original oficial
        updateMessage("Base restaurada e higienizada assumindo produção oficial...");
        Files.move(caminhoTemporarioRestaurado, caminhoOriginal, StandardCopyOption.ATOMIC_MOVE);

        updateMessage("Sucesso: Manutenção Concluída! Banco operacional sob o nome original.");
        updateProgress(100, 100);

        return null;
    }
    /**
     * Executa de forma atômica e encapsulada cada comando CLI do Firebird.
     */
    private void executarSubProcesso(List<String> comando) throws Exception{
        ProcessBuilder builder = new ProcessBuilder(comando);
        Process process = builder.start();
        
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String line;
            while ((line = reader.readLine()) != null) {                
                // Envia os outputs parciais linha por linha para alimentar o TextArea de logs
                updateMessage(line);
            }
        }
        int exitCode = process.waitFor();
        // gfix pode retornar códigos de saída de aviso caso ache erros, o que é normal.
        // Já o gbak exige código de saída 0 para garantir que o backup/restore não corrompeu.
        if (exitCode != 0 && comando.get(0).contains("gbak.exe")) {
            throw new RuntimeException("Falha na subetapa de backup/restauração do Firebird. Código: " + exitCode);
        }
    }
}
