package com.mycompany.nighthawkdb.controller;

import com.mycompany.nighthawkdb.core.FirebirdMaintenanceTask;
import com.mycompany.nighthawkdb.core.ManutencaoCompletaTask;
import com.mycompany.nighthawkdb.core.DatabaseComparator;
import com.mycompany.nighthawkdb.core.FirebirdVersionDetector;
import com.mycompany.nighthawkdb.db.DatabaseInfoService;
import java.io.File;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import javafx.scene.paint.Color;

public class MainController {

    @FXML private ProgressBar myProgressBar;
    @FXML private Label myStatusLabel;
    @FXML private TextArea terminalLogArea;
    @FXML private Label lblCharsetResult;
    @FXML private Circle circle3050, circle3051, circle3052, circle53052, circle53055;
    @FXML private TextField caminhoBancoTextField;

    // DBCompiler
    @FXML private TextField txtBancoA, txtBancoB;
    @FXML private TextArea txtResultadosScript;
    @FXML private ListView<String> listViewBancoA;
    @FXML private ListView<String> listViewBancoB;
    @FXML private CheckBox chkTables, chkIndexes, chkTriggers, chkProcedures;
    @FXML private CheckBox chkCompararDados;   // novo

    @FXML private VBox sidebarVBox;            // para destaque da sidebar

    private final DatabaseInfoService infoService = new DatabaseInfoService();
    private final DatabaseComparator comparator = new DatabaseComparator();

    private static String caminhoBancoSelecionado = "";
    private static int portaAtivaFirebird = 3050;

    @FXML
    public void initialize() {
        iniciarMonitoramentoPortas();
        portaAtivaFirebird = descobrirPortaAtiva();

        if (caminhoBancoSelecionado.isEmpty()) {
            String caminhoPadrao = "C:\\Fortes\\AC\\AC.fdb";
            File dbPadrao = new File(caminhoPadrao);
            if (dbPadrao.exists()) {
                caminhoBancoSelecionado = caminhoPadrao;
                atualizarCampoCaminho();
                carregarDiagnosticoBanco(caminhoBancoSelecionado);
                logTerminal("[SISTEMA] Banco padrão carregado: " + caminhoPadrao);
            } else {
                logTerminal("[SISTEMA] Banco padrão não encontrado. Use 'Selecionar FDB' para escolher um banco.");
            }
        } else {
            atualizarCampoCaminho();
            carregarDiagnosticoBanco(caminhoBancoSelecionado);
        }
    }

    private void atualizarCampoCaminho() {
        if (caminhoBancoTextField != null) {
            caminhoBancoTextField.setText(caminhoBancoSelecionado);
        }
    }

    private void iniciarMonitoramentoPortas() {
        Thread monitorThread = new Thread(() -> {
            int[] portas = {3050, 3051, 3052, 53052, 53055};
            Circle[] circulos = {circle3050, circle3051, circle3052, circle53052, circle53055};
            while (true) {
                for (int i = 0; i < portas.length; i++) {
                    final int indice = i;
                    boolean ativa = testarConexaoPorta("localhost", portas[i]);
                    javafx.application.Platform.runLater(() -> {
                        if (circulos[indice] != null) {
                            circulos[indice].setFill(ativa ? Color.CHARTREUSE : Color.RED);
                        }
                    });
                }
                try { Thread.sleep(5000); } catch (InterruptedException e) { break; }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private boolean testarConexaoPorta(String host, int porta) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, porta), 300);
            return true;
        } catch (IOException e) { return false; }
    }

    private int descobrirPortaAtiva() {
        int[] portas = {3050, 3051, 3052, 53052, 53055};
        for (int p : portas) {
            if (testarConexaoPorta("localhost", p)) {
                logTerminal("[SISTEMA] Firebird detectado na porta " + p);
                return p;
            }
        }
        logTerminal("[SISTEMA] Nenhuma porta Firebird encontrada!");
        return 3050;
    }

