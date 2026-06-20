package com.mycompany.nighthawkdb;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

/**
 * Ponto de entrada da aplicação Manutenção Firebird. Responsável por iniciar o
 * JavaFX e carregar a tela inicial do Dashboard.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dashboard.fxml"));
        Parent root = loader.load();

        AppContext.getInstance().addView("/view/dashboard.fxml", root);

        Scene scene = new Scene(root, 1100, 700);

        stage.setTitle("Manutenção Firebird - Enterprise Edition");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setMaximized(true);
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/view/images/logo.png")));
        } catch (Exception e) {
            System.err.println("Ícone não encontrado. O sistema iniciará com o ícone padrão.");
        }
        stage.show();
    }

    public static void main(String[] args) {
        // Lança a aplicação JavaFX (chama start internamente)
        launch(args);
    }
}
