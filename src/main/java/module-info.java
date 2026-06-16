module com.mycompany.nighthawkdb {
    requires javafx.controls;
    requires javafx.fxml;
    
    // ESTA É A LINHA QUE FALTOU PARA O BANCO DE DADOS FUNCIONAR:
    requires java.sql; 

    // Permite que o JavaFX acesse as suas telas e controladores
    opens com.mycompany.nighthawkdb to javafx.fxml;
    opens com.mycompany.nighthawkdb.controller to javafx.fxml;
    
    // Exporta os pacotes para que o sistema consiga executá-los
    exports com.mycompany.nighthawkdb;
    exports com.mycompany.nighthawkdb.core;
}