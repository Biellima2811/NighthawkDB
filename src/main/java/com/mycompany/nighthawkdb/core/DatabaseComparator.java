package com.mycompany.nighthawkdb.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList; // Adicionado
import java.util.List;      // Adicionado
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Mecanismo robusto de comparação estrutural de bancos Firebird. Resolve o bug
 * de falso-positivo de linhas deslocadas do script Python.
 */
public class DatabaseComparator {

    /**
     * Compara dois arquivos SQL gerados pelo dump de metadados do ISQL. Utiliza
     * ordenação natural (TreeSet) para garantir que a ordem das linhas não
     * quebre a comparação.
     */
    public List<String> compararMetadados(String pathMeta1, String pathMeta2) throws IOException {
        // Carrega todas as linhas eliminando espaços em branco extras e linhas vazias
        TreeSet<String> meta1Normalizado = Files.lines(Path.of(pathMeta1))
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("/*")) // Ignora comentários do Firebird
                .collect(Collectors.toCollection(TreeSet::new));

        TreeSet<String> meta2Normalizado = Files.lines(Path.of(pathMeta2))
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("/*"))
                .collect(Collectors.toCollection(TreeSet::new));

        List<String> diferencas = new ArrayList<>();

        // Encontra o que tem no Banco 1 que está faltando no Banco 2
        for (String linha : meta1Normalizado) {
            if (!meta2Normalizado.contains(linha)) {
                diferencas.add("Exclusivo no Banco 1: " + linha);
            }
        }

        // Encontra o que tem no Banco 2 que está faltando no Banco 1
        for (String linha : meta2Normalizado) {
            if (!meta1Normalizado.contains(linha)) {
                diferencas.add("Exclusivo no Banco 2: " + linha);
            }
        }

        return diferencas;
    }

    /**
     * Executa o utilitário isql.exe em background para extrair o esquema DDL de
     * um banco de dados.
     *
     * @param dbPath Caminho físico do arquivo .fdb
     * @param outputFile Arquivo de texto .sql que receberá o dump dos metadados
     */
    private void extrairMetadadosComIsql(String dbPath, String outputFile) throws Exception {
        String binFolder = com.mycompany.nighthawkdb.config.ConfigManager.getFirebirdBinPath();
        String isqlExecutable = binFolder.isEmpty() ? "isql.exe" : binFolder + java.io.File.separator + "isql.exe";

        // Monta o comando de extração estrutural (-x extrai metadados, -o redireciona a saída)
        List<String> comando = List.of(
                isqlExecutable,
                "-user", "sysdba",
                "-pass", "masterkey",
                "-x", // Extrair definições de metadados
                "-o", outputFile, // Enviar o resultado direto para o arquivo de destino
                dbPath
        );

        ProcessBuilder builder = new ProcessBuilder(comando);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("O isql falhou ao extrair metadados. Código: " + exitCode);
        }
    }
}
