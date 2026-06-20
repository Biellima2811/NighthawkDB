package com.mycompany.nighthawkdb.db;

import java.sql.Connection;           // Interface JDBC para conexão com o banco
import java.sql.DriverManager;        // Gerencia a criação de conexões JDBC
import java.sql.ResultSet;            // Representa o resultado de uma consulta SQL
import java.sql.Statement;            // Envia comandos SQL para o banco
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço JDBC para interagir diretamente com o banco de dados rodando comandos
 * SQL. Compatível com Firebird 5.0.
 */
public class DatabaseInfoService {

    /**
     * Conecta ao banco informado e retorna um relatório com informações
     * básicas: charset padrão e total de tabelas de usuário.
     *
     * @param dbUrl URL JDBC (ex: jdbc:firebirdsql://localhost:3050/caminho)
     * @param user Usuário do banco
     * @param password Senha do banco
     * @return String formatada com as informações
     */
    public String obterPainelInformacoes(String dbUrl, String user, String password) {
        StringBuilder infoReport = new StringBuilder();
        // SQL para obter o nome do charset padrão do banco
        String sqlQuery = "SELECT rdb$character_set_name FROM rdb$database";

        // try-with-resources garante que Connection, Statement e ResultSet sejam fechados
        try (Connection conn = DriverManager.getConnection(dbUrl, user, password); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlQuery)) {

            infoReport.append("--- PAINEL DE INFORMAÇÕES DO BANCO (Firebird 5.0) ---\n");
            infoReport.append("Status da Conexão: Ativa e Saudável\n");

            // Se houver resultado, extrai o charset
            if (rs.next()) {
                infoReport.append("Charset Padrão do Banco: ").append(rs.getString(1).trim());
            }

            // Segunda consulta: contar tabelas de usuário (ignora tabelas de sistema)
            String countTablesSql = "SELECT COUNT(*) FROM rdb$relations WHERE rdb$view_blr IS NULL AND (rdb$system_flag IS NULL OR rdb$system_flag = 0)";
            // Reutiliza o Statement para executar nova consulta
            try (ResultSet rsTables = stmt.executeQuery(countTablesSql)) {
                if (rsTables.next()) {
                    infoReport.append("\nTotal de Tabelas de Usuário: ").append(rsTables.getInt(1));
                }
            }

        } catch (Exception e) {
            infoReport.append("Erro ao ler integridade do banco: ").append(e.getMessage());
        }

        return infoReport.toString();
    }

    /**
     * NOVO MÉTODO: Executa uma auditoria de saúde nos índices do banco. Varre
     * as tabelas de sistema procurando índices com unique flag = 1 que estejam
     * marcados como inativos (possivelmente corrompidos).
     *
     * @return Lista de strings com o log da auditoria
     */
    public List<String> verificarSaudeIndices(String dbUrl, String user, String password) {
        List<String> logsAuditoria = new ArrayList<>();

        // SQL: seleciona nomes de índices únicos que estão inativos
        String queryIndices = "SELECT RDB$INDEX_NAME FROM RDB$INDICES WHERE RDB$UNIQUE_FLAG = 1 AND RDB$INDEX_INACTIVE = 1";

        try (Connection conn = DriverManager.getConnection(dbUrl, user, password); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(queryIndices)) {

            logsAuditoria.add("[AUDITORIA] Iniciando checagem de integridade de chaves...");
            boolean encontrouProblema = false;

            // Itera sobre os índices problemáticos encontrados
            while (rs.next()) {
                encontrouProblema = true;
                String nomeIndice = rs.getString(1).trim();
                logsAuditoria.add("[ALERTA CRÍTICO] Índice desativado detectado: " + nomeIndice);
            }

            if (!encontrouProblema) {
                logsAuditoria.add("[SUCESSO] Todos os índices exclusivos estão ativos e íntegros.");
            }

        } catch (Exception e) {
            logsAuditoria.add("[ERRO] Falha ao auditar índices de sistema: " + e.getMessage());
        }

        return logsAuditoria;
    }
}
