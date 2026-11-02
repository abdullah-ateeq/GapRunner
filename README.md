# GapRunner 🚀🎮

**GapRunner** is an exciting, fast-paced arcade runner Android game built natively with **Kotlin** and **Jetpack Compose**. Navigate through challenging gaps, test your reflexes, achieve high scores, and enjoy immersive sound and haptic feedback!

---

## ✨ Features

- **Modern UI & Animations:** Built entirely with **Jetpack Compose** and **Material 3** components, featuring sleek screens (Splash, Home, Game, Result, Settings, and About).
- **Smooth Game Engine:** Custom high-performance game loop utilizing Kotlin Coroutines and StateFlow for precise collision detection and smooth frame rendering.
- **Persistent High Scores & Preferences:** Utilizes **Jetpack DataStore** to locally store best scores, sound settings, and haptic feedback preferences.
- **Immersive Feedback:** Custom sound effects and haptic vibrations for actions, collisions, and milestones.
- **Adaptive Architecture:** Clean separation of concerns with MVVM architecture, robust state management, and comprehensive unit tests.

---

## 🛠️ Tech Stack & Libraries

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3
- **Architecture:** MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow
- **Navigation:** Jetpack Navigation Compose
- **Data Persistence:** Jetpack DataStore Preferences
- **Testing:** JUnit & Compose UI Testing

---

## 📁 Project Structure

```text
com.example.gaprunner/
├── audio/          # Sound effects & haptic feedback management
├── data/           # DataStore repository for preferences & scores
├── game/           # Core game engine, collision detection, and score calculation
├── ui/
│   ├── navigation/ # Navigation routes & graphs
│   ├── screens/    # Splash, Home, Game, Result, Settings, and About screens
│   ├── theme/      # Material 3 colors, typography, and theme setup
│   └── viewmodel/  # Game & Settings ViewModels
└── MainActivity.kt
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Koala or later.
- JDK 11 or higher.
- Android SDK (minSdk 24, targetSdk 37).

### Installation & Running
1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/GapRunner.git
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle and run the app on an Android emulator or physical device.

---

## 🧪 Testing

Run unit tests locally via Gradle:
```bash
./gradlew test
```

---

## 📄 License

This project is licensed under the MIT License.
