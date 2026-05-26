package com.imNikos.attributeswapfix;

import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Uses reflection to disable Paper's "update-equipment-on-player-actions" flag
 * at runtime, restoring vanilla attribute swapping behaviour.
 *
 * Paper stores this flag in io.papermc.paper.configuration.GlobalConfiguration
 * under the path:
 *   unsupportedSettings.updateEquipmentOnPlayerActions (boolean, default true)
 *
 * Setting it to false tells Paper's ServerGamePacketListenerImpl to skip the
 * forced equipment refresh that normally fires when a player sends a
 * ServerboundSetCarriedItemPacket (hotbar swap) or
 * ServerboundPlayerActionPacket, which would otherwise clear the attribute
 * swap window before the attack damage is calculated.
 */
public class PaperConfigPatch {

    private PaperConfigPatch() {}

    public static boolean disableEquipmentUpdateOnPlayerActions(JavaPlugin plugin) {
        Logger log = plugin.getLogger();

        try {
            // 1. Grab the GlobalConfiguration singleton via its static get() method.
            //    Class name: io.papermc.paper.configuration.GlobalConfiguration
            Class<?> globalConfigClass = Class.forName(
                    "io.papermc.paper.configuration.GlobalConfiguration");

            Method getMethod = globalConfigClass.getMethod("get");
            Object globalConfig = getMethod.invoke(null);

            // 2. Navigate to the "unsupportedSettings" inner config node.
            //    Field name in Paper source: public UnsupportedSettings unsupportedSettings
            Field unsupportedSettingsField = globalConfigClass.getField("unsupportedSettings");
            Object unsupportedSettings = unsupportedSettingsField.get(globalConfig);

            // 3. Find and set "updateEquipmentOnPlayerActions" to false.
            //    Field name: public boolean updateEquipmentOnPlayerActions
            Field updateEquipField = unsupportedSettings.getClass()
                    .getField("updateEquipmentOnPlayerActions");
            updateEquipField.setAccessible(true);

            boolean current = (boolean) updateEquipField.get(unsupportedSettings);
            log.info("update-equipment-on-player-actions was: " + current);

            updateEquipField.set(unsupportedSettings, false);

            boolean after = (boolean) updateEquipField.get(unsupportedSettings);
            log.info("update-equipment-on-player-actions is now: " + after);

            return !after; // true = successfully set to false

        } catch (ClassNotFoundException e) {
            log.severe("Could not find GlobalConfiguration class. Is this a Paper server?");
            log.severe("Exception: " + e.getMessage());
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            log.severe("Paper internals changed — field/method not found: " + e.getMessage());
            log.severe("This build of AttributeSwapFix may need updating for your Paper version.");
        } catch (Exception e) {
            log.severe("Reflection error patching Paper config: " + e.getClass().getName()
                    + ": " + e.getMessage());
        }
        return false;
    }
}