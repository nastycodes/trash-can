# Trash Can

[🇩🇪 Deutsche Version](README.de.md)

Fabric mod for **Minecraft 1.21.11** (Fabric Loader 0.20.1+, Fabric API). Adds a **trash can**: it has the same
interface as a chest (27 slots) plus a **Delete** button that destroys everything inside for good.

## Installation

Put `trashcan-1.0.0.jar` and [Fabric API](https://modrinth.com/mod/fabric-api) into the `mods` folder – on the
server **and** on every client. [Mod Menu](https://modrinth.com/mod/modmenu) is optional and adds the in-game
settings screen.

## How to play

1. Craft the trash can:

   ```
   Iron Ingot   –            Iron Ingot
   Iron Ingot   Chest        Iron Ingot
   Iron Ingot   Iron Ingot   Iron Ingot
   ```

   (Creative: "Functional Blocks" tab, right after the barrel.)
2. Right-click to open it and put items in – exactly like a chest.
3. Click **Delete** in the top right corner → the button turns into a red **Sure?**. A second click within 3 seconds
   deletes the entire contents for good. Afterwards the button is greyed out until something is inside again.

### Details

- **Nothing** is lost until you click "Delete": the contents are stored like in a chest, across restarts too. If you
  put something in by mistake, just take it out again.
- Only the trash can's contents are deleted – never your own inventory or the item on your cursor.
- **Hoppers** can move items in (and out), **comparators** read how full it is.
- When the trash can is mined (pickaxe), its contents drop like a chest's.
- It can be renamed in an anvil; the name then shows as the title of its GUI.
- The lid opens while somebody looks inside.
- Every deletion is written to the server log, e.g. `Steve deleted 81 items in the trash can at 10, 64, -3`.

### Delete when closed

With the option **Delete when closed** the trash can empties itself as soon as the last player closes it – handy
for throwing junk away quickly. Hoppers can still fill it (e.g. a hopper line from a mob farm), and nothing is
deleted while somebody has it open.

## Settings

With Mod Menu: **Mods → Trash Can → settings button**. Every option has a tooltip, a reset button and is checked
before saving; changes apply immediately. Without Mod Menu, edit `config/trashcan.json` (created on first start) and
run `/trashcan reload` (operator level 2).

The settings are read in two places:

- **Delete button** (`requireConfirmation`, `confirmSeconds`): read by each player's own game, so every player
  decides for themselves – also on servers.
- **Deleting** (`deleteOnClose`, `deleteEffects`, `logDeletions`): read by the world – your own settings in
  singleplayer and LAN worlds, the server's `config/trashcan.json` on a dedicated server.

| Key | Default | Meaning |
|---|---|---|
| `requireConfirmation` | true | the first click only arms the button ("Sure?"), a second click deletes; off: the first click deletes |
| `confirmSeconds` | 3 | how long "Sure?" waits for the second click (1–30) |
| `deleteOnClose` | false | delete the contents automatically when the last player closes the can |
| `deleteEffects` | true | smoke and a hiss when a trash can is emptied |
| `logDeletions` | true | write every deletion to the log (player, item count, position) |

## Commands

| Command | Permission | Effect |
|---|---|---|
| `/trashcan reload` | operator level 2 | reloads `config/trashcan.json` |

## Languages

English and German (Deutsch). The game language decides; the config screen is translated too.

## Development

```
gradlew build              # builds build/libs/trashcan-1.0.0.jar and runs the translation check
gradlew runClient          # test client (with Mod Menu)
gradlew runClientGameTest  # end-to-end test in a real client, screenshots in build/run/clientGameTest/screenshots
python tools/gen_textures.py .   # regenerates the textures and the icon
```

The client game test (English, survival, 1920×1080) opens the trash can with a right click, moves items in, deletes
them with both clicks and checks on the server that everything is gone. Also: an expired confirmation does not
delete, the lid opens/closes, saving/loading, mining drops the contents, the comparator signal, "ask before
deleting" off (one click deletes), "delete when closed" (keeps items while closed and open, deletes on closing), and
the config screen behind Mod Menu's button including invalid input and the "discard changes?" prompt.

## License

[MIT](LICENSE)
