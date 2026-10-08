# DOWWREY Beds 1.8.0 — финальный аудит стабильности

## Обновления

- Minecraft 26.3
- Fabric Loader 0.19.5
- Fabric API 0.162.0+26.3
- Fabric Loom 1.18.2
- Gradle 9.7.1
- Java target 25

## Hot-path audit

- Нет постоянного disk I/O в `BedSavedData.get()`.
- Recovery backup не выполняется на каждую мутацию.
- Нет постоянной загрузки чанков ради списка.
- Нет обхода всех игроков каждый tick.
- Активные телепортации хранятся только для игроков, которые действительно телепортируются.
- Friend Bed selector не выполняет повторный O(players × beds) подсчёт.
- Client HUD не форматирует динамическую строку каждый кадр.

## Network hardening

- Server-side validation для владельца, лимита, цены, cooldown, точки и состояния кровати.
- Rate limit для списков и Friend Bed selector.
- Payload списков игроков рассчитан на 100+ игроков.
- Disconnect cleanup для transient maps.

## Persistence

- Vanilla `SavedData` остаётся основным источником данных.
- Schema version 3 совместима с предыдущими сохранениями.
- Recovery backup атомарно заменяет старый файл.

## Что всё ещё требует реального теста

- Полный `gradlew build` на Temurin 25.0.4/25.0.4.1.
- Dedicated server 50 игроков.
- Dedicated server 100 игроков.
- Клиент + сервер + два клиента с Friend Beds.
- Перезапуск сервера во время телепорта/запроса.
- Профилирование TPS/FPS на реальной машине.

Статический аудит не заменяет эти живые тесты.
