1 - Configuration :
    - Télécharger JavaFX SDK 11 ou supérieur depuis https://gluonhq.com/products/javafx/
    - Décompresser le fichier téléchargé dans un répertoire de votre choix (exemple : D:\JavaFX11)
    - Configurer votre IDE (Eclipse, IntelliJ, NetBeans, etc.) pour utiliser JavaFX en ajoutant le chemin vers le dossier "lib" de JavaFX SDK dans les options de compilation et d'exécution comme un librairie externe.
        Exemple de chemin : D:\JavaFX11\javafx-sdk-11\lib
    - Si vous utilisez un IDE, assurez-vous de :
        - ajouter les vm arguments suivants avant l'exécution :
        --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls,javafx.fxml

        Note : Changer le chemin "D:\JavaFX11\javafx-sdk-11\lib" selon votre installation de JavaFX.


2 - Compilation et Execution :
    Compilation :
        javac -d bin --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls,javafx.fxml (Get-ChildItem -Path src -Recurse -Filter *.java).FullName

        Note : Changer le chemin "D:\JavaFX11\javafx-sdk-11\lib" selon votre installation de JavaFX.

    Execution :
        Executer avec Console View :
            Sans Arguments :
                    java --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls -cp bin MainConsole
                    Note : Changer le chemin "D:\JavaFX11\javafx-sdk-11\lib" selon votre installation de JavaFX.

            Avec Arguments (exemple avec un fichier "r.txt") :
                java --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls -cp bin MainCosole instances/r.txt 10

        Executer avec JavaFX Graphical View :
            Sans Arguments :
                java --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls -cp bin MainFX
                Note : Changer le chemin "D:\JavaFX11\javafx-sdk-11\lib" selon votre installation de JavaFX.

            Avec Arguments (exemple avec un fichier "r.txt") :
                java --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls -cp bin MainFX instances/r.txt 10

        Note : Changer le chemin "D:\JavaFX11\javafx-sdk-11\lib" selon votre installation de JavaFX.
        Note : les fichiers qui contient des instances predefinis des reseaux sont situés dans le dossier "instances"
        Note : le deuxieme argument est optionnel (lambda) et vaut 10 par defaut si non fourni.