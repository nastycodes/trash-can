# Trash Can (Mülleimer)

[🇬🇧 English version](README.md)

Fabric-Mod für **Minecraft 1.21.11** (Fabric Loader 0.20.1+, Fabric API). Fügt einen **Mülleimer** hinzu: Er hat
dieselbe Oberfläche wie eine Truhe (27 Plätze), dazu einen **Löschen**-Button, der alles darin endgültig vernichtet.

## Installation

`trashcan-1.0.0.jar` zusammen mit der [Fabric API](https://modrinth.com/mod/fabric-api) in den `mods`-Ordner legen –
auf dem Server **und** bei jedem Client. [Mod Menu](https://modrinth.com/mod/modmenu) ist optional und bringt den
Einstellungsbildschirm im Spiel.

## Spielen

1. Mülleimer craften:

   ```
   Eisenbarren   –            Eisenbarren
   Eisenbarren   Truhe        Eisenbarren
   Eisenbarren   Eisenbarren  Eisenbarren
   ```

   (Kreativ: Reiter „Funktionsblöcke“, direkt hinter dem Fass.)
2. Mit Rechtsklick öffnen und Items hineinlegen – genau wie bei einer Truhe.
3. **Löschen** oben rechts klicken → der Button wird zu einem roten **Sicher?**. Ein zweiter Klick innerhalb von
   3 Sekunden löscht den gesamten Inhalt endgültig. Danach ist der Button ausgegraut, bis wieder etwas drinliegt.

### Verhalten im Detail

- Bis zum Klick auf „Löschen“ geht **nichts** verloren: Der Inhalt bleibt gespeichert wie in einer Truhe, auch über
  Neustarts. Wer etwas versehentlich hineingelegt hat, kann es wieder herausnehmen.
- Gelöscht wird nur der Inhalt des Mülleimers – nie das eigene Inventar oder das Item am Mauszeiger.
- **Trichter** können Items hinein- (und heraus-)befördern, **Komparatoren** lesen den Füllstand aus.
- Wird der Mülleimer abgebaut (Spitzhacke), fällt sein Inhalt heraus wie bei einer Truhe.
- Lässt sich im Amboss umbenennen; der Name erscheint dann als Titel im GUI.
- Der Deckel klappt auf, solange jemand hineinschaut.
- Jede Löschung wird im Server-Log vermerkt, z. B. `Steve deleted 81 items in the trash can at 10, 64, -3`.

### Beim Schließen löschen

Mit der Option **Beim Schließen löschen** leert sich der Mülleimer von selbst, sobald der letzte Spieler ihn
schließt – praktisch, um Kram schnell loszuwerden. Trichter können ihn weiterhin befüllen (z. B. eine Trichterleitung
aus einer Mobfarm), und solange jemand hineinschaut, wird nichts gelöscht.

## Einstellungen

Mit Mod Menu: **Mods → Mülleimer → Einstellungs-Button**. Jede Option hat einen Tooltip und einen „Standard“-Button,
Eingaben werden vor dem Speichern geprüft, Änderungen gelten sofort. Ohne Mod Menu `config/trashcan.json` bearbeiten
(wird beim ersten Start angelegt) und `/trashcan reload` ausführen (OP-Level 2).

Die Einstellungen werden an zwei Stellen gelesen:

- **Löschen-Button** (`requireConfirmation`, `confirmSeconds`): vom eigenen Spiel jedes Spielers – jeder entscheidet
  das selbst, auch auf Servern.
- **Löschen** (`deleteOnClose`, `deleteEffects`, `logDeletions`): von der Welt – in Einzelspieler- und LAN-Welten
  deine eigenen Einstellungen, auf einem Server die `config/trashcan.json` des Servers.

| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `requireConfirmation` | true | erster Klick macht den Button nur zu „Sicher?“, erst der zweite löscht; aus: der erste Klick löscht |
| `confirmSeconds` | 3 | so lange wartet „Sicher?“ auf den zweiten Klick (1–30) |
| `deleteOnClose` | false | Inhalt automatisch löschen, wenn der letzte Spieler den Mülleimer schließt |
| `deleteEffects` | true | Rauch und Zischen, wenn ein Mülleimer geleert wird |
| `logDeletions` | true | jede Löschung ins Log schreiben (Spieler, Anzahl, Position) |

## Befehle

| Befehl | Berechtigung | Wirkung |
|---|---|---|
| `/trashcan reload` | OP-Level 2 | lädt `config/trashcan.json` neu |

## Sprachen

Englisch und Deutsch. Es gilt die Spielsprache; auch der Einstellungsbildschirm ist übersetzt.

## Entwickeln

```
gradlew build              # baut build/libs/trashcan-1.0.0.jar, inkl. Übersetzungsprüfung
gradlew runClient          # Testclient (mit Mod Menu)
gradlew runClientGameTest  # End-to-End-Test im echten Client, Screenshots in build/run/clientGameTest/screenshots
python tools/gen_textures.py .   # erzeugt die Texturen und das Icon neu
```

`checkTranslations` (Teil von `gradlew build`) bricht ab, wenn `en_us.json` und `de_de.json` nicht dieselben
Schlüssel haben. Der Client-Gametest läuft auf Englisch in 1920×1080. Er öffnet den Mülleimer per Rechtsklick, schiebt
Items hinein, löscht sie über beide Klicks
und prüft auf dem Server, dass alles weg ist. Außerdem: abgelaufene Bestätigung löscht nicht, Deckel öffnet/schließt,
Speichern/Laden, Abbauen lässt den Inhalt fallen, Komparator-Signal, „Vor dem Löschen nachfragen“ aus,
„Beim Schließen löschen“ und der Einstellungsbildschirm (ungültige Eingaben, „Änderungen verwerfen?“).

## Lizenz

[MIT](LICENSE)
