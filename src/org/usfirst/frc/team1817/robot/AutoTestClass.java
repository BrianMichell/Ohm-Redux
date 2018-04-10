package org.usfirst.frc.team1817.robot;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class AutoTestClass {
	//Autonomous selectors
	private final SendableChooser<String> AUTO = new SendableChooser<>();
	private final String TIMED_CROSS = "Timed Cross Line";
	private final String SWITCH_AUTO = "Switch";
	private final String WIP_CENTER = "WIP Center auto (2 cube)";

	//Alliance station selectors
	private final SendableChooser<String> STATION = new SendableChooser<>();
	private final String LEFT_STATION = "Left station";
	private final String RIGHT_STATION = "Right station";
	private final String MIDDLE_STATION = "Middle station";

	//Field specific constants
	private final int ROBOT_LENGTH = 38;
	// private final int DISTANCE_TO_SWITCH_FRONT = 144 - ROBOT_LENGTH;
	private final int DISTANCE_TO_SWITCH_MID = (170 - ROBOT_LENGTH / 2);
	private final int DISTANCE_TO_SWITCH_BACK = 196;
	private final int SWITCH_LENGTH = 154;
	private final int LEFT_TURN = -90;
	private final int RIGHT_TURN = 90;
	private final int MID_RIGHT = 40;
	private final int MID_LEFT = -40;
	//private final int HYPOTONUSE = 180;
	private final int HYPOTONUSE = 100;

	//Robot speeds
	private final double DRIVE_SPEED = 0.75;
	private final double TURN_SPEED = 0.65;

	//States
	private int state;
	private final int DISABLED = 0;
	private final int ENABLED = 1;

	//Robot classes
	private final DriverStation ds;
	private final Hardware hw;
	private final Drive drive;
	private final Hand hand;
	private final Fingers fingers;
	private final Timer timer;

	private boolean hasError = false;

	private double lastAngle = 0.0;
	private boolean turnDone = false;

	private boolean firstDone = false;

	public AutoTestClass(Hardware hw, Drive drive, Shift shift, Hand hand, Fingers fingers) {
		state = DISABLED;

		this.ds = DriverStation.getInstance();

		AUTO.addDefault(TIMED_CROSS, TIMED_CROSS);
		AUTO.addObject(SWITCH_AUTO, SWITCH_AUTO);
		AUTO.addObject(WIP_CENTER, WIP_CENTER);

		SmartDashboard.putData("Auto", AUTO);

		STATION.addDefault(MIDDLE_STATION, MIDDLE_STATION);
		STATION.addObject(RIGHT_STATION, RIGHT_STATION);
		STATION.addObject(LEFT_STATION, LEFT_STATION);

		SmartDashboard.putData("Station", STATION);

		this.hw = hw;
		this.drive = drive;
		this.hand = hand;
		this.fingers = fingers;

		timer = new Timer();
		timer.start();

	}

	/*
	public void run() {
		shift.setInHighGear(false);
		while (!Thread.interrupted()) {
			SmartDashboard.putNumber("AUTO STATE", state);
			SmartDashboard.putNumber("Last angle", lastAngle);
			SmartDashboard.putBoolean("Turn done", turnDone);
	
			switch (state) {
			case DISABLED:
				break;
			case ENABLED:
				runAuto();
				break;
			}
	
			if (!ds.isAutonomous()) {
				stop();
			}
	
			Timer.delay(0.005);
		}
	}
	*/

	public void start() {
		state = ENABLED;

		turnDone = false;
		lastAngle = 0.0;

		timer.reset();
		timer.start();
	}

	public void stop() {
		state = DISABLED;

		timer.stop();
		timer.reset();
	}

	public void runAuto() {
		double time = timer.get();
		String station = STATION.getSelected();
		new Thread(() -> {
			switch (AUTO.getSelected()) {
			case TIMED_CROSS:
				timedCross(time);
				break;
			case SWITCH_AUTO:
				//Refactored to call switchLocation() every iteration to allow for recovery of an error
				while(switchLocation()=='E' && timer.get()<5.0) {
					//do nothing
				}
				reset();
				if (switchLocation() == 'E') {
					toSwitchMid();
				} else if (switchLocation() == station.charAt(0)) {
					sameSideSwitchAuto(time);
				} else if (station == MIDDLE_STATION) {
					middleSwitchAuto(time);
				} else {
					oppositeSideSwitchAuto(time);
				}
				break;
			case WIP_CENTER:
				middleSwitchAuto(time);
				middleSwitchAutoExp(time);
				break;
			}
		}).start();
	}

	private void timedCross(double time) {
		if (time < 7.5) {
			drive.stop();
		} else if (time < 11) {
			drive.arcade(DRIVE_SPEED, 0);
		} else {
			drive.stop();
		}
	}

	/*
	 * In the event of an error drive up to the middle of the switch
	 * This will allow us to score quickly if it recovers or get us across
	 * the line if it does not
	 */
	private void toSwitchMid() {
		gyroDriveForward(DRIVE_SPEED, DISTANCE_TO_SWITCH_MID);
	}

	//TODO Tune
	private void middleSwitchAuto(double time) {
		while(timer.get()<0.5 && isAuto()) {
			drive.arcade(0.5, 0);
		}
		reset();
		while(!goodEnoughTurn(selectAngle()) && timer.get()<1.5) {
			gyroTurn(TURN_SPEED, selectAngle());
		}
		reset();
		while(!goodEnoughDrive(HYPOTONUSE) && timer.get()<2.5) {
			gyroDriveForward(DRIVE_SPEED, HYPOTONUSE);
		}
		reset();
		while(!goodEnoughTurn(-selectAngle()) && timer.get()<1.5) {
			gyroTurn(TURN_SPEED, -selectAngle());
		}
		reset();
		while(timer.get()<0.5 && isAuto()) {
			drive.arcade(0.6, 0);
			hand.score();
		}
		reset();
		while(timer.get()<1.5 && isAuto()) {
			fingers.setSpeed(1.0);
		}
		fingers.setSpeed(0);
		hand.stow();
		reset();
	}

	//TODO Test and tune
	private void middleSwitchAutoExp(double time) {
		
		
		/*
			} else if (time < 8.25) { //END ONE CUBE
				fingers.setSpeed(1.0);
			} else if (time < 8.75) {
				fingers.setSpeed(0);
				drive.arcade(-0.5, 0);
			} else if (time < 10) {
				gyroTurn(TURN_SPEED, -selectAngle() * 2);
			} else if (time < 11) {
				hand.extend();
				fingers.setSpeed(-1);
				gyroDriveForward(DRIVE_SPEED, SWITCH_LENGTH / 1.5);
			} else if (time < 12.5) {
				if (!firstDone) {
					hw.resetEncoders();
					firstDone = true;
				}
				hand.stow();
				fingers.setSpeed(0);
				gyroDriveForward(-DRIVE_SPEED, SWITCH_LENGTH / 1.5);
			} else if (time < 14) {
				gyroTurn(TURN_SPEED, selectAngle() / 2);
			} else {
				drive.arcade(0.5, 0);
				//hand.score();
				//if(time<14.5) 
				//fingers.setSpeed(1);
			}
		} else {
			timer.reset();
		}
		*/
	}

	private void sameSideSwitchAuto(double time) {
		while(!goodEnoughDrive(DISTANCE_TO_SWITCH_MID) && timer.get()<4.5) {
			gyroDriveForward(DRIVE_SPEED, DISTANCE_TO_SWITCH_MID);
		}
		while(hasError) {
			switchLocation();
		}
		reset();
		while(!goodEnoughTurn(selectAngle()) && timer.get()<2.5) {
			gyroTurn(TURN_SPEED, selectAngle());
		}
		reset();
		while(timer.get()<1 && isAuto()) {
			drive.arcade(0.5, 0);
		}
		reset();
		while(timer.get()<1 && isAuto()) {
			drive.stop();
			hand.score();
		} 
		reset();
		while(timer.get()<2) {
			fingers.setSpeed(1);
		}
	}

	//TODO Test and tune
	private void oppositeSideSwitchAuto(double time) {
		while(!goodEnoughDrive(DISTANCE_TO_SWITCH_BACK) && timer.get()<4.0) {
			gyroDriveForward(DRIVE_SPEED, DISTANCE_TO_SWITCH_BACK + ROBOT_LENGTH / 2);
		}
		while(hasError) {
			switchLocation();
		}
		reset();
		while(!goodEnoughTurn(selectAngle()) && timer.get()<2.5) {
			gyroTurn(TURN_SPEED, selectAngle());
		}
		reset();
		while(!goodEnoughDrive(SWITCH_LENGTH) && timer.get()<3.0) {
			gyroDriveForward(DRIVE_SPEED+0.15, SWITCH_LENGTH);
		}
		reset();
		while(!goodEnoughTurn(selectAngle()) &&  timer.get()<2.5) {
			gyroTurn(TURN_SPEED, selectAngle());
		}
		reset();
		while(timer.get()<1.0 && isAuto()) {
			drive.arcade(0.5, 0);
			hand.score();
		}
		reset();
		while(timer.get()<2.0 && isAuto()) {
			fingers.setSpeed(1);
		}
	}

	private char switchLocation() {
		String locations = DriverStation.getInstance().getGameSpecificMessage();
		if (locations.length() == 3) {
			hasError = false; //There is a chance that the error is recoverable later on.
			return locations.charAt(0);
		}
		hasError = true;
		return 'E'; //If the GameSpecificMessage is empty return an error state
	}

	private int selectAngle() {
		char location = switchLocation();
		if (location != 'E') {
			boolean isLeft = (location == 'L');
			switch (STATION.getSelected()) {
			case (MIDDLE_STATION):
				if (isLeft) {
					return MID_LEFT;
				} else {
					return MID_RIGHT;
				}
			case (LEFT_STATION):
				return RIGHT_TURN;
			default:
				return LEFT_TURN;
			}
		}
		return 0; //In the event that there is an error do not turn
	}

	private boolean goodEnoughDrive(double target) {
		if(!isAuto()) // Effectively skips all steps if auto has ended
			return true;
		return Math.abs(hw.getDistance()) <= 4 && hw.driveAtRest();
	}
	
	private boolean goodEnoughTurn(double angle) {
		if(!isAuto())
			return true;
		return Math.abs(hw.gyro.getAngle())<2.5 && hw.driveAtRest();
	}
	
	private boolean isAuto() {
		return ds.isAutonomous();
	}
	
	private void reset() {
		hw.resetSensors();
		timer.reset();
	}

	private void lastAngle() {
		if (!turnDone) {
			lastAngle = hw.gyro.getAngle();
		}
	}

	private double normalize(double value, double max) {
		return Math.max(-max, Math.min(value, max));
	}

	private void gyroDriveForward(double speed, double distance) {

		double angle = -hw.gyro.getAngle() + lastAngle;
		double dist = hw.getDistance();
		//if (distance > 0 && dist < distance)
		if (goodEnoughDrive(distance)) {
			drive.arcade(0, 0);
		} else if (dist < distance) {
			drive.arcade(speed, normalize(angle / 100.0, speed));
		} else if (dist > distance) {
			//else if (distance <= 0 && dist > distance)
			drive.arcade(-speed, normalize(angle / 100.0, speed));
		}

	}

	private void gyroTurn(double speed, double targetAngle) {
		double currentAngle = hw.gyro.getAngle();

		double turn = (targetAngle - currentAngle) / 10.0;

		drive.arcade(0, normalize(turn, speed));
		hw.resetEncoders();
	}
}