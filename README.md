# Rise of the Bird

An Angry Birds-style game where the player launches a roster of birds at the
boss "Mouseleficent". Originally a Java midterm project for HUST's
*Introduction to Java* course; this branch is a modernised refactor in
**data-oriented Java** that runs stand-alone on any machine with a JDK 21+ —
no IDE required.

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

### Run the tests

The pure logic layer is fully unit-tested with JUnit 5 — no AWT, no canvas,
no asset files involved.

```
./mvnw test
```

### Custom roster

The default roster is two Thords followed by two Bulks. Override via system
property:

```
java -Droster=THORD,BULK,THORD -jar target/rise-of-the-bird-1.0.0.jar
```

Unknown character names are skipped with a warning. Available characters
are `THORD` and `BULK`.

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
│       ├── App.java              main(): parses roster, builds Bird list,
│       │                         hands it to the runtime GameLoop
│       ├── data/                 PURE DATA - records, sealed types, enums
│       │   ├── Vec2.java         double-precision 2D vector
│       │   ├── Form.java         enum REGULAR | USING_SKILL
│       │   ├── Phase.java        enum READY | AIMING_ANGLE | AIMING_POWER
│       │   │                     | FLYING | GAME_OVER
│       │   ├── BirdState.java    shared in-flight state every bird carries
│       │   ├── Bird.java         sealed interface permits Thord, Bulk
│       │   ├── Thord.java        record (BirdState, Lightning) implements Bird
│       │   ├── Bulk.java         record (BirdState, int size) implements Bird
│       │   ├── Lightning.java    Thord's skill projectile record
│       │   ├── Mouse.java        boss record (HP, position, oscillation)
│       │   ├── ControllerState.java  angle / power / skill UI state record
│       │   ├── InputSnapshot.java    keyboard snapshot with edge accessors
│       │   ├── Hit.java          sealed interface permits Hit.Landed, Hit.Missed
│       │   └── World.java        top-level game state record
│       ├── logic/                PURE FUNCTIONS over data; no I/O
│       │   ├── Birds.java        advance, knockBack, useSkill (sealed switch)
│       │   ├── Lightnings.java   advance + collision step
│       │   ├── Mice.java         advance, takeDamage, collide -> Hit
│       │   ├── Controllers.java  tickAngle, applyAngle, tickPower, ...
│       │   └── Worlds.java       tick(World, InputSnapshot) -> World
│       ├── input/                IMPURE: reads StdDraw, returns InputSnapshot
│       │   └── Input.java        Input.poll(prev) -> InputSnapshot
│       ├── render/               IMPURE: reads World, calls StdDraw
│       │   ├── Background.java   canvas + background sprite
│       │   └── Renderer.java     Phase + Bird sealed-switch dispatch
│       ├── runtime/              IMPURE: the loop; the only place with `while`
│       │   └── GameLoop.java     holds `World current`, drives the tick
│       └── assets/
│           ├── Assets.java       classpath asset resolver
│           └── Sprites.java      sprite-path constants
├── src/main/resources/assets/
│   ├── background/               ragnarok_background.jpg
│   ├── bird/thord/               regular_form.png, using_skill_form.png
│   ├── controller/angle/         arrow.png
│   ├── controller/power/         a.png ... k.png
│   ├── menu/                     won.jpg, lost.jpg
│   └── mouse/mouseleficent/      mouseleficent_{1,2,3}hp.png
└── src/test/java/com/riseofthebird/
    ├── data/RecordValidationTest.java   compact-ctor + immutability checks
    └── logic/WorldsTickTest.java        end-to-end pure-tick assertions
```

Bulk's sprites and Thord's lightning frames were not shipped with the
original repo. The asset loader treats them as optional, so the game runs
without them — Bulk renders invisibly while still applying physics, and
Thord's lightning damages mice without an on-screen effect. Drop authored
PNGs at the expected paths under `src/main/resources/assets/` to enable them.

## Architecture — data-oriented Java

The codebase follows the Goetz / Parlog data-oriented programming style for
modern Java (sealed types + records + exhaustive pattern-matching `switch`),
with a clean **functional core / imperative shell** split:

```
   data/        PURE DATA       records, sealed types, enums  (no behaviour)
     ▲
     │ reads
     │
   logic/       PURE FUNCTIONS  static fns over data           (no I/O)
     ▲
     │ called by
     │
   runtime/     IMPERATIVE      GameLoop holds `World current`
   render/                      Renderer reads World -> StdDraw
   input/                       Input.poll reads StdDraw -> InputSnapshot
```

The four DOP principles, mapped onto this codebase:

1. **Model data immutably and transparently.** Every entity is a `record`
   with public components and zero encapsulation. State transitions return
   new records via `with*` constructors; nothing in `data/` or `logic/` ever
   mutates an existing value.
2. **Model the data, the whole data, and nothing but the data.** `data/`
   contains records, sealed interfaces, and enums — no methods that
   implement gameplay. Behaviour lives in `logic/`.
3. **Make illegal states unrepresentable.** A `Bulk` structurally cannot own
   a `Lightning` bolt because the type doesn't carry one; `Mouse`'s compact
   constructor refuses negative HP; `Hit.Missed` has no fields, so a "miss
   that somehow damaged a mouse" cannot be expressed.
4. **Separate operations from data.** Every gameplay function (`Birds.advance`,
   `Mice.collide`, `Controllers.tickAngle`, `Worlds.tick`) is a `static`
   method on a `*s` utility class. The data records carry no methods beyond
   `with*` constructors and trivial derived accessors (`isAlive`, `isWon`).

### Game loop

The runtime loop is a tight three-step pipeline at ~30 ms:

```
while (running) {
    lastInput = Input.poll(lastInput);                         // impure
    current   = Worlds.tick(current, lastInput, frameCount);   // PURE
    Renderer.render(current, background);                      // impure
    StdDraw.show();
}
```

`Worlds.tick` dispatches over the round-lifecycle `Phase` enum via an
exhaustive `switch` expression:

```
READY ──SPACE press──▶ AIMING_ANGLE ──SPACE release──▶ AIMING_POWER
                                                            │
                                                            SPACE release
                                                            ▼
GAME_OVER ◀── round ends ── FLYING ◀──────────────── (launch)
   │
   ENTER press ─▶ new run ─▶ READY
```

Adding a new phase forces the compiler to flag every switch over `Phase`.
The same applies to the sealed `Bird` hierarchy: adding a third bird type
forces every `switch (bird)` in `logic/Birds`, `render/Renderer`, and
`runtime/GameLoop` to be updated.

### Why the split pays off

- **Trivial unit tests.** `Worlds.tick(initialWorld, spaceDownSnap)` is a
  pure function. The 50-test suite asserts on score, phase transitions,
  knock-back, skill activation, and replay reset in ~50 ms with no AWT,
  no canvas, no threads.
- **Determinism.** A starting `World` plus a sequence of `InputSnapshot`s
  fully determines the game's evolution, which is the foundation for any
  future replay-recorder feature.
- **No hidden state.** There are no static counters or singleton holders.
  The whole game is one `World` record graph; `System.out.println(world)`
  prints everything that matters.
- **Clear boundary.** The only modules that import `StdDraw` are
  `render/`, `input/`, and `runtime/` (for `enableDoubleBuffering` and
  `show`). The entire `data/` and `logic/` tree is portable, side-effect
  free Java.

## License

See [LICENSE](LICENSE).