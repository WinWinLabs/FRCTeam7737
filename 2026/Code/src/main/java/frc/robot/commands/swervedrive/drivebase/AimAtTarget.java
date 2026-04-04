package frc.robot.commands.swervedrive.drivebase;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.swervedrive.SwerveSubsystem;
import frc.robot.subsystems.swervedrive.LimelightHelpers;

public class AimAtTarget extends Command {
  private final SwerveSubsystem drivetrain;
  
  // Phase 3 & 4: PID Controller handles kP and Deadband for you!
  private final PIDController controller = new PIDController(0.05, 0, 0); 

  public AimAtTarget(SwerveSubsystem drivetrain) {
    this.drivetrain = drivetrain;
    addRequirements(drivetrain);
    controller.setTolerance(1.0); // Phase 4: Deadband of 1 degree
  }

  @Override
  public void execute() {
    // Phase 1: Target Acquisition
    boolean hasTarget = LimelightHelpers.getTV("limelight");

    if (hasTarget) {
      // Phase 2: Calculate Error (tx)
      double tx = LimelightHelpers.getTX("limelight");

      // Phase 3: Motor Output
      double rotationSpeed = controller.calculate(tx, 0); 
      
      // Tell the swerve to stay still (0,0) but rotate at our calculated speed
      drivetrain.drive(new Translation2d(0, 0), rotationSpeed, true);
    } else {
      // No target? Stop rotating.
      drivetrain.drive(new Translation2d(0, 0), 0, true);
    }

    
  }

  @Override
  public void end(boolean interrupted) {
    drivetrain.drive(new Translation2d(0, 0), 0, true);
  }
}
