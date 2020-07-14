package net.hypercubemc.redduels.config;

import net.hypercubemc.redduels.object.DuelType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/*
Singleton class that loads the config for RedDuels
 */
public class RedDuelsConfig {

    private static RedDuelsConfig instance = null;
    private Logger logger;

    // a list of all duel types
    public ArrayList<DuelType> duelTypes = new ArrayList<DuelType>();

    // the tag that is shown next to RedDuels-related messages
    private final String chatTag;
    private final ChatColor chatTagTextColor;
    private final ChatColor chatTagBracketColor;
    private final boolean chatTagEnabled;

    // error messages
    private final ChatColor errorColor;
    private final boolean errorBold;

    // received offers
    private final ChatColor duelOfferReceivedColor;
    private final boolean duelOfferReceivedBold;

    // sent offers
    private final ChatColor duelOfferSentColor;
    private final boolean duelOfferSentBold;

    // info messages
    private final ChatColor infoColor;
    private final boolean infoBold;
    private final boolean infoEnabled;

    // duel resolution messages
    private final ChatColor duelResolutionColor;
    private final boolean duelResolutionBold;
    private final boolean duelResolutionEnabled;

    private RedDuelsConfig(Plugin plugin) {
        // grab a logger for debugging
        logger = Bukkit.getLogger();
        logger.info("[RedDuels] Loading configuration");
        // get the config
        FileConfiguration config = plugin.getConfig();
        // load duel types
        loadDuelTypes(config);
        // load variables we need
        chatTag = config.getString("ChatTag.Text");
        chatTagTextColor = stringToChatColor(config.getString("ChatTag.TextColor"));
        chatTagBracketColor = stringToChatColor(config.getString("ChatTag.BracketColor"));
        chatTagEnabled = config.getBoolean("ChatTag.Enabled");

        errorColor = stringToChatColor(config.getString("ErrorMessage.Color"));
        errorBold = config.getBoolean("ErrorMessage.Bold");

        duelOfferReceivedColor = stringToChatColor(config.getString("DuelOfferReceivedMessage.Color"));
        duelOfferReceivedBold = config.getBoolean("DuelOfferReceivedMessage.Bold");

        duelOfferSentColor = stringToChatColor(config.getString("DuelOfferSentMessage.Color"));
        duelOfferSentBold = config.getBoolean("DuelOfferSentMessage.Bold");

        infoColor = stringToChatColor(config.getString("InfoMessage.Color"));
        infoBold = config.getBoolean("InfoMessage.Bold");
        infoEnabled = config.getBoolean("InfoMessage.Enabled");

        duelResolutionColor = stringToChatColor(config.getString("DuelResolutionMessage.Color"));
        duelResolutionBold = config.getBoolean("DuelResolutionMessage.Bold");
        duelResolutionEnabled = config.getBoolean("DuelResolutionMessage.Enabled");
    }

