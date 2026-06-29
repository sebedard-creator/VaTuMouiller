# Changelog - VaTuMouiller

Toutes les modifications notables apportées à ce projet seront consignées dans ce fichier.

## [Non publié] - 2026-06-28
### Ajouté
- Documentation initiale du projet : [structure.md](file:///Y:/VaTuMouiller/structure.md), [architecture.md](file:///Y:/VaTuMouiller/architecture.md), et [changelog.md](file:///Y:/VaTuMouiller/changelog.md).
- Configuration du projet Gradle Kotlin DSL avec support de Java 1.8, configurations AndroidX, activation de Jetpack Compose (requis pour Glance) et injection BuildConfig via [gradle.properties](file:///Y:/VaTuMouiller/gradle.properties).
- Gestion sécurisée des clés d'API via [local.properties](file:///Y:/VaTuMouiller/local.properties) et [.gitignore](file:///Y:/VaTuMouiller/.gitignore).
- Service Locator simple pour l'injection manuelle de dépendances ([DependencyContainer.kt](file:///Y:/VaTuMouiller/app/src/main/java/com/sebed/vatumouiller/di/DependencyContainer.kt)).
- Helper GPS asynchrone basé sur FusedLocationProviderClient ([FusedLocationHelper.kt](file:///Y:/VaTuMouiller/app/src/main/java/com/sebed/vatumouiller/gps/FusedLocationHelper.kt)).
- Parsing et interfaces Retrofit pour Open-Meteo, Tomorrow.io et OpenWeatherMap ([WeatherResponseModels.kt](file:///Y:/VaTuMouiller/app/src/main/java/com/sebed/vatumouiller/data/model/WeatherResponseModels.kt)).
- Logique métier de consensus météo glissant avec fallback vers cache local ([WeatherRepository.kt](file:///Y:/VaTuMouiller/app/src/main/java/com/sebed/vatumouiller/data/repository/WeatherRepository.kt)).
- Interface graphique et contrôleur de débogage pour tester les permissions et APIs en direct ([DebugActivity.kt](file:///Y:/VaTuMouiller/app/src/main/java/com/sebed/vatumouiller/ui/DebugActivity.kt)).
- Tâche de fond automatique toutes les 30 minutes utilisant WorkManager ([WeatherUpdateWorker.kt](file:///Y:/VaTuMouiller/app/src/main/java/com/sebed/vatumouiller/worker/WeatherUpdateWorker.kt)).
- Widget d'écran d'accueil Glance Compose-like avec style sombre premium ([WeatherWidget.kt](file:///Y:/VaTuMouiller/app/src/main/java/com/sebed/vatumouiller/widget/WeatherWidget.kt) et [WeatherWidgetReceiver.kt](file:///Y:/VaTuMouiller/app/src/main/java/com/sebed/vatumouiller/widget/WeatherWidgetReceiver.kt)).

### Corrigé
- Ajout de la tâche stub `prepareKotlinBuildScriptModel` dans les fichiers build.gradle.kts pour résoudre l'erreur d'importation sous Android Studio avec Gradle 9.
- Mise à niveau de l'Android Gradle Plugin vers la version 8.5.0 et de Kotlin vers la version 1.9.24 pour résoudre l'erreur de mutation de dépendance (`debugCompileClasspath`) avec Gradle 9.
- Activation de Jetpack Compose dans buildFeatures et configuration de composeOptions dans app/build.gradle.kts pour résoudre l'erreur de ressources manquantes AAPT.
- Correction de la référence de layout initial dans [weather_widget_info.xml](file:///Y:/VaTuMouiller/app/src/main/res/xml/weather_widget_info.xml) pour pointer vers un layout personnalisé extrêmement simple [my_loading_layout.xml](file:///Y:/VaTuMouiller/app/src/main/res/layout/my_loading_layout.xml) afin de contourner tout crash d'inflation par le launcher (Nova Launcher).
- Correction du thème de l'application dans [AndroidManifest.xml](file:///Y:/VaTuMouiller/app/src/main/AndroidManifest.xml) pour utiliser `@style/Theme.AppCompat.DayNight.NoActionBar` au lieu de `@android:style/Theme.DeviceDefault` afin de corriger le crash fatal d'incompatibilité de thème de `DebugActivity` au démarrage.
- Sécurisation du `WeatherWidgetReceiver` en enveloppant les appels à `WorkManager` dans des blocs `try-catch` pour prévenir tout crash lié à une initialisation tardive ou non configurée de WorkManager.
- Retrait de l'enveloppe `GlanceTheme` dans `WeatherWidget.kt` pour éviter les pannes de thèmes Material3 non configurés sur certains lanceurs Android.
- Correction des déclarations `ColorProvider` dans `WeatherWidget.kt` pour utiliser des instances de `androidx.compose.ui.graphics.Color` plutôt que des entiers de couleur Android (`android.graphics.Color`), empêchant ainsi l'inflation système de lever une exception `Resources.NotFoundException` (qui causait le plantage "cant load widget").
- Amélioration de l'affichage du label `(Cache)` sur le widget : il ne s'affiche désormais que si les données en cache ont plus de 40 minutes (indiquant une déconnexion ou panne de synchronisation), évitant la confusion lorsque les données viennent d'être actualisées avec succès.
- Ajout de la prévision pour la 4e heure (H+4, minutes 181 à 240) dans la logique de consensus de `WeatherRepository`, la console de `DebugActivity` et l'affichage du widget. Les valeurs de chaque tranche relative (0-60min, 61-120min, 121-180min, 181-240min) sont désormais calculées via une moyenne pondérée par la minute actuelle (ex: à 14h15, la tranche 0-60min est composée à 75% du consensus de 14h00 et à 25% du consensus de 15h00), garantissant une précision totale par rapport à la tranche glissante réelle.
- Suppression du libellé d'en-tête "Pluie (Consensus)" sur le widget pour libérer de l'espace vertical.
- Implémentation du rendu réactif (`LocalSize.current` + `SizeMode.Exact`) sur le widget : si le widget est au format large 4x1 (largeur >= 250dp), la taille des textes grossit significativement (pourcentage à 28sp, labels à 13sp) et affiche l'heure H+4 pour remplir harmonieusement l'espace. De plus, les dimensions se réduisent intelligemment en hauteur (isTall) pour éviter le clipping vertical des textes de mise à jour (HH:mm) dans les cellules minces (57dp de hauteur).
- Ajout d'une fonctionnalité d'opacité ajustable du fond du widget : introduction d'un composant `SeekBar` (slider) dans l'interface de l'application, persistance de la valeur dans `SharedPreferences` et redessinage en temps réel du widget via `GlanceAppWidgetManager` lors du glissement.



