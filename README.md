# Bastion

Paper 1.21. Кланы, приваты на WorldGuard, ивент GreatHunt.

## Возможности

### Кланы
- Создание, инвайт, кик, выход, disband
- GUI (`/clan`)
- База: `/setbase`, `/base`, `/delbase`
- Подсветка соклановцев
- Friendly fire внутри клана выключается
- PlaceholderAPI

### Приваты (WorldGuard)
- Блок-ядро ставит cuboid-регион `ps_x...`
- Лимиты через LuckPerms-группы
- Голограммы DecentHolograms
- Кастомный TNT
- `/ps`

### GreatHunt
- Раз в сутки выбирается цель (время в конфиге)
- BossBar, таймер, награды/штрафы (Vault / PlayerPoints)
- `/ghswitch`, `/ghreload`

## Команды

| Команда | Кто | Что делает |
|---------|-----|------------|
| `/clan` | игрок | меню и подкоманды клана |
| `/base` | член клана | тп на базу |
| `/setbase` | владелец | поставить базу |
| `/delbase` | владелец | убрать базу |
| `/ps` | игрок | приваты |
| `/ghswitch` | op | вкл/выкл GreatHunt |
| `/ghreload` | op | перезагрузка охоты |

## Зависимости

WorldEdit, WorldGuard, Vault, PlayerPoints, PlaceholderAPI, ProtocolLib, DecentHolograms, LuckPerms.

## Сборка

JDK 21+.

```bash
./gradlew build
```

Jar: `build/libs/Bastion-1.0-SNAPSHOT.jar`

Paper API с Maven. Остальные jar'ы при необходимости в `libs/`.

## Установка

1. Jar в `plugins/` + зависимости
2. Рестарт
3. `config.yml`, `privates.yml`, `guis.yml`

## Структура

```
src/main/java/dev/portfolio/bastion/
  Bastion.java
  Clans/
  Privates/
  GreatHunt/
src/main/resources/
  plugin.yml config.yml privates.yml guis.yml data.yml
```

## Лицензия

MIT, [LICENSE](LICENSE).
