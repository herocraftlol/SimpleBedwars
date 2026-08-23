# BedwarsPlugin — Paper 1.21

**BedwarsPlugin** est un plugin BedWars complet pour serveurs **Paper 1.21+**, entièrement
configurable en jeu via la commande `/bd` : création et clonage d'arènes, équipes
personnalisables, générateurs de ressources, shop et améliorations d'équipe façon Hypixel,
PNJ vendeurs à skin de joueur, lobby d'attente flottant, pièges, boules de feu, statistiques
et classements des joueurs, mort subite avec dragons — le tout **sans aucune dépendance
externe**.

## ✨ Nouveautés de la v1.0.21

- **🔒 Coffres d'équipe vraiment privés** : les coffres enregistrés via
  `/bd <nom> chest <couleur>` ne peuvent désormais être ouverts **que par leur équipe**
  tant que celle-ci n'est pas totalement éliminée — fini le pillage de base en début de
  partie. Un message clair prévient l'intrus, et les **coffres doubles** sont couverts même
  si une seule moitié a été enregistrée. Une fois l'équipe éliminée, son coffre devient
  pillable par tous, comme sur Hypixel.
- **🧪 De vraies potions buvables** : les potions du shop (Force, Vitesse, Invisibilité,
  Saut, Soin) sont maintenant de vraies potions **que l'on boit** — plus de potions
  jetables par erreur aux pieds de ses coéquipiers.
- **⚖️ Prix rééquilibrés** : les potions de **Force** et d'**Invisibilité** passent de
  **4 à 2 émeraudes**, et l'**Arc (Puissance I)** coûte désormais **24 lingots d'or**
  au lieu de 24 lingots de fer — un achat early-game enfin rentable.
- **🧹 Onglet Potions nettoyé** : les anciens articles génériques sans effet ont été
  retirés de la catégorie — seules les vraies potions avec leurs effets restent,
  aux côtés de vos articles personnalisés.
- **🛠️ Correctifs de compilation Paper 1.21** : imports et API `PlayerProfile` remis
  d'équerre — le plugin compile proprement avec `mvn clean package`.

### Nouveautés (v1.0.20)

- **📊 Statistiques persistantes** : chaque joueur accumule désormais ses résultats d'une
  partie à l'autre (parties jouées, victoires, kills, final kills, lits détruits), sauvegardés
  dans `stats.yml`. Consultez les vôtres avec **`/bd stats`**, ou ceux de n'importe qui avec
  **`/bd stats <joueur>`**.
- **🏆 Classements holographiques** : invoquez un classement flottant là où vous vous trouvez
  avec **`/bd leaderboard <beds|wins|kills|finalkills|games>`** (et retirez-le avec
  `/bd leaderboard <catégorie> remove`). Un hologramme par catégorie — top lits détruits,
  victoires, kills, final kills ou parties jouées — persisté (`leaderboards.yml`) et
  **rafraîchi automatiquement toutes les 30 secondes**, façon HikaBrain.
- **⭐ Onglet « Quick Buy » dans le shop** : une nether star trône désormais en haut à gauche
  du Marchand — c'est l'onglet affiché **par défaut** à l'ouverture, comme sur Hypixel.
  **Shift-clic** sur n'importe quel article pour l'ajouter (ou le retirer) de vos favoris ;
  ils sont sauvegardés par joueur (`favorites.yml`) et regroupés proprement dans cet onglet.
- **🔥 Boules de feu façon Hypixel** : la fire charge achetée au shop se **lance d'un clic
  droit** dans la direction regardée (au lieu d'allumer un feu), explose au premier obstacle
  en ne cassant que les blocs posés par les joueurs, et **propulse en l'air** quiconque se
  trouve près de l'impact — y compris vous-même pour des sauts boostés.
- **🧪 Nouveaux articles au shop** : cinq potions prêtes à l'emploi (Force, Vitesse,
  Invisibilité, Saut, Soin — 20 secondes) dans l'onglet Potions, et deux **arcs enchantés**
  (Puissance I ; Puissance I + Recul I) dans l'onglet Ranged. Ils s'ajoutent automatiquement
  à côté de vos articles configurés, sans jamais polluer `shop.yml`.
