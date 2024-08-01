# CurveBuilding
The best plugin for building curves.
Make curved structures using commands and WorldEdit.

## Current project status
The project is currently only minimally maintained. No new features are currently planned. Only work will be carried out to support the latest version.  
This includes support for legacy versions and for the latest versions.

If you are a developer and would like to add new features to this project, fix bugs, or support other versions, please submit a PR.


## Download
Please visit [here](https://github.com/kous500/CurveBuilding/releases), select your desired version and download.


## Detailed description and downloads
- Plugin (Bukkit) : https://legacy.curseforge.com/minecraft/bukkit-plugins/curvebuilding
- Mod (Fabric) : https://legacy.curseforge.com/minecraft/mc-mods/curvebuilding


## Edit the Code
To add new features or bug fixes to CurveBuilding, follow these steps:

1. Download CurveBuilding's source code using Git.
2. Install any version of Java greater than or equal to 21.
3. Download and install [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/).
4. In the IDE, open the folder where you saved CurveBuilding's code. This will create a new project in IntelliJ IDEA.


##### If you want to run CurveBuilding's source code, follow these instructions:
- To build as a Bukkit plugin
  1. Select "Run `curvebuilding-bukkit [build]`" from Run/Debug Configurations.
  2. The build process will compile the source code and create a jar file in `curvebuilding-bukkit/build/libs`.
- To build as a Fabric mod
  1. Select "Run `curvebuilding-fabric [build]`" from Run/Debug Configurations.
  2. The build process will compile the source code and create a jar file in `curvebuilding-fabric/build/libs`.
- To start the game client as a Fabric mod
  1. Select "Run `curvebuilding-fabric [runClient]`" from Run/Debug Configurations.
  2. The game client with the mod applied will launch.
- To start the game server as a Fabric mod
  1. Select "Run `curvebuilding-fabric [runServer]`" from Run/Debug Configurations.
  2. The game server with the mod applied will launch on localhost.


## About other language support
If you would like to translate the language files for this plugin into your language and merge them into this plugin, please store the language files under `\src\main\resources\messages` and open a PullRequest.
File names should conform to BCP 47 (Language-Region).


#### Currently supported languages are;
| Language    | File Name                         |
|-------------|-----------------------------------|
| English(US) | en.yml (Will be fixed to "en-US") |
| 日本語         | jp.yml (Will be fixed to "ja-JP") |
| 繁體中文        | zh-Hant.yml                       |
| 简体中文        | zh-CN.yml                         |