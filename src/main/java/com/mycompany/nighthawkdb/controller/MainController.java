package com.mycompany.nighthawkdb.controller;

import com.mycompany.nighthawkdb.core.FirebirdMaintenanceTask;
import com.mycompany.nighthawkdb.core.ManutencaoCompletaTask;
import com.mycompany.nighthawkdb.core.DatabaseComparator;
import com.mycompany.nighthawkdb.db.DatabaseInfoService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;

/**
 * Classe Controladora Principal Unificada. Gerencia os eventos visuais do
 * Dashboard e do DBCompiler, invocando as regras de negócio em background para
 * manter a interface fluida.
 */
public class MainController {

    // Componentes injetados do dashboard.fxml
    @FXML
    private ProgressBar myProgressBar;

    @FXML
    private Label myStatusLabel;

    @FXML
    private TextArea terminalLogArea;

    @FXML
    private Label lblCharsetResult;

    @FXML
    private Label lblTotalTables;

    // Instância do serviço JDBC para consultas nativas no Firebird
    private final DatabaseInfoService infoService = new DatabaseInfoService();

    private static String caminhoBancoSelecionado = "";

    @FXML
    private javafx.scene.control.ListView<String> listViewBancoA;

    @FXML
    private javafx.scene.control.ListView<String> listViewBancoB;

    private final DatabaseComparator comparator = new DatabaseComparator();

    /**
     * O método initialize() é um gatilho nativo do JavaFX. Ele é executado
     * automaticamente assim que o FXML termina de carregar, ideal para carregar
     * dados iniciais e conectar ao banco.
     */
    @FXML
    public void initialize() {
        // Só carrega o banco padrão se for a primeira vez que o software abre
        if (caminhoBancoSelecionado.isEmpty()) {
            System.out.println("NighthawkDB inicializado. Carregando base de dados padrão...");
            caminhoBancoSelecionado = "C:\\Fortes\\AC\\AC.fdb";
            carregarDiagnosticoBanco(caminhoBancoSelecionado);
        } else {
            // Se já tem um banco na memória global, apenas atualiza a UI
            carregarDiagnosticoBanco(caminhoBancoSelecionado);
        }
    }

