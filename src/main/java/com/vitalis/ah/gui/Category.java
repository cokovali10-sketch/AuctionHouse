package com.vitalis.ah.gui;

import org.bukkit.Material;

/** Listing categories with GUI icons. Matcher is intentionally simple and fast. */
public enum Category {
    ALL("All", Material.CHEST, "Все лоты"),
    BLOCKS("Blocks", Material.DIRT, "Блоки"),
    TOOLS("Tools", Material.IRON_PICKAXE, "Инструменты"),
    WEAPONS("Weapons", Material.DIAMOND_SWORD, "Оружие"),
    ARMOR("Armor", Material.DIAMOND_CHESTPLATE, "Броня"),
    FOOD("Food", Material.COOKED_BEEF, "Еда"),
    POTIONS("Potions", Material.POTION, "Зелья"),
    ENCHANTED("Enchanted", Material.ENCHANTING_TABLE, "Зачарованное"),
    MISC("Misc", Material.BARREL, "Разное");

    public final String id;
    public final Material icon;
    public final String nameRu;

    Category(String id, Material icon, String nameRu) {
        this.id = id;
        this.icon = icon;
        this.nameRu = nameRu;
    }

    public static Category parse(String s) {
        if (s == null) return ALL;
        try {
            return Category.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }

    /** Does this material belong to the category? hasEnchants = item has any enchantments. */
    public boolean matches(Material mat, boolean hasEnchants) {
        return switch (this) {
            case ALL -> true;
            case ENCHANTED -> hasEnchants;
            case BLOCKS -> mat.isBlock();
            case FOOD -> mat.isEdible();
            case POTIONS -> mat == Material.POTION || mat == Material.SPLASH_POTION
                    || mat == Material.LINGERING_POTION || mat == Material.TIPPED_ARROW;
            case WEAPONS -> {
                String n = mat.name();
                yield n.endsWith("_SWORD") || n.endsWith("_AXE") || n.equals("BOW")
                        || n.equals("CROSSBOW") || n.equals("TRIDENT") || n.equals("MACE")
                        || n.equals("SHIELD");
            }
            case TOOLS -> isTool(mat);
            case ARMOR -> {
                String n = mat.name();
                yield n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
                        || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")
                        || n.equals("ELYTRA") || n.equals("TURTLE_HELMET");
            }
            case MISC -> !BLOCKS.matches(mat, hasEnchants) && !TOOLS.matches(mat, hasEnchants)
                    && !WEAPONS.matches(mat, hasEnchants) && !ARMOR.matches(mat, hasEnchants)
                    && !FOOD.matches(mat, hasEnchants) && !POTIONS.matches(mat, hasEnchants);
        };
    }

    private static boolean isTool(Material mat) {
        String n = mat.name();
        return n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_SHOVEL")
                || n.endsWith("_HOE") || n.equals("SHEARS") || n.equals("FISHING_ROD")
                || n.equals("FLINT_AND_STEEL") || n.equals("BRUSH") || n.equals("COMPASS")
                || n.equals("CLOCK");
    }
}
