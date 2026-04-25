# Rise of the Bird

An Angry Birds-style game where the player launches a roster of birds at the
boss "Mouseleficent". Originally a Java midterm project for HUST's
*Introduction to Java* course; this branch is a modernised refactor that runs
stand-alone on any machine with a JDK 21+ — no IDE required.

Rendering uses Princeton's `StdDraw` (vendored in
`src/main/java/edu/princeton/cs/introcs/StdDraw.java`).

## Requirements

- **JDK 21 or newer** on the `PATH` (`java -version` must succeed).
- The bundled **Maven Wrapper** (`./mvnw`) — no system Maven needed; the
  wrapper downloads the right Maven version into `~/.m2/wrapper` on first run.

## Build & run

From the repo root:

```
./mvnw -DskipTests package
java -jar target/rise-of-the-bird-1.0.0.jar
```

The jar at `target/rise-of-the-bird-1.0.0.jar` is fully self-contained: all
assets, classes, and the vendored `StdDraw` are inside it. The project has
zero runtime dependencies, so the plain `maven-jar-plugin` output is already
executable — no shade / uber-jar step is involved. You can copy the jar
anywhere on disk and run it from any directory; there is no working-directory
requirement.

On Windows, replace `./mvnw` with `mvnw.cmd`.

### Run during development

For a quick edit-compile-run cycle without rebuilding the jar:

```
./mvnw -DskipTests compile
./mvnw exec:java
```

### Custom roster

The default roster is two Thords followed by two Bulks. Override via system
property:

```
java -Droster=THORD,BULK,THORD -jar target/rise-of-the-bird-1.0.0.jar
```

Unknown character names are ignored with a warning. Available characters are
the values of `BirdCharacter` (`THORD`, `BULK`).

## Controls

- **SPACE** while at the spawn screen — start aiming.
- **SPACE held** during the angle phase — angle oscillates; release to lock.
- **SPACE held** during the power phase — power oscillates; release to launch.
- **SPACE** while in flight — activate the current bird's skill (one-shot).
- **ENTER** on the win/lose screen — start a new run.

## Project layout

```
RiseOfTheBird/
├── pom.xml                       Maven build descriptor (Java 21, plain jar)
├── mvnw / mvnw.cmd               Maven Wrapper scripts
├── .mvn/wrapper/                 Wrapper jar + properties
├── src/main/java/
│   ├── edu/princeton/cs/introcs/
│   │   └── StdDraw.java          Vendored Princeton StdDraw
│   └── com/riseofthebird/
│       ├── App.java              main(): parses roster, starts the loop
│       ├── assets/
│       │   ├── Assets.java       Classpath asset resolver
│       │   └── Sprites.java      Sprite-path constants
│       ├── core/
│       │   ├── GameObject.java   Base class for drawable world entities
│       │   ├── Bird.java         Projectile motion + skill API
│       │   ├── Bulk.java         Heavyweight bird (grow + drop)
│       │   ├── Thord.java        Fast bird (boost + lightning bolt)
│       │   ├── Lightning.java    Independent projectile spawned by Thord
│       │   └── Mouse.java        Mouseleficent boss
│       ├── game/
│       │   ├── BirdCharacter.java  Enum + per-constant factory
│       │   ├── BirdManager.java    Ordered queue of birds for a run
│       │   ├── MouseManager.java   Live boss list + safe iteration
│       │   ├── Controller.java     Angle / Power / Skill UI state
│       │   └── GameLoop.java       Tick-driven phase state machine
│       ├── input/
│       │   └── InputState.java   Edge-detected keyboard polling
│       └── render/
│           └── Background.java   Canvas + background sprite
└── src/main/resources/assets/
    ├── background/               ragnarok_background.jpg
    ├── bird/thord/               regular_form.png, using_skill_form.png
    ├── controller/angle/         arrow.png
    ├── controller/power/         a.png ... k.png
    ├── menu/                     won.jpg, lost.jpg
    └── mouse/mouseleficent/      mouseleficent_{1,2,3}hp.png
```

Bulk's sprites and Thord's lightning frames were not shipped with the
original repo. The asset loader treats them as optional, so the game runs
without them — Bulk renders invisibly while still applying physics, and
Thord's lightning damages mice without an on-screen effect. Drop authored
PNGs at the expected paths under `src/main/resources/assets/` to enable them.

## Architecture summary

The loop is a single `while` that fires every ~30 ms (`GameLoop.TICK_MS`).
Each tick:

1. `InputState.poll()` — snapshot the keyboard so SPACE / ENTER edges are
   stable for the rest of the tick.
2. `update()` — advance the active `Phase`'s state machine.
3. `render()` — draw the world for the current phase, then `StdDraw.show()`
   flushes the back buffer.

`Phase` transitions are edge-triggered, never key-held:

```
READY ──SPACE press──▶ AIMING_ANGLE ──SPACE release──▶ AIMING_POWER
                                                            │
                                                            SPACE release
                                                            ▼
GAME_OVER ◀── round ends ── FLYING ◀──────────────── (launch)
   │
   ENTER press ─▶ new run ─▶ READY
```

Per-tick game state advances are pure (no canvas writes) so that
`Background.clear()` on each render pass cannot lose a frame. Bird, mouse,
and lightning rendering live in their `show()` / `drawSkillEffect()`
methods, called explicitly during the render pass.

## Bug fixes vs the original

- `Bird.move()` computed the parabola's drop term as `(1 / 2) * g * t * t`,
  which is `0` due to integer division. Restored to `0.5 * g * t * t`.
- `formIndex` on `Bird` latched to `1` once the skill activated and never
  reset, so a bird stayed in "using skill" form for the rest of the game.
  Now reset by `Bird.resetForRound()` between rounds and on replay.
- `MouseConsole.runAllMouses()` mutated its list mid-iteration with an
  enhanced `for` loop, throwing `ConcurrentModificationException` the
  moment a second mouse was added. Rewritten with `Iterator.remove()`.
- `BirdCharacter.VALKYRD` was declared but had no implementation, so adding
  it to a roster silently no-op'd. Removed from the enum until a concrete
  `Bird` subclass exists.
- Asset paths were `new File("src/...").getAbsolutePath()`, hard-binding the
  game to the working directory and reading from a *source* folder at
  runtime. Replaced with classpath lookup so the packaged jar runs anywhere.
- Busy-wait input loops (`while (StdDraw.isKeyPressed(VK_SPACE))`) replaced
  with a single tick-driven loop and edge-detected key snapshots.

## License

See [LICENSE](LICENSE).