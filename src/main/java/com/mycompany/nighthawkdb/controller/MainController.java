package com.mycompany.nighthawkdb.controller;
import javafx.scene.image.Image;
import com.mycompany.nighthawkdb.core.FirebirdMaintenanceTask;
import com.mycompany.nighthawkdb.core.ManutencaoCompletaTask;
import com.mycompany.nighthawkdb.core.DatabaseComparator;
import com.mycompany.nighthawkdb.core.FirebirdVersionDetector;
import com.mycompany.nighthawkdb.db.DatabaseInfoService;
import com.mycompany.nighthawkdb.AppContext;
import java.io.File;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane; // <-- IMPORT NECESSÁRIO ADICIONADO
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

    @FXML
    private ProgressBar myProgressBar;
    @FXML
    private Label myStatusLabel;
    @FXML
    private TextArea terminalLogArea;
    @FXML
    private Label lblCharsetResult;      // charset do banco (exibição futura)
    @FXML
    private Label lblInstanceName;       // nome do arquivo do banco
    @FXML
    private Circle circle3050, circle3051, circle3052, circle53052, circle53055;
    @FXML
    private TextField caminhoBancoTextField;

    @FXML
    private GridPane gridAcoes; // <-- CORREÇÃO: VARIÁVEL DECLARADA AQUI!

    // DBCompiler
    @FXML
    private TextField txtBancoA, txtBancoB;
    @FXML
    private TextArea txtResultadosScript;
    @FXML
    private ListView<String> listViewBancoA;
    @FXML
    private ListView<String> listViewBancoB;
    @FXML
    private ListView<String> listViewCountDiff;   // diferenças de contagem de registros
    @FXML
    private CheckBox chkTables, chkIndexes, chkTriggers, chkProcedures;
    @FXML
    private CheckBox chkCompararDados;

    @FXML
    private VBox sidebarVBox;

    private final DatabaseInfoService infoService = new DatabaseInfoService();
    private final DatabaseComparator comparator = new DatabaseComparator();

    @FXML
    public void initialize() {
        iniciarMonitoramentoPortas();

        int porta = descobrirPortaAtiva();
        AppContext.getInstance().setPortaAtivaFirebird(porta);

        // Destacar aba correta
        if (txtBancoA != null) {
            setActiveTab("Comparador Firebird");
        } else {
            setActiveTab("Firebird/Manutenção");
        }

        // Carregar banco padrão ou do AppContext
        String caminhoSalvo = AppContext.getInstance().getCaminhoBancoSelecionado();
        if (caminhoSalvo.isEmpty()) {
            String caminhoPadrao = "C:\\Fortes\\AC\\AC.fdb";
            File dbPadrao = new File(caminhoPadrao);
            if (dbPadrao.exists()) {
                caminhoSalvo = caminhoPadrao;
                AppContext.getInstance().setCaminhoBancoSelecionado(caminhoSalvo);
            }
        }

        if (!caminhoSalvo.isEmpty()) {
            atualizarCampoCaminho(caminhoSalvo);
            carregarDiagnosticoBanco(caminhoSalvo);
            logTerminal("[SISTEMA] Banco carregado: " + caminhoSalvo);
        } else {
            logTerminal("[SISTEMA] Nenhum banco selecionado. Use 'Selecionar FDB'.");
        }
    }

    private void atualizarCampoCaminho(String path) {
        if (caminhoBancoTextField != null) {
            caminhoBancoTextField.setText(path);
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
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private boolean testarConexaoPorta(String host, int porta) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, porta), 300);
            return true;
        } catch (IOException e) {
            return false;
        }
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
        if (dbPath == null || dbPath.isEmpty()) {
            return;
        }
        new Thread(() -> {
            try {
                int porta = AppContext.getInstance().getPortaAtivaFirebird();
                String dbUrl = "jdbc:firebirdsql://localhost:" + porta + "/" + dbPath.replace("\\", "/");
                String relatorio = infoService.obterPainelInformacoes(dbUrl, "SYSDBA", "masterkey");
                String nomeArquivo = new File(dbPath).getName();
                javafx.application.Platform.runLater(() -> {
                    if (lblInstanceName != null) {
                        lblInstanceName.setText(nomeArquivo);
                    }
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
            String path = file.getAbsolutePath();
            AppContext.getInstance().setCaminhoBancoSelecionado(path);
            atualizarCampoCaminho(path);
            carregarDiagnosticoBanco(path);
            logTerminal("[SISTEMA] Banco selecionado: " + path);
            String versao = FirebirdVersionDetector.detectarVersao(path);
            logTerminal("[INFO] Versão Firebird: " + versao);
        }
    }

    private boolean isBancoSelecionadoValido() {
        String path = AppContext.getInstance().getCaminhoBancoSelecionado();
        if (path == null || path.isEmpty()) {
            logTerminal("[ERRO] Selecione um banco primeiro.");
            return false;
        }
        if (!new File(path).exists()) {
            logTerminal("[ERRO] Arquivo não encontrado: " + path);
            return false;
        }
        return true;
    }

    @FXML
    public void gerenciarCliqueVerificar() {
        if (!isBancoSelecionadoValido()) {
            return;
        }
        executarOperacaoMecanicaCLI("gfix.exe", List.of("-v", "-f", "-user", "sysdba", "-pass", "masterkey",
                AppContext.getInstance().getCaminhoBancoSelecionado()));
    }

    @FXML
    public void gerenciarCliqueCorrigir() {
        if (!isBancoSelecionadoValido()) {
            return;
        }
        executarOperacaoMecanicaCLI("gfix.exe", List.of("-m", "-f", "-user", "sysdba", "-pass", "masterkey",
                AppContext.getInstance().getCaminhoBancoSelecionado()));
    }

    @FXML
    public void gerenciarCliqueSweep() {
        if (!isBancoSelecionadoValido()) {
            return;
        }
        executarOperacaoMecanicaCLI("gfix.exe", List.of("-sweep", "-user", "sysdba", "-pass", "masterkey",
                AppContext.getInstance().getCaminhoBancoSelecionado()));
    }

    @FXML
    public void gerenciarCliqueBackup() {
        if (!isBancoSelecionadoValido()) {
            return;
        }
        String path = AppContext.getInstance().getCaminhoBancoSelecionado();
        String pasta = path.substring(0, path.lastIndexOf(File.separator));
        String backup = pasta + File.separator + "Backup_Manual.fbk";
        executarOperacaoMecanicaCLI("gbak.exe", List.of("-b", "-v", "-user", "sysdba", "-pass", "masterkey", path, backup));
    }

    @FXML
    public void restaurarBackup() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backup (*.fbk)", "*.fbk"));
        File f = fc.showOpenDialog(caminhoBancoTextField.getScene().getWindow());
        if (f != null) {
            String destino = f.getParent() + File.separator + "Restaurado_" + System.currentTimeMillis() + ".fdb";
            executarOperacaoMecanicaCLI("gbak.exe", List.of("-r", "-v", "-p", "8192", "-user", "sysdba", "-pass", "masterkey",
                    f.getAbsolutePath(), destino));
        }
    }

    @FXML
    public void gerenciarCliqueManutencaoCompleta() {
        String path = AppContext.getInstance().getCaminhoBancoSelecionado();
        if (path.isEmpty() || myProgressBar == null) {
            return;
        }

        // Bloqueia os botões enquanto a manutenção completa roda
        if (gridAcoes != null) {
            gridAcoes.setDisable(true);
        }

        ManutencaoCompletaTask macro = new ManutencaoCompletaTask(path);

        // Listener para progresso (evita bind)
        macro.progressProperty().addListener((obs, oldVal, newVal)
                -> myProgressBar.setProgress(newVal.doubleValue()));
        myProgressBar.progressProperty().unbind();
        // Bind do status label (ok, pois é string)
        myStatusLabel.textProperty().bind(macro.messageProperty());

        macro.messageProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                if (newVal.startsWith("CONFIRMAR_TROCA")) {
                    String[] parts = newVal.split("\\|");
                    long orig = Long.parseLong(parts[1]);
                    long novo = Long.parseLong(parts[2]);
                    String msg = String.format("Original (%s): %d bytes\nNovo (%s): %d bytes\nSubstituir?",
                            parts[3], orig, parts[4], novo);
                    javafx.application.Platform.runLater(() -> {
                        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
                        a.setTitle("Confirmação");
                        a.showAndWait().ifPresent(r -> {
                            if (r == ButtonType.YES) {
                                macro.confirmarTroca();
                            } else {
                                macro.cancelarOperacao();
                            }
                        });
                    });
                } else {
                    if (terminalLogArea != null) {
                        terminalLogArea.appendText(newVal + "\n");
                    }
                }
            }
        });

        // Libera a tela quando terminar
        macro.setOnSucceeded(e -> {
            if (gridAcoes != null) {
                gridAcoes.setDisable(false);
            }
        });
        macro.setOnFailed(e -> {
            if (gridAcoes != null) {
                gridAcoes.setDisable(false);
            }
        });
        macro.setOnCancelled(e -> {
            if (gridAcoes != null) {
                gridAcoes.setDisable(false);
            }
        });

        new Thread(macro).start();
    }

    private void executarOperacaoMecanicaCLI(String util, List<String> args) {
        if (gridAcoes != null) {
            gridAcoes.setDisable(true); // Trava a tela
        }
        FirebirdMaintenanceTask task = new FirebirdMaintenanceTask(util, args);

        // O SEGREDO: Soltar (unbind) as barras antigas antes de amarrar a nova
        if (myProgressBar != null) {
            myProgressBar.progressProperty().unbind();
            myProgressBar.progressProperty().bind(task.progressProperty());
        }
        if (myStatusLabel != null) {
            myStatusLabel.textProperty().unbind();
            myStatusLabel.textProperty().bind(task.messageProperty());
        }

        task.messageProperty().addListener((obs, old, newVal) -> {
            if (newVal != null && terminalLogArea != null) {
                if (newVal.startsWith("RESUMO_GFIX")) {
                    String[] p = newVal.split("\\|");
                    logTerminal(String.format("Resumo: Código=%s Erros=%s Correções=%s", p[1], p[2], p[3]));
                } else {
                    logTerminal(newVal); // Usa nosso novo log formatado
                }
            }
        });

        // Libera a tela quando terminar
        task.setOnSucceeded(e -> {
            if (gridAcoes != null) {
                gridAcoes.setDisable(false);
            }
        });
        task.setOnFailed(e -> {
            if (gridAcoes != null) {
                gridAcoes.setDisable(false);
            }
        });
        task.setOnCancelled(e -> {
            if (gridAcoes != null) {
                gridAcoes.setDisable(false);
            }
        });

        new Thread(task).start();
    }

    // DBCompiler
    @FXML
    public void procurarBancoA() {
        txtBancoA.setText(escolherFDB());
    }

    @FXML
    public void procurarBancoB() {
        txtBancoB.setText(escolherFDB());
    }

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

        boolean bTab = chkTables != null && chkTables.isSelected();
        boolean bInd = chkIndexes != null && chkIndexes.isSelected();
        boolean bTrig = chkTriggers != null && chkTriggers.isSelected();
        boolean bProc = chkProcedures != null && chkProcedures.isSelected();

        txtResultadosScript.setText("/* Extraindo metadados... Isso pode demorar alguns segundos... */");
        logTerminal("[DBCOMPILER] Extraindo metadados. Isso pode demorar alguns segundos...");

        new Thread(() -> {
            try {
                DatabaseComparator.ResultadoComparacao resultado
                        = comparator.compararBancos(pathA, pathB, bTab, bInd, bTrig, bProc);

                javafx.application.Platform.runLater(() -> {
                    // VERIFICA SE OS BANCOS SÃO IGUAIS:
                    if (resultado.difsBancoA.isEmpty() && resultado.difsBancoB.isEmpty()) {
                        listViewBancoA.getItems().setAll("✅ Idêntico");
                        listViewBancoB.getItems().setAll("✅ Idêntico");
                        txtResultadosScript.setText("/* =======================================\n   PARABÉNS! Nenhuma diferença encontrada.\n   Os bancos estão 100% IDÊNTICOS.\n   ======================================= */");
                        logTerminal("[DBCOMPILER - SUCESSO] Bancos 100% estruturalmente idênticos.");
                    } else {
                        listViewBancoA.getItems().setAll(resultado.difsBancoA);
                        listViewBancoB.getItems().setAll(resultado.difsBancoB);
                        txtResultadosScript.setText(resultado.scriptDDL);
                        logTerminal("[DBCOMPILER] Foram encontradas divergências estruturais!");
                    }
                });

                if (chkCompararDados != null && chkCompararDados.isSelected()) {
                    Map<String, Long> countsA = contarRegistros(pathA);
                    Map<String, Long> countsB = contarRegistros(pathB);
                    List<String> diffs = new ArrayList<>();
                    for (String tabela : countsA.keySet()) {
                        long ca = countsA.get(tabela);
                        long cb = countsB.getOrDefault(tabela, 0L);
                        if (ca != cb) {
                            diffs.add(tabela + " : A=" + ca + "  B=" + cb);
                        }
                    }
                    javafx.application.Platform.runLater(() -> {
                        if (listViewCountDiff != null) {
                            listViewCountDiff.getItems().setAll(diffs);
                        }
                    });
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> txtResultadosScript.setText("/* ERRO: " + e.getMessage() + " */"));
            }
        }).start();
    }

    private Map<String, Long> contarRegistros(String dbPath) {
        Map<String, Long> map = new HashMap<>();
        int porta = AppContext.getInstance().getPortaAtivaFirebird();
        String url = "jdbc:firebirdsql://localhost:" + porta + "/" + dbPath.replace("\\", "/");
        try (Connection c = DriverManager.getConnection(url, "SYSDBA", "masterkey"); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT rdb$relation_name FROM rdb$relations WHERE rdb$view_blr IS NULL AND (rdb$system_flag IS NULL OR rdb$system_flag=0)")) {
            while (rs.next()) {
                String tabela = rs.getString(1).trim();
                try (Statement s2 = c.createStatement(); ResultSet rs2 = s2.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
                    if (rs2.next()) {
                        map.put(tabela, rs2.getLong(1));
                    }
                } catch (SQLException ignored) {
                }
            }
        } catch (Exception e) {
            logTerminal("[ERRO] contagem: " + e.getMessage());
        }
        return map;
    }

    // Navegação
    @FXML
    public void navegarParaDashboard(ActionEvent e) throws IOException {
        trocarCena("/view/dashboard.fxml", (Node) e.getSource(), "Firebird/Manutenção");
    }

    @FXML
    public void navegarParaCompiler(ActionEvent e) throws IOException {
        trocarCena("/view/dbcompiler.fxml", (Node) e.getSource(), "Comparador Firebird");
    }

    @FXML
    public void abrirMSSQLPlaceholder(ActionEvent e) throws IOException {
        trocarCena("/view/mssql.fxml", (Node) e.getSource(), "MSSQL Tools");
    }

    private void trocarCena(String fxml, Node node, String tabName) throws IOException {
        Scene scene = node.getScene();
        Parent root = AppContext.getInstance().getView(fxml);

        if (root == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            root = loader.load();
            AppContext.getInstance().addView(fxml, root);
        }

        // Recupera a sidebar pela classe CSS e FORÇA a aba correta a ficar "acesa"
        VBox sidebar = (VBox) root.lookup(".sidebar");
        if (sidebar != null) {
            for (Node n : sidebar.getChildren()) {
                if (n instanceof Button btn) {
                    btn.getStyleClass().remove("nav-button-active");
                    if (btn.getText() != null && btn.getText().contains(tabName)) {
                        btn.getStyleClass().add("nav-button-active");
                    }
                }
            }
        }
        scene.setRoot(root);
    }

    public void setActiveTab(String texto) {
        if (sidebarVBox == null) {
            return;
        }
        for (Node n : sidebarVBox.getChildren()) {
            if (n instanceof Button btn) {
                btn.getStyleClass().remove("nav-button-active");
                if (btn.getText() != null && btn.getText().contains(texto)) {
                    btn.getStyleClass().add("nav-button-active");
                }
            }
        }
    }

    @FXML
    public void abrirJanelaSobre() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Sobre");
        a.setHeaderText("Manutenção MSSQL/Firebird - Enterprise Edition");
        a.setContentText("Desenvolvido por Gabriel Levi\nSoluções de TI & Database Maintenance\n\nUtilitário Avançado Multi-SGBD.");
        a.showAndWait();
    }

    private void logTerminal(String msg) {
        com.mycompany.nighthawkdb.core.LoggerService.log(msg); // 1. Grava no ficheiro de Log
        if (terminalLogArea != null) {
            javafx.application.Platform.runLater(() -> {
                terminalLogArea.appendText(msg + "\n");        // 2. Exibe na tela
            });
        }
    }
}
