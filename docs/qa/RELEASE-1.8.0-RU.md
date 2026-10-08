# DOWWREY Beds 1.8.0 — релизная сборка

## Статус

Это финализированная исходная сборка для Minecraft 26.3 / Fabric после этапа оптимизации и стабилизации.

## Технологический стек

- Minecraft 26.3
- Fabric Loader 0.19.5+
- Fabric API 0.162.0+26.3
- Fabric Loom 1.18.2
- Gradle 9.7.1
- Java 25+

## Runtime-зависимости

Обязательны только Minecraft, Fabric Loader, Fabric API и Java 25+. ImGui, FranklyLib, GeckoLib и YACL не требуются для запуска DOWWREY Beds 1.8.0.

## Что оптимизировано

- уменьшен disk I/O от recovery backup;
- исключён файловый доступ из `BedSavedData.get()`;
- оптимизирован расчёт количества кроватей для Friend Bed player selector;
- увеличен лимит payload списка онлайн-игроков до 256;
- добавлены server-side rate limits для дорогих сетевых запросов;
- transient state очищается при disconnect и server stop;
- сохранены server-authoritative validation и safe-position checks без принудительной загрузки чанков.

## Перед настоящим публичным релизом

На машине разработчика обязательно выполнить:

1. Gradle `clean`.
2. Gradle `build` на Eclipse Temurin 25.0.4/25.0.4.1.
3. `runClient`.
4. Dedicated server 50 игроков.
5. Dedicated server 100 игроков.
6. Одиночный мир с 10 сохранёнными кроватями.
7. Friend Beds между двумя клиентами.
8. Перезапуск сервера во время активного запроса/телепорта.
9. Проверку старого мира с существующими SavedData.

Нагрузочные и build-тесты в этой среде не выдаются за пройденные: здесь отсутствует JDK 25 и внешний Gradle-кэш.
