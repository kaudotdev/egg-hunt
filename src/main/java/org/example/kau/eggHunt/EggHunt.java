package org.example.kau.eggHunt;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import  org.example.kau.eggHunt.EventGUI.EventSettings;

import java.util.*;
import java.util.logging.Logger;

public class EggHunt extends JavaPlugin implements Listener {
    private Map<UUID, Integer> playerEggs = new HashMap<>();
    private boolean eventActive = false;
    private FileConfiguration config;
    public Location corner1, corner2;
    public Location waitingRoom;
    private BukkitTask huntTask = null;
    private ScoreboardManager scoreboardManager;
    private Map<UUID, Scoreboard> originalScoreboards = new HashMap<>();
    private CombatPhase combatPhase;

    private EggManager eggManager;
    private Set<UUID> blacklistedPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);
        scoreboardManager = Bukkit.getScoreboardManager();

        eggManager = new EggManager(this);
        combatPhase = new CombatPhase(this);

        getCommand("starthunt").setExecutor(new StartCommand(this));
        getCommand("endevent").setExecutor(new EndEventCommand(this)); // Novo comando para finalizar o evento
        getCommand("event").setExecutor(new EventGUI(this)); // Novo comando para a GUI de configuração
        getCommand("setcorner1").setExecutor(new SetCorner1Command(this));
        getCommand("setcorner2").setExecutor(new SetCorner2Command(this));
        getCommand("setwaitingroom").setExecutor(new SetWaitingRoomCommand(this));

        loadConfigLocations();
    }



    public EggManager getEggManager() {
        return eggManager;
    }

    public CombatPhase getCombatPhase() {
        return combatPhase;
    }

    public void setConfig(FileConfiguration config) {
        this.config = config;
    }

    public void setBlacklistedPlayers(Set<UUID> blacklistedPlayers) {
        this.blacklistedPlayers = new HashSet<>(blacklistedPlayers);
    }

    public void loadConfigLocations() {
        if (config.contains("corner1") && config.contains("corner2") && config.contains("waitingRoom")) {
            String world = config.getString("world", "world");

            double x1 = config.getDouble("corner1.x");
            double y1 = config.getDouble("corner1.y");
            double z1 = config.getDouble("corner1.z");
            corner1 = new Location(Bukkit.getWorld(world), x1, y1, z1);

            double x2 = config.getDouble("corner2.x");
            double y2 = config.getDouble("corner2.y");
            double z2 = config.getDouble("corner2.z");
            corner2 = new Location(Bukkit.getWorld(world), x2, y2, z2);

            double xw = config.getDouble("waitingRoom.x");
            double yw = config.getDouble("waitingRoom.y");
            double zw = config.getDouble("waitingRoom.z");
            waitingRoom = new Location(Bukkit.getWorld(world), xw, yw, zw);

            getLogger().info("Localizações carregadas com sucesso!");
        } else {
            getLogger().warning("Algumas localizações não estão configuradas! Use /setcorner1, /setcorner2 e /setwaitingroom.");
        }
    }

    public void startEvent() {
        if (corner1 == null || corner2 == null || waitingRoom == null) {
            Bukkit.broadcastMessage(ChatColor.RED + "[Caça aos Ovos] Erro: Coordenadas não configuradas. Use /setcorner1, /setcorner2 e /setwaitingroom.");
            return;
        }

        if (eventActive) {
            endEvent();
        }

        eventActive = true;
        playerEggs.clear();
        originalScoreboards.clear();

        eggManager.clearEggs();

        for (Player player : Bukkit.getOnlinePlayers()) {
            originalScoreboards.put(player.getUniqueId(), player.getScoreboard());
        }

        if (huntTask != null) {
            huntTask.cancel();
            huntTask = null;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (blacklistedPlayers.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "[Caça aos Ovos] Você está na lista de exclusão e não participará do evento.");
                continue;
            }

            player.teleport(waitingRoom);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 220, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 220, 10));
        }

        startCountdown();

        new BukkitRunnable() {
            @Override
            public void run() {
                eggManager.spawnInitialEggs(corner1, corner2);
                int eggCount = config.getInt("egg-count", 10);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Caça aos Ovos] " + eggCount + " ovos foram escondidos no mapa!");
            }
        }.runTaskLater(this, 20L);

        huntTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (eventActive) {
                    endEvent();
                }
            }
        }.runTaskLater(this, config.getInt("egg-hunt-time", 15) * 60 * 20L);

        blacklistedPlayers.clear();
    }

    private void startCountdown() {
        for (int i = 10; i >= 1; i--) {
            final int count = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle(
                                ChatColor.GOLD + "A caça aos ovos começa em",
                                ChatColor.RED + "" + count + ChatColor.GOLD + " segundos",
                                5, 10, 5
                        );

                        float pitch = 0.5f + ((10-count) * 0.05f); 
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
                    }
                }
            }.runTaskLater(this, (10 - count) * 20L);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (eventActive) {
                    teleportPlayersToMap();
                    setupScoreboard();

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle(
                                ChatColor.GOLD + "A CAÇA COMEÇOU!",
                                ChatColor.GREEN + "Encontre os ovos!",
                                10, 40, 10
                        );
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    }

                    Bukkit.broadcastMessage(ChatColor.GOLD + "[Caça aos Ovos] A caça aos ovos começou! Você tem " +
                            config.getInt("egg-hunt-time", 15) + " minutos para coletar ovos!");
                }
            }
        }.runTaskLater(this, 200L);
    }

    private void setupScoreboard() {
        Scoreboard scoreboard = scoreboardManager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("egghunt", "dummy", ChatColor.GOLD + "Caça aos Ovos");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (Player player : Bukkit.getOnlinePlayers()) {

            playerEggs.put(player.getUniqueId(), 0);
            Team team = scoreboard.registerNewTeam(player.getName());
            team.addEntry(player.getName());
            player.setScoreboard(scoreboard);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEggCollect(PlayerInteractEvent event) {
        if (!eventActive) return;

        Player player = event.getPlayer();


        Block block = event.getClickedBlock();
        if (!eggManager.isEggCollectionEvent(event)) return;

        if (eggManager.processEggCollection(player, block)) {
            int newCount = playerEggs.getOrDefault(player.getUniqueId(), 0) + 1;
            playerEggs.put(player.getUniqueId(), newCount);

            updatePlayerTag(player);
            updateScoreboard();

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.GREEN + "Você encontrou um ovo! Total: " + newCount));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            new BukkitRunnable() {
                @Override
                public void run() {
                    eggManager.spawnNewEgg(corner1, corner2);
                }
            }.runTaskLater(this, 5L);
        }
    }

    private void updateScoreboard() {
        List<Map.Entry<UUID, Integer>> sortedPlayers = new ArrayList<>(playerEggs.entrySet());
        sortedPlayers.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        for (Player player : Bukkit.getOnlinePlayers()) {

            Scoreboard board = player.getScoreboard();
            Objective objective = board.getObjective("egghunt");

            if (objective != null) {
                for (String entry : board.getEntries()) {
                    board.resetScores(entry);
                }

                int position = 1;
                for (Map.Entry<UUID, Integer> entry : sortedPlayers) {
                    if (position > 5) break; 

                    Player ranked = Bukkit.getPlayer(entry.getKey());
                    if (ranked != null) {
                        objective.getScore(ranked.getName()).setScore(entry.getValue());
                    }
                    position++;
                }
            }
        }
    }

    private void updatePlayerTag(Player player) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

            Scoreboard scoreboard = onlinePlayer.getScoreboard();
            Team team = scoreboard.getTeam(player.getName());

            if (team == null) {
                team = scoreboard.registerNewTeam(player.getName());
                team.addEntry(player.getName());
            }
        }
    }

    public void endEvent() {
        if (!eventActive) return;

        if (huntTask != null) {
            huntTask.cancel();
            huntTask = null;
        }

        eventActive = false;

        if (!playerEggs.isEmpty()) {
            Map.Entry<UUID, Integer> winner = Collections.max(playerEggs.entrySet(), Comparator.comparingInt(Map.Entry::getValue));
            Player winnerPlayer = Bukkit.getPlayer(winner.getKey());

            if (winnerPlayer != null) {
                Bukkit.broadcastMessage(ChatColor.GOLD + "==============================================");
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Caça aos Ovos] O vencedor é " + winnerPlayer.getName() + " com " + winner.getValue() + " ovos!");
                Bukkit.broadcastMessage(ChatColor.GOLD + "==============================================");

                winnerPlayer.playSound(winnerPlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                if (!playerEggs.isEmpty()) {

                    combatPhase.startCombatPhase(playerEggs);

                    return;
                }
            }
        } else {
            Bukkit.broadcastMessage(ChatColor.RED + "[Caça aos Ovos] O evento terminou sem vencedores!");
        }

        resetScoreboardsAndPrefixes();

        for (Player player : Bukkit.getOnlinePlayers()) {

            player.teleport(waitingRoom);
        }

        eggManager.clearEggs();
    }

    private void resetScoreboardsAndPrefixes() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            Scoreboard original = originalScoreboards.get(player.getUniqueId());
            if (original != null) {
                player.setScoreboard(original);
            } else {
                player.setScoreboard(scoreboardManager.getNewScoreboard());
            }
        }
        originalScoreboards.clear();
    }

    public void teleportPlayersToMap() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            Location teleportLoc = findSafeLocation();

            if (teleportLoc != null) {
                player.teleport(teleportLoc);
            } else {
                double midX = (Math.min(corner1.getX(), corner2.getX()) + Math.max(corner1.getX(), corner2.getX())) / 2;
                double midZ = (Math.min(corner1.getZ(), corner2.getZ()) + Math.max(corner1.getZ(), corner2.getZ())) / 2;
                double y = corner1.getWorld().getHighestBlockYAt((int)midX, (int)midZ) + 1;
                player.teleport(new Location(corner1.getWorld(), midX, y, midZ));
            }

            playerEggs.put(player.getUniqueId(), 0); 
            player.sendMessage(ChatColor.GOLD + "Encontre os ovos escondidos pelo mapa! Use o botão direito para coletar.");
        }
    }

    private Location findSafeLocation() {
        Random random = new Random();

        int minX = Math.min((int)corner1.getX(), (int)corner2.getX());
        int maxX = Math.max((int)corner1.getX(), (int)corner2.getX());
        int minZ = Math.min((int)corner1.getZ(), (int)corner2.getZ());
        int maxZ = Math.max((int)corner1.getZ(), (int)corner2.getZ());

        for (int attempt = 0; attempt < 10; attempt++) {
            int x = minX + random.nextInt(maxX - minX + 1);
            int z = minZ + random.nextInt(maxZ - minZ + 1);

            int y = corner1.getWorld().getHighestBlockYAt(x, z);

            Location loc = new Location(corner1.getWorld(), x, y + 1, z);
            Block block = loc.getBlock();
            Block blockBelow = loc.getWorld().getBlockAt(x, y, z);

            if (block.getType().isAir() && !blockBelow.isLiquid() && blockBelow.getType().isSolid()) {
                return loc;
            }
        }

        return null;
    }


    public void setCorner1(Location loc) {
        corner1 = loc;
        config.set("corner1.x", loc.getX());
        config.set("corner1.y", loc.getY());
        config.set("corner1.z", loc.getZ());
        saveConfig();
    }

    public void setCorner2(Location loc) {
        corner2 = loc;
        config.set("corner2.x", loc.getX());
        config.set("corner2.y", loc.getY());
        config.set("corner2.z", loc.getZ());
        saveConfig();
    }

    public void setWaitingRoom(Location loc) {
        waitingRoom = loc;
        config.set("waitingRoom.x", loc.getX());
        config.set("waitingRoom.y", loc.getY());
        config.set("waitingRoom.z", loc.getZ());
        config.set("world", loc.getWorld().getName());
        saveConfig();
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    public Logger getPluginLogger() {
        return getLogger();
    }
}