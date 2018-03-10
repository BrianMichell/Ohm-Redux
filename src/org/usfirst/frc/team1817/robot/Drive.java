package org.usfirst.frc.team1817.robot;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;

public class Drive implements Runnable {
    private final double DEADZONE = 0.1;
    private final double RAMP = 0.08;

    private int state;
    private final int DISABLED = 0;
    private final int ENABLED = 1;

    private int mode;
    private final int TANK = 0;
    private final int ARCADE = 1;

    private DifferentialDrive chassis;
    private double leftOrPower, rightOrTurn;
    private final Thread t;

    public Drive(Hardware hw) {
        state = DISABLED;
        mode = ARCADE;

        this.chassis = hw.chassis;

        leftOrPower = 0.0;
        rightOrTurn = 0.0;

        t = new Thread(this, "Drive");
        t.start();
    }

    public void run() {
        while (!Thread.interrupted()) {
            switch (state) {
            case DISABLED:
                chassis.stopMotor();
                leftOrPower = 0.0;
                rightOrTurn = 0.0;
                break;
            case ENABLED:
                drive();
                break;
            }

            Timer.delay(0.005);
        }
    }

    public void drive() {
        leftOrPower = deadband(leftOrPower);
        rightOrTurn = deadband(rightOrTurn);

        switch (mode) {
        case TANK:
            chassis.tankDrive(leftOrPower, rightOrTurn);
            break;
        case ARCADE:
            chassis.arcadeDrive(leftOrPower, rightOrTurn);
            break;
        }
    }

    public void disable() {
        state = DISABLED;
    }

    public void enable() {
        state = ENABLED;
    }

    public void tank(double left, double right) {
        mode = TANK;

        double deltaL = Math.max(Math.min(left - leftOrPower, RAMP), -RAMP);
        double deltaR = Math.max(Math.min(right - rightOrTurn, RAMP), -RAMP);

        leftOrPower += deltaL;
        rightOrTurn += deltaR;
    }

    public void arcade(double power, double turn) {
        mode = ARCADE;

        double deltaP = Math.max(Math.min(power - leftOrPower, RAMP), -RAMP);
        double deltaT = Math.max(Math.min(turn - rightOrTurn, RAMP), -RAMP);

        leftOrPower += deltaP;
        rightOrTurn += deltaT;
    }

    private double deadband(double value) {
        return Math.abs(value) > DEADZONE ? value : 0.0;
    }
}