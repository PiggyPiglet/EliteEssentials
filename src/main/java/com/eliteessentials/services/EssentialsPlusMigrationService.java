package com.eliteessentials.services;

import com.eliteessentials.model.*;
import com.eliteessentials.storage.PlayerFileStorage;
import com.eliteessentials.storage.WarpStorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * Migrates data from fof1092's EssentialsPlus plugin to EliteEssentials.
 * 
 * Source: mods/fof1092_EssentialsPlus/
 * - kits/KITNAME.json (each kit is a separate file)
 * - homes.json (array of all player homes)
 * - warps.json (array of all warps)
 */
public class EssentialsPlusMigrationService {
    
    private static final Logger logger = Logger.getLogger("EliteEssentials");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private final File modsFolder;
    private final WarpStorage warpStorage;
    private final KitService kitService;
    private final PlayerFileStorage playerFileStorage;
    
    // Migration stats
    private int warpsImported = 0;
    private int kitsImported = 0;
    private int playersImported = 0;
    private int homesImported = 0;
    private final List<String> errors = new ArrayList<>();
    
    public EssentialsPlusMigrationService(File dataFolder, WarpStorage warpStorage, 
                                          KitService kitService, PlayerFileStorage playerFileStorage) {
        // Go up from EliteEssentials folder to mods folder
        this.modsFolder = dataFolder.getParentFile();
        this.warpStorage = warpStorage;
        this.kitService = kitService;
        this.playerFileStorage = playerFileStorage;
    }
    
    /**
     * Check if EssentialsPlus data exists.
     */
    public boolean hasEssentialsPlusData() {
        File essentialsFolder = new File(modsFolder, "fof1092_EssentialsPlus");
        return essentialsFolder.exists() && essentialsFolder.isDirectory();
    }
    
    /**
     * Get the EssentialsPlus folder path.
     */
    public File getEssentialsPlusFolder() {
        return new File(modsFolder, "fof1092_EssentialsPlus");
    }

    
    /**
     * Run the full migration.
     * @return MigrationResult with stats and any errors
     */
    public MigrationResult migrate() {
        // Reset stats
        warpsImported = 0;
        kitsImported = 0;
        playersImported = 0;
        homesImported = 0;
        errors.clear();
        
        File essentialsFolder = getEssentialsPlusFolder();
        
        if (!essentialsFolder.exists()) {
            errors.add("EssentialsPlus folder not found at: " + essentialsFolder.getAbsolutePath());
            return new MigrationResult(false, warpsImported, kitsImported, playersImported, homesImported, errors);
        }
        
        logger.info("[Migration] ========================================");
        logger.info("[Migration] Starting EssentialsPlus migration...");
        logger.info("[Migration] Source: " + essentialsFolder.getAbsolutePath());
        logger.info("[Migration] ========================================");
        
        // Migrate warps
        migrateWarps(essentialsFolder);
        
        // Migrate kits
        migrateKits(essentialsFolder);
        
        // Migrate homes
        migrateHomes(essentialsFolder);
        
        logger.info("[Migration] ========================================");
        logger.info("[Migration] EssentialsPlus migration complete!");
        logger.info("[Migration] - Warps: " + warpsImported);
        logger.info("[Migration] - Kits: " + kitsImported);
        logger.info("[Migration] - Players: " + playersImported);
        logger.info("[Migration] - Homes: " + homesImported);
        if (!errors.isEmpty()) {
            logger.info("[Migration] - Errors: " + errors.size());
        }
        logger.info("[Migration] ========================================");
        
        return new MigrationResult(errors.isEmpty(), warpsImported, kitsImported, playersImported, homesImported, errors);
    }
    
