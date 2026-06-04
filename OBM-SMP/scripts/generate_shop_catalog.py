#!/usr/bin/env python3
"""
Gera shop.categories para OBM-SMP (Material names 1.20+).
Exclui spawn eggs, spawners e itens de comando/debug.
"""
from __future__ import annotations

import urllib.request
import re
from collections import OrderedDict

# Spigot 1.21 material enum (subset via known list file)
ENUM_URL = "https://raw.githubusercontent.com/Hubert-MC/MinecraftMaterials/main/1.21/materials.txt"

CATEGORIES = [
    "combate",
    "armaduras",
    "comida",
    "mineracao",
    "construcao",
    "decoracao",
    "utilidades",
    "pocoes",
    "armazenamento",
    "redstone",
    "raros",
]

BLOCKED_SHOP = {
    "NETHERITE_INGOT",
    "NETHERITE_BLOCK",
    "NETHERITE_SCRAP",
    "ELYTRA",
    "DRAGON_EGG",
    "BEACON",
}

SKIP_EXACT = {
    "AIR", "CAVE_AIR", "VOID_AIR", "BARRIER", "STRUCTURE_VOID",
    "STRUCTURE_BLOCK", "JIGSAW", "COMMAND_BLOCK", "CHAIN_COMMAND_BLOCK",
    "REPEATING_COMMAND_BLOCK", "COMMAND_BLOCK_MINECART",
    "SPAWNER", "TRIAL_SPAWNER", "VAULT", "LIGHT",
    "DEBUG_STICK", "KNOWLEDGE_BOOK", "BUNDLE",
    "FILLED_MAP", "MAP",
    "FIREWORK_ROCKET", "FIREWORK_STAR",
    "ENCHANTED_BOOK", "WRITTEN_BOOK", "WRITABLE_BOOK",
    "PLAYER_HEAD", "DRAGON_HEAD", "ZOMBIE_HEAD", "CREEPER_HEAD",
    "PIGLIN_HEAD", "SKELETON_SKULL", "WITHER_SKELETON_SKULL",
}

SKIP_PREFIX = (
    "SPAWN_EGG",
    "LEGACY_",
)

FOOD = {
    "APPLE", "BAKED_POTATO", "BEEF", "BEETROOT", "BEETROOT_SOUP", "BREAD",
    "CARROT", "CHICKEN", "CHORUS_FRUIT", "COD", "COOKED_BEEF", "COOKED_CHICKEN",
    "COOKED_COD", "COOKED_MUTTON", "COOKED_PORKCHOP", "COOKED_RABBIT",
    "COOKED_SALMON", "COOKIE", "DRIED_KELP", "ENCHANTED_GOLDEN_APPLE",
    "GOLDEN_APPLE", "GOLDEN_CARROT", "HONEY_BOTTLE", "MELON_SLICE",
    "MUSHROOM_STEW", "MUTTON", "POISONOUS_POTATO", "PORKCHOP", "POTATO",
    "PUFFERFISH", "PUMPKIN_PIE", "RABBIT", "RABBIT_STEW", "ROTTEN_FLESH",
    "SALMON", "SPIDER_EYE", "SUSPICIOUS_STEW", "SWEET_BERRIES", "TROPICAL_FISH",
    "GLOW_BERRIES", "BEEF", "PORKCHOP", "CHICKEN", "COD", "SALMON",
    "CAKE", "PUMPKIN", "MELON", "SUGAR", "EGG", "MILK_BUCKET", "HONEYCOMB",
    "COCOA_BEANS", "KELP", "SEAGRASS", "BAMBOO", "CACTUS", "WHEAT",
    "WHEAT_SEEDS", "BEETROOT_SEEDS", "MELON_SEEDS", "PUMPKIN_SEEDS",
    "TORCHFLOWER_SEEDS", "PITCHER_POD",
}

