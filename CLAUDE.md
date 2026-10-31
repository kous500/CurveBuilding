# CurveBuilding

## Compatibility

- Support the major Minecraft versions from 1.20 onward that run on Java 21 or newer, all built
  from `main`. Bukkit is a single jar. A Fabric jar should cover every Minecraft version it can
  run on (a version range in `fabric.mod.json`); add a jar only where the API breaks.
- Never create branches for older Minecraft versions; handle version differences with
  version-specific code (Stonecutter comments). Keep Fabric rendering differences in the preview
  backends (`client/render/backend`, one file per version range, picked by `PreviewBackends`) and
  other differences in `FabricCompat`; the preview geometry code must not branch by version.
- Stonecutter's `vcsVersion` is the newest supported Minecraft version; when adding a newer one,
  make it the `vcsVersion`. Commit only with the sources in that state (`Reset active project`).
- Core and Bukkit target Java 21. Do not use Java 22+ language features or APIs there.
- When adding a message key, add it to every message file in both the Bukkit and the Fabric module.

## Code comments

These rules and the ones in CONTRIBUTING.md apply to comments in the code, the Gradle build scripts
and the GitHub Actions workflows.

- Write code comments in English.
- Write what someone opening the file half a year later needs: the reasons, assumptions and
  constraints that the code does not show. The history of the work belongs in the pull request
  description, not in comments.
- Keep them short and limited to what is needed.

## Versions and releases

- The release tag is the only source of the version number. Do not bump version numbers in build
  files or resources.
- Never create or push tags, or create GitHub Releases. Releases are done by the maintainers.

## Git and GitHub

- Follow CONTRIBUTING.md for branch names, commit messages and pull request titles and descriptions.
  Write the description with the headings of `.github/PULL_REQUEST_TEMPLATE.md`.
- Never push to `main` or force-push. Pull requests are squash-merged.
- When reusing code from someone else's pull request, credit them with a `Co-authored-by:` trailer
  at the end of the pull request description, which becomes the squash commit message.
