
package com.tf.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainMenuController {
    @FXML private Button btnSingle;
    @FXML private Label lblStatus;

    @FXML
    public void initialize() {
        lblStatus.setText("Bienvenido a Tutti-Frutti (demo)");
    }

    @FXML
    public void onSingleClicked() {
        lblStatus.setText("Modo un jugador seleccionado (demo)") ;
    }
}
