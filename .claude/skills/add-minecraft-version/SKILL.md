---
name: add-minecraft-version
description: What to do when CurveBuilding gets support for another Minecraft version, on Fabric or on Bukkit. Use when a maintainer asks to support a version, for example "support 1.21.9 on Fabric" or "add Bukkit 26.4".
---

# Add a Minecraft version

The versions built, the order of the imports and the like can be read from the repository. This lists what is easy
to miss. Compare with the last pull request that added a version: `git log --oneline --grep="^mc:"`.

## Fabric

### Find the range of the jar

A jar covers every version it can run on, so try the neighbours of the requested version before adding a jar.

1. Find the dependencies of each candidate:
   - WorldEdit: `game_versions` of its Fabric releases on Modrinth. A version without a WorldEdit release, or with only
     a beta, cannot be promised. The artifact on `maven.enginehub.org` is not always named after the version: 7.3.16
     supports 1.21.6 - 1.21.8 and is published as `worldedit-fabric-mc1.21.7`. A release can also pull in a Fabric API
     for a newer version, which then refuses to load; an older release of WorldEdit may be needed for the test run.
   - Fabric API: its Modrinth releases for that version.
2. Build the jar against the newest version of the range.
3. Set `minecraft_min` and `minecraft_max` wide for the trial, and add `fabric-test-runs/<version>/gradle.properties`
   for each neighbour. Without the wide range, Fabric Loader refuses the jar.
4. Run the game tests there: `./gradlew -PtestRuns :fabric-test-runs:<version>:runGameTest`.
5. The game tests run on a server, so they do not load the client code. Also compare the Minecraft members that
   `remapJar`'s output refers to with the intermediary jar of each version
   (`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-intermediary/`), using `javap`.
   Check the Fabric API classes the client code uses too, as they move between versions.
6. On a remapped version, `fabric-test-runs` runs the development jar, which uses Mojang names. A `NoSuchMethodError`
   there can be a renamed method that the released jar, with intermediary names, does not mind. Find out which it is.
7. Keep only the oldest version of the range in `fabric-test-runs`.

### Make it build

- A newer version than all the others becomes `vcsVersion` in `settings.gradle` and `stonecutter.active`, and the
  sources move to that state (`Reset active project`).
- Differences go in the places CLAUDE.md names. A new preview backend is also picked in `PreviewBackends`.
- The `game_test` swap in `curvebuilding-fabric/build.gradle` and the helper methods of `CurveBuildingGameTests` also
  depend on the version.
- Read in the jars how the world render event of that version sets up the matrices before relying on a pose: it
  differs between versions (see the comments of the backends).
- A Java version not yet used: `java_version`, the `java` case in `publish.yml`, `java-version` and `GRADLE_TOOLCHAINS`
  in `build.yml`, and the JDKs named in the README and CONTRIBUTING.md. `core` stays on Java 21; a Minecraft
  version that needs an older Java is built on `java-17`.

### Files every new jar needs

- `settings.gradle`: the version list.
- `curvebuilding-fabric/versions/<version>/`: `gradle.properties` and `curvebuilding.accesswidener`, even if empty.
- `fabric-test-runs/<oldest version>/gradle.properties`, if the jar covers more than one version.
- `.github/workflows/build.yml`: the `server` and the `gametest` matrices, for the jar and for its oldest version.
- `.run/`:
  - `curvebuilding-fabric <version> [runClient]` and `[runServer]`
  - `fabric-test-runs <oldest version> [runClient]` and `[runServer]`
  - the task list of `curvebuilding-fabric [build]`
- README: the sentence with the versions Fabric is built for.
- `config/checkstyle/suppressions.xml`, if a file gets Stonecutter-switched imports.

### Check it

- `./gradlew build allTests checkCodeStyle --warning-mode all`, and that `allTests` ran the new versions.
- Dry-run the new run configurations, and look at the jar name and at the `depends` of its `fabric.mod.json`.
- `runServer` waits for input, so leave the server start to CI, and say so.
- Ask the maintainer to check the preview in the game on both ends of the range before committing: the curve, the
  points and the handles line up with the blocks, the translucent fill shows, and showing them through walls works.
  This also tests receiving the positions on the client, which the game tests do not.

## Bukkit

Bukkit is a single jar, so a new version usually needs no code.

- `curvebuilding-bukkit/build.gradle`: `spigot-api`, when compiling against the newer version.
- `.github/workflows/build.yml`, the `bukkit-server` matrix: the oldest and the newest version, with the Java it needs
  and a WorldEdit named by its Modrinth version ID, as the Fabric and Bukkit releases share version numbers.
- `.github/workflows/publish.yml`: `BUKKIT_GAME_VERSIONS`, with the same ends as that matrix.
- CI starts Paper only, reloads the plugin, and fails on any warning from CurveBuilding, such as ParticleNativeAPI
  failing to load. It does not see the particles or run `//bc`, and Spigot needs BuildTools, so ask the maintainer
  whether those were tried.

## The pull request

- Follow CONTRIBUTING.md: the branch `mc/<platform>-<version>-<yyyymmdd>`, the title `mc: Support Fabric <range>`, and
  `Closes #<issue>` for an issue that asks for the version.
- In "How it was tested", name the versions each check ran on, what ran on the development jar, and what was not run.
- A jar is released with the next release, not when the pull request is merged.
