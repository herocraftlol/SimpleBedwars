# BedwarsPlugin — Paper 1.21

Plugin complet permettant de créer et configurer des maps BedWars via la commande `/bd`.

## ⚠️ Compilation

Ce projet a été écrit **hors ligne** : mon environnement n'a pas accès au dépôt Maven de
PaperMC (`repo.papermc.io`), donc je n'ai pas pu compiler le jar moi-même ni faire tourner
de tests. Pour compiler chez toi (avec une connexion internet normale) :

```bash
cd bedwars-plugin
mvn clean package
```

Le jar final apparaît dans `target/BedwarsPlugin.jar` (le `pom.xml` inclut déjà le
dépôt PaperMC et le shade-plugin). Placer ce jar dans le dossier `plugins/` de ton
serveur Paper 1.21.

Si tu obtiens des erreurs de compilation, dis-le moi avec le message d'erreur exact
et je corrigerai — le code suit fidèlement l'API Paper/Bukkit mais n'a pas pu être
vérifié par un vrai compilateur ici.

## Commandes

```
/bd create <nom>
/bd admin gui
/bd join <nom>        (ajouté : nécessaire pour que les joueurs rejoignent une partie)
/bd leave
/bd list
/bd <nom> equipe <2/4/6/8> <joueurs par équipe>
/bd <nom> pos1 | pos2 | posconfirm
/bd <nom> bed <couleur>
/bd <nom> spawn <couleur>
/bd <nom> item <fer|or|diamand|emeraude>
/bd <nom> shop <shop|upgrade> color <couleur>
/bd <nom> spec
/bd <nom> lobby pos1 | pos2 | posconfirm
/bd <nom> save
/bd <nom> config
```

Permission `bedwars.admin` (op par défaut) pour tout ce qui configure une arène ;
`bedwars.join` (tout le monde par défaut) pour rejoindre une partie.

## Choix faits / hypothèses (à valider avec toi)

- **`/bd join <nom>`** : ta description ne précisait pas comment les joueurs rejoignent
  une partie (le GUI admin sert à l'administration, pas à faire entrer les joueurs).
  J'ai ajouté cette commande pour que le plugin soit jouable — dis-moi si tu veux
  un autre système (ex: un hub avec des items cliquables).
- **Couleurs d'équipe → couleur de bloc/armure exactes**, mais en `ChatColor` (texte du
  chat / scoreboard) certaines couleurs comme "Magenta" ou "Rose" n'existent pas
  nativement dans la palette de 16 couleurs de Minecraft : j'ai choisi les plus
  proches visuellement (voir `TeamColor.java`) plutôt que ta commande. Rose→`LIGHT_PURPLE`, Magenta→`AQUA` par exemple.
- **NPC du `/bd admin gui`** : Paper/Bukkit natif ne permet pas de créer un "vrai" NPC
  cosmétique sans mob associé. J'ai utilisé un Villageois avec IA désactivée et
  invulnérable comme support cliquable. Pour un NPC avec skin de joueur personnalisé,
  il faudrait une dépendance externe type Citizens (non installée ici, en dehors du
  périmètre demandé).
- **Réinitialisation de la map** : j'ai implémenté un système de sauvegarde/restauration
  de tous les blocs de la zone de jeu (pos1/pos2), stocké dans
  `plugins/BedwarsPlugin/arenas/<nom>_region.dat`, capturé lors du `/bd <nom> save`.
  Sur une très grosse map, ce fichier peut être volumineux et la restauration prendre
  un instant (elle est synchrone) — à surveiller en conditions réelles.
- **Villageois du shop/upgrade** : la commande crée bien le villageois positionné et
  coloré par équipe, mais je n'ai pas implémenté le contenu du magasin (les objets
  achetables et leurs prix n'étaient pas décrits dans ta demande). Dis-moi si tu veux
  que j'ajoute une vraie interface de shop.
- **"À 25 minutes" / "à 5 minutes"** pour l'augmentation du taux de diamant/émeraude :
  j'ai interprété ça comme "quand il **reste** 25 minutes" et "quand il **reste**
  5 minutes" avant la mort subite (sinon l'ordre n'aurait pas de sens). Si tu voulais
  plutôt "25 minutes écoulées depuis le début", dis-le moi, c'est un changement d'une ligne.
- **Dragons de la mort subite** : un dragon par équipe encore en vie apparaît 20 blocs
  au-dessus de son lit et cible périodiquement un joueur adverse aléatoire (l'API
  Minecraft ne permet pas de rendre un Ender Dragon nativement 100% fidèle à un vrai
  combat multi-cibles complexe, donc le comportement est simplifié).

## Structure du code

```
com.bedwars
 ├─ BedwarsPlugin.java          (point d'entrée)
 ├─ arena/                      (modèle de données + persistance des maps)
 ├─ commands/BedwarsCommand.java
 ├─ game/                       (GameInstance = logique d'une partie, GameManager)
 ├─ gui/                        (NPC admin + interface double-coffre)
 ├─ listeners/                  (combat, casse de lits, protection, ramassage)
 ├─ scoreboard/                 (scoreboard en jeu)
 └─ util/                       (Location <-> YAML, sauvegarde de région, lits)
```
