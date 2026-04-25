package com.riseofthebird.logic;

import com.riseofthebird.data.Bird;
import com.riseofthebird.data.BirdState;
import com.riseofthebird.data.Bulk;
import com.riseofthebird.data.Form;
import com.riseofthebird.data.Lightning;
import com.riseofthebird.data.Mouse;
import com.riseofthebird.data.Thord;
import com.riseofthebird.data.Vec2;
import java.util.List;

/**
 * Pure functions over {@link Bird} and {@link BirdState}.
 *
 * <p>None of these functions touch StdDraw, the clock, or any global state.
 * Each one takes the relevant data in and returns the updated data out.
 * Skill dispatch goes through an exhaustive {@code switch} over the sealed
 * {@link Bird} hierarchy so adding a new bird kind is a compile-time
 * deliverable.
 */
public final class Birds {

    /** Per-tick increment of the trajectory's time parameter. */
    private static final double TIME_STEP = 0.5;

    /** Knock-back gravity bump applied each time a bird hits a target. */
    private static final double KNOCKBACK_GRAVITY_BUMP = 0.2;

    private Birds() {}

    /**
     * Advances a bird's parabolic trajectory by one tick.
     *
     * <p>Pure: returns a new {@link Bird} with an updated {@link BirdState};
     * does not mutate the input.
     */
    public static Bird advance(Bird bird) {
        return bird.withState(advanceState(bird.state()));
    }

    /**
     * Advances the underlying physics state by one tick. Used directly when
     * the caller already holds a {@link BirdState} and does not need the
     * enclosing {@link Bird} envelope.
     */
    public static BirdState advanceState(BirdState s) {
        double angleRad = Math.toRadians(s.angle());
        double vx = Math.cos(angleRad) * s.velocity();
        double vy = Math.sin(angleRad) * s.velocity() - s.gravity() * s.time();

        double x = vx * s.time() + s.spawn().x();
        double y = vy * s.time() - 0.5 * s.gravity() * s.time() * s.time() + s.spawn().y();

        // Track the heading so the sprite rotates with the trajectory. Guard
        // against a vertical launch (vx == 0) producing NaN via atan(...).
        double newAngle = (vx != 0) ? Math.toDegrees(Math.atan(vy / vx)) : s.angle();

        return s
            .withPos(new Vec2(x, y))
            .withAngle(newAngle)
            .withTime(s.time() + TIME_STEP);
    }

    /**
     * Bounces the bird back when it hits a target: flips its angle, reverses
     * velocity, resets the trajectory clock to the current position, and
     * slightly increases gravity so each subsequent bounce arcs more sharply.
     */
    public static Bird knockBack(Bird bird) {
        BirdState s = bird.state();
        BirdState bounced = new BirdState(
            s.pos(),                           // new spawn = current position
            s.pos(),
            s.angle() + 180,
            -s.velocity(),
            s.gravity() + KNOCKBACK_GRAVITY_BUMP,
            0,                                 // reset trajectory clock
            s.form(),
            s.skillActivated()
        );
        return bird.withState(bounced);
    }

    /**
     * True once the bird has flown off the right edge of the world or below
     * the bottom of the world. The current round ends at that point.
     */
    public static boolean isOverreached(Bird bird, int worldWidth, int worldHeight) {
        double x = bird.state().pos().x();
        double y = bird.state().pos().y();
        int size = sizeOf(bird);
        return (x - size) >= worldWidth || (y - size) <= -worldHeight;
    }

    /** Render and hitbox size for the given bird kind. */
    public static int sizeOf(Bird bird) {
        return switch (bird) {
            case Thord t -> Thord.SIZE;
            case Bulk b -> b.size();
        };
    }

    /**
     * Drives the bird's per-tick skill behaviour while the skill is active.
     * Dispatches over the sealed {@link Bird} hierarchy.
     *
     * @return the post-skill bird, the post-skill mouse list, and the number
     *         of fresh hits the skill scored this tick
     */
    public static SkillResult useSkill(Bird bird, List<Mouse> mice) {
        return switch (bird) {
            case Thord t -> useThordSkill(t, mice);
            case Bulk b -> useBulkSkill(b, mice);
        };
    }

    private static SkillResult useThordSkill(Thord thord, List<Mouse> mice) {
        BirdState s = thord.state();
        Lightning bolt = thord.bolt();

        // First activation tick: latch the skill, apply the velocity boost,
        // and spawn the bolt in front of Thord heading in his current direction.
        if (!s.skillActivated()) {
            s = s
                .withSkillActivated(true)
                .withVelocity(s.velocity() + Thord.VELOCITY_BOOST)
                .withForm(Form.USING_SKILL);
            bolt = bolt.spawnedAt(s.pos().add(100, 0), s.angle());
        }

        // Once struck, the bolt no longer advances or scores.
        if (bolt.struck()) {
            return new SkillResult(new Thord(s, bolt), mice, 0);
        }

        Lightnings.Advance step = Lightnings.advance(bolt, mice);
        return new SkillResult(new Thord(s, step.bolt()), step.mice(), step.hits());
    }

    private static SkillResult useBulkSkill(Bulk bulk, List<Mouse> mice) {
        BirdState s = bulk.state()
            .withSkillActivated(true)
            .withForm(Form.USING_SKILL)
            .withGravity(bulk.state().gravity() + Bulk.GRAVITY_GROWTH_PER_TICK);
        Bulk grown = new Bulk(s, bulk.size() + Bulk.SIZE_GROWTH_PER_TICK);
        return new SkillResult(grown, mice, 0);
    }

    /**
     * Outcome of a per-tick skill invocation: the post-skill bird, the
     * mouse list as it stands after the skill (some mice may have taken
     * damage), and the number of fresh hits the skill scored this tick.
     */
    public record SkillResult(Bird bird, List<Mouse> mice, int hits) {}
}
