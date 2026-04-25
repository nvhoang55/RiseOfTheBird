# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A Java midterm project for HUST's Introduction to Java course — an Angry Birds-style game where birds are launched at a "Mouseleficent" boss across multiple rounds. This branch is a **data-oriented Java** refactor (Java 21, Maven, classpath assets, sealed types + records + pattern matching) that runs stand-alone via `java -jar` with no IDE.

Rendering uses Princeton's `StdDraw`, vendored at `src/main/java/edu/princeton/cs/introcs/StdDraw.java` (no external library dependency).

## Build, run, test

This is a standard Maven project with a bundled wrapper.

- **Build:** `./mvnw -DskipTests package` → `target/rise-of-the-bird-1.0.0.jar`
- **Run jar:** `java -jar target/rise-of-the-bird-1.0.0.jar` (cwd-independent)
- **Run from sources during dev:** `./mvnw -DskipTests compile && ./mvnw exec:java`
- **Run tests:** `./mvnw test` — JUnit 5 suite over the pure logic layer (~50 cases, ~50 ms)
- **Entry point:** `com.riseofthebird.App#main` (`src/main/java/com/riseofthebird/App.java`)
- **JDK:** 21+ (set via `<maven.compiler.release>21</maven.compiler.release>` in `pom.xml`)
- **Custom roster at runtime:** `java -Droster=THORD,BULK,THORD -jar target/...jar`

There is no lint config and no CI in this repo.

## Asset loading model

All gameplay sprites live under `src/main/resources/assets/` and are addressed via `com.riseofthebird.assets.Assets`. The resolver returns paths of the form `/assets/foo/bar.png`, which `StdDraw.getImage(...)` looks up via its built-in classpath fallback (`StdDraw.class.getResource("/" + filename)`). This means:

- The packaged jar is fully self-contained — no asset extraction step at runtime.
- Assets work identically whether running from `target/classes` or from inside the jar.
- The cwd never matters.

`Assets.require(path)` throws if the asset is missing; `Assets.optional(path)` returns `null` and lets the renderer no-op. Bulk's sprites, Thord's lightning frames, the score-bar heart, and the replay menu prompt are all loaded *optionally* because they were never authored in the original repo — the game must continue to run with them missing.

`com.riseofthebird.assets.Sprites` centralises the sprite path arrays. Their indexing is gameplay-coupled:

- Bird arrays: index `0` = regular form, index `1` = using-skill form (matches `data.Form.ordinal()`).
- Mouse arrays: index = current HP (0..3); the renderer picks `frames[hp]` so the sprite changes as the boss takes damage.

## Architecture — data-oriented programming

The codebase follows the Goetz / Parlog DOP style for modern Java with a strict **functional core / imperative shell** split:

```
   data/        PURE DATA       records, sealed types, enums  (no behaviour)
     ▲
     │ reads
     │
   logic/       PURE FUNCTIONS  static fns over data           (no I/O, no globals)
     ▲
     │ called by
     │
   runtime/     IMPERATIVE      GameLoop holds `World current`
   render/                      Renderer reads World -> StdDraw
   input/                       Input.poll reads StdDraw -> InputSnapshot
```

### Module layout

The main flow lives in `src/main/java/com/riseofthebird/`:

- **`App`** — parses optional `-Droster=...`, builds `List<Bird>` of fresh at-spawn birds via a small private `BirdKind` parser enum, and constructs `runtime.GameLoop`.
- **`data/`** — every record, sealed interface, and enum. Contains zero behaviour beyond `with*` constructors and trivial derived accessors (`isAlive`, `isWon`, `hasMoreBirds`).
- **`logic/`** — every gameplay function as `static` methods on `*s`-named utility classes (`Birds`, `Mice`, `Lightnings`, `Controllers`, `Worlds`). Pure: no `StdDraw`, no clock reads, no static mutable state.
- **`input/Input`** — `static InputSnapshot poll(InputSnapshot prev)`. The only impure step is reading `StdDraw.isKeyPressed`; the returned snapshot is an immutable value with edge-detection accessors.
- **`render/Renderer`** — `static void render(World, Background)`. Dispatches over `Phase` and the sealed `Bird` hierarchy via exhaustive `switch` expressions and writes `StdDraw` calls. Holds zero instance state.
- **`runtime/GameLoop`** — the only module with a `while` loop. Holds exactly two pieces of mutable state: `World current` and `InputSnapshot lastInput`. Each tick: `Input.poll` → `Worlds.tick` → `Renderer.render` → `StdDraw.show`.

