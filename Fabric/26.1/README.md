# DeepFrozen - Fabric 26.1

Backport of the Minecraft 26.4 Ice Caves drop for Minecraft 26.1 (Fabric).

```
./gradlew build
./gradlew runClient
```

Needs Java 25.

## Notes

- Built from the 26.2 sources with the 26.1 differences: entity type constants on `EntityType`, the icicle built on `PointedDripstoneBlock`, no Frostbite advancement extensions (package names differ), no sulfur cube archetype data.
- Not compiled against 26.1 yet.
