# EliteEssentials

A server-side essentials plugin for Hytale that provides essential teleportation and utility commands for survival multiplayer servers.

## Features

### Home System
- **`/sethome [name]`** - Save named home locations (default: "home")
- **`/home [name]`** - Teleport to your saved homes
- **`/delhome <name>`** - Delete a home
- **`/homes`** - List all your homes
- Configurable max homes per player

### Back Command
- **`/back`** - Return to your previous location
- Remembers multiple locations (configurable history)
- Works on death - return to where you died (configurable)

### Random Teleport
- **`/rtp`** - Teleport to a random location in the world
- Safe landing detection (avoids spawning underground)
- Configurable min/max range, cooldown, and warmup

### Teleport Requests
- **`/tpa <player>`** - Request to teleport to another player
- **`/tpaccept`** - Accept a teleport request
- **`/tpdeny`** - Deny a teleport request
- Clickable ACCEPT / DENY buttons in chat
- 30-second timeout (configurable)

### Spawn
- **`/spawn`** - Teleport to the world spawn point

### Sleep Percentage (Admin)
- **`/sleeppercent <0-100>`** - Set percentage of players needed to skip the night

## Configuration

All settings are fully configurable via `mods/EliteEssentials/config.json`:

- Enable/disable any command
- Cooldowns - prevent command spam
- Warmups - require players to stand still before teleporting
- RTP range - set min/max teleport distance
- Home limits - max homes per player
- Back history - how many locations to remember
- Death tracking - enable/disable /back to death location

Config file is automatically created on first server start with sensible defaults.

## Known Issues

Currently being worked on:
- Warmup not detecting movement
- Sleep percentage not fully working

## Building

```bash
# Build the plugin
gradlew.bat shadowJar

# Output: build/libs/EliteEssentials-1.0.0.jar
```

## Requirements

- Java 21+
- Hytale Server

## License

MIT
