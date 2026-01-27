# YourBagBuddy

A production-ready Android application to help travelers pack correctly and never forget essentials.

## 🎯 Project Overview

YourBagBuddy is an offline-first Android app built with modern Android development practices. It helps users:
- Create and manage trips
- Build manual packing checklists
- Generate AI-based smart packing lists
- Access travel medicine suggestions

## 🏗️ Architecture

The app follows **Clean Architecture** principles with clear separation of concerns:

```
app/
├── domain/          # Business logic layer
│   ├── model/      # Domain models
│   ├── repository/ # Repository interfaces
│   └── usecase/    # Business use cases
├── data/           # Data layer
│   ├── local/      # Room database (entities, DAOs)
│   ├── remote/     # Firebase data sources
│   └── repository/ # Repository implementations
└── presentation/  # UI layer
    ├── screen/     # Compose screens
    ├── viewmodel/  # ViewModels
    └── navigation/ # Navigation setup
```

### Key Architectural Decisions

1. **Offline-First**: Room database is the single source of truth
2. **Repository Pattern**: Abstracts data sources, making Firebase replaceable
3. **Use Cases**: Encapsulate business logic independently of UI
4. **MVVM**: ViewModels handle UI state, screens are stateless

## 🛠️ Tech Stack

- **UI**: Jetpack Compose, Material 3
- **Architecture**: MVVM, Clean Architecture
- **Dependency Injection**: Hilt
- **Local Database**: Room
- **Cloud Sync**: Firebase Firestore
- **Authentication**: Firebase Auth (Email, Google, Guest)
- **Async**: Kotlin Coroutines, Flow
- **Navigation**: Navigation Compose

## 📦 Features

### 1. Trip Management
- Create, edit, and delete trips
- Trip fields: name, destination, dates, people count, trip type
- Progress tracking per trip

### 2. Manual Packing Checklist
- Add, edit, delete checklist items
- Mark items as packed/unpacked
- Categorized items (Clothes, Essentials, Documents, Other)
- Real-time progress tracking

### 3. Smart Pack (AI Checklist)
- Generate packing lists based on:
  - Destination
  - Month/Date
  - Trip duration
  - Number of people
  - Trip type
- Edit and customize generated items
- Save generated lists as trips

### 4. Travel Medicine Suggestions
- Static content with categories:
  - Fever/Pain
  - Stomach issues
  - Motion sickness
  - First aid
  - Dehydration
- Clear disclaimers (no medical advice)

## 🔐 Authentication

- **Guest Mode**: Works offline, data stored locally
- **Email/Password**: Firebase Authentication
- **Google Sign-In**: Firebase Authentication
- **Sync**: Automatic sync when logged in (last-write-wins)

## 💾 Data Storage

### Local (Room Database)
- Primary source of truth
- Works completely offline
- Tables: `trips`, `checklist_items`

### Cloud (Firebase Firestore)
- Syncs when user is logged in
- Structure: `users/{userId}/trips/{tripId}`
- Conflict resolution: Last-write-wins

## 🚀 Future Backend Migration

The architecture is designed to easily replace Firebase with a custom backend:

### Current Firebase Dependencies
- `AuthRepositoryImpl` - Firebase Auth
- `FirebaseTripDataSource` - Firestore sync
- `FirebaseModule` - DI setup

### Migration Path to Ktor + MongoDB
1. **Replace AuthRepositoryImpl**: Implement JWT-based auth
2. **Replace FirebaseTripDataSource**: Create REST API data source
3. **Update RepositoryModule**: Bind new implementations
4. **No UI changes required**: Domain layer remains unchanged

### Example Migration Steps

```kotlin
// 1. Create new data source
class KtorTripDataSource @Inject constructor(
    private val api: TripApi
) {
    suspend fun syncTrips(userId: String, trips: List<TripEntity>): Result<Unit> {
        // REST API calls
    }
}

// 2. Update repository to use new data source
class TripRepositoryImpl @Inject constructor(
    private val tripDao: TripDao,
    private val ktorTripDataSource: KtorTripDataSource  // Changed
) : TripRepository { ... }

// 3. Update DI module
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    fun provideTripApi(): TripApi { ... }
}
```

## 📱 UI/UX

- **Material 3** design system
- **Dark mode** support
- **Color Palette**:
  - Primary: #F07A3F
  - Secondary: #F6B48A
  - Background: #FAF7F4
  - Success: #34C759

## 🧪 Testing Strategy

- **Unit Tests**: ViewModels, Use Cases, Repositories
- **Integration Tests**: Database operations
- **UI Tests**: Compose UI testing

## 📋 Setup Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd YourBagBuddy
   ```

2. **Add Firebase Configuration**
   - Place `google-services.json` in `app/` directory
   - Configure Firebase project with Authentication and Firestore

3. **Build and Run**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

## 🔧 Configuration

### Firebase Setup
1. Create Firebase project
2. Enable Authentication (Email/Password, Google)
3. Enable Firestore Database
4. Download `google-services.json` to `app/` directory

### Room Database
- Database name: `yourbagbuddy_database`
- Version: 1
- Auto-migration: Not configured (manual migrations required)

## 📝 Code Quality

- **Readable code**: Clear naming, comments where needed
- **Modular**: Each feature is self-contained
- **Testable**: ViewModels and Use Cases are easily testable
- **Scalable**: Architecture supports 10k+ users
- **Maintainable**: Clean separation of concerns

## 🎨 Design Principles

- **Clarity > Cleverness**: Simple, readable code
- **Simplicity > Overengineering**: Only add complexity when needed
- **Offline-First**: App works without internet
- **Graceful Degradation**: AI and sync features fail gracefully

## 📄 License

[Add your license here]

## 👥 Contributors

[Add contributors here]

## 📞 Support

[Add support information here]

---

**Note**: This is a production application designed for real-world use. The architecture prioritizes maintainability, scalability, and future flexibility.