COMBATE = {
    "WOODEN_SWORD", "STONE_SWORD", "IRON_SWORD", "GOLDEN_SWORD", "DIAMOND_SWORD",
    "NETHERITE_SWORD", "WOODEN_AXE", "STONE_AXE", "IRON_AXE", "GOLDEN_AXE",
    "DIAMOND_AXE", "NETHERITE_AXE", "BOW", "CROSSBOW", "TRIDENT", "SHIELD",
    "ARROW", "SPECTRAL_ARROW", "TIPPED_ARROW", "FIREWORK_ROCKET",
    "MACE", "WIND_CHARGE", "BREEZE_ROD",
}

ARMOR = {n for n in """
LEATHER_HELMET LEATHER_CHESTPLATE LEATHER_LEGGINGS LEATHER_BOOTS
CHAINMAIL_HELMET CHAINMAIL_CHESTPLATE CHAINMAIL_LEGGINGS CHAINMAIL_BOOTS
IRON_HELMET IRON_CHESTPLATE IRON_LEGGINGS IRON_BOOTS
GOLDEN_HELMET GOLDEN_CHESTPLATE GOLDEN_LEGGINGS GOLDEN_BOOTS
DIAMOND_HELMET DIAMOND_CHESTPLATE DIAMOND_LEGGINGS DIAMOND_BOOTS
NETHERITE_HELMET NETHERITE_CHESTPLATE NETHERITE_LEGGINGS NETHERITE_BOOTS
TURTLE_HELMET ELYTRA CARVED_PUMPKIN
""".split()}

TOOLS = {n for n in """
WOODEN_PICKAXE STONE_PICKAXE IRON_PICKAXE GOLDEN_PICKAXE DIAMOND_PICKAXE NETHERITE_PICKAXE
WOODEN_SHOVEL STONE_SHOVEL IRON_SHOVEL GOLDEN_SHOVEL DIAMOND_SHOVEL NETHERITE_SHOVEL
WOODEN_HOE STONE_HOE IRON_HOE GOLDEN_HOE DIAMOND_HOE NETHERITE_HOE
SHEARS FISHING_ROD FLINT_AND_STEEL BRUSH
""".split()}

STORAGE = {
    "CHEST", "TRAPPED_CHEST", "ENDER_CHEST", "BARREL", "SHULKER_BOX",
    "WHITE_SHULKER_BOX", "ORANGE_SHULKER_BOX", "MAGENTA_SHULKER_BOX",
    "LIGHT_BLUE_SHULKER_BOX", "YELLOW_SHULKER_BOX", "LIME_SHULKER_BOX",
    "PINK_SHULKER_BOX", "GRAY_SHULKER_BOX", "LIGHT_GRAY_SHULKER_BOX",
    "CYAN_SHULKER_BOX", "PURPLE_SHULKER_BOX", "BLUE_SHULKER_BOX",
    "BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX", "RED_SHULKER_BOX", "BLACK_SHULKER_BOX",
    "HOPPER", "DROPPER", "DISPENSER",
}

REDSTONE = {n for n in """
REDSTONE REDSTONE_BLOCK REDSTONE_TORCH REDSTONE_WALL_TORCH REPEATER COMPARATOR
OBSERVER PISTON STICKY_PISTON PISTON_HEAD MOVER PISTON_EXTENSION
LEVER BUTTON STONE_BUTTON OAK_BUTTON POWERED_RAIL DETECTOR_RAIL ACTIVATOR_RAIL RAIL
DAYLIGHT_DETECTOR TRIPWIRE_HOOK TRIPWIRE TARGET NOTE_BLOCK BELL SCULK_SENSOR
CALIBRATED_SCULK_SENSOR SCULK_SHRIEKER HOPPER DROPPER DISPENSER
REDSTONE_LAMP OAK_PRESSURE_PLATE STONE_PRESSURE_PLATE HEAVY_WEIGHTED_PRESSURE_PLATE
LIGHT_WEIGHTED_PRESSURE_PLATE SLIME_BLOCK HONEY_BLOCK TNT MINECART CHEST_MINECART
HOPPER_MINECART FURNACE_MINECART TNT_MINECART REDSTONE_ORE DEEPSLATE_REDSTONE_ORE
""".split() if n}

