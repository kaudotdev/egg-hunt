package org.example.kau.eggHunt;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EggManager {
    private final EggHunt plugin;
    private final Logger logger;
    private final FileConfiguration config;
    private final List<String> eggTextures = Arrays.asList(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDdjZDY5NjBlMTYwYzZlYzQ4OTZmNzc0ZmYyMzY0Y2IwN2YxMTk5ZjFmOWJlMzQ4MTY5MjFjYzE3NzViNWU4MiJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGYzZjRkM2EyZjQ4NDY2OTE4ZmEwNWNkNjBjOTgzMGNjMThmYzc0MzkwY2ZhZWFhZDFhMTE3Nzg2YWJhOTBjZCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjE1NGJhNDJkMjgyMmY1NjUzNGU3NTU4Mjc4OTJlNDcxOWRlZWYzMjhhYmI1OTU4NGJlNjk2N2YyNWY0OGNiNCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWY1NWM1MDVkN2YwMDEzNWI1ZjUyMjViNzVjZDkyZWQxMjIwMWNjOTVjNDFkZWVkOGE3N2RhOGZkNmI3Yjk2MyJ9fX0="
    );
    private final Map<Location, String> eggLocations = new HashMap<>();

    public EggManager(EggHunt plugin) {
        this.plugin = plugin;
        this.logger = plugin.getPluginLogger();
        this.config = plugin.getPluginConfig();
    }

    public void spawnInitialEggs(Location corner1, Location corner2) {
        int count = config.getInt("egg-count", 10);
        int eggsSpawned = 0;
        int attempts = 0;
        final int MAX_ATTEMPTS = 200;

        while (eggsSpawned < count && attempts < MAX_ATTEMPTS) {
            if (spawnNewEgg(corner1, corner2)) {
                eggsSpawned++;
            }
            attempts++;
        }
    }

    public boolean spawnNewEgg(Location corner1, Location corner2) {
        Location loc = getRandomLocation(corner1, corner2);

        if (loc != null) {
            Block block = loc.getBlock();
            block.setType(Material.PLAYER_HEAD);

            Random random = new Random();
            String texture = eggTextures.get(random.nextInt(eggTextures.size()));

            Location exactLoc = new Location(
                    loc.getWorld(),
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ()
            );
            eggLocations.put(exactLoc, texture);

            Skull skull = (Skull) block.getState();
            skull.setRotation(BlockFace.NORTH);

            try {
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "EggHunt");
                profile.setProperty(new ProfileProperty("textures", texture));
                skull.setPlayerProfile(profile);
                skull.update(true, false);
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao aplicar textura ao ovo: " + e.getMessage(), e);
                block.setType(Material.AIR);
                return false;
            }
        }
        return false;
    }

    public boolean isEggCollectionEvent(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return false;
        if (event.getClickedBlock() == null) return false;

        Block block = event.getClickedBlock();
        return block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD;
    }

    public boolean processEggCollection(Player player, Block block) {
        Location blockLoc = block.getLocation();

        for (Iterator<Map.Entry<Location, String>> it = eggLocations.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Location, String> entry = it.next();
            Location eggLoc = entry.getKey();

            if (eggLoc.getWorld().equals(blockLoc.getWorld()) &&
                    eggLoc.getBlockX() == blockLoc.getBlockX() &&
                    eggLoc.getBlockY() == blockLoc.getBlockY() &&
                    eggLoc.getBlockZ() == blockLoc.getBlockZ()) {

                block.setType(Material.AIR);
                it.remove();
                return true;
            }
        }

        return false;
    }

    private boolean isValidSurface(Block block) {
        if (!block.getType().isSolid() || block.isLiquid()) {
            return false;
        }

        BlockData blockData = block.getBlockData();

        if (blockData instanceof Door ||
                blockData instanceof Slab ||
                blockData instanceof Switch ||
                blockData instanceof TrapDoor ||
                block.getType().toString().contains("PRESSURE_PLATE") ||
                block.getType().toString().contains("BUTTON") ||
                block.getType().toString().contains("FENCE") ||
                block.getType().toString().contains("WALL")) {
            return false;
        }

        return true;
    }

    private Location getRandomLocation(Location corner1, Location corner2) {
        Random random = new Random();

        int minX = Math.min((int)corner1.getX(), (int)corner2.getX());
        int maxX = Math.max((int)corner1.getX(), (int)corner2.getX());
        int minY = Math.min((int)corner1.getY(), (int)corner2.getY());
        int maxY = Math.max((int)corner1.getY(), (int)corner2.getY());
        int minZ = Math.min((int)corner1.getZ(), (int)corner2.getZ());
        int maxZ = Math.max((int)corner1.getZ(), (int)corner2.getZ());

        World world = corner1.getWorld();
        int y = minY + random.nextInt(maxY - minY + 1);

        for (int attempt = 0; attempt < 15; attempt++) {
            int x = minX + random.nextInt(maxX - minX + 1);
            int z = minZ + random.nextInt(maxZ - minZ + 1);

            for (int currentY = minY; currentY <= maxY; currentY++) {
                Block block = world.getBlockAt(x, currentY, z);
                Block blockAbove = world.getBlockAt(x, currentY + 1, z);

                if (isValidSurface(block) && blockAbove.getType().isAir()) {
                    return new Location(world, x, currentY + 1, z);
                }
            }
        }

        return null;
    }

    public void clearEggs() {
        for (Location loc : eggLocations.keySet()) {
            Block block = loc.getBlock();
            if (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {
                block.setType(Material.AIR);
            }
        }
        eggLocations.clear();
    }
}