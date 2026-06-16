package com.mycompany.nighthawkdb.config;
import java.io.*;
import java.util.Properties;
/**
 * Gerenciador de Configurações do FireDoctor.
 * Responsável por salvar e recuperar propriedades como o caminho da pasta BIN do Firebird.
 */
public class ConfigManager {
    // Nome do arquivo de configurações que será salvo na pasta do usuario
    private static final String CONFIG_FILE = System.getProperty("user.home") + File.separator + "nighthawkdb.properties";
    private static final Properties properties = new Properties();
    
    // Bloco estático para carregar o arquivo assim que a classe for invocada
    static {
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
     * Recupera o caminho salvo da pasta BIN do Firebird.
     * @return String contendo o caminho ou valor vazio se não existir.
     */
    public static String getFirebirdBinPath(){
        return properties.getProperty("firebird.bin.path", "");
    }
    
    /**
     * Salva o caminho da pasta BIN do Firebird de forma persistente.
     * @param path O caminho absoluto selecionado pelo usuário.
     */
    public static void saveFirebirdBinPath(String path){
        properties.setProperty("firebird.bin.path", path);
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)){
            properties.store(output, "Configurações do FireDoctor");
        } catch (IOException e) {
            System.err.println("Erro ao salvar configurações: " +  e.getMessage());
        }
    }
}
