# DOWWREY Beds — Modrinth

## Основные данные

**Название:** DOWWREY Beds  
**Slug:** `dowwrey-beds`  
**Тип:** Mod  
**Minecraft:** 26.3  
**Loader:** Fabric  
**Окружение:** Client + Server  
**Java:** 25+  
**Fabric API:** 0.162.0+26.3  

### Краткое описание

> Сохраняйте найденные кровати, выбирайте точки возрождения и возвращайтесь к ним за Око Эндера — в аккуратном стиле ванильного Minecraft.

---

# Русская версия

## DOWWREY Beds

**DOWWREY Beds** — система сохранённых кроватей для Minecraft 26.3 на Fabric, созданная с упором на ванильный баланс, удобство и качественный интерфейс.

Мод позволяет сохранять найденные кровати, управлять ими через отдельное меню, выбирать кровать для возрождения, телепортироваться к сохранённым точкам за Око Эндера и передавать сохранённые кровати другим игрокам.

Главный принцип DOWWREY Beds — удобство без превращения Minecraft в свободный `/home`. Телепортация работает только к реально сохранённым кроватям и требует ресурс.

## Возможности

### 🛏 Сохранённые кровати

- До 10 сохранённых кроватей на игрока.
- Уникальные названия.
- Координаты и измерение.
- Выбор активной кровати.
- Редактирование.
- Удаление.
- Индивидуальный цвет линии.
- Индивидуальная иконка.
- Сохранение данных между перезапусками.

### ★ Возрождение

После смерти можно выбрать любую доступную сохранённую кровать.

Активная кровать отмечена:

> ★ Название

Перед возрождением сервер повторно проверяет состояние точки и безопасное место.

### 🤝 Friend Beds

Можно отправить сохранённую кровать другому игроку.

После принятия:

- кровать появляется у получателя;
- у отправителя она не создаётся повторно;
- сервер повторно проверяет лимит, название и данные точки;
- запрос имеет ограниченный срок действия.

### ✦ Телепортация

Телепортация возможна только к сохранённым кроватям.

| Время | Стоимость |
|---|---:|
| 6 секунд | 1 Око Эндера |
| 3 секунды | 2 Ока Эндера |
| 1 секунда | 3 Ока Эндера |

После успешной телепортации действует cooldown 25 секунд.

Телепортация отменяется только при получении реального урона. При отмене Око возвращается.

### ⚠ Сломанная кровать

Если сохранённая кровать физически разрушена, она получает статус:

> Сломана

Оригинальное название сохраняется.

Для сломанной точки:

- 5 секунд;
- 3 Ока Эндера.

После успешной телепортации запись удаляется.

Незагруженный чанк не считается автоматически сломанной кроватью.

### 🎨 Цвета и иконки

Для каждой кровати можно выбрать цвет линии и иконку.

Примеры иконок:

- Дом;
- Шахта;
- Деревня;
- Замок;
- Ферма;
- Лагерь;
- Портал;
- Лес;
- Незер;
- Край.

Специальные состояния имеют приоритет:

- активная кровать — золотая линия;
- сломанная кровать — красная линия.

### 🔔 Уведомления

Обычные игровые сообщения не засоряют чат.

Используется отдельная система уведомлений для:

- сохранения;
- редактирования;
- удаления;
- Friend Bed;
- телепортации;
- отмены;
- cooldown;
- недостатка ресурсов;
- изменения состояния кровати.

### ⚙ Настройки

Доступны клиентские настройки:

- уведомления;
- эффекты телепорта;
- отображение cooldown;
- плавные анимации.

Серверные настройки позволяют менять:

- максимальное количество кроватей;
- Friend Beds;
- телепортацию;
- телепортацию к сломанным кроватям;
- cooldown;
- стоимость и длительность телепорта;
- срок действия запросов;
- server-side rate limits.

### 🎛 Управление

По умолчанию:

> F4 — открыть/закрыть меню

Также можно назначить любую клавишу в:

> Настройки → Управление → DOWWREY Beds

`Esc` закрывает меню.

### 🌍 Измерения

Поддерживаются:

- Верхний мир;
- Незер;
- Край;
- другие зарегистрированные измерения.

### 🛡 Multiplayer

Ключевые действия проверяются на сервере:

- сохранение;
- редактирование;
- удаление;
- Friend Beds;
- respawn;
- teleport;
- cooldown;
- расход и возврат Ока Эндера.

Мод рассчитан на одиночную игру и dedicated servers.

## Установка

