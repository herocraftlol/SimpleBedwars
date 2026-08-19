# 🎮 SimpleBedwars — Paper 1.21+

**Plugin BedWars complet, configurable en jeu et sans aucune dépendance externe** pour serveur Minecraft Paper 1.21+.

SimpleBedwars transforme ton serveur Paper en une plateforme BedWars clé en main. Tout se configure
**directement en jeu** via la commande `/bd` — aucune édition de fichiers ni recompilation pour ajuster
tes arènes, ton shop ou tes améliorations d'équipe. Le gameplay reprend les codes classiques du BedWars :
plusieurs équipes, des lits à protéger, des générateurs de ressources (fer, or, diamant, émeraude), un
shop par équipe, des améliorations partagées, des pièges défensifs et une phase de **mort subite** avec
dragons.

> 🔧 Aucune dépendance requise. ProtocolLib et Citizens ne sont **pas** nécessaires.

## 📦 Installation

1. Télécharge le fichier `BedwarsPlugin-1.0.10.jar` depuis la [dernière release](https://github.com/herocraftlol/SimpleBedwars/releases/latest).
2. Place le `.jar` dans le dossier `plugins/` de ton serveur Paper 1.21+.
3. Redémarre le serveur.
4. Configure tes arènes avec les commandes `/bd ...` (voir ci-dessous).

## 🛠️ Compilation depuis les sources

```bash
git clone https://github.com/herocraftlol/SimpleBedwars.git
cd SimpleBedwars
mvn clean package
```

Le jar final apparaît dans `target/BedwarsPlugin.jar` (le `pom.xml` inclut déjà le dépôt PaperMC et le
shade-plugin). Compilation vérifiée et réussie avec Maven + Paper 1.21.1 API (Java 21).

## Commandes

```
/bd create <nom>
/bd delete <nom>                                    (demande confirmation : /bd delete <nom> confirm)
/bd copy <arène source> <nouveau nom>               (clone une arène, translatée à votre position)
/bd shop <catégorie> <slot> <item> <quantité> <prix> <minerai>   (configure le contenu du Marchand)
/bd arene gui                                        (affiche directement le GUI des arènes)
/bd arene clean                                      (purge les PNJ marchand/amélioration fantômes)
/bd join <nom>
/bd leave
/bd list
/bd <nom> equipe <2/4/6/8> <joueurs par équipe>
/bd <nom> pos1 | pos2 | posconfirm
/bd <nom> bed <couleur>
/bd <nom> spawn <couleur>
/bd <nom> item <fer|or|diamand|emeraude>
/bd <nom> shop <shop|upgrade> color <couleur> [pseudo]   (pseudo optionnel = skin du PNJ)
/bd <nom> spec                                       (= aussi le centre du lobby d'attente flottant)
/bd <nom> minplayers <nombre|off>                    (seuil pour lancer le compte à rebours)
/bd <nom> save
/bd <nom> config
```

Permission `bedwars.admin` (op par défaut) pour tout ce qui configure/supprime une arène ;
`bedwars.join` (tout le monde par défaut) pour rejoindre une partie.

## Nouveautés de cette itération

- **PNJ Marchand / Amélioration** (`ShopNpcManager`) : placés via `/bd <nom> shop <shop|upgrade> color <couleur> [pseudo]`,
  ce sont désormais des mobs immobiles (Zombie, IA/bruit/dégâts/combustion désactivés) habillés
  d'une tête de joueur et d'une armure de cuir teintée à la couleur de l'équipe. Un clic n'importe
  où sur le PNJ ouvre le bon menu, de façon garantie. Si un pseudo Minecraft est fourni, son skin
  réel est récupéré de façon asynchrone (API Mojang via `PlayerProfile#update`) et appliqué à la
  tête. Sans pseudo, la tête reste neutre (aucun appel réseau). C'est la meilleure approximation
  d'un « vrai NPC joueur » sans plugin tiers (Citizens) ni paquets réseau bruts.
- **Shop d'équipe** (`com.bedwars.shop`) : clic sur le Marchand ouvre un GUI avec des onglets en haut.
  Depuis la v3, le contenu (catégories/articles/prix) est entièrement configurable en jeu via
  `/bd shop ...` (voir "Nouveautés de cette itération (v3)" plus haut) — ce paragraphe historique
  décrivait l'ancienne version figée en dur avec 8 catégories façon Hypixel ; ce n'est plus le cas,
  mais rien n'empêche de recréer les mêmes catégories/objets à la main avec la nouvelle commande.
- **Amélioration d'équipe façon Hypixel** (`com.bedwars.upgrade`) : clic sur le PNJ Amélioration ouvre
  Forge (accélère fer/or), Sharpened Blades (Force), Reinforced Armor (Résistance), Maniac Miner (Hâte
  près de la base), Heal Pool (Régénération près du lit), Dragon Buff (dragon gardien) et 3 emplacements
  de pièges (Alarme, Contre-attaque, C'est un piège !, Fatigue du mineur — prix progressif 1/2/4 émeraudes),
  déclenchés automatiquement quand un ennemi entre dans la zone du lit. Tout se paie en diamants (émeraudes
  pour les pièges), prélevés dans l'inventaire du joueur qui achète (voir hypothèses ci-dessous).
- **`/bd delete <nom>`** : supprime intégralement une map (config, région sauvegardée, PNJ, partie en
  cours le cas échéant). Demande une confirmation explicite (`/bd delete <nom> confirm` dans les 15
  secondes, configurable via `game.delete-confirmation-seconds`) pour éviter les suppressions accidentelles.
- **Lobby d'attente automatique** : la commande `/bd <nom> lobby` a été supprimée. À la place, une cage
  invisible (blocs BARRIER) est construite automatiquement, centrée sur l'emplacement défini par
  `/bd <nom> spec`, dimensionnée via `lobby.cage-radius` / `lobby.cage-height` dans `config.yml`. Elle
  apparaît dès qu'un premier joueur rejoint (pour que tout le monde voie la map en dessous en attendant),
  et disparaît (blocs d'origine restaurés) dès que la partie démarre ou que tout le monde repart.
- **GUI principal repensé** : la grille se remplit désormais de haut en bas puis de gauche à droite
  (ordre de lecture naturel), le menu est ouvert à tous les joueurs (plus seulement aux admins), et
  cliquer sur une partie verte (en attente) rejoint directement la partie et téléporte au lobby flottant ;
  cliquer sur une partie rouge (en cours) passe en spectateur comme avant.

## Nouveautés de cette itération (v3)

- **Shop entièrement configurable en jeu** (`/bd shop <catégorie> <slot> <item> <quantité> <prix> <minerai>`) :
  remplace l'ancien système figé en dur dans le code (`ShopCategory.java` a été supprimé). Chaque
  article est désormais stocké dans `plugins/BedwarsPlugin/shop.yml`, modifiable sans recompiler.
  Exemple : `/bd shop blocks 9 WHITE_WOOL 16 4 fer` place 16x laine blanche au slot 9 de la catégorie
  "blocks", pour 4 fer. Le slot doit être compris entre 9 et 44 (les 9 premiers slots du GUI, 0-8,
  sont réservés aux onglets de catégories). Une catégorie apparaît automatiquement comme nouvel onglet
  dès qu'un article y est ajouté (jusqu'à 8 onglets custom, le 9e étant toujours réservé à "Tools").
  Au tout premier démarrage (aucun `shop.yml`), un jeu d'articles de départ est généré automatiquement
  pour que le shop reste utilisable tout de suite.
- **Onglet spécial "Tools"** (toujours présent, non configurable via la commande ci-dessus, car son
  comportement est particulier) : pioche et hache à 4 paliers — Bois → Fer → Or → Diamant. Acheter un
  palier remplace directement l'outil existant. **À chaque mort**, le palier redescend d'un cran
  (jamais en dessous du bois) ; le palier bois est acquis pour toujours et fait partie du kit de base.
- **Épée en bois protégée** : toujours présente au tout premier slot de la hotbar de chaque joueur en
  partie, impossible à drop (touche Q), déplacer, dupliquer ou échanger en main secondaire (touche F),
  de quelque façon que ce soit (`KitProtectionListener` + `KitProtectionUtil`).
- **Couleurs d'équipe génériques pour tout bloc "coloré"** : laine, terre cuite, béton, béton en
  poudre, verre teinté, tapis, terre cuite vernissée... Peu importe la couleur de base choisie en
  configurant un article du shop (ex: `RED_WOOL`), l'objet est automatiquement recoloré à la couleur
  de l'équipe de l'acheteur — aussi bien dans l'aperçu du GUI que lors de l'achat (`TeamColorUtil`).
- **Armure en cuir colorée à l'équipe dès le début de partie** : déjà en place (`GameInstance#giveKit`),
  reconfirmé fonctionnel avec cette itération.
- **GUI de sélection d'arène façon HikaBrain** (`AdminGUIManager` entièrement réécrit) : pagination
  automatique (45 arènes par page, navigation précédent/suivant), bouton "Rejoindre une arène
  aléatoire" qui privilégie les arènes qui ont déjà des joueurs, même code couleur par statut
  (vert = disponible, rouge = en cours → spectateur, orange = pleine, gris = non configurée).

### Limite assumée : le PNJ "Amélioration" n'est pas concerné par `/bd shop`

Le PNJ "Amélioration" (Forge, Sharpened Blades, Reinforced Armor, Maniac Miner, Heal Pool, Dragon
Buff, pièges) garde sa structure spéciale à paliers/effets — ce n'est pas un simple "achète N objets
pour un prix" comme le Marchand, donc la commande simplifiée ne s'y applique pas. Si tu veux aussi
pouvoir reconfigurer les prix de l'amélioration en jeu, dis-le moi, j'ajouterai une commande dédiée.

## Nouveautés de cette itération (v4)

- **`/bd copy <arène source> <nouveau nom>`** — porté du système d'arènes de HikaBrain (voir
  `ArenaManager#copy` / `GameManager#copyConfigurationTo` dans HikaBrain) : clone intégralement une
  arène déjà configurée (zone de jeu, lits/spawns/PNJ Marchand+Amélioration de chaque équipe,
  générateurs, nombre d'équipes/joueurs) vers une toute nouvelle arène, en translatant TOUTES les
  coordonnées par le même décalage. Le joueur qui exécute la commande doit se tenir à l'endroit où
  il a reconstruit la structure à l'identique (au même point relatif que le `/bd <source> spec`
  d'origine) — un peu comme un "coller" WorldEdit, mais pour toute la configuration Bedwars. Si la
  copie est immédiatement complète, elle est directement recapturée et jouable ; sinon elle reste en
  mode configuration. Très utile pour déployer rapidement plusieurs exemplaires d'une même map (utile
  par exemple si tu veux plusieurs parties simultanées sur le même layout).
- Le **GUI paginé de jointure des arènes** (façon HikaBrain, ajouté à l'itération précédente) a été
  vérifié à nouveau : pagination, bouton "arène aléatoire", clic pour rejoindre/spectate, tout reste
  cohérent avec ce nouveau système de copie (les arènes clonées apparaissent automatiquement dedans).

## Nouveautés de cette itération (v5)

- **`/bd <nom> minplayers <nombre|off>`** : nombre de joueurs minimum pour lancer le compte à rebours
  du lobby, indépendamment du nombre max (`teamCount x playersPerTeam`). Par défaut (`off`), le
  comportement d'origine est conservé : il faut que le lobby soit complet. Si tu configures par
  exemple `minplayers 4` sur une arène 8v8, le compte à rebours démarrera dès 4 joueurs, sans
  attendre que la salle soit pleine (les retardataires peuvent encore rejoindre pendant le compte
  à rebours, et celui-ci s'annule si le nombre repasse sous ce seuil).
- **3 items spéciaux dans le lobby d'attente**, donnés automatiquement à chaque joueur qui rejoint
  (`LobbyItemUtil` + `LobbyItemListener`), verrouillés comme l'épée en bois (indroppables,
  indéplaçables, indupliquables) :
  - **Slot 1 : diamant "Forcer le lancement"** — réservé aux joueurs avec la permission
    `bedwars.admin` (les autres joueurs ne le reçoivent pas). Lance la partie immédiatement, peu
    importe le nombre de joueurs présents (même en dessous du minimum configuré).
  - **Slot 3 : bloc "Choisir son équipe"** (laine, recolorée à l'équipe choisie) — ouvre un menu
    listant les équipes disponibles avec leur remplissage (X/Y), pour préréserver une place dans
    l'équipe de son choix avant le lancement. Les préférences sont honorées en priorité au moment
    de la répartition des équipes ; les joueurs qui n'ont rien choisi (ou dont l'équipe visée était
    déjà pleine) sont répartis aléatoirement sur les places restantes.
  - **Slot 5 : bloc barrière "Quitter la partie"** — équivalent de `/bd leave`, renvoie le joueur au
    spawn du monde.

## Choix faits / hypothèses (à valider avec toi)

- **Vrai "NPC joueur"** : Paper/Bukkit ne permet nativement d'afficher une silhouette de joueur (skin
  complet, tête + corps) qu'à travers un Armor Stand habillé (ce que j'ai fait) ou via des paquets réseau
  bruts / un plugin comme Citizens pour une vraie fausse-entité "Joueur". J'ai choisi l'Armor Stand car
  il ne demande aucune dépendance externe et reste entièrement stable dans le temps (contrairement à du
  code bas niveau lié à une version précise du serveur).
