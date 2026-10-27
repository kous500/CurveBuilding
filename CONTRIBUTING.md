# Contributing to CurveBuilding

Thank you for your interest in improving CurveBuilding! Bug reports, feature ideas, translations and code are all welcome.

- [Before you start](#before-you-start)
- [Development setup](#development-setup)
- [Pull requests](#pull-requests)
- [Translations](#translations)
- [Supported versions](#supported-versions)
- [Branches (maintainers)](#branches-maintainers)
- [Versioning and releases (maintainers)](#versioning-and-releases-maintainers)
- [What you can expect from maintainers](#what-you-can-expect-from-maintainers)
- [License](#license)
- [Security](#security)

## Before you start

- **Bugs**: open an issue using the bug report template. Please include your CurveBuilding, Minecraft, server/loader and WorldEdit versions.
- **Questions and ideas**: use [Discussions](https://github.com/kous500/CurveBuilding/discussions).
- **Large changes** (new features, refactoring, support for another Minecraft version): please open an issue or a discussion **before** writing the code, so we can agree on the approach first. This avoids work that cannot be merged.
- Small fixes and translations can go straight to a pull request.

## Development setup

See [Edit the Code](README.md#edit-the-code) in the README for setting up IntelliJ IDEA and building the Bukkit and Fabric jars. JDK 25 and JDK 21 are required for development: Gradle runs on Java 25, and the jars for older Minecraft versions are compiled with Java 21.

## Pull requests

- **Target branch**: `main`.
- **Allow edits by maintainers**: please keep this option enabled, so maintainers can fix the title or make small adjustments.
- **One topic per pull request.** Pull requests are merged with **squash merge**, so the pull request becomes a single commit on `main`. Commits inside your branch can be anything.

### Title

The title becomes the commit message on `main`, so it must follow this format:

```
<type>: <summary>
```

| Type | Use for |
|---|---|
| `feature` | User-visible additions or improvements, including new translations |
| `fix` | Bug fixes |
| `mc` | Support for a new Minecraft / platform version (always name the platform and version) |
| `chore` | Everything users do not see: documentation, CI, refactoring, dependency updates |

- Write the summary in **English**, in the imperative mood, starting with a capital letter and without a trailing period. Aim for 72 characters or fewer.
- Do not add the pull request number; GitHub adds it automatically when merging.

Examples:

```
feature: Add preview color option
feature: Add Korean translation
fix: Particles disappear after bcpos clearall
mc: Support Bukkit 26.4
chore: Add release workflow
```

### Description

The description may be written in **English or Japanese**. The template asks for:

- **What** changed and **why**.
- **How you tested it**: Minecraft version, server software or loader (Paper, Spigot, Fabric, ...) and WorldEdit / FastAsyncWorldEdit version.
- `Closes #<issue>` if it resolves an issue.
- A line starting with `BREAKING:` if server owners need to act, for example when keys in `config.yml` or the message files change.

### Checks

Pull requests must pass CI before they are merged: the build and tests, a Fabric server start on each built Minecraft version, and, for branches in this repository, the branch name. CI does not run on draft pull requests; it starts when the pull request is marked ready for review. For contributors outside the project, a maintainer approves the CI run first.

Please do not share built jars in issues or pull requests. Official builds are only distributed through [GitHub Releases](https://github.com/kous500/CurveBuilding/releases) and the official download pages listed in the README.

## Translations

Message files live in two places and should be kept in sync:

- `curvebuilding-bukkit/src/main/resources/messages/`
- `curvebuilding-fabric/src/main/resources/messages/`

To add a language, copy `en-US.yml`, translate the values (not the keys), and name the file with a [BCP 47](https://www.rfc-editor.org/info/bcp47) language tag such as `ko-KR.yml`. Please also add the language to the table in the README.

## Supported versions

| Platform | Target support |
|---|---|
| Bukkit (Spigot / Paper) | Minecraft 1.20 and later on Java 21 or newer, with a single jar |
| Fabric | Major Minecraft versions from 1.20 onward on Java 21 or newer. Each jar covers as many Minecraft versions as it can run on |

All supported versions are built from `main`. We do not keep separate branches for older Minecraft versions. If a change cannot work on every supported version, it needs version-specific code instead of a separate branch. In the Fabric module, write it with [Stonecutter](https://stonecutter.kikugie.dev/) comments, for example `//? if >=26.1 {`.

The Fabric sources in the repository are kept in the state of the newest supported Minecraft version, Stonecutter's `vcsVersion` in `settings.gradle`. When support for a newer Minecraft version is added, make it the `vcsVersion` in the same pull request. Before committing, run the `Reset active project` task so that the sources are in that state.

> Support for more Fabric versions is being added. The Minecraft versions built right now are listed in `settings.gradle`.

## Branches (maintainers)

Contributors working in their own fork can name branches freely. Branches created in this repository follow these rules:

```
<type>/<summary>-<yyyymmdd>
<type>/<issue>-<summary>-<yyyymmdd>    # when there is a related issue
```

- `<type>` is one of `feature`, `fix`, `mc` or `chore` (the same as pull request titles).
- `<yyyymmdd>` is the date the branch was created. It keeps branch names unique, even for similar tasks.
- Use **lowercase** letters, digits, `.` and `-` only. Do not use `/` inside the summary.
  Git on Windows does not distinguish upper and lower case, and `fix/a` cannot coexist with `fix/a/b`.
- `main` is the only long-lived branch. Branches are deleted automatically after merging; continue follow-up work on a new branch.

Examples: `fix/40-particles-disappear-on-clearall-20261024`, `mc/bukkit-26.4-20261024`, `chore/release-workflow-20261024`

A branch that does not start with `<type>/` or does not end with `-<yyyymmdd>` cannot be pushed to this repository: GitHub rejects creating it. The rest of the name, such as upper case letters in the summary, is checked by CI when the pull request is opened. To fix a name, push the commits again under a correct name and open a new pull request.

## Versioning and releases (maintainers)

A release is a **tag on `main`**. The tag is the only place where the version number is decided; it is not written in the source. The release workflow creates the tag, so maintainers do not tag by hand.

### Version numbers

While the version is `0.x`, the person who creates the tag picks the version from the pull requests in the release milestone:

| Bump | When | Example |
|---|---|---|
| Minor (`0.6.x` → `0.7.0`) | User-visible changes: new features or commands, or anything marked `BREAKING:` | New configuration option |
| Patch (`0.6.8` → `0.6.9`) | Bug fixes, support for new Minecraft versions, translations | `mc: Support Bukkit 26.4` |

- Pre-releases come before the version they lead to, in this order:
  - `-alpha.N`, for example `0.7.0-alpha.1`: early builds for trying out unfinished features or a new Minecraft version. They may have known bugs.
  - `-rc.N`, for example `0.7.0-rc.1`: release candidates, feature-complete and meant to become the release unchanged.
- Tags have no `v` prefix: `0.6.9`.
- Other builds are named after the latest release and the commit, as `git describe` prints them: `0.6.8-5-g3db5589` is 5 commits after 0.6.8 at commit `3db5589`, with `-dirty` added when there are uncommitted changes. They are not distributed.

### Jar names

| Platform | Name |
|---|---|
| Bukkit | `curvebuilding-bukkit-<version>.jar` |
| Fabric | `curvebuilding-fabric-<version>+<minecraft range>.jar` |

`<minecraft range>` is the oldest and newest Minecraft version the jar supports, joined with `-`, or a single version when the jar supports only one:

- `curvebuilding-fabric-0.6.9+1.21.9-1.21.11.jar` supports Minecraft 1.21.9 to 1.21.11.
- `curvebuilding-fabric-0.6.9+26.3.jar` supports only Minecraft 26.3.

Both ends must be versions the jar has been tested on; a new Minecraft patch is not included until it has been tested. The same range is used for `minecraft` in `fabric.mod.json`, and the mod version is the part of the file name after `curvebuilding-fabric-` (for example `0.6.9+1.21.9-1.21.11`).

### Release steps

1. Make sure every pull request planned for the release (its milestone) is merged into `main`.
2. In the Actions tab, open the **Release** workflow, choose **Run workflow** on `main` and enter the version.
3. CI checks the version, builds every jar and runs the same checks as for pull requests. Only then does it tag the commit and create a **draft** release with the jars and `SHA256SUMS`. If anything fails, no tag is created and the workflow can be run again.
4. Write the release notes in the draft, in the style of the earlier releases (in Claude Code, `/release-notes <version>` writes them from the changes since the previous release), and publish it.
5. A maintainer approves the `release` environment. CI then publishes the jars to Modrinth and CurseForge.

> Step 5 is being set up. Until then, upload the jars to Modrinth and CurseForge manually.

To try the release first, check **Dry run** in step 2. A dry run works on any branch, so changes to the release or build workflows can be tried before they are merged, and leaves no tag or release. It builds the jars with the version `<version>-dry-run.g<commit>`, keeps them in the run for 7 days (for testing only, do not share them) and shows the files and the pull requests the release would contain.

## What you can expect from maintainers

- A **first response within one month**. Even when a decision takes longer, we will tell you that we received your contribution and what the next step is.
- If we decide not to merge a pull request, we **close it with the reason** instead of leaving it open.
- If we reuse parts of your work in another pull request, we credit you with a `Co-authored-by:` trailer.

## License

CurveBuilding is licensed under the [GNU General Public License v3.0](LICENSE). By submitting a contribution, you agree that it is licensed under the same license. No separate contributor agreement is required.

## Security

Please do not report vulnerabilities in public issues. See [SECURITY.md](SECURITY.md).
