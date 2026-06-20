package com.mycompany.nighthawkdb;

/**
 * Classe utilitária simples para obter informações sobre a versão do Java e do
 * JavaFX em execução. Útil para logs e diagnósticos.
 */
public class SystemInfo {

    /**
     * Retorna a versão do Java Runtime (ex: "17.0.2")
     */
    public static String javaVersion() {
        // System.getProperty("java.version") busca a propriedade do sistema
        return System.getProperty("java.version");
    }

    /**
     * Retorna a versão do JavaFX (ex: "17.0.2")
     */
    public static String javafxVersion() {
        // Propriedade definida pelo JavaFX no classpath
        return System.getProperty("javafx.version");
    }
}