1. Установите Minecraft 26.3.
2. Установите Fabric Loader.
3. Установите Java 25+.
4. Установите Fabric API 0.162.0+26.3.
5. Поместите `DOWWREY-Beds-1.0.0.jar` в `.minecraft/mods`.
6. Запустите Minecraft через Fabric.

Для полной multiplayer-функциональности мод должен быть установлен и на сервере.

## Зависимости

### Обязательные

- Fabric Loader 0.19.5+
- Fabric API 0.162.0+26.3
- Java 25+

### Не требуются

- Fabric GUI ImGui
- GeckoLib
- FranklyLib
- YACL
- сторонние shader-паки

## FAQ

### Можно хранить больше 10 кроватей?

Нет, базовый лимит — 10.

### Можно одинаковые названия?

Нет, у одного игрока названия должны быть уникальными.

### Что происходит после разрушения кровати?

Точка становится «Сломана». Оригинальное название не изменяется.

### Можно телепортироваться к сломанной кровати?

Да, специальный телепорт стоит 3 Ока и занимает 5 секунд. После успешного телепорта запись удаляется.

### Что будет, если получить урон?

Телепортация отменяется, Око возвращается.

### Можно телепортироваться по координатам?

Нет. Только к сохранённым точкам кроватей.

### Можно передать кровать другу?

Да.

### Нужно ставить мод на сервер?

Для полноценной multiplayer-функциональности — да.

## Changelog — 1.0.0

Первый публичный релиз DOWWREY Beds.

- сохранение до 10 кроватей;
- менеджер кроватей;
- активная кровать;
- respawn;
- Friend Beds;
- телепортация за Око Эндера;
- 6 / 3 / 1 секунда;
- 1 / 2 / 3 Ока;
- cooldown 25 секунд;
- отмена по урону;
- возврат Ока;
- сломанные кровати;
- цвета и иконки;
- уведомления;
- настройки;
- серверный конфиг;
- адаптивный UI;
- GUI Scale 1–4;
- F11;
- настраиваемый keybind.

## AI disclosure

При разработке проекта использовались инструменты генеративного ИИ для части программирования, анализа, исправления кода и подготовки документации.

Использование ИИ должно быть раскрыто на странице Modrinth в соответствии с актуальной политикой платформы.

## Лицензия

**Лицензия:** `[ЗАПОЛНИТЬ]`

## Ссылки

**Исходный код:** `[ЗАПОЛНИТЬ]`  
**Issues:** `[ЗАПОЛНИТЬ]`  
**Discord:** `[ЗАПОЛНИТЬ]`  
**Wiki:** `[ЗАПОЛНИТЬ]`

---

# English Version

## DOWWREY Beds

**DOWWREY Beds** is a saved-bed system for Minecraft 26.3 on Fabric, designed around vanilla balance, convenience and a clean, polished interface.

Save real beds you discover, manage them in a dedicated menu, choose a respawn bed, teleport back to saved locations using Eyes of Ender, and send saved beds to other players.

The main idea behind DOWWREY Beds is convenience without turning Minecraft into a free `/home` system. Teleportation only works with real saved beds and requires a resource.

## Features

### 🛏 Saved Beds

- Up to 10 saved beds per player.
- Unique names.
- Coordinates and dimension.
- Select an active bed.
- Rename and edit beds.
- Delete beds.
- Custom line color.
- Custom icon.
- Persistent storage across restarts.

### ★ Respawn

After death, you can choose any available saved bed.

The active bed is marked with:

> ★ Bed Name

The server validates the destination and a safe respawn position before completing the action.

### 🤝 Friend Beds

Send a saved bed to another player.

After acceptance:

- the bed is added to the recipient;
- it is not duplicated for the sender;
- the server re-validates the bed, name and bed limit;
- requests expire after a limited time.

### ✦ Teleportation

Teleportation only works to saved beds.

| Time | Cost |
|---|---:|
| 6 seconds | 1 Eye of Ender |
| 3 seconds | 2 Eyes of Ender |
| 1 second | 3 Eyes of Ender |

A successful teleport starts a 25-second cooldown.

Teleportation is cancelled only when the player takes real damage. The Eye(s) of Ender are refunded when a teleport is cancelled.

### ⚠ Broken Beds

When a saved bed is physically destroyed, it becomes:

> Broken

The original bed name remains unchanged.

Broken-bed teleport:

- 5 seconds;
- 3 Eyes of Ender.

After a successful teleport, the broken bed entry is removed.

An unloaded chunk does not automatically mean that a bed is broken.

### 🎨 Colors and Icons

Each saved bed can have its own line color and icon.

Example icons:

