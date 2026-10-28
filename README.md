# CurveBuilding

### The best Minecraft mod and plugin for building curves

Make curved structures using commands and WorldEdit.

- Freely place the selected structure along a Bézier Curve.
- Preview a simplified drawing of the curve.
- Make the curve with continuously connected curves.
- Great for building infrastructure such as roads.
- WorldEdit undo available.

WorldEdit or FastAsyncWorldEdit required. CurveBuilding is compatible with Bukkit and Fabric.

If you have suggestions for other versions or features, please comment on the [Discussions](https://github.com/kous500/CurveBuilding/discussions).

## Detailed description and downloads

- For the plugin (Bukkit): <https://legacy.curseforge.com/minecraft/bukkit-plugins/curvebuilding>
- For the mod (Fabric): <https://legacy.curseforge.com/minecraft/mc-mods/curvebuilding>

## Edit the Code

If you want to contribute to the CurveBuilding project by adding features, fixing bugs, or supporting other versions, you can do so by following the methods below.

### Setting up the Development Environment

1. Download CurveBuilding's source code using Git.
2. Download and install [IntelliJ IDEA](https://www.jetbrains.com/idea/download/).
3. Install the **Minecraft Development** plugin in IDEA.
4. In the IDEA, open the folder where you saved CurveBuilding's code. This will create a new project in IDEA.
5. Install JDK 25 and JDK 21. Gradle runs on JDK 25 (set in `gradle/gradle-daemon-jvm.properties`) and finds installed JDKs automatically. Set the project SDK version to 25.

All modules are part of a single Gradle build. The Fabric module uses [Stonecutter](https://stonecutter.kikugie.dev/) to build several jars from the same code. Each jar is built against one Minecraft version and can support a range of compatible versions; it has its own project `:curvebuilding-fabric:<minecraft version>` with settings in `curvebuilding-fabric/versions/<minecraft version>/gradle.properties`.
Fabric is currently built for Minecraft 1.21.10, 1.21.11 and 26.1 - 26.3 (one jar for 26.1 - 26.3).

### Running the Project

Each task can be started from the Run/Debug Configurations in IDEA or with Gradle. `<version>` is a Minecraft version that Fabric is built for.

#### Building

| What | Run configuration | Command | Jars in |
|---|---|---|---|
| Everything | - | `./gradlew build` | the folders below |
| Bukkit | `curvebuilding-bukkit [build]` | `./gradlew :curvebuilding-bukkit:build` | `curvebuilding-bukkit/build/libs` |
| Fabric, every version | `curvebuilding-fabric [build]` | - | `curvebuilding-fabric/versions/<version>/build/libs` |
| Fabric, one version | - | `./gradlew :curvebuilding-fabric:<version>:build` | `curvebuilding-fabric/versions/<version>/build/libs` |

#### Running in the game

| What | Run configuration | Command |
|---|---|---|
| Fabric client | `curvebuilding-fabric <version> [runClient]` | `./gradlew :curvebuilding-fabric:<version>:runClient` |
| Fabric server (localhost) | `curvebuilding-fabric <version> [runServer]` | `./gradlew :curvebuilding-fabric:<version>:runServer` |
| A Fabric jar on another version in its range, for example the 26.1 - 26.3 jar on 26.1 | `fabric-test-runs <version> [runClient]` or `[runServer]` | `./gradlew -PtestRuns :fabric-test-runs:<version>:runClient` |

Each Minecraft version runs in its own folder, `curvebuilding-fabric/run/<version>`. The other versions in a jar's range are set up in `fabric-test-runs/<version>/gradle.properties`; add a folder there to run another version.

#### Testing

| What | Run configuration | Command |
|---|---|---|
| Every test | `CurveBuilding [allTests]` | `./gradlew allTests --continue` |
| Unit tests | - | `./gradlew test` |
| Game tests on one version | - | `./gradlew :curvebuilding-fabric:<version>:runGameTest`, or `./gradlew -PtestRuns :fabric-test-runs:<version>:runGameTest` for another version in a jar's range |

- The game tests (`curvebuilding-fabric/src/gametest`) build curves with WorldEdit on a real server. The server runs in `build/run/gameTest` of the version's project, emptied before each run, and the task fails if a test fails. `build` compiles the game tests but does not run them.
- `--continue` runs the remaining tests after a failure.

Once you have completed testing, please submit a pull request.
Before contributing, please read [CONTRIBUTING.md](CONTRIBUTING.md) for the pull request title format and other rules.

## About other language support

If you would like to translate the language files for this plugin into your language and merge them into this plugin, please store the language files under `src/main/resources/messages` of both `curvebuilding-bukkit` and `curvebuilding-fabric` and open a pull request.
File names should conform to BCP 47 (Language-Region). See [Translations](CONTRIBUTING.md#translations) for details.

### Currently supported languages are

| Language | File Name |
|-|-|
| English(US) | en-US.yml |
| Spanish(ES) | es-ES.yml |
| Spanish(Latin America) | es-419.yml |
| 日本語 | ja-JP.yml |
| 繁體中文 | zh-Hant.yml |
| 简体中文 | zh-CN.yml |
