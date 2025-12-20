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
            (elle contient pas le main, mais elle contient la méthode start() qui est le point d'entrée de l'application JavaFX)
            (View)

    -------------------------------------------------------------------------
    COMPILATION
    -------------------------------------------------------------------------
    Assurez-vous que le dossier "bin" existe à la racine du projet (sinon : mkdir bin).

        Powershell (Windows):
        javac -d bin --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls,javafx.fxml (Get-ChildItem -Path src -Recurse -Filter *.java).FullName

        CMD (Windows):
        javac -d bin --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls,javafx.fxml src\Model\*.java src\View\*.java src\Controller\*.java src\Algorithms\*.java src\Exceptions\*.java src\*.java

        Linux / macOS (bash / zsh) :
        Option 1  (manuelle) :
        javac -d bin --module-path /chemin/javafx/lib --add-modules javafx.controls,javafx.fxml src/*.java src/Model/*.java src/View/*.java src/Controller/*.java src/Algorithms/*.java src/Exceptions/*.java

        Option 2 (automatique) :
        javac -d bin --module-path /chemin/javafx/lib --add-modules javafx.controls,javafx.fxml $(find src -name "*.java")

        Note : -------------------------------------------------------------------------------
         Changer le chemin "D:\JavaFX11\javafx-sdk-11\lib" selon votre installation de JavaFX.
        --------------------------------------------------------------------------------------

    -------------------------------------------------------------------------
    EXECUTION
    -------------------------------------------------------------------------

        Executer avec Console View :
            Sans Arguments :
                    java --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls -cp bin MainConsole
                    Note : Changer le chemin "D:\JavaFX11\javafx-sdk-11\lib" selon votre installation de JavaFX.

            Avec Arguments (exemple avec un fichier "r.txt") :
                java --module-path "D:\JavaFX11\javafx-sdk-11\lib" --add-modules javafx.controls -cp bin MainConsole instances/r.txt 10

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
            Ce projet résout un problème d'optimisation combinatoire complexe (le raccordement optimal maisons-générateurs)
            en utilisant une adaptation personnalisée d'un Algorithme Génétique. L'approche est stochastique, ce qui permet
            d'explorer l'espace des solutions bien plus efficacement qu'une approche déterministe.

            Principes Fondamentaux :
            L'algorithme repose sur l'évolution d'un ensemble de solutions candidates appelé "Population".
            Au fil de plusieurs itérations, appelées "Générations", cette population s'améliore par sélection naturelle.
            Plus la population est grande, plus la diversité est élevée ; plus le nombre de générations est grand,
            plus l'algorithme a de temps pour converger vers l'optimum.

            1. Modélisation de l'Individu (Génome) :
               Contrairement aux approches binaires classiques, nous modélisons une solution (un individu) par une
               Map<Maison, Generateur>.
               - Avantage : Cette structure de données garantit structurellement le respect de la contrainte forte
                 "une maison est connectée à un unique générateur". Il est impossible de générer une solution invalide
                 lors des croisements.

            2. Cycle d'Évolution (Itératif) :
               L'algorithme répète les étapes suivantes sur un nombre défini de générations :

               A. Évaluation et Tri de la Population :
                  Chaque solution est d'abord évaluée via la fonction de coût (Dispersion + Lambda * Surcharge).
                  Ensuite, la population entière est triée par ordre croissant de coût afin de placer les meilleures
                  solutions en tête de liste.

               B. Sélection par Élitisme :
                  Une stratégie d'élitisme strict est appliquée : seule la moitié supérieure (50%) de la population triée
                  est conservée. Ces "élites" survivent intactes et constituent le bassin unique de parents pour la suite.

               C. Croisement (Uniform Crossover) :
                  Pour combler les places vides, deux parents sont tirés au hasard parmi l'élite.
                  Pour chaque maison, l'enfant hérite de la connexion du père ou de la mère de manière équiprobable (50/50).

               D. Mutation :
                  Avec une probabilité définie (ex: 10%), une mutation est appliquée sur les nouveaux enfants.
                  Elle consiste à changer aléatoirement le générateur d'une maison, introduisant ainsi de la diversité.

            3. Optimisations Avancées :

               - Paramètres Dynamiques (Scalabilité) :
                 Pour éviter le gaspillage de ressources sur de petits réseaux ou une recherche insuffisante sur de grands réseaux,
                 les paramètres sont calculés à la volée avec des bornes de sécurité :
                 * Taille Population = Max(50, 5 * Nb_Maisons)
                 * Nombre Générations = Min(20000, Max(1000, 100 * Nb_Maisons))

               - Parallélisme (Multi-threading) :
                 L'algorithme étant probabiliste, il peut varier d'une exécution à l'autre. Nous exploitons la puissance
                 moderne des CPU en lançant N instances indépendantes de l'algorithme en parallèle (N = nombre de cœurs logiques).
                 À la fin de l'exécution, les résultats de tous les threads sont comparés et seule la meilleure solution globale est retenue.

               - Tolérance aux Pannes (Robustesse) :
                 L'application intègre un mécanisme de secours (Fall-back). Si l'exécution multi-thread échoue pour une raison
                 technique (ex: environnement restreint), l'algorithme bascule automatiquement et instantanément sur une
                 exécution séquentielle classique. Cela garantit que l'utilisateur obtient toujours un résultat.

            Note : Pour une démonstration visuelle et plus logique, veuillez consulter le rapport technique (PDF) fourni dans le dossier du projet.