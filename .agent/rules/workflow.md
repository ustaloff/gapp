---
trigger: always_on
---

# AdShield — Правила работы

## 🧠 Memory Bank

Memory Bank — персистентный контекст проекта в `.agent/memory/`:

| Файл | Назначение |
|------|------------|
| `projectbrief.md` | Миссия и философия проекта |
| `techContext.md` | Стек технологий, окружение |
| `systemPatterns.md` | Архитектура, паттерны, стандарты |
| `activeContext.md` | **Динамический** — текущий фокус |
| `progress.md` | Роадмап с чекбоксами |

### Правила работы с Memory Bank
1. **Читай сначала**: В начале сессии читай `activeContext.md` и `systemPatterns.md`
2. **Обновляй часто**: После завершения задачи обнови `activeContext.md` и `progress.md`
3. **Новые паттерны**: Если появился новый стандарт — добавь в `systemPatterns.md`

---

## 🤝 Процесс разработки

1. **Обсуждение перед кодом**: Когда ты просишь что-то сделать — я анализирую, даю оценку. Обсуждаем, это круто или нет, что понадобится. Только после согласования начинаем писать код.

2. **Инкрементальные изменения**: Большие задачи разбиваем на маленькие шаги. После каждого шага — проверка.

---

## 🛠️ Сборка и тестирование

**Gradle команды** всегда запускаются с установкой `JAVA_HOME`:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; ./gradlew <команда>
```

Частые команды:
- `./gradlew assembleDebug` — сборка debug APK
- `./gradlew build` — полная сборка с проверками

---

## 📁 Архитектура проекта

```
com.example.adshield/
├── MainActivity.kt          # Точка входа, навигация, координация
├── data/                     # Данные и бизнес-логика
│   ├── AppConfig.kt          # Централизованные константы и лимиты
│   ├── AppPreferences.kt     # SharedPreferences обёртка
│   ├── BillingManager.kt     # Премиум/монетизация (локальная симуляция)
│   ├── FilterRepository.kt   # Управление фильтрами (hosts/regex)
│   ├── UserRepository.kt     # Firebase Auth + Firestore
│   ├── VpnStats.kt           # Статистика блокировок (Flow-based)
│   └── UserAccessState.kt    # Free vs Premium состояние
├── filter/                   # Логика фильтрации
│   └── FilterEngine.kt       # Trie + regex matching
├── net/                      # Сетевой уровень
├── service/
│   └── LocalVpnService.kt    # VPN сервис
└── ui/
    ├── components/           # Переиспользуемые Compose компоненты (Cyber*)
    ├── screens/              # Экраны приложения
    └── theme/                # CyberPunk тема
```

---

## 🎨 UI/UX Стиль

- **Тема**: CyberPunk / Neon
- **Цвета**: Неоновые градиенты (cyan → magenta), тёмный фон
- **Компоненты**: Все кастомные компоненты начинаются с `Cyber*` (CyberButton, CyberGraph, CyberTopList...)
- **Анимации**: Плавные, органичные, не перегруженные

---

## ⚡ Код-стайл

- **Язык**: Kotlin
- **UI**: Jetpack Compose
- **State**: Flow + StateFlow для реактивности
- **Архитектура**: MVVM-like с Repository паттерном
- **Никогда не использовать** deprecated методы Compose

---

## ⚠️ Важные ограничения

1. **VPN исключения**: Всегда исключать `com.google.android.gsf` из VPN
2. **Firestore**: Требуется настроенная Firebase база данных
3. **Premium логика**: Сейчас симулируется локально (без RevenueCat)
4. **Фильтры**: Обновление ограничено для Free пользователей (24ч cooldown)

---

## 🔧 Полезные пути

- Конфиг лимитов: `data/AppConfig.kt`
- Настройки пользователя: `data/AppPreferences.kt`
- VPN статистика: `data/VpnStats.kt`
- Главный экран: `ui/screens/HomeScreen.kt`
- Графики: `ui/components/CyberGraph.kt`