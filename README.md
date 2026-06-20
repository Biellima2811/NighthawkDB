# 🦅 NighthawkDB Pro - Enterprise Edition

O **NighthawkDB Pro** é uma ferramenta gráfica avançada (GUI) desenvolvida em **JavaFX** para administração, manutenção e comparação de bancos de dados. Inicialmente focado em ecossistemas corporativos, o sistema oferece suporte a rotinas de otimização tanto para **Firebird** quanto para **Microsoft SQL Server**.

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-Modern_UI-47A248?style=for-the-badge&logo=javafx&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build_Tool-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

---

## ✨ Funcionalidades Principales

O sistema é dividido em três módulos principais de atuação:

### 🔥 1. Firebird / Manutenção
Módulo dedicado à saúde e otimização de bancos de dados `.fdb`. Executa processos nativos em *background* sem congelar a interface.
* **Verificação de Erros (`gfix -v -f`)**: Mapeamento de páginas corrompidas e problemas de integridade.
* **Correção de Erros (`gfix -m -f`)**: Correção de transações órfãs e reparo de banco de dados.
* **Sweep Automático (`gfix -sweep`)**: Coleta de lixo e otimização de performance.
* **Backup e Restore Nativo (`gbak`)**: Geração e restauração de arquivos `.fbk`.
* **Manutenção Automática (Macro)**: Executa um ciclo completo de reparo, sweep, backup e restore, substituindo o arquivo em produção de forma segura (*Atomic Move*).

### ⚖️ 2. Comparador Firebird (DBCompiler)
Ferramenta no estilo *IBExpert* para fazer o "Diff" entre dois bancos de dados.
* **Comparação de Metadados**: Mapeia Tabelas, Índices, Triggers, Procedures, Views, Exceptions e Generators.
* **Geração de Script DDL**: Cria automaticamente o script de migração (com comandos `CREATE`, `DROP`, `ALTER`) para atualizar o "Banco Alvo" para a versão do "Banco de Referência".
* **Auditoria de Dados**: Opção para comparar a quantidade de registros (`COUNT`) entre todas as tabelas de ambos os bancos para validação de migrações.

### 🔷 3. MSSQL Tools
Módulo focado em administração de rotina para Microsoft SQL Server.
* **Dashboard de Saúde**: Leitura de tamanho em disco, status online/offline e contagem de tabelas.
* **Verificação de Integridade**: Execução de `DBCC CHECKDB` e `DBCC CHECKTABLE`.
* **Otimização de Performance**: Rebuild de Índices massivo e Atualização de Estatísticas (`sp_updatestats`).
* **Backup & Restore**: Geração de arquivos `.bak` e restauração segura.

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem**: Java 17+
* **Interface Gráfica**: JavaFX (FXML + CSS Customizado tema Dark/IDE)
* **Gerenciamento de Dependências**: Apache Maven
* **Drivers JDBC**:
  * `org.firebirdsql.jdbc:jaybird` (Firebird)
  * `com.microsoft.sqlserver:mssql-jdbc` (SQL Server)

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
* Java Development Kit (JDK) 11 ou superior (Recomendado 17+).
* Apache Maven instalado e configurado nas variáveis de ambiente.
* Para manutenção Firebird, é necessário ter o `gfix.exe` e `gbak.exe` instalados na máquina. (O sistema possui autodescoberta do caminho da pasta `bin`).

### Rodando via Maven
Para compilar e executar o projeto diretamente via terminal, utilize:

```bash
mvn clean javafx:run