- **🎒 Hotbar enfin libre** : l'épée, la pioche, la hache et le bloc « choisir son équipe »
  sont toujours placés au slot choisi via `/bd quickmenu` au moment où ils vous sont donnés,
  mais vous pouvez ensuite **les réorganiser librement dans votre hotbar** en pleine partie.
  Ils restent indroppables et ne peuvent pas quitter la hotbar (impossible de les perdre) ;
  seuls les vrais boutons du lobby (forcer le lancement, quitter) restent totalement verrouillés.
- **🛏️ `/bd <nom> autobeds`** : détecte automatiquement tous les lits de la zone `pos1`/`pos2`
  et les assigne aux équipes d'après leur couleur — fini la configuration lit par lit.
- **🛒 `/bd shopmenu`** : ouvre un aperçu du shop utilisable depuis le lobby d'attente, pour
  préparer sa stratégie (et ses favoris Quick Buy) avant même le début de la partie.

## 📦 Installation

1. Téléchargez `BedwarsPlugin.jar` depuis la [dernière release](../../releases/latest).
2. Placez-le dans le dossier `plugins/` de votre serveur Paper 1.21+.
3. Redémarrez le serveur, puis créez votre première arène avec `/bd create <nom>`.

## 🔧 Compilation depuis les sources

```bash
mvn clean package
```

Le jar final apparaît dans `target/BedwarsPlugin.jar` (le `pom.xml` inclut le dépôt PaperMC
et le shade-plugin). Nécessite un JDK 21+ et Maven.

## Commandes

```
/bd create <nom>
/bd delete <nom>                                    (demande confirmation : /bd delete <nom> confirm)
/bd copy <arène source> <nouveau nom>               (clone une arène, translatée à votre position)
/bd shop <catégorie> <slot> <item> <quantité> <prix> <minerai>   (configure le contenu du Marchand)
/bd arene gui                                        (affiche directement le GUI des arènes)
/bd arene clean                                      (purge les PNJ marchand/amélioration fantômes)
/bd join <nom>
/bd spectate <nom>
/bd leave
/bd quickmenu <épée|pioche|hache|bloc> <0-8>        (personnalisez vos slots de hotbar)
/bd shopmenu                                        (aperçu du shop utilisable au lobby)
/bd stats [joueur]                                  (statistiques globales persistantes)
/bd leaderboard <beds|wins|kills|finalkills|games> [remove]   (invoque un classement)
/bd list
/bd <nom> equipe <2/4/6/8> <joueurs par équipe>
/bd <nom> pos1 | pos2 | posconfirm
/bd <nom> bed <couleur>
/bd <nom> autobeds                                   (détecte et assigne les lits par couleur)
/bd <nom> spawn <couleur>
/bd <nom> item <diamand|emeraude>                    (générateurs communs de la map)
/bd <nom> forge <couleur>                            (crée fer+or de l'équipe, accélérés par l'amélioration Forge)
/bd <nom> chest <couleur>                            (ajoute le coffre visé à l'équipe, vidé à chaque partie)
/bd <nom> geninfo                                    (liste les générateurs pour diagnostiquer)
/bd <nom> reset                                      (réinitialise immédiatement, même en pleine partie)
/bd <nom> shop <shop|upgrade> color <couleur> [pseudo]   (pseudo optionnel = skin du PNJ)
/bd <nom> spec                                       (= aussi le centre du lobby d'attente flottant)
/bd <nom> specspawn                                  (spawn spectateurs pendant la partie, ex: milieu de la map)
/bd <nom> minplayers <nombre|off>                    (seuil pour lancer le compte à rebours)
/bd <nom> edit                                       (= config, réactive la modification libre de la map)
/bd <nom> save
/bd <nom> config
```

Permission `bedwars.admin` (op par défaut) pour tout ce qui configure/supprime une arène ;
`bedwars.join` (tout le monde par défaut) pour rejoindre une partie.

## Historique des versions

### Nouveautés (v1.0.19)

- **Morts par chute enfin fiables** : la mort instantanée lorsqu'on tombe sous la map se
  calcule maintenant à partir des spawns et des lits des équipes (avec une grande marge
  de 15 blocs), au lieu du rectangle brut `pos1`/`pos2`. Résultat : plus aucune mort
  fantôme en pleine partie dès qu'un joueur descendait quelques blocs sous le niveau à
  peine plus haut de la zone déclarée, tout en continuant à tuer immédiatement toute
  personne qui tombe réellement hors de la map.