### Data layer (`data/`)

| Type | Kind | Purpose |
|---|---|---|
| `Vec2` | record | 2D double vector; replaces `java.awt.Point`. NaN rejected in compact ctor. |
| `Form` | enum | `REGULAR`, `USING_SKILL` — sprite frame index. |
| `Phase` | enum | Round lifecycle: `READY`, `AIMING_ANGLE`, `AIMING_POWER`, `FLYING`, `GAME_OVER`. |
| `BirdState` | record | Shared in-flight state every bird carries (spawn, pos, angle, velocity, gravity, time, form, skillActivated). |
| `Bird` | sealed interface | `permits Thord, Bulk`. Defines `state()` and `withState(BirdState)`. |
| `Thord` | record | `(BirdState state, Lightning bolt)` — fast bird with an independent projectile. |
| `Bulk` | record | `(BirdState state, int size)` — heavyweight bird with a growing hitbox. |
| `Lightning` | record | Thord's projectile (pos, angle, spawned, struck). `DORMANT` is the pre-spawn singleton. |
| `Mouse` | record | Boss state (HP, position, oscillation step, distance, latch). HP range checked in compact ctor. |
| `ControllerState` | record | Per-round angle/power/skill UI state. |
| `InputSnapshot` | record | Current + previous tick keyboard state with edge accessors (`wasSpacePressed`, etc.). |
| `Hit` | sealed interface | `permits Hit.Landed, Hit.Missed`. Collision result; `Landed` carries the post-damage `Mouse`. |
| `World` | record | Top-level game state: roster, currentBird, mice, controller, phase, score, playerWon. Lists are defensively copied in the compact ctor. |

### Logic layer (`logic/`)

Every function is `static`, takes data in, returns data out, and never reads or writes global state.

- **`Birds`** — `advance(Bird)`, `advanceState(BirdState)`, `knockBack(Bird)`, `isOverreached(Bird, w, h)`, `sizeOf(Bird)`, `useSkill(Bird, List<Mouse>) -> SkillResult`. `useSkill` dispatches via a switch on the sealed `Bird` hierarchy, so adding a new bird kind is a compile-time deliverable here.
- **`Lightnings`** — `advance(Lightning, List<Mouse>) -> Advance`. Steps the bolt and runs collision against every live mouse in one pass.
- **`Mice`** — `advance(Mouse)`, `takeDamage(Mouse)`, `collide(Mouse, Bird) -> Hit`, `clearHitLatch(Mouse)`. Returns the sealed `Hit` rather than a boolean + side effects.
- **`Controllers`** — `tickAngle`, `applyAngle`, `tickPower`, `applyPower`, `activateSkill`. Each takes a `ControllerState` and returns a new one, plus an `applyTo` style for writing the locked value into a `Bird`.
- **`Worlds`** — `tick(World, InputSnapshot, int powerFrameCount) -> World`. The single entry point the runtime calls each frame. Top-level dispatch is an exhaustive `switch (world.phase())`; per-phase handlers compose calls into the other logic classes.

### Game loop (`runtime/GameLoop`)

Tight three-step pipeline at fixed `TICK_MS = 30` ms:

```
while (running) {
    lastInput = Input.poll(lastInput);                         // impure
    current   = Worlds.tick(current, lastInput, frameCount);   // PURE
    Renderer.render(current, background);                      // impure
    StdDraw.show();
}
```

Phase transitions happen inside `Worlds.tick`; they fire on edge events (`wasSpacePressed`, `wasSpaceReleased`, `wasEnterPressed`), never on raw key-held state, so a single SPACE tap cleanly advances exactly one phase:

```
READY ─SPACE press─▶ AIMING_ANGLE ─SPACE release─▶ AIMING_POWER ─SPACE release─▶ FLYING
                                                                                    │
                                                                            round ends
                                                                                    ▼
                  (next bird) READY  ◀──────  endRound()  ──────▶  GAME_OVER ─ENTER─▶ new run
```

### Rendering model

