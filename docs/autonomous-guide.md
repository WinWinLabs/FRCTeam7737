# Autonomous Development Guide — FRC Team 7737

This guide covers everything about how autonomous works on this robot: the tools, the files, the AprilTag system, known issues that must be fixed before autos will work reliably, and step-by-step instructions with sample code to build real competition autos.

> **Links to related docs:**
> - [docs/onboarding.md](onboarding.md) — overall robot code structure
> - [docs/subsystem-setup-guide.md](subsystem-setup-guide.md) — PIDF tuning reference

---

## Table of Contents

1. [How Autonomous Works on This Robot](#1-how-autonomous-works-on-this-robot)
2. [The Files Involved](#2-the-files-involved)
3. [Critical Bugs to Fix Before Autos Will Work](#3-critical-bugs-to-fix-before-autos-will-work)
4. [AprilTag Vision — How It Works and Why It Matters for Auto](#4-apriltag-vision--how-it-works-and-why-it-matters-for-auto)
5. [PathPlanner — Creating and Editing Paths](#5-pathplanner--creating-and-editing-paths)
6. [NamedCommands — Wiring Robot Actions Into Paths](#6-namedcommands--wiring-robot-actions-into-paths)
7. [Sample Code — Building Real Competition Autos](#7-sample-code--building-real-competition-autos)
   - [Fix 1: Enable Vision in Periodic](#fix-1-enable-vision-in-periodic)
   - [Fix 2: AprilTag Snap-To command](#fix-2-apriltag-snap-to-command)
   - [A Two-Piece Auto Routine](#a-two-piece-auto-routine)
   - [NamedCommands Registration Block](#namedcommands-registration-block)
8. [Implementation Steps — Start to Finish](#8-implementation-steps--start-to-finish)
9. [Tuning Autonomous PID](#9-tuning-autonomous-pid)
10. [Existing Commands Reference](#10-existing-commands-reference)

---

## 1. How Autonomous Works on This Robot

The autonomous system has three layers that work together:

```
PathPlanner GUI
    ↓  (creates .path and .auto files)
src/main/deploy/pathplanner/
    ↓  (read at runtime by PathPlannerLib)
SwerveSubsystem.getAutonomousCommand("Name")
    ↓  (returns a Command)
Robot.autonomousInit() schedules it
    ↓
Limelight MegaTag2 AprilTags → Vision.updatePoseEstimation()
    ↓  (continuously corrects where the robot thinks it is)
PPHolonomicDriveController drives the swerve to follow the path
```

**The key insight:** PathPlanner does not teleport the robot along a line. It uses the robot's *odometry* (where it thinks it is) to calculate corrections every 20ms. If odometry is wrong, the robot will drive to the wrong place. This is why AprilTags are so important — they correct odometry drift continuously.

**Current state:** `Robot.java` calls `m_robotContainer.getAutonomousCommand()` in `autonomousInit()`, which returns `drivebase.getAutonomousCommand("New Auto")`. That auto runs a single placeholder path ("New Path"). Nothing else. There are no shooter, intake, or game-piece commands registered yet — those need to be added.

---

## 2. The Files Involved

### Java files

| File | Role in autonomous |
|---|---|
| [src/main/java/frc/robot/Robot.java](../2026/Code/src/main/java/frc/robot/Robot.java) | Calls `getAutonomousCommand()` at auto start, cancels it at teleop start |
| [src/main/java/frc/robot/RobotContainer.java](../2026/Code/src/main/java/frc/robot/RobotContainer.java) | Registers `NamedCommands`, returns the auto command to run |
| [src/main/java/frc/robot/subsystems/swervedrive/SwerveSubsystem.java](../2026/Code/src/main/java/frc/robot/subsystems/swervedrive/SwerveSubsystem.java) | Contains `setupPathPlanner()`, `getAutonomousCommand()`, `driveToPose()` |
| [src/main/java/frc/robot/subsystems/swervedrive/Vision.java](../2026/Code/src/main/java/frc/robot/subsystems/swervedrive/Vision.java) | Reads Limelight MegaTag2 AprilTag data, feeds it into swerve odometry |
| [src/main/java/frc/robot/subsystems/swervedrive/LimelightHelpers.java](../2026/Code/src/main/java/frc/robot/subsystems/swervedrive/LimelightHelpers.java) | Low-level Limelight API — do not modify |
| [src/main/java/frc/robot/commands/swervedrive/auto/AutoBalanceCommand.java](../2026/Code/src/main/java/frc/robot/commands/swervedrive/auto/AutoBalanceCommand.java) | Legacy 2022 charge station balance — not applicable to 2026 Reefscape |
| [src/main/java/frc/robot/commands/swervedrive/drivebase/AimAtTarget.java](../2026/Code/src/main/java/frc/robot/commands/swervedrive/drivebase/AimAtTarget.java) | Limelight rotation-only aim PID command (kP=0.05, 1° tolerance) |

### Deploy / config files

| File | Role |
|---|---|
| [src/main/deploy/pathplanner/settings.json](../2026/Code/src/main/deploy/pathplanner/settings.json) | Robot physical dimensions for PathPlanner GUI — max velocity 3 m/s, NEO motors, 0.9m robot |
| [src/main/deploy/pathplanner/autos/New Auto.auto](../2026/Code/src/main/deploy/pathplanner/autos/New%20Auto.auto) | The current auto routine — runs "New Path", resets odometry on start |
| [src/main/deploy/pathplanner/paths/New Path.path](../2026/Code/src/main/deploy/pathplanner/paths/New%20Path.path) | A placeholder S-curve path from (2,7) to (7.2,6) on the blue side |
| [src/main/deploy/pathplanner/SamplePath.path](../2026/Code/src/main/deploy/pathplanner/SamplePath.path) | Old-format legacy sample — not used by any auto, safe to delete |
| [src/main/deploy/pathplanner/navgrid.json](../2026/Code/src/main/deploy/pathplanner/navgrid.json) | 2026 Reefscape field obstacle map — used by PathPlanner pathfinding to avoid field elements |
| [src/main/deploy/swerve/modules/pidfproperties.json](../2026/Code/src/main/deploy/swerve/modules/pidfproperties.json) | Motor PIDF values — **has a critical bug (see Section 3)** |
| [src/main/deploy/swerve/modules/physicalproperties.json](../2026/Code/src/main/deploy/swerve/modules/physicalproperties.json) | Wheel diameter, gear ratios, conversion factors — **has a critical bug (see Section 3)** |

---

## 3. Critical Bugs to Fix Before Autos Will Work

These are not tuning suggestions — they are bugs that will prevent the autonomous from driving correctly.

---

### Bug 1 — Conversion factors are zero (odometry is completely broken)

**File:** [src/main/deploy/swerve/modules/physicalproperties.json](../2026/Code/src/main/deploy/swerve/modules/physicalproperties.json)

**Current state:**
```json
"conversionFactors": {
  "angle": { "gearRatio": 21.43, "factor": 0 },
  "drive": { "diameter": 4, "gearRatio": 6.75, "factor": 0 }
}
```

**What this means:** YAGSL uses `factor` to convert raw motor encoder rotations into meters (drive) and degrees (angle). With `factor: 0`, the robot believes every encoder tick moves it zero meters. Odometry will show the robot never moving. PathPlanner will command increasingly large corrections trying to reach a position it thinks the robot never reaches.

**Fix — calculate the correct values:**

- **Drive factor** (meters per motor rotation):  
  `factor = (wheel_diameter_inches × 0.0254 × π) / drive_gear_ratio`  
  `factor = (4 × 0.0254 × π) / 6.75 = 0.04728`

- **Angle factor** (degrees per motor rotation):  
  `factor = 360 / steer_gear_ratio`  
  `factor = 360 / 21.43 = 16.8`

**Corrected file:**
```json
"conversionFactors": {
  "angle": { "gearRatio": 21.43, "factor": 16.8 },
  "drive": { "diameter": 4, "gearRatio": 6.75, "factor": 0.04728 }
}
```

> Verify your actual gear ratios against the module's spec sheet (SDS MK4i, Kraken, etc.). The values above match the numbers already in the file — confirm they match the physical hardware.

---

### Bug 2 — Drive PIDF has no feedforward (robot won't reach target speed)

**File:** [src/main/deploy/swerve/modules/pidfproperties.json](../2026/Code/src/main/deploy/swerve/modules/pidfproperties.json)

**Current state:**
```json
{ "drive": { "p": 0.0020645, "i": 0, "d": 0, "f": 0, "iz": 0 } }
```

**What this means:** `f: 0` means zero feedforward. For velocity control on a NEO, the `f` term does ~90% of the work to reach target speed. Without it, the motor relies entirely on `p` (error correction), which causes the robot to be slow out of the starting position and then overshoot. Path following will be sluggish and inaccurate.

**Fix:**
```json
{
  "drive": { "p": 0.0001, "i": 0, "d": 0, "f": 0.00017, "iz": 0 },
  "angle": { "p": 0.5,    "i": 0, "d": 0, "f": 0,       "iz": 0 }
}
```

See [docs/subsystem-setup-guide.md](subsystem-setup-guide.md#recommended-pidfpropertiesjson) for the full explanation of why these values.

---

### Bug 3 — Vision is disabled (AprilTags not updating odometry)

**File:** [src/main/java/frc/robot/subsystems/swervedrive/SwerveSubsystem.java](../2026/Code/src/main/java/frc/robot/subsystems/swervedrive/SwerveSubsystem.java), line ~56

**Current state:**
```java
private final boolean visionDriveTest = false;
```

**What this means:** The `periodic()` method only calls `vision.updatePoseEstimation(swerveDrive)` when `visionDriveTest == true`. Since it is `false`, the Limelight's AprilTag data is *never* used to correct odometry during a match. The robot navigates blind on wheel odometry alone, which drifts.

**Fix:** See [Sample Code — Fix 1](#fix-1-enable-vision-in-periodic) below. The right solution is to instantiate `Vision` unconditionally and always call `updatePoseEstimation()` in `periodic()`.

---

### Bug 4 — `SamplePath.path` is a legacy format file

**File:** [src/main/deploy/pathplanner/SamplePath.path](../2026/Code/src/main/deploy/pathplanner/SamplePath.path)

This file uses the old PathPlanner format (2023-era) with `anchorPoint`, `holonomicAngle`, `isReversal`, `stopEvent` fields. PathPlanner 2026 uses `anchor`, `rotationTargets`, `eventMarkers`. This file won't import correctly in the PathPlanner GUI and can confuse new team members. It is not referenced by any auto. **Safe to delete.**

---

## 4. AprilTag Vision — How It Works and Why It Matters for Auto

### What AprilTags are

AprilTags are printed fiducial markers (like QR codes) placed around the 2026 Reefscape field at known, fixed positions. Every tag has a unique ID. The field layout for 2026 is stored in:

```java
AprilTagFieldLayout fieldLayout = AprilTagFieldLayout.loadField(AprilTagFields.k2026RebuiltAndymark);
```

This is loaded in [Vision.java](../2026/Code/src/main/java/frc/robot/subsystems/swervedrive/Vision.java). It tells the robot exactly where every tag is on the field (position and orientation in 3D space).

> **Note:** `AprilTagFieldLayout.loadField(AprilTagFields.k2026RebuiltAndymark)` (the static factory method) is the form used by this robot's code. An older API style — `AprilTagFields.k2026RebuiltAndymark.loadAprilTagLayoutField()` — also exists but is not used here.

### How the Limelight reads them

The Limelight camera is mounted on the robot facing forward:
- **Position:** 10 inches forward, 0 lateral, 15 inches up from the robot center
- **Pitch:** 20° upward (to see tags on the scoring structures)

The Limelight runs its own image processing internally and publishes results over NetworkTables. This robot uses **MegaTag2** — the Limelight's built-in multi-tag pose solver, which is more accurate than single-tag estimates.

```java
// In Vision.java — this is the key call:
LimelightHelpers.PoseEstimate mt2 = 
    LimelightHelpers.getBotPoseEstimate_wpiBlue_MegaTag2("limelight");
```

This returns a full robot pose on the field (x, y, rotation) computed by the Limelight from all visible AprilTags, in Blue alliance coordinates (the standard WPILib convention).

### How poses are fed into YAGSL odometry

When a valid pose estimate is received, Vision.java calls:
```java
swerveDrive.addVisionMeasurement(pose, timestamp, stdDevs);
```

YAGSL fuses this measurement with the wheel odometry using a Kalman filter. The `stdDevs` (standard deviations) tell the filter how much to trust the vision estimate:
- **Single tag, close** → stdDevs `[4, 4, 8]` → low trust (tag could be ambiguous)
- **Multiple tags** → stdDevs scale with `1 + (distance² / 30)` → high trust when close
- **Single tag, >4m away** → stdDevs set to `Double.MAX_VALUE` → ignored entirely

### Why this matters for autonomous

Without AprilTag correction:
- The robot starts wherever you put it. If you place it 3cm off, it drives 3cm off the entire path.
- Wheel odometry drifts as the robot accelerates, turns, and the carpet pushes back.
- By the 5-second mark of autonomous, position error is often 10–20cm. That is enough to miss a scoring element.

With AprilTag correction:
- Every time the Limelight sees a tag (which happens constantly on the Reefscape field), it recalculates the robot's exact position.
- Path error resets to near-zero multiple times per second.
- The robot can reliably score game pieces at precise positions.

**Bottom line: fix Bug 3 above. Vision must be enabled for autonomous to work at competition.**

---

## 5. PathPlanner — Creating and Editing Paths

PathPlanner is the standalone GUI tool (and library) used to design autonomous paths. Download it from [pathplanner.dev](https://pathplanner.dev) or through the WPILib VS Code extension.

### Opening your robot's project in PathPlanner

1. Open PathPlanner.
2. File → Open Robot Project → navigate to `2026/Code/`.
3. PathPlanner reads `src/main/deploy/pathplanner/settings.json` to load robot dimensions and constraints.
4. The field shown will be the 2026 Reefscape field.

### Path files

Paths live in `src/main/deploy/pathplanner/paths/`. Each `.path` file is a JSON description of a Bezier-spline trajectory. You edit them in the GUI by dragging waypoints, not by editing JSON directly.

**Current paths:**

| File | Description | Start → End |
|---|---|---|
| `New Path.path` | Placeholder S-curve | (2, 7) → (7.2, 6) — Blue side, near the wall |
| `SamplePath.path` | Legacy format, unused | (0.84, 3.59) → (5, 3) |

**Path anatomy:**

```
Waypoints:     The anchor points the robot must pass through
Control points: Bezier handles that define the curvature between waypoints
Rotation targets: Independent heading goals (swerve can rotate separately from movement)
Event markers:  Trigger a NamedCommand at a specific point along the path
Constraint zones: Override max speed/acceleration for a segment (e.g., slow down at the reef)
Goal end state: Final velocity (0 = stop, >0 = continue into next path)
```

### Auto files

Autos live in `src/main/deploy/pathplanner/autos/`. Each `.auto` file defines a sequence of commands.

**Current auto:**

`New Auto.auto` — runs "New Path", resets odometry at the start:
```json
{
  "resetOdom": true,
  "command": {
    "type": "sequential",
    "commands": [
      { "type": "path", "data": { "pathName": "New Path" } }
    ]
  }
}
```

**In the PathPlanner GUI, an auto can contain:**

| Command type | What it does |
|---|---|
| `path` | Follow a path file |
| `named` | Run a registered NamedCommand (e.g., "ScoreNote", "RunIntake") |
| `sequential` | Run commands one after another |
| `parallel` | Run commands at the same time |
| `deadline` | Run parallel commands, stop when one finishes |
| `race` | Run parallel commands, stop when the first finishes |
| `wait` | Wait for a fixed number of seconds |

### navgrid.json

The `navgrid.json` file is the field's obstacle map — a boolean grid at 0.3m resolution covering the 17.5m × 8m field. `true` = passable, `false` = blocked. PathPlanner's on-the-fly pathfinding (used by `AutoBuilder.pathfindToPose()`) uses this to route around field elements like the Reef.

**Do not edit navgrid.json by hand.** Regenerate it from PathPlanner if you import a new field layout.

---

## 6. NamedCommands — Wiring Robot Actions Into Paths

PathPlanner paths can trigger named events at specific points along the path (e.g., "start the shooter when 0.5 seconds from the scoring position"). These are called **NamedCommands**. The names in the path file must exactly match the names registered in Java.

### How they are registered

In [RobotContainer.java](../2026/Code/src/main/java/frc/robot/RobotContainer.java), in the constructor:

```java
NamedCommands.registerCommand("test", Commands.print("I EXIST"));
```

This is the only registered command right now — it just prints a message. It needs to be replaced with real robot actions before competition autos are useful.

### How they are used in PathPlanner

In the PathPlanner GUI, drag the orange event marker pin to any point along a path. Type the command name (e.g., `"ScoreNote"`). PathPlanner will trigger it at that point during auto.

### Rules for NamedCommands

- Register ALL named commands before `configureBindings()` runs (as they are now).
- The name is case-sensitive and must match the path file exactly.
- A NamedCommand runs as part of the PathPlanner auto scheduler. It must have `addRequirements()` or it will run in parallel with the path without coordination.
- For game-piece actions that need to happen *while driving*, use the `deadline` or `parallel` command types in the PathPlanner auto editor.

---

## 7. Sample Code — Building Real Competition Autos

### Fix 1: Enable Vision in Periodic

**File to edit:** [src/main/java/frc/robot/subsystems/swervedrive/SwerveSubsystem.java](../2026/Code/src/main/java/frc/robot/subsystems/swervedrive/SwerveSubsystem.java)

Replace the vision-gated setup with unconditional vision initialization and a clean periodic update:

```java
// In the constructor, after setupPathPlanner():
// Remove:   if (visionDriveTest) { setupPhotonVision(); ... }
// Add:
vision = new Vision(swerveDrive::getPose, swerveDrive.field);
```

```java
@Override
public void periodic() {
    // Always update odometry from wheel encoders
    swerveDrive.updateOdometry();
    // Always fuse AprilTag vision measurements
    vision.updatePoseEstimation(swerveDrive);
}
```

Also remove or ignore the `visionDriveTest` field — it is no longer needed.

> **Note:** the first time you do this, test in simulation first. If the Limelight is not connected, `LimelightHelpers.getBotPoseEstimate_wpiBlue_MegaTag2` returns a pose with `tagCount == 0`, which `Vision.java` already guards against — so it is safe to call unconditionally.

---

### Fix 2: AprilTag Snap-To command

This command drives the robot to align precisely to a specific AprilTag using PathPlanner's on-the-fly pathfinding. Use this during auto to score on a reef branch or coral station with sub-centimeter precision.

Create **`src/main/java/frc/robot/commands/swervedrive/auto/AlignToTagCommand.java`**:

```java
package frc.robot.commands.swervedrive.auto;

import com.pathplanner.lib.path.PathConstraints;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.subsystems.swervedrive.SwerveSubsystem;

public class AlignToTagCommand {

    /**
     * Returns a Command that pathfinds to a position directly in front of
     * the specified AprilTag, facing it.
     *
     * @param drivebase    The swerve subsystem
     * @param tagId        The AprilTag ID to align to (1-22 for 2026 Reefscape)
     * @param offsetMeters How far in front of the tag to stop (e.g. 0.5 for 50cm)
     */
    public static Command alignTo(SwerveSubsystem drivebase, int tagId, double offsetMeters) {
        AprilTagFieldLayout layout = AprilTagFields.k2026RebuiltAndymark.loadAprilTagLayoutField();

        return Commands.defer(() -> {
            var tagPoseOpt = layout.getTagPose(tagId);
            if (tagPoseOpt.isEmpty()) {
                return Commands.print("WARNING: AprilTag ID " + tagId + " not found in field layout!");
            }

            Pose3d tagPose3d = tagPoseOpt.get();
            Pose2d tagPose = tagPose3d.toPose2d();

            // Stand offsetMeters in front of the tag, facing it
            Pose2d targetPose = tagPose.transformBy(
                new Transform2d(-offsetMeters, 0.0, Rotation2d.fromDegrees(180)));

            PathConstraints constraints = new PathConstraints(
                2.0,                          // max velocity m/s
                3.0,                          // max acceleration m/s²
                Units.degreesToRadians(360),  // max angular velocity
                Units.degreesToRadians(540)); // max angular acceleration

            return drivebase.driveToPose(targetPose);
        }, java.util.Set.of(drivebase));
    }
}
```

**Usage in RobotContainer (teleop button or auto NamedCommand):**
```java
// Bind to a button for driver-assist alignment during teleop:
driverXbox.b().whileTrue(AlignToTagCommand.alignTo(drivebase, 7, 0.5));

// Or register as a NamedCommand for use in PathPlanner autos:
NamedCommands.registerCommand("AlignReefLeft",  AlignToTagCommand.alignTo(drivebase, 7,  0.5));
NamedCommands.registerCommand("AlignReefRight", AlignToTagCommand.alignTo(drivebase, 8,  0.5));
NamedCommands.registerCommand("AlignCoralStation", AlignToTagCommand.alignTo(drivebase, 1, 0.8));
```

---

### A Two-Piece Auto Routine

This is the Java-side structure for a two-piece auto. The path files are created in PathPlanner. The NamedCommands are registered here and triggered from within the path event markers.

**In `RobotContainer.java`, updated `getAutonomousCommand()`:**

```java
public Command getAutonomousCommand() {
    // PathPlanner reads the name from autos/ folder at deploy time
    return drivebase.getAutonomousCommand("2Piece-Center");
}
```

**NamedCommands to register (in the RobotContainer constructor, before configureBindings()):**

```java
// Replace the "test" placeholder with real commands.
// These reference subsystems you will create (shooter, intake, etc.)
// See docs/subsystem-setup-guide.md for shooter setup.

NamedCommands.registerCommand("ShootSpeaker",
    new SequentialCommandGroup(
        new RunCommand(() -> m_shooter.setRPM(ShooterConstants.kSpeakerRPM), m_shooter)
            .withTimeout(1.5),                        // spin up for 1.5 seconds
        new InstantCommand(() -> m_feeder.feed()),    // push the note
        Commands.waitSeconds(0.5),                    // wait for note to clear
        new InstantCommand(m_shooter::stop)
    )
);

NamedCommands.registerCommand("IntakeNote",
    new RunCommand(() -> m_intake.run(), m_intake)
        .until(() -> m_intake.hasNote())              // stop when beam break triggers
        .withTimeout(3.0)                             // safety timeout
);

NamedCommands.registerCommand("StopAll",
    Commands.runOnce(() -> {
        m_shooter.stop();
        m_intake.stop();
    }, m_shooter, m_intake)
);
```

**In PathPlanner, design `2Piece-Center.auto` like this:**

```
Sequential:
  1. Path: "PreloadToScore"          (drive from start to scoring position)
       Event marker at 80%: "ShootSpeaker"   (start spinning up before arrival)
  2. Path: "ScoreToPickup"           (drive to note pickup position)
       Event marker at start: "IntakeNote"    (run intake while driving)
  3. Path: "PickupToScore2"          (drive back to scoring position)
       Event marker at 80%: "ShootSpeaker"
```

**Tips for path design:**
- Set `goalEndState.velocity = 0` only on the last path. Set it to `1.5` or `2.0` on intermediate paths so the robot does not fully decelerate between segments.
- Place event markers slightly *before* the action needs to happen (e.g., spin up the shooter 0.5s before reaching the scoring position).
- Use `constraintZones` to slow down near game pieces — running at 3 m/s while picking up notes will miss them.
- Set `resetOdom: true` on the first auto's `.auto` file so odometry is seeded from the starting pose. Do not set it on any subsequent path files.

---

### NamedCommands Registration Block

Here is a complete registration block template. Edit the command implementations as your subsystems are created:

```java
// In RobotContainer constructor, before configureBindings():

// ─── Shooter ──────────────────────────────────────────────────────
NamedCommands.registerCommand("SpinUpSpeaker",
    new RunCommand(() -> m_shooter.setRPM(ShooterConstants.kSpeakerRPM), m_shooter));

NamedCommands.registerCommand("SpinUpAmp",
    new RunCommand(() -> m_shooter.setRPM(ShooterConstants.kAmpRPM), m_shooter));

NamedCommands.registerCommand("StopShooter",
    new InstantCommand(m_shooter::stop, m_shooter));

// ─── Intake ───────────────────────────────────────────────────────
NamedCommands.registerCommand("RunIntake",
    new RunCommand(() -> m_intake.run(), m_intake));

NamedCommands.registerCommand("StopIntake",
    new InstantCommand(m_intake::stop, m_intake));

// ─── Combined actions ─────────────────────────────────────────────
NamedCommands.registerCommand("ShootAndMove",
    new ParallelDeadlineGroup(
        Commands.waitSeconds(2.0),           // deadline: 2 seconds
        new RunCommand(() -> m_shooter.setRPM(ShooterConstants.kSpeakerRPM), m_shooter),
        new RunCommand(() -> m_feeder.feed(), m_feeder)
    )
);

// ─── AprilTag alignment ───────────────────────────────────────────
NamedCommands.registerCommand("AlignReef",
    AlignToTagCommand.alignTo(drivebase, 7, 0.5));
```

---

## 8. Implementation Steps — Start to Finish

Follow these steps in order. Do not skip to path design until the robot can drive a straight line accurately.

### Step 0 — Fix the config bugs (30 minutes)

1. Edit [physicalproperties.json](../2026/Code/src/main/deploy/swerve/modules/physicalproperties.json): set `"factor": 16.8` for angle and `"factor": 0.04728` for drive (verify gear ratios).
2. Edit [pidfproperties.json](../2026/Code/src/main/deploy/swerve/modules/pidfproperties.json): set `"f": 0.00017` for drive, `"p": 0.0001` for drive, `"p": 0.5` for angle.
3. Edit `SwerveSubsystem.java`: unconditionally initialize `Vision` and call `updatePoseEstimation()` in `periodic()`.
4. Run `./gradlew build`. Fix any errors.
5. Deploy and verify the robot drives straight using the swerve telemetry in Shuffleboard.

### Step 1 — Verify odometry accuracy (1–2 hours)

1. Place the robot at a known field position.
2. In Shuffleboard or Glass, watch the robot's pose update as it drives.
3. Drive the robot exactly 1 meter forward. The odometry should read ~1m. If it reads 0.2m or 5m, the conversion factors or gear ratios are wrong.
4. Verify the Limelight is publishing AprilTag data (check NetworkTables in Shuffleboard: `limelight/botpose_wpiblue`).
5. With the Limelight active, the pose estimate should snap to the correct position whenever a tag is visible.

### Step 2 — Create your first real path (1 hour)

1. Open PathPlanner with the robot project.
2. Delete or archive `New Path.path` and `New Auto.auto` (rename them, don't edit in place).
3. Create a new path named `Mobility` — a simple straight line 3 meters forward from any starting position. Set max velocity 2 m/s, end velocity 0.
4. Create a new auto named `Mobility.auto` that runs this path with `resetOdom: true`.
5. In `RobotContainer.java`, change `getAutonomousCommand()` to return `"Mobility"`.
6. Deploy and test. The robot should drive 3m forward and stop.

### Step 3 — Add NamedCommands for your mechanisms (varies)

As each subsystem (shooter, intake, etc.) is built and tested in teleop:
1. Create the command classes (or inline lambdas) following the pattern in [docs/subsystem-setup-guide.md](subsystem-setup-guide.md).
2. Register them with `NamedCommands.registerCommand()` in the `RobotContainer` constructor.
3. Test each command in isolation (bind to a button, test in teleop) before using it in auto.

### Step 4 — Build multi-piece autos (varies)

1. In PathPlanner, design the full path sequence.
2. Place event markers at the correct positions and type the exact NamedCommand names.
3. Use the PathPlanner auto editor to wrap paths in `sequential` / `parallel` / `deadline` blocks as needed.
4. Test in simulation (`./gradlew simulateJava`) first — watch the robot follow the path in Glass.
5. Test on robot in an open area before running near field elements.

### Step 5 — Tune for competition accuracy

1. Run the auto 5 times from the same starting position. Mark where it finishes each time.
2. If paths are consistently off in the same direction: the starting pose in PathPlanner does not match where you are placing the robot. Adjust the starting waypoint.
3. If paths are randomly inaccurate: vision correction is not working. Check Limelight connection and tag visibility. Check `stdDevs` in Vision.java.
4. If the robot oscillates or overshoots scoring positions: lower the PathPlanner PID values. See [Section 9](#9-tuning-autonomous-pid).
5. If the robot stops short or is sluggish: the drive `f` value in pidfproperties.json needs to be increased.

---

## 9. Tuning Autonomous PID

These PIDs are configured in [SwerveSubsystem.java](../2026/Code/src/main/java/frc/robot/subsystems/swervedrive/SwerveSubsystem.java) inside `setupPathPlanner()`:

```java
new PPHolonomicDriveController(
    new PIDConstants(5.0, 0.0, 0.0),   // Translation PID (X/Y position)
    new PIDConstants(5.0, 0.0, 0.0))   // Rotation PID (heading)
```

These are PathPlanner's correction PIDs — they correct for deviation from the planned path, not for motor speed. They are **separate** from the motor PIDF in `pidfproperties.json`.

| Symptom | Likely cause | Fix |
|---|---|---|
| Robot wiggles side to side along the path | Translation P too high | Lower translation P from 5.0 toward 3.0 |
| Robot oscillates around final heading | Rotation P too high | Lower rotation P from 5.0 toward 3.0 |
| Robot consistently drifts off path | Translation P too low, or odometry inaccurate | Check conversion factors first; then raise P if needed |
| Robot undershoots final position | Translation P too low | Raise P, or check motor f value in pidfproperties.json |
| Robot spins excessively at waypoints | Rotation P too high | Lower rotation P |

**Recommended starting investigation order:** Fix conversion factors → fix motor PIDF feedforward → then adjust PathPlanner PIDs. Most path-following problems are odometry or motor control problems, not PID problems.

---

## 10. Existing Commands Reference

These commands already exist in the codebase and can be used as building blocks or referenced in autos.

### `AimAtTarget.java`
[src/main/java/frc/robot/commands/swervedrive/drivebase/AimAtTarget.java](../2026/Code/src/main/java/frc/robot/commands/swervedrive/drivebase/AimAtTarget.java)

Rotates the robot to face any target that the Limelight sees, using a PID loop on `TX` (horizontal angle offset). Does not move the robot's position.

```java
// Usage: rotate in place to aim at the nearest target
new AimAtTarget(drivebase)
```

- PID: kP=0.05, tolerance=1°
- Uses `LimelightHelpers.getTV("limelight")` to check for target validity
- Does not finish on its own (`isFinished()` not overridden) — use `.withTimeout()` or `.until()`

### `AutoBalanceCommand.java`
[src/main/java/frc/robot/commands/swervedrive/auto/AutoBalanceCommand.java](../2026/Code/src/main/java/frc/robot/commands/swervedrive/auto/AutoBalanceCommand.java)

Drives forward/backward using the robot's pitch to balance on a tilted platform, then locks wheels when level.

> **2026 note:** This was written for the 2022–2023 Charge Station game element. There is no equivalent in 2026 Reefscape. Keep the file for reference but do not use it in 2026 autos.

### `SwerveSubsystem.driveToPose(Pose2d)`
[SwerveSubsystem.java](../2026/Code/src/main/java/frc/robot/subsystems/swervedrive/SwerveSubsystem.java)

Uses PathPlanner's on-the-fly pathfinding to navigate to any field position, avoiding obstacles mapped in `navgrid.json`. This is the highest-level auto primitive — use it for driver-assist alignment and end-of-auto adjustments.

```java
// Navigate to a specific field position
drivebase.driveToPose(new Pose2d(3.5, 5.5, Rotation2d.fromDegrees(180)))
```

Constraints are hardcoded to `maxVel = swerveDrive.getMaximumChassisVelocity()` and `maxAccel = 4.0 m/s²`. Override in the `AlignToTagCommand` wrapper if you need slower approach speeds.