    /**
     * Migrate warps from EssentialsPlus warps.json.
     * Format: { "version": "1.0", "warps": [ { "name": "...", "position": {...}, "rotation": {...}, "world": "..." } ] }
     */
    private void migrateWarps(File essentialsFolder) {
        File warpsFile = new File(essentialsFolder, "warps.json");
        if (!warpsFile.exists()) {
            logger.info("[Migration] No warps.json found, skipping warp migration.");
            return;
        }
        
        logger.info("[Migration] Migrating EssentialsPlus warps.json...");
        
        try (Reader reader = new InputStreamReader(new FileInputStream(warpsFile), StandardCharsets.UTF_8)) {
            EssentialsPlusWarpsFile warpsData = gson.fromJson(reader, EssentialsPlusWarpsFile.class);
            
            if (warpsData == null || warpsData.warps == null || warpsData.warps.isEmpty()) {
                logger.info("[Migration] - No warps found in file.");
                return;
            }
            
            for (EssentialsPlusWarp epWarp : warpsData.warps) {
                String warpName = epWarp.name;
                
                // Check if warp already exists
                if (warpStorage.hasWarp(warpName)) {
                    logger.info("[Migration] - Skipping warp '" + warpName + "' (already exists)");
                    continue;
                }
                
                // Convert to our format - use world UUID as world name
                Location location = new Location(
                    epWarp.world,
                    epWarp.position.x,
                    epWarp.position.y,
                    epWarp.position.z,
                    epWarp.rotation.y, // yaw
                    0f // pitch - set to 0 to avoid player tilt
                );
                
                Warp warp = new Warp(warpName, location, Warp.Permission.ALL, "EssentialsPlus Migration");
                warpStorage.setWarp(warp);
                warpsImported++;
                logger.info("[Migration] - Imported warp: " + warpName);
            }
            
        } catch (Exception e) {
            String error = "Failed to migrate EssentialsPlus warps: " + e.getMessage();
            logger.severe("[Migration] " + error);
            errors.add(error);
        }
    }

    
    /**
     * Migrate kits from EssentialsPlus kits/ folder.
     * Each kit is a separate JSON file named after the kit.
     * Format: { "version": "...", "name": "...", "cooldown": ms, "storage": {...}, "hotbar": {...}, "armor": {...}, ... }
     */
    private void migrateKits(File essentialsFolder) {
        File kitsFolder = new File(essentialsFolder, "kits");
        if (!kitsFolder.exists() || !kitsFolder.isDirectory()) {
            logger.info("[Migration] No kits folder found, skipping kit migration.");
            return;
        }
        
        logger.info("[Migration] Migrating EssentialsPlus kits...");
        
        File[] kitFiles = kitsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (kitFiles == null || kitFiles.length == 0) {
            logger.info("[Migration] - No kit files found.");
            return;
        }
        
        for (File kitFile : kitFiles) {
            try {
                migrateKitFile(kitFile);
            } catch (Exception e) {
                String error = "Failed to migrate kit " + kitFile.getName() + ": " + e.getMessage();
                logger.warning("[Migration] " + error);
                errors.add(error);
            }
        }
    }
    
