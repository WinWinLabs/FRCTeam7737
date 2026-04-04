# Subsystem Setup Guide — Shooter & Drivetrain

This guide covers how to set up a shooter subsystem with RPM-based control and how to tune the swerve drivetrain PIDF values for SPARK MAX (NEO) motors.

---

## Table of Contents

1. [Shooter Setup](#1-shooter-setup)
   - [Constants.java](#constantsjava)
   - [ShooterSubsystem.java](#shootersubsystemjava)
   - [RobotContainer.java](#robotcontainerjava)
   - [Tuning Steps](#tuning-steps)
2. [Swerve Drivetrain PIDF Tuning (SPARK MAX / NEO)](#2-swerve-drivetrain-pidf-tuning-spark-max--neo)
   - [Recommended pidfproperties.json](#recommended-pidfpropertiesjson)
   - [Why These Values](#why-these-values)
   - [physicalproperties.json — Conversion Factors](#physicalpropertiesjson--conversion-factors)

---

## 1. Shooter Setup

The shooter is split across three files following standard WPILib Command-Based practice. `Constants.java` holds configuration values, `ShooterSubsystem.java` talks to the hardware, and `RobotContainer.java` maps the buttons.

### Constants.java

Add a `ShooterConstants` inner class. This is your single source of truth — CAN IDs, RPM presets, and PIDF values all go here. Never put raw numbers directly in the subsystem.

```java
public static final class ShooterConstants {
    // CAN IDs — must match REV Hardware Client and electrical/README.md
    public static final int kLeaderMotorId   = 10;
    public static final int kFollowerMotorId = 11;

    // RPM Presets
    public static final double kSpeakerRPM = 4500.0;
    public static final double kAmpRPM     = 1500.0;

    // PIDF Values (tune these for your specific shooter)
    public static final double kP  = 0.0001;
    public static final double kFF = 0.00018; // Approx 1/MaxRPM for NEOs (~1/5676)
}
```

### ShooterSubsystem.java

Create `src/main/java/frc/robot/subsystems/ShooterSubsystem.java`. This class owns the two motor objects and exposes `setRPM()` and `stop()` — nothing else. Commands should not manipulate motors directly.

```java
package frc.robot.subsystems;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkLowLevel.MotorType;
import com.revrobotics.SparkPIDController;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.ShooterConstants;

public class ShooterSubsystem extends SubsystemBase {

    private final CANSparkMax leader   = new CANSparkMax(ShooterConstants.kLeaderMotorId,   MotorType.kBrushless);
    private final CANSparkMax follower = new CANSparkMax(ShooterConstants.kFollowerMotorId, MotorType.kBrushless);
    private final SparkPIDController pidController = leader.getPIDController();

    public ShooterSubsystem() {
        leader.restoreFactoryDefaults();
        follower.restoreFactoryDefaults();

        // Make the follower mirror the leader.
        // Set 'true' if the motors face opposite directions (one is physically inverted).
        follower.follow(leader, true);

        // Apply PIDF constants from Constants.java
        pidController.setP(ShooterConstants.kP);
        pidController.setFF(ShooterConstants.kFF);

        // Save settings to the motor controller flash memory.
        // This prevents losing configuration on a power cycle.
        leader.burnFlash();
        follower.burnFlash();
    }

    /** Spin the shooter to a target RPM using the onboard PID controller. */
    public void setRPM(double rpm) {
        pidController.setReference(rpm, CANSparkMax.ControlType.kVelocity);
    }

    /** Cut power to both motors. */
    public void stop() {
        leader.set(0);
    }
}
```

### RobotContainer.java

Add the shooter subsystem instance and button bindings to `RobotContainer.java`. The operator controller is on **port 1** (driver is port 0).

```java
// In RobotContainer — add these declarations alongside the existing drivebase fields:
private final ShooterSubsystem m_shooter  = new ShooterSubsystem();
private final CommandXboxController m_operator = new CommandXboxController(1);
```

Inside `configureBindings()` (or the equivalent binding method), add:

```java
// Hold Y → spin up for Speaker shot. Release → stop.
m_operator.y()
    .whileTrue(new RunCommand(() -> m_shooter.setRPM(ShooterConstants.kSpeakerRPM), m_shooter))
    .onFalse(new InstantCommand(m_shooter::stop, m_shooter));

// Hold X → spin up for Amp shot. Release → stop.
m_operator.x()
    .whileTrue(new RunCommand(() -> m_shooter.setRPM(ShooterConstants.kAmpRPM), m_shooter))
    .onFalse(new InstantCommand(m_shooter::stop, m_shooter));
```

Required imports:
```java
import edu.wpi.first.wpilibj2.command.RunCommand;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.Constants.ShooterConstants;
```

---

### Tuning Steps

Follow these in order. Do not skip to PIDF tuning until the hardware is verified.

1. **Verify CAN IDs** — Open REV Hardware Client, confirm each motor controller's CAN ID matches what you put in `Constants.java`. Check against `2026/resources/electrical/README.md`.

2. **Check motor directions** — With the robot disabled, manually spin the shooter wheels and verify both motors are mechanically correct. Then enable and run at a low RPM (e.g. 500). If the motors fight each other or the shooter decelerates instead of spinning up, flip the `true`/`false` in `follower.follow(leader, true)`.

3. **Tune kFF first** — `kFF` (feedforward) is the primary tuner for velocity control. It is approximately `1 / MaxRPM`. For NEOs, `MaxRPM ≈ 5676`, so the starting value is `~0.000176`. Increase `kFF` if the shooter consistently falls short of target RPM at steady state. Decrease it if it overshoots.

4. **Tune kP second** — `kP` only corrects the remaining error after `kFF` does the heavy work. Start at `0.0001`. If the shooter oscillates around the target, decrease `kP`. If it is slow to reach steady state, increase it.

5. **Verify with Shuffleboard** — Add the motor's encoder velocity to Shuffleboard to watch live RPM. Compare against your setpoint to confirm the PID is tracking correctly.

---

## 2. Swerve Drivetrain PIDF Tuning (SPARK MAX / NEO)

The swerve configuration files live in `src/main/deploy/swerve/`. These are read at runtime — you can update them without recompiling Java. Deploy to the robot after any change.

### Recommended pidfproperties.json

These are the baseline values for SPARK MAX with NEO motors. Update `src/main/deploy/swerve/pidfproperties.json`:

```json
{
  "drive": {
    "p": 0.0001,
    "i": 0,
    "d": 0,
    "f": 0.00017,
    "iz": 0
  },
  "angle": {
    "p": 0.5,
    "i": 0,
    "d": 0,
    "f": 0,
    "iz": 0
  }
}
```

### Why These Values

**Drive `f` (0.00017) — the most important field:**
This is feedforward for velocity control. It is calculated as `1 / MaxRPM` (≈ `1 / 5676`). The `f` value does ~90% of the work to get the motor to the target speed. Without it, `p` alone will lag significantly. If your robot drives slower than expected, increase `f` slightly. If it overshoots speed, decrease it.

**Drive `p` (0.0001):**
Kept small because `f` handles the baseline. This `p` value only corrects the small remaining error. The starting value of `0.0020645` (WPILib default) is too aggressive when combined with a proper `f` — it will cause oscillation.

**Angle `p` (0.5):**
Position control (steering) requires a much higher `p` than velocity control. Tune this by feel:
- Modules **jitter or shake** → lower `p` toward `0.1`
- Modules **turn too slowly** or don't reach the target angle → raise `p` toward `1.0`
- Modules **oscillate** around the target → lower `p` or add a small `d` value

`f` is `0` for angle control because position control does not benefit from feedforward.

---

### physicalproperties.json — Conversion Factors

YAGSL needs to know the physical geometry of your swerve modules to convert motor rotations into real-world units. Set these in `src/main/deploy/swerve/physicalproperties.json`:

```json
"conversionFactors": {
  "angle": {
    "gearRatio": 21.4285714286,
    "factor": 16.8
  },
  "drive": {
    "diameter": 4,
    "gearRatio": 6.75,
    "factor": 0.04728
  }
}
```

| Field | Unit | How to calculate |
|---|---|---|
| `angle.gearRatio` | — | Steer gear ratio (motor rotations per wheel rotation). Check your module's spec sheet. |
| `angle.factor` | Degrees per rotation | `360 / gearRatio` |
| `drive.diameter` | Inches | Physical wheel diameter |
| `drive.gearRatio` | — | Drive gear ratio (motor rotations per wheel rotation) |
| `drive.factor` | Meters per rotation | `(diameter_inches × 0.0254 × π) / gearRatio` |

If the robot drives farther or shorter than expected (e.g., you command 1 meter and it drives 1.2m), your `drive.factor` is wrong — recalculate it with the actual wheel diameter and gear ratio.
