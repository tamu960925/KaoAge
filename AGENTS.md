# KaoAge – AGENTS.md

## Configuration
The agent must respond **only in Japanese** for all outputs, explanations, and interactions.

## Tech guardrails
- Language: Kotlin for SDK, **Java-friendly API** (@JvmStatic/@JvmOverloads, Builder/Callback).
- Android 10+; camera control is **app-side**. SDK returns analysis + BestShot signals only.
- Use **ML Kit Face Detection**; expose `box`, `euler(yaw/pitch/roll)`, **landmarks** (eyes/nose/mouth).
- Return **Parcelable models + toJson()**. No image persistence. On-device only.

## Tests & quality
- **TDD**: write failing tests first (JUnit/Robolectric).  
- No new deps without tests + LICENSE check.  
- Models are **external**: provide `scripts/download_models.sh`; SDK loads via File/ByteBuffer/AFD.

## Deliverables
- `/sdk-core`, `/sdk-bestshot`, `/samples/cashier-app`, `/docs`.

## Active Technologies
- Kotlin 1.9.24 with Android Gradle Plugin 8.6.0 targeting Android API 29+ + ML Kit Face Detection on-device API; kotlinx-serialization JSON for `toJson()`; AndroidX lifecycle & coroutines for frame pipeline coordination (001-add-face-insights)
- None — results remain in memory; confirm no caching layers added (001-add-face-insights)

## Recent Changes
- 001-add-face-insights: Added Kotlin 1.9.24 with Android Gradle Plugin 8.6.0 targeting Android API 29+ + ML Kit Face Detection on-device API; kotlinx-serialization JSON for `toJson()`; AndroidX lifecycle & coroutines for frame pipeline coordination
