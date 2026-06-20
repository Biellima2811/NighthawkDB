package com.mycompany.nighthawkdb;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Ponto de entrada da aplicação Manutenção Firebird.
 * Responsável por iniciar o JavaFX e carregar a tela inicial do Dashboard.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Carrega o arquivo FXML da interface principal (Dashboard)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dashboard.fxml"));
        Parent root = loader.load();   // Converte o FXML em árvore de componentes

        // Define a cena com tamanho padrão 1100x700
        Scene scene = new Scene(root, 1100, 700);

        // Nome da janela alterado para "Manutenção Firebird"
        stage.setTitle("Manutenção Firebird - Enterprise Edition");
        stage.setScene(scene);
        stage.show();   // Exibe a janela
    }

    public static void main(String[] args) {
        // Lança a aplicação JavaFX (chama start internamente)
        launch(args);
    }
}