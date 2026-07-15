# Verification

- `./gradlew buildAll`: passed with Gradle 9.4.0 and Java 25.
- Fabric client: Minecraft 26.1.2 / Fabric Loader 0.18.6 recognized and initialized PoseStack Debugger 2.0.0.
- NeoForge client: Minecraft 26.1.2 / NeoForge 26.1.2.76 recognized and initialized PoseStack Debugger 2.0.0.
- No Mixin injection errors were present in either final launch log.
- A one-shot local test mod intentionally leaked one GUI matrix entry; both loaders emitted `SCREEN IMBALANCE` with depth `0 -> 1`.
- Both loaders created `logs/posestack-debugger.log`.
- The test environment has no audio device, so Minecraft disabled OpenAL sound; client startup otherwise continued normally.

## Artifact SHA-256

```text
80d154f7c91e2a8ac0e7894f2864c89216439e9cc2690350880e26f5b4ee7d37  posestackdebugger-fabric-2.0.0.jar
941c0c64a087bdb81cb68b78ab3ac9a66891c604b410b9b996dd09c599aa2a2b  posestackdebugger-neoforge-2.0.0.jar
```
