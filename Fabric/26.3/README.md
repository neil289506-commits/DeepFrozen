# DeepFrozen - Fabric 26.3

Backport of the Minecraft 26.4 Ice Caves drop for Minecraft 26.3 (Fabric).

```
./gradlew build
./gradlew runClient
```

Needs Java 25.

## Notes

- Built from the 26.2 sources with the 26.3 differences: `Feature` is an interface registered in `FEATURE_TYPE`, brewing is data driven (13 recipes under `recipe/brewing`), worldgen data is under `worldgen/feature` with flattened configs, `offset` replaces `random_offset`, mob spawns are an environment attribute, loot tables use the single-`condition` / `modifier` format (26 converted).
- These are based on 26.4 snapshot 7, which the 26.3 API appears to match; not compiled or run against 26.3 yet.
- Advancement and recipe JSON are still in the 26.2 format.
