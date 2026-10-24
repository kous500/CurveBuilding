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
5. Set the project SDK version to 21.

All modules are part of a single Gradle build. The Fabric module uses [Stonecutter](https://stonecutter.kikugie.dev/) to build one jar per Minecraft version; each version has its own project `:curvebuilding-fabric:<minecraft version>` with settings in `curvebuilding-fabric/versions/<minecraft version>/gradle.properties`.

### Running the Project

- To build CurveBuilding Bukkit:
  1. Select "Run `curvebuilding-bukkit [build]`" from Run/Debug Configurations, or run `./gradlew :curvebuilding-bukkit:build`.
  2. The project will be built and a jar file will be created in `curvebuilding-bukkit/build/libs`.
- To build CurveBuilding Fabric:
  1. Select "Run `curvebuilding-fabric [build]`" from Run/Debug Configurations, or run `./gradlew :curvebuilding-fabric:1.21.10:build`.
  2. The project will be built and a jar file will be created in `curvebuilding-fabric/versions/1.21.10/build/libs`.
- To build everything, run `./gradlew build`.
- To run CurveBuilding Fabric as a game client:
  1. Select "Run `curvebuilding-fabric [runClient]`" from Run/Debug Configurations.
  2. The game client will start with the mod applied.
- To run CurveBuilding Fabric as a game server:
  1. Select "Run `curvebuilding-fabric [runServer]`" from Run/Debug Configurations.
  2. The game server will start on localhost with the mod applied.

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
