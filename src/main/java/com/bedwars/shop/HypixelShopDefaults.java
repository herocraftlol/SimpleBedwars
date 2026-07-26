package com.bedwars.shop;

import com.bedwars.arena.GeneratorType;
import org.bukkit.Material;

/**
 * Remplit une {@link ShopConfig} fraîchement créée avec un contenu par défaut inspiré
 * des vrais prix/catégories du shop Hypixel BedWars, pour que le plugin soit jouable
 * "prêt à l'emploi" sans configuration manuelle. Tout reste entièrement éditable
 * ensuite via les commandes /bd shop ...
 *
 * Les prix sont ceux couramment documentés pour le mode 3v3v3v3/4v4v4v4 ; Hypixel les
 * ajuste de temps en temps, donc considère ça comme un point de départ fidèle plutôt
 * qu'une garantie de valeurs strictement identiques à la version actuelle du jeu.
 */
public final class HypixelShopDefaults {

    private HypixelShopDefaults() {}

    public static void apply(ShopConfig config) {
        buildBaseMenu(config);
        buildBlocks(config);
        buildMelee(config);
        buildArmor(config);
        buildTools(config);
        buildBow(config);
        buildUtility(config);
        buildUpgrades(config);
    }

    private static void buildBaseMenu(ShopConfig config) {
        GuiDefinition base = config.getOrCreateShopGui(GuiDefinition.BASE);
        base.setSlot(10, GuiSlot.shopLink(Material.WHITE_WOOL, "blocks"));
        base.setSlot(11, GuiSlot.shopLink(Material.IRON_SWORD, "melee"));
        base.setSlot(12, GuiSlot.shopLink(Material.IRON_CHESTPLATE, "armor"));
        base.setSlot(13, GuiSlot.shopLink(Material.IRON_PICKAXE, "tools"));
        base.setSlot(14, GuiSlot.shopLink(Material.BOW, "bow"));
        base.setSlot(15, GuiSlot.shopLink(Material.ENDER_PEARL, "utility"));
    }

    private static void buildBlocks(ShopConfig config) {
        GuiDefinition gui = config.getOrCreateShopGui("blocks");
        // WHITE_WOOL sert de "joker" : recoloré automatiquement à la couleur de
        // l'équipe de l'acheteur au moment de l'achat (voir ShopGUIManager).
        gui.setSlot(10, GuiSlot.shopItem(Material.WHITE_WOOL, 16, 4, GeneratorType.FER));
        gui.setSlot(11, GuiSlot.shopItem(Material.SANDSTONE, 16, 12, GeneratorType.FER));
        gui.setSlot(12, GuiSlot.shopItem(Material.END_STONE, 12, 24, GeneratorType.FER));
        gui.setSlot(13, GuiSlot.shopItem(Material.LADDER, 16, 4, GeneratorType.FER));
        gui.setSlot(14, GuiSlot.shopItem(Material.OAK_PLANKS, 16, 4, GeneratorType.OR));
        gui.setSlot(15, GuiSlot.shopItem(Material.OBSIDIAN, 4, 4, GeneratorType.EMERAUDE));
    }

    private static void buildMelee(ShopConfig config) {
        GuiDefinition gui = config.getOrCreateShopGui("melee");
        gui.setSlot(10, GuiSlot.shopItem(Material.STONE_SWORD, 1, 10, GeneratorType.FER));
        gui.setSlot(11, GuiSlot.shopItem(Material.IRON_SWORD, 1, 7, GeneratorType.OR));
        gui.setSlot(12, GuiSlot.shopItem(Material.DIAMOND_SWORD, 1, 4, GeneratorType.EMERAUDE));
        gui.setSlot(13, GuiSlot.shopItem(Material.STICK, 1, 10, GeneratorType.OR));
    }

