# 🎮 BedwarsPlugin — Paper 1.21

**SimpleBedwars** est un plugin Minecraft complet et configurable pour créer et gérer des parties de Bed Wars sur un serveur Paper 1.21. Avec une interface d'administration intuitive et un système de shop extensible, gérez vos arènes en toute simplicité.

## ✨ Fonctionnalités

### 🏰 Création d'Arènes
- Création simple d'arènes avec la commande `/bd create <nom>`
- Configuration flexible du nombre d'équipes (2, 4, 6 ou 8)
- Définition des spawns d'équipe et positions de lits
- Système de lobby configurable avec points d'entrée
- Suppression d'arènes avec `/bd delete <nom>`

### ⚔️ Gameplay Complet
- **Générateurs de ressources** : Fer, Or, Diamant, Émeraude avec montée en niveau
- **Système de lits** : Protection du lit de votre équipe
- **Mort subite** : Dragons Ender apparaissent quand il reste 5 minutes
- **Montées de niveau** : Taux de génération augmentés à 25 minutes puis 5 minutes

### 🛒 Système de Shop Configurable
- NPCs de boutique pour chaque équipe avec skins de joueur
- **Avec ProtocolLib** : NPCs avec skin de joueur classique, immobiles et tournant la tête vers le joueur le plus proche
- **Sans ProtocolLib** : repli automatique sur des villageois (garanti fonctionnel)
- Système d'amélioration d'équipe extensible
- Interface d'achat intuitive

### 🔧 Améliorations d'Équipe
- **Heal** : Régénération de vie augmentée
- **Sharp** : Augmentation des dégâts d'attaque
- **Armur** : Augmentation de la protection d'armure
- **Maniac** : Vitesse de minage accrue
- **Chute** : Réduction des dégâts de chute
- **Forge** : Génération de ressources accélérée
- **TrapA / TrapC / TrapM** : Pièges avec détection et effets
- **Dragon** : Bonus du dragon en phase finale

### 🎨 Interface d'Administration
- GUI d'administration interactif via `/bd admin gui`
- Menu joueur pour rejoindre les parties via `/bd gui`
- Configuration en jeu avec assistant de configuration
- Sauvegarde et restauration automatique des maps

### 📊 Scoreboard Dynamique
- Affichage en temps réel du score et de l'état de la partie
- Suivi des éliminations et des lits cassés

## 📋 Commandes

| Commande | Description |
|----------|-------------|
| `/bd create <nom>` | Créer une nouvelle arène |
| `/bd delete <nom>` | Supprimer une arène |
| `/bd gui` | Menu joueur listant les parties |
| `/bd admin gui` | Ouvrir le GUI d'administration |
| `/bd join <nom>` | Rejoindre une partie |
| `/bd leave` | Quitter la partie en cours |
| `/bd list` | Lister toutes les arènes |
| `/bd <nom> equipe <2/4/6/8> <joueurs>` | Configurer les équipes |
| `/bd <nom> pos1/pos2/confirm` | Définir la zone de jeu |
| `/bd <nom> bed <couleur>` | Définir la position du lit |
| `/bd <nom> spawn <couleur>` | Définir le point de spawn |
| `/bd <nom> item <type>` | Configurer les générateurs |
| `/bd <nom> shop <shop/upgrade> color <couleur>` | Configurer les NPCs |
| `/bd <nom> lobby pos1/pos2/confirm` | Définir le lobby |
| `/bd <nom> save` | Sauvegarder l'arène |
| `/bd <nom> config` | Voir la configuration |
| `/bd shop shop custom <gui> slot <n> item <item> [price <n>]` | Ajouter un item au shop |
| `/bd shop upgrade custom slot <n> item <type> price <n> diamand` | Ajouter une amélioration |

## 🔑 Permissions

| Permission | Description | Par défaut |
|------------|-------------|------------|
| `bedwars.admin` | Accès complet à l'administration | OP |
| `bedwars.join` | Rejoindre une partie | Tous |

## 🛠️ Compilation

```bash
# Cloner le dépôt
git clone https://github.com/herocraftlol/SimpleBedwars.git
cd SimpleBedwars

# Compiler avec Maven
mvn clean package

# Le JAR sera dans target/BedwarsPlugin.jar
```

## 📦 Installation

1. Placez `BedwarsPlugin.jar` dans le dossier `plugins/` de votre serveur Paper 1.21
2. **(Optionnel)** Installez [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) pour activer les NPCs avec skin de joueur
3. Redémarrez le serveur
4. Configurez vos arènes avec les commandes ci-dessus

## 📋 Prérequis

- Java 21
- Serveur Paper 1.21
- Maven 3.6+ (pour compiler)
- ProtocolLib (optionnel, pour les NPCs avec skin)

## 🏗️ Structure du Code

```
com.bedwars
 ├─ BedwarsPlugin.java          (Point d'entrée)
 ├─ arena/                      (Modèles de données et persistance)
 ├─ commands/                   (Gestion des commandes)
 ├─ game/                       (Logique de jeu)
 ├─ gui/                        (Interfaces graphiques)
 ├─ listeners/                  (Écouteurs d'événements)
 ├─ scoreboard/                 (Gestion du scoreboard)
 ├─ shop/                       (Système de shop et upgrades)
 ├─ npc/                        (NPCs avec ProtocolLib)
 └─ util/                       (Utilitaires)
```

## 🆕 Historique des Versions

### v1.0.2
- Ajout du système de shop configurable avec GUI personnalisé
- Ajout des améliorations d'équipe (Heal, Sharp, Armur, Maniac, etc.)
- NPCs avec skin de joueur via ProtocolLib
- Nouveau menu joueur `/bd gui` pour rejoindre les parties
- Système de pièges (TrapA, TrapC, TrapM)
- Dragons de mort subite avec comportement amélioré
- Sol invisible du lobby en attente de joueurs
- Suppression d'arènes avec `/bd delete`

### v1.0.0
- Version initiale du plugin Bedwars
