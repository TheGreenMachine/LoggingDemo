/**
 *  Class that organizes gains used when assigning values to slots
 */
package frc.robot;

public class Gains {
	public final double kP;
	public final double kI;
	public final double kD;
	public final double kS;
	public final double kPeakOutput;
	
	public Gains(double _kP, double _kI, double _kD, double _kS, double _kPeakOutput){
		kP = _kP;
		kI = _kI;
		kD = _kD;
		kS = _kS;
		kPeakOutput = _kPeakOutput;
	}
}
