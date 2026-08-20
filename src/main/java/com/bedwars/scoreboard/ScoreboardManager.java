package com.bedwars.scoreboard;

import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaTeam;
import com.bedwars.arena.TeamColor;
import com.bedwars.game.GameInstance;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;

/**
 * Construit et met à jour un scoreboard propre pour une partie en cours :
 * état de chaque équipe (lit + joueurs vivants) et stats perso (kills / final kills).
 */
public class ScoreboardManager {

    public void update(GameInstance game) {
        Arena arena = game.getArena();
        for (Player player : game.getAllParticipants()) {
            Scoreboard board = player.getScoreboard();
            // Chaque joueur a besoin de son propre scoreboard (pour ses stats perso),
            // on en crée un neuf à chaque tick de rafraîchissement (léger, appelé 1x/seconde).
            Scoreboard newBoard = org.bukkit.Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = newBoard.registerNewObjective("bedwars", Criteria.DUMMY,
                    ChatColor.YELLOW + "" + ChatColor.BOLD + "BEDWARS");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            int score = 15;
            score = writeLine(obj, score, ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "―――――――――――――");
            score = writeLine(obj, score, arena.getName());
            score = writeLine(obj, score, " ");

            for (TeamColor color : TeamColor.forTeamCount(arena.getTeamCount())) {
                ArenaTeam team = arena.getTeams().get(color);
                if (team == null) continue;
                String bedIcon = team.isBedDestroyed() ? ChatColor.RED + "✘" : ChatColor.GREEN + "✔";
                String line = color.getChatColor() + shortName(color) + ChatColor.GRAY + ": " + bedIcon
                        + ChatColor.GRAY + " (" + team.getAlivePlayers().size() + ")";
                score = writeLine(obj, score, line);
            }

            score = writeLine(obj, score, "  ");
            score = writeLine(obj, score, ChatColor.AQUA + game.getPhaseCountdownLabel());
            score = writeLine(obj, score, ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + game.getKills(player.getUniqueId()));
            score = writeLine(obj, score, ChatColor.YELLOW + "Final Kills: " + ChatColor.WHITE + game.getFinalKills(player.getUniqueId()));
            score = writeLine(obj, score, "   ");
            writeLine(obj, score, ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "―――――――――――――");

            player.setScoreboard(newBoard);
        }
    }

    private String shortName(TeamColor color) {
        return color.getDisplayName();
    }

    private int writeLine(Objective obj, int score, String text) {
        // Les scoreboards Bukkit n'autorisent pas deux lignes identiques : on complète avec des couleurs invisibles.
        String unique = text + ChatColor.values()[score % 15].toString() + ChatColor.RESET;
        obj.getScore(unique).setScore(score);
        return score - 1;
    }

    public void clear(Player player) {
        player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
