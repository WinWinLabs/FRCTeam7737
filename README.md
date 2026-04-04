# FRC Team 7737 — Repository

Welcome to the official repository for **FRC Team 7737**. This repo is the single source of truth for **everything the team produces** — robot code, mechanical drawings, electrical schematics, standard operating procedures, team roster, and strategy documents — organized by competition year.

---

## Repository Structure

```
FRCTeam7737/
├── .github/                  # GitHub config: issue templates, PR templates, CI workflows
├── docs/                     # Cross-year documentation (onboarding, git workflow, contributing)
├── resources/                # Shared resources across all years (training materials, templates)
├── 2026/                     # 2026 Reefscape season
│   ├── robot/                # WPILib robot project (Java/C++)
│   └── resources/
│       ├── sops/             # Software, electrical, mechanical SOPs
│       ├── roster/           # Team roster and roles
│       ├── mechanical/       # CAD files, engineering drawings, BOM
│       ├── electrical/       # Wiring diagrams, CAN bus maps, PDH layout
│       └── strategy/         # Game analysis, autonomous strategies, scouting
├── 2027/                     # Created at kickoff each January
└── ...
```

---

## Branch Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Protected. Always competition-ready code. Only merged via approved PRs. |
| `develop` | Integration branch. Features merge here first for testing. |
| `YYYY/feature/<name>` | Year-prefixed feature branches (e.g. `2026/feature/auto-balance`) |
| `YYYY/fix/<name>` | Year-prefixed bug fix branches (e.g. `2026/fix/shooter-speed`) |

**Nobody pushes directly to `main`.** All changes go through a Pull Request with at least one review.

---

## Robot Code — WPILib

Robot code lives in `YYYY/robot/`. It is a standard **WPILib Gradle project** (Java).

- **WPILib is NOT forked.** It is declared as a version-locked dependency in `build.gradle` and `vendordeps/`.
- To update WPILib: open VS Code with the WPILib extension → "Manage Vendor Libraries" → update year.
- Each season, start from the previous year's subsystems as a baseline and update the WPILib version.

### Quick Start (Robot Code)
```bash
# Install WPILib VS Code extension first: https://docs.wpilib.org/en/stable/docs/zero-to-robot/step-2/wpilib-setup.html
cd 2026/robot
./gradlew build          # Compile
./gradlew simulateJava   # Simulate on desktop
./gradlew deploy         # Deploy to roboRIO (robot must be connected)
```

---

## How We Use GitHub (Beyond Code)

| Feature | How Team 7737 Uses It |
|---------|----------------------|
| **Issues** | Track tasks for all subteams — software, mechanical, electrical, outreach |
| **Labels** | `software`, `mechanical`, `electrical`, `strategy`, `outreach`, `bug`, `priority-high` |
| **Milestones** | Kickoff → Bag Day (or equivalent), Week 1 event, Championship |
| **Projects (Kanban)** | Sprint boards for each build phase |
| **Pull Requests** | Code review gate before anything hits `main` |
| **Wiki** | Team knowledge base: robot history, lesson learned, setup guides |
| **Releases** | Tag competition-ready commits (e.g. `2026-week1-event`, `2026-championship`) |
| **GitHub Actions** | Automated build/test on every push to ensure code compiles |
| **Discussions** | Team announcements, design decisions, retrospectives |

---

## New Season Checklist

Run this at kickoff every January:

- [ ] Create `YYYY/robot/` project from WPILib VS Code template
- [ ] Copy proven subsystems from previous year's `robot/src/main/java/frc/robot/subsystems/`
- [ ] Update WPILib version in `build.gradle` and `.wpilib/wpilib_preferences.json`
- [ ] Create `YYYY/resources/` folders (sops, roster, mechanical, electrical, strategy)
- [ ] Create a GitHub Milestone for the season with key dates
- [ ] Create a GitHub Project (Kanban) for the build season
- [ ] Update team roster in `YYYY/resources/roster/`
- [ ] Branch protection: confirm `main` requires PR + 1 review

---

## Contributing

See [docs/contributing.md](docs/contributing.md) for the full workflow.  
See [docs/git-workflow.md](docs/git-workflow.md) for a beginner-friendly Git guide.

---

## Resources

- [WPILib Documentation](https://docs.wpilib.org)
- [Chief Delphi (FRC Community Forum)](https://www.chiefdelphi.com)
- [FRC Game Manual (FIRST)](https://www.firstinspires.org/robotics/frc)
- [WPILib GitHub](https://github.com/wpilibsuite/allwpilib)
