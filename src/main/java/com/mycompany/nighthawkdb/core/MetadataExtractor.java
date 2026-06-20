package com.mycompany.nighthawkdb.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Responsável por parsear o arquivo SQL gerado pelo isql -x e extrair
 * listas de objetos (tabelas, índices, triggers, procedures, views, generators, exceções).
 */
public class MetadataExtractor {

    /**
     * Representa um objeto do banco com seu nome e o DDL completo.
     */
    public static class DatabaseObject {
        public final String type;   // TABLE, INDEX, TRIGGER, PROCEDURE, VIEW, GENERATOR, EXCEPTION
        public final String name;
        public final String ddl;    // Script completo de criação

        public DatabaseObject(String type, String name, String ddl) {
            this.type = type;
            this.name = name;
            this.ddl = ddl;
        }

        @Override
        public String toString() {
            return type + " " + name;
        }
    }

    /**
     * Parseia o arquivo DDL e retorna uma lista de DatabaseObject.
     */
    public static List<DatabaseObject> extrairObjetos(String caminhoArquivoSQL) throws IOException {
        List<DatabaseObject> objetos = new ArrayList<>();
        StringBuilder blocoAtual = new StringBuilder();
        String tipoAtual = null;
        String nomeAtual = null;

        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivoSQL))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String linhaTrim = linha.trim();

                // Ignora comentários e linhas vazias
                if (linhaTrim.isEmpty() || linhaTrim.startsWith("/*") || linhaTrim.startsWith("--")) {
                    continue;
                }

                // Detecta início de um novo objeto
                if (linhaTrim.startsWith("CREATE TABLE") || linhaTrim.startsWith("CREATE INDEX") ||
                    linhaTrim.startsWith("CREATE OR ALTER TRIGGER") || linhaTrim.startsWith("CREATE TRIGGER") ||
                    linhaTrim.startsWith("CREATE OR ALTER PROCEDURE") || linhaTrim.startsWith("CREATE PROCEDURE") ||
                    linhaTrim.startsWith("CREATE VIEW") || linhaTrim.startsWith("CREATE GENERATOR") ||
                    linhaTrim.startsWith("CREATE EXCEPTION")) {

                    // Salva o objeto anterior, se houver
                    if (tipoAtual != null && nomeAtual != null) {
                        objetos.add(new DatabaseObject(tipoAtual, nomeAtual, blocoAtual.toString()));
                    }

                    // Reinicia para o novo objeto
                    blocoAtual = new StringBuilder();
                    blocoAtual.append(linha).append("\n");

                    // Determina o tipo e extrai o nome
                    if (linhaTrim.startsWith("CREATE TABLE")) {
                        tipoAtual = "TABLE";
                        nomeAtual = extrairNomeSimples(linhaTrim, "CREATE TABLE");
                    } else if (linhaTrim.startsWith("CREATE INDEX")) {
                        tipoAtual = "INDEX";
                        nomeAtual = extrairNomeSimples(linhaTrim, "CREATE INDEX");
                    } else if (linhaTrim.startsWith("CREATE OR ALTER TRIGGER") || linhaTrim.startsWith("CREATE TRIGGER")) {
                        tipoAtual = "TRIGGER";
                        nomeAtual = extrairNomeTrigger(linhaTrim);
                    } else if (linhaTrim.startsWith("CREATE OR ALTER PROCEDURE") || linhaTrim.startsWith("CREATE PROCEDURE")) {
                        tipoAtual = "PROCEDURE";
                        nomeAtual = extrairNomeSimples(linhaTrim, "PROCEDURE");
                    } else if (linhaTrim.startsWith("CREATE VIEW")) {
                        tipoAtual = "VIEW";
                        nomeAtual = extrairNomeSimples(linhaTrim, "VIEW");
                    } else if (linhaTrim.startsWith("CREATE GENERATOR")) {
                        tipoAtual = "GENERATOR";
                        nomeAtual = extrairNomeSimples(linhaTrim, "GENERATOR");
                    } else if (linhaTrim.startsWith("CREATE EXCEPTION")) {
                        tipoAtual = "EXCEPTION";
                        nomeAtual = extrairNomeSimples(linhaTrim, "EXCEPTION");
                    }
                } else if (linhaTrim.startsWith("ALTER TABLE")) {
                    // ALTER TABLE pode aparecer separado, mas no isql -x geralmente já está dentro do CREATE TABLE
                    // Vamos tratá-lo como parte do objeto anterior (se for TABLE)
                    if ("TABLE".equals(tipoAtual)) {
                        blocoAtual.append(linha).append("\n");
                    }
                } else if (linhaTrim.equals("^")) {
                    // Terminador padrão do isql (^) – ignorar
                } else {
                    // Continuação do bloco atual
                    if (tipoAtual != null) {
                        blocoAtual.append(linha).append("\n");
                    }
                }
            }

            // Não esquecer o último objeto
            if (tipoAtual != null && nomeAtual != null) {
                objetos.add(new DatabaseObject(tipoAtual, nomeAtual, blocoAtual.toString()));
            }
        }

        return objetos;
    }

    /**
     * Extrai o nome simples após uma palavra-chave (ex: "CREATE TABLE NOME").
     */
    private static String extrairNomeSimples(String linha, String palavraChave) {
        String resto = linha.substring(linha.indexOf(palavraChave) + palavraChave.length()).trim();
        // Remove caracteres não alfanuméricos do início (ex: aspas)
        resto = resto.replaceAll("^\"", "").trim();
        int fim = resto.indexOf(' ');
        if (fim == -1) fim = resto.indexOf('(');
        if (fim == -1) fim = resto.length();
        return resto.substring(0, fim).replace("\"", "").trim();
    }

    /**
     * Extrai o nome de um trigger (trata "CREATE TRIGGER nome FOR tabela").
     */
    private static String extrairNomeTrigger(String linha) {
        String base = linha;
        if (base.startsWith("CREATE OR ALTER TRIGGER")) {
            base = base.substring("CREATE OR ALTER TRIGGER".length()).trim();
        } else if (base.startsWith("CREATE TRIGGER")) {
            base = base.substring("CREATE TRIGGER".length()).trim();
        }
        int fim = base.indexOf(' ');
        if (fim == -1) fim = base.length();
        return base.substring(0, fim).replace("\"", "").trim();
    }
}