1- Aller dans le fichier source du TP

2- Compiler avec la commande "ant"

3- Aller dans le répertoire ./bin/

4- Entrer la commande "rmiregistry & <port>"; mettre le port 5000 partout

5- Revenir à la source du fichier du TP avec cd..

6- Démarrer le  service de répertoire de noms avec la commande "./ns.sh <nom> <adress:port local>"

7- Dans un autre terminal, Démarrer le répartiteur avec la commande "./lb.sh <nom> <unip> <adresse:port repartiteur> <nom repertoire> <adresse:port repertoire > [-m]";le [-m] sert à partir le répartiteur en mode sécurisé ou non-sécurisé 

8- Dans un autre terminal, ouvrir une session SSH sur une autre machine

9- Aller dans le fichier source du TP

10- Aller dans le répertoire ./bin/

11- Entrer la commande "rmiregistry & <port>"; mettre le port 5000 partout

12- Revenir à la source du fichier du TP avec cd..

13- Démarrer le serveur avec la commande "./server.sh <nom> <adress:port local> <nom repertoire> <adresse:port repertoire> [-m <taux de malice>] [-q capacite] [-f]"

14- Recommencer les étapes 8 à 13 pour obtenir le nombre de serveurs voulus

15 Démarrer le client avec la commande "./client.sh <fichier operation> <nom distributeur> <adresse distributeur> <port distributeur>"

***s'il y a une erreur "Erreur: Port already in use: xxxxx; " durant le lancement, il faut modifier le port à l'intérieur du fichier server.sh à la ligne -Drmi.object.active-port="xxxx"