    /**
     * Carrega e atualiza o painel de informações via JDBC de forma totalmente
     * dinâmica e assíncrona para qualquer banco de dados selecionado.
     *
     * @param dbPath Caminho absoluto do arquivo .fdb selecionado
     */
    private void carregarDiagnosticoBanco(String dbPath) {
        if (dbPath == null || dbPath.isEmpty()) {
            return;
        }

        // Thread em background para garantir que a consulta SQL ao Firebird 5.0 não trave a interface
        Thread dbDiagnosticsThread = new Thread(() -> {
            try {
                /*
                 * Ajuste de String de Conexão: O Firebird via JDBC exige que as barras invertidas 
                 * do Windows (\) sejam convertidas em barras normais (/) para a URL de rede.
                 */
                String formatoUrl = dbPath.replace("\\", "/");
                String dbUrl = "jdbc:firebirdsql://localhost:3050/" + formatoUrl;
                String usuario = "SYSDBA";
                String senha = "masterkey";

                // Executa a varredura lendo as tabelas de sistema do Firebird
                String relatorioDiagnostico = infoService.obterPainelInformacoes(dbUrl, usuario, senha);

                // Extrai apenas o nome do arquivo para exibir de forma elegante na tela
                String nomeArquivo = dbPath.substring(dbPath.lastIndexOf(java.io.File.separator) + 1);

                // Sincroniza a resposta de volta para a Thread principal do JavaFX
                javafx.application.Platform.runLater(() -> {
                    if (lblCharsetResult != null) {
                        lblCharsetResult.setText("Instância: " + nomeArquivo);
                    }
                    if (terminalLogArea != null) {
                        terminalLogArea.appendText("\n[SISTEMA] Diagnóstico estrutural atualizado para: " + nomeArquivo + "\n");
                        terminalLogArea.appendText(relatorioDiagnostico + "\n");
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    if (lblCharsetResult != null) {
                        lblCharsetResult.setText("Result: Erro ao conectar à base");
                    }
                });
            }
        });

        dbDiagnosticsThread.setDaemon(true);
        dbDiagnosticsThread.start();
    }

    /**
     * Método genérico e reutilizável para disparar qualquer comando de
     * manutenção do Firebird. Centraliza a lógica evitando repetição de código
     * (Princípio DRY - Don't Repeat Yourself).
     */
    private void executarOperacaoMecanica(String utilitario, List<String> argumentos) {
        if (myProgressBar == null || myStatusLabel == null) {
            return;
        }

        // Cria a tarefa assíncrona customizada.
        FirebirdMaintenanceTask task = new FirebirdMaintenanceTask(utilitario, argumentos);

        // Vincula a reatividade dos componentes visuais à task em backuground
        myProgressBar.progressProperty().bind(task.progressProperty());
        myStatusLabel.textProperty().bind(task.messageProperty());

        // Adiciona um listener para escutar as linhas geradas pelo terminal do firebird
        task.messageProperty().addListener((obs, oldVal, newVal) -> {
            if (terminalLogArea != null && newVal != null) {
                terminalLogArea.appendText(newVal + "\n");
            }
        });
        // Dispara o processo numa Thread dedicada para não congelar o Scene Builder/App
        Thread backgroundThread = new Thread(task);
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

    /**
     * Evento do botão "Verificar Erros"
     */
    @FXML
    public void gerenciarCliqueVerificar() {
        // 1. Executa o comando de prompt gfix padrão
        List<String> args = List.of("-v", "-f", "-user", "sysdba", "-pass", "masterkey", "C:\\Fortes\\AC\\AC.FDB");
        executarOperacaoMecanica("gfix.exe", args);

        // 2. Dispara a nossa verificação de saúde lógica via JDBC em uma Thread paralela
        Thread auditoriaThread = new Thread(() -> {
            String dbUrl = "jdbc:firebirdsql://localhost:3050/C:/Fortes/AC/AC.FDB";
            List<String> logs = infoService.verificarSaudeIndices(dbUrl, "SYSDBA", "masterkey");

            // Retorna os resultados para a Thread principal atualizar a interface de log
            javafx.application.Platform.runLater(() -> {
                if (terminalLogArea != null) {
                    terminalLogArea.appendText("\n--- RESULTADO DA AUDITORIA DE SAÚDE ---\n");
                    for (String linhaLog : logs) {
                        terminalLogArea.appendText(linhaLog + "\n");
                    }
                    terminalLogArea.appendText("---------------------------------------\n");
                }
            });
        });

        auditoriaThread.setDaemon(true);
        auditoriaThread.start();
    }

    /**
     * Evento do botão "Corrigir Erros"
     */
    @FXML
    public void gerenciarCliqueCorrigir() {
        List<String> args = List.of("-m", "-f", "-user", "sysdba", "-pass", "masterkey", "C:\\Fortes\\AC\\AC.FDB");
        executarOperacaoMecanica("gfix.exe", args);
    }

    /**
     * Evento do botão "Executar Sweep"
     */
    @FXML
    public void gerenciarCliqueSweep() {
        List<String> args = List.of("-sweep", "-user", "sysdba", "-pass", "masterkey", "C:\\Fortes\\AC\\AC.FDB");
        executarOperacaoMecanica("gfix.exe", args);
    }

    /**
     * Evento do botão "Fazer Backup"
     */
    @FXML
    public void gerenciarCliqueBackup() {
        List<String> args = List.of("-b", "-v", "-user", "sysdba", "-pass", "masterkey", "C:\\Fortes\\AC\\AC.FDB", "C:\\FireDoctor\\Backup.fbk");
        executarOperacaoMecanica("gbak.exe", args);
    }

    /**
     * Evento acionado ao clicar no botão "Manutenção Automática". Executa em
     * sequência assíncrona todas as fases de reparo e aplica o Safe File Swap.
     */
    @FXML
    public void gerenciarCliqueManutencaoCompleta() {
        if (myProgressBar == null || myStatusLabel == null) {
            return;
        }

        // Caminho do banco configurado no escopo (Exemplo padrão do seu projeto)
        String bancoAlvoPath = "C:\\Fortes\\AC\\AC.FDB";

        // Instancia a nova macro tarefa customizada
        ManutencaoCompletaTask macroTask = new ManutencaoCompletaTask(bancoAlvoPath);

        // Vincula os componentes visuais de forma reativa e atômica
        myProgressBar.progressProperty().bind(macroTask.progressProperty());
        myStatusLabel.textProperty().bind(macroTask.messageProperty());

        // Escuta os logs textuais e anexa no TextArea em tempo real
        macroTask.messageProperty().addListener((obs, oldVal, newVal) -> {
            if (terminalLogArea != null && newVal != null) {
                terminalLogArea.appendText(newVal + "\n");
            }
        });

        // Dispara em Thread paralela dedicada para manter o painel responsivo
        Thread threadMacro = new Thread(macroTask);
        threadMacro.setDaemon(true);
        threadMacro.start();
    }

    @FXML
    public void gerenciarCliqueSincronizar() {
        if (listViewBancoA == null || listViewBancoB == null) {
            return;
        }

        listViewBancoA.getItems().clear();
        listViewBancoB.getItems().clear();

        // Simulação elegante enquanto não criamos a extração real
        listViewBancoA.getItems().addAll("--- PRONTO PARA EXTRAÇÃO DDL ---", "Tabela: FORNECEDORES", "Index: IDX_NOME");
        listViewBancoB.getItems().addAll("--- PRONTO PARA EXTRAÇÃO DDL ---", "Tabela: FORNECEDORES", "Index: IDX_NOME_NOVO");

        if (terminalLogArea != null) {
            terminalLogArea.appendText("[DBCOMPILER] Módulo inicializado e aguardando integração com ISQL.exe\n");
        }
    }

    /**
     * Abre uma janela nativa de seleção de arquivos (FileChooser) para escolher
     * o banco .fdb do cliente.
     */
    @FXML
    public void selecionarBancoDados(javafx.event.ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Selecionar Banco de Dados Firebird");

        // Filtra para exibir apenas arquivos de extensão de banco Firebird
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Firebird Databases (*.fdb)", "*.fdb")
        );

        // Obtém o palco (Stage) atual para ancorar a janela de arquivos
        javafx.scene.Node node = (javafx.scene.Node) event.getSource();
        javafx.stage.Stage stage = (javafx.stage.Stage) node.getScene().getWindow();

        java.io.File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            this.caminhoBancoSelecionado = file.getAbsolutePath();
            if (terminalLogArea != null) {
                terminalLogArea.appendText("[SISTEMA] Base de dados selecionada para operações: " + caminhoBancoSelecionado + "\n");
                carregarDiagnosticoBanco(this.caminhoBancoSelecionado);
            }
        }
    }