- **Économie du shop/améliorations** : par simplicité, le prix est prélevé dans l'inventaire du joueur
  qui clique sur "Acheter", pas dans une "banque d'équipe" partagée comme sur Hypixel (où n'importe quel
  coéquipier peut compléter l'achat d'un autre). Les effets des améliorations (Forge, Sharpened Blades,
  etc.) restent bien partagés par toute l'équipe une fois achetés.
- **Prix du shop / améliorations** : basés sur des données publiques Hypixel Bedwars (recherchées en
  ligne), simplifiés sur quelques objets très cosmétiques (ex: Bedbug, Compact Pop-up Tower non inclus).
  Facilement ajustables dans `ShopCategory.java` / `UpgradeGUIManager.java`.
- **`/bd join <nom>`** : ta description ne précisait pas comment les joueurs rejoignent
  une partie en dehors du GUI. Cette commande reste disponible en complément du clic dans le GUI principal.
- **Couleurs d'équipe → couleur de bloc/armure exactes**, mais en `ChatColor` (texte du
  chat / scoreboard) certaines couleurs comme "Magenta" ou "Rose" n'existent pas
  nativement dans la palette de 16 couleurs de Minecraft : j'ai choisi les plus
  proches visuellement (voir `TeamColor.java`). Rose→`LIGHT_PURPLE`, Magenta→`AQUA` par exemple.