POTIONS = {n for n in """
POTION SPLASH_POTION LINGERING_POTION TIPPED_ARROW
GLASS_BOTTLE DRAGON_BREATH FERMENTED_SPIDER_EYE BLAZE_POWDER MAGMA_CREAM
GHAST_TEAR GOLDEN_CARROT RABBIT_FOOT TURTLE_HELMET PHANTOM_MEMBRANE
GUNPOWDER NETHER_WART BREWING_STAND CAULDRON
""".split() if n}

RARE = {n for n in """
NETHERITE_INGOT NETHERITE_BLOCK NETHERITE_SCRAP ANCIENT_DEBRIS
BEACON DRAGON_EGG ELYTRA NETHER_STAR DRAGON_BREATH
ENCHANTED_GOLDEN_APPLE TOTEM_OF_UNDYING HEART_OF_THE_SEA TRIDENT
ECHO_SHARD RECOVERY_COMPASS NETHERITE_UPGRADE_SMITHING_TEMPLATE
COAST_ARMOR_TRIM_SMITHING_TEMPLATE DUNE_ARMOR_TRIM_SMITHING_TEMPLATE
EYE_ARMOR_TRIM_SMITHING_TEMPLATE HOST_ARMOR_TRIM_SMITHING_TEMPLATE
RAISER_ARMOR_TRIM_SMITHING_TEMPLATE RIB_ARMOR_TRIM_SMITHING_TEMPLATE
SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE
SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE
SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE TIDE_ARMOR_TRIM_SMITHING_TEMPLATE
VEX_ARMOR_TRIM_SMITHING_TEMPLATE WARD_ARMOR_TRIM_SMITHING_TEMPLATE
WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE WILD_ARMOR_TRIM_SMITHING_TEMPLATE
EXPERIENCE_BOTTLE END_CRYSTAL DRAGON_EGG
""".split() if n}

ORES = set()
for base in (
    "COAL", "IRON", "GOLD", "DIAMOND", "EMERALD", "LAPIS", "REDSTONE",
    "COPPER", "NETHERITE", "QUARTZ", "AMETHYST",
):
    for suffix in ("", "_ORE", "_INGOT", "_BLOCK", "_NUGGET", "_RAW"):
        ORES.add(f"{base}{suffix}")
        ORES.add(f"DEEPSLATE_{base}{suffix}")
        ORES.add(f"RAW_{base}")
ORES.update({
    "COAL", "CHARCOAL", "COAL_BLOCK", "IRON_INGOT", "IRON_BLOCK", "RAW_IRON",
    "GOLD_INGOT", "GOLD_BLOCK", "RAW_GOLD", "GOLD_NUGGET", "COPPER_INGOT",
    "RAW_COPPER", "COPPER_BLOCK", "DIAMOND", "DIAMOND_BLOCK", "EMERALD",
    "EMERALD_BLOCK", "LAPIS_LAZULI", "LAPIS_BLOCK", "REDSTONE", "REDSTONE_BLOCK",
    "QUARTZ", "NETHER_QUARTZ_ORE", "AMETHYST_SHARD", "AMETHYST_BLOCK",
    "BUDDING_AMETHYST", "SMALL_AMETHYST_BUD", "MEDIUM_AMETHYST_BUD",
    "LARGE_AMETHYST_BUD", "AMETHYST_CLUSTER", "ANCIENT_DEBRIS", "NETHERITE_SCRAP",
    "NETHERITE_INGOT", "GLOWSTONE", "GLOWSTONE_DUST", "BLAZE_ROD", "BLAZE_POWDER",
    "MAGMA_CREAM", "GHAST_TEAR", "ENDER_PEARL", "ENDER_EYE", "SHULKER_SHELL",
    "PRISMARINE_SHARD", "PRISMARINE_CRYSTALS", "NAUTILUS_SHELL", "SCUTE",
})