- **Correctifs de compilation** : mise à jour vers l'API Paper 1.21 moderne
  (`PlayerProfile#update()` au lieu de l'ancien `complete()`) et import corrigé de
  `LeatherArmorMeta`. Le plugin compile à nouveau proprement avec `mvn clean package`.

### Nouveautés (v1.0.18)

- **`/bd quickmenu <épée|pioche|hache|bloc> <0-8>`** : chaque joueur personnalise lui-même
  la position de son épée, sa pioche, sa hache (en partie) et du bloc « choisir son équipe »
  (dans le lobby d'attente) dans sa hotbar. La préférence est individuelle et sauvegardée
  (`playerprefs.yml`), puis appliquée automatiquement à chaque partie ou lobby suivant. La
  protection anti-drop/déplacement suit désormais l'item lui-même plutôt qu'un numéro de
  slot fixe : elle fonctionne donc quel que soit le slot choisi.
- **`/bd <nom> chest <couleur>`** : enregistre le coffre visé (jusqu'à 10 blocs) dans la
  liste des coffres d'une équipe (plusieurs coffres possibles). Ces coffres — ainsi que le
  coffre ender de chaque participant — sont désormais **vidés automatiquement au début ET
  à la fin de chaque partie**, pour éviter tout stockage d'objets d'une partie à l'autre.
- **Pioche et hache ne sont plus données au spawn** : chaque joueur doit d'abord les acheter
  dans l'onglet « Tools » du shop pour les avoir et les garder le reste de la partie. Les
  paliers utilisent maintenant les mêmes noms que l'épée (Bois → Pierre → Fer → Diamant) :
  Bois **10 fer**, Pierre **10 fer**, Fer **4 or**, Diamant **12 or**. Plus rien n'est
  gratuit pour pioche/hache, mais une fois le palier bois acheté, il reste acquis pour
  toujours ; la dégradation d'un cran à chaque mort ne concerne que les paliers supérieurs.
  L'épée n'est pas concernée : son palier bois reste donné gratuitement au spawn.
- **Spectateurs libres de se déplacer dans toute l'arène** : le confinement utilisait un
  rayon fixe de 60 blocs depuis le point spectateur, ce qui pouvait téléporter un spectateur
  même au milieu d'une grande map. La vraie zone de jeu (`pos1`/`pos2`, avec une bonne marge
  tout autour) est maintenant utilisée à la place de ce simple cercle.
- **Les minerais n'« éjectent » plus** : vitesse nulle systématique sur tous les items
  générés (fer, or, diamant, émeraude), qui apparaissent pile à l'endroit prévu au lieu de
  sursauter ou glisser. Le rayon de dispersion de la Forge est également resserré.
- **Fireball ajoutée au shop par défaut** (onglet utilitaires, **40 fer**). Si votre shop
  est déjà configuré, ajoutez-la avec `/bd shop utility <slot> FIRE_CHARGE 1 40 fer`.


### Nouveautés (v17)

- **Construction strictement limitée à la zone de jeu** : pendant une partie, tout bloc
  placé en dehors des limites définies par `pos1`/`pos2` — en hauteur, sur les côtés ou
  en dessous — est désormais annulé, avec un message d'avertissement pour le joueur. Le
  mode édition (`/bd <nom> edit`) n'est pas concerné : un admin peut toujours construire
  librement pour configurer la map.
- **Bug corrigé : les PNJ marchand/amélioration ne se font plus disparaître entre eux** :
  le nettoyage anti-doublons balayait un rayon de 3 blocs autour d'un nouveau PNJ, ce qui
  supprimait aussi le PNJ voisin (le marchand et l'amélioration d'une même équipe sont
  souvent placés côte à côte). Le rayon de nettoyage est maintenant réduit à l'emplacement
  exact (0,5 bloc) : il détecte toujours les vrais doublons, sans jamais toucher un autre
  PNJ placé juste à côté.
- **Map toujours propre à chaque partie** : tous les items et minerais laissés au sol dans
  la zone de jeu sont automatiquement nettoyés au début ET à la fin de chaque partie (y
  compris après un `/bd <nom> reset`), pour repartir sur une map impeccable à chaque
  lancement.

## Nouveautés de cette itération

- **PNJ Marchand / Amélioration** (`ShopNpcManager`) : placés via `/bd <nom> shop <shop|upgrade> color <couleur> [pseudo]`,
  ce sont des mobs immobiles (Zombies déguisés, IA désactivée, voir "Correction de bug (v7)" plus bas)
  en armure de cuir teintée à la couleur de l'équipe, avec une tête de joueur. Si un pseudo Minecraft
  est fourni, son skin réel est récupéré de façon asynchrone (API Mojang via `PlayerProfile#complete`)
  et appliqué à la tête. Sans pseudo, la tête reste neutre (aucun appel réseau). Voir la remarque
  "vrai NPC joueur" ci-dessous : c'est la meilleure approximation possible sans plugin tiers (Citizens)
  ni paquets réseau bruts.
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

## Corrections de bugs (v6)

- **Les PNJ Marchand/Amélioration ne se dupliquent plus au redémarrage.** La cause : ils étaient
  marqués persistants (`setPersistent(true)`), donc sauvegardés par Minecraft dans le monde ; au
  redémarrage, le plugin les respawnait quand même par-dessus (son registre interne, lui, est remis
  à zéro à chaque démarrage) — d'où des doublons qui s'accumulaient à chaque redémarrage, invisibles
  jusqu'à ce que leur chunk se recharge. Corrigé : ces PNJ ne sont plus persistants (c'est le plugin
  qui les fait toujours réapparaître lui-même au démarrage, `setPersistent(false)` suffit), et avant
  chaque apparition, le chunk concerné est chargé puis purgé de tout PNJ résiduel marqué par notre
  tag interne — ce qui nettoie aussi, une bonne fois pour toutes, les doublons déjà accumulés par les
  versions précédentes. Une commande **`/bd arene clean`** a aussi été ajoutée pour relancer ce
  nettoyage à la demande, sans avoir à redémarrer le serveur.
- **Le PNJ "hub" (ancien `/bd admin gui`, remplacé par `/bd arene gui`) est définitivement supprimé.**
  Comme il n'est plus utilisé, `AdminNPCManager` ne le respawn plus jamais : au premier démarrage
  après cette mise à jour, tout PNJ hub encore présent dans le monde est retrouvé (chunk chargé de
  force) et supprimé, puis son fichier de sauvegarde (`npc.yml`) est effacé pour de bon.
- **Une arène déjà sauvegardée (`/bd <nom> save`) n'apparaît plus "non configurée" après un
  redémarrage du serveur.** La cause : l'état runtime de l'arène (WAITING/SETUP/...) n'était en fait
  jamais sauvegardé dans le fichier de configuration — au rechargement, chaque arène repartait donc
  systématiquement sur la valeur par défaut du code (SETUP), même si elle avait été correctement
  sauvegardée. Corrigé : au chargement, une arène marquée comme sauvegardée (`saved: true`) est
  maintenant remise en état `WAITING` (disponible, ouverte) automatiquement — plus besoin de refaire
  `/bd <nom> save` après chaque redémarrage.

## Correction de bug (v7) : les PNJ Marchand/Amélioration n'ouvraient pas toujours le GUI au clic

Cause : ces PNJ étaient des **Armor Stands**, qui utilisent un événement de clic différent et
"positionnel" (`PlayerInteractAtEntityEvent`, qui dépend de l'endroit précis cliqué sur le corps
pour gérer l'équipement d'armure) — peu fiable pour garantir l'ouverture d'un menu à chaque clic.

Corrigé : ils sont maintenant des **Zombies déguisés** (IA désactivée, invulnérables, silencieux,
ne prennent pas feu au soleil, ne poussent pas les joueurs), habillés d'une tête de joueur et
d'une armure en cuir teintée à la couleur de l'équipe — un mob classique déclenche toujours
l'événement standard et fiable `PlayerInteractEntityEvent`, peu importe où on clique dessus.
Toute la logique anti-duplication (v6) a été conservée à l'identique sur ce nouveau type d'entité.

## Nouveautés / corrections (v8)

- **Le bug de l'arène "non configurée" au redémarrage est cette fois vraiment corrigé.** Cause
  réelle trouvée : il restait une ligne orpheline (`arena.setState(ArenaState.SETUP)`) tout à la
  fin de `ArenaManager#loadArena`, qui écrasait silencieusement le correctif de la version
  précédente juste avant l'enregistrement en mémoire. Supprimée.
- **Les minerais ne spawn plus jamais en dehors d'une partie réellement active** : garde-fous
  ajoutés sur tous les générateurs (fer/or, diamant/émeraude, bonus de Forge) — rien ne se
  déclenche si l'arène n'est pas en `PLAYING`/`SUDDEN_DEATH`, ni si plus aucun joueur n'est
  effectivement en jeu.
- **Toutes les améliorations d'équipe repartent à zéro à chaque partie** (déjà vrai de fait grâce à
  l'instance de partie fraîche à chaque lancement, désormais aussi garanti explicitement en début
  de partie).
