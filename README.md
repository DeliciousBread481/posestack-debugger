# PoseStack Debugger

Client-side rendering stack diagnostics for Minecraft 26.1.x on Fabric and NeoForge.

## Target

- Minecraft 26.1.2
- Java 25
- Fabric Loader 0.18.6 + Fabric API 0.154.2+26.1.2
- NeoForge 26.1.2.76
- Architectury Loom no-remap 1.14.473

Minecraft 26.1 ships unobfuscated official names, so the project uses
`dev.architectury.loom-no-remap` instead of the mapping-based Loom plugin.

## Important 26.1 rendering changes

- `PoseStack#pushPose`, `popPose`, and `last` still exist.
- `PoseStack` now stores reusable entries in a `List` and tracks the active top with
  an integer index. The debugger reads its mixin-maintained depth first and falls
  back to that index instead of searching for a `Deque`.
- Living rendering is state-based:
  `LivingEntityRenderer#submit(LivingEntityRenderState, PoseStack,
  SubmitNodeCollector, CameraRenderState)`.
- Layers use
  `RenderLayer#submit(PoseStack, SubmitNodeCollector, int,
  EntityRenderState, float, float)`.
- GUI extraction no longer uses `PoseStack` or `GuiGraphics`.
  `GuiGraphicsExtractor#pose()` returns JOML `Matrix3x2fStack`. The common GUI
  tracker reads its active `curr` index at the platform screen callbacks, while
  world/entity rendering continues to instrument `PoseStack`.

## Build

```bash
./gradlew buildAll
```

Outputs:

- `fabric/build/libs/posestackdebugger-fabric-*.jar`
- `neoforge/build/libs/posestackdebugger-neoforge-*.jar`

The log is overwritten on each client launch at
`logs/posestack-debugger.log` and is capped at 20 MiB.
