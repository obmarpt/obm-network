#!/usr/bin/env python3
"""
Gera sell.prices a partir de shop-catalog.yml com multiplicadores por categoria e anti-farm.
Saída: OBM-SMP/src/main/resources/sell-catalog.yml
"""
from __future__ import annotations

import math
import pathlib
import re
from collections import OrderedDict

ROOT = pathlib.Path(__file__).resolve().parent.parent
SHOP_PATH = ROOT / "src" / "main" / "resources" / "shop-catalog.yml"
OUT_PATH = ROOT / "src" / "main" / "resources" / "sell-catalog.yml"

# Multiplicadores por categoria da shop (sell = shop * mult)
CATEGORY_MULTIPLIER = {
    "mineracao": 0.45,
    "combate": 0.40,
    "armaduras": 0.40,
    "utilidades": 0.35,
    "armazenamento": 0.35,
    "redstone": 0.35,
    "pocoes": 0.35,
    "construcao": 0.25,
    "decoracao": 0.20,
    "comida": 0.20,
    "raros": 0.20,
}

# Anti-farm (sobrepõe categoria)
ANTI_FARM_CROPS = {
    "WHEAT", "CARROT", "POTATO", "BEETROOT",
    "WHEAT_SEEDS", "BEETROOT_SEEDS", "MELON_SEEDS", "PUMPKIN_SEEDS",
    "POTATOES",  # typo guard
}
ANTI_FARM_AUTO = {
    "KELP", "DRIED_KELP", "BAMBOO", "SUGAR_CANE", "CACTUS",
    "BAMBOO_BLOCK", "KELP_BLOCK", "DRIED_KELP_BLOCK",
}
ANTI_FARM_MOB = {
    "ROTTEN_FLESH", "STRING", "BONE", "GUNPOWDER",
}
ANTI_FARM_PASSIVE = {
    "WOOL", "LEATHER",
}
ANTI_FARM_MULT = {
    "crops": 0.15,
    "auto": 0.10,
    "mob": 0.15,
    "passive": 0.15,
}

# Não vendáveis (sell = 0 → omitidos do YAML)
FORBIDDEN_SELL = {
    "BEACON", "NETHER_STAR", "TOTEM_OF_UNDYING", "END_CRYSTAL", "ELYTRA",
    "DRAGON_EGG", "NETHERITE_INGOT", "NETHERITE_BLOCK", "NETHERITE_SCRAP",
    "COMMAND_BLOCK", "BARRIER", "STRUCTURE_VOID",
}

CATEGORY_LABEL = {
    "mineracao": "MINERACAO 45%",
    "combate": "COMBATE 40%",
    "armaduras": "ARMADURAS 40%",
    "utilidades": "UTILIDADES 35%",
    "armazenamento": "ARMAZENAMENTO 35%",
    "redstone": "REDSTONE 35%",
    "pocoes": "POCOES 35%",
    "construcao": "CONSTRUCAO 25%",
    "decoracao": "DECORACAO 20%",
    "comida": "COMIDA 20%",
    "raros": "RAROS 20%",
}

ANTI_LABEL = {
    "crops": "ANTI-FARM CROPS 15%",
    "auto": "ANTI-FARM AUTO 10%",
    "mob": "ANTI-FARM MOB 15%",
    "passive": "ANTI-FARM PASSIVE 15%",
}


def parse_shop_catalog(path: pathlib.Path) -> tuple[set[str], OrderedDict[str, dict[str, int]]]:
    text = path.read_text(encoding="utf-8")
    blocked: set[str] = set()
    in_blocked = False
    for line in text.splitlines():
        s = line.strip()
        if s.startswith("blocked-items:"):
            in_blocked = True
            continue
        if in_blocked and s.startswith("- "):
            blocked.add(s[2:].strip())
        elif in_blocked and s.startswith("categories:"):
            in_blocked = False

    categories: OrderedDict[str, dict[str, int]] = OrderedDict()
    current_cat: str | None = None
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        if stripped == "shop:":
            continue
        if stripped.startswith("blocked-items:") or stripped.startswith("categories:"):
            current_cat = None
            continue
        if stripped.startswith("vip-items:"):
            current_cat = None
            continue
        m_item = re.match(r"^([A-Z0-9_]+):\s*(\d+)\s*$", stripped)
        if m_item and current_cat:
            categories[current_cat][m_item.group(1)] = int(m_item.group(2))
            continue
        m_cat = re.match(r"^([a-z]+):\s*$", stripped)
        if m_cat and stripped not in ("shop", "blocked-items", "categories", "vip-items"):
            current_cat = m_cat.group(1)
            categories.setdefault(current_cat, {})

    return blocked, categories


