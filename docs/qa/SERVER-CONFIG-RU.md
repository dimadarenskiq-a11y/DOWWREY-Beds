# DOWWREY Beds 1.8.0 — серверная конфигурация

Файл создаётся автоматически:

`config/dowwrey_beds_server.json`

Все эти параметры проверяются и ограничиваются сервером. Клиент не может подменить серверные правила.

Пример безопасных значений по умолчанию:

```json
{
  "configVersion": 1,
  "maxBeds": 10,
  "friendBedsEnabled": true,
  "teleportEnabled": true,
  "brokenBedTeleportEnabled": true,
  "teleportCooldownSeconds": 25,
  "slowTeleportSeconds": 6,
  "slowTeleportCost": 1,
  "fastTeleportSeconds": 3,
  "fastTeleportCost": 2,
  "instantTeleportSeconds": 1,
  "instantTeleportCost": 3,
  "brokenTeleportSeconds": 5,
  "brokenTeleportCost": 3,
  "friendRequestLifetimeSeconds": 300,
  "listRequestCooldownTicks": 10,
  "friendRequestCooldownTicks": 20,
  "interactionRangeBlocks": 5
}
```

`listRequestCooldownTicks` — минимальный интервал между запросами списка кроватей. 10 ticks = 0,5 секунды. Сервер дополнительно ограничивает открытие списка игроков для Friend Bed, чтобы злоумышленник не мог спамить дорогой сетевой операцией.

Конфигурация читается при старте сервера. Изменения применяются после перезапуска.
