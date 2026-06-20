package com.mycompany.nighthawkdb.core;

import com.mycompany.nighthawkdb.config.ConfigManager;
import java.io.File;
import java.sql.*;
import java.util.*;

public class MssqlComparator {

    public static List<String> getTables(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"});
        while (rs.next()) {
            tables.add(rs.getString("TABLE_NAME"));
        }
        return tables;
    }

    public String extrairMetadados(String dbPath) throws Exception {
        File tempFile = File.createTempFile("metadados_", ".sql");
        String outputFile = tempFile.getAbsolutePath();
        extrairMetadadosComIsql(dbPath, outputFile);
        return outputFile;
    }

    /**
     * Compara dois bancos e retorna um relatório de diferenças, juntamente com
     * o script DDL para sincronizar o Banco B com o A.
     */
    public ResultadoComparacao compararBancos(String pathBancoA, String pathBancoB) throws Exception {
        // Extrai os metadados de ambos
        String arqA = extrairMetadados(pathBancoA);
        String arqB = extrairMetadados(pathBancoB);

        // Parseia os objetos
        List<MetadataExtractor.DatabaseObject> objetosA = MetadataExtractor.extrairObjetos(arqA);
        List<MetadataExtractor.DatabaseObject> objetosB = MetadataExtractor.extrairObjetos(arqB);

        // Remove arquivos temporários
        new File(arqA).delete();
        new File(arqB).delete();

        // Mapa (tipo -> nome -> ddl) para cada banco
        Map<String, Map<String, String>> mapA = agruparPorTipo(objetosA);
        Map<String, Map<String, String>> mapB = agruparPorTipo(objetosB);

        StringBuilder script = new StringBuilder();
        List<String> diferencasVisuaisA = new ArrayList<>();
        List<String> diferencasVisuaisB = new ArrayList<>();

        // Ordem de criação (importante para dependências)
        String[] ordemTipos = {"TABLE", "INDEX", "VIEW", "GENERATOR", "EXCEPTION", "PROCEDURE", "TRIGGER"};

        for (String tipo : ordemTipos) {
            Map<String, String> objetosTipoA = mapA.getOrDefault(tipo, new HashMap<>());
            Map<String, String> objetosTipoB = mapB.getOrDefault(tipo, new HashMap<>());

            for (String nome : objetosTipoA.keySet()) {
                if (!objetosTipoB.containsKey(nome)) {
                    // Objeto presente apenas no Banco A
                    String ddl = objetosTipoA.get(nome);
                    script.append("-- ").append(tipo).append(" '").append(nome).append("' ausente no Banco B\n");
                    script.append(ddl).append("\n\n");
                    diferencasVisuaisA.add(tipo + ": " + nome + " (ausente em B)");
                }
                // (pode-se adicionar comparação de alterações, mas é complexo)
            }

            for (String nome : objetosTipoB.keySet()) {
                if (!objetosTipoA.containsKey(nome)) {
                    // Objeto presente apenas no Banco B (não gera script, só informa)
                    diferencasVisuaisB.add(tipo + ": " + nome + " (ausente em A)");
                }
            }
        }

        return new ResultadoComparacao(script.toString(), diferencasVisuaisA, diferencasVisuaisB);
    }

    private Map<String, Map<String, String>> agruparPorTipo(List<MetadataExtractor.DatabaseObject> objetos) {
        Map<String, Map<String, String>> mapa = new HashMap<>();
        for (MetadataExtractor.DatabaseObject obj : objetos) {
            mapa.computeIfAbsent(obj.type, k -> new HashMap<>()).put(obj.name, obj.ddl);
        }
        return mapa;
    }

    /**
     * Executa o isql.exe para gerar o DDL.
     */
    private void extrairMetadadosComIsql(String dbPath, String outputFile) throws Exception {
        String binFolder = ConfigManager.getFirebirdBinPath();
        String isqlExecutable = binFolder.isEmpty() ? "isql.exe" : binFolder + java.io.File.separator + "isql.exe";

        List<String> comando = List.of(
                isqlExecutable,
                "-user", "sysdba",
                "-pass", "masterkey",
                "-x",
                "-o", outputFile,
                dbPath
        );

        ProcessBuilder builder = new ProcessBuilder(comando);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("isql falhou ao extrair metadados. Código: " + exitCode);
        }
    }

    // Classe interna para resultado
    public static class ResultadoComparacao {

        public final String scriptDDL;
        public final List<String> difsBancoA;  // Coisas que existem só em A
        public final List<String> difsBancoB;  // Coisas que existem só em B

        public ResultadoComparacao(String scriptDDL, List<String> difsA, List<String> difsB) {
            this.scriptDDL = scriptDDL;
            this.difsBancoA = difsA;
            this.difsBancoB = difsB;
        }
    }
}
