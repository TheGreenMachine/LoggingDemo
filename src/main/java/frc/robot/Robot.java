package frc.robot;

import badlog.lib.BadLog;
import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.StatusFrameEnhanced;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.sensors.PigeonIMU;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Robot extends TimedRobot {

  private final TalonSRX mTurret = new TalonSRX(3);
  private final PigeonIMU mPigeon = new PigeonIMU(13);
  Joystick mJoy = new Joystick(0);
  private BadLog mLogger;
  private double mTargetPosition = Constants.turretStart;

  private double loopStart;

  @Override
  public void robotInit() {

    /* Setup logger file name pattern to use date and time */
    var logFile = new SimpleDateFormat("MMdd_HH-mm").format(new Date());
    var robotName = System.getenv("ROBOT_NAME");
    if (robotName == null) robotName = "default";
    var filePath = " /home/lvuser/" + robotName + "_" + logFile + ".bag";
    // if there is a usb drive use it NOTE: must be Fat32
    if(Files.exists(Path.of("/media/sda1"))) {
      filePath = "/media/sda1/" + robotName + "_" + logFile + ".bag";
    }
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      filePath = System.getenv("temp") + "\\" + robotName + "_" + logFile + ".bag";
    }
    mLogger = BadLog.init(filePath);

    /* https://github.com/dominikWin/badlog */

    // This line will out single values seen at top of report useful when tuning or logging auto selections etc.
    BadLog.createValue("Turret PID", String.format("kP = %f, kI = %f, kD = %f, kF = %f", Constants.kGains_Turning.kP, Constants.kGains_Turning.kI, Constants.kGains_Turning.kD, Constants.kGains_Turning.kF));

    // this tracks loop time in rio you can see if too much processing is being done good for seein loop overrun messages.
    // since there is no additional sub item in join this is a single graph displayed by default
    BadLog.createTopic("Timings/RobotLoop", "ms", this::getLastLoop, "hide", "join:Timings");

    // This is handy line to use timestamps on x axis otherwise items may have varing time since the default is just a count of log items
    BadLog.createTopic("Timings/Timestamp", "s", Timer::getFPGATimestamp, "xaxis", "hide");

    // Turret collapsed graph
    BadLog.createTopic("Turret/Desired", "NativeUnits", () -> mTargetPosition, "hide", "join:Turret/Position");
    BadLog.createTopic("Turret/Actual", "NativeUnits", () -> (double) getTurretPosition(), "hide" , "join:Turret/Position");
    BadLog.createTopic("Turret/Error", "NativeUnits", () -> (double) mTurret.getClosedLoopError(Constants.PID_PRIMARY), "hide", "join:Turret/Error");

    // pigeon collapsed graph
    BadLog.createTopic("Heading/Robot", "Degrees", this::getRobotHeading, "hide", "join:Pigeon/Heading");
    BadLog.createTopic("Heading/Turret", "Degrees", this::getTurretHeading, "hide", "join:Pigeon/Heading");

    // Errors collapsed graph
    BadLog.createTopic("Errors/Pigeon", "Integer", () -> (double) getPigeonState(), "hide", "join:Errors/Devices");
    mLogger.finishInitialization();

    /* Factory Default all hardware to prevent unexpected behaviour */
    mTurret.configFactoryDefault();
    mPigeon.configFactoryDefault();

    /* Configure output and sensor direction */
    mTurret.setInverted(false);
    mTurret.setSensorPhase(false);

    /* Set status frame periods to ensure we don't have stale data
     * https://phoenix-documentation.readthedocs.io/en/latest/ch18_CommonAPI.html#motor-controllers
     *  */
    mTurret.setStatusFramePeriod(StatusFrameEnhanced.Status_2_Feedback0, 1, Constants.kTimeoutMs);

    /* Configure neutral deadband */
    mTurret.configNeutralDeadband(Constants.kNeutralDeadband, Constants.kTimeoutMs);

    mTurret.configPeakOutputForward(+1.0, Constants.kTimeoutMs);
    mTurret.configPeakOutputReverse(-1.0, Constants.kTimeoutMs);

    /* FPID Gains for turn servo */
    mTurret.config_kP(Constants.kSlot_Position, Constants.kGains_Turning.kP, Constants.kTimeoutMs);
    mTurret.config_kI(Constants.kSlot_Position, Constants.kGains_Turning.kI, Constants.kTimeoutMs);
    mTurret.config_kD(Constants.kSlot_Position, Constants.kGains_Turning.kD, Constants.kTimeoutMs);
    mTurret.config_kF(Constants.kSlot_Position, Constants.kGains_Turning.kF, Constants.kTimeoutMs);
    mTurret.config_IntegralZone(Constants.kSlot_Position, Constants.kGains_Turning.kIzone, Constants.kTimeoutMs);
    mTurret.configClosedLoopPeakOutput(Constants.kSlot_Position, Constants.kGains_Turning.kPeakOutput, Constants.kTimeoutMs);
    mTurret.configAllowableClosedloopError(Constants.kSlot_Position, 4, Constants.kTimeoutMs);

    int closedLoopTimeMs = 1;
    mTurret.configClosedLoopPeriod(Constants.kSlot_Position, closedLoopTimeMs, Constants.kTimeoutMs);

  }

  private double getLastLoop() {
    return (Timer.getFPGATimestamp() - loopStart) * 1000;
  }

  @Override
  public void teleopInit() {
    mTurret.setNeutralMode(NeutralMode.Brake);
    zeroSensors();
    mTurret.set(ControlMode.Position, Constants.turretStart);
  }

  @Override
  public void teleopPeriodic() {
    loopStart = Timer.getFPGATimestamp();
    mTargetPosition = Constants.turretStart + getRobotHeading() * Constants.kSensorUnitsPerRotation / 360;
    mTurret.set(ControlMode.Position, mTargetPosition);
    mLogger.updateTopics();
    mLogger.log();
    if (mJoy.getRawButton(1)) {
      // Reboot Pigeon to force a error in IMU
      mPigeon.enterCalibrationMode(PigeonIMU.CalibrationMode.BootTareGyroAccel);
    }
  }

  @Override
  public void disabledInit() {
    super.disabledInit();
    mTurret.setNeutralMode(NeutralMode.Coast);
  }

  int getTurretPosition() {
    /* get the absolute pulse width position */
    return mTurret.getSensorCollection().getQuadraturePosition();
  }

  double getRobotHeading() {
    return mPigeon.getFusedHeading();
  }

  double getTurretHeading() {
    return (getTurretPosition() - Constants.turretStart) * 360.0 / Constants.kSensorUnitsPerRotation;
  }

  /**
   * Zero all sensors, both Pigeon and Talons
   */
  void zeroSensors() {
    /* Update Quadrature position to match absolute */
    mTurret.getSensorCollection().setQuadraturePosition(mTurret.getSensorCollection().getPulseWidthPosition() & 0xFFF, Constants.kTimeoutMs);
    mPigeon.setYaw(0, Constants.kTimeoutMs);
    mPigeon.setFusedHeading(0, Constants.kTimeoutMs);
    mPigeon.setAccumZAngle(0, Constants.kTimeoutMs);
    System.out.println("All sensors are zeroed.\n");
  }

  int getPigeonState() {
    return mPigeon.getState().value;
  }

}
