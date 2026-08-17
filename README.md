# 🎮 SimpleBedwars — Plugin Paper 1.21

> **Plugin Minecraft BedWars complet, configurable en jeu et sans dépendance externe.**
> Crée tes arènes, configure le shop et les améliorations directement depuis le serveur,
> puis lance des parties façon Hypixel — le tout pour **Paper 1.21+** (Java 21).

![Version](https://img.shields.io/badge/version-1.0.5-blue)
![Paper](https://img.shields.io/badge/Paper-1.21%2B-orange)
![Java](https://img.shields.io/badge/Java-21-red)
![License](https://img.shields.io/badge/build-Maven-success)

---

## 📖 Qu'est-ce que SimpleBedwars ?

**SimpleBedwars** est un plugin qui transforme ton serveur Paper en une plateforme BedWars
clé en main. Conçu pour être **entièrement configurable en jeu** via la commande `/bd`,
il ne demande aucune édition de fichiers ni recompilation pour ajuster tes arènes, ton shop
ou tes améliorations d'équipe.

Le gameplay reprend les codes classiques du BedWars : plusieurs équipes, des lits à protéger,
des générateurs de ressources (fer, or, diamant, émeraude), un shop par équipe, des
améliorations partagées, des pièges défensifs et une phase de **mort subite** avec dragons.

### 🌟 Points forts

- ✅ **Aucune dépendance externe** — pas besoin de Citizens, ProtocolLib ou autre. Tout
  repose sur l'API native Paper/Bukkit (les PNJ utilisent des Armor Stands habillés et
  teintés à la couleur de l'équipe, avec tête de joueur).
- ✅ **Shop 100 % configurable en jeu** — ajoute, modifie ou retire des articles sans
  recompiler, le tout sauvegardé dans `shop.yml`.
- ✅ **Améliorations d'équipe façon Hypixel** — Forge, Lames aiguisées, Armure renforcée,
  Mineur maniaque, Bassin de soin, Buff du dragon et 3 emplacements de pièges.
- ✅ **Lobby d'attente flottant automatique** — cage invisible en BARRIER centrée sur le
  point de spectateur, qui apparaît dès le premier joueur et disparaît au lancement.
- ✅ **Réinitialisation automatique de la map** — sauvegarde/restauration de tous les blocs
  de la zone de jeu après chaque partie.
- ✅ **Scoreboard dynamique**, GUI d'admin paginé, générateurs évolutifs, mort subite.

---

## ✨ Fonctionnalités

### 🏰 Création & configuration d'arènes
- Création simple avec `/bd create <nom>`
- Équipes de **2 / 4 / 6 / 8** joueurs
- Définition des spawns, lits, générateurs et PNJ en jeu
- Suppression sécurisée avec confirmation (`/bd delete <nom> confirm`)
- Sauvegarde et restauration automatique de la région de jeu

### ⚔️ Gameplay
- **Générateurs de ressources** : Fer, Or, Diamant, Émeraude avec montée en niveau
  (accélération à 25 min puis 5 min restantes).
- **Système de lits** : tant qu'un lit est debout, tes coéquipiers réapparaissent.
- **Mort subite** : dragons Ender à 5 minutes de la fin, avec buff possible via amélioration.
- **Couleurs d'équipe génériques** : tout bloc coloré (laine, béton, verre, tapis…) est
  automatiquement recoloré à la couleur de l'équipe de l'acheteur.

### 🛒 Shop configurable en jeu
- Contenu (catégories, articles, prix) stocké dans `plugins/BedwarsPlugin/shop.yml`.
- Commande d'ajout : `/bd shop <catégorie> <slot> <item> <quantité> <prix> <minerai>`.
- Onglet spécial **Tools** : pioche & hache à 4 paliers (Bois → Fer → Or → Diamant),
  avec perte d'un palier à chaque mort (jamais sous le bois).
- Jeu d'articles de départ généré automatiquement au premier démarrage.

### 🔧 Améliorations d'équipe
| Amélioration | Effet |
|--------------|-------|
| **Forge** | Accélère la génération de fer/or |
| **Lames aiguisées** | Bonus de Force (dégâts) |
| **Armure renforcée** | Bonus de Résistance |
| **Mineur maniaque** | Hâte près de la base |
| **Bassin de soin** | Régénération près du lit |
| **Buff du dragon** | Dragon gardien renforcé |
| **Pièges** (×3) | Alarme, Contre-attaque, Fatigue du mineur — prix progressif 1/2/4 émeraudes |

### 🎨 Interface
- **GUI d'administration** paginé (45 arènes/page) avec navigation.
- **Menu joueur** listant les parties : vert = disponible, rouge = en cours (spectateur),
  orange = pleine, gris = non configurée. Clic direct pour rejoindre.
- **PNJ Marchand & Amélioration** à apparence de joueur, armure de cuir teintée à l'équipe,
  avec récupération asynchrone du skin Mojang si un pseudo est fourni.

### 📊 Scoreboard dynamique
Affichage en temps réel du score, de l'état de la partie et des éliminations.

---

## 📋 Commandes

| Commande | Description |
|----------|-------------|
| `/bd create <nom>` | Créer une nouvelle arène |
| `/bd delete <nom> [confirm]` | Supprimer une arène (confirmation requise) |
| `/bd admin gui` | Ouvrir le GUI d'administration |
| `/bd join <nom>` | Rejoindre une partie |
| `/bd leave` | Quitter la partie en cours |
| `/bd list` | Lister toutes les arènes |
| `/bd <nom> equipe <2/4/6/8> <joueurs>` | Configurer les équipes |
| `/bd <nom> pos1 \| pos2 \| posconfirm` | Définir la zone de jeu |
| `/bd <nom> bed <couleur>` | Définir la position du lit |
| `/bd <nom> spawn <couleur>` | Définir le point de spawn |
| `/bd <nom> item <fer\|or\|diamand\|emeraude>` | Configurer les générateurs |
| `/bd <nom> shop <shop\|upgrade> color <couleur> [pseudo]` | Placer les PNJ (pseudo = skin) |
| `/bd <nom> spec` | Point spectateur (= centre du lobby flottant) |
| `/bd <nom> save` | Sauvegarder l'arène |
| `/bd <nom> config` | Voir la configuration |
| `/bd shop <catégorie> <slot> <item> <qté> <prix> <minerai>` | Configurer le shop |

## 🔑 Permissions

| Permission | Description | Par défaut |
|------------|-------------|------------|
| `bedwars.admin` | Administration & configuration des arènes | OP |
| `bedwars.join` | Rejoindre une partie | Tous |

---

## 🚀 Installation

1. Télécharge le fichier **`BedwarsPlugin-1.0.5.jar`** depuis la [dernière release](../../releases/latest).
2. Place le `.jar` dans le dossier `plugins/` de ton serveur Paper 1.21+.
3. Redémarre le serveur.
4. Configure tes arènes avec les commandes `/bd ...` ci-dessus.

> Aucune dépendance n'est requise. ProtocolLib/Citizens ne sont **pas** nécessaires.

## 🛠️ Compilation depuis les sources

```bash
git clone https://github.com/herocraftlol/SimpleBedwars.git
cd SimpleBedwars
mvn clean package
```

Le jar final apparaît dans `target/BedwarsPlugin.jar`.

### Prérequis
- Java 21
- Serveur Paper 1.21+
- Maven 3.6+ (pour compiler)

---

## 🏗️ Structure du code

```
com.bedwars
 ├─ BedwarsPlugin.java          (point d'entrée)
 ├─ arena/                      (modèle de données + persistance des maps)
 ├─ commands/BedwarsCommand.java
 ├─ game/                       (GameInstance, GameManager, WaitingLobbyManager)
 ├─ gui/                        (NPC hub + interface "parties disponibles")
 ├─ npc/                        (PNJ Marchand/Amélioration à apparence de joueur)
 ├─ shop/                       (shop configurable + onglet Tools à paliers)
 ├─ upgrade/                    (améliorations d'équipe façon Hypixel)
 ├─ listeners/                  (combat, lits, protection, shop/PNJ, GUI)
 ├─ scoreboard/                 (scoreboard en jeu)
 └─ util/                       (Location<->YAML, région, lits, économie)
```

---

## 🆕 Nouveautés de la version 1.0.5

Cette version apporte une **compilation réussie et vérifiée** du plugin (plusieurs erreurs
de compilation ont été corrigées) ainsi qu'une documentation mise à jour.

### ✅ Corrections & fiabilité
- **Compilation corrigée** : trois erreurs bloquantes résolues —
  - `LeatherArmorMeta` déplacée vers le bon paquet (`org.bukkit.inventory.meta`).
  - API de profil joueur corrigée : utilisation de `PlayerProfile#update()` (asynchrone)
    à la place de la méthode `complete()` inexistante dans Paper 1.21.1.
  - Récupération du skin Mojang désormais pleinement asynchrone et sûre.
- Le jar `BedwarsPlugin-1.0.5.jar` est **compilé et testé** avec Maven + Paper 1.21.1 API.

### 🔄 Mise à jour
- Numéro de version porté à **1.0.5** (`pom.xml` + `plugin.yml`).
- README entièrement réécrit et rendu plus agréable à lire.

### 🎁 Fonctionnalités incluses (rappel)
- Lobby flottant automatique (cage BARRIER invisible).
- Shop entièrement configurable en jeu (`/bd shop ...`), sauvegardé dans `shop.yml`.
- Onglet **Tools** à 4 paliers avec perte d'un palier à chaque mort.
- Améliorations d'équipe façon Hypixel (Forge, Lames, Armure, Mineur, Soin, Dragon, pièges).
- PNJ Marchand/Amélioration à apparence de joueur, armure teintée à l'équipe, skin asynchrone.
- Épée en bois protégée (non jetable/échangeable) au slot 1 de la hotbar.
- Couleurs d'équipe génériques (recoloration automatique des blocs colorés).
- GUI d'admin paginé + menu joueur avec codes couleur par statut.
- Mort subite avec dragons, générateurs évolutifs, scoreboard dynamique.
- Réinitialisation automatique de la map après chaque partie.

---

## 📜 Historique des versions

### v1.0.5
- Compilation corrigée et vérifiée (LeatherArmorMeta, PlayerProfile API).
- Récupération de skin Mojang asynchrone.
- Version 1.0.5, README réécrit.

### v1.0.4
- Mise à jour de version, compilation Maven.

### v1.0.2
- Système de shop configurable avec GUI personnalisé.
- Améliorations d'équipe (Forge, Lames, Armure, Mineur, etc.).
- PNJ avec skin de joueur, menu joueur `/bd gui`, pièges, dragons de mort subite.
- Suppression d'arènes avec `/bd delete`.

### v1.0.1
- Compilation réussie, README amélioré.

### v1.0.0
- Version initiale du plugin Bedwars.

---

*Plugin compilé avec Maven — API Paper 1.21. Aucune dépendance externe requise.*