    /*
    Load custom, user-defined duel types from the config file
     */
    private void loadDuelTypes(FileConfiguration config) {
        // first we get the DuelTypes section, which contains all duel type definitions
        ConfigurationSection duelTypesSection = config.getConfigurationSection("DuelTypes");
        for (String key : duelTypesSection.getKeys(false)) {
            logger.info("[RedDuels] Found DuelType with key: " + key);
            // attempt to register a new DuelType
            DuelType duelType = new DuelType();
            duelType.name = key;
            // get the DuelType's arena world
            duelType.worldName = config.getString("DuelTypes." + key + ".World");
            logger.info("[RedDuels] Assigned world " + duelType.worldName + " to DuelType " + duelType.name);
            // if a permission is required to use this duel type, register it
            if (config.getBoolean("DuelTypes." + key + ".RequirePermission")) {
                Permission p = new Permission("redduels.dueltype." + duelType.name.toLowerCase());
                p.addParent("redduels.dueltype.*", true);
                Bukkit.getPluginManager().addPermission(p);
                duelType.permission = p;
                logger.info("[RedDuels] Registered new permission " + p.getName());
            }
            // get the description
            duelType.description = config.getString("DuelTypes." + key + ".Description");
            // get the DuelType's player spawn locations
            int player1X = config.getInt("DuelTypes." + key + ".Player1Spawn.X");
            int player1Y = config.getInt("DuelTypes." + key + ".Player1Spawn.Y");
            int player1Z = config.getInt("DuelTypes." + key + ".Player1Spawn.Z");
            duelType.player1SpawnLocation = new int[]{
                    player1X,
                    player1Y,
                    player1Z
            };
            int player2X = config.getInt("DuelTypes." + key + ".Player2Spawn.X");
            int player2Y = config.getInt("DuelTypes." + key + ".Player2Spawn.Y");
            int player2Z = config.getInt("DuelTypes." + key + ".Player2Spawn.Z");
            duelType.player2SpawnLocation = new int[]{
                    player2X,
                    player2Y,
                    player2Z
            };
            logger.info("[RedDuels] Successfully assigned spawn locations for DuelType " + duelType.name);
            // iterate through items
            ConfigurationSection items = config.getConfigurationSection("DuelTypes." + key + ".Items");
            duelType.items = parseItems(items);
            ConfigurationSection armor = config.getConfigurationSection("DuelTypes." + key + ".Armor");
            duelType.armor = parseArmor(armor);
            logger.info("[RedDuels] Parsed items and armor for DuelType " + duelType.name);
            duelTypes.add(duelType);
        }
    }

