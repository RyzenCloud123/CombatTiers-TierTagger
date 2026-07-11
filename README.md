# Combat Tiers TierTagger — Fabric 1.21.11

Native source project for Combat Tiers.

## Features

- Fabric 1.21.11
- Java 21
- Live Firebase Realtime Database sync
- TAB-list tier rendering through a real Fabric Mixin
- Vanilla icon font glyph
- Async HTTP networking
- Request timeouts
- Local cache
- Case-insensitive usernames
- `None` tiers hidden
- Sodium-safe: no Sodium classes are modified

## Database

The mod reads:
Expected player data:

```json
{
  "negativetier": {
    "username": "NegativeTier",
    "tiers": {
      "vanilla": "HT3"
    }
  }
}
```

## Build on GitHub

1. Create a new GitHub repository.
2. Upload every file and folder from this project.
3. Open **Actions**.
4. Open **Build TierTagger**.
5. Run the workflow.
6. Download the artifact.
7. Use the normal JAR from `build/libs/`, not the `-sources.jar`.

## Build locally

Install Java 21 and Gradle, then run:

```bash
gradle build
```

Output:

```text
build/libs/combattiers-tiertagger-1.0.0.jar
```

## Install

Put the compiled JAR inside the Minecraft profile's `mods` folder with:

- Fabric Loader 0.19.3+
- Fabric API for Minecraft 1.21.11
- Java 21

Delete every older Combat TierTagger JAR first.
