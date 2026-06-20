package com.mycompany.nighthawkdb.config;

import java.io.*;
import java.util.Properties;

/**
 * Gerencia o arquivo de configuração que armazena o caminho da pasta BIN do Firebird.
 * Caso a pasta não esteja configurada, tenta descobrir automaticamente.
 */
public class ConfigManager {

    private static final String CONFIG_FILE =
            System.getProperty("user.home") + File.separator + "manutencaofirebird.properties"; // Nome amigável
    private static final Properties properties = new Properties();

    // Caminhos comuns de instalação do Firebird (Windows)
    private static final String[] CAMINHOS_COMUNS = {
        "C:\\Fortes\\Firebird_2_5\\bin",
        "C:\\Fortes\\Firebird_2_5\\F\\bin",
        "C:\\Fortes\\Firebird\\Firebird_2_5\\bin",
        "C:\\Program Files (x86)\\Firebird\\Firebird_2_5\\bin",
        "C:\\Program Files\\Firebird\\Firebird_2_5\\bin"
    };

    static {
        carregarConfiguracoes();   // Lê o arquivo de propriedades assim que a classe é carregada
    }

    /**
     * Lê o arquivo de propriedades do disco, se existir.
     */
    private static void carregarConfiguracoes() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                properties.load(input);
                System.out.println("[ConfigManager] Configurações carregadas de: " + CONFIG_FILE);
            } catch (IOException e) {
                System.err.println("[ConfigManager] Erro ao ler arquivo: " + e.getMessage());
            }
        } else {
            System.out.println("[ConfigManager] Nenhum arquivo de configuração encontrado. Será feita autodescoberta.");
        }
    }

    /**
     * Retorna o caminho da pasta BIN do Firebird. Se não estiver salvo,
     * tenta descobrir automaticamente e salva no arquivo.
     */
    public static String getFirebirdBinPath() {
        String pathAtual = properties.getProperty("firebird.bin.path", "");
        if (pathAtual.isEmpty()) {
            System.out.println("[ConfigManager] Caminho da BIN não definido. Iniciando autodescoberta...");
            String novo = descobrirCaminhoFirebird();
            if (!novo.isEmpty()) {
                return novo;
            } else {
                System.err.println("[ConfigManager] ATENÇÃO: Não foi possível localizar a pasta BIN do Firebird.");
                return "";
            }
        }
        return pathAtual;
    }

    /**
     * Percorre a lista de caminhos comuns e verifica se o gbak.exe existe.
     */
    private static String descobrirCaminhoFirebird() {
        for (String caminho : CAMINHOS_COMUNS) {
            File pastaBin = new File(caminho);
            File gbak = new File(pastaBin, "gbak.exe");
            if (pastaBin.exists() && gbak.exists()) {
                System.out.println("[ConfigManager] Firebird encontrado em: " + caminho);
                saveFirebirdBinPath(caminho);
                return caminho;
            }
        }
        System.err.println("[ConfigManager] Nenhum dos caminhos comuns contém o Firebird.");
        return "";
    }

    /**
     * Salva o caminho da pasta BIN no arquivo de propriedades.
     */
    public static void saveFirebirdBinPath(String path) {
        properties.setProperty("firebird.bin.path", path);
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Configuração da Manutenção Firebird");
            System.out.println("[ConfigManager] Caminho salvo com sucesso.");
        } catch (IOException e) {
            System.err.println("[ConfigManager] Erro ao salvar arquivo: " + e.getMessage());
        }
    }
}