UTIL = {
    "BUCKET", "WATER_BUCKET", "LAVA_BUCKET", "MILK_BUCKET", "POWDER_SNOW_BUCKET",
    "AXOLOTL_BUCKET", "TADPOLE_BUCKET", "COD_BUCKET", "SALMON_BUCKET",
    "PUFFERFISH_BUCKET", "TROPICAL_FISH_BUCKET", "ENDER_PEARL", "ENDER_EYE",
    "EXPERIENCE_BOTTLE", "LEAD", "NAME_TAG", "SADDLE", "CARROT_ON_A_STICK",
    "WARPED_FUNGUS_ON_A_STICK", "COMPASS", "CLOCK", "SPYGLASS", "RECOVERY_COMPASS",
    "LANTERN", "SOUL_LANTERN", "TORCH", "SOUL_TORCH", "CAMPFIRE", "SOUL_CAMPFIRE",
    "LADDER", "CHAIN", "ANVIL", "CHIPPED_ANVIL", "DAMAGED_ANVIL", "GRINDSTONE",
    "SMITHING_TABLE", "FURNACE", "BLAST_FURNACE", "SMOKER", "CARTOGRAPHY_TABLE",
    "FLETCHING_TABLE", "LOOM", "STONECUTTER", "COMPOSTER", "LECTERN",
    "CRAFTING_TABLE", "ENCHANTING_TABLE", "BOOKSHELF", "BOOK", "PAPER",
    "MAP", "EMPTY_MAP", "ITEM_FRAME", "GLOW_ITEM_FRAME", "PAINTING",
    "ARMOR_STAND", "END_CRYSTAL", "FIRE_CHARGE", "BONE_MEAL", "STRING",
    "FEATHER", "FLINT", "GUNPOWDER", "BLAZE_ROD", "BREEZE_ROD", "WIND_CHARGE",
    "STICK", "BOWL", "GLASS_BOTTLE", "CLAY_BALL", "BRICK", "NETHER_BRICK",
    "SLIME_BALL", "PHANTOM_MEMBRANE", "RABBIT_HIDE", "LEATHER", "SCUTE",
    "TURTLE_SCUTE", "ARMADILLO_SCUTE", "HONEYCOMB", "CANDLE", "GOAT_HORN",
    "OMINOUS_BOTTLE", "TRIAL_KEY", "OMINOUS_TRIAL_KEY",
}


def load_materials() -> list[str]:
    import os
    import pathlib
    import subprocess
    import sys

    script_dir = pathlib.Path(__file__).parent
    local = script_dir / "materials-1.21.txt"

    def read_local() -> list[str] | None:
        if not local.exists():
            return None
        lines = [
            l.strip()
            for l in local.read_text(encoding="utf-8").splitlines()
            if l.strip() and not l.startswith("404")
        ]
        return lines if len(lines) > 100 else None

    cached = read_local()
    if cached:
        return cached

    try:
        with urllib.request.urlopen(ENUM_URL, timeout=15) as r:
            text = r.read().decode("utf-8")
        lines = [
            line.strip()
            for line in text.splitlines()
            if line.strip() and not line.startswith("#")
        ]
        if len(lines) > 100:
            local.write_text("\n".join(lines) + "\n", encoding="utf-8")
            return lines
    except Exception:
        pass

    smp_root = script_dir.parent
    dump_java = script_dir / "DumpMaterials.java"
    try:
        mvn = "mvn.cmd" if os.name == "nt" else "mvn"
        cp_proc = subprocess.run(
            [mvn, "-q", "-f", str(smp_root / "pom.xml"), "dependency:build-classpath",
             "-Dmdep.outputFile=target/materials-cp.txt"],
            cwd=str(smp_root),
            capture_output=True,
            text=True,
            timeout=120,
        )
        if cp_proc.returncode != 0:
            raise RuntimeError(cp_proc.stderr or cp_proc.stdout)
        cp_file = smp_root / "target" / "materials-cp.txt"
        classpath = cp_file.read_text(encoding="utf-8").strip()
        compile_proc = subprocess.run(
            ["javac", "-cp", classpath, str(dump_java)],
            capture_output=True,
            text=True,
            timeout=60,
        )
        if compile_proc.returncode != 0:
            raise RuntimeError(compile_proc.stderr)
        run_proc = subprocess.run(
            ["java", "-cp", classpath + os.pathsep + str(script_dir),
             "DumpMaterials"],
            capture_output=True,
            text=True,
            timeout=60,
        )
        if run_proc.returncode != 0:
            raise RuntimeError(run_proc.stderr)
        lines = [l.strip() for l in run_proc.stdout.splitlines() if l.strip()]
        if len(lines) > 100:
            local.write_text("\n".join(lines) + "\n", encoding="utf-8")
            return lines
    except Exception as ex:
        print(f"Java Material dump failed: {ex}", file=sys.stderr)

    raise SystemExit("Materials list unavailable; run mvn in OBM-SMP then re-run script")


