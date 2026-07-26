package com.bedwars.npc.provider;

import com.bedwars.npc.NpcRecord;
import org.bukkit.entity.Player;

/**
 * Abstraction du "moteur" d'affichage d'un NPC shop/upgrade.
 * Volontairement séparée de HumanNPCManager pour que les classes qui référencent
 * ProtocolLib ne soient chargées par la JVM que si ProtocolLib est réellement présent
 * (chargement paresseux des classes = pas de crash si la dépendance manque).
 */
public interface NpcProvider {

    /** Fait apparaître le NPC pour tous les joueurs déjà connectés. */
    void spawn(NpcRecord record);

    /** Fait apparaître un NPC déjà existant pour un joueur qui vient de se connecter. */
    void showTo(NpcRecord record, Player viewer);

    /** Retire le NPC pour tout le monde. */
    void despawn(NpcRecord record);

    /** Oriente le NPC pour qu'il regarde en direction du joueur donné. */
    void lookAt(NpcRecord record, Player target);

    /** true si ce provider gère les clics lui-même (paquets), false s'il faut écouter PlayerInteractEntityEvent. */
    boolean handlesInteractionItself();
}