    // iterate through items under a YAML key and stick them into an array
    private ItemStack[] parseItems(ConfigurationSection cfgSection) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        // if there are no items this will throw an NPE
        try {
            for (String itemKey : cfgSection.getKeys(false)) {
                int itemNum = cfgSection.getInt(itemKey + ".Number");
                ItemStack item = new ItemStack(Material.getMaterial(itemKey), itemNum);
                logger.info("[RedDuels] Found item: " + itemKey);
                parseEnchantments(itemKey, item, cfgSection);
                items.add(item);
            }
        } catch (NullPointerException e) {
            logger.info("[RedDuels] No items detected for DuelType");
        }
        return items.toArray(new ItemStack[items.size()]);
    }

    // iterate through items under a YAML key and stick them into an array
    // this is special because armor has to be arranged in a funky way
    private ItemStack[] parseArmor(ConfigurationSection cfgSection) {
        ItemStack[] items = new ItemStack[4];
        // if there are no items this will throw an NPE
        try {
            Iterator i = cfgSection.getKeys(false).iterator();
            while (i.hasNext()) {
                String itemKey = (String) i.next();
                int itemNum = cfgSection.getInt(itemKey + ".Number");
                ItemStack item = new ItemStack(Material.getMaterial(itemKey), itemNum);
                logger.info("[RedDuels] Found item: " + itemKey);
                parseEnchantments(itemKey, item, cfgSection);
                if (itemKey.contains("BOOTS")) items[0] = item;
                if (itemKey.contains("LEGGINGS")) items[1] = item;
                if (itemKey.contains("CHESTPLATE") || itemKey.equals("ELYTRA")) items[2] = item;
                if (itemKey.contains("HELMET")) items[3] = item;
            }
        } catch (NullPointerException e) {
            logger.info("[RedDuels] No armor detected for DuelType");
        }
            return items;
    }

    /**
     * Applies enchantments in a given ConfigurationSection to an ItemStack
     *
     * @param itemKey The ItemKey associated with the given item
     * @param item The ItemStack that enchantments will be applied to
     * @param cfgSection The ConfigurationSection associated with the given ItemStack
     */
    private void parseEnchantments(String itemKey, ItemStack item, ConfigurationSection cfgSection) {
        try {
            ConfigurationSection enchantmentsSection = cfgSection.getConfigurationSection(itemKey + ".Enchantments");
            Map<Enchantment, Integer> enchantments = new HashMap<>();
            Iterator i = enchantmentsSection.getKeys(false).iterator();
            while (i.hasNext()) {
                String enchantmentName = (String) i.next();
                logger.info("[RedDuels] Found enchantment " + enchantmentName + " for item " + itemKey);
                int enchantmentLevel = enchantmentsSection.getInt(enchantmentName);
                // enchantments should be taken by key because they are badly named
                NamespacedKey enchantmentKey = NamespacedKey.minecraft(enchantmentName.toLowerCase());
                Enchantment enchantment = Enchantment.getByKey(enchantmentKey);
                enchantments.put(enchantment, enchantmentLevel);
            }
            // add the enchantments to the item
            item.addEnchantments(enchantments);
        } catch (NullPointerException e) {
            logger.info("[RedDuels] No enchantments found for item " + item.getType().toString() + " in DuelType");
        }
    }

    public static RedDuelsConfig getInstance(Plugin plugin) {
        if (instance == null) {
            instance = new RedDuelsConfig(plugin);
        }
        return instance;
    }

    /*
    Adds a chat tag to a message
     */
    public String addChatTagIfEnabled(String stringIn) {
        if (chatTagEnabled) {
            String chatTagString = chatTagBracketColor + "[" + chatTagTextColor + chatTag + chatTagBracketColor + "] ";
            return chatTagString + stringIn;
        } else {
            return stringIn;
        }
    }

    /*
    Returns a formatted error message
     */
    public String formatError(String msg) {
        String msgOut = errorBold ? ChatColor.BOLD + "" : "";
        msgOut += errorColor;
        msgOut += msg;
        return addChatTagIfEnabled(msgOut);
    }

    /*
    Returns a formatted received duel offer message
     */
    public String formatReceivedOffer(String msg) {
        String msgOut = duelOfferReceivedBold ? ChatColor.BOLD + "" : "";
        msgOut += duelOfferReceivedColor;
        msgOut += msg;
        return addChatTagIfEnabled(msgOut);
    }

    /*
    Returns a formatted sent duel offer message
     */
    public String formatSentOffer(String msg) {
        String msgOut = duelOfferSentBold ? ChatColor.BOLD + "" : "";
        msgOut += duelOfferSentColor;
        msgOut += msg;
        return addChatTagIfEnabled(msgOut);
    }

    /*
    Returns a formatted info message
     */
    public String formatInfo(String msg) {
        String msgOut = infoBold ? ChatColor.BOLD + "" : "";
        msgOut += infoColor;
        msgOut += msg;
        return addChatTagIfEnabled(msgOut);
    }

    /*
    Returns a formatted info message - with or without a chat tag
    */
    public String formatInfo(String msg, boolean chatTag) {
        String msgOut = infoBold ? ChatColor.BOLD + "" : "";
        msgOut += infoColor;
        msgOut += msg;
        if (chatTag) {
            return addChatTagIfEnabled(msgOut);
        } else {
            return msgOut;
        }
    }

    /*
    Returns a formatted duel resolution message
     */
    public String formatDuelResolution(String msg) {
        String msgOut = duelResolutionBold ? ChatColor.BOLD + "" : "";
        msgOut += duelResolutionColor;
        msgOut += msg;
        return addChatTagIfEnabled(msgOut);
    }

    /*
    Returns true if info messages are enabled
     */
    public boolean isInfoEnabled() {
        return infoEnabled;
    }

    /*
    Converts a string to a chat color
     */
    public ChatColor stringToChatColor(String stringIn) {
        switch (stringIn) {
            case "BLACK": return ChatColor.BLACK;
            case "DARK_BLUE": return ChatColor.DARK_BLUE;
            case "DARK_GREEN": return ChatColor.DARK_GREEN;
            case "DARK_AQUA": return ChatColor.DARK_AQUA;
            case "DARK_PURPLE": return ChatColor.DARK_PURPLE;
            case "GOLD": return ChatColor.GOLD;
            case "DARK_GRAY": return ChatColor.DARK_GRAY;
            case "BLUE": return ChatColor.BLUE;
            case "GREEN": return ChatColor.GREEN;
            case "AQUA": return ChatColor.AQUA;
            case "RED": return ChatColor.RED;
            case "LIGHT_PURPLE": return ChatColor.LIGHT_PURPLE;
            case "YELLOW": return ChatColor.YELLOW;
            case "WHITE": return ChatColor.WHITE;
            case "MAGIC": return ChatColor.MAGIC;
            default: return ChatColor.DARK_RED;
        }
    }
}
