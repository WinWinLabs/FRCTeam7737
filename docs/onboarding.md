# Onboarding Guide — FRC Team 7737

This document is for anyone new to the team's code or repository. It explains what every folder and file does, how the robot code is structured, and the exact steps to make a change and get it on the robot.

---

## Table of Contents

1. [Repository Overview](#1-repository-overview)
2. [docs/ — Documentation](#2-docs--documentation)
3. [2026/resources/ — Season Resources](#3-2026resources--season-resources)
4. [2026/Code/ — Robot Code Deep Dive](#4-2026code--robot-code-deep-dive)
   - [Project Layout](#project-layout)
   - [Java Source Files](#java-source-files)
   - [Commands](#commands)
   - [Subsystems](#subsystems)
   - [Deploy Files (JSON Configs)](#deploy-files-json-configs)
   - [Vendor Libraries](#vendor-libraries)
5. [How to Make a Change](#5-how-to-make-a-change)
   - [Setup (First Time Only)](#setup-first-time-only)
   - [The Standard Workflow](#the-standard-workflow)
   - [Building and Testing](#building-and-testing)
   - [Deploying to the Robot](#deploying-to-the-robot)
   - [Emergency Rollback](#emergency-rollback)
6. [Competition Day Policy](#6-competition-day-policy)

---

## 1. Repository Overview

```
FRCTeam7737/
├── docs/               ← You are here. Cross-year guides and references.
├── 2026/
│   ├── Code/
│   │   └── 2026/       ← The actual 2026 robot code (WPILib Gradle project)
│   ├── resources/
│   │   ├── electrical/ ← CAN bus maps, PDH wiring, electrical diagrams
│   │   ├── mechanical/ ← CAD links, drawings, bill of materials
│   │   ├── roster/     ← Team roster and roles
│   │   ├── sops/       ← Standard Operating Procedures (deploy, startup, safety)
│   │   └── strategy/   ← Game analysis, autonomous routines, scouting
│   └── README.md       ← 2026 season overview and key dates
└── README.md           ← Top-level repo overview, branch strategy, quick start
```

**The robot code lives at:** `2026/Code/`

---

## 2. docs/ — Documentation

| File | What it is |
|---|---|
| `onboarding.md` | This file — the complete new-member guide |
| `contributing.md` | Coding standards: file structure rules, commit message format, PR requirements |
| `git-workflow.md` | Git workflow step-by-step, golden rules, emergency rollback commands |

If you have never used Git before, read `git-workflow.md` first.

---

## 3. 2026/resources/ — Season Resources

These folders are for the whole team, not just software.

### `roster/team-roster.md`
The team roster for the 2026 season. Lists everyone's name, role, and subteam. If you join the team, add yourself here.

### `sops/` (Standard Operating Procedures)
Step-by-step procedures everyone should know. Do not skip these at events.

| File | What it covers |
|---|---|
| `software-deploy.md` | Exactly how to build and deploy code to the roboRIO — prerequisites, every step, rollback |
| `robot-startup.md` | Pre-match startup checklist |
| `electrical-safety.md` | Electrical safety rules |
| `pit-procedures.md` | Competition pit setup and repair |

### `electrical/README.md`
Contains the **CAN Bus Device ID map** — every motor controller, sensor, and the Pigeon 2 gyro with their assigned CAN IDs. **These IDs must match the constants in `Constants.java`.** Also contains the PDH (Power Distribution Hub) port assignments and breaker sizes.

### `mechanical/README.md`
Links to Onshape/Fusion 360 CAD documents, naming conventions for drawings, and the bill of materials (BOM). Add your CAD links here so the whole team can find them.

### `strategy/README.md`
Game analysis, the autonomous routine index (which auto paths exist, expected points, status), and scouting resources (Statbotics, The Blue Alliance, custom scouting sheets).

---

## 4. 2026/Code/ — Robot Code Deep Dive

This is a standard **WPILib Command-Based Java** project built with Gradle.

### Project Layout

```
2026/Code/
├── src/main/java/frc/robot/   ← All Java source code
├── src/main/deploy/           ← JSON config files copied to the roboRIO at deploy time
├── vendordeps/                ← Vendor library declarations (what hardware libraries to use)
├── build.gradle               ← Build configuration (WPILib version, Java version, dependencies)
├── settings.gradle            ← Gradle project name
├── gradlew / gradlew.bat      ← Gradle wrapper (use this to build — do NOT install Gradle separately)
└── WPILib-License.md          ← WPILib license
```

---

### Java Source Files

All source lives under `src/main/java/frc/robot/`.

#### `Main.java`
**Do not touch this file.** It is the program entry point — it just calls `RobotBase.startRobot(Robot::new)` to start the robot. There is nothing to configure here.

#### `Robot.java`
The top-level robot class. WPILib calls its methods at the right times automatically.

| Method | When it runs | What it does here |
|---|---|---|
| `robotInit()` | Once at startup | Creates `RobotContainer`, starts the wheel-lock timer |
| `robotPeriodic()` | Every 20ms, always | Runs the `CommandScheduler` (processes all commands) |
| `disabledInit()` | When the robot is disabled | Enables motor brake mode so the robot doesn't roll |
| `disabledPeriodic()` | Every 20ms while disabled | Releases brake after 10 seconds (prevents motor heating) |
| `autonomousInit()` | Start of autonomous | Fetches and schedules the selected PathPlanner auto routine |
| `teleopInit()` | Start of teleop | Cancels the autonomous command if it is still running |
| `testInit()` | Entering test mode | Cancels all running commands |

You would edit `Robot.java` if you need logic that runs at specific match phase transitions (e.g., something that must happen exactly when autonomous begins).

#### `RobotContainer.java`
**This is the most important file to understand.** It wires everything together:
- Creates the `SwerveSubsystem` (the drivetrain)
- Creates the driver Xbox controller (port 0)
- Defines the **default drive command** (what the robot does when no other command is running)
- **Binds controller buttons to commands**

Current button bindings (normal teleop):
| Button | Action |
|---|---|
| A | Zero the gyro (reset forward direction) |
| X | Add a fake vision reading (for testing odometry) |
| Left Bumper | Lock wheels (X-pattern, stops the robot from being pushed) |

If you want to add a new button binding (e.g., "press B to run the intake"), this is the file you edit.

#### `Constants.java`
**All numbers that describe the robot go here.** Never put a raw number directly in a command or subsystem — always define it here and reference it by name.

| Constant | Value | What it means |
|---|---|---|
| `ROBOT_MASS` | ~57.8 kg | Robot weight (used for physics simulation) |
| `MAX_SPEED` | 14.5 ft/s (~4.42 m/s) | Maximum drivetrain speed |
| `LOOP_TIME` | 0.13 s | 20ms robot loop + 110ms SPARK MAX velocity lag |
| `DrivebaseConstants.WHEEL_LOCK_TIME` | 10 s | How long to hold brake after disable before releasing |
| `OperatorConstants.DEADBAND` | 0.1 | Joystick dead zone (ignore inputs below 10%) |
| `OperatorConstants.TURN_CONSTANT` | 6 | Scales the turning input rate |

Motor CAN IDs, PID values, sensor ports — all of these belong in `Constants.java`.

---

### Commands

Commands are actions the robot performs. They live in `src/main/java/frc/robot/commands/`.

#### Drive Commands (`commands/swervedrive/drivebase/`)

| File | What it does |
|---|---|
| `AbsoluteDrive.java` | Field-centric drive. The right stick X/Y sets the robot's heading direction directly (like pointing). The robot always faces the direction you push the stick. |
| `AbsoluteDriveAdv.java` | Same as `AbsoluteDrive` but adds 4 snap buttons — press a button to instantly face forward, backward, left, or right. Also has a fine-tune rotation axis. |
| `AbsoluteFieldDrive.java` | Field-centric drive using a single heading value instead of a vector. |
| `AimAtTarget.java` | Uses the Limelight's `TX` (horizontal angle to target) and a PID controller to rotate the robot to face the best visible target. Only rotates — translation (movement) still comes from the driver. |

The **active default drive command** is set in `RobotContainer.java` and uses `SwerveInputStream` (a YAGSL utility that reads controller axes and applies deadbands, scaling, and alliance-relative flipping automatically).

#### Auto Commands (`commands/swervedrive/auto/`)

| File | What it does |
|---|---|
| `AutoBalanceCommand.java` | Drives onto the charge station and balances using a PID controller watching the robot's pitch angle. Locks wheels when balanced. (Legacy — may not apply to 2026 game.) |

Autonomous **paths** (the actual routes the robot drives) are made in **PathPlanner** and stored as JSON files in `src/main/deploy/pathplanner/`. You do not write Java code for path following — you design the path in the PathPlanner GUI, save it, and then reference it by name in `RobotContainer.java`.

---

### Subsystems

Subsystems represent physical parts of the robot. They live in `src/main/java/frc/robot/subsystems/`.

#### `SwerveSubsystem.java`
The entire swerve drivetrain. Powered by **YAGSL** (Yet Another Generic Swerve Library).

Key things it does:
- Loads swerve hardware configuration from JSON files in `deploy/swerve/`
- Integrates with **PathPlanner** for autonomous path following (PID: translation `[5,0,0]`, rotation `[5,0,0]`)
- Mirrors paths automatically when on the Red alliance
- Provides `drive()`, `driveFieldOriented()`, `lock()`, `zeroGyro()`, `resetOdometry()`, `getPose()`
- Runs SysId characterization routines for tuning (accessible in simulation/test mode)
- Calls `Vision.updatePoseEstimation()` every loop to fuse camera data into odometry

If a drive motor stops working, check the CAN ID in `deploy/swerve/modules/` against `electrical/README.md`.

#### `Vision.java`
Handles camera-based pose estimation (figuring out where the robot is on the field using AprilTags).

- Field layout: `AprilTagFields.k2026RebuiltAndymark` (2026 Reefscape field)
- Camera defined: `LIMELIGHT` — limelight camera, mounted 10 in forward, 15 in high, pitched 20° up
- Uses **Limelight MegaTag2 (MT2)** — the Limelight's built-in multi-tag pose solver
- Falls back to PhotonVision processing for other camera types
- In simulation, creates a virtual camera that sees virtual AprilTags

#### `LimelightHelpers.java`
A utility class provided by Limelight Robotics. Do not modify this file. It provides the API used by `Vision.java` and `AimAtTarget.java` to read data from the Limelight over NetworkTables.

---

### Deploy Files (JSON Configs)

These files live in `src/main/deploy/` and are copied to the roboRIO every time you deploy. They are read at runtime — you do not need to recompile Java to change them.

#### `swerve/` — YAGSL Swerve Configuration
These JSON files describe the physical hardware of the swerve drive. If you change a motor, swap a CAN ID, or modify the wheel/module geometry, you edit these files (not Java code).

| File | What it configures |
|---|---|
| `swervedrive.json` | Top-level: max speed, gyro type and CAN ID, module positions |
| `controllerproperties.json` | Drive and steer PID values, motor type (NEO/TalonFX), conversion factors |
| `modules/frontleft.json` | Front-left module: drive motor CAN ID, steer motor CAN ID, encoder offset |
| `modules/frontright.json` | Front-right module: same |
| `modules/backleft.json` | Back-left module: same |
| `modules/backright.json` | Back-right module: same |

**Encoder offsets** (the angle each steer motor encoder reads when the wheel is pointed forward) are set in the per-module JSON files. After any wheel or encoder replacement, you must re-measure and update these offsets.

#### `pathplanner/` — Autonomous Paths
| File/Folder | What it is |
|---|---|
| `settings.json` | PathPlanner app settings (robot dimensions, max speed/acceleration) |
| `navgrid.json` | The field obstacle map PathPlanner uses for pathfinding |
| `paths/` | Individual path files (`.path`) — designed in the PathPlanner GUI |
| `autos/` | Auto routine files (`.auto`) — sequences of paths and commands |
| `generatedJSON/` | Auto-generated by PathPlanner, do not edit manually |
| `SamplePath.path` | Example path provided by PathPlanner |

To create a new autonomous routine:
1. Open PathPlanner (standalone app or from WPILib VS Code)
2. Design your path, save it — it appears in `paths/`
3. Build an auto routine in `autos/` referencing that path
4. In `RobotContainer.java`, pass your auto name to `drivebase.getAutonomousCommand("YourAutoName")`

---

### Vendor Libraries

Vendor libraries are hardware support packages declared in `vendordeps/`. WPILib downloads them automatically when you build. You never interact with these files directly unless updating to a new library version.

| Library | What hardware it supports |
|---|---|
| `WPILibNewCommands.json` | WPILib Command-Based framework (core — always present) |
| `yagsl-2026.2.27.1.json` | YAGSL swerve drive library |
| `PathplannerLib.json` | PathPlanner autonomous path following |
| `REVLib.json` | REV Robotics SPARK MAX / SPARK FLEX motor controllers, NEO motors |
| `Phoenix6-frc2026-latest.json` | CTRE TalonFX motor controllers, Pigeon 2 gyro (current generation) |
| `Phoenix5-frc2026-latest.json` | CTRE TalonSRX / VictorSPX (older CTRE hardware, kept for compatibility) |
| `photonlib.json` | PhotonVision camera processing library |
| `ReduxLib-2026.1.1.json` | Redux Robotics (Canandgyro, Canandmag encoders) |
| `ThriftyLib.json` | Thrifty Bot hardware (encoders, etc.) |
| `Studica.json` | Studica hardware support |

---

## 5. How to Make a Change

### Setup (First Time Only)

1. **Install WPILib** — download the WPILib installer from [docs.wpilib.org](https://docs.wpilib.org). It installs VS Code, the WPILib extension, and all tools in one step.
2. **Install Git** — download from [git-scm.com](https://git-scm.com).
3. **Clone the repository:**
   ```
   git clone https://github.com/FRCTeam7737/FRCTeam7737.git
   cd FRCTeam7737
   ```
4. **Open the robot project in WPILib VS Code:**
   - File → Open Folder → select `2026/Code/`
   - WPILib will detect the project automatically.
5. **Set your team number** (only needed once):
   - Press `Ctrl+Shift+P` → type `WPILib: Set Team Number` → enter `7737`

---

### The Standard Workflow

Every change, no matter how small, follows these steps:

**Step 1 — Get the latest code**
```
git checkout develop
git pull origin develop
```
Always start from an up-to-date `develop` branch.

**Step 2 — Create your branch**
```
git checkout -b 2026/feature/your-description
```
Replace `your-description` with something short and clear, e.g. `intake-command`, `auto-two-piece`, `fix-gyro-drift`.

Use the right prefix:
| Prefix | Use for |
|---|---|
| `2026/feature/` | New functionality |
| `2026/fix/` | Bug fixes |
| `2026/docs/` | Documentation changes |
| `2026/hotfix/` | Critical fixes at competition |

**Step 3 — Make your changes**

Edit files in VS Code. Common tasks:
- **Add a button binding** → edit `RobotContainer.java`
- **Add a new subsystem** → create a file in `subsystems/`, instantiate it in `RobotContainer.java`
- **Add a new command** → create a file in `commands/`, add a binding in `RobotContainer.java`
- **Change a constant** (max speed, PID value, CAN ID) → edit `Constants.java`
- **Change swerve hardware** (motor ID, encoder offset) → edit the appropriate file in `deploy/swerve/`
- **Create an autonomous path** → use PathPlanner GUI

**Step 4 — Build and verify**

In the WPILib VS Code terminal:
```
./gradlew build
```
This must say `BUILD SUCCESSFUL` before you continue. Fix any errors before committing.

**Step 5 — Commit your changes**
```
git add .
git commit -m "Add intake command triggered by right bumper"
```
Commit messages should be short, specific, and written as an imperative sentence (what the commit *does*, not what you *did*).

Good:
- `Add two-piece center autonomous routine`
- `Fix gyro drift in teleop after disable`
- `Update front-left encoder offset to 0.23`

Bad:
- `changes`
- `fixed stuff`
- `wip`

**Step 6 — Push and open a Pull Request**
```
git push origin 2026/feature/your-description
```
Then go to GitHub and open a Pull Request targeting `develop` (not `main`). Fill out the PR template completely.

**Step 7 — Address review feedback**

At least one person (software lead or mentor) must approve the PR. Make any requested changes, then push again — the PR updates automatically.

**Step 8 — Merge**

After approval and a passing build, the PR is merged into `develop`. Your feature is now in the shared codebase.

---

### Building and Testing

| Command | What it does |
|---|---|
| `./gradlew build` | Compiles all Java code. Run this before every commit. |
| `./gradlew simulateJava` | Runs the robot code on your laptop. Opens a simulated Driver Station and field view. Useful for testing autonomous paths and command logic without a robot. |
| `./gradlew clean` | Deletes all build output. Run this if you get mysterious build errors. |

**Simulation tips:**
- The simulated robot appears in Glass (the YAGSL sim viewer).
- PathPlanner paths can be visualized and tested in simulation before running on hardware.
- Use the Shuffleboard widget in simulation to monitor subsystem states.

---

### Deploying to the Robot

> Read the full procedure in `2026/resources/sops/software-deploy.md` before deploying for the first time.

**Prerequisites:**
- Laptop connected to robot radio Wi-Fi (`7737_XXXX`) or USB tethered
- Robot powered on, roboRIO booted (status light solid green)
- `./gradlew build` passes

**Deploy:**
```
./gradlew deploy
```
Or press `Ctrl+Shift+P` → `WPILib: Deploy Robot Code`.

After deploy, the Driver Station should show "Robot Code" in green. Before enabling, check the Driver Station for any fault messages.

**First enable after a deploy:** always test in **Disabled** mode first, then enable in **Teleop** before running **Auto**. Watch for unexpected motor movement.

---

### Emergency Rollback

If something breaks and you need to revert to a previous working version:

```
# See recent commits
git log --oneline -10

# Revert source files to a previous commit (replace <hash> with the commit hash)
git checkout <hash> -- src/

# Rebuild and deploy
./gradlew deploy
```

At competition, only the Drive Coach or Software Lead may authorize a rollback.

---

## 6. Competition Day Policy

- **Nobody pushes directly to `main` ever.** At competition, changes follow the same PR process but with abbreviated review.
- The fix workflow at competition: create a `2026/hotfix/<name>` branch → test in the pit → open PR → mentor approves → deploy from the approved commit.
- **Never deploy untested code during a match queue.**
- Only the **Drive Coach** or **Software Lead** may authorize enabling the robot or approving code changes at an event.
- If the robot behaves unexpectedly, disable immediately and use `git log` to identify what changed.
