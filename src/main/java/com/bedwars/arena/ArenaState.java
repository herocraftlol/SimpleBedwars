package com.bedwars.arena;

public enum ArenaState {
    /** En cours de configuration (pas encore sauvegardée / confirmée). */
    SETUP,
    /** Configurée, en attente de joueurs (lobby ouvert). */
    WAITING,
    /** Lobby plein, compte à rebours de 20 secondes avant lancement. */
    STARTING,
    /** Partie en cours (phase normale). */
    PLAYING,
    /** Partie en cours (mort subite). */
    SUDDEN_DEATH,
    /** Partie terminée, en cours de réinitialisation. */
    ENDING
}
