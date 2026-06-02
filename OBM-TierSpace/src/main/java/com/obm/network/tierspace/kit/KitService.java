package com.obm.network.tierspace.kit;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitService {

    private final Map<String, KitDefinition> kits = new HashMap<>();

    public void reload(FileConfiguration config) {
        kits.clear();
        ConfigurationSection section = config.getConfigurationSection("kits");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection kitSection = section.getConfigurationSection(id);
            if (kitSection != null) {
                kits.put(id.toLowerCase(), KitDefinition.fromConfig(kitSection));
            }
        }
    }

    public void applyKit(Player player, String kitId) {
        KitDefinition kit = kits.get(kitId.toLowerCase());
        if (kit == null) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(null);

        ItemStack helmet = enchant(new ItemStack(kit.armor()), kit.armorEnchantLevel());
        ItemStack chest = enchant(new ItemStack(kit.armor()), kit.armorEnchantLevel());
        ItemStack legs = enchant(new ItemStack(kit.armor()), kit.armorEnchantLevel());
        ItemStack boots = enchant(new ItemStack(kit.armor()), kit.armorEnchantLevel());

        inventory.setHelmet(helmet);
        inventory.setChestplate(chest);
        inventory.setLeggings(legs);
        inventory.setBoots(boots);

        ItemStack weapon = enchant(new ItemStack(kit.weapon()), kit.weaponEnchantLevel());
        inventory.setItem(kit.weaponSlot(), weapon);

        if (kit.offhand() != null && kit.offhand() != Material.AIR) {
            inventory.setItemInOffHand(new ItemStack(kit.offhand(), kit.offhandAmount()));
        }

        if (kit.food() != null && kit.food() != Material.AIR) {
            inventory.setItem(8, new ItemStack(kit.food(), kit.foodAmount()));
        }

        for (ExtraItem extra : kit.extraItems()) {
            ItemStack item = buildExtraItem(extra);
            if (item != null) {
                inventory.setItem(extra.slot(), item);
            }
        }

        for (PotionEffect effect : kit.effects()) {
            player.addPotionEffect(effect);
        }

        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
    }

    private ItemStack enchant(ItemStack item, int level) {
        if (level <= 0 || item.getType().isAir()) {
            return item;
        }
        Enchantment enchant = item.getType().name().contains("SWORD")
                || item.getType().name().contains("AXE")
                || item.getType().name().contains("MACE")
                ? Enchantment.DAMAGE_ALL
                : Enchantment.PROTECTION_ENVIRONMENTAL;
        item.addUnsafeEnchantment(enchant, level);
        return item;
    }

    private ItemStack buildExtraItem(ExtraItem extra) {
        Material material = Material.matchMaterial(extra.material());
        if (material == null || material.isAir()) {
            return null;
        }
        ItemStack item = new ItemStack(material, extra.amount());
        if (extra.enchantLevel() > 0) {
            item.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, extra.enchantLevel());
        }
        if (extra.potionType() != null && item.getItemMeta() instanceof PotionMeta meta) {
            try {
                PotionType type = PotionType.valueOf(extra.potionType());
                meta.setBasePotionData(new org.bukkit.potion.PotionData(type, false, extra.potionLevel() > 1));
                item.setItemMeta(meta);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return item;
    }

    public record KitDefinition(
            Material armor,
            int armorEnchantLevel,
            Material weapon,
            int weaponEnchantLevel,
            int weaponSlot,
            Material offhand,
            int offhandAmount,
            Material food,
            int foodAmount,
            List<ExtraItem> extraItems,
            List<PotionEffect> effects
    ) {
        static KitDefinition fromConfig(ConfigurationSection section) {
            Material armor = parseMaterial(section.getString("armor", "DIAMOND"), Material.DIAMOND);
            Material weapon = parseMaterial(section.getString("weapon", "DIAMOND_SWORD"), Material.DIAMOND_SWORD);
            Material offhand = parseMaterial(section.getString("offhand", "GOLDEN_APPLE"), Material.GOLDEN_APPLE);
            Material food = parseMaterial(section.getString("food", "COOKED_BEEF"), Material.COOKED_BEEF);

            List<ExtraItem> extras = new ArrayList<>();
            if (section.isList("extra-items")) {
                for (Object raw : section.getList("extra-items")) {
                    if (raw instanceof Map<?, ?> map) {
                        extras.add(ExtraItem.fromMap(map));
                    }
                }
            } else {
                ConfigurationSection extrasSection = section.getConfigurationSection("extra-items");
                if (extrasSection != null) {
                    for (String key : extrasSection.getKeys(false)) {
                        extras.add(ExtraItem.fromSection(extrasSection.getConfigurationSection(key)));
                    }
                }
            }

            List<PotionEffect> effects = new ArrayList<>();
            if (section.isList("effects")) {
                for (String line : section.getStringList("effects")) {
                    PotionEffect effect = parseEffect(line);
                    if (effect != null) {
                        effects.add(effect);
                    }
                }
            }

            return new KitDefinition(
                    armor,
                    section.getInt("armor-enchant-level", 0),
                    weapon,
                    section.getInt("weapon-enchant-level", 0),
                    section.getInt("weapon-slot", 0),
                    offhand,
                    section.getInt("offhand-amount", 8),
                    food,
                    section.getInt("food-amount", 32),
                    extras,
                    effects
            );
        }

        private static Material parseMaterial(String name, Material fallback) {
            Material material = Material.matchMaterial(name == null ? "" : name);
            return material == null ? fallback : material;
        }

        private static PotionEffect parseEffect(String line) {
            if (line == null || line.isBlank()) {
                return null;
            }
            String[] parts = line.split(":");
            try {
                PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                if (type == null) {
                    return null;
                }
                int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) - 1 : 0;
                int duration = parts.length > 2 ? Integer.parseInt(parts[2]) * 20 : 20 * 60 * 60;
                return new PotionEffect(type, duration, Math.max(0, amplifier));
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    public record ExtraItem(
            String material,
            int amount,
            int slot,
            int enchantLevel,
            String potionType,
            int potionLevel
    ) {
        static ExtraItem fromSection(ConfigurationSection section) {
            if (section == null) {
                return new ExtraItem("AIR", 1, 0, 0, null, 1);
            }
            return new ExtraItem(
                    section.getString("material", "STONE"),
                    section.getInt("amount", 1),
                    section.getInt("slot", 1),
                    section.getInt("enchant-level", 0),
                    section.getString("potion-type"),
                    section.getInt("potion-level", 1)
            );
        }

        static ExtraItem fromMap(Map<?, ?> map) {
            return new ExtraItem(
                    stringValue(map.get("material"), "STONE"),
                    parseInt(map.get("amount"), 1),
                    parseInt(map.get("slot"), 1),
                    parseInt(map.get("enchant-level"), 0),
                    map.get("potion-type") == null ? null : stringValue(map.get("potion-type"), null),
                    parseInt(map.get("potion-level"), 1)
            );
        }

        private static String stringValue(Object value, String fallback) {
            return value == null ? fallback : value.toString();
        }

        private static int parseInt(Object value, int fallback) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            try {
                return value == null ? fallback : Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
    }
}
