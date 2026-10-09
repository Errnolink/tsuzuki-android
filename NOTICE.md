# Tsuzuki Android Notices and Attributions

Tsuzuki Android is licensed under the Apache License, Version 2.0 (the "License").
You may obtain a copy of the License in the accompanying `LICENSE` file or at:
http://www.apache.org/licenses/LICENSE-2.0

Copyright (c) 2026 Tsuzuki Contributors.

---

## Upstream Project and Lineage

Tsuzuki Android is a fork of **Komikku** (v1.14.1), which itself builds upon **TachiyomiSY** and **Mihon / Tachiyomi**.

### 1. Komikku
* **Repository:** https://github.com/komikku-app/komikku
* **License:** Apache License 2.0
* **Copyright:** Copyright 2024 cuong-tran and Komikku contributors

### 2. TachiyomiSY
* **Repository:** https://github.com/jobobby04/TachiyomiSY
* **License:** Apache License 2.0
* **Copyright:** Copyright TachiyomiSY contributors

### 3. Mihon / Tachiyomi
* **Repository:** https://github.com/mihonapp/mihon / https://github.com/tachiyomiorg/tachiyomi
* **License:** Apache License 2.0
* **Copyright:** Copyright 2015 Javier Tomás and Mihon/Tachiyomi contributors

---

## Ported and Adapted Code: Neko

Tsuzuki Android incorporates features and logic ported from **Neko** (v3.8.2, commit `c43e64d`):
* **Repository:** https://github.com/CarlosEsco/Neko
* **License:** Apache License 2.0
* **Copyright:** Copyright 2020 Carlos Escobedo and Neko contributors

As documented in `docs/NEKO-PORT.md`, the following files contain ported or adapted code from Neko:
* `app/src/main/java/exh/md/handlers/StatusHandler.kt` — Reading status observation and MangaDex progress sync (ported from Neko `…/source/online/handlers/StatusHandler.kt`).
* `app/src/main/java/exh/md/handlers/MangaDexFeatures.kt`, `exh/md/handlers/MangaDexArtworkStore.kt`, `exh/md/handlers/MangaDexDynamicCovers.kt` — Cover-art gallery, cache, and custom MDLists (ported/split from Neko `ArtworkHandler.kt`, `ListHandler.kt`, `FeedUpdatesHandler.kt`).
* `app/src/main/java/exh/md/handlers/AutoAddTrackers.kt` — Automatic tracker binding upon library addition (ported from Neko `…/usecases/tracking/AutoAddTrackers.kt`).
* `app/src/main/java/exh/md/handlers/MangaDexSearch.kt` — Search paging and UUID resolution (adapted from Neko `…/source/online/handlers/SearchHandler.kt`).
* `app/src/main/java/exh/md/handlers/ApiMangaParser.kt` — Chapter list re-parsing without network round-trips (extended from Neko `ApiMangaParser.kt`).
* `app/src/main/java/exh/md/handlers/PageHandler.kt` — External-host and at-home image resolution fallbacks (extended from Neko `PageHandler.kt`).
* `app/src/main/java/exh/md/handlers/MangaHandler.kt` — Aggregate chapter parsing (extended from Neko `MangaHandler.kt`).
* `app/src/main/java/exh/md/MangaDexPreferences.kt` — MangaDex preferences model (adapted from Neko `MangaDex.kt`).
* `app/src/main/java/dev/errnolink/tsuzuki/mangadex/**` — MangaDex UI presentation and bridge adapters built to achieve behavioral parity with Neko.

---

## Third-Party Libraries and Components

The following third-party libraries are incorporated or vendored in Tsuzuki Android:

### 1. Haze
* **Artifact:** `dev.chrisbanes.haze:haze:1.7.2`
* **License:** Apache License 2.0
* **Copyright:** Copyright (c) 2023 Chris Banes

### 2. Backdrop
* **Artifact:** `io.github.kyant0:backdrop:1.0.6`
* **License:** Apache License 2.0
* **Copyright:** Copyright (c) 2024 Kyant0

### 3. FlexibleAdapter
* **Artifact:** `com.github.arkon:FlexibleAdapter:844a07002c`
* **License:** Apache License 2.0
* **Copyright:** Copyright (c) 2015-2016 Davide Rossi

### 4. Additional Libraries
* **Square OkHttp & Okio:** Apache License 2.0, Copyright Square, Inc.
* **Coil:** Apache License 2.0, Copyright Coil Contributors
* **SQLDelight:** Apache License 2.0, Copyright Cash App
* **Voyager:** Apache License 2.0, Copyright Adriel Café
* **FlagKit:** MIT License, Copyright Made By Made
* **QuickJS-Android:** Apache License 2.0, Copyright Hai Zhang

---

## License Compatibility

All incorporated, ported, and vendored components are distributed under Apache License 2.0 or compatible permissive licenses (MIT). No GPL-only, proprietary, or incompatible code is included in the Android application.
