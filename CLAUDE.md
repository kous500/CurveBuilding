# CurveBuilding

## Compatibility

- Every supported Minecraft version is built from `main`: Bukkit 1.20+ as a single jar, Fabric as
  one jar per representative version from 1.20.1. Never create branches for older Minecraft
  versions; handle version differences with version-specific code.
- Core and Bukkit target Java 17. Do not use Java 18+ language features or APIs there.
- When adding a message key, add it to every message file in both the Bukkit and the Fabric module.

## Versions and releases

- The release tag is the only source of the version number. Do not bump version numbers in build
  files or resources.
- Never create or push tags, or create GitHub Releases. Releases are done by the maintainers.

## Git and GitHub

- Follow CONTRIBUTING.md for branch names, commit messages and pull request titles.
- Never push to `main` or force-push. Pull requests are squash-merged.
- Ask before anything other people can see: pushing, opening or merging pull requests, commenting
  on issues or pull requests, changing repository settings.
- When reusing code from someone else's pull request, credit them with a `Co-authored-by:` trailer.
