/* Copyright (c) 2018 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package team13813.mode;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.util.ArrayList;

import team13813.io.FileSerialization;
import team13813.io.GamepadManager;
import team13813.motion.MotionManager;
import team13813.state.GoldPositions;
import team13813.state.State;
import team13813.util.Configuration;
import team13813.vision.VisionManager;

/**
 * This 2018-2019 OpMode illustrates the basics of using the TensorFlow Object Detection API to
 * determine the position of the gold and silver minerals.
 *
 * Use Android Studio to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this opmode to the Driver Station OpMode list.
 *
 * IMPORTANT: In order to use this OpMode, you need to obtain your own Vuforia license key as
 * is explained below.
 */
@TeleOp(name = "KokiAutoNext", group = "OpMode")
@Disabled
// TODO: WARNING - When there is an issue updating the opMode, try: Build->Clean Project
public class KokiAutoNext extends OpMode {
    // Declare OpMode members.
    // 1000ticks = 1sec


    // variables in the father class
    // time = 0.0;
    // gamepad1;
    // gamepad2;
    // telemetry;
    // hardwareMap;
    // startTime;

    // functions in the father class
    // OpMode()
    // init() This method will be called once when the INIT button is pressed.
    // init_loop() This method will be called repeatedly once when the INIT button is pressed.
    // start() This method will be called once when this op mode is running.
    // loop() This method will be called repeatedly in a loop while this op mode is running.
    // stop() This method will be called when this op mode is first disabled
    // requestOpModeStop() Shutdown the current OpMode
    // resetStartTime() set time to 0
    // internalPreInit(); internalPostInitLoop(); internalPostLoop();

    private MotionManager motionManager;
    private VisionManager visionManager;
    private GamepadManager gamepadManager;
    private ArrayList<GoldPositions> positionStream = new ArrayList<>();
    private String debugMessage = "";

    public void setTeam() {
        throw new UnsupportedOperationException("setTeam() method not implemented!");
    }
    public void setFacing() {
        throw new UnsupportedOperationException("setTemprename() method not implemented!");
    }
    public void setState() {
        throw new UnsupportedOperationException("setState() method not implemented!");
    }

    @Override
    public void init() {
        super.msStuckDetectInit     = 5000;
        super.msStuckDetectInitLoop = 5000;
        super.msStuckDetectStart    = 5000;
        super.msStuckDetectLoop     = 5000;
        super.msStuckDetectStop     = 10000;

        setTeam();
        setFacing();
        setState();

        visionManager = new VisionManager(hardwareMap, Configuration.INFER, Configuration.flashLight);
        motionManager = new MotionManager(telemetry, hardwareMap);
        gamepadManager = new GamepadManager(telemetry);
        visionManager.start();
        telemetry.addData("DEBUG", debugMessage);
        telemetry.update();
    }

    @Override
    public void init_loop() {
        long lStartTime = System.currentTimeMillis();

        // update [GoldPositions laskKnown]
        GoldPositions positions = visionManager.fetch();
        if (positionStream.size() > Configuration.maximumStream)
            positionStream.remove(0);
        positionStream.add(positions);
        GoldPositions _ = calculateLastKnown(positionStream);
        if (_ != GoldPositions.UNKNOWN) {
            Configuration.setGoldPosition(_);
            telemetry.addData("Sensor", "randomPosition = %s", Configuration.getGoldPosition());
        }

        long timeElapsed = System.currentTimeMillis() - lStartTime;
        telemetry.addData("Record", "timeElapsed = %d", timeElapsed);
        telemetry.addData("DEBUG", debugMessage);
        telemetry.update();
    }

    @Override
    public void start() {
//        if (Configuration.getGoldPosition() == GoldPositions.UNKNOWN) throw new InvalidParameterException("GoldPositions cannot be 'UNKNOWN'");
        visionManager.disable();
        resetStartTime();
        if (Configuration.getState() == State.AUTONOMOUS) {
            debugMessage = debugMessage + "Getting file in " + Configuration.getFileName() + "; ";

            Object _ = FileSerialization.loadInternal(hardwareMap.appContext, Configuration.getFileName(), telemetry);
            if (_ != null) {
                Configuration.gamepadsTimeStream = (ArrayList<GamepadManager>) _;
                debugMessage = debugMessage + "File != null; ";
            } else {
                debugMessage = debugMessage + "File == null; Object is null; ";
            }
        }
        telemetry.addData("DEBUG", debugMessage);
        telemetry.update();
    }

    @Override
    public void loop() {
        telemetry.addData("Record", "timeElapsed = %f", time);
        if (Configuration.getState() == State.CONTROL) {
            gamepadManager.update(gamepad1, gamepad2);
        } else if (Configuration.getState() == State.RECORDING) {
            gamepadManager.update(gamepad1, gamepad2);
            Configuration.gamepadsTimeStream.add(gamepadManager.clone());
        } else if (Configuration.getState() == State.AUTONOMOUS && Configuration.gamepadsTimeStream.size() >0) {
            telemetry.addData("Running Record:", Configuration.getFileName());
            gamepadManager = Configuration.gamepadsTimeStream.get(0);
            Configuration.gamepadsTimeStream.remove(0);
        }
        motionManager.updateWithException(gamepadManager);
//        motionManager.update(gamepadManager);
        telemetry.addData("DEBUG", debugMessage);
        telemetry.update();
    }

    @Override
    public void stop() {
        if (Configuration.getState() == State.RECORDING) {
            boolean successful = FileSerialization.saveInternal(hardwareMap.appContext, Configuration.getFileName(), Configuration.gamepadsTimeStream, telemetry);
            debugMessage = debugMessage + "Configuration saved in " + Configuration.getFileName() + "; Successful = " + Boolean.toString(successful) + "; ";
//            String s = FileSerialization.readInternal(hardwareMap.appContext, Configuration.getFileName(), telemetry);
//            FileSerialization.setClipboard(hardwareMap.appContext, s);
        }
        telemetry.addData("Time", "time = %f", time);
        telemetry.addData("DEBUG", debugMessage);
        telemetry.update();
    }

    private static GoldPositions calculateLastKnown(ArrayList<GoldPositions> positionStream) {
        for (GoldPositions pos: positionStream) {
            if (positionStream.get(0) != pos) {
                return GoldPositions.UNKNOWN;
            }
        }
        return positionStream.get(0);
    }
}