def should_skip(name: str) -> bool:
    if name in SKIP_EXACT:
        return True
    for p in SKIP_PREFIX:
        if name.startswith(p):
            return True
    if "SPAWN_EGG" in name:
        return True
    if name.endswith("_SPAWN_EGG"):
        return True
    if "INFESTED" in name:
        return True
    if name in ("PISTON_HEAD", "MOVING_PISTON", "FROSTED_ICE", "BUBBLE_COLUMN"):
        return True
    return False


def price_for(name: str, cat: str) -> int:
    n = name
    if cat == "raros":
        if "NETHERITE" in n or n == "BEACON" or n == "DRAGON_EGG":
            return 2500
        if "ENCHANTED_GOLDEN" in n or n == "TOTEM_OF_UNDYING":
            return 1800
        if "SMITHING_TEMPLATE" in n or "TRIM" in n:
            return 800
        return 600
    if cat == "pocoes":
        if "POTION" in n or "TIPPED" in n:
            return 45
        if n in ("BREWING_STAND", "CAULDRON", "DRAGON_BREATH"):
            return 120
        return 25
    if cat == "combate":
        if "NETHERITE" in n:
            return 900
        if "DIAMOND" in n:
            return 280
        if "BOW" in n or "CROSSBOW" in n or n == "TRIDENT":
            return 220
        if "ARROW" in n:
            return 4
        if "SHIELD" in n:
            return 150
        return 120
    if cat == "armaduras":
        if "NETHERITE" in n:
            return 850
        if "DIAMOND" in n:
            if "CHEST" in n:
                return 320
            if "LEGG" in n:
                return 280
            return 200
        if "IRON" in n:
            if "CHEST" in n:
                return 95
            return 65
        if "CHAIN" in n:
            return 55
        if "LEATHER" in n:
            return 35
        return 80
    if cat == "mineracao":
        if "NETHERITE" in n or "ANCIENT" in n:
            return 650
        if "DIAMOND" in n:
            return 220
        if "EMERALD" in n:
            return 180
        if "GOLD" in n:
            return 90
        if "IRON" in n:
            return 55
        if "COPPER" in n:
            return 35
        if "COAL" in n or "CHARCOAL" in n:
            return 12
        if "LAPIS" in n:
            return 28
        if "REDSTONE" in n and "ORE" in n:
            return 18
        if "QUARTZ" in n or "AMETHYST" in n:
            return 40
        if "ORE" in n or "RAW_" in n:
            return 22
        if "INGOT" in n or "NUGGET" in n:
            return 45
        return 20
    if cat == "comida":
        if "ENCHANTED_GOLDEN" in n:
            return 1200
        if "GOLDEN_APPLE" in n:
            return 180
        if "GOLDEN" in n:
            return 45
        if "STEW" in n or "SOUP" in n:
            return 22
        if "BEEF" in n or "PORK" in n or "CHICKEN" in n or "MUTTON" in n:
            return 18
        return 10
    if cat == "utilidades":
        if "NETHERITE" in n:
            return 500
        if "DIAMOND" in n:
            return 200
        if "IRON" in n:
            return 90
        if "BUCKET" in n:
            return 35
        if "ENDER_PEARL" in n:
            return 85
        if "ANVIL" in n:
            return 250
        if "ENCHANTING" in n:
            return 320
        return 55
    if cat == "redstone":
        if "MINECART" in n:
            return 75
        if "OBSERVER" in n or "COMPARATOR" in n or "REPEATER" in n:
            return 45
        if "PISTON" in n:
            return 55
        return 28
    if cat == "armazenamento":
        if "SHULKER" in n:
            return 180
        if "ENDER_CHEST" in n:
            return 350
        return 35
    if cat == "decoracao":
        if "SHULKER" in n:
            return 120
        if "GLASS" in n or "STAINED" in n:
            return 12
        if "WOOL" in n or "CARPET" in n or "BANNER" in n:
            return 8
        if "CONCRETE" in n or "TERRACOTTA" in n or "GLAZED" in n:
            return 14
        if "FLOWER" in n or "PETALS" in n or "ROSE" in n or "ORCHID" in n:
            return 6
        if "CANDLE" in n:
            return 10
        if "MUSIC_DISC" in n:
            return 250
        return 7
    # construcao
    if "OBSIDIAN" in n:
        return 45
    if "CRYING_OBSIDIAN" in n:
        return 55
    if "LOG" in n or "STEM" in n or "HYPHAE" in n:
        return 8
    if "PLANKS" in n or "WOOD" in n:
        return 6
    if "STONE" in n or "DEEPSLATE" in n or "COBBLE" in n:
        return 4
    if "BRICK" in n:
        return 10
    if "SAND" in n or "GRAVEL" in n or "DIRT" in n:
        return 3
    if "ICE" in n:
        return 8
    if "LEAVES" in n or "SAPLING" in n:
        return 4
    return 5


