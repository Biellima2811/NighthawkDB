package com.mycompany.nighthawkdb.core;

import com.mycompany.nighthawkdb.config.ConfigManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FirebirdVersionDetector {

    /**
     * Executa gbak -z no banco informado e retorna a versão detectada (ex:
     * "2.5", "3.0"). Retorna string vazia se não conseguir.
     */
    public static String detectarVersao(String dbPath) {
        String bin = ConfigManager.getFirebirdBinPath();
        String gbak;
        if (bin.isEmpty()) {
            gbak = "gbak.exe";
        } else {
            gbak = bin + java.io.File.separator + "gbak.exe";
        }

        List<String> cmd = List.of(gbak, "-z", "-user", "sysdba", "-pass", "masterkey", dbPath);
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Exemplo de linha: "gbak: version WI-V2.5.9.27139 Firebird 2.5"
                    if (line.contains("Firebird")) {
                        String[] parts = line.split(" ");
                        for (int i = 0; i < parts.length; i++) {
                            if (parts[i].equalsIgnoreCase("Firebird") && i + 1 < parts.length) {
                                return parts[i + 1]; // retorna "2.5", "3.0", etc.
                            }
                        }
                    }
                }
            }
            proc.waitFor();
        } catch (Exception e) {
            // log
        }
        return "";
    }
}
