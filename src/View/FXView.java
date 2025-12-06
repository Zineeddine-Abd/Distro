package View;

import View.GraphicalView;
import javafx.application.Application;

/**
 * Point d'entrée principal pour l'application avec interface graphique JavaFX
 *
 * Usage:
 * - Sans arguments: Lance l'interface avec un réseau vide
 * - Avec fichier: java MainGUI chemin/vers/fichier.txt [lambda]
 *
 * Exemples:
 *   java MainGUI
 *   java MainGUI src/r.txt
 *   java MainGUI src/r.txt 15
 */
public class FXView {
    public static void main(String[] args) {
        // Lance l'application JavaFX
        Application.launch(GraphicalView.class, args);
    }
}