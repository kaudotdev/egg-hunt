package org.example.kau.eggHunt;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class StartCommand implements CommandExecutor {
    private final EggHunt plugin;

    public StartCommand(EggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("egghunt.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        plugin.startEvent();
        sender.sendMessage(ChatColor.GREEN + "Evento de caça aos ovos iniciado!");
        return true;
    }
}

class SetCorner1Command implements CommandExecutor {
    private final EggHunt plugin;

    public SetCorner1Command(EggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }

        if (!sender.hasPermission("egghunt.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        Player player = (Player) sender;
        plugin.setCorner1(player.getLocation());
        player.sendMessage(ChatColor.GREEN + "Primeiro canto definido na sua localização atual.");
        return true;
    }
}

class SetCorner2Command implements CommandExecutor {
    private final EggHunt plugin;

    public SetCorner2Command(EggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }

        if (!sender.hasPermission("egghunt.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        Player player = (Player) sender;
        plugin.setCorner2(player.getLocation());
        player.sendMessage(ChatColor.GREEN + "Segundo canto definido na sua localização atual.");
        return true;
    }
}

class SetWaitingRoomCommand implements CommandExecutor {
    private final EggHunt plugin;

    public SetWaitingRoomCommand(EggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }

        if (!sender.hasPermission("egghunt.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        Player player = (Player) sender;
        plugin.setWaitingRoom(player.getLocation());
        player.sendMessage(ChatColor.GREEN + "Sala de espera definida na sua localização atual.");
        return true;
    }
}

class ReloadCommand implements CommandExecutor {
    private final EggHunt plugin;

    public ReloadCommand(EggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("egghunt.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        plugin.reloadConfig();
        plugin.setConfig(plugin.getConfig());
        plugin.loadConfigLocations();

        sender.sendMessage(ChatColor.GREEN + "Configuração do EggHunt recarregada com sucesso!");
        return true;
    }
}

