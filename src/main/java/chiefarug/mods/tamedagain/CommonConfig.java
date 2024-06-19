package chiefarug.mods.tamedagain;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfig {
    private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue enableStaff = builder //TODO: implement this
            .comment("If all items in the tamedagain:staff item tag should immediately tame.",
                    "To completely remove the staff from the game use tamedagain-startup.json")
            .define("enable_staff", true);
    public static final ForgeConfigSpec.BooleanValue useTranslations = builder
            .comment("If translation keys should be used for displaying sit and follow action bar text.",
                    "Disable this if you are using it on a server without it being required client side",
                    "and are not providing a resource pack containing the required lang keys.")
            .define("use_translations", true);
    public static final ForgeConfigSpec.DoubleValue staffXpCost = builder
            .comment("This number is multiplied by the health the entity has and then that many",
                    "levels (rounded up) are deducted from the player when they tame the entity",
                    "using anything in the tamedagain:staff tag.",
                    "Obviously does nothing if the staff is disabled.")
            .defineInRange("staff_xp_cost", 0.1, 0, 1);
    public static final ForgeConfigSpec.BooleanValue staffXpCostUseCurrentHealth = builder
            .comment("If true, the staff xp cost is calculated using the entities current health",
                    "instead of its maximum health. This means that mobs with a lot of health",
                    "like the Warden can be damaged to reduce the xp cost required to tame",
                    "with the staff/equivalently tagged item")
            .define("staff_xp_cost_use_max_health", false);
    static final ForgeConfigSpec spec = builder.build();
}