- **`/bd <nom> forge <couleur>`** : nouvelle commande qui définit l'ancre de la "forge de base"
  d'une équipe. `/bd <nom> item <fer|or|diamand|emeraude> [couleur]` accepte maintenant une couleur
  d'équipe optionnelle pour lier un générateur fer/or à une équipe — **c'est ce qui manquait pour
  que la Forge fonctionne** : avant cette version, aucun générateur n'était jamais rattaché à une
  équipe, donc l'amélioration Forge n'avait littéralement aucun effet.
- **Paliers de la Forge, implémentés exactement comme demandé** :
  - Palier 1 : fer/or **x1,25** plus rapide
  - Palier 2 : fer/or **x1,75** plus rapide
  - Palier 3 : fer/or **x2** plus rapide, + **diamant** au point `forge` de l'équipe (1 toutes les 60s)
  - Palier 4 : fer/or **x2,25** plus rapide, diamant **toutes les 15s**, + **émeraude toutes les 2 minutes**
- **Dragon Buff repensé** : n'apparaît plus immédiatement à l'achat. Il donne désormais un **second
  dragon**, uniquement à la mort subite (une fois le compteur et toutes les phases terminés), pour
  les équipes qui l'ont acheté.
- **Épée/hache/pioche à slots fixes et verrouillés** : épée toujours au slot 1, hache au slot 2,
  pioche au slot 3 de la hotbar — indroppables, indéplaçables, indupliquables (`KitProtectionUtil`
  généralisé aux 3 outils). L'épée a maintenant ses propres paliers (Bois → Pierre → Fer → Diamant,
  achetables dans l'onglet "Tools" du shop, à côté de pioche/hache) et redescend d'un palier à
  chaque mort comme les deux autres (le palier bois reste toujours acquis).