def categorize(name: str) -> str | None:
    if should_skip(name):
        return None
    if name in BLOCKED_SHOP:
        return "raros"  # still listed in raros for display but blocked by plugin

    if name in COMBATE or any(x in name for x in ("_SWORD", "CROSSBOW")) and "BANNER" not in name:
        if "BANNER" in name:
            pass
        elif name.endswith("_SWORD") or name in ("BOW", "CROSSBOW", "TRIDENT", "SHIELD", "MACE"):
            return "combate"
        elif name.endswith("_AXE") and "PICKAXE" not in name and name not in TOOLS:
            return "combate"

    if name in ARMOR or any(name.endswith(s) for s in ("_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS")):
        return "armaduras"

    if name in FOOD or name.endswith("_STEW") or "CHICKEN" in name and "BUCKET" not in name:
        return "comida"

    if name in POTIONS or "POTION" in name:
        return "pocoes"

    if name in STORAGE or "SHULKER_BOX" in name or name in ("CHEST", "BARREL", "ENDER_CHEST"):
        return "armazenamento"

    if name in REDSTONE or "REDSTONE" in name or name.endswith("_PRESSURE_PLATE") and "WEIGHTED" in name:
        return "redstone"
    if name.endswith("_PRESSURE_PLATE") or name.endswith("_BUTTON") and "STONE" not in name:
        if "WOOD" in name or "OAK" in name or "PLANKS" in name:
            return "redstone"

    if name in RARE or "NETHERITE" in name or name == "BEACON" or "DRAGON_EGG" in name:
        return "raros"

    if name in ORES or "_ORE" in name or name.startswith("RAW_") or name.endswith("_INGOT"):
        return "mineracao"
    if name.endswith("_NUGGET") or name.endswith("_BLOCK") and any(m in name for m in ("COAL", "IRON", "GOLD", "DIAMOND", "EMERALD", "LAPIS", "COPPER", "NETHERITE")):
        return "mineracao"

    if name in TOOLS or name.endswith("_PICKAXE") or name.endswith("_SHOVEL") or name.endswith("_HOE"):
        return "utilidades"

    if name in UTIL or "BUCKET" in name:
        return "utilidades"

    decor_kw = (
        "WOOL", "CARPET", "BANNER", "CANDLE", "FLOWER", "ORCHID", "TULIP", "ROSE",
        "DAISY", "LILY", "PETAL", "VINE", "MOSS", "FERN", "BUSH", "DRIED",
        "STAINED_GLASS", "GLASS_PANE", "TERRACOTTA", "CONCRETE", "GLAZED",
        "PAINTING", "ITEM_FRAME", "GLOW_ITEM", "MUSIC_DISC", "POT", "SAPLING",
        "CORAL", "LANTERN", "CHAIN", "SCAFFOLDING", "BAMBOO", "SPORE",
        "SHROOMLIGHT", "AMETHYST", "POINTED_DRIPSTONE", "DRIPSTONE",
        "SCULK", "FROGLIGHT", "VERDANT", "PEARLESCENT", "OCULUS",
        "TORCHFLOWER", "PITCHER", "PINK", "BLUE", "RED", "YELLOW", "WHITE",
        "BLACK", "GRAY", "CYAN", "PURPLE", "BROWN", "GREEN", "ORANGE", "MAGENTA",
        "LIGHT_BLUE", "LIME",
    )
    if any(k in name for k in decor_kw):
        if "ORE" not in name and "INGOT" not in name:
            return "decoracao"

    # default: construction blocks
    if name.endswith("_LOG") or name.endswith("_WOOD") or name.endswith("_PLANKS") or name.endswith("_STEM") or name.endswith("_HYPHAE"):
        return "construcao"
    if any(k in name for k in ("STONE", "COBBLE", "DIRT", "SAND", "GRAVEL", "CLAY", "MUD", "BRICK", "SLAB", "STAIRS", "WALL", "FENCE", "DOOR", "TRAPDOOR", "GATE", "LEAVES", "ICE", "SNOW", "OBSIDIAN", "BASALT", "DEEPSLATE", "TUFF", "CALCITE", "DRIPSTONE", "NYLIUM", "ROOT", "SOUL", "BLACKSTONE", "END_STONE", "PURPUR", "PRISMARINE", "QUARTZ_BLOCK")):
        return "construcao"

    # remaining items -> utilidades
    return "utilidades"


