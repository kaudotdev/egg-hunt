package org.example.kau.eggHunt;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EndEventCommand implements CommandExecutor {
    private final EggHunt plugin;

    public EndEventCommand(EggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("egghunt.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage(ChatColor.GOLD + "[Caça aos Ovos] Finalizando o evento...");
        }

        CombatPhase combatPhase = plugin.getCombatPhase();
        if (combatPhase != null && combatPhase.isPhaseActive()) {
            combatPhase.endCombatPhase();
            sender.sendMessage(ChatColor.GREEN + "Fase de combate finalizada!");
        } else {
            plugin.endEvent();
            sender.sendMessage(ChatColor.GREEN + "Evento de caça aos ovos finalizado!");
        }

        return true;
    }
}