    private void migrateKitFile(File kitFile) throws Exception {
        try (Reader reader = new InputStreamReader(new FileInputStream(kitFile), StandardCharsets.UTF_8)) {
            EssentialsPlusKit epKit = gson.fromJson(reader, EssentialsPlusKit.class);
            
            if (epKit == null || epKit.name == null) {
                logger.warning("[Migration] - Invalid kit file: " + kitFile.getName());
                return;
            }
            
            String kitId = epKit.name.toLowerCase();
            
            // Check if kit already exists
            if (kitService.getKit(kitId) != null) {
                logger.info("[Migration] - Skipping kit '" + kitId + "' (already exists)");
                return;
            }
            
            // Convert items from all sections
            List<KitItem> items = new ArrayList<>();
            
            // Process hotbar items
            if (epKit.hotbar != null && epKit.hotbar.items != null) {
                for (Map.Entry<String, EssentialsPlusItem> entry : epKit.hotbar.items.entrySet()) {
                    int slot = parseIntSafe(entry.getKey(), 0);
                    EssentialsPlusItem epItem = entry.getValue();
                    items.add(new KitItem(epItem.itemId, epItem.quantity, "hotbar", slot));
                }
            }
            
            // Process storage items
            if (epKit.storage != null && epKit.storage.items != null) {
                for (Map.Entry<String, EssentialsPlusItem> entry : epKit.storage.items.entrySet()) {
                    int slot = parseIntSafe(entry.getKey(), 0);
                    EssentialsPlusItem epItem = entry.getValue();
                    items.add(new KitItem(epItem.itemId, epItem.quantity, "storage", slot));
                }
            }
            
            // Process armor items
            if (epKit.armor != null && epKit.armor.items != null) {
                for (Map.Entry<String, EssentialsPlusItem> entry : epKit.armor.items.entrySet()) {
                    int slot = parseIntSafe(entry.getKey(), 0);
                    EssentialsPlusItem epItem = entry.getValue();
                    items.add(new KitItem(epItem.itemId, epItem.quantity, "armor", slot));
                }
            }
            
            // Process utility items
            if (epKit.utility != null && epKit.utility.items != null) {
                for (Map.Entry<String, EssentialsPlusItem> entry : epKit.utility.items.entrySet()) {
                    int slot = parseIntSafe(entry.getKey(), 0);
                    EssentialsPlusItem epItem = entry.getValue();
                    items.add(new KitItem(epItem.itemId, epItem.quantity, "utility", slot));
                }
            }
            
            // Convert cooldown from milliseconds to seconds
            int cooldownSeconds = (int) (epKit.cooldown / 1000);
            
            // Create kit
            Kit kit = new Kit(
                kitId,
                epKit.name, // displayName
                "Imported from EssentialsPlus",
                null, // icon
                cooldownSeconds,
                false, // replaceInventory - EssentialsPlus adds items
                false, // onetime
                kitId.equalsIgnoreCase("starter"), // starterKit if named "starter"
                items
            );
            
            kitService.saveKit(kit);
            kitsImported++;
            logger.info("[Migration] - Imported kit: " + kitId + " (" + items.size() + " items, " + cooldownSeconds + "s cooldown)");
        }
    }
    
    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    
    /**
     * Migrate homes from EssentialsPlus homes.json.
     * Format: { "version": "1.0", "homes": [ { "uuid": "...", "name": "...", "position": {...}, "rotation": {...}, "world": "..." } ] }
     */
    private void migrateHomes(File essentialsFolder) {
        File homesFile = new File(essentialsFolder, "homes.json");
        if (!homesFile.exists()) {
            logger.info("[Migration] No homes.json found, skipping home migration.");
            return;
        }
        
        logger.info("[Migration] Migrating EssentialsPlus homes.json...");
        
        try (Reader reader = new InputStreamReader(new FileInputStream(homesFile), StandardCharsets.UTF_8)) {
            EssentialsPlusHomesFile homesData = gson.fromJson(reader, EssentialsPlusHomesFile.class);
            
            if (homesData == null || homesData.homes == null || homesData.homes.isEmpty()) {
                logger.info("[Migration] - No homes found in file.");
                return;
            }
            
            // Group homes by player UUID
            Map<UUID, List<EssentialsPlusHome>> homesByPlayer = new HashMap<>();
            for (EssentialsPlusHome epHome : homesData.homes) {
                try {
                    UUID uuid = UUID.fromString(epHome.uuid);
                    homesByPlayer.computeIfAbsent(uuid, k -> new ArrayList<>()).add(epHome);
                } catch (IllegalArgumentException e) {
                    logger.warning("[Migration] - Skipping home with invalid UUID: " + epHome.uuid);
                }
            }
            
            // Process each player's homes
            for (Map.Entry<UUID, List<EssentialsPlusHome>> entry : homesByPlayer.entrySet()) {
                UUID uuid = entry.getKey();
                List<EssentialsPlusHome> playerHomes = entry.getValue();
                
                // Get or create our player file
                PlayerFile ourPlayer = playerFileStorage.getPlayer(uuid);
                if (ourPlayer == null) {
                    // Create new player with unknown name (will update when they join)
                    ourPlayer = playerFileStorage.getPlayer(uuid, "Unknown");
                }
                
                int homesForPlayer = 0;
                for (EssentialsPlusHome epHome : playerHomes) {
                    String homeName = epHome.name;
                    
                    // Skip if home already exists
                    if (ourPlayer.hasHome(homeName)) {
                        logger.info("[Migration] - Skipping home '" + homeName + "' for " + uuid + " (already exists)");
                        continue;
                    }
                    
                    // Convert to our format - use world UUID as world name
                    Location location = new Location(
                        epHome.world,
                        epHome.position.x,
                        epHome.position.y,
                        epHome.position.z,
                        epHome.rotation.y, // yaw
                        0f // pitch - set to 0 to avoid player tilt
                    );
                    
                    Home home = new Home(homeName, location);
                    ourPlayer.setHome(home);
                    homesForPlayer++;
                    homesImported++;
                }
                
                if (homesForPlayer > 0) {
                    playerFileStorage.saveAndMarkDirty(uuid);
                    playersImported++;
                    logger.info("[Migration] - Imported " + homesForPlayer + " home(s) for player " + uuid);
                }
            }
            
        } catch (Exception e) {
            String error = "Failed to migrate EssentialsPlus homes: " + e.getMessage();
            logger.severe("[Migration] " + error);
            errors.add(error);
        }
        
        logger.info("[Migration] - Migrated " + homesImported + " homes for " + playersImported + " players");
    }

    
    // ==================== Inner Classes for EssentialsPlus Format ====================
    
