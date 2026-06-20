package com.mycompany.nighthawkdb.controller;

import javafx.scene.image.Image;
import com.mycompany.nighthawkdb.AppContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;

import java.io.File;
import java.sql.*;
import java.util.*;

public class MssqlController {

    @FXML
    private TextField txtServer, txtPort, txtDatabase, txtUser;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private ProgressBar myProgressBar;
    @FXML
    private Label myStatusLabel;
    @FXML
    private TextArea terminalLogArea;
    @FXML
    private Label lblDbSize, lblDbStatus, lblTableCount;
    @FXML
    private TextField txtTableName;   // para CHECKTABLE
    @FXML
    private VBox sidebarVBox;

    @FXML
    public void initialize() {
        txtServer.setText("localhost");
        txtPort.setText("1433");
        txtDatabase.setText("master");
        txtUser.setText("sa");
        setActiveTab("MSSQL Tools");
        log("[MSSQL] Módulo inicializado.");
    }

    private void setActiveTab(String texto) {
        if (sidebarVBox == null) {
            return;
        }
        for (Node n : sidebarVBox.getChildren()) {
            if (n instanceof Button) {
                Button btn = (Button) n;
                btn.getStyleClass().remove("nav-button-active");
                if (btn.getText().contains(texto)) {
                    btn.getStyleClass().add("nav-button-active");
                }
            }
        }
    }

    @FXML
    public void navegarParaDashboard(ActionEvent e) throws Exception {
        trocarCena("/view/dashboard.fxml", (Node) e.getSource());
    }

    @FXML
    public void navegarParaCompiler(ActionEvent e) throws Exception {
        trocarCena("/view/dbcompiler.fxml", (Node) e.getSource());
    }

