package com.imNikos.attributeswapfix;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Preserves the attack cooldown ticker across hotbar item swaps.
 *
 * Paper's ServerGamePacketListenerImpl resets the attack cooldown when it
 * processes a ServerboundSetCarriedItemPacket. This listener captures the
 * cooldown value via NMS reflection immediately before the swap is
 * processed, then restores it on the tick after — keeping the player's
 * charged-attack window intact so the vanilla attribute swap can fire.
 */
public class CooldownPreserveListener implements Listener {

    private final JavaPlugin plugin;
    // UUID -> attack cooldown value saved just before the swap
    private final Map<UUID, Float> savedCooldowns = new HashMap<>();

    public CooldownPreserveListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        // Save the current attack cooldown via NMS before the swap takes effect
        float cooldown = getAttackCooldown(player);
        if (cooldown > 0.0f) {
            savedCooldowns.put(player.getUniqueId(), cooldown);

            // Restore it on the very next tick (after Paper has finished
            // processing the packet and potentially resetting the cooldown)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Float saved = savedCooldowns.remove(player.getUniqueId());
                if (saved != null && player.isOnline()) {
                    setAttackCooldown(player, saved);
                }
            }, 1L);
        }
    }

    // -----------------------------------------------------------------------
    //  NMS helpers — read/write the attack cooldown (attackStrengthTicker)
    //  from the EntityPlayer / ServerPlayer object
    // -----------------------------------------------------------------------

    private float getAttackCooldown(Player player) {
        try {
            Object nmsPlayer = getNMSPlayer(player);
            if (nmsPlayer == null) return 0f;

            // getAttackStrengthScale(0f) returns 0..1 representing charge fraction
            Method m = findMethod(nmsPlayer.getClass(), "getAttackStrengthScale", float.class);
            if (m != null) {
                m.setAccessible(true);
                return (float) m.invoke(nmsPlayer, 0f);
            }
        } catch (Exception e) {
            // Non-fatal — fall through and return 0
        }
        return 0f;
    }

    private void setAttackCooldown(Player player, float value) {
        try {
            Object nmsPlayer = getNMSPlayer(player);
            if (nmsPlayer == null) return;

            // attackStrengthTicker is the raw int ticker (max = getAttackStrengthTickMax())
            // We need to set it to: round(value * maxTick)
            // First get the max tick value
            Method maxMethod = findMethod(nmsPlayer.getClass(), "getCurrentItemAttackStrengthDelay");
            if (maxMethod == null) {
                // fallback field name used in some builds
                maxMethod = findMethod(nmsPlayer.getClass(), "getAttackStrengthTickMax");
            }

            int maxTick = 20; // safe default (1 second)
            if (maxMethod != null) {
                maxMethod.setAccessible(true);
                Object result = maxMethod.invoke(nmsPlayer);
                if (result instanceof Integer i) maxTick = i;
                else if (result instanceof Number n) maxTick = n.intValue();
            }

            int tickerValue = Math.round(value * maxTick);

            Field tickerField = findField(nmsPlayer.getClass(), "attackStrengthTicker");
            if (tickerField != null) {
                tickerField.setAccessible(true);
                tickerField.set(nmsPlayer, tickerValue);
            }
        } catch (Exception e) {
            // Non-fatal
        }
    }

    private Object getNMSPlayer(Player player) {
        try {
            Method getHandle = player.getClass().getMethod("getHandle");
            return getHandle.invoke(player);
        } catch (Exception e) {
            return null;
        }
    }

    private Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException ignored) {}
            c = c.getSuperclass();
        }
        return null;
    }

    private Field findField(Class<?> clazz, String name) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {}
            c = c.getSuperclass();
        }
        return null;
    }
}