- **Sharpened Blades ciblé** : n'applique plus un effet de potion Force (qui boostait aussi les
  poings et toute autre arme) mais pose directement l'enchantement Tranchant correspondant sur
  l'épée du slot 1 de chaque membre de l'équipe qui l'a acheté.
- **Mode édition** : `/bd <nom> edit` (alias de `config`) remet l'arène en configuration, combiné à
  un contournement explicite dans la protection anti-casse de bloc qui laisse un admin construire
  librement tant que l'arène est en `SETUP`.
- **`/bd spectate <nom>`** : commande dédiée pour rejoindre une partie en cours en spectateur (en
  plus du clic dans le GUI, qui fonctionnait déjà).
- **Les PNJ marchand/amélioration réapparaissent systématiquement au lancement de la partie**
  (`spawnForArena` appelé explicitement dans `startGame()`), même s'ils avaient disparu entre la
  configuration et le lancement (chunk déchargé, etc.).
- **Scoreboard** : nouvelle ligne affichant le temps restant avant la prochaine phase (Phase 2,
  Phase 3, ou Mort subite selon l'avancement de la partie). Déjà scopé uniquement aux joueurs et
  spectateurs de l'arène concernée (jamais affiché à qui que ce soit d'autre), déjà visible aux
  spectateurs (la liste des participants d'une partie inclut déjà les spectateurs).

### Notes / limites

- **`/bd copy`** (ajouté à une itération précédente) copie déjà toute la configuration d'une arène
  (zone de jeu, lits, spawns, PNJ shop/upgrade, générateurs) en la translatant vers un nouvel
  emplacement. Les **coffres et leur contenu** ne sont en revanche pas copiés spécifiquement : la
  capture de région (`RegionSchematic`) sauvegarde le type de chaque bloc mais pas le contenu des
  coffres (inventaire). Dis-moi si tu veux que j'ajoute la copie du contenu des coffres.
- **Mode édition** : je n'ai pas trouvé de restriction de construction générale dans le code (seule
  la casse de bloc était bloquée, et seulement pour un joueur déjà "participant" d'une partie) — le
  nouveau contournement explicite couvre ce cas précis. Si le problème vient d'ailleurs (un autre
  plugin de protection de terrain, un mauvais gamemode, etc.), dis-le moi.

