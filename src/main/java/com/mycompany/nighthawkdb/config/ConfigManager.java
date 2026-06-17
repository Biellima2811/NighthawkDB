package com.mycompany.nighthawkdb.config;

import java.io.*;
import java.util.Properties;

/**
 * MENTORIA: O ConfigManager é o nosso "cérebro" de configurações.
 * Em Java, evitamos usar o "os.environ" do Python. Em vez disso, 
 * guardamos o caminho absoluto e passamos ele direto para o ProcessBuilder.
 */
public class ConfigManager {
    
    private static final String CONFIG_FILE = System.getProperty("user.home") + File.separator + "nighthawkdb.properties";
    private static final Properties properties = new Properties();
    
    // Lista de pastas onde o Firebird costuma ser instalado (trazido do seu Python)
    private static final String[] CAMINHOS_COMUNS = {
        "C:\\Fortes\\Firebird_2_5\\bin",
        "C:\\Fortes\\Firebird_2_5\\F\\bin",
        "C:\\Fortes\\Firebird\\Firebird_2_5\\bin",
        "C:\\Program Files (x86)\\Firebird\\Firebird_2_5\\bin",
        "C:\\Program Files\\Firebird\\Firebird_2_5\\bin"
    };
    
    static {
        carregarConfiguracoes();
    }

    private static void carregarConfiguracoes() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                properties.load(input);
            } catch (IOException e) {
                System.err.println("Erro ao carregar configurações: " + e.getMessage());
            }
        }
    }
    
    /**
     * Tenta retornar o caminho guardado. Se estiver vazio, aciona a 
     * autodescoberta para facilitar a vida dos técnicos.
     */
    public static String getFirebirdBinPath() {
        String pathAtual = properties.getProperty("firebird.bin.path", "");
        
        if (pathAtual.isEmpty()) {
            return descobrirCaminhoFirebird();
        }
        return pathAtual;
    }
    
    /**
     * MENTORIA: Este método varre a lista de diretórios. É o equivalente
     * direto ao seu método `acessar_diretorio_firebird()` do Python.
     */
    private static String descobrirCaminhoFirebird() {
        for (String caminho : CAMINHOS_COMUNS) {
            File pastaBin = new File(caminho);
            // Verifica se a pasta existe e se o gbak está lá dentro
            if (pastaBin.exists() && new File(pastaBin, "gbak.exe").exists()) {
                System.out.println("[ConfigManager] Autodescoberta: Firebird encontrado em " + caminho);
                saveFirebirdBinPath(caminho); // Guarda para as próximas vezes
                return caminho;
            }
        }
        return ""; // Retorna vazio se falhar redondamente
    }
    
    public static void saveFirebirdBinPath(String path) {
        properties.setProperty("firebird.bin.path", path);
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Configurações Corporativas do NighthawkDB");
        } catch (IOException e) {
            System.err.println("Erro ao salvar configurações: " +  e.getMessage());
        }
    }
}