    /**
     * EssentialsPlus position format (x, y, z).
     */
    private static class EssentialsPlusPosition {
        double x;
        double y;
        double z;
    }
    
    /**
     * EssentialsPlus rotation format (x=pitch, y=yaw, z=roll).
     */
    private static class EssentialsPlusRotation {
        float x; // pitch
        float y; // yaw
        float z; // roll
    }
    
    /**
     * EssentialsPlus warps.json file format.
     */
    private static class EssentialsPlusWarpsFile {
        String version;
        List<EssentialsPlusWarp> warps;
    }
    
    /**
     * EssentialsPlus warp entry.
     */
    private static class EssentialsPlusWarp {
        String name;
        EssentialsPlusPosition position;
        EssentialsPlusRotation rotation;
        String world;
    }
    
    /**
     * EssentialsPlus homes.json file format.
     */
    private static class EssentialsPlusHomesFile {
        String version;
        List<EssentialsPlusHome> homes;
    }
    
    /**
     * EssentialsPlus home entry.
     */
    private static class EssentialsPlusHome {
        String uuid;
        String name;
        EssentialsPlusPosition position;
        EssentialsPlusRotation rotation;
        String world;
    }
    
    /**
     * EssentialsPlus kit format.
     */
    private static class EssentialsPlusKit {
        String version;
        String name;
        long cooldown; // in milliseconds
        EssentialsPlusItemContainer storage;
        EssentialsPlusItemContainer armor;
        EssentialsPlusItemContainer hotbar;
        EssentialsPlusItemContainer utility;
        Map<String, Long> lastClaimed;
    }
    
    /**
     * EssentialsPlus item container (storage, hotbar, armor, utility).
     */
    private static class EssentialsPlusItemContainer {
        int capacity;
        Map<String, EssentialsPlusItem> items;
    }
    
    /**
     * EssentialsPlus item format.
     */
    private static class EssentialsPlusItem {
        String itemId;
        int quantity;
        double durability;
        double maxDurability;
        boolean overrideDroppedItemAnimation;
        // cachedPacket is ignored - it's just a duplicate
    }
    
    // ==================== Migration Result ====================
    
    /**
     * Result of a migration operation.
     */
    public static class MigrationResult {
        private final boolean success;
        private final int warpsImported;
        private final int kitsImported;
        private final int playersImported;
        private final int homesImported;
        private final List<String> errors;
        
        public MigrationResult(boolean success, int warpsImported, int kitsImported, 
                              int playersImported, int homesImported, List<String> errors) {
            this.success = success;
            this.warpsImported = warpsImported;
            this.kitsImported = kitsImported;
            this.playersImported = playersImported;
            this.homesImported = homesImported;
            this.errors = new ArrayList<>(errors);
        }
        
        public boolean isSuccess() { return success; }
        public int getWarpsImported() { return warpsImported; }
        public int getKitsImported() { return kitsImported; }
        public int getPlayersImported() { return playersImported; }
        public int getHomesImported() { return homesImported; }
        public List<String> getErrors() { return errors; }
        
        public int getTotalImported() {
            return warpsImported + kitsImported + homesImported;
        }
    }
}
