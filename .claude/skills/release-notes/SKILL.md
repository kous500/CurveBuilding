---
name: release-notes
description: Write the release notes of a CurveBuilding release from the changes since the previous release, in the style of the earlier releases. Use when a maintainer asks for release notes, for example "/release-notes 0.6.9".
---

# Release notes

The release workflow creates a draft release with the jars and a generated list of pull requests. The release
notes are written by hand in the style below, and a maintainer pastes them into the draft and publishes it.

## Find the changes

1. The new version is the argument (for example `0.6.9`). Run `git fetch --tags origin`. If the tag exists, the
   release is the tag; otherwise it is `origin/main` (the notes are written before running the workflow).
2. The previous version is the release to compare with (`gh release list --exclude-drafts`):
   - for a release such as `0.7.0`, the latest earlier release that is not a pre-release, so the notes cover
     every change since then, including those already in `0.7.0-alpha.1` or `0.7.0-rc.1`;
   - for a pre-release such as `0.7.0-rc.2`, the latest earlier release of any kind.
3. List the pull requests in the range: `git log --first-parent --format=%s <previous>..<new>`. Each squash
   commit ends with `(#<number>)`. Read each one with `gh pr view <number> --json title,body` and look at the
   diff (`git show <commit>`) when the title does not say enough for users.
4. Find the supported versions:
   - Fabric: one jar per `curvebuilding-fabric/versions/<version>/`; `minecraft_min` and `minecraft_max` in its
     `gradle.properties`.
   - Bukkit: keep the range of the previous release notes unless a pull request changed it. Ask the maintainer
     when unsure, for example after an `mc: Support Bukkit ...` pull request.

## Write the notes

- English, in the past tense ("Fixed", "Added", "Changed"), for server owners and players, not for developers.
- List items start with `-`.
- Sections, in this order, each only when it has entries:
  - `## General`: changes that affect both the Bukkit plugin and the Fabric mod.
  - `## Plugin`: the Bukkit plugin. Starts with `- Supported versions: Bukkit \`<min> to <max>\``.
  - `## Mod`: the Fabric mod. Starts with `- Supported versions: Fabric \`<min> to <max>\`` for each jar range
    (only the version for a jar of one version), separated by `, `.
  - `## For Developers`: changes that matter to people who build the project or use its code.
- Lines starting with `BREAKING:` in pull request descriptions come first in their section, marked
  `**Breaking:**`, with what server owners need to do. List changed configuration defaults as
  `key: \`old\` -> \`new\``.
- Add the issue number when a pull request closes one: `(#40)`.
- Leave out `chore` pull requests (CI, documents, refactoring, dependency updates) unless they change something
  for users or developers.
- End with `**Full Changelog**: https://github.com/kous500/CurveBuilding/compare/<previous>...<new>`.

The examples are earlier releases, rewritten in the format above: list items, the supported versions lines, the
order of the sections and the Full Changelog line were different in some of them.

Example (0.6.8):

```markdown
## Plugin

- Supported versions: Bukkit `1.20.2 to 26.3`
- Mitigated an issue where particle rendering of points and guidelines could stop for all players after running `//bcpos clearall` (#40)

**Full Changelog**: https://github.com/kous500/CurveBuilding/compare/0.6.7...0.6.8
```

Example (0.6.5, with every section):

```markdown
## General

- Fixed an issue where the `//undo` command would not completely revert all changes in specific environments.
- Default values in the config file have been changed:
  - curve.max-set-length: `1000` -> `10000`

## Plugin

- Supported versions: Bukkit `1.20.2 to 1.21.8`

## Mod

- Supported versions: Fabric `1.21.4`

## For Developers

- It is no longer necessary to modify the code when adding new language files to the resources.

**Full Changelog**: https://github.com/kous500/CurveBuilding/compare/0.6.4...0.6.5
```

## Hand them over

- Show the notes in the conversation and write them to `build/release-notes/<version>.md` (ignored by Git).
- Do not edit or publish the release yourself (`gh release edit` and `gh release create` are denied in
  `.claude/settings.json`). The maintainer pastes the notes into the draft release and publishes it.
