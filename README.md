# 🏰 BedwarsPlugin — Paper 1.21

Plugin BedWars complet et configurable pour serveur Minecraft Paper 1.21+. Créer, configurer et gérer vos propres arènes BedWars en jeu avec une interface intuitive.

## ✨ Fonctionnalités

### 🎮 Gameplay
- **Système de jeu complet** : équipes de 2/4/6/8 joueurs, gestion des generators, mort subite avec dragons
- **Lobby flottant automatique** : plateforme générée automatiquement au-dessus de la zone de jeu
- **Cage spectateur** : confinement sécurisé pour les joueurs éliminés
- **Système de pièges** : Traps (A, C, M) à usage unique avec niveaux
- **Upgrades personnalisables** : Tranchant, Armure Renforcée, Mineur Maniaque, Chute, Forge, Soin, Dragon

### 🛒 Boutique
- **Shop et Upgrade configurables** : Interface GUI complète avec items, prix et niveaux
- **NPC à skin de joueur** : Intégration ProtocolLib pour des NPCs réalistes (fallback villageois si absent)
- **Contenu par défaut Hypixel** : Shop pré-rempli avec les prix et catégories Hypixel BedWars

### ⚙️ Configuration
- **Commande `/bd`** : Interface d'administration complète via commandes
- **Menu GUI joueur** : Liste des parties disponibles accessible à tous
- **Système de sauvegarde** : Restauration automatique des blocs de la zone de jeu

## 📋 Commandes

| Commande | Description |
|----------|-------------|
| `/bd create <nom>` | Créer une nouvelle arène |
| `/bd gui` | Menu des parties disponibles |
| `/bd join <nom>` | Rejoindre une partie |
| `/bd leave` | Quitter la partie en cours |
| `/bd list` | Lister toutes les arènes |
| `/bd <nom> delete` | Supprimer une arène |
| `/bd admin gui` | Menu d'administration |
| `/bd <nom> equipe <2/4/6/8> <joueurs>` | Configurer les équipes |
| `/bd <nom> pos1/pos2/posconfirm` | Définir la zone de jeu |
| `/bd <nom> bed <couleur>` | Configurer les lits |
| `/bd <nom> spawn <couleur>` | Définir les spawns |
| `/bd shop shop/upgrade custom...` | Personnaliser le shop |
| `/bd <nom> save` | Sauvegarder l'arène |
| `/bd <nom> config` | Mode configuration |

## 🔑 Permissions

- `bedwars.admin` — Accès administration (OP par défaut)
- `bedwars.join` — Rejoindre une partie (Tout le monde)

## ⚙️ Compilation

```bash
mvn clean package
```

Le fichier JAR final se trouve dans `target/BedwarsPlugin.jar`. Placez ce fichier dans le dossier `plugins/` de votre serveur Paper 1.21+.

**Dépendances optionnelles :**
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) — Active les NPCs à skin de joueur (sinon utilise des villageois)

## 📁 Structure du projet

```
com.bedwars
 ├─ BedwarsPlugin.java          (point d'entrée)
 ├─ arena/                      (modèle de données + persistance des maps)
 ├─ commands/                   (commandes /bd)
 ├─ game/                       (logique d'une partie, GameManager)
 ├─ gui/                        (menu joueur listant les parties)
 ├─ shop/                       (config shop/upgrade, GUI, contenu par défaut Hypixel)
 ├─ npc/                        (NPC shop/upgrade : ProtocolLib ou repli villageois)
 ├─ listeners/                  (combat, lits, protection, ramassage, shop)
 ├─ scoreboard/                 (scoreboard en jeu)
 └─ util/                       (Location, région, lits, lobby, cage spectateur)
```

## 🆕 Changelog

### v1.0.1
- **Correction** : Import LeatherArmorMeta corrigé pour Paper 1.21
- **Pré-compilé** : JAR prêt à l'emploi disponible

### Fonctionnalités incluses
- Lobby flottant automatique centré au-dessus de la zone de jeu
- Cage spectateur automatique avec confinement par mouvement
- NPCs avec skin de joueur (via ProtocolLib) ou villageois (fallback)
- Boutique et upgrades pré-configurés style Hypixel
- Système de pièges avec niveaux (TrapA, TrapC, TrapM)
- Confirmation de suppression d'arène (15 secondes)
- Countdown de 10 secondes pour le lancement
- La laine se colore automatiquement selon l'équipe acheteuse
- Générateurs de ressources (fer, or, diamant, émeraude)
- Mort subite avec dragons

---