    private static void buildArmor(ShopConfig config) {
        GuiDefinition gui = config.getOrCreateShopGui("armor");
        gui.setSlot(10, GuiSlot.shopItem(Material.CHAINMAIL_BOOTS, 1, 40, GeneratorType.FER));
        gui.setSlot(11, GuiSlot.shopItem(Material.IRON_BOOTS, 1, 12, GeneratorType.OR));
        gui.setSlot(12, GuiSlot.shopItem(Material.DIAMOND_BOOTS, 1, 6, GeneratorType.EMERAUDE));
    }

    private static void buildTools(ShopConfig config) {
        GuiDefinition gui = config.getOrCreateShopGui("tools");
        gui.setSlot(10, GuiSlot.shopItem(Material.WOODEN_PICKAXE, 1, 10, GeneratorType.FER));
        gui.setSlot(11, GuiSlot.shopItem(Material.IRON_PICKAXE, 1, 6, GeneratorType.OR));
        gui.setSlot(12, GuiSlot.shopItem(Material.WOODEN_AXE, 1, 10, GeneratorType.FER));
        gui.setSlot(13, GuiSlot.shopItem(Material.IRON_AXE, 1, 6, GeneratorType.OR));
        gui.setSlot(14, GuiSlot.shopItem(Material.SHEARS, 1, 20, GeneratorType.FER));
    }

    private static void buildBow(ShopConfig config) {
        GuiDefinition gui = config.getOrCreateShopGui("bow");
        gui.setSlot(10, GuiSlot.shopItem(Material.ARROW, 8, 2, GeneratorType.OR));
        gui.setSlot(11, GuiSlot.shopItem(Material.BOW, 1, 12, GeneratorType.OR));
        gui.setSlot(12, GuiSlot.shopItem(Material.BOW, 1, 24, GeneratorType.OR)); // "Power I" (même item, à personnaliser)
        gui.setSlot(13, GuiSlot.shopItem(Material.BOW, 1, 6, GeneratorType.EMERAUDE)); // "Power I, Punch I"
    }

    private static void buildUtility(ShopConfig config) {
        GuiDefinition gui = config.getOrCreateShopGui("utility");
        gui.setSlot(10, GuiSlot.shopItem(Material.TNT, 1, 4, GeneratorType.OR));
        gui.setSlot(11, GuiSlot.shopItem(Material.WATER_BUCKET, 1, 2, GeneratorType.OR));
        gui.setSlot(12, GuiSlot.shopItem(Material.GOLDEN_APPLE, 1, 3, GeneratorType.OR));
        gui.setSlot(13, GuiSlot.shopItem(Material.ENDER_PEARL, 1, 4, GeneratorType.OR));
        gui.setSlot(14, GuiSlot.shopItem(Material.SPONGE, 1, 4, GeneratorType.FER));
        gui.setSlot(15, GuiSlot.shopItem(Material.MILK_BUCKET, 1, 4, GeneratorType.OR));
        gui.setSlot(16, GuiSlot.shopItem(Material.FIRE_CHARGE, 1, 40, GeneratorType.FER));
        gui.setSlot(17, GuiSlot.shopItem(Material.IRON_GOLEM_SPAWN_EGG, 1, 120, GeneratorType.FER)); // "Dream Defender"
    }

