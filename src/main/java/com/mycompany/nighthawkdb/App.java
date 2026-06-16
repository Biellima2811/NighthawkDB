package com.mycompany.nighthawkdb;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Ponto de entrada principal da aplicação FireDoctor PRO.
 * Responsável por gerir o ciclo de vida do JavaFX e carregar o layout 
 * inicial (Dashboard) contido nos recursos do projeto.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        /*
         * Carrega o ficheiro visual do Dashboard a partir da pasta de recursos (resources).
         * O caminho "/view/dashboard.fxml" aponta diretamente para o ficheiro que desenhámos.
         */
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dashboard.fxml"));
        Parent root = loader.load();
        
        /*
         * Define a cena principal (Scene) injetando o nosso layout e estabelecendo
         * as dimensões padrão recomendadas no CSS (1100 de largura por 700 de altura).
         */
        Scene scene = new Scene(root, 1100, 700);
        
        // Configurações do Palco (Janela principal do Windows)
        stage.setTitle("NighthawkDB Pro - Enterprise Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        // Dispara a infraestrutura nativa do JavaFX
        launch(args);
    }
}