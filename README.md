<!-- BANNER -->
<div align="center">

![AttributeSwapFix Banner](https://placehold.co/860x180/0d1117/58a6ff?text=AttributeSwapFix&font=montserrat)

# Attribute Swap Fix

**Restore vanilla attribute swapping on Paper servers — stun slams, spear lunges, and all.**

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.x-brightgreen?style=flat-square&logo=minecraft&logoColor=white)](https://minecraft.net)
[![Paper](https://img.shields.io/badge/Server-Paper-blue?style=flat-square)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-LGPL--3.0-lightgrey?style=flat-square)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.0.0-purple?style=flat-square)]()

</div>

---

## What is Attribute Swapping?

Attribute swapping is a **vanilla Minecraft mechanic** — tracked as [MC-28289](https://bugs.mojang.com/browse/MC-28289) — where swapping your held item within the same tick as an attack causes the server to resolve damage using attributes from *both* items simultaneously. This enables a class of advanced combat techniques:


---

## Why Does Paper Break This?

Paper adds two anti-exploit patches that together kill the attribute swap window:

1. **`update-equipment-on-player-actions`** — When `true` (Paper's default), Paper forces a full equipment recalculation the instant it receives the client's item-swap packet, *before* the attack resolves. The attribute window closes before it ever opens.

2. **Attack cooldown reset** — Paper also resets the attack strength ticker when processing hotbar swap packets, meaning even if you disable the above, your cooldown resets on every swap and the charged-attack window is lost.

AttributeSwapFix undoes both patches at runtime, restoring the original vanilla behaviour.

---

## How It Works

AttributeSwapFix does not reimplement attribute swapping. It does not add new mechanics. It **removes two Paper-specific restrictions** and gets out of the way:

### 1. Config Patch (Reflection)
On startup, the plugin uses Java reflection to reach Paper's live `GlobalConfiguration` singleton and sets `unsupportedSettings.updateEquipmentOnPlayerActions` to `false` at runtime — no need to manually edit `paper-global.yml`, no restart loop required.

```
[AttributeSwapFix] update-equipment-on-player-actions was: true
[AttributeSwapFix] update-equipment-on-player-actions is now: false
[AttributeSwapFix] Vanilla attribute swapping restored.
```

### 2. Cooldown Preservation (NMS)
A lightweight event listener captures each player's `attackStrengthTicker` value just before a hotbar swap event fires, then restores it on the next tick via the server scheduler. This prevents Paper's cooldown reset from collapsing the charge window that vanilla combat depends on.

### What This Plugin Does NOT Do
- ❌ Does not touch damage values
- ❌ Does not add custom attributes
- ❌ Does not change enchantment behaviour
- ❌ Does not affect non-attribute-swap combat in any way


---

## Requirements

| Requirement | Version |
|---|---|
| Minecraft Java Edition | 26.1.x (Paper build) |
| Server Software | [Paper](https://papermc.io/downloads/paper) |
| Java | 21 or higher |
| Maven (to build from source) | 3.8+ |

---

## Installation

### Option A — Drop in the JAR *(recommended)*

1. Download `AttributeSwapFix-1.0.0.jar` from the [Releases](../../releases) page.
2. Drop it into your server's `plugins/` folder.
3. **Add the JVM flag** to your startup script (see below — this is required).
4. Start or restart your server.

### Option B — Build from Source

```bash
git clone https://github.com/imNikos/AttributeSwapFix.git
cd AttributeSwapFix
mvn clean package
# Output: target/AttributeSwapFix-1.0.0.jar
```

Requires Java 21 and Maven 3.8+. In Eclipse: right-click the project → **Run As → Maven build...** → Goals: `clean package`.

---

## ⚠️ Required JVM Flag

> **This step is mandatory.** Without it, Java 21+ will block the reflection this plugin uses and it will not work.

Open your server startup script (`start.bat` or `start.sh`) and add `--add-opens java.base/java.lang=ALL-UNNAMED` to your java arguments.

**Example `start.sh`:**
```bash
java \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  -Xms4G -Xmx4G \
  -jar paper-26.1.2.jar nogui
```

**Example `start.bat`:**
```bat
java --add-opens java.base/java.lang=ALL-UNNAMED -Xms4G -Xmx4G -jar paper-26.1.2.jar nogui
```

If you use [Aikar's flags](https://docs.papermc.io/paper/aikars-flags), just prepend it before `-jar`:

```bash
java --add-opens java.base/java.lang=ALL-UNNAMED -Xms10G -Xmx10G \
  -XX:+UseG1GC ... -jar paper-26.1.2.jar nogui
```

---

## Verifying It Works

After startup, check your console for these lines:

```
[AttributeSwapFix] update-equipment-on-player-actions was: true
[AttributeSwapFix] update-equipment-on-player-actions is now: false
[AttributeSwapFix] AttributeSwapFix enabled. Paper's update-equipment-on-player-actions patched. Vanilla attribute swapping restored.
```

If you see a warning instead:
```
[AttributeSwapFix] Could not patch Paper config via reflection. Attempting fallback cooldown-preservation only.
```
...double-check that the `--add-opens` JVM flag is present in your startup script and that you are running a Paper-based server (not Spigot, Purpur, etc. — though Purpur should also work as it is Paper-based).

---

## Compatibility

| Software | Status |
|---|---|
| Paper 26.1.x | ✅ Fully supported |
| Purpur (26.1.x base) | ✅ Should work (Paper-based) |
| Folia | ⚠️ Untested |
| Spigot | ❌ Not supported (different internals) |
| Fabric / Forge | ❌ Not applicable |

---

## Project Structure

```
AttributeSwapFix/
├── src/main/java/com/imNikos/attributeswapfix/
│   ├── AttributeSwapFix.java          # Plugin entry point
│   ├── PaperConfigPatch.java          # Reflection patch for update-equipment-on-player-actions
│   └── CooldownPreserveListener.java  # NMS cooldown ticker preservation
├── src/main/resources/
│   └── plugin.yml
└── pom.xml
```

---

## FAQ

**Q: Will this get my server flagged by anticheat plugins like Grim?**
A: Grim and similar plugins may flag players performing attribute swaps — that's a separate concern between your anticheat config and your players. This plugin restores the mechanic serverside; whether you police it is up to you.

**Q: Does this affect PvE as well as PvP?**
A: Yes. Attribute swapping works on any entity, not just players. Mobs can't perform it (they don't swap items), but players can use it against mobs freely.

**Q: Will this break on the next Paper update?**
A: Possibly, if Paper renames the internal config field this plugin patches via reflection. If it stops working after a Paper update, check the [Issues](../../issues) page — a fix will be posted there.

**Q: Do players need to do anything?**
A: No. Once the plugin is loaded and the JVM flag is set, attribute swapping works for all players automatically.

---

## License

[LGPL-3.0](LICENSE) — you may use, modify, and distribute this plugin freely, including in commercial server networks, as long as modifications to this plugin itself are shared under the same license.

---

<div align="center">

Made by imNikos for Paper servers.
**[Report an Issue](../../issues) · [View Releases](../../releases)**

</div>
