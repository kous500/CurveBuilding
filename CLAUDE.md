# CurveBuilding

## Compatibility

- `main` builds the Minecraft versions that run on Java 21 or newer. Bukkit is a single jar. A
  Fabric jar should cover every Minecraft version it can run on (a version range in
  `fabric.mod.json`); add a jar only where the API breaks.
- Work on `main`. Change the long-lived branch `java-17` only when asked (see CONTRIBUTING.md).
- Never create other branches for older Minecraft versions; handle version differences with
  version-specific code (Stonecutter comments). Keep Fabric rendering differences in the preview
  backends (`client/render/backend`, one file per version range, picked by `PreviewBackends`) and
  other differences in `FabricCompat`; the preview geometry code must not branch by version.
- Stonecutter's `vcsVersion` is the newest supported Minecraft version; when adding a newer one,
  make it the `vcsVersion`. Commit only with the sources in that state (`Reset active project`).
- Core and Bukkit target Java 21. Do not use Java 22+ language features or APIs there.
- When adding a message key, add it to every message file in both the Bukkit and the Fabric module.

## Code comments

- Follow "Code comments" in CONTRIBUTING.md.
- Write comments for whoever works on the code later, without this conversation: only what stays
  true and helps them. Leave out anything that depends on the current task or on when it was
  written: the request, the previous code, the change, or the time ("now", "currently", "for
  now", "no longer", "recently", "instead of", "fixed", "added", "as requested").
- Before writing a comment, see whether a clearer name or a smaller method would say it instead.
- When changing code, update or remove the comments it makes wrong in the same change.
- Only write what you have checked. For the behavior of WorldEdit, Minecraft or another library,
  read its code (for example in the jar) before relying on it; do not guess from names.
- Start a Javadoc with what the caller gets, not "This method".

## After changing code

- Run `./gradlew build`, which compiles every Minecraft version and runs the unit tests.
- When the Fabric code or the game tests changed, also run
  `./gradlew :curvebuilding-fabric:26.3:runGameTest`. When the change differs between Minecraft
  versions (Stonecutter conditions, `FabricCompat`, the preview backends), run `./gradlew allTests`.
- Run `./gradlew checkCodeStyle` and fix the warnings in the lines you wrote.
- Read the whole diff before reporting: leftover debugging, unrelated changes, and code that does
  not match the files around it.
- Report which of these you ran and their results. Leave out checks you did not run, such as
  playing in the game; ask the user instead of writing them in the pull request.

## Versions and releases

- The release tag is the only source of the version number. Do not bump version numbers in build
  files or resources.
- Never create or push tags, or create GitHub Releases. Releases are done by the maintainers.

## Git and GitHub

- Follow CONTRIBUTING.md for branch names, commit messages and pull request titles and descriptions.
  Write the description with the headings of `.github/PULL_REQUEST_TEMPLATE.md`.
- Never push to `main` or `java-17`, or force-push. Pull requests into `main` are squash-merged.
- When reusing code from someone else's pull request, credit them with a `Co-authored-by:` trailer
  at the end of the pull request description, which becomes the squash commit message.