## Simplification (v9) : la Forge remplace `item fer/or`

- **`/bd <nom> item`** ne gère plus que `diamand` et `emeraude` (générateurs communs de la map).
- **`/bd <nom> forge <couleur>`** crée maintenant directement, au même endroit, les générateurs de
  fer ET d'or de l'équipe (plus besoin de faire `/bd <nom> item fer/or <couleur>` en plus) : une
  seule commande pour toute la base de ressources d'une équipe. Relancer `/bd <nom> forge <couleur>`
  déplace la forge (et ses générateurs fer/or) au nouvel endroit, en retirant proprement les anciens.
- **`/bd <nom> save`** exige maintenant qu'une forge soit définie pour chaque équipe (au lieu d'exiger
  globalement "au moins un générateur de fer" et "au moins un générateur d'or", ce qui pouvait être
  rempli sans qu'aucune équipe n'ait de fer/or accéléré par son amélioration Forge — d'où le blocage
  que tu rencontrais).

## À propos du signalement "la forge ne donne pas d'items" (v10)

J'ai relu très attentivement toute la chaîne (création des générateurs par `/bd <nom> forge`,
persistance, boucle de tick, calcul d'intervalle, distribution aux joueurs) et je n'ai pas trouvé
de bug qui empêcherait totalement le spawn — la logique tient debout sur le papier. Ceci dit, j'ai
identifié et corrigé un vrai problème de **timing** : l'or était réglé par défaut sur 4 secondes
(`gold-interval-ticks: 80`) au lieu des 2 secondes demandées. Corrigé à `40` (2s) ; le fer était
déjà à 1 seconde (`iron-interval-ticks: 20`), inchangé.

**Important** : si tu as déjà un fichier `config.yml` généré sur ton serveur (dans
`plugins/BedwarsPlugin/`), remplacer le plugin ne suffira pas — Bukkit ne réécrit jamais un
`config.yml` déjà existant. Il faut soit supprimer ce fichier (il sera régénéré avec les nouvelles
valeurs au redémarrage), soit éditer manuellement la ligne `gold-interval-ticks` toi-même.

J'ai aussi ajouté deux choses pour t'aider à diagnostiquer/confirmer que ça fonctionne bien :
- **`/bd <nom> geninfo`** : liste tous les générateurs de l'arène (type, équipe assignée, position)
  — vérifie que ta forge a bien créé un générateur FER et un OR avec la bonne équipe.
- **Un son de ramassage** joue désormais à chaque fois qu'un générateur fer/or donne un item à un
  joueur à proximité (rayon élargi de 4 à 6 blocs), pour que ce soit immédiatement perceptible.