    private void carregarDiagnosticoBanco(String dbPath) {
        if (dbPath == null || dbPath.isEmpty()) return;
        new Thread(() -> {
            try {
                String dbUrl = "jdbc:firebirdsql://localhost:" + portaAtivaFirebird + "/" + dbPath.replace("\\", "/");
                String relatorio = infoService.obterPainelInformacoes(dbUrl, "SYSDBA", "masterkey");
                String nomeArquivo = dbPath.substring(dbPath.lastIndexOf(File.separator) + 1);
                javafx.application.Platform.runLater(() -> {
                    if (lblCharsetResult != null) lblCharsetResult.setText("Instância: " + nomeArquivo);
                    if (terminalLogArea != null) {
                        terminalLogArea.appendText("\n[JDBC] " + relatorio + "\n");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> logTerminal("[ERRO JDBC] " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void selecionarBancoDeDados(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar Banco Firebird");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Firebird (*.fdb)", "*.fdb"));
        File file = fc.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (file != null) {
            caminhoBancoSelecionado = file.getAbsolutePath();
            atualizarCampoCaminho();
            carregarDiagnosticoBanco(caminhoBancoSelecionado);
            logTerminal("[SISTEMA] Banco selecionado: " + caminhoBancoSelecionado);
            String versao = FirebirdVersionDetector.detectarVersao(caminhoBancoSelecionado);
            logTerminal("[INFO] Versão Firebird: " + versao);
        }
    }

    private boolean isBancoSelecionadoValido() {
        if (caminhoBancoSelecionado == null || caminhoBancoSelecionado.isEmpty()) {
            logTerminal("[ERRO] Selecione um banco primeiro.");
            return false;
        }
        if (!new File(caminhoBancoSelecionado).exists()) {
            logTerminal("[ERRO] Arquivo não encontrado: " + caminhoBancoSelecionado);
            return false;
        }
        return true;
    }

    @FXML public void gerenciarCliqueVerificar() {
        if (!isBancoSelecionadoValido()) return;
        executarOperacaoMecanicaCLI("gfix.exe", List.of("-v", "-f", "-user", "sysdba", "-pass", "masterkey", caminhoBancoSelecionado));
    }
    @FXML public void gerenciarCliqueCorrigir() {
        if (!isBancoSelecionadoValido()) return;
        executarOperacaoMecanicaCLI("gfix.exe", List.of("-m", "-f", "-user", "sysdba", "-pass", "masterkey", caminhoBancoSelecionado));
    }
    @FXML public void gerenciarCliqueSweep() {
        if (!isBancoSelecionadoValido()) return;
        executarOperacaoMecanicaCLI("gfix.exe", List.of("-sweep", "-user", "sysdba", "-pass", "masterkey", caminhoBancoSelecionado));
    }
    @FXML public void gerenciarCliqueBackup() {
        if (!isBancoSelecionadoValido()) return;
        String pasta = caminhoBancoSelecionado.substring(0, caminhoBancoSelecionado.lastIndexOf(File.separator));
        String backup = pasta + File.separator + "Backup_Manual.fbk";
        executarOperacaoMecanicaCLI("gbak.exe", List.of("-b", "-v", "-user", "sysdba", "-pass", "masterkey", caminhoBancoSelecionado, backup));
    }
    @FXML public void restaurarBackup() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backup (*.fbk)", "*.fbk"));
        File f = fc.showOpenDialog(caminhoBancoTextField.getScene().getWindow());
        if (f != null) {
            String destino = f.getParent() + File.separator + "Restaurado_" + System.currentTimeMillis() + ".fdb";
            executarOperacaoMecanicaCLI("gbak.exe", List.of("-r", "-v", "-p", "8192", "-user", "sysdba", "-pass", "masterkey", f.getAbsolutePath(), destino));
        }
    }

    @FXML
    public void gerenciarCliqueManutencaoCompleta() {
        if (!isBancoSelecionadoValido() || myProgressBar == null) return;
        ManutencaoCompletaTask macro = new ManutencaoCompletaTask(caminhoBancoSelecionado);
        myProgressBar.progressProperty().bind(macro.progressProperty());
        myStatusLabel.textProperty().bind(macro.messageProperty());
        macro.messageProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                if (newVal.startsWith("CONFIRMAR_TROCA")) {
                    String[] parts = newVal.split("\\|");
                    long orig = Long.parseLong(parts[1]), novo = Long.parseLong(parts[2]);
                    String msg = String.format("Original (%s): %d bytes\nNovo (%s): %d bytes\nSubstituir?", parts[3], orig, parts[4], novo);
                    javafx.application.Platform.runLater(() -> {
                        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
                        a.setTitle("Confirmação");
                        a.showAndWait().ifPresent(r -> {
                            if (r == ButtonType.YES) macro.confirmarTroca();
                            else macro.cancelarOperacao();
                        });
                    });
                } else {
                    if (terminalLogArea != null) terminalLogArea.appendText(newVal + "\n");
                }
            }
        });
        new Thread(macro).start();
    }

    private void executarOperacaoMecanicaCLI(String util, List<String> args) {
        FirebirdMaintenanceTask task = new FirebirdMaintenanceTask(util, args);
        myProgressBar.progressProperty().bind(task.progressProperty());
        myStatusLabel.textProperty().bind(task.messageProperty());
        task.messageProperty().addListener((obs, old, newVal) -> {
            if (terminalLogArea != null && newVal != null) {
                if (newVal.startsWith("RESUMO_GFIX")) {
                    String[] p = newVal.split("\\|");
                    terminalLogArea.appendText(String.format("\nResumo: Código=%s Erros=%s Correções=%s\n", p[1], p[2], p[3]));
                } else {
                    terminalLogArea.appendText(newVal + "\n");
                }
            }
        });
        new Thread(task).start();
    }

    // DBCompiler
    @FXML public void procurarBancoA() { txtBancoA.setText(escolherFDB()); }
    @FXML public void procurarBancoB() { txtBancoB.setText(escolherFDB()); }

    private String escolherFDB() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Firebird (*.fdb)", "*.fdb"));
        File f = fc.showOpenDialog(txtBancoA.getScene().getWindow());
        return f != null ? f.getAbsolutePath() : "";
    }

    @FXML
    public void executarComparacaoIBExpert() {
        String pathA = txtBancoA.getText(), pathB = txtBancoB.getText();
        if (pathA.isEmpty() || pathB.isEmpty()) {
            txtResultadosScript.setText("/* ERRO: selecione dois bancos */");
            return;
        }
        txtResultadosScript.setText("/* Extraindo metadados... */");
        new Thread(() -> {
            try {
                DatabaseComparator.ResultadoComparacao resultado = comparator.compararBancos(pathA, pathB);
                javafx.application.Platform.runLater(() -> {
                    listViewBancoA.getItems().setAll(resultado.difsBancoA);
                    listViewBancoB.getItems().setAll(resultado.difsBancoB);
                    txtResultadosScript.setText(resultado.scriptDDL);
                });

                if (chkCompararDados != null && chkCompararDados.isSelected()) {
                    Map<String, Long> countsA = contarRegistros(pathA);
                    Map<String, Long> countsB = contarRegistros(pathB);
                    List<String> diffCountA = new ArrayList<>();
                    List<String> diffCountB = new ArrayList<>();
                    for (String tabela : countsA.keySet()) {
                        long ca = countsA.get(tabela);
                        long cb = countsB.getOrDefault(tabela, 0L);
                        if (ca != cb) {
                            diffCountA.add(tabela + " = " + ca);
                            diffCountB.add(tabela + " = " + cb);
                        }
                    }
                    javafx.application.Platform.runLater(() -> {
                        listViewBancoA.getItems().addAll(diffCountA);
                        listViewBancoB.getItems().addAll(diffCountB);
                    });
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> txtResultadosScript.setText("/* ERRO: " + e.getMessage() + " */"));
            }
        }).start();
    }

    private Map<String, Long> contarRegistros(String dbPath) {
        Map<String, Long> map = new HashMap<>();
        String url = "jdbc:firebirdsql://localhost:" + portaAtivaFirebird + "/" + dbPath.replace("\\", "/");
        try (Connection c = DriverManager.getConnection(url, "SYSDBA", "masterkey");
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT rdb$relation_name FROM rdb$relations WHERE rdb$view_blr IS NULL AND (rdb$system_flag IS NULL OR rdb$system_flag=0)")) {
            while (rs.next()) {
                String tabela = rs.getString(1).trim();
                try (Statement s2 = c.createStatement();
                     ResultSet rs2 = s2.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
                    if (rs2.next()) map.put(tabela, rs2.getLong(1));
                } catch (SQLException ignored) {}
            }
        } catch (Exception e) {
            logTerminal("[ERRO] contagem: " + e.getMessage());
        }
        return map;
    }

    // Navegação
    @FXML public void navegarParaDashboard(ActionEvent e) throws IOException { trocarCena("/view/dashboard.fxml", (Node) e.getSource()); }
    @FXML public void navegarParaCompiler(ActionEvent e) throws IOException { trocarCena("/view/dbcompiler.fxml", (Node) e.getSource()); }
    @FXML public void abrirMSSQLPlaceholder(ActionEvent e) throws IOException { trocarCena("/view/mssql.fxml", (Node) e.getSource()); }

    private void trocarCena(String fxml, Node node) throws IOException {
        Stage stage = (Stage) node.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        Parent root = loader.load();
        stage.setScene(new Scene(root, 1100, 700));
    }

    // Destaque da sidebar
    public void setActiveTab(String texto) {
    if (sidebarVBox == null) return;
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

    @FXML public void abrirJanelaSobre() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Sobre");
        a.setHeaderText("Manutenção Firebird");
        a.showAndWait();
    }

    private void logTerminal(String msg) {
        if (terminalLogArea != null) terminalLogArea.appendText(msg + "\n");
    }
}