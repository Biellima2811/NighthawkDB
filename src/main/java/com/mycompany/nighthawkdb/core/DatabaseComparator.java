package com.mycompany.nighthawkdb.core;

import com.mycompany.nighthawkdb.config.ConfigManager;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class DatabaseComparator {

    public String extrairMetadados(String dbPath) throws Exception {
        File tmp = File.createTempFile("meta_", ".sql");
        extrairMetadadosComIsql(dbPath, tmp.getAbsolutePath());
        return tmp.getAbsolutePath();
    }

    public ResultadoComparacao compararBancos(String pathA, String pathB) throws Exception {
        String arqA = extrairMetadados(pathA), arqB = extrairMetadados(pathB);
        List<MetadataExtractor.DatabaseObject> objA = MetadataExtractor.extrairObjetos(arqA);
        List<MetadataExtractor.DatabaseObject> objB = MetadataExtractor.extrairObjetos(arqB);
        new File(arqA).delete(); new File(arqB).delete();

        Map<String, Map<String, String>> mapA = agrupar(objA), mapB = agrupar(objB);
        StringBuilder script = new StringBuilder(), scriptReverso = new StringBuilder();
        List<String> difA = new ArrayList<>(), difB = new ArrayList<>();
        String[] ordem = {"TABLE", "INDEX", "VIEW", "GENERATOR", "EXCEPTION", "PROCEDURE", "TRIGGER"};

        for (String tipo : ordem) {
            Map<String, String> ta = mapA.getOrDefault(tipo, Collections.emptyMap());
            Map<String, String> tb = mapB.getOrDefault(tipo, Collections.emptyMap());
            for (String nome : ta.keySet()) {
                if (!tb.containsKey(nome)) {
                    script.append("-- ").append(tipo).append(" ").append(nome).append(" ausente em B\n").append(ta.get(nome)).append("\n\n");
                    difA.add(tipo + ": " + nome + " (ausente em B)");
                }
            }
            for (String nome : tb.keySet()) {
                if (!ta.containsKey(nome)) {
                    difB.add(tipo + ": " + nome + " (ausente em A)");
                    scriptReverso.append("DROP ").append(tipo).append(" \"").append(nome).append("\";\n");
                }
            }
        }
        return new ResultadoComparacao(script.toString(), scriptReverso.toString(), difA, difB);
    }

    private Map<String, Map<String, String>> agrupar(List<MetadataExtractor.DatabaseObject> lista) {
        Map<String, Map<String, String>> mapa = new HashMap<>();
        for (var obj : lista) mapa.computeIfAbsent(obj.type, k -> new HashMap<>()).put(obj.name, obj.ddl);
        return mapa;
    }

    private void extrairMetadadosComIsql(String dbPath, String out) throws Exception {
        String bin = ConfigManager.getFirebirdBinPath();
        String isql = bin.isEmpty() ? "isql.exe" : bin + File.separator + "isql.exe";
        ProcessBuilder pb = new ProcessBuilder(isql, "-user", "sysdba", "-pass", "masterkey", "-x", "-o", out, dbPath);
        Process p = pb.start();
        if (p.waitFor() != 0) throw new RuntimeException("isql falhou");
    }

    public static class ResultadoComparacao {
        public final String scriptDDL, scriptReverso;
        public final List<String> difsBancoA, difsBancoB;
        public ResultadoComparacao(String s, String r, List<String> a, List<String> b) {
            scriptDDL = s; scriptReverso = r; difsBancoA = a; difsBancoB = b;
        }
    }
}