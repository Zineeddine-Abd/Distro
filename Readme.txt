1 - Configuration :
    - Télécharger JavaFX SDK 11 ou supérieur depuis https://gluonhq.com/products/javafx/
    - Décompresser le fichier téléchargé dans un répertoire de votre choix (exemple : D:\JavaFX11)
    - Configurer votre IDE (Eclipse, IntelliJ, NetBeans, etc.) pour utiliser JavaFX en ajoutant le chemin vers le dossier "lib" de JavaFX SDK dans les options de compilation et d'exécution comme un librairie externe.
        Exemple de chemin : D:\JavaFX11\javafx-sdk-11\lib
    - Si vous utilisez un IDE, assurez-vous de :
        - ajouter les vm arguments suivants avant l'exécution :
        --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls,javafx.fxml

        Note : -------------------------------------------------------------------------------
         Changer le chemin "D:\JavaFX11\javafx-sdk-11\lib" selon votre installation de JavaFX.
        --------------------------------------------------------------------------------------


2 - Compilation et Execution :
    Classes Principales :
        - MainConsole : pour exécuter l'application en mode console.
        - MainFX : pour exécuter l'application en mode graphique JavaFX.

    Compilation :
        Powershell (Windows):
        javac -d bin --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls,javafx.fxml (Get-ChildItem -Path src -Recurse -Filter *.java).FullName

        CMD (Windows):
        javac -d bin --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls,javafx.fxml src\**\*.java

        Linux / macOS (bash / zsh) :
        javac -d bin --module-path /chemin/javafx/lib --add-modules javafx.controls,javafx.fxml $(find src -name "*.java")

        Note : -------------------------------------------------------------------------------
         Changer le chemin "D:\JavaFX11\javafx-sdk-11\lib" selon votre installation de JavaFX.
        --------------------------------------------------------------------------------------

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

        Note : --------------------------------------------------------------------------------------------------
         - Changer le chemin "D:\JavaFX11\javafx-sdk-11\lib" selon votre installation de JavaFX.
         - Les fichiers qui contient des instances predefinis des reseaux sont situés dans le dossier "instances"
         - Le deuxieme argument est optionnel (lambda) et vaut 10 par defaut si non fourni.
        ---------------------------------------------------------------------------------------------------------

    Fonctionnalités implementées : (100%)
        - Lecture et parsing des fichiers d'instances de réseaux.
        — Implementation d'un algorithme stochastique inspiré de fameux algorithme genetique avec une adaptation pertinente a notre probleme.
        - Implementation d'une interface graphique avec JavaFX pour visualiser les réseaux.
        - Implementation des tests unitaires pour valider les composants critiques de l'application.
        - Gestion des erreurs et des exceptions.
        - Une Architecture MVC modulaire et extensible avec des classes bien définies pour chaque composant et separation des responsabilites.
        - Respect des bonnes pratiques de programmation orientée objet.
        - Utilisation correcte des paramètres de la ligne de commande avec documentation de la compilation et l'exécution.
        - Une bonne qualité de l’interface textuelle et graphique.
        - Documentation complète du code avec des commentaires clairs et concis.

    Algorithme de resolution :