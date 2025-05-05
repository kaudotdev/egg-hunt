package org.example.kau.eggHunt;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.block.Block;


import java.util.*;

public class CombatPhase implements Listener {
    private final EggHunt plugin;
    private Map<UUID, Integer> playerEggs = new HashMap<>();
    private boolean phaseActive = false;
    private BukkitTask combatTask = null;
    private ScoreboardManager scoreboardManager;
    private Map<UUID, ItemStack[]> playerInventories = new HashMap<>();
    private Map<UUID, ItemStack[]> playerArmor = new HashMap<>();
    private UUID playerWithMostEggs = null;
    private LockingSystem lockingSystem;

    private static final int COMBAT_PHASE_TIME = 12; 
    private static final String SWORD_NAME = ChatColor.RED + "Roubador de Ovos";

    public CombatPhase(EggHunt plugin) {
        this.plugin = plugin;
        this.scoreboardManager = Bukkit.getScoreboardManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        this.lockingSystem = new LockingSystem(plugin);
    }

    /**
     * @param playerEggsFromPhase1
     */
    public void startCombatPhase(Map<UUID, Integer> playerEggsFromPhase1) {
        if (phaseActive) {
            return;
        }

        this.playerEggs = new HashMap<>(playerEggsFromPhase1);
        phaseActive = true;

        plugin.getEggManager().clearEggs();

        preparePlayersForCombat();

        startCombatCountdown();

        updatePlayerWithMostEggs();

        combatTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (phaseActive) {
                    endCombatPhase();
                }
            }
        }.runTaskLater(plugin, COMBAT_PHASE_TIME * 60 * 20L);
    }

    private void preparePlayersForCombat() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerInventories.put(player.getUniqueId(), player.getInventory().getContents());
            playerArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());

            player.getInventory().clear();

            ItemStack sword = createCombatSword();
            player.getInventory().addItem(sword);

            lockingSystem.giveItemsToPlayer(player);

            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));

            int eggCount = playerEggs.getOrDefault(player.getUniqueId(), 0);
            player.sendMessage(ChatColor.RED + "Proteja seus " + eggCount + " ovos. Boa sorte :}");
        }

        setupCombatScoreboard();
    }

    private ItemStack createCombatSword() {
        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        meta.setDisplayName(SWORD_NAME);
        meta.setUnbreakable(true);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Use para roubar ovos de outros jogadores");
        lore.add(ChatColor.RED + "Mate-os a sangue frio");
        meta.setLore(lore);

        meta.addEnchant(Enchantment.KNOCKBACK, 1, true);

        sword.setItemMeta(meta);
        return sword;
    }

    private void startCombatCountdown() {
        for (int i = 5; i >= 1; i--) {
            final int count = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle(
                                ChatColor.RED + "FASE DE COMBATE",
                                ChatColor.GOLD + "Começa em " + count + " segundos",
                                5, 10, 5
                        );

                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    }
                }
            }.runTaskLater(plugin, (5 - count) * 20L);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (phaseActive) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle(
                                ChatColor.RED + "ROUBE OS OVOS!",
                                ChatColor.GOLD + "Derrote outros jogadores!",
                                10, 40, 10
                        );

                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);

                        player.sendMessage(ChatColor.RED + "========================================");
                        player.sendMessage(ChatColor.RED + "A FASE DE COMBATE COMEÇOU!");
                        player.sendMessage(ChatColor.GOLD + "Derrote outros jogadores para roubar seus ovos!");
                        player.sendMessage(ChatColor.YELLOW + "O jogador com mais ovos está brilhando!");
                        player.sendMessage(ChatColor.RED + "========================================");
                    }

                    startGlowingEffectTask();
                }
            }
        }.runTaskLater(plugin, 100L);
    }

    private void setupCombatScoreboard() {
        Scoreboard scoreboard = scoreboardManager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("combathunt", "dummy", ChatColor.RED + "Fase de Combate");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.getScore(ChatColor.WHITE + "Tempo restante:").setScore(15);
        objective.getScore(ChatColor.YELLOW + timeFormat(COMBAT_PHASE_TIME * 60)).setScore(14);
        objective.getScore(" ").setScore(13);
        objective.getScore(ChatColor.WHITE + "Top Jogadores:").setScore(12);

        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = scoreboard.registerNewTeam(player.getName());
            team.addEntry(player.getName());

            objective.getScore(player.getName()).setScore(playerEggs.getOrDefault(player.getUniqueId(), 0));

            player.setScoreboard(scoreboard);
        }

        startScoreboardTimerTask();
    }

    private String timeFormat(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void startScoreboardTimerTask() {
        int totalSeconds = COMBAT_PHASE_TIME * 60;

        new BukkitRunnable() {
            int secondsLeft = totalSeconds;

            @Override
            public void run() {
                if (!phaseActive || secondsLeft <= 0) {
                    this.cancel();
                    return;
                }

                secondsLeft--;
                String timeFormatted = timeFormat(secondsLeft);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    Scoreboard board = player.getScoreboard();
                    Objective objective = board.getObjective("combathunt");

                    if (objective != null) {
                        for (String entry : board.getEntries()) {
                            if (entry.startsWith(ChatColor.YELLOW.toString())) {
                                board.resetScores(entry);
                            }
                        }

                        objective.getScore(ChatColor.YELLOW + timeFormatted).setScore(14);

                        if (secondsLeft <= 30 && secondsLeft % 5 == 0) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startGlowingEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!phaseActive) {
                    this.cancel();
                    return;
                }

                updatePlayerWithMostEggs();
                updateScoreboard();
            }
        }.runTaskTimer(plugin, 20L, 100L); 
    }

    private void updatePlayerWithMostEggs() {
        if (playerWithMostEggs != null) {
            Player oldLeader = Bukkit.getPlayer(playerWithMostEggs);
            if (oldLeader != null && oldLeader.isOnline()) {
                oldLeader.removePotionEffect(PotionEffectType.GLOWING);
                oldLeader.removePotionEffect(PotionEffectType.SPEED);
                oldLeader.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            }
        }

        Optional<Map.Entry<UUID, Integer>> leadingPlayer = playerEggs.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (leadingPlayer.isPresent() && leadingPlayer.get().getValue() > 0) {
            playerWithMostEggs = leadingPlayer.get().getKey();
            Player leader = Bukkit.getPlayer(playerWithMostEggs);

            if (leader != null && leader.isOnline()) {
                leader.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120 * 20, 0, false, false));

                leader.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120 * 20, 0, false, false)); 
                leader.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 120 * 20, 0, false, false)); 

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(ChatColor.GOLD + leader.getName() +
                            " está liderando com " + leadingPlayer.get().getValue() + " ovos!" +
                            ChatColor.YELLOW + " (Recebeu Speed I e Resistance I)");
                }
            }
        }
    }


    private void updateScoreboard() {
        List<Map.Entry<UUID, Integer>> sortedPlayers = new ArrayList<>(playerEggs.entrySet());
        sortedPlayers.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard board = player.getScoreboard();
            Objective objective = board.getObjective("combathunt");

            if (objective != null) {
                for (String entry : new ArrayList<>(board.getEntries())) {
                    if (!entry.startsWith(ChatColor.WHITE.toString()) &&
                            !entry.startsWith(ChatColor.YELLOW.toString()) &&
                            !entry.equals(" ")) {
                        board.resetScores(entry);
                    }
                }

                int position = 0;
                for (Map.Entry<UUID, Integer> entry : sortedPlayers) {
                    if (position >= 10) break; 

                    Player ranked = Bukkit.getPlayer(entry.getKey());
                    if (ranked != null) {
                        Team team = board.getTeam(ranked.getName());
                        if (team != null) {
                            team.setPrefix(ChatColor.GOLD + "🥚 " + entry.getValue() + " ");
                        }

                        objective.getScore(ranked.getName()).setScore(entry.getValue());
                    }
                    position++;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!phaseActive) return;

        Player victim = event.getEntity();
        UUID victimId = victim.getUniqueId();

        int currentEggs = playerEggs.getOrDefault(victimId, 0);
        if (currentEggs <= 0) return;

        int eggsLost = (int) Math.ceil(currentEggs * 0.3); 
        int remainingEggs = currentEggs - eggsLost;

        playerEggs.put(victimId, remainingEggs);

        Player killer = victim.getKiller();

        if (killer != null) {
            UUID killerId = killer.getUniqueId();
            int killerEggs = playerEggs.getOrDefault(killerId, 0);
            playerEggs.put(killerId, killerEggs + eggsLost);

            victim.sendMessage(ChatColor.RED + "Você morreu e perdeu " + eggsLost + " ovos! Agora você tem " + remainingEggs + " ovos.");
            killer.sendMessage(ChatColor.GREEN + "Você matou " + victim.getName() + " e ganhou " + eggsLost + " ovos!");

            killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        } else {
            victim.sendMessage(ChatColor.RED + "Você morreu e perdeu " + eggsLost + " ovos! Agora você tem " + remainingEggs + " ovos.");

            Bukkit.broadcastMessage(ChatColor.GOLD + victim.getName() + " morreu e perdeu " + eggsLost + " ovos!");
        }

        victim.playSound(victim.getLocation(), Sound.ENTITY_BLAZE_DEATH, 0.5f, 1.0f);

        updatePlayerWithMostEggs();
        updateScoreboard();

        ItemStack[] contents = event.getDrops().toArray(new ItemStack[0]);
        event.getDrops().clear();

        for (ItemStack item : contents) {
            if (item != null && item.getType() == Material.STONE_SWORD &&
                    item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().equals(SWORD_NAME)) {
                continue;
            }

            if (item != null &&
                    ((item.getType() == Material.IRON_DOOR &&
                            item.hasItemMeta() &&
                            item.getItemMeta().hasDisplayName() &&
                            item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Tranca Temporal")) ||
                            (item.getType() == Material.TRIPWIRE_HOOK &&
                                    item.hasItemMeta() &&
                                    item.getItemMeta().hasDisplayName() &&
                                    item.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "Lockpick Mágico")))) {
                continue;
            }

            event.getDrops().add(item);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (victim.isOnline()) {
                    if (!playerHasCombatSword(victim)) {
                        victim.getInventory().addItem(createCombatSword());
                    }

                    boolean hasLockpick = false;
                    boolean hasDoorLock = false;

                    for (ItemStack item : victim.getInventory().getContents()) {
                        if (item != null && item.getType() == Material.TRIPWIRE_HOOK &&
                                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                                item.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "Lockpick Mágico")) {
                            hasLockpick = true;
                        }

                        if (item != null && item.getType() == Material.IRON_DOOR &&
                                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                                item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Tranca Temporal")) {
                            hasDoorLock = true;
                        }
                    }

                    if (!hasLockpick || !hasDoorLock) {
                        lockingSystem.giveItemsToPlayer(victim);
                    }
                }
            }
        }.runTaskLater(plugin, 5L);
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!phaseActive) return;

        Player player = event.getPlayer();

        if (plugin.corner1 != null && plugin.corner2 != null) {
            Location respawnLoc = findSafeLocation();
            if (respawnLoc != null) {
                event.setRespawnLocation(respawnLoc);
            }
        }

        if (!playerHasCombatSword(player)) {
            player.getInventory().addItem(createCombatSword());
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 4)); // 5 segundos
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!phaseActive) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        int playerEggCount = playerEggs.getOrDefault(playerId, 0);
        if (playerEggCount <= 0) return;

        List<Player> onlinePlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(playerId)) {
                onlinePlayers.add(p);
            }
        }

        int onlineCount = onlinePlayers.size();
        if (onlineCount <= 0) return;

        int eggsPerPlayer = playerEggCount / onlineCount;
        int remainingEggs = playerEggCount % onlineCount;

        for (Player recipient : onlinePlayers) {
            UUID recipientId = recipient.getUniqueId();
            int currentEggs = playerEggs.getOrDefault(recipientId, 0);
            int extraEgg = 0;

            if (remainingEggs > 0) {
                extraEgg = 1;
                remainingEggs--;
            }

            int eggsToAdd = eggsPerPlayer + extraEgg;
            playerEggs.put(recipientId, currentEggs + eggsToAdd);

            recipient.sendMessage(
                    ChatColor.GOLD + player.getName() + " desconectou! " +
                            ChatColor.GREEN + "Você recebeu " + eggsToAdd + " ovos."
            );

            recipient.playSound(recipient.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        }

        Bukkit.broadcastMessage(
                ChatColor.RED + player.getName() + " desconectou durante a fase de combate!" +
                        ChatColor.GOLD + " Seus " + playerEggCount + " ovos foram distribuídos entre os jogadores restantes."
        );

        playerEggs.remove(playerId);

        updatePlayerWithMostEggs();
        updateScoreboard();
    }
    private boolean playerHasCombatSword(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.IRON_SWORD &&
                    item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().equals(SWORD_NAME)) {
                return true;
            }
        }
        return false;
    }

    private Location findSafeLocation() {
        Random random = new Random();

        if (plugin.corner1 == null || plugin.corner2 == null) {
            return null;
        }

        int minX = Math.min((int)plugin.corner1.getX(), (int)plugin.corner2.getX());
        int maxX = Math.max((int)plugin.corner1.getX(), (int)plugin.corner2.getX());
        int minZ = Math.min((int)plugin.corner1.getZ(), (int)plugin.corner2.getZ());
        int maxZ = Math.max((int)plugin.corner1.getZ(), (int)plugin.corner2.getZ());

        for (int attempt = 0; attempt < 10; attempt++) {
            int x = minX + random.nextInt(maxX - minX + 1);
            int z = minZ + random.nextInt(maxZ - minZ + 1);

            int y = plugin.corner1.getWorld().getHighestBlockYAt(x, z);

            Location loc = new Location(plugin.corner1.getWorld(), x, y + 1, z);
            Block block = loc.getBlock();
            Block blockBelow = loc.getWorld().getBlockAt(x, y, z);

            if (block.getType().isAir() && !blockBelow.isLiquid() && blockBelow.getType().isSolid()) {
                return loc;
            }
        }

        return null;
    }

    public void endCombatPhase() {
        if (!phaseActive) return;

        if (combatTask != null) {
            combatTask.cancel();
            combatTask = null;
        }

        phaseActive = false;

        lockingSystem.cleanupAllData();

        if (!playerEggs.isEmpty()) {
            Map.Entry<UUID, Integer> winner = Collections.max(playerEggs.entrySet(), Comparator.comparingInt(Map.Entry::getValue));
            Player winnerPlayer = Bukkit.getPlayer(winner.getKey());

            if (winnerPlayer != null) {
                Bukkit.broadcastMessage(ChatColor.GOLD + "==============================================");
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Caça aos Ovos] O grande campeão é " + winnerPlayer.getName() +
                        " com " + winner.getValue() + " ovos!");
                Bukkit.broadcastMessage(ChatColor.GOLD + "==============================================");

                winnerPlayer.playSound(winnerPlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendTitle(
                            ChatColor.GOLD + "CAMPEÃO FINAL: " + winnerPlayer.getName(),
                            ChatColor.GREEN + "Com " + winner.getValue() + " ovos!",
                            10, 70, 20
                    );
                    if (player != winnerPlayer) {
                        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f);
                    }
                }

                if (plugin.getPluginConfig().getBoolean("give-rewards", true)) {
                    List<String> rewards = plugin.getPluginConfig().getStringList("winner-commands");
                    for (String command : rewards) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                command.replace("%player%", winnerPlayer.getName()));
                    }
                }
            }
        } else {
            Bukkit.broadcastMessage(ChatColor.RED + "[Caça aos Ovos] O evento terminou sem vencedores!");
        }

        restorePlayerInventories();

        if (plugin.waitingRoom != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleport(plugin.waitingRoom);

                player.removePotionEffect(PotionEffectType.GLOWING);
                player.removePotionEffect(PotionEffectType.SPEED);
                player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            }
        }
    }


    private void restorePlayerInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();

            if (playerInventories.containsKey(playerId)) {
                player.getInventory().setContents(playerInventories.get(playerId));
            } else {
                player.getInventory().clear();
            }


            if (playerArmor.containsKey(playerId)) {
                player.getInventory().setArmorContents(playerArmor.get(playerId));
            }
        }


        playerInventories.clear();
        playerArmor.clear();
    }

    public boolean isPhaseActive() {
        return phaseActive;
    }

    public Map<UUID, Integer> getPlayerEggs() {
        return playerEggs;
    }
}