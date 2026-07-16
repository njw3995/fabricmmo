# FabricMMO

FabricMMO is a Fabric-native port of mcMMO for Minecraft 1.21.1. It targets Java 21 and uses mcMMO 2.3.000 at commit `2b855372a2856f68630a4f36866f59de95539184` as its behavioral baseline.

Status: pre-alpha.

## Modules

- `fabricmmo-api`: public addon API
- `fabricmmo-core`: server-side progression and gameplay
- `fabricmmo-client`: optional client features
- `parity-tests`: integration and upstream comparison tests
- `tools/upstream-diff`: upstream change analysis

The Cobblemon integration is maintained in the separate `fabricmmo-cobblemon` repository.

## Requirements

- Java 21
- Minecraft 1.21.1
- Fabric Loader 0.18.4
- Fabric API 0.116.13+1.21.1

## Build

```bash
./gradlew clean build
```

Windows:

```powershell
.\gradlew.bat clean build
```

## Upstream comparison

Clone the pinned mcMMO source beside this repository:

```bash
git clone https://github.com/mcMMO-Dev/mcMMO.git ../mcmmo-upstream
git -C ../mcmmo-upstream checkout 2b855372a2856f68630a4f36866f59de95539184
```

Run the diff utility with two mcMMO revisions:

```bash
./gradlew :tools:upstream-diff:run --args='../mcmmo-upstream 35ec12e447cea347d11c675f2ebe6b8cf5a147d6 2b855372a2856f68630a4f36866f59de95539184'
```

## License

FabricMMO is licensed under GPL-3.0. It is not affiliated with or endorsed by the mcMMO project.
