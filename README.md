# Nightfall 0.5.0

Fabric-Client fuer Minecraft 1.21.11, Java 21. Komplett neu aufgebaut, ohne Mixins.

## Build
1. Werte in `gradle.properties` auf https://fabricmc.net/develop pruefen (Minecraft 1.21.11).
2. Gradle-Wrapper aus dem offiziellen Fabric-Example-Mod kopieren (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`).
3. `./gradlew build` -> `build/libs/nightfall-0.5.0.jar` in den `mods`-Ordner (Fabric API noetig).

## Tasten (in config/nightfall.json aenderbar: guiKey, hudKey, mapKey als GLFW-Codes)
| Taste | Funktion |
|---|---|
| Right Shift | ClickGUI (Suchleiste oben, Mittelklick auf Modul = Keybind) |
| H | HUD-Editor (Ziehen, Rechtsklick ein/aus, Mausrad Groesse, Mittelklick sperren, R reset, G Raster) |
| M | Vollbildkarte (Ziehen, Mausrad Zoom, 1/2/3 Filter, C zentrieren, Rechtsklick Waypoint) |

## Module
- **Combat:** TriggerBot, AutoPvP, CrystalAura, AutoAnchor, MaceAssist, AutoTotem, AutoGap, KillAura (Vanilla-Reichweite, Cooldown, Freundesliste: `/nf friend x add|del <name>`), TrapCrit (Ziel unter Fadenkreuz, Reichweite, Sichtlinie, 1x2-Einfassung, Cooldown, Crit-Fenster)
- **Movement:** Fly, Speed, Sprint, Step, SafeWalk, LongJump, Jesus, WaterWalk
- **Visuals:** PlayerESP, EntityESP, ItemESP, StorageESP, SpawnerESP, ChunkESP (Surface/Frames/Pulse)
- **World:** Waypoints
- **Basefinder:** inkrementeller Scanner, Score + Begruendung, SUS / SUS WEG, Markierungen werden pro Server+Dimension gespeichert
- **Misc:** AutoTPA (nur /tpa, nicht /tpahere; Whitelist/Blacklist)
- **HUD:** Watermark, Coordinates, ModuleList, Minimap, ScannerStatus, CombatStatus, WaypointList, Debug, FPS, Ping, Clock, Direction, Biome

## Befehle
`/nf wp add <name> [kategorie]`, `/nf wp del <name>`, `/nf wp list`,
`/nf tpa wl|bl add|del <name>`, `/nf tpa list`,
`/nf profile save|load <name>` (Export/Import aller Einstellungen inkl. HUD-Layout),
`/nf scan clear`

## Wichtige Hinweise
- **ESP-Darstellung:** Boxen/Tracer/Chunk-Rahmen werden im Screen-Space aus projizierten Weltkoordinaten gezeichnet
  (reine Mathematik, keine Render-Pipeline-Eingriffe). Nur Ego-Perspektive; leichte Abweichungen bei View-Bobbing und
  Sprint-FOV moeglich. Kein Durchsehen-Rendering wie bei echtem Depth-Test-ESP, keine gefuellten Flaechen.
- Keine Anti-Cheat-Umgehung. Server koennen Bewegung/Angriffe korrigieren oder blockieren; Nutzung auf eigenes Risiko
  und nur dort, wo es erlaubt ist.
- Nicht enthalten: Block-ESP (Block-Scan), NoSlow (braucht Mixin), TPS-Anzeige (clientseitig nicht verlaesslich),
  Teamfilter, ESP-Presets (Ersatz: /nf profile), AH-Sniper.
