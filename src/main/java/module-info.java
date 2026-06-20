module com.mycompany.nighthawkdb {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.firebirdsql.jaybird;   // Jaybird é um módulo válido
    requires com.microsoft.sqlserver.jdbc;

    opens com.mycompany.nighthawkdb to javafx.fxml;
    opens com.mycompany.nighthawkdb.controller to javafx.fxml;

    exports com.mycompany.nighthawkdb;
    exports com.mycompany.nighthawkdb.core;
}