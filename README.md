# 🎮 BedwarsPlugin — Paper 1.21

**SimpleBedwars** est un plugin Minecraft complet pour créer et gérer des parties de Bed Wars sur un serveur Paper 1.21. Configurez vos arènes, gérez les équipes, et lancez des parties excitantes avec une interface d'administration intuitive.

## ✨ Fonctionnalités

### 🏰 Création d'Arènes
- Création simple d'arènes avec la commande `/bd create <nom>`
- Configuration flexible du nombre d'équipes (2, 4, 6 ou 8)
- Définition des spawns d'équipe et positions de lits
- Système de lobby configurable avec points d'entrée

### ⚔️ Gameplay Complet
- **Générateurs de ressources** : Fer, Or, Diamant, Émeraude
- **Système de lits** : Protection du lit de votre équipe
- **Mort subite** : Dragons Ender apparaissent quand il reste 5 minutes
- **Montées de niveau** : Taux de generation augmentés à 25 minutes puis 5 minutes

### 🛒 Magasins & Améliorations
- NPCs de boutique pour chaque équipe avec couleurs personnalisées
- Système d'amélioration d'équipe

### 🎨 Interface d'Administration
- GUI d'administration interactif via `/bd admin gui`
- Configuration en jeu avec assistant de configuration
- Sauvegarde et restauration automatique des maps

### 📊 Scoreboard Dynamique
- Affichage en temps réel du score et de l'état de la partie
- Suivi des éliminations et des lits cassés

## 📋 Commandes

| Commande | Description |
|----------|-------------|
| `/bd create <nom>` | Créer une nouvelle arène |
| `/bd admin gui` | Ouvrir le GUI d'administration |
| `/bd join <nom>` | Rejoindre une partie |
| `/bd leave` | Quitter la partie en cours |
| `/bd list` | Lister toutes les arènes |
| `/bd <nom> team <2/4/6/8> <joueurs>` | Configurer les équipes |
| `/bd <nom> pos1/pos2/confirm` | Définir la zone de jeu |
| `/bd <nom> bed <couleur>` | Définir la position du lit |
| `/bd <nom> spawn <couleur>` | Définir le point de spawn |
| `/bd <nom> item <type>` | Configurer les générateurs |
| `/bd <nom> shop <shop/upgrade> color <couleur>` | Configurer les NPCs |
| `/bd <nom> lobby pos1/pos2/confirm` | Définir le lobby |
| `/bd <nom> save` | Sauvegarder l'arène |
| `/bd <nom> config` | Voir la configuration |

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
2. Redémarrez le serveur
3. Configurez vos arènes avec les commandes ci-dessus

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
 └─ util/                       (Utilitaires)
```

## 📋 Prérequis

- Java 21
- Serveur Paper 1.21
- Maven 3.6+ (pour compiler)
