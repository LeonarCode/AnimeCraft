# AnimeCraft

A Minecraft (Fabric) mod inspired by shonen anime — original items, abilities, and (soon) mobs built around five power "styles," each echoing a different series without copying its characters or names directly.

> Built by Leonard as a personal learning/modding project.

## Tech Stack

| Component       | Version        |
|-----------------|----------------|
| Minecraft       | 26.3           |
| Mod Loader      | Fabric Loader 0.19.5 |
| Fabric API      | 0.163.0+26.3   |
| Loom            | 1.17           |
| Java            | 25             |
| Mappings        | Mojang (unobfuscated, per Minecraft 26.x) |

## Power Styles

Five original "styles," each loosely inspired by a different anime, with its own energy type:

| Style         | Inspired by     | Energy          |
|---------------|------------------|------------------|
| Shinobi Arts  | Naruto           | Chakra           |
| Breathing Styles | Demon Slayer  | Stamina          |
| Soul Reaper   | Bleach           | Spirit Pressure  |
| Guild Magic   | Fairy Tail       | Mana             |
| Titan Shifter | Attack on Titan  | Titan Power      |

Each style has a starter ability defined in `AbilityRegistry` (energy cost + cooldown), though abilities aren't wired to real gameplay effects yet — see **Roadmap**.

## Features So Far

### Items

- **Twin Fang Kunai** (`twin_fang_kunai`) — Shinobi Arts. Right-click to throw; on impact, teleports the thrower to the hit location (Flying Thunder God-style warp), with particles + sound.
- **Vertical Gear** (`vertical_gear`) — Titan Shifter. Right-click while aiming at a block within 30 blocks to grapple toward it. Pulls the player continuously (not an instant snap) with a streaming particle "cable" effect along the path until close to the target.

### Ability System

- `PowerStyle` enum — the five styles above.
- `Ability` record — id, display name, style, energy cost, cooldown (in ticks).
- `AbilityRegistry` — central registry; `bootstrap()` seeds one starter ability per style.

## Project Structure

```
src/main/java/com/leonard/animecraft/
├── Animecraft.java              # Main mod entrypoint (ModInitializer)
├── ability/
│   ├── PowerStyle.java
│   ├── Ability.java
│   └── AbilityRegistry.java
├── item/
│   ├── ModItems.java            # Item registry
│   ├── KunaiItem.java           # Twin Fang Kunai behavior
│   ├── VerticalGearItem.java    # Vertical Gear behavior
│   └── GrappleEffects.java      # Server-tick grapple pull + cable particles
├── entity/
│   ├── ModEntityTypes.java      # Entity type registry
│   └── KunaiEntity.java         # Thrown kunai projectile
└── client/
    └── AnimecraftClient.java    # Client entrypoint (entity renderers, etc.)

src/main/resources/
├── fabric.mod.json
└── assets/animecraft/
    ├── lang/en_us.json
    ├── items/                   # Client item model definitions
    ├── models/item/             # Item models
    └── textures/item/           # Item textures (placeholder art — see Notes)
```

## Running the Mod

From the project root, in PowerShell:

```powershell
.\gradlew runClient
```

First run takes a while (downloads Minecraft assets + decompiles sources). Subsequent runs are much faster.

To build a distributable jar:

```powershell
.\gradlew build
```

Output lands in `build/libs/`.

## Roadmap

- [ ] Link abilities in `AbilityRegistry` to real in-game effects (energy bar, cooldown HUD, keybinds)
- [ ] More items for the remaining styles (Breathing Styles sword, Soul Reaper blade, Guild Magic item)
- [ ] Custom mobs
- [ ] Replace placeholder textures with real pixel art

## Notes

- Items and abilities use **original names** (e.g. "Twin Fang Kunai," "Vertical Gear") rather than direct character names from the source anime, to keep the project safe to share/publish without trademark issues. The mechanics and vibe are the inspiration — not a 1:1 recreation of any copyrighted character or technique.
- Minecraft 26.3 is new enough that some Fabric API class names/packages differ from older modding tutorials (e.g. `Identifier` instead of `ResourceLocation` in places, `CreativeModeTabEvents` lives under `creativetab.v1`). If you're following along with older guides, expect some naming differences.