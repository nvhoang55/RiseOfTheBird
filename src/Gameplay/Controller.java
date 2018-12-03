package Gameplay;

import GameObject.Bird;
import edu.princeton.cs.introcs.StdDraw;

import java.io.File;
import java.util.ArrayList;

public abstract class Controller {

    public static void reset()
    {
        Power.reset();
        Angle.reset();
    }

    //Power Gameplay.Controller
    public static final class Power {

        private static ArrayList<String> powerBarFrames = new ArrayList<>();
        private static int currentFrame = 0;
        private static double currentVelocity = 0;
        private static int changeLevel = -1;

        static
        {
            for (int frameIdx = 1; frameIdx <= 12; frameIdx++)
            {
                powerBarFrames.add(new File("src\\Model\\ControllerModel\\Power bar\\powerbar (" + frameIdx + ").png").getAbsolutePath());
            }
        }

        private static void reset()
        {
            currentFrame = 0;
            currentVelocity = 0;
            changeLevel = -1;
        }

        public static void setInitialVelocity(Bird bird)
        {
            bird.setVelocity(currentVelocity);
        }

        public static void getInitialVelocityByPlayer()
        {
            if (currentFrame % (powerBarFrames.size() - 1) == 0)
            {
                changeLevel *= -1;   //change direction of power flow if it reaches the bar's edges
            }
            currentFrame += changeLevel;
            currentVelocity = currentFrame * 20;

            StdDraw.picture(0, -Game.getBackground().getHeight() / 2 + 50, Power.powerBarFrames.get(currentFrame), Game.getBackground().getWidth(), 50);
        }
    }

    //Shooting Angle Gameplay.Controller
    public static final class Angle {

        private static ArrayList<String> angleModelPath = new ArrayList<>();
        private static final String ARROW_MODEL_PATH = "F:\\IntelliJ IDEA 2018.2.1\\Workspace\\GunnyChick\\src\\Model\\ControllerModel\\Angle bar\\arrow.png";
        private static int currentFrame = 0;
        private static final int MAX_ANGLE = 75;
        private static double currentAngle = 0;
        private static int changeLevel = 1;

        static
        {
            for (int angle = 0; angle <= 360; angle++)
            {
                angleModelPath.add("" + angle + " degree");
            }
        }

        private static void reset()
        {
            currentAngle = 0;
            currentFrame = 0;
        }

        public static void setShootingAngle(Bird bird)
        {
            bird.setCurrentAngle(currentAngle);
        }

        public static void getShootingAngleByPlayer()
        {
            if (currentAngle >= MAX_ANGLE || currentAngle < 0) changeLevel *= -1;
            currentAngle += 3 * changeLevel;
        }

        public static void performCurrentShootingAngle(Bird bird)
        {
            double arrowX = bird.getInitialCoordinate().getX() + bird.getModelSize() + 25;
            double arrowY = bird.getInitialCoordinate().getY();

            StdDraw.picture(arrowX, arrowY, ARROW_MODEL_PATH, 300, 150, currentAngle);
        }
    }
}