- Home;
- Mine;
- Village;
- Castle;
- Farm;
- Camp;
- Portal;
- Forest;
- Nether;
- End.

Special states override the custom line color:

- active bed — gold line;
- broken bed — red line.

### 🔔 Notifications

DOWWREY Beds does not spam the normal chat with routine notifications.

A dedicated notification system is used for:

- save;
- edit;
- delete;
- Friend Beds;
- teleport;
- cancellation;
- cooldown;
- missing resources;
- bed state changes.

### ⚙ Settings

Client settings include:

- notifications;
- teleport effects;
- cooldown display;
- smooth animations.

Server settings can control:

- maximum beds;
- Friend Beds;
- teleportation;
- broken-bed teleportation;
- cooldown;
- teleport cost and duration;
- request lifetime;
- server-side rate limits.

### 🎛 Controls

Default key:

> F4 — open/close the DOWWREY menu

The key can be changed in:

> Options → Controls → DOWWREY Beds

`Esc` closes the current menu.

### 🌍 Dimensions

Supported:

- Overworld;
- Nether;
- End;
- other registered dimensions.

### 🛡 Multiplayer

Important actions are validated server-side:

- save;
- edit;
- delete;
- Friend Beds;
- respawn;
- teleport;
- cooldown;
- Eye of Ender consumption and refunds.

The mod is designed for both singleplayer and dedicated servers.

## Installation

1. Install Minecraft 26.3.
2. Install Fabric Loader.
3. Install Java 25+.
4. Install Fabric API 0.162.0+26.3.
5. Put `DOWWREY-Beds-1.0.0.jar` into `.minecraft/mods`.
6. Launch Minecraft using Fabric.

For full multiplayer functionality, DOWWREY Beds must also be installed on the server.

## Dependencies

### Required

- Fabric Loader 0.19.5+
- Fabric API 0.162.0+26.3
- Java 25+

### Not required

- Fabric GUI ImGui
- GeckoLib
- FranklyLib
- YACL
- third-party shader packs

## FAQ

### Can I store more than 10 beds?

No. The default limit is 10 beds per player.

### Can two beds have the same name?

No. Bed names must be unique for each player.

### What happens when a bed is destroyed?

The saved point becomes `Broken`. Its original name is preserved.

### Can I teleport to a broken bed?

Yes. Broken-bed teleport costs 3 Eyes of Ender and takes 5 seconds. After a successful teleport, the entry is removed.

### What happens if I take damage during teleportation?

The teleport is cancelled and the Eyes of Ender are refunded.

### Can I teleport to arbitrary coordinates?

No. Teleportation is limited to saved bed locations.

### Can I send a bed to another player?

Yes.

### Does the mod need to be installed on the server?

Yes, for full multiplayer functionality.

## Changelog — 1.0.0

First public release of DOWWREY Beds.

- up to 10 saved beds;
- saved-bed manager;
- active bed;
- respawn;
- Friend Beds;
- Eye of Ender teleportation;
- 6 / 3 / 1 second teleport modes;
- 1 / 2 / 3 Eyes of Ender;
- 25-second cooldown;
- damage cancellation;
- Eye refunds;
- broken-bed handling;
- colors and icons;
- notifications;
- settings;
- server configuration;
- responsive UI;
- GUI Scale 1–4;
- fullscreen support;
- configurable keybind.

## AI disclosure

Generative AI tools were used during development for parts of programming, code analysis, debugging and documentation.

AI usage should be disclosed on the Modrinth project page according to the current platform policy.

## License

**License:** `[FILL IN]`

## Links

**Source:** `[FILL IN]`  
**Issues:** `[FILL IN]`  
**Discord:** `[FILL IN]`  
**Wiki:** `[FILL IN]`

---

# Modrinth metadata

## Project

**Name:** `DOWWREY Beds`  
**Slug:** `dowwrey-beds`  
**Type:** `Mod`  
**Minecraft:** `26.3`  
**Loader:** `Fabric`  
**Environment:** `Client + Server`  
**Java:** `25+`

## Version

**Version number:** `1.0.0`  
**Release channel:** `Release`  
**Game version:** `26.3`  
**Loader:** `Fabric`

## Suggested categories

- Adventure
- Utility
- Fabric

## Required dependency

- Fabric API `0.162.0+26.3`

## Public summary

**RU:**

> Сохраняйте найденные кровати, выбирайте точки возрождения и возвращайтесь к ним за Око Эндера — в аккуратном стиле ванильного Minecraft.

**EN:**

> Save discovered beds, choose respawn points, and return to them using Eyes of Ender — all in a clean vanilla-style Minecraft experience.