def main():
    materials = load_materials()
    cats: OrderedDict[str, OrderedDict[str, int]] = OrderedDict((c, OrderedDict()) for c in CATEGORIES)

    seen = set()
    for name in sorted(materials):
        if should_skip(name):
            continue
        cat = categorize(name)
        if cat is None:
            continue
        if name in seen:
            continue
        seen.add(name)
        p = price_for(name, cat)
        cats[cat][name] = p

    out = []
    out.append("# MineSpace SMP Shop — gerado por scripts/generate_shop_catalog.py")
    out.append("# Colar em OBM-SMP/config.yml sob shop:")
    out.append("shop:")
    out.append("  blocked-items:")
    for b in sorted(BLOCKED_SHOP):
        out.append(f"    - {b}")
    out.append("  categories:")
    for cat in CATEGORIES:
        items = cats[cat]
        if not items:
            continue
        out.append(f"    {cat}:")
        for mat, price in items.items():
            out.append(f"      {mat}: {price}")

    total = sum(len(v) for v in cats.values())
    out.append(f"# Total items: {total}")
    import pathlib
    resources = pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "resources"
    catalog_path = resources / "shop-catalog.yml"
    catalog_path.write_text("\n".join(out) + "\n", encoding="utf-8")

    config_path = resources / "config.yml"
    config_text = config_path.read_text(encoding="utf-8")
    shop_start = config_text.find("\nshop:")
    if shop_start < 0:
        shop_start = config_text.find("shop:")
    if shop_start >= 0:
        prefix = config_text[:shop_start].rstrip() + "\n\n"
    else:
        prefix = config_text.rstrip() + "\n\n"
    config_path.write_text(prefix + "\n".join(out) + "\n", encoding="utf-8")

    deploy = pathlib.Path(__file__).resolve().parents[2] / "PluginsBaixadosDoExaraton" / "OBM-SMP" / "config.yml"
    if deploy.parent.exists():
        deploy_text = deploy.read_text(encoding="utf-8")
        d_start = deploy_text.find("\nshop:")
        if d_start < 0:
            d_start = deploy_text.find("shop:")
        deploy_prefix = deploy_text[:d_start].rstrip() + "\n" if d_start >= 0 else prefix
        deploy.write_text(deploy_prefix + "\n".join(out) + "\n", encoding="utf-8")
        print(f"Updated {deploy}")

    for cat, items in cats.items():
        print(f"  {cat}: {len(items)}")
    print(f"Wrote {catalog_path} ({total} items)")
    print(f"Merged shop into {config_path}")


if __name__ == "__main__":
    main()
