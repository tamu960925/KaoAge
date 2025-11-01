# KaoAge – AGENTS.md

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