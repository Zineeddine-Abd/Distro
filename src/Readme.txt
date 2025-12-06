Compilation :
    javac -d bin --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls,javafx.fxml (Get-ChildItem -Path src -Recurse -Filter *.java).FullName
    Note : Changer le chemin "D:\JavaFX11\javafx-sdk-11\lib" selon votre installation de JavaFX.

Execution :
    Executer avec Console View :
        Sans Arguments :
                java --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls -cp bin MainConsole
                Note : Changer le chemin "D:\JavaFX11\javafx-sdk-11\lib" selon votre installation de JavaFX.

            Avec Arguments (exemple avec un fichier "r.txt") :
                java --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls -cp bin MainCosole src/r.txt 10

    Executer avec JavaFX Graphical View :
        Sans Arguments :
            java --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls -cp bin MainFX
            Note : Changer le chemin "D:\JavaFX11\javafx-sdk-11\lib" selon votre installation de JavaFX.

        Avec Arguments (exemple avec un fichier "r.txt") :
            java --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls -cp bin MainFX src/r.txt 10