All drawing goes through `StdDraw` static calls. The canvas coordinate system is set once in `Background`'s constructor: `setXscale(-width/2.0, width/2.0)`, `setYscale(-height/2.0, height/2.0)` — origin is **center of screen**. `StdDraw.enableDoubleBuffering()` is called once in `GameLoop`'s constructor; `StdDraw.show()` flushes after each tick.

`Worlds.tick` and `Renderer.render` are strictly separated: nothing in `logic/` or `data/` ever touches the canvas. The render pass clears the background, then re-paints every visible entity by reading `World`. This invariant is what allows a single `Background.clear()` per frame without losing sprites mid-frame, and is also what makes the entire logic layer testable as a pure function.

## Test layer (`src/test/java/`)

Two suites, all pure value comparisons (no AWT, no canvas, no asset files, no threads).

- **`data/RecordValidationTest`** (28 cases) — compact-constructor checks (`Vec2` rejects NaN, `Mouse` rejects negative or out-of-range HP, `World` rejects null fields and out-of-range `currentBird`/`score`), defensive list copies (`World` snapshots roster and mice), `with*` immutability, and the singleton/identity properties of `Hit.Missed.INSTANCE` and `Lightning.DORMANT`.
- **`logic/WorldsTickTest`** (22 cases) — drives `Worlds.tick` through every phase transition, scoring path, skill path, and replay reset. Each test seeds a `World`, ticks it once or a few times with synthetic `InputSnapshot` values, and asserts on the returned `World`. Includes a purity check that `tick` returns a new instance and does not mutate its input.

Running `./mvnw test` should always be green; if it goes red, fix the test or fix the code before committing.

## Things to watch when modifying

- **Adding a new bird kind:** add a record under `data/` that implements the sealed `Bird` interface (and add it to the `permits` list). The compiler will then refuse to compile every existing `switch (bird)` until you handle the new case — `logic/Birds.useSkill`, `render/Renderer.renderBird`, `logic/Worlds.respawn`, `runtime/GameLoop.freshenRoster`, and `App.BirdKind`.
- **Adding a new phase:** add the constant to `data/Phase`. The compiler will then refuse to compile `Worlds.tick`'s top-level switch and `Renderer.render`'s phase switch until you handle the new case.
- **Adding a new mouse / boss:** drop a fresh `Mouse` value into `World.mice()`; the logic layer iterates the list generically. There is no special-case code path for individual boss kinds today.
- **Adding a new asset:** drop it under `src/main/resources/assets/<group>/` and reference it via `Assets.require(...)` (mandatory) or `Assets.optional(...)` (graceful no-op). It will be picked up by the next `mvn package` and shipped inside the packaged jar.
- **Skill that spawns a projectile:** see `Thord` + `Lightning` for the canonical pattern. The projectile is a record stored as a component on the bird's record, the physics live in `logic/Lightnings`, and the renderer's bird-switch case for the new bird kind is responsible for drawing the effect.
- **Mutability rule:** nothing in `data/` or `logic/` should mutate any field, period. State transitions return new records via `with*` constructors. The only mutable state in the entire codebase is `GameLoop.current` (a `World`) and `GameLoop.lastInput` (an `InputSnapshot`). If you find yourself reaching for a mutable field anywhere else, reconsider.
- **Phase transitions:** only add transitions inside `Worlds.tick`'s phase handlers. Never transition during `Renderer.render`.
- **Tick rate** is fixed at `GameLoop.TICK_MS` (30 ms). Per-phase pauses no longer exist; visual speeds are uniform.
- **`BirdState.pos` is recomputed each tick** from `vx*time + spawn.x()`. Setting `pos` on its own does not survive the next `Birds.advance` call; tests and any code that needs a bird at a specific world location must set `spawn` (the trajectory anchor), not `pos`. See the inline comments in `WorldsTickTest.flying_overreached_*` for the captured gotcha.
- **At `time == 0`** the parabola yields exactly the spawn point, so the first FLYING tick after launch reports no displacement. This matches the original game; tests that observe motion seed `time = 1` to skip the no-op first tick.
- **Asset paths** are case-sensitive on Linux. Lower-case all new resource paths.
- **`with*` constructors on records:** they exist because Java 21 lacks a `with` keyword (JEP 468 is preview). Each one returns a new record with the named component replaced — this is *not* re-encapsulation, it is a shorthand for `new Foo(a, b, newC, d, e)`. Do not add behaviour beyond field replacement to these.