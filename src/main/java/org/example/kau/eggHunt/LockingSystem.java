package org.example.kau.eggHunt;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class LockingSystem implements Listener {
    private final EggHunt plugin;
    private final Map<Location, Material> lockedDoors = new HashMap<>();
    private final Map<Location, BukkitTask> lockTimers = new HashMap<>();
    private final Map<UUID, Location> unlockingPlayers = new HashMap<>();
    private final Map<UUID, BukkitTask> unlockingTasks = new HashMap<>();

    private final Map<UUID, Long> doorLockCooldowns = new HashMap<>();
    private final Map<UUID, Long> lockpickCooldowns = new HashMap<>();

    private static final int LOCK_DURATION = 3 * 60;
    private static final int UNLOCK_TIME = 10;
    private static final int COOLDOWN_TIME = 30;
    private static final String DOORLOCK_NAME = ChatColor.GOLD + "Tranca Temporal";
    private static final String LOCKPICK_NAME = ChatColor.AQUA + "Lockpick Mágico";

    public LockingSystem(EggHunt plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public ItemStack createDoorLock() {
        ItemStack doorLock = new ItemStack(Material.IRON_DOOR);
        ItemMeta meta = doorLock.getItemMeta();

        meta.setDisplayName(DOORLOCK_NAME);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Tranca uma porta por " + LOCK_DURATION + " segundos");
        lore.add(ChatColor.YELLOW + "Clique com botão direito em uma porta para trancar");
        lore.add(ChatColor.RED + "Cooldown: " + COOLDOWN_TIME + " segundos");
        meta.setLore(lore);

        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        doorLock.setItemMeta(meta);
        return doorLock;
    }

    public ItemStack createLockpick() {
        ItemStack lockpick = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = lockpick.getItemMeta();

        meta.setDisplayName(LOCKPICK_NAME);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Abre uma porta trancada após " + UNLOCK_TIME + " segundos");
        lore.add(ChatColor.RED + "Não pode se mover ou tomar dano durante o processo");
        lore.add(ChatColor.RED + "Cooldown: " + COOLDOWN_TIME + " segundos");
        meta.setLore(lore);

        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        lockpick.setItemMeta(meta);
        return lockpick;
    }

    public void giveItemsToPlayer(Player player) {
        player.getInventory().addItem(createDoorLock());
        player.getInventory().addItem(createLockpick());
        player.sendMessage(ChatColor.GOLD + "Você recebeu uma " + DOORLOCK_NAME +
                ChatColor.GOLD + " e um " + LOCKPICK_NAME + ChatColor.GOLD + "!");
    }

    public boolean isDoorLock(ItemStack item) {
        return item != null &&
                item.getType() == Material.IRON_DOOR &&
                item.hasItemMeta() &&
                item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().getDisplayName().equals(DOORLOCK_NAME);
    }

    public boolean isLockpick(ItemStack item) {
        return item != null &&
                item.getType() == Material.TRIPWIRE_HOOK &&
                item.hasItemMeta() &&
                item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().getDisplayName().equals(LOCKPICK_NAME);
    }

    private int getDoorLockCooldownRemaining(UUID playerID) {
        if (!doorLockCooldowns.containsKey(playerID)) {
            return 0;
        }

        long lastUseTime = doorLockCooldowns.get(playerID);
        long currentTime = System.currentTimeMillis();
        long elapsedTime = (currentTime - lastUseTime) / 1000;

        if (elapsedTime >= COOLDOWN_TIME) {
            doorLockCooldowns.remove(playerID);
            return 0;
        }

        return (int)(COOLDOWN_TIME - elapsedTime);
    }

    private int getLockpickCooldownRemaining(UUID playerID) {
        if (!lockpickCooldowns.containsKey(playerID)) {
            return 0;
        }

        long lastUseTime = lockpickCooldowns.get(playerID);
        long currentTime = System.currentTimeMillis();
        long elapsedTime = (currentTime - lastUseTime) / 1000;

        if (elapsedTime >= COOLDOWN_TIME) {
            lockpickCooldowns.remove(playerID);
            return 0;
        }

        return (int)(COOLDOWN_TIME - elapsedTime);
    }

    private List<Block> getAdjacentDoors(Block doorBlock) {
        List<Block> doors = new ArrayList<>();
        doors.add(doorBlock);

        Block bottomDoor = getDoorBottomBlock(doorBlock);

        Block[] adjacentBlocks = {
                bottomDoor.getRelative(1, 0, 0),
                bottomDoor.getRelative(-1, 0, 0),
                bottomDoor.getRelative(0, 0, 1),
                bottomDoor.getRelative(0, 0, -1)
        };

        for (Block adjacent : adjacentBlocks) {
            if (isDoor(adjacent)) {
                Block adjacentBottom = getDoorBottomBlock(adjacent);
                doors.add(adjacentBottom);
            }
        }

        return doors;
    }

    private boolean isDoor(Block block) {
        Material type = block.getType();
        return type == Material.OAK_DOOR ||
                type == Material.SPRUCE_DOOR ||
                type == Material.BIRCH_DOOR ||
                type == Material.JUNGLE_DOOR ||
                type == Material.ACACIA_DOOR ||
                type == Material.DARK_OAK_DOOR ||
                type == Material.CRIMSON_DOOR ||
                type == Material.WARPED_DOOR ||
                type == Material.IRON_DOOR;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (isDoorLock(item) || isLockpick(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Este item não pode ser colocado!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDoorInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        if (!isDoor(block)) return;

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        Block bottomBlock = getDoorBottomBlock(block);
        List<Block> doubleDoors = getAdjacentDoors(bottomBlock);

        if (isDoorLock(itemInHand)) {
            boolean anyDoorLocked = false;

            for (Block door : doubleDoors) {
                if (lockedDoors.containsKey(door.getLocation())) {
                    anyDoorLocked = true;
                    break;
                }
            }

            if (anyDoorLocked) {
                player.sendMessage(ChatColor.RED + "Uma das portas já está trancada!");
                return;
            }

            int cooldownRemaining = getDoorLockCooldownRemaining(player.getUniqueId());
            if (cooldownRemaining > 0) {
                player.sendMessage(ChatColor.RED + "Você precisa esperar " + cooldownRemaining +
                        " segundos para usar a tranca novamente!");
                return;
            }

            for (Block door : doubleDoors) {
                if (door.getType() == Material.IRON_DOOR) continue;

                Location doorLocation = door.getLocation();
                Material originalType = door.getType();
                lockedDoors.put(doorLocation, originalType);

                boolean wasOpen = isDoorOpen(door);
                safeTransformDoorToIron(door);

                BukkitTask task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (lockedDoors.containsKey(doorLocation)) {
                            Material originalDoorType = lockedDoors.get(doorLocation);
                            lockedDoors.remove(doorLocation);

                            Block currentDoor = doorLocation.getBlock();
                            boolean isOpen = isDoorOpen(currentDoor);
                            safeTransformDoorToType(currentDoor, originalDoorType, isOpen);

                            doorLocation.getWorld().playSound(doorLocation, Sound.BLOCK_WOODEN_DOOR_OPEN, 1.0f, 1.0f);
                            doorLocation.getWorld().spawnParticle(
                                    Particle.VILLAGER_HAPPY,
                                    doorLocation.clone().add(0.5, 0.5, 0.5),
                                    15, 0.5, 0.5, 0.5, 0
                            );
                        }
                        lockTimers.remove(doorLocation);
                    }
                }.runTaskLater(plugin, LOCK_DURATION * 20L);

                lockTimers.put(doorLocation, task);
            }

            player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 1.0f);
            player.spawnParticle(Particle.CRIT_MAGIC, bottomBlock.getLocation().add(0.5, 0.5, 0.5), 20, 0.5, 0.5, 0.5, 0);

            doorLockCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            player.sendMessage(ChatColor.GREEN + "Portas trancadas por " + LOCK_DURATION + " segundos!");

            event.setCancelled(true);
            return;
        }

        if (isLockpick(itemInHand)) {
            boolean anyDoorLocked = false;
            Location firstLockedDoorLocation = null;
            List<Location> lockedDoorLocations = new ArrayList<>();

            for (Block door : doubleDoors) {
                Location doorLocation = door.getLocation();
                if (lockedDoors.containsKey(doorLocation)) {
                    anyDoorLocked = true;
                    if (firstLockedDoorLocation == null) {
                        firstLockedDoorLocation = doorLocation;
                    }
                    lockedDoorLocations.add(doorLocation);
                }
            }

            if (!anyDoorLocked) return;

            int cooldownRemaining = getLockpickCooldownRemaining(player.getUniqueId());
            if (cooldownRemaining > 0) {
                player.sendMessage(ChatColor.RED + "Você precisa esperar " + cooldownRemaining +
                        " segundos para usar o lockpick novamente!");
                return;
            }

            if (unlockingPlayers.containsKey(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Você já está tentando abrir uma porta!");
                return;
            }

            unlockingPlayers.put(player.getUniqueId(), firstLockedDoorLocation);
            player.sendMessage(ChatColor.YELLOW + "Tentando desbloquear as portas... Não se mova ou tome dano por " + UNLOCK_TIME + " segundos!");

            player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 0.5f, 0.5f);

            player.setLevel(UNLOCK_TIME);
            player.setExp(0.0f);

            final List<Location> finalLockedDoorLocations = lockedDoorLocations;

            BukkitTask unlockTask = new BukkitRunnable() {
                int timeLeft = UNLOCK_TIME;

                @Override
                public void run() {
                    timeLeft--;
                    player.setLevel(timeLeft);
                    player.setExp(1.0f - ((float)timeLeft / UNLOCK_TIME));

                    for (Location doorLoc : finalLockedDoorLocations) {
                        player.spawnParticle(Particle.CRIT, doorLoc.clone().add(0.5, 1.0, 0.5), 5, 0.2, 0.2, 0.2, 0);
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 0.3f, 1.0f);

                    if (timeLeft <= 0) {
                        for (Location doorLoc : finalLockedDoorLocations) {
                            if (lockedDoors.containsKey(doorLoc)) {
                                Material originalDoorType = lockedDoors.get(doorLoc);
                                lockedDoors.remove(doorLoc);

                                Block currentDoor = doorLoc.getBlock();
                                boolean isOpen = isDoorOpen(currentDoor);
                                safeTransformDoorToType(currentDoor, originalDoorType, isOpen);

                                BukkitTask oldTask = lockTimers.remove(doorLoc);
                                if (oldTask != null) {
                                    oldTask.cancel();
                                }
                            }
                        }

                        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1.0f, 1.0f);
                        for (Location doorLoc : finalLockedDoorLocations) {
                            player.spawnParticle(Particle.VILLAGER_HAPPY, doorLoc.clone().add(0.5, 1.0, 0.5),
                                    15, 0.5, 0.5, 0.5, 0);
                        }

                        player.sendMessage(ChatColor.GREEN + "Portas desbloqueadas com sucesso!");

                        lockpickCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

                        unlockingPlayers.remove(player.getUniqueId());
                        unlockingTasks.remove(player.getUniqueId());
                        player.setExp(0.0f);
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 20L, 20L);

            unlockingTasks.put(player.getUniqueId(), unlockTask);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (unlockingPlayers.containsKey(playerId)) {
            Location from = event.getFrom();
            Location to = event.getTo();

            if (from.getBlockX() != to.getBlockX() ||
                    from.getBlockY() != to.getBlockY() ||
                    from.getBlockZ() != to.getBlockZ()) {

                cancelUnlocking(player, "Você se moveu! Desbloqueio cancelado.");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (unlockingPlayers.containsKey(player.getUniqueId())) {
            cancelUnlocking(player, "Você tomou dano! Desbloqueio cancelado.");
        }
    }

    private void cancelUnlocking(Player player, String message) {
        UUID playerId = player.getUniqueId();

        if (unlockingTasks.containsKey(playerId)) {
            BukkitTask task = unlockingTasks.get(playerId);
            task.cancel();
            unlockingTasks.remove(playerId);
        }

        unlockingPlayers.remove(playerId);
        player.sendMessage(ChatColor.RED + message);
        player.setExp(0.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
    }

    private Block getDoorBottomBlock(Block doorBlock) {
        BlockData blockData = doorBlock.getBlockData();

        if (blockData instanceof Door) {
            Door door = (Door) blockData;
            if (door.getHalf() == Bisected.Half.TOP) {
                return doorBlock.getRelative(0, -1, 0);
            }
        }

        return doorBlock;
    }

    private boolean isDoorOpen(Block doorBlock) {
        BlockData blockData = doorBlock.getBlockData();
        if (blockData instanceof Openable) {
            return ((Openable) blockData).isOpen();
        }
        return false;
    }

    private void setDoorOpen(Block doorBlock, boolean open) {
        BlockData blockData = doorBlock.getBlockData();
        if (blockData instanceof Openable) {
            Openable openable = (Openable) blockData;
            openable.setOpen(open);
            doorBlock.setBlockData(openable);

            if (blockData instanceof Door) {
                Block topDoor = doorBlock.getRelative(0, 1, 0);
                BlockData topData = topDoor.getBlockData();
                if (topData instanceof Door) {
                    ((Openable) topData).setOpen(open);
                    topDoor.setBlockData(topData);
                }
            }
        }
    }

    private void safeTransformDoorToIron(Block doorBlock) {
        boolean isOpen = isDoorOpen(doorBlock);
        Door oldDoorData = null;
        if (doorBlock.getBlockData() instanceof Door) {
            oldDoorData = (Door) doorBlock.getBlockData().clone();
        }

        Block topBlock = doorBlock.getRelative(0, 1, 0);
        Door oldTopDoorData = null;
        if (topBlock.getBlockData() instanceof Door) {
            oldTopDoorData = (Door) topBlock.getBlockData().clone();
        }

        World world = doorBlock.getWorld();
        Door finalOldDoorData = oldDoorData;
        Door finalOldTopDoorData = oldTopDoorData;
        new BukkitRunnable() {
            @Override
            public void run() {
                doorBlock.setType(Material.IRON_DOOR, false);

                if (finalOldDoorData != null) {
                    BlockData newData = doorBlock.getBlockData();
                    if (newData instanceof Door) {
                        Door newDoor = (Door) newData;
                        newDoor.setFacing(finalOldDoorData.getFacing());
                        newDoor.setHinge(finalOldDoorData.getHinge());
                        newDoor.setHalf(Bisected.Half.BOTTOM);
                        doorBlock.setBlockData(newDoor, false);
                    }
                }

                topBlock.setType(Material.IRON_DOOR, false);

                if (finalOldTopDoorData != null) {
                    BlockData newTopData = topBlock.getBlockData();
                    if (newTopData instanceof Door) {
                        Door newDoor = (Door) newTopData;
                        newDoor.setFacing(finalOldTopDoorData.getFacing());
                        newDoor.setHinge(finalOldTopDoorData.getHinge());
                        newDoor.setHalf(Bisected.Half.TOP);
                        topBlock.setBlockData(newDoor, false);
                    }
                }

                if (doorBlock.getBlockData() instanceof Openable) {
                    Openable openable = (Openable) doorBlock.getBlockData();
                    openable.setOpen(false);
                    doorBlock.setBlockData(openable, false);
                }

                if (topBlock.getBlockData() instanceof Openable) {
                    Openable openable = (Openable) topBlock.getBlockData();
                    openable.setOpen(false);
                    topBlock.setBlockData(openable, false);
                }
            }
        }.runTask(plugin);
    }

    private void safeTransformDoorToType(Block doorBlock, Material doorType, boolean shouldBeOpen) {
        Door oldDoorData = null;
        if (doorBlock.getBlockData() instanceof Door) {
            oldDoorData = (Door) doorBlock.getBlockData().clone();
        }

        Block topBlock = doorBlock.getRelative(0, 1, 0);
        Door oldTopDoorData = null;
        if (topBlock.getBlockData() instanceof Door) {
            oldTopDoorData = (Door) topBlock.getBlockData().clone();
        }

        World world = doorBlock.getWorld();
        Door finalOldDoorData = oldDoorData;
        Door finalOldTopDoorData = oldTopDoorData;
        new BukkitRunnable() {
            @Override
            public void run() {
                doorBlock.setType(doorType, false);

                if (finalOldDoorData != null) {
                    BlockData newData = doorBlock.getBlockData();
                    if (newData instanceof Door) {
                        Door newDoor = (Door) newData;
                        newDoor.setFacing(finalOldDoorData.getFacing());
                        newDoor.setHinge(finalOldDoorData.getHinge());
                        newDoor.setHalf(Bisected.Half.BOTTOM);
                        doorBlock.setBlockData(newDoor, false);
                    }
                }

                topBlock.setType(doorType, false);

                if (finalOldTopDoorData != null) {
                    BlockData newTopData = topBlock.getBlockData();
                    if (newTopData instanceof Door) {
                        Door newDoor = (Door) newTopData;
                        newDoor.setFacing(finalOldTopDoorData.getFacing());
                        newDoor.setHinge(finalOldTopDoorData.getHinge());
                        newDoor.setHalf(Bisected.Half.TOP);
                        topBlock.setBlockData(newDoor, false);
                    }
                }

                if (doorBlock.getBlockData() instanceof Openable) {
                    Openable openable = (Openable) doorBlock.getBlockData();
                    openable.setOpen(shouldBeOpen);
                    doorBlock.setBlockData(openable, false);
                }

                if (topBlock.getBlockData() instanceof Openable) {
                    Openable openable = (Openable) topBlock.getBlockData();
                    openable.setOpen(shouldBeOpen);
                    topBlock.setBlockData(openable, false);
                }
            }
        }.runTask(plugin);
    }

    public void cleanupAllData() {
        for (Map.Entry<Location, Material> entry : new HashMap<>(lockedDoors).entrySet()) {
            Location doorLoc = entry.getKey();
            Material originalType = entry.getValue();

            Block doorBlock = doorLoc.getBlock();
            boolean isOpen = isDoorOpen(doorBlock);

            safeTransformDoorToType(doorBlock, originalType, isOpen);

            BukkitTask task = lockTimers.remove(doorLoc);
            if (task != null) {
                task.cancel();
            }
        }

        for (BukkitTask task : unlockingTasks.values()) {
            task.cancel();
        }

        lockedDoors.clear();
        lockTimers.clear();
        unlockingPlayers.clear();
        unlockingTasks.clear();
        doorLockCooldowns.clear();
        lockpickCooldowns.clear();
    }
}