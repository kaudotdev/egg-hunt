package org.example.kau.eggHunt;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EventGUI implements CommandExecutor, Listener {
    private final EggHunt plugin;
    private final Map<UUID, EventSettings> playerSettings = new HashMap<>();
    private static final String GUI_TITLE = ChatColor.GOLD + "Configuração do Evento";
    private static final String BLACKLIST_GUI_TITLE = ChatColor.RED + "Lista de Exclusão";
    
    private static final int TIME_SLOT = 11;
    private static final int EGG_COUNT_SLOT = 13;
    private static final int BLACKLIST_SLOT = 15;
    private static final int START_SLOT = 31;
    
    public EventGUI(EggHunt plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public class EventSettings {
        private int eventTime;
        private int eggCount;
        private Set<UUID> blacklistedPlayers;
        
        public EventSettings() {
            this.eventTime = plugin.getPluginConfig().getInt("egg-hunt-time", 15);
            this.eggCount = plugin.getPluginConfig().getInt("egg-count", 10);
            this.blacklistedPlayers = new HashSet<>();
        }
        
        public int getEventTime() {
            return eventTime;
        }
        
        public void setEventTime(int eventTime) {
            this.eventTime = eventTime;
        }
        
        public int getEggCount() {
            return eggCount;
        }
        
        public void setEggCount(int eggCount) {
            this.eggCount = eggCount;
        }
        
        public Set<UUID> getBlacklistedPlayers() {
            return blacklistedPlayers;
        }
        
        public void addBlacklistedPlayer(UUID playerId) {
            blacklistedPlayers.add(playerId);
        }
        
        public void removeBlacklistedPlayer(UUID playerId) {
            blacklistedPlayers.remove(playerId);
        }
        
        public boolean isBlacklisted(UUID playerId) {
            return blacklistedPlayers.contains(playerId);
        }
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
        openMainGUI(player);
        return true;
    }
    
    private void openMainGUI(Player player) {
        if (!playerSettings.containsKey(player.getUniqueId())) {
            playerSettings.put(player.getUniqueId(), new EventSettings());
        }
        
        EventSettings settings = playerSettings.get(player.getUniqueId());
        Inventory gui = Bukkit.createInventory(null, 36, GUI_TITLE);
        
        ItemStack timeItem = createItem(Material.CLOCK, 
                ChatColor.YELLOW + "Tempo do Evento", 
                Arrays.asList(
                        ChatColor.WHITE + "Clique para ajustar o tempo",
                        ChatColor.GREEN + "Tempo atual: " + settings.getEventTime() + " minutos",
                        "",
                        ChatColor.GRAY + "Clique com botão esquerdo: +1 minuto",
                        ChatColor.GRAY + "Clique com botão direito: -1 minuto",
                        ChatColor.GRAY + "Shift + Esquerdo: +5 minutos",
                        ChatColor.GRAY + "Shift + Direito: -5 minutos"
                ));
        
        ItemStack eggItem = createItem(Material.DRAGON_EGG,
                ChatColor.LIGHT_PURPLE + "Quantidade de Ovos",
                Arrays.asList(
                        ChatColor.WHITE + "Clique para ajustar a quantidade de ovos",
                        ChatColor.GREEN + "Total atual: " + settings.getEggCount() + " ovos",
                        "",
                        ChatColor.GRAY + "Clique com botão esquerdo: +5 ovos",
                        ChatColor.GRAY + "Clique com botão direito: -5 ovos",
                        ChatColor.GRAY + "Shift + Esquerdo: +10 ovos",
                        ChatColor.GRAY + "Shift + Direito: -10 ovos"
                ));
        
        ItemStack blacklistItem = createItem(Material.BARRIER,
                ChatColor.RED + "Lista de Exclusão",
                Arrays.asList(
                        ChatColor.WHITE + "Clique para gerenciar jogadores excluídos",
                        ChatColor.YELLOW + "Jogadores excluídos: " + settings.getBlacklistedPlayers().size(),
                        "",
                        ChatColor.GRAY + "Estes jogadores não participarão do evento"
                ));
        
        ItemStack startItem = createItem(Material.EMERALD_BLOCK,
                ChatColor.GREEN + "Iniciar Evento",
                Arrays.asList(
                        ChatColor.WHITE + "Clique para iniciar o evento",
                        ChatColor.YELLOW + "com as configurações definidas"
                ));
        
        ItemStack borderItem = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", Collections.emptyList());
        for (int i = 0; i < gui.getSize(); i++) {
            if (i < 9 || i >= gui.getSize() - 9 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, borderItem);
            }
        }
        
        gui.setItem(TIME_SLOT, timeItem);
        gui.setItem(EGG_COUNT_SLOT, eggItem);
        gui.setItem(BLACKLIST_SLOT, blacklistItem);
        gui.setItem(START_SLOT, startItem);
        
        player.openInventory(gui);
    }
    

    private void openBlacklistGUI(Player player) {
        EventSettings settings = playerSettings.get(player.getUniqueId());
        Inventory blacklistGUI = Bukkit.createInventory(null, 54, BLACKLIST_GUI_TITLE);
        
        ItemStack backItem = createItem(Material.ARROW, 
                ChatColor.YELLOW + "Voltar", 
                Arrays.asList(ChatColor.GRAY + "Voltar para o menu principal"));
        blacklistGUI.setItem(45, backItem);
        

        int slot = 0;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.isOp()) continue; 
            
            if (slot >= 45) break; 
            
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(onlinePlayer);
            
            String playerName = onlinePlayer.getName();
            boolean isBlacklisted = settings.isBlacklisted(onlinePlayer.getUniqueId());
            
            if (isBlacklisted) {
                meta.setDisplayName(ChatColor.RED + playerName + " " + ChatColor.GRAY + "(Excluído)");
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Este jogador está excluído do evento",
                        ChatColor.YELLOW + "Clique para incluir no evento"
                ));
            } else {
                meta.setDisplayName(ChatColor.GREEN + playerName);
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Este jogador participará do evento",
                        ChatColor.YELLOW + "Clique para excluir do evento"
                ));
            }
            
            head.setItemMeta(meta);
            blacklistGUI.setItem(slot, head);
            slot++;
        }
        
        player.openInventory(blacklistGUI);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (!title.equals(GUI_TITLE) && !title.equals(BLACKLIST_GUI_TITLE)) return;
        
        event.setCancelled(true);
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        if (title.equals(GUI_TITLE)) {
            processMainGUIClick(player, event.getSlot(), event.isRightClick(), event.isShiftClick());
        }
        else if (title.equals(BLACKLIST_GUI_TITLE)) {
            processBlacklistGUIClick(player, event.getSlot(), clickedItem);
        }
    }
    
    private void processMainGUIClick(Player player, int slot, boolean isRightClick, boolean isShiftClick) {
        EventSettings settings = playerSettings.get(player.getUniqueId());
        
        if (slot == TIME_SLOT) {
            int timeChange;
            if (isShiftClick) {
                timeChange = isRightClick ? -5 : 5;
            } else {
                timeChange = isRightClick ? -1 : 1;
            }
            
            int newTime = Math.max(1, settings.getEventTime() + timeChange);
            settings.setEventTime(newTime);
            
            openMainGUI(player);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        else if (slot == EGG_COUNT_SLOT) {
            int eggChange;
            if (isShiftClick) {
                eggChange = isRightClick ? -10 : 10;
            } else {
                eggChange = isRightClick ? -5 : 5;
            }
            
            int newCount = Math.max(1, settings.getEggCount() + eggChange);
            settings.setEggCount(newCount);
            
            openMainGUI(player);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        else if (slot == BLACKLIST_SLOT) {
            openBlacklistGUI(player);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
        }
        else if (slot == START_SLOT) {
            startEvent(player, settings);
            player.closeInventory();
        }
    }
    
    private void processBlacklistGUIClick(Player player, int slot, ItemStack clickedItem) {
        EventSettings settings = playerSettings.get(player.getUniqueId());
        
        if (slot == 45 && clickedItem.getType() == Material.ARROW) {
            openMainGUI(player);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        
       if (clickedItem.getType() == Material.PLAYER_HEAD && clickedItem.hasItemMeta()) {
            SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
            if (meta.getOwningPlayer() != null) {
                UUID clickedPlayerId = meta.getOwningPlayer().getUniqueId();
                
                if (settings.isBlacklisted(clickedPlayerId)) {
                    settings.removeBlacklistedPlayer(clickedPlayerId);
                } else {
                    settings.addBlacklistedPlayer(clickedPlayerId);
                }
                
                openBlacklistGUI(player);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            }
        }
    }
    
    private void startEvent(Player player, EventSettings settings) {
        plugin.getPluginConfig().set("egg-hunt-time", settings.getEventTime());
        plugin.getPluginConfig().set("egg-count", settings.getEggCount());
        plugin.saveConfig();
        
        player.sendMessage(ChatColor.GOLD + "[Caça aos Ovos] Iniciando evento com:");
        player.sendMessage(ChatColor.YELLOW + "- Tempo: " + settings.getEventTime() + " minutos");
        player.sendMessage(ChatColor.YELLOW + "- Ovos: " + settings.getEggCount());
        
        if (!settings.getBlacklistedPlayers().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "- " + settings.getBlacklistedPlayers().size() + " jogadores excluídos");
        }
        
        plugin.setBlacklistedPlayers(settings.getBlacklistedPlayers());
        
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.startEvent();
            }
        }.runTaskLater(plugin, 20L);
    }
    
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        
        if (title.equals(BLACKLIST_GUI_TITLE)) {
            return;
        }
        
        if (title.equals(GUI_TITLE)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.getOpenInventory().getTitle().equals(BLACKLIST_GUI_TITLE)) {
                        playerSettings.remove(player.getUniqueId());
                    }
                }
            }.runTaskLater(plugin, 2L);
        }
    }
}