    /**
     * Transiciona o ecrã atual do aplicativo para a interface do DBCompiler.
     * Captura o evento de clique para identificar a janela ativa de forma
     * dinâmica.
     */
    @FXML
    public void navegarParaCompiler(javafx.event.ActionEvent event) throws IOException {
        // Captura o componente que disparou o clique e passa para o alternador de cena
        javafx.scene.Node botaoDisparador = (javafx.scene.Node) event.getSource();
        trocarCena("/view/dbcompiler.fxml", botaoDisparador);
    }

    /**
     * Transiciona o ecrã atual do aplicativo de volta para o Dashboard.
     */
    @FXML
    public void navegarParaDashboard(javafx.event.ActionEvent event) throws IOException {
        javafx.scene.Node botaoDisparador = (javafx.scene.Node) event.getSource();
        trocarCena("/view/dashboard.fxml", botaoDisparador);
    }

    /**
     * Método auxiliar privado responsável por realizar a troca de cenas (Views)
     * de forma genérica e segura contra NullPointerException.
     */
    private void trocarCena(String fxmlPath, javafx.scene.Node nodoVisual) throws IOException {
        // Obtém a janela (stage) atual a partir do nodo visual que disparou a ação
        Stage stage = (Stage) nodoVisual.getScene().getWindow();

        // Carrega o novo arquivo estrutural FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        // Substitui a cena preservando o dimensionamento padrão do Dashboard
        Scene scene = new Scene(root, 1100, 700);
        stage.setScene(scene);
    }

    /**
     * Exibe a caixa de diálogo informativa de créditos do sistema.
     */
    @FXML
    public void abrirJanelaSobre() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sobre o NighthawkDB Pro");
        alert.setHeaderText("NighthawkDB Pro - Enterprise Edition");
        alert.setContentText("Desenvolvido por Gabriel Levi\n"
                + "Soluções de TI & Database Maintenance\n\n"
                + "Um utilitário avançado para otimização, manutenção completa, "
                + "saúde de índices e validação estrutural de servidores Firebird 2.5 até 5.0.");
        alert.showAndWait();
    }
}