    @FXML
    public void abrirJanelaSobre() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Sobre");
        a.setHeaderText("Manutenção MSSQL/Firebird - Enterprise Edition");
        a.setContentText("Desenvolvido por Gabriel Levi\nSoluções de TI & Database Maintenance\n\nUtilitário Avançado Multi-SGBD.");
        a.showAndWait();
    }

    private void trocarCena(String fxml, Node node) throws Exception {
        // Obtém a cena atual (que já está em tela cheia)
        Scene scene = node.getScene();

        // 1. Tenta pegar a tela do Cache
        Parent root = AppContext.getInstance().getView(fxml);

        // 2. Se não existir, carrega e guarda no Cache
        if (root == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            root = loader.load();
            AppContext.getInstance().addView(fxml, root);
        }

        // Substitui apenas o miolo, mantendo a janela perfeitamente maximizada
        scene.setRoot(root);
    }

    private String buildJdbcUrl() {
        return String.format("jdbc:sqlserver://%s:%s;databaseName=%s;integratedSecurity=true;encrypt=false;trustServerCertificate=true",
                txtServer.getText().trim(), txtPort.getText().trim(), txtDatabase.getText().trim());
    }

    private Connection getConnection() throws SQLException {
        // Força a máquina virtual do Java (JVM) a acordar e carregar o driver do SQL Server
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            log("[ERRO CRÍTICO] Driver do SQL Server não foi encontrado no sistema.");
        }

        return DriverManager.getConnection(buildJdbcUrl(), txtUser.getText().trim(), txtPassword.getText().trim());
    }

    private void log(String msg) {
        com.mycompany.nighthawkdb.core.LoggerService.log(msg);
        Platform.runLater(() -> {
            if (terminalLogArea != null) {
                terminalLogArea.appendText(msg + "\n");

                // PERFORMANCE: Limita o terminal a 500 linhas para não travar
                String textoAtual = terminalLogArea.getText();
                int maxCaracteres = 30000;
                if (textoAtual.length() > maxCaracteres) {
                    terminalLogArea.setText(textoAtual.substring(textoAtual.length() - maxCaracteres));
                    terminalLogArea.positionCaret(terminalLogArea.getText().length());
                }
            }
        });
    }

    private void setStatus(String msg) {
        Platform.runLater(() -> {
            if (myStatusLabel != null) {
                myStatusLabel.setText(msg);
            }
        });
    }

    private void setProgressIndeterminate(boolean v) {
        Platform.runLater(() -> {
            if (myProgressBar != null) {
                myProgressBar.setProgress(v ? ProgressBar.INDETERMINATE_PROGRESS : 0);
            }
        });
    }

    @FXML
    public void testaConexao() {
        new Thread(() -> {
            setStatus("Testando...");
            setProgressIndeterminate(true);
            try (Connection c = getConnection()) {
                String info = c.getMetaData().getDatabaseProductName() + " " + c.getMetaData().getDatabaseProductVersion();
                Platform.runLater(() -> lblDbStatus.setText("Online"));
                log("[SUCESSO] " + info);
            } catch (Exception e) {
                log("[ERRO] " + e.getMessage());
                Platform.runLater(() -> lblDbStatus.setText("Offline"));
            } finally {
                setProgressIndeterminate(false);
            }
        }).start();
    }

    @FXML
    public void carregarInformacoesBanco() {
        new Thread(() -> {
            setStatus("Obtendo informações...");
            try (Connection c = getConnection(); Statement s = c.createStatement()) {
                ResultSet rs = s.executeQuery("SELECT CAST(SUM(size*8/1024) AS VARCHAR)+' MB' FROM sys.master_files WHERE database_id=DB_ID()");
                if (rs.next()) {
                    String size = rs.getString(1);
                    Platform.runLater(() -> lblDbSize.setText(size));
                    log("[INFO] Tamanho: " + size);
                }
                rs = s.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE'");
                if (rs.next()) {
                    int t = rs.getInt(1);
                    Platform.runLater(() -> lblTableCount.setText(String.valueOf(t)));
                    log("[INFO] Tabelas: " + t);
                }
            } catch (Exception e) {
                log("[ERRO] " + e.getMessage());
            } finally {
                setProgressIndeterminate(false);
            }
        }).start();
    }

    @FXML
    public void executarCheckDB() {
        executarComandoSQL("DBCC CHECKDB", "DBCC CHECKDB");
    }

    @FXML
    public void executarCheckTable() {
        String t = txtTableName.getText().trim();
        if (t.isEmpty()) {
            log("Informe a tabela.");
            return;
        }
        executarComandoSQL("DBCC CHECKTABLE", "DBCC CHECKTABLE ('" + t + "')");
    }

    @FXML
    public void executarRebuildIndexes() {
        String sql = "DECLARE @sql NVARCHAR(MAX)=''; SELECT @sql += 'ALTER INDEX '+QUOTENAME(i.name)+' ON '+QUOTENAME(OBJECT_SCHEMA_NAME(o.object_id))+'.'+QUOTENAME(o.name)+' REBUILD; ' FROM sys.indexes i JOIN sys.objects o ON i.object_id=o.object_id WHERE o.type='U' AND i.index_id>0; EXEC sp_executesql @sql;";
        executarComandoSQL("Rebuild", sql);
    }

    @FXML
    public void executarUpdateStatistics() {
        executarComandoSQL("Update Stats", "EXEC sp_updatestats;");
    }

    @FXML
    public void executarBackup() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backup (*.bak)", "*.bak"));
        File f = fc.showSaveDialog(txtServer.getScene().getWindow());
        if (f != null) {
            String sql = String.format("BACKUP DATABASE [%s] TO DISK=N'%s' WITH FORMAT,INIT,STATS=10", txtDatabase.getText().trim(), f.getAbsolutePath().replace("\\", "\\\\"));
            executarComandoSQL("Backup", sql);
        }
    }

    @FXML
    public void executarRestore() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backup (*.bak)", "*.bak"));
        File f = fc.showOpenDialog(txtServer.getScene().getWindow());
        if (f != null) {
            String sql = String.format("ALTER DATABASE [%s] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; RESTORE DATABASE [%s] FROM DISK=N'%s' WITH REPLACE,STATS=10; ALTER DATABASE [%s] SET MULTI_USER;",
                    txtDatabase.getText().trim(), txtDatabase.getText().trim(), f.getAbsolutePath().replace("\\", "\\\\"), txtDatabase.getText().trim());
            executarComandoSQL("Restore", sql);
        }
    }

    private void executarComandoSQL(String desc, String sql) {
        setProgressIndeterminate(true);
        setStatus("Executando: " + desc + "...");
        log("[MSSQL] Iniciando -> " + desc);

        new Thread(() -> {
            try (Connection c = getConnection(); Statement s = c.createStatement()) {
                long startTime = System.currentTimeMillis();

                boolean isResult = s.execute(sql);

                if (isResult) {
                    ResultSet rs = s.getResultSet();
                    ResultSetMetaData meta = rs.getMetaData();
                    StringBuilder sb = new StringBuilder();
                    while (rs.next()) {
                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            sb.append(rs.getString(i)).append("\t");
                        }
                        sb.append("\n");
                    }
                    log("[RETORNO DO BANCO]\n" + sb.toString());
                } else {
                    int affectedRows = s.getUpdateCount();
                    log("[MSSQL] Linhas afetadas/processadas: " + affectedRows);
                }

                long totalTime = (System.currentTimeMillis() - startTime) / 1000;
                log("[SUCESSO] Operação " + desc + " finalizada em " + totalTime + " segundos.");
                Platform.runLater(() -> setStatus("Concluído."));
            } catch (Exception e) {
                log("[ERRO CRÍTICO] " + desc + " - " + e.getMessage());
                Platform.runLater(() -> setStatus("Erro na execução!"));
            } finally {
                setProgressIndeterminate(false);
            }
        }).start();
    }

    @FXML
    public void limparTerminal() {
        if (terminalLogArea != null) {
            terminalLogArea.clear();
        }
    }
}
