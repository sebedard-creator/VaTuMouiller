# Architecture du Projet - VaTuMouiller

Ce document décrit la structure, la stack technique et les choix d'architecture de l'application Android **VaTuMouiller**.

## Stack Technique
*   **Langage :** Kotlin 1.9.24+
*   **Version SDK :** Minimum SDK 26 (Android 8.0), Target SDK 34 (Android 14)
*   **Réseau & Parsing :** Retrofit 2.9.0 + OkHttp 4.12.0 + Kotlinx Serialization 1.6.2
*   **Localisation :** Google Play Services Fused Location Provider 21.1.0
*   **Tâches en arrière-plan :** WorkManager 2.9.0
*   **Widget UI :** Jetpack Glance 1.0.0 (Compose-like)
*   **Stockage local (Cache) :** Android SharedPreferences pour stocker les prévisions consensus (H+1 à H+4), la valeur d'opacité personnalisée du fond du widget et le timestamp de dernière mise à jour.
*   **Injection de dépendances :** Manuelle (Service Locator simple).

## Structure des répertoires du projet
Le projet est organisé de manière modulaire au sein du module unique `:app` :
```
Y:/VaTuMouiller/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── res/
│           │   ├── layout/              # Mises en page XML (ex: my_loading_layout.xml)
│           │   └── xml/                 # Métadonnées du widget (ex: weather_widget_info.xml)
│           └── java/com/sebed/vatumouiller/
│               ├── di/                  # Service Locator pour l'injection manuelle
│               │   └── DependencyContainer.kt
│               ├── gps/                 # Helpers pour FusedLocationProviderClient
│               │   └── FusedLocationHelper.kt
│               ├── data/                # Modèles de données, interfaces API et Repository
│               │   ├── model/           # Modèles de réponse JSON pour Open-Meteo, Tomorrow, OpenWeather
│               │   ├── api/             # Interfaces Retrofit
│               │   └── repository/      # WeatherRepository (logique de consensus et cache)
│               ├── worker/              # Planification en arrière-plan avec WorkManager
│               │   └── WeatherUpdateWorker.kt
│               ├── widget/              # Composants Jetpack Glance
│               │   ├── WeatherWidget.kt
│               │   └── WeatherWidgetReceiver.kt
│               └── ui/                  # Activité de débogage pour tester le backend/GPS
│                   └── DebugActivity.kt
├── build.gradle.kts
├── settings.gradle.kts
├── local.properties (non versionné)
└── architecture.md
```

## Conventions de Code
*   **Secrets** : Interdiction formelle de hardcoder des clés API ou autres secrets. Les clés d'API (Tomorrow.io et OpenWeatherMap) doivent être spécifiées dans `local.properties` et sont injectées via le BuildConfig par Gradle.
*   **Gestion des erreurs réseau** : Chaque appel API est enveloppé individuellement pour éviter qu'un échec d'un service ne fasse échouer les autres (consensus glissant).
*   **Modèle matériel** : L'accès GPS doit gérer le cas où la permission est absente ou le GPS désactivé en arrière-plan.
*   **Widget** : Le Widget n'est dessiné qu'après validation complète du backend via l'activité de débogage.
*   **Interpolation Temporelle** : Les prévisions de pluie glissantes (0-60m, 61-120m, etc.) sont calculées via une moyenne pondérée par la minute courante des heures absolues des API, garantissant une exactitude temporelle relative lors de la consultation.
*   **Rendu réactif du Widget** : Utilisation de `LocalSize.current` avec `SizeMode.Exact` pour changer dynamiquement l'affichage en format 4x1 (les textes s'agrandissent à 34sp et l'heure H+4 s'ajoute). Les éléments sont placés dans des `Box` imbriqués avec `contentAlignment` pour respecter le système Glance de `RemoteViews`.
*   **Opacité dynamique** : L'opacité du widget est lue depuis le cache local à chaque dessin, permettant sa mise à jour instantanée en arrière-plan à la modification du slider de l'application.
