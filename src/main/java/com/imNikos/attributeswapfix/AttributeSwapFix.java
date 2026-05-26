package com.imNikos.attributeswapfix;

import org.bukkit.plugin.java.JavaPlugin;

public final class AttributeSwapFix extends JavaPlugin {

    @Override
    public void onEnable() {
        // Step 1: flip Paper's config flag off at runtime
        boolean configPatched = PaperConfigPatch.disableEquipmentUpdateOnPlayerActions(this);

        // Step 2: register the cooldown-preservation listener
        getServer().getPluginManager().registerEvents(new CooldownPreserveListener(this), this);

        if (configPatched) {
            getLogger().info("AttributeSwapFix enabled. Paper's update-equipment-on-player-actions patched. Vanilla attribute swapping restored.");
        } else {
            getLogger().warning("AttributeSwapFix: Could not patch Paper config via reflection. Attempting fallback cooldown-preservation only.");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("AttributeSwapFix disabled.");
    }
}