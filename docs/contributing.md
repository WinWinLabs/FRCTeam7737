# Contributing Guide — FRC Team 7737

---

## Who Should Read This?

Everyone who commits anything to this repo — code, documents, drawings, roster updates. Even if you never write a line of Java, you may submit SOPs or electrical diagrams via PRs.

---

## Repository Layout

```
FRCTeam7737/
├── YYYY/robot/          → WPILib Java project for that year's robot
├── YYYY/resources/      → Non-code: SOPs, roster, drawings, strategy
├── docs/                → This guide, Git workflow, onboarding
├── resources/           → Cross-year resources (training, templates)
└── .github/             → Issue templates, PR template, CI workflows
```

---

## Robot Code Standards (Java / WPILib)

### File Structure

The robot project follows the WPILib Command-based framework:

```
robot/src/main/java/frc/robot/
├── Constants.java          # ALL hardware IDs, PID values, speeds — no magic numbers in subsystems
├── Main.java
├── Robot.java
├── RobotContainer.java     # Subsystem instantiation + button bindings
├── commands/               # Command classes
└── subsystems/             # Subsystem classes (one file per subsystem)
```

### Coding Rules

1. **No magic numbers.** All constants go in `Constants.java` with descriptive names.
   ```java
   // Bad
   motor.set(0.75);
   // Good
   motor.set(Constants.ShooterConstants.SHOOTER_SPEED_HIGH_GOAL);
   ```

2. **One subsystem per file.** `DriveSubsystem.java`, `ShooterSubsystem.java`, etc.

3. **Use WPILib shuffleboard for tuning.** Don't hardcode values you'll want to change during competition. Use `SmartDashboard` or `Shuffleboard` for live adjustments.

4. **Commands for actions, subsystems for state.** A subsystem shouldn't call another subsystem directly — use Commands to coordinate.

5. **Autonomous routines live in `commands/auto/`** as either `SequentialCommandGroup` subclasses or PathPlanner routines.

---

## Document / Resource Contributions

For non-code PRs (adding a wiring diagram, updating the roster, writing an SOP):

1. Use `2026/docs` branch naming: `2026/docs/add-wiring-diagram-v2`
2. PDFs and images are fine. Avoid committing very large files (> 25 MB) — use a link to Onshape, Google Drive, or similar instead.
3. Use the engineering doc issue template to announce the PR.

---

## Commit Messages

Format: imperative tense, present tense, under 72 characters for the first line.

```
Add two-piece autonomous routine for center start
Fix intake rollers reversing on jam detection
Update CAN bus map for 2026 robot configuration
Add wiring diagram v2 with corrected PDH ports
```

For bigger changes, add a body:
```
Fix shooter speed regression introduced in d4f3a2c

The RPM was accidentally reverted to 3200 during merge. Correct
value for high goal is 4200 RPM per testing on 2026-03-10.

Closes #47
```

---

## Branch Naming

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `YYYY/feature/<name>` | `2026/feature/auto-align-speaker` |
| Fix | `YYYY/fix/<name>` | `2026/fix/shooter-speed-regression` |
| Docs/Resources | `YYYY/docs/<name>` | `2026/docs/update-roster` |
| Hotfix | `YYYY/hotfix/<name>` | `2026/hotfix/comp-day-drive-fix` |

---

## Pull Request Requirements

- All PRs target `develop`, not `main`
- At least **1 approval** required (software lead or mentor)
- `./gradlew build` must pass (enforced by CI on code PRs)
- Fill out the PR template completely

---

## Getting Help

- Stuck on Git? Ask in the team chat or see `docs/git-workflow.md`
- Stuck on WPILib? See [docs.wpilib.org](https://docs.wpilib.org)
- Found something wrong in a document? Open an issue with the "Bug Report" or "Task" template