Si après ça le problème persiste, dis-moi précisément : est-ce qu'aucun item n'apparaît du tout
(même en te tenant littéralement à l'endroit où tu as fait `/bd <nom> forge`), ou est-ce plus lent
que prévu ? Le résultat de `/bd <nom> geninfo` m'aiderait aussi beaucoup à confirmer si le problème
vient de la configuration (générateur manquant/mal placé) ou du code.

## Nouveautés (v11)

- **`/bd leave` (et le bouton "Quitter" du lobby) fait vraiment quitter l'arène** : en plus de
  sortir du lobby d'attente, si le joueur était en train de jouer, il est retiré de son équipe et
  son scoreboard disparaît immédiatement (`GameInstance#handlePlayerLeave`). Une déconnexion pure
  et simple du serveur (`PlayerQuitEvent`) déclenche désormais exactement le même nettoyage complet
  (avant, seule la sortie du lobby d'attente était gérée).
- **Victoire automatique si une seule équipe a encore des joueurs** : que ce soit parce que le lit
  d'une équipe a été détruit et son dernier joueur tué, OU parce que tous ses joueurs ont quitté
  volontairement la partie (peu importe l'état de son lit), la condition de victoire est vérifiée
  de la même façon à chaque fois — dès qu'il ne reste plus qu'une équipe avec des joueurs, elle
  gagne automatiquement.
- **`/bd <nom> reset`** : réinitialise immédiatement une arène, peu importe son état (lobby
  d'attente, compte à rebours, partie en cours, mort subite...) — tous les joueurs et spectateurs
  sont renvoyés au spawn du monde, la map est entièrement restaurée à son état sauvegardé, et
  l'arène redevient aussitôt disponible pour une nouvelle partie.

## Nouveautés / corrections (v12)

- **Bug critique corrigé : impossible de casser le lit adverse.** Cause trouvée : `/bd <nom> bed
  <couleur>` enregistrait la position **du joueur qui tape la commande**, pas celle du bloc du lit
  lui-même. Sauf à se tenir exactement à l'intérieur du lit au moment de la commande (ce qui n'était
  précisé nulle part), la position stockée ne correspondait jamais exactement au bloc du lit, donc au
  moment de le casser en jeu, le code ne le reconnaissait pas comme un lit et le traitait comme un
  bloc de map normal (protégé, non cassable). Corrigé : la commande vise maintenant directement le
  bloc du lit regardé (raytrace jusqu'à 10 blocs), avec vérification que c'est bien un lit — le
  fonctionnement attendu est maintenant garanti : lit intact = respawn illimité, lit détruit = mort
  définitive au prochain décès, plus aucune équipe adverse = victoire automatique (déjà en place).
- **Répartition des équipes dans le lobby** : déjà logique (2 joueurs → toujours deux équipes
  différentes ; 4+ → répartition équilibrée). Ajout : si le nombre de joueurs est impair, quelle
  équipe récupère le joueur "en trop" est maintenant tirée au sort (avant, c'était toujours la même
  équipe qui héritait du surplus).
- **Les dragons sont supprimés au début ET à la fin de chaque partie** (`GameInstance#killAllDragons`),
  pour éviter qu'un dragon oublié d'une mort subite précédente ne traîne dans une nouvelle partie.
- **Zone de spawn de la Forge réduite à 3x3** : les ressources de la Forge (fer/or/diamant/émeraude)
  apparaissent désormais dispersées aléatoirement dans une zone de 3x3 blocs autour du point `forge`,
  plutôt que toujours exactement au même endroit.
- **Mort instantanée en sortant de la zone par en dessous** : plus besoin d'attendre de tomber dans
  le vide du monde — dès qu'un joueur passe sous la limite basse de la zone de jeu (`pos1`/`pos2`),
  il meurt immédiatement (mort normale ou définitive selon l'état de son lit, comme toute autre mort).
- **Compte à rebours du lobby porté à 30 secondes** par défaut (`game.countdown-lobby-seconds`).
- **On peut de nouveau réorganiser son propre inventaire pendant que le shop/l'amélioration est
  ouvert** : seuls les clics dans le GUI du shop lui-même (et le shift-click, qui enverrait l'objet
  directement dedans) sont bloqués ; les outils protégés du kit restent verrouillés comme avant.
- **`/bd <nom> specspawn`** : nouveau point de spawn dédié aux spectateurs pendant la partie
  (typiquement le milieu de la map), utilisé en priorité par rapport à `spec` (qui reste le centre du
  lobby d'attente flottant). Les joueurs éliminés définitivement y sont téléportés et ne peuvent plus
  en sortir (comme avant, la contention aux abords de ce point est automatique).
- **En fin de partie, chaque joueur est retéléporté exactement là où il se trouvait juste avant son
  tout premier `/bd join`** (ou `/bd arene gui` → clic sur une arène), au lieu du spawn spectateur de
  l'arène. Fonctionne aussi bien pour une fin de partie normale que pour un `/bd leave` avant même
  que la partie ne démarre.

## Nouveautés (v13)

- **La TNT s'amorce toute seule dès qu'elle est posée** (pas besoin de silex et acier, comme sur
  Hypixel Bedwars), et **son explosion ne peut casser que les blocs posés par les joueurs pendant
  la partie** — jamais la structure d'origine de la map (`TntListener`).
- **Les minerais de la Forge restent uniquement là où elle a été définie** : ils ne sont plus jamais
  "envoyés" directement dans l'inventaire des joueurs à proximité, contrairement à un générateur
  commun — il faut aller les ramasser au sol, comme demandé.
- **Vitesse de base du fer/or divisée par deux** (fer : 1 toutes les 2 secondes au lieu d'une ;
  or : 1 toutes les 4 secondes au lieu de 2) — les multiplicateurs de palier de Forge (x1.25 / x1.75
  / x2 / x2.25) s'appliquent toujours de la même façon par-dessus, donc l'accélération relative
  reste cohérente, juste calée sur cette nouvelle base plus lente.
- **Plafond au sol du fer/or de la Forge** : 24 fer / 12 or de base, qui augmente de +8 fer / +4 or à
  chaque palier de Forge, jusqu'à devenir illimité au dernier palier (4) — évite que les ressources
  s'accumulent indéfiniment si personne ne vient les ramasser, tout en récompensant la Forge upgradée.
- **Hologramme au-dessus de chaque générateur de diamant/émeraude** : affiche en temps réel le
  temps restant avant le prochain spawn ainsi que le palier actuel en chiffres romains (ex: "Diamant
  II - 12s"), qui correspond aux paliers d'accélération avant la mort subite (Phase 1/2/3). Le palier
  actuel est aussi indiqué dans le scoreboard de la partie.
- **Compte à rebours de 5 secondes après chaque mort non-finale** : le joueur passe en spectateur
  (peut observer la partie), un compte à rebours s'affiche en gros à l'écran (title), puis il
  réapparaît automatiquement à son spawn d'équipe à la fin — au lieu d'un respawn instantané.

## Nouveautés / corrections (v14)

- **Impossible de construire en dehors de la zone de jeu** : en hauteur, sur les côtés, ou même en
  dessous — tout placement de bloc hors des limites définies par `pos1`/`pos2` est désormais annulé
  pendant une partie active (`PlayerProtectionListener`). Le mode édition (`/bd <nom> edit`, arène en
  `SETUP`) n'est pas concerné : un admin peut toujours construire librement pour configurer la map.
- **Bug corrigé : placer un PNJ marchand/amélioration faisait disparaître le PNJ voisin.** Cause
  trouvée : le nettoyage anti-doublons (`ShopNpcManager`) balayait un rayon de 3 blocs autour du
  nouvel emplacement pour supprimer d'éventuels résidus — ce qui supprimait aussi n'importe quel
  AUTRE PNJ placé à proximité (le marchand et l'amélioration d'une même équipe, souvent installés
  côte à côte). Corrigé : le rayon de nettoyage est réduit à l'exact même emplacement (0,5 bloc), qui
  suffit à détecter un vrai doublon sans jamais toucher un PNJ différent placé juste à côté.
- **Les items/minerais au sol sont nettoyés au début ET à la fin de chaque partie** (dans la zone de
  jeu définie par `pos1`/`pos2`), pour repartir sur une map propre à chaque lancement, sans résidus
  d'une partie précédente (fonctionne aussi bien pour une fin de partie normale que pour un
  `/bd <nom> reset`).

## Choix faits / hypothèses (à valider avec toi)

- **Vrai "NPC joueur"** : Paper/Bukkit ne permet nativement d'afficher une silhouette de joueur (skin
  complet, tête + corps) qu'à travers un Armor Stand habillé, un mob déguisé (ce que j'ai fait, voir
  "Correction de bug (v7)"), ou via des paquets réseau bruts / un plugin comme Citizens pour une vraie
  fausse-entité "Joueur". J'ai d'abord choisi l'Armor Stand, puis basculé vers un Zombie déguisé (IA
  désactivée) car le clic dessus n'ouvrait pas toujours le GUI de façon fiable. Aucun des deux ne
  demande de dépendance externe et reste stable dans le temps (contrairement à du code bas niveau lié
  à une version précise du serveur).
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
 ├─ shop/                       (catégories + GUI du shop façon Hypixel, articles spéciaux)
 ├─ stats/                      (statistiques persistantes + hologrammes de classement)
 ├─ upgrade/                    (améliorations d'équipe + GUI façon Hypixel)
 ├─ listeners/                  (combat, casse de lits, protection, ramassage, shop/PNJ,
 │                                TNT et boules de feu)
 ├─ scoreboard/                 (scoreboard en jeu)
 └─ util/                       (Location <-> YAML, sauvegarde de région, lits, économie)
```
