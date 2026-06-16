module com.mycompany.nighthawkdb {
    requires javafx.controls;
    requires javafx.fxml; // Permite o uso de telas em formato FXML
    requires java.sql;    // Necessário para o Jaybird/JDBC no futuro

    // Abre o pacote de controle para o JavaFX conseguir injetar os botões e componentes
    opens com.mycompany.nighthawkdb.controller to javafx.fxml;
    
    exports com.mycompany.nighthawkdb;
}