    private static void buildUpgrades(ShopConfig config) {
        GuiDefinition gui = config.getUpgradeGui();
        gui.setSlot(10, GuiSlot.upgradeItem(Material.BLAZE_POWDER, UpgradeType.FORGE, 1));
        gui.setSlot(11, GuiSlot.upgradeItem(Material.IRON_SWORD, UpgradeType.SHARP, 1));
        gui.setSlot(12, GuiSlot.upgradeItem(Material.IRON_CHESTPLATE, UpgradeType.ARMOR, 1));
        gui.setSlot(13, GuiSlot.upgradeItem(Material.IRON_PICKAXE, UpgradeType.MANIAC, 1));
        gui.setSlot(14, GuiSlot.upgradeItem(Material.FEATHER, UpgradeType.CHUTE, 1));
        gui.setSlot(15, GuiSlot.upgradeItem(Material.GOLDEN_APPLE, UpgradeType.HEAL, 1));
        gui.setSlot(19, GuiSlot.upgradeItem(Material.TRIPWIRE_HOOK, UpgradeType.TRAP_ALARM, 1));
        gui.setSlot(20, GuiSlot.upgradeItem(Material.SLIME_BALL, UpgradeType.TRAP_BLIND, 1));
        gui.setSlot(21, GuiSlot.upgradeItem(Material.REDSTONE, UpgradeType.TRAP_MINER, 1));
        gui.setSlot(22, GuiSlot.upgradeItem(Material.DRAGON_HEAD, UpgradeType.DRAGON, 1));

        // Forge : niveau 1 = base, 2 = Iron Forge, 3 = Golden Forge, 4 = Emerald Forge, 5 = Molten Forge
        UpgradeTypeConfig forge = config.getUpgradeConfig(UpgradeType.FORGE);
        forge.setMaxLevel(5);
        forge.setPriceForLevel(2, 2);
        forge.setPriceForLevel(3, 4);
        forge.setPriceForLevel(4, 6);
        forge.setPriceForLevel(5, 8);

        // Sharpened Swords : sur Hypixel, un seul palier (Sharpness I). On garde 2 niveaux (1=base, 2=Sharpness I).
        UpgradeTypeConfig sharp = config.getUpgradeConfig(UpgradeType.SHARP);
        sharp.setMaxLevel(2);
        sharp.setPriceForLevel(2, 8);

        // Reinforced Armor : Protection I à IV
        UpgradeTypeConfig armor = config.getUpgradeConfig(UpgradeType.ARMOR);
        armor.setMaxLevel(5);
        armor.setPriceForLevel(2, 2);
        armor.setPriceForLevel(3, 4);
        armor.setPriceForLevel(4, 8);
        armor.setPriceForLevel(5, 16);

        // Maniac Miner : Haste I puis II
        UpgradeTypeConfig maniac = config.getUpgradeConfig(UpgradeType.MANIAC);
        maniac.setMaxLevel(3);
        maniac.setPriceForLevel(2, 2);
        maniac.setPriceForLevel(3, 4);

        // Chute (upgrade non-officielle Hypixel, ajoutée sur demande) : réduction progressive
        UpgradeTypeConfig chute = config.getUpgradeConfig(UpgradeType.CHUTE);
        chute.setMaxLevel(5);
        chute.setPriceForLevel(2, 2);
        chute.setPriceForLevel(3, 4);
        chute.setPriceForLevel(4, 8);
        chute.setPriceForLevel(5, 16);

        // Pièges (à usage unique, se rachètent après déclenchement)
        UpgradeTypeConfig trapAlarm = config.getUpgradeConfig(UpgradeType.TRAP_ALARM);
        trapAlarm.setMaxLevel(2);
        trapAlarm.setPriceForLevel(1, 2);
        trapAlarm.setPriceForLevel(2, 4);

        UpgradeTypeConfig trapBlind = config.getUpgradeConfig(UpgradeType.TRAP_BLIND);
        trapBlind.setMaxLevel(2);
        trapBlind.setPriceForLevel(1, 1);
        trapBlind.setPriceForLevel(2, 2);

        UpgradeTypeConfig trapMiner = config.getUpgradeConfig(UpgradeType.TRAP_MINER);
        trapMiner.setMaxLevel(2);
        trapMiner.setPriceForLevel(1, 2);
        trapMiner.setPriceForLevel(2, 4);

        UpgradeTypeConfig heal = config.getUpgradeConfig(UpgradeType.HEAL);
        heal.setPriceForLevel(1, 2);

        UpgradeTypeConfig dragon = config.getUpgradeConfig(UpgradeType.DRAGON);
        dragon.setPriceForLevel(1, 5);
    }
}
