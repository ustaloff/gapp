# System Patterns

## Architecture Overview
```
┌─────────────────────────────────────────────────────┐
│                   MainActivity                       │
│              (Navigation + Coordination)             │
├─────────────────────────────────────────────────────┤
│                    UI Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │ HomeScreen  │  │ StatsScreen │  │SettingsScreen│ │
│  └─────────────┘  └─────────────┘  └─────────────┘  │
├─────────────────────────────────────────────────────┤
│                   Data Layer                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │VpnStats     │  │FilterRepo   │  │UserRepo     │  │
│  │(StateFlow)  │  │(Hosts+Regex)│  │(Firebase)   │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  │
├─────────────────────────────────────────────────────┤
│                  Service Layer                       │
│  ┌─────────────────────────────────────────────────┐│
│  │              LocalVpnService                     ││
│  │         (DNS Interception + Filtering)           ││
│  └─────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────┘
```

## UI Patterns

### Cyber* Component Naming
Все кастомные UI компоненты используют префикс `Cyber*`:
- `CyberButton` — стилизованные кнопки
- `CyberGraph` — неоновые графики
- `CyberTopList` — списки с градиентами
- `CyberPowerButton` — главная кнопка включения
- `CyberLogo` — анимированный логотип

### Theme System
- **Цветовая схема**: Dark theme only (CyberPunk)
- **Акценты**: Cyan (#00FFFF) → Magenta (#FF00FF) градиенты
- **Фон**: Тёмный с subtle patterns
- **Анимации**: Плавные, 300-500ms, ease-in-out

## Data Patterns

### State Management
```kotlin
// Используем StateFlow для реактивного UI
private val _state = MutableStateFlow<UiState>(Initial)
val state: StateFlow<UiState> = _state.asStateFlow()

// В Composable собираем как State
val state by viewModel.state.collectAsState()
```

### Repository Pattern
- `VpnStats` — singleton, in-memory статистика с Flow
- `FilterRepository` — загрузка/кэширование фильтров
- `UserRepository` — Firebase Auth + Firestore
- `AppPreferences` — SharedPreferences wrapper

## VPN Patterns

### Package Exclusions
Критически важно исключать некоторые пакеты из VPN:
```kotlin
// ОБЯЗАТЕЛЬНО исключать:
builder.addDisallowedApplication("com.google.android.gsf") // Google Services Framework
// + пользовательские исключения из AppPreferences
```

### Filter Engine
- **Trie** для hosts-based фильтров (быстрый prefix matching)
- **Regex** для сложных паттернов
- **Heuristics** для доменов без явных правил

## Error Handling
- **VPN Permission**: Graceful handling если разрешение не дано
- **Network Errors**: Retry с exponential backoff
- **Filter Updates**: Fallback на кэшированные фильтры
