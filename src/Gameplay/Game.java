package Gameplay;

import Background.Background;
import GameObject.Bird;
import GameObject.Mouse;
import edu.princeton.cs.introcs.StdDraw;

import java.io.File;
import java.util.ArrayList;

import static java.awt.event.KeyEvent.VK_SPACE;

public final class Game {

    private static final Background background = new Background(1900, 1000);
    private static final ArrayList<Bird> birdList = new ArrayList<>();
    private static final ArrayList<Mouse> mouseList = new ArrayList<>();

    private static int point = 0;

    public static void main(String[] args)
    {
        StdDraw.enableDoubleBuffering();
        background.clear();
        won();
    }

    public static void play()
    {
        for (Bird bird : birdList)
        {
            playOneRound(bird);
        }

        if (point < 3)
        {
            lost();
        } else
        {
            won();
        }
    }

    private static void lost()
    {
        String path = new File("src\\Gameplay\\lost.jpg").getAbsolutePath();
        StdDraw.picture(0, 0, path, background.getWidth(), background.getHeight());
        StdDraw.show();
    }

    private static void won()
    {
        String path = new File("src\\Gameplay\\won.jpg").getAbsolutePath();
        StdDraw.picture(0, 0, path, background.getWidth() / 2, background.getHeight());
        StdDraw.show();
    }

    public static void playOneRound(Bird currentBird)
    {
        StdDraw.enableDoubleBuffering();

        background.clear();

        currentBird.show();
        runAllMouses();
        StdDraw.show();

        setShootingAngle(currentBird);
        setInitialVelocity(currentBird);
        shoot(currentBird);

        reset();
    }

    private static void reset()
    {
        Controller.reset();
        resetHittingStatusOfAllMouses();
    }

    private static void resetHittingStatusOfAllMouses()
    {
        for (Mouse mouse : mouseList)
        {
            mouse.setJustGotHit(false);
        }
    }

    private static void setShootingAngle(Bird currentBird)
    {
        while (true)
        {
            if (StdDraw.isKeyPressed(VK_SPACE))
            {
                while (StdDraw.isKeyPressed(VK_SPACE))
                {
                    Controller.Angle.getShootingAngleByPlayer();
                    Controller.Angle.setShootingAngle(currentBird);

                    background.clear();
                    currentBird.show();
                    Controller.Angle.performCurrentShootingAngle(currentBird);

                    runAllMouses();

                    StdDraw.pause(60);
                    StdDraw.show();
                }
                break;
            }
        }
    }

    private static void setInitialVelocity(Bird currentBird)
    {
        while (true)
        {
            if (StdDraw.isKeyPressed(VK_SPACE))
            {
                while (StdDraw.isKeyPressed(VK_SPACE))
                {
                    background.clear();
                    Controller.Power.getInitialVelocityByPlayer();
                    currentBird.show();

                    runAllMouses();

                    StdDraw.pause(40);
                    StdDraw.show();
                }
                Controller.Power.setInitialVelocity(currentBird);
                break;
            }
        }

    }

    private static void shoot(Bird currentBird)
    {
        while (!currentBird.isOverreached())
        {
            background.clear();
            currentBird.move();

            runAllMouses();

            StdDraw.pause(20);
            StdDraw.show();
        }

    }

    public static void addBird(String[] chickModel, int modelSize)
    {
        addBird(new Bird(-700, -300, chickModel, modelSize));
    }

    private static void addBird(Bird addedBird)
    {
        birdList.add(addedBird);
    }

    public static void addMouse(String[] mouseModel, int modelSize)
    {
        Mouse addedMouse = new Mouse(700, -300, mouseModel, modelSize);
        mouseList.add(addedMouse);
    }

    public static Background getBackground()
    {
        return background;
    }

    private static void runAllMouses()
    {
        for (Mouse mouse : mouseList)
        {
            for (Bird bird : birdList)
            {
                if (mouse.getHP() == 0)
                {
                    mouseList.remove(mouse);
                    return;
                } else if (mouse.getHitBy(bird))
                {
                    mouse.getDamaged();
                    bird.knockBack();
                    point++;
                }
            }
            mouse.move();
        }
    }
}