# Software Deploy SOP

**Version:** 1.0  
**Last Updated:** 2026  
**Owner:** Software Lead

---

## Prerequisites

- WPILib VS Code extension installed
- Laptop connected to robot radio (Wi-Fi or USB tether)
- Robot is powered on, roboRIO is booted (status light solid green)

## Steps

1. Open `2026/robot/` in VS Code with the WPILib extension active.
2. Confirm team number is set: **Ctrl+Shift+P** → "WPILib: Set Team Number" → `7737`
3. Run `./gradlew build` and confirm **BUILD SUCCESSFUL** before deploying.
4. Connect to robot Wi-Fi (`7737_XXXX`) or USB tether.
5. **Ctrl+Shift+P** → "WPILib: Deploy Robot Code" (or `./gradlew deploy`).
6. Wait for deploy to complete — Driver Station should show "Robot Code" indicator turn green.
7. Enable robot in **Disabled** mode first, check that no faults appear in Driver Station.

## Rollback

If new code causes problems during competition:
```bash
git log --oneline -10   # find the last known-good commit hash
git checkout <hash> -- src/
./gradlew deploy
```

## Notes

- Never deploy untested code during a match queue.
- Always test in Teleop disabled mode before enabling.
- Driver Station must show team number `7737` before enabling.
