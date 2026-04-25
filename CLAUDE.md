# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A Java midterm project for HUST's Introduction to Java course — an Angry Birds-style game where birds are launched at a "Mouseleficent" boss across multiple rounds. This branch is a modernised refactor (Java 21, Maven, classpath assets) that runs stand-alone via `java -jar` with no IDE.

Rendering uses Princeton's `StdDraw`, vendored at `src/main/java/edu/princeton/cs/introcs/StdDraw.java` (no external library dependency).

## Build & run

This is a standard Maven project with a bundled wrapper.

- **Build:** `./mvnw -DskipTests package` → `target/rise-of-the-bird-1.0.0.jar`
- **Run jar:** `java -jar target/rise-of-the-bird-1.0.0.jar` (cwd-independent)
- **Run from sources during dev:** `./mvnw -DskipTests compile && ./mvnw exec:java`
- **Entry point:** `com.riseofthebird.App#main` (`src/main/java/com/riseofthebird/App.java`)
- **JDK:** 21+ (set via `<maven.compiler.release>21</maven.compiler.release>` in `pom.xml`)
- **Custom roster at runtime:** `java -Droster=THORD,BULK,THORD -jar target/...jar`

There are no tests, no lint config, and no CI in this repo.

## Asset loading model

All gameplay sprites and audio live under `src/main/resources/assets/` and are addressed via `com.riseofthebird.assets.Assets`. The resolver returns paths of the form `/assets/foo/bar.png`, which `StdDraw.getImage(...)` looks up via its built-in classpath fallback (`StdDraw.class.getResource("/" + filename)`). This means:

- The shaded jar is fully self-contained — no asset extraction step at runtime.
- Assets work identically whether running from `target/classes` or from inside the jar.
- The cwd never matters.

`Assets.require(path)` throws if the asset is missing; `Assets.optional(path)` returns `null` and lets the renderer no-op. Bulk's sprites, Thord's lightning frames, the score-bar heart, and the replay menu prompt are all loaded *optionally* because they were never authored in the original repo — the game must continue to run with them missing.

`com.riseofthebird.assets.Sprites` centralises the sprite path arrays. Their indexing is gameplay-coupled:

- Bird arrays: index `0` = regular form, index `1` = using-skill form.
- Mouse arrays: index = current HP (0..3); the mouse renderer picks `frames[hp]` so the sprite changes as the boss takes damage.

## Architecture

### Object hierarchy

- `com.riseofthebird.core.GameObject` (abstract) — base for anything drawn on the canvas. Holds `initialCoordinate`, `currentCoordinate`, `modelPath` (frame array), `modelSize`. Subclasses implement `move()` (pure physics, no draw) and `show()` (pure draw, no state advance).
  - `core.Bird` (abstract) — adds projectile motion: `velocity`, `currentAngle`, `gravity`, `time`, `formIndex`. `move()` advances a parabolic trajectory each tick. `useSkill(List<Mouse>)` is abstract and **returns the number of fresh hits the skill scored this tick**, so skills with independent projectiles report damage without the game loop having to know about each subclass. `drawSkillEffect()` is an overridable hook for skill projectiles to render during the render pass.
    - `core.Bulk` — skill grows the model and gravity; returns 0 hits.
    - `core.Thord` — skill grants a one-shot velocity boost and spawns a `Lightning` that travels independently and can damage mice; returns 1 if the bolt struck this tick.
  - `core.Mouse` — boss enemy; oscillates back-and-forth, has HP (3), shrinks and speeds up on damage. **Motion and HUD are now decoupled:** `move()` only advances physics, `show()` only paints the body sprite, and `renderHpBar(w, h)` is called separately by the game loop to draw the HUD.
- `core.Lightning` — Thord's skill projectile. `dart(mice)` advances physics and reports hits; `show()` paints the current frame. The bolt's render is gated on `spawned && !striked` and is a no-op when the frame asset is missing.

### Game loop / coordinators

The main flow lives in `src/main/java/com/riseofthebird/game/`:

