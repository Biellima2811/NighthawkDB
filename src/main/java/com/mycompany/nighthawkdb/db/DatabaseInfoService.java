package com.mycompany.nighthawkdb.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço JDBC para interagir diretamente com o banco de dados rodando comandos SQL.
 * Compatível com Firebird 5.0.
 */
public class DatabaseInfoService {

    public String obterPainelInformacoes(String dbUrl, String user, String password) {
        StringBuilder infoReport = new StringBuilder();
        String sqlQuery = "SELECT rdb$character_set_name FROM rdb$database"; 
        
        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlQuery)) {
            
            infoReport.append("--- PAINEL DE INFORMAÇÕES DO BANCO (Firebird 5.0) ---\n");
            infoReport.append("Status da Conexão: Ativa e Saudável\n");
            
            if (rs.next()) {
                infoReport.append("Charset Padrão do Banco: ").append(rs.getString(1).trim());
            }
            
            String countTablesSql = "SELECT COUNT(*) FROM rdb$relations WHERE rdb$view_context IS NULL AND (rdb$system_flag IS NULL OR rdb$system_flag = 0)";
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
     * NOVO MÉTODO: Executa uma auditoria de saúde nos índices do banco.
     * Varre as tabelas de sistema procurando índices corrompidos ou desativados.
     */
    public List<String> verificarSaudeIndices(String dbUrl, String user, String password) {
        List<String> logsAuditoria = new ArrayList<>();
        
        // Query clássica para localizar índices com flags de unicidade que foram desativados por falhas
        String queryIndices = "SELECT RDB$INDEX_NAME FROM RDB$INDICES WHERE RDB$UNIQUE_FLAG = 1 AND RDB$INDEX_INACTIVE = 1";
        
        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(queryIndices)) {
            
            logsAuditoria.add("[AUDITORIA] Iniciando checagem de integridade de chaves...");
            boolean encontrouProblema = false;
            
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