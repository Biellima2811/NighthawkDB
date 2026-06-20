package com.mycompany.nighthawkdb;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.Parent;

/**
 * Singleton que guarda o estado da aplicação, como o caminho do banco
 * selecionado e a porta ativa do Firebird.
 */
public class AppContext {

    private final Map<String, Parent> viewCache = new HashMap<>();
    private static final AppContext INSTANCE = new AppContext();

    private String caminhoBancoSelecionado = "";
    private int portaAtivaFirebird = 3050;

    private AppContext() {
    }

    public static AppContext getInstance() {
        return INSTANCE;
    }

    public String getCaminhoBancoSelecionado() {
        return caminhoBancoSelecionado;
    }

    public void setCaminhoBancoSelecionado(String caminho) {
        this.caminhoBancoSelecionado = caminho;
    }

    public int getPortaAtivaFirebird() {
        return portaAtivaFirebird;
    }

    public void setPortaAtivaFirebird(int porta) {
        this.portaAtivaFirebird = porta;
    }

    public Parent getView(String fxml) {
        return viewCache.get(fxml);
    }

    public void addView(String fxml, Parent view) {
        viewCache.put(fxml, view);
    }
}