- `App.main` — parses optional `-Droster=...`, instantiates `GameLoop`, calls `loop.run()`.
- `GameLoop` — single tick loop (~30 ms / `TICK_MS`). Owns the `Background`, `BirdManager`, `MouseManager`, `Controller`, and `InputState`. Each tick: `input.poll()` → phase-specific `update()` → phase-specific `render()` → `StdDraw.show()`. The round lifecycle is encoded explicitly in the `Phase` enum:
    ```
    READY ─SPACE press─▶ AIMING_ANGLE ─SPACE release─▶ AIMING_POWER ─SPACE release─▶ FLYING ─round ends─▶ (next bird → READY) or GAME_OVER ─ENTER press─▶ new run
    ```
- `BirdManager` — instance type wrapping the ordered queue of birds for a run. `current()`, `advance()`, `hasMore()`. Constructed from a `BirdCharacter[]` roster.
- `MouseManager` — instance type holding the live mouse list. `tick(birds)` does motion + bird-mouse collision detection and **returns the number of hits scored this tick**; the game loop adds that to `score`. Iteration uses `Iterator.remove()` so dead mice are removed safely (no more `ConcurrentModificationException`).
- `Controller` — instance type holding three sub-controllers: `Power`, `Angle`, `Skill`. Each is purely tick-driven (`tick()`, `applyTo(bird)`, `render(...)`); none hold static state, so a fresh `Controller` is the natural reset.
- `BirdCharacter` (enum: `THORD`, `BULK`) — each constant carries a `Supplier<Bird>` factory. Adding a new character requires supplying a `Bird` subclass; the compiler enforces it. `VALKYRD` from the original enum was removed because no `Bird` implementation exists.

### Input

`com.riseofthebird.input.InputState` snapshots `StdDraw.isKeyPressed(VK_SPACE)` and `VK_ENTER` once per tick (`poll()`) and exposes:

- `isSpaceDown()` / `isEnterDown()` — held this tick
- `wasSpacePressed()` / `wasEnterPressed()` — rising edge
- `wasSpaceReleased()` — falling edge

All phase transitions in `GameLoop` use edge events, never raw held state, so a single key tap cleanly advances exactly one phase. There are no busy-wait input loops anywhere in the game code.

### Rendering model

All drawing goes through `StdDraw` static calls. The canvas coordinate system is set once in `Background`'s constructor: `setXscale(-width/2.0, width/2.0)`, `setYscale(-height/2.0, height/2.0)` — origin is **center of screen**. `StdDraw.enableDoubleBuffering()` is called once at `GameLoop` construction; `StdDraw.show()` flushes after each tick. Sprite rotation is supported via the angle parameter on `StdDraw.picture(...)`.

`update()` and `render()` are strictly separated: physics methods (`Bird.move()`, `Mouse.move()`, `Lightning.dart()`) never touch the canvas. The render pass clears the background, then re-paints every visible entity from its current state. This invariant is what allows a single `Background.clear()` per frame without losing sprites mid-frame.

## Things to watch when modifying

- **Adding a new bird character:** add the enum constant in `BirdCharacter` with its `Supplier<Bird>` factory — the compiler will refuse to forget the implementation.
- **Adding a new mouse / boss:** call `mouseManager.addMouse(new Mouse(...))` after constructing the manager, or build a roster method analogous to `MouseManager.defaultRoster()`. Iteration is safe.
- **Adding a new asset:** drop it under `src/main/resources/assets/<group>/` and reference it via `Assets.require(...)` (mandatory) or `Assets.optional(...)` (graceful no-op). It will be picked up by the next `mvn package` and shipped inside the shaded jar.
- **Skill that spawns a projectile:** override `Bird.useSkill(mice)` to advance physics and return hit counts, and override `Bird.drawSkillEffect()` to render during the render pass. See `Thord` + `Lightning` for the canonical pattern.
- **Phase transitions:** only add transitions inside `GameLoop.update*()`. Do not transition during `render()`.
- **Tick rate** is fixed at `GameLoop.TICK_MS` (30 ms). Per-phase pauses no longer exist; visual speeds are uniform.
- **Asset paths** are case-sensitive on Linux. Lower-case all new resource paths.
- **`Bird.resetForRound()`** restores `time`, `velocity`, `currentAngle`, `formIndex`, `gravity`, and the skill-activated latch. New per-round state on a `Bird` subclass should be reset there too (see `Thord.resetForRound()` resetting `lightning`).