def anti_farm_kind(material: str) -> str | None:
    if material in ANTI_FARM_AUTO:
        return "auto"
    if material in ANTI_FARM_CROPS:
        return "crops"
    if material in ANTI_FARM_MOB:
        return "mob"
    if material in ANTI_FARM_PASSIVE:
        return "passive"
    if material.endswith("_WOOL") or material == "WOOL":
        return "passive"
    return None


def compute_sell(shop_price: int, mult: float) -> int:
    if shop_price <= 0:
        return 0
    raw = int(round(shop_price * mult))
    if raw <= 0:
        return 0
    cap = shop_price - 1
    if cap <= 0:
        return 0
    return min(raw, cap)


def main() -> None:
    if not SHOP_PATH.exists():
        raise SystemExit(f"Missing {SHOP_PATH}")

    shop_blocked, categories = parse_shop_catalog(SHOP_PATH)
    all_blocked = shop_blocked | FORBIDDEN_SELL

    # bucket: (category, anti_kind or None) -> list of (mat, shop, sell)
    buckets: OrderedDict[tuple[str, str | None], list[tuple[str, int, int]]] = OrderedDict()

    stats = {"total": 0, "skipped_blocked": 0, "skipped_zero": 0, "anti_farm": 0}

    for cat, items in categories.items():
        base_mult = CATEGORY_MULTIPLIER.get(cat, 0.25)
        for material, shop_price in sorted(items.items()):
            stats["total"] += 1
            if material in all_blocked:
                stats["skipped_blocked"] += 1
                continue

            farm = anti_farm_kind(material)
            if farm:
                mult = ANTI_FARM_MULT[farm]
                stats["anti_farm"] += 1
                key = (cat, farm)
            else:
                mult = base_mult
                key = (cat, None)

            sell = compute_sell(shop_price, mult)
            if sell <= 0:
                stats["skipped_zero"] += 1
                continue

            buckets.setdefault(key, []).append((material, shop_price, sell))

    lines: list[str] = []
    lines.append("# MineSpace SMP — sell prices (gerado por scripts/generate_sell_catalog.py)")
    lines.append("# Regra: sell < shop | anti-farm aplicado | blocked-items ignorados")
    lines.append("# Regenerar após alterar shop-catalog.yml: python scripts/generate_sell_catalog.py")
    lines.append("sell:")
    lines.append("  prices:")

    # Ordem: categorias normais, depois blocos anti-farm destacados
    cat_order = list(CATEGORY_MULTIPLIER.keys())
    for cat in cat_order:
        label = CATEGORY_LABEL.get(cat, cat.upper())
        block = buckets.get((cat, None))
        if block:
            lines.append(f"    # --- {label} ---")
            for mat, shop_p, sell_p in sorted(block, key=lambda x: x[0]):
                lines.append(f"    {mat}: {sell_p}  # shop {shop_p}")

    for farm_key in ("crops", "auto", "mob", "passive"):
        lines.append(f"    # === {ANTI_LABEL[farm_key]} ===")
        for cat in cat_order:
            block = buckets.get((cat, farm_key))
            if not block:
                continue
            lines.append(f"    # [{cat}] {ANTI_LABEL[farm_key]}")
            for mat, shop_p, sell_p in sorted(block, key=lambda x: x[0]):
                lines.append(f"    {mat}: {sell_p}  # shop {shop_p} [ANTI-FARM]")

    sell_count = sum(len(v) for v in buckets.values())
    lines.append(f"# Total sellable: {sell_count} | shop items scanned: {stats['total']}")
    lines.append(f"# Blocked: {stats['skipped_blocked']} | Anti-farm: {stats['anti_farm']} | Zero sell: {stats['skipped_zero']}")

    OUT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {OUT_PATH} ({sell_count} prices)")
    print(f"  blocked skipped: {stats['skipped_blocked']}")
    print(f"  anti-farm items: {stats['anti_farm']}")


if __name__ == "__main__":
    main()
