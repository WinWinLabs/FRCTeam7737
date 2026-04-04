# Electrical Resources — 2026

This folder stores all electrical engineering documents for the 2026 robot.

## File Organization

```
electrical/
├── wiring-diagrams/   # Full robot wiring diagrams
├── can-bus/           # CAN bus layout and device ID map
├── pdh-layout/        # Power Distribution Hub port assignments
└── pneumatics/        # Pneumatics diagrams (if applicable)
```

## CAN Bus Device ID Map

Update this each time a new device is added.

| Device | Type | CAN ID | Notes |
|--------|------|--------|-------|
| roboRIO | Controller | 0 | Fixed |
| PDP/PDH | Power | 1 | |
| Left Front Drive Motor | TalonFX / NEO | 10 | |
| Left Back Drive Motor | TalonFX / NEO | 11 | |
| Right Front Drive Motor | TalonFX / NEO | 12 | |
| Right Back Drive Motor | TalonFX / NEO | 13 | |
| Shooter Motor | TalonFX / NEO | 20 | |
| Intake Motor | TalonFX / NEO | 30 | |
| Gyroscope (Pigeon 2) | IMU | 5 | |

## PDH Port Assignments

| Port | Device | Breaker (A) |
|------|--------|-------------|
| 0 | Drive Motor FL | 40 |
| 1 | Drive Motor BL | 40 |
| 2 | Drive Motor FR | 40 |
| 3 | Drive Motor BR | 40 |
| ... | | |

## Notes

- All CAN IDs must match constants in `robot/src/main/java/frc/robot/Constants.java`.
- Run Phoenix Tuner X / REV Hardware Client to configure device IDs before deploy.