- **`/bd arene gui`** (remplace l'ancien `/bd admin gui`) : ouvre désormais le GUI directement pour
  celui qui tape la commande, plus besoin de placer/cliquer un PNJ. L'infrastructure du NPC hub
  (`AdminNPCManager`) reste en place dans le code pour la compatibilité (un PNJ déjà placé avant ce
  changement continue de fonctionner), mais il n'y a plus de commande pour en placer un nouveau.
- **Réinitialisation de la map** : système de sauvegarde/restauration de tous les blocs de la zone de
  jeu (pos1/pos2), stocké dans `plugins/BedwarsPlugin/arenas/<nom>_region.dat`, capturé lors du
  `/bd <nom> save`. Sur une très grosse map, ce fichier peut être volumineux et la restauration prendre
  un instant (elle est synchrone) — à surveiller en conditions réelles.
- **"À 25 minutes" / "à 5 minutes"** pour l'augmentation du taux de diamant/émeraude :
  j'ai interprété ça comme "quand il **reste** 25 minutes" et "quand il **reste**
  5 minutes" avant la mort subite. Si tu voulais plutôt "25 minutes écoulées depuis le début", dis-le
  moi, c'est un changement d'une ligne.
- **Dragons de la mort subite / Dragon Buff** : comportement simplifié (ciblage périodique d'un joueur
  adverse aléatoire), l'API Minecraft ne permettant pas un comportement de boss 100% fidèle nativement.

## Structure du code

```
com.bedwars
 ├─ BedwarsPlugin.java          (point d'entrée)
 ├─ arena/                      (modèle de données + persistance des maps)
 ├─ commands/BedwarsCommand.java
 ├─ game/                       (GameInstance = logique d'une partie, GameManager,
 │                                WaitingLobbyManager = cage invisible du lobby)
 ├─ gui/                        (NPC hub + interface double-coffre "parties disponibles")
 ├─ npc/                        (PNJ Marchand/Amélioration à apparence de joueur)
 ├─ shop/                       (catégories + GUI du shop façon Hypixel)
 ├─ upgrade/                    (améliorations d'équipe + GUI façon Hypixel)
 ├─ listeners/                  (combat, casse de lits, protection, ramassage, shop/PNJ)
 ├─ scoreboard/                 (scoreboard en jeu)
 └─ util/                       (Location <-> YAML, sauvegarde de région, lits, économie)
```

## 🎁 Fonctionnalités

- 🏰 **Création & configuration d'arènes** en jeu (équipes 2/4/6/8, spawns, lits, générateurs, PNJ).
- 📋 **Clonage d'arènes** (`/bd copy`) pour dupliquer une map complète instantanément.
- 🖥️ **Menu d'arènes direct** (`/bd arene gui`) pour rejoindre une partie en un clic.
- ⚔️ **Gameplay complet** : générateurs de ressources évolutifs, système de lits, mort subite avec dragons.
- 🛒 **Shop 100 % configurable en jeu** (`/bd shop ...`), sauvegardé dans `shop.yml`.
- 🔧 **Améliorations d'équipe façon Hypixel** : Forge, Lames aiguisées, Armure renforcée, Mineur maniaque, Bassin de soin, Buff du dragon et 3 pièges.
- 🧰 **Onglet Tools** : pioche & hache à 4 paliers (Bois → Fer → Or → Diamant), avec perte d'un palier à chaque mort.
- 🎨 **PNJ Marchand/Amélioration** à apparence de joueur, armure teintée à l'équipe, skin asynchrone, clic 100 % fiable (sans dépendance externe).
- 🪄 **Lobby flottant automatique** (cage BARRIER invisible) centré sur le point spectateur.
- 🛡️ **Épée en bois protégée** (non jetable/échangeable) au slot 1 de la hotbar.
- 🎨 **Couleurs d'équipe génériques** : recoloration automatique des blocs colorés.
- 🖥️ **GUI d'admin paginé** + menu joueur avec codes couleur par statut.
- 📊 **Scoreboard dynamique** et réinitialisation automatique de la map après chaque partie.

## 🆕 Nouveautés de la version 1.0.10

Cette version refond les PNJ **Marchand** et **Amélioration** pour rendre l'ouverture des menus
fiable à 100 %, quel que soit l'endroit exact où le joueur clique sur le PNJ.

### 🧟 Des PNJ repensés : adieu les Armor Stands, bonjour les « vendeurs »
- Les PNJ ne sont **plus des Armor Stands** mais des mobs immobiles (Zombies déguisés : IA,
  dégâts, combustion au soleil, bruit et ramassage d'objets désactivés, aucun drop d'équipement),
  habillés d'une tête de joueur et d'une armure en cuir teintée à la couleur de l'équipe.
- Pourquoi ce changement ? Les Armor Stands utilisent un événement de clic séparé et
  *positionnel* (`PlayerInteractAtEntityEvent`) dont la fiabilité dépend de l'endroit précis du
  clic sur le corps du PNJ. Un mob classique déclenche toujours l'événement standard
  `PlayerInteractEntityEvent` : **le menu s'ouvre à tous les coups**, où que l'on clique.

### 🖱️ Clic PNJ corrigé (plus de double ouverture)
- Le gestionnaire d'événement écoute désormais `PlayerInteractEntityEvent` et ignore la main
  secondaire (`EquipmentSlot.HAND`), ce qui élimine le double déclenchement main/off-hand
  qui pouvait ouvrir le menu deux fois.

### 🧹 Purge anti-doublons élargie
- Le nettoyage automatique des PNJ résiduels avant chaque spawn ne se limite plus aux Armor
  Stands : tous les PNJ marqués par le tag interne du plugin sont purgés, quel que soit leur
  type d'entité — les éventuels restes des anciennes versions sont nettoyés au passage.

### ✅ Fiabilité & compilation
- Compilation **vérifiée et réussie** avec Maven + Paper 1.21.1 API (Java 21).
- Le jar `BedwarsPlugin-1.0.10.jar` est compilé et prêt à l'emploi.

### 🔄 Mise à jour
- Numéro de version porté à **1.0.10** (`pom.xml` + `plugin.yml`).
- README mis à jour (nouveautés 1.0.10).

## 📜 Historique — version 1.0.9

Cette version est avant tout une **version de fiabilisation** : elle élimine définitivement les PNJ
« fantômes » qui se dupliquaient à chaque redémarrage du serveur, et corrige le comportement des
arènes après un reboot.

### 🧹 Fin des PNJ fantômes (marchand & amélioration)
- Les PNJ Marchand / Amélioration ne sont **plus persistés par le monde** (`setPersistent(false)`) :
  c'est le plugin qui les fait réapparaître proprement à chaque démarrage. Avant, Minecraft les
  sauvegardait aussi de son côté, ce qui créait un doublon supplémentaire à chaque redémarrage.
- Chaque PNJ est désormais marqué d'un tag interne (`NamespacedKey`) : avant tout nouveau spawn,
  le plugin charge le chunk concerné et **supprime automatiquement tout PNJ résiduel** trouvé à
  proximité. Les doublons accumulés par les anciennes versions sont donc nettoyés une fois pour
  toutes, sans intervention.

### 🧼 Nouvelle commande `/bd arene clean`
- Purge immédiatement tous les PNJ marchand/amélioration résiduels, puis les fait réapparaître
  proprement pour toutes les arènes sauvegardées — pratique pour nettoyer un serveur **sans
  redémarrer**. Réservée à la permission `bedwars.admin`.

### 🗑️ Suppression définitive de l'ancien PNJ « hub »
- Le vieux villageois cliquable (remplacé depuis un moment par `/bd arene gui`) est maintenant
  **purgé automatiquement au démarrage**, et son fichier de sauvegarde est effacé pour que le
  nettoyage n'ait lieu qu'une seule fois.

### 🔄 État des arènes après redémarrage
- Une arène complète et sauvegardée (`/bd <nom> save`) revient désormais à l'état **EN ATTENTE**
  après un redémarrage, au lieu de repasser « non configurée » (SETUP) : vos arènes restent
  jouables même après un reboot du serveur.

### ✅ Fiabilité & compilation
- Compilation **vérifiée et réussie** avec Maven + Paper 1.21.1 API (Java 21).
- Le jar `BedwarsPlugin-1.0.9.jar` est compilé et prêt à l'emploi.

### 🔄 Mise à jour
- Numéro de version porté à **1.0.9** (`pom.xml` + `plugin.yml`).
- README mis à jour (commande `/bd arene clean`, nouveautés 1.0.9).

## 📜 Historique — version 1.0.8

### 👥 Nombre de joueurs minimum (`/bd <nom> minplayers`)
- **Nouvelle commande `/bd <nom> minplayers <nombre|off>`** : définit le seuil de joueurs à partir
  duquel le compte à rebours du lobby démarre automatiquement, indépendamment du nombre maximum.
- En `off` (par défaut), le comportement d'origine est conservé : il faut que le lobby soit complet
  pour lancer la partie. Avec par exemple `minplayers 4` sur une arène 8v8, la partie démarre dès
  4 joueurs présents, sans attendre que la salle soit pleine.
- Les retardataires peuvent encore rejoindre pendant le compte à rebours ; celui-ci s'annule
  automatiquement si le nombre de joueurs repasse sous le seuil configuré.

### 🎒 Items du lobby d'attente
Trois items spéciaux sont désormais donnés automatiquement à chaque joueur qui rejoint le lobby,
verrouillés comme l'épée en bois (indéplaçables, indroppables, indupliquables) :
- **Slot 1 — diamant « Forcer le lancement »** (réservé aux admins `bedwars.admin`) : lance la partie
  immédiatement, peu importe le nombre de joueurs présents.
- **Slot 3 — bloc « Choisir son équipe »** (laine recolorée à l'équipe choisie) : ouvre un menu
  listant les équipes disponibles avec leur remplissage, pour préréserver une place dans l'équipe
  de son choix. Les préférences sont honorées en priorité au moment de la répartition ; les joueurs
  n'ayant rien choisi sont répartis aléatoirement sur les places restantes.
- **Slot 5 — bloc barrière « Quitter la partie »** : équivalent de `/bd leave`, renvoie au spawn du monde.

### ✅ Fiabilité & compilation
- Compilation **vérifiée et réussie** avec Maven + Paper 1.21.1 API (Java 21).
- Méthode de récupération du skin Mojang corrigée (`PlayerProfile#update().join()`) pour la pleine
  compatibilité avec l'API Paper 1.21.1 (`LeatherArmorMeta` déplacée vers `org.bukkit.inventory.meta`).
- Le jar `BedwarsPlugin-1.0.8.jar` est compilé, testé et prêt à l'emploi.

### 🔄 Mise à jour
- Numéro de version porté à **1.0.8** (`pom.xml` + `plugin.yml`).
- README mis à jour (commande `minplayers`, items de lobby, nouveautés 1.0.8, historique).

## 🔑 Permissions
- `bedwars.admin` — Administration & configuration des arènes (OP par défaut)
- `bedwars.join` — Rejoindre une partie (tous par défaut)
