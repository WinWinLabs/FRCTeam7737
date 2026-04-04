# 2026 Season — Reefscape

This folder contains everything for the **2026 FRC Reefscape** competition season.

## Contents

| Folder | Contents |
|--------|---------|
| `robot/` | WPILib Java robot project — deploy this to the roboRIO |
| `resources/sops/` | Standard Operating Procedures (software, electrical, mechanical) |
| `resources/roster/` | Team roster, roles, and contact sheet |
| `resources/mechanical/` | CAD files, engineering drawings, BOM |
| `resources/electrical/` | Wiring diagrams, CAN bus map, PDH layout |
| `resources/strategy/` | Game analysis, autonomous paths, match scouting sheets |

## Key Dates (Update as confirmed)

| Event | Date |
|-------|------|
| Kickoff | January 2026 |
| Week 1 Event | TBD |
| Championship | TBD |

## Robot Code Quick Start

```bash
cd 2026/robot
./gradlew build        # verify it compiles
./gradlew simulateJava # run desktop simulation
./gradlew deploy       # deploy to connected roboRIO
```

## WPILib Version

Update this after each WPILib release:

```
WPILib Version: 2026.x.x
```
(See `.wpilib/wpilib_preferences.json` inside `robot/`)
