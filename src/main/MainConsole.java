package main;

import main.Controller.AppController;
import main.Model.Reseau;
import main.View.ConsoleView;

public class MainConsole {

    public static void main(String[] args) {
        Reseau reseau = new Reseau();
        AppController controller = new AppController(reseau);
        ConsoleView view = new ConsoleView(controller);
        view.start(args);
    }
}