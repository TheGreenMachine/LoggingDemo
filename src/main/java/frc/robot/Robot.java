package frc.robot;

import com.ctre.phoenix6.configs.Pigeon2Configuration;
import com.ctre.phoenix6.configs.TalonFXSConfiguration;
import com.ctre.phoenix6.hardware.Pigeon2;
import com.ctre.phoenix6.hardware.TalonFXS;
import com.ctre.phoenix6.signals.ExternalFeedbackSensorSourceValue;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.MotorArrangementValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.sim.ChassisReference;
import com.ctre.phoenix6.sim.Pigeon2SimState;
import com.ctre.phoenix6.sim.TalonFXSSimState;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.simulation.DCMotorSim;

public class Robot extends TimedRobot {

    private final TalonFXS mTurret = new TalonFXS(3);
    private final TalonFXSSimState mTurretSim = mTurret.getSimState();

    private final Pigeon2 mPigeon = new Pigeon2(13);
    private final Pigeon2SimState mPigeonSim = mPigeon.getSimState();
    private double simRotation = 0;

    private final DCMotorSim mMotorSimModel = new DCMotorSim(
            LinearSystemId.createDCMotorSystem(DCMotor.getBag(1), .01, Constants.kGearRatio), DCMotor.getBag(1));

    Joystick mJoy = new Joystick(0);
    private double mTargetPosition = Constants.turretStart;

    private double loopStart;

    public Robot () {

        /* creating a new configuration will reset to Factory Default to prevent unexpected behaviour */
        var turretConfig = new TalonFXSConfiguration();
        turretConfig.Commutation.MotorArrangement = MotorArrangementValue.Brushed_DC;
        turretConfig.MotorOutput.Inverted = InvertedValue.CounterClockwise_Positive;
        turretConfig.ExternalFeedback.ExternalFeedbackSensorSource = ExternalFeedbackSensorSourceValue.PulseWidth;
        turretConfig.Slot0.kD = Constants.kGains_Turning.kD;
        turretConfig.Slot0.kP = Constants.kGains_Turning.kP;
        turretConfig.Slot0.kI = Constants.kGains_Turning.kI;
        turretConfig.Slot0.kS = Constants.kGains_Turning.kS;
        turretConfig.CurrentLimits.StatorCurrentLimit = Constants.kGains_Turning.kPeakOutput;
        turretConfig.CurrentLimits.StatorCurrentLimitEnable = true;
        mTurret.getConfigurator().apply(turretConfig);

        var pigeonConfig = new Pigeon2Configuration();
        mPigeon.getConfigurator().apply(pigeonConfig);

    }

    @Override
    public void robotInit() {

        // Silence joystick errors
        DriverStation.silenceJoystickConnectionWarning(true);

        // This line will out single values seen at top of report useful when tuning or logging auto selections etc.
        GreenLogger.log(String.format("Turret PID: kP = %f, kI = %f, kD = %f, kS = %f", Constants.kGains_Turning.kP, Constants.kGains_Turning.kI, Constants.kGains_Turning.kD, Constants.kGains_Turning.kS));

        // this tracks loop time in rio you can see if too much processing is being done good for seein loop overrun messages.
        // since there is no additional sub item in join this is a single graph displayed by default
        GreenLogger.periodicLog("Timings/RobotLoop (ms)", this::getLastLoopMilliseconds);

        // Turret collapsed graph
        GreenLogger.periodicLog("Turret/Desired", () -> mTargetPosition);
        GreenLogger.periodicLog("Turret/Actual", () -> (double) getTurretPosition());
        GreenLogger.periodicLog("Turret/Error", mTurret.getClosedLoopError().asSupplier());

        // pigeon collapsed graph
        GreenLogger.periodicLog("Heading/Robot (deg)", this::getRobotHeading);
        GreenLogger.periodicLog("Heading/Turret", this::getTurretHeading);

        // Errors collapsed graph
        GreenLogger.periodicLog("Errors/Pigeon Reset", mPigeon.getStickyFaultField().asSupplier());
    }

    private double getLastLoopMilliseconds() {
        return (Timer.getFPGATimestamp() - loopStart) * 1000;
    }

    @Override
    public void teleopInit() {
        mTurret.setNeutralMode(NeutralModeValue.Brake);
        zeroSensors();
        mTurret.setPosition(Constants.turretStart);
    }

    @Override
    public void teleopPeriodic() {
        loopStart = Timer.getFPGATimestamp();
        mTargetPosition = Constants.turretStart + getRobotHeading();
        mTurret.setPosition(mTargetPosition);
        GreenLogger.updatePeriodic();
        if (mJoy.getRawButton(1)) {
            // Reboot Pigeon to force a error in IMU
            mPigeon.reset();
        }
    }

    @Override
    public void disabledInit() {
        super.disabledInit();
        mTurret.setNeutralMode(NeutralModeValue.Coast);
    }

    double getTurretPosition() {
        /* get the absolute pulse width position */
        return mTurret.getRawQuadraturePosition().getValueAsDouble();
    }

    double getRobotHeading() {
        return mPigeon.getRotation2d().getDegrees();
    }

    double getTurretHeading() {
        return (getTurretPosition() - Constants.turretStart);
    }

    /**
     * Zero all sensors, both Pigeon and Talons
     */
    void zeroSensors() {
        mPigeon.setYaw(0);
        GreenLogger.log("All sensors are zeroed.\n");
    }

    @Override
    public void simulationInit() {
        mTurretSim.MotorOrientation = ChassisReference.Clockwise_Positive;
    }

    @Override
    public void simulationPeriodic() {
        mPigeonSim.setSupplyVoltage(RobotController.getBatteryVoltage());
        mTurretSim.setSupplyVoltage(RobotController.getBatteryVoltage());

        mMotorSimModel.setInputVoltage(mTurretSim.getMotorVoltage());
        mMotorSimModel.update(Timer.getFPGATimestamp() - loopStart);

        mTurretSim.setRawRotorPosition(mMotorSimModel.getAngularPosition().times(Constants.kGearRatio));
        mTurretSim.setRotorVelocity(mMotorSimModel.getAngularVelocity().times(Constants.kGearRatio));

        if (DriverStation.isEnabled()) {
            simRotation += .1;
            mPigeonSim.setRawYaw(simRotation);
        }
    }
}
