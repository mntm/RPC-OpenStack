1- Aller dans le fichier source du TP

2- Compiler avec la commande "ant"

3- Aller dans le r�pertoire ./bin/

4- Entrer la commande "rmiregistry & <port>"; mettre le port 5000 partout

5- Revenir � la source du fichier du TP avec cd..

6- D�marrer le  service de r�pertoire de noms avec la commande "./ns.sh <nom> <adress:port local>"

7- Dans un autre terminal, D�marrer le r�partiteur avec la commande "./lb.sh <nom> <unip> <adresse:port repartiteur> <nom repertoire> <adresse:port repertoire > [-m]";le [-m] sert � partir le r�partiteur en mode s�curis� ou non-s�curis� 

8- Dans un autre terminal, ouvrir une session SSH sur une autre machine

9- Aller dans le fichier source du TP

10- Aller dans le r�pertoire ./bin/

11- Entrer la commande "rmiregistry & <port>"; mettre le port 5000 partout

12- Revenir � la source du fichier du TP avec cd..

13- D�marrer le serveur avec la commande "./server.sh <nom> <adress:port local> <nom repertoire> <adresse:port repertoire> [-m <taux de malice>] [-q capacite] [-f]"

14- Recommencer les �tapes 8 � 13 pour obtenir le nombre de serveurs voulus

15 D�marrer le client avec la commande "./client.sh <fichier operation> <nom distributeur> <adresse distributeur> <port distributeur>"

***s'il y a une erreur "Erreur: Port already in use: xxxxx; " durant le lancement, il faut modifier le port � l'int�rieur du fichier server.sh � la ligne -Drmi.object.active-port="xxxx"


