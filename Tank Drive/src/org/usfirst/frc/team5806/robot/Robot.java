package org.usfirst.frc.team5806.robot;

import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;

public class Robot extends IterativeRobot {
	
	private static double limitedJoyL, limitedJoyR;
	
	// Driving objects
	private class Sonar extends AnalogInput {
		private int channel;
		public Sonar(int c) {
			super(c);
			channel = c;
		}
		public double getMM() {
			double milivolts = getVoltage() * 1000;
			double constant = -1;
			for (int i = 0; i < mvThresholds.length; i++) {
				if (milivolts <= mvThresholds[i]) {
					constant = voltDistanceConstants[i];
					break;
				}
			}
			if (constant == -1) constant = voltDistanceConstants[voltDistanceConstants.length - 1];
			double milimeters = milivolts * constant;
			return milimeters;
		}
		public int getChannel() {
			return channel;
		}
	}
	
	RobotDrive robot;
	Joystick joystick;
	
	// Sensors
	Encoder[] encoders;
	DigitalInput magnetSwitch;
	boolean magnetSwitched;

	IMU imu;

	Sonar[] sonars;

	// HAS TO BE A NEGATIVE NUMBER SO IT GOES THE RIGHT WAY
	private static final double DAMPENING_COEFFICIENT = -0.75;
	// MINIMUM CHANGE IN JOYSTICK POSITION TO CAUSE CHANGE IN MOTORS
	private static double rampCoefficient = 0.05;
	private static final double[] voltDistanceConstants = 
		{
		//each constant is a number of mm per mV with a certain mV value
		1.0246, 1.0239, 1.0235
		};
	private static final double[] mvThresholds = {4.88, 293, 4885};
	
	//At <= 4.88 mV, use 1.0246 mm / mV
	//At 4.88 mV < V <= 293 mV, use 1.0239 mm / mV
	//At 293 mV < V <= 4885 mV, use 1.0235 mm / mV
	
	Button addButton;
	Button subtractButton;
	
	// Pneumatics object
	Compressor compressor;
	DoubleSolenoid solenoid;
	
	public int getRollerRPM(int samplePeriodMillis) {
		int magnetCounter = 0;
		boolean detectedLastTime = false;
		long startingTime = System.currentTimeMillis();
		
		while(System.currentTimeMillis() - startingTime < samplePeriodMillis) {
			if (magnetSwitch.get() != magnetSwitched) {
				if(detectedLastTime == false){
					magnetCounter++;
					detectedLastTime = true;
				}
			} else {
				detectedLastTime = false;
			}
		}
		
		return magnetCounter;
	}
	
	public void robotInit() {
		robot = new RobotDrive(1, 0);
		joystick = new Joystick(1);
		magnetSwitch = new DigitalInput(4);
		magnetSwitched = false;

		encoders = new Encoder[2];
		encoders[0] = new Encoder(0, 1);
		encoders[0].reset();
		encoders[1] = new Encoder(2, 3);
		encoders[1].reset();
		
		sonars[0] = new Sonar(8);
		sonars[1] = new Sonar(9);
		
		addButton = new Button(joystick, 3);
		subtractButton = new Button(joystick, 4);
		
		compressor = new Compressor(0);
		compressor.start();
		
		imu = new IMU();
	}
	
	public void testInit() {
		teleopInit();
	}
	
	public void testPeriodic() {
		LiveWindow.run();
		teleopPeriodic();
		
		System.out.print(imu.getRotationalDisplacement());
		
	}

	public void teleopInit() {
		limitedJoyL = 0.1;
		limitedJoyR = 0.1;
	}
	
	public void teleopPeriodic() {
		if(addButton.readButton()) {
			rampCoefficient += 0.01;
			System.out.println("Button: " + rampCoefficient);
		}
		if(subtractButton.readButton()) {
			rampCoefficient -= 0.01;
			System.out.println("Button: " + rampCoefficient);
		}
		
		// Using exponential moving averages for joystick limiting
		for (int i = 0; i < sonars.length; i++) {
			System.out.println("Sonar " + (i+1) + " Dist = " + sonars[i].getMM() + " mm");
		}
		
		//using exponential moving averages for joystick limiting
		double desiredL = joystick.getRawAxis(1);
		double desiredR = joystick.getRawAxis(5);
		double errorL = desiredL - limitedJoyL;
		double errorR = desiredR - limitedJoyR;
		limitedJoyL += errorL * rampCoefficient;
		limitedJoyR += errorR * rampCoefficient;
		robot.tankDrive(DAMPENING_COEFFICIENT*limitedJoyL, DAMPENING_COEFFICIENT*limitedJoyR, true);
	}
}
