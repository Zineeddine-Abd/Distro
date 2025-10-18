import Controller.AppController;
import Model.Reseau;
import View.ConsoleView;

public class Main {

    public static void main(String[] args) {

        Reseau reseau = new Reseau();
        AppController controller = new AppController(reseau);
        ConsoleView view = new ConsoleView(controller);

        view.start();
    }
}