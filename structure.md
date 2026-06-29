# Structure du Projet : Android Micro-Widget Météo (Nowcasting)

## 1. Vue d'ensemble du projet
L'objectif est de créer une application Android ultra-légère dont l'interface principale est un **Widget d'écran d'accueil**. 
Le widget doit récupérer la position GPS exacte de l'utilisateur, interroger trois APIs météo distinctes (non-redondantes), analyser les données et afficher la probabilité de précipitation pour les trois prochaines heures (H+1, H+2, H+3).

### Spécificités de l'analyse :
Pour éviter la redondance, l'application croise trois modèles mathématiques et physiques différents :
1. **Modèle Local Canadien (GEM/HRDPS) via Open-Meteo** (Précision géographique de 2,5 km).
2. **Modèle Radar / Nowcasting US via Tomorrow.io** (Données radars et maillage hyper-local).
3. **Modèle Global / Européen (ECMWF) via OpenWeatherMap** (Consensus mondial recalculé).

---

## 2. Architecture Technique (Android)

* **Langage :** Kotlin (ou Python/Kivy si spécifié, mais Kotlin recommandé pour les widgets natifs).
* **Version Android minimum :** Android 8.0 (API 26) pour la gestion moderne des tâches de fond.
* **Composants clés :**
    * `FusedLocationProviderClient` : Pour récupérer la latitude et la longitude exactes de l'appareil.
    * `GlanceAppWidget` (Jetpack Glance) ou `AppWidgetProvider` : Pour bâtir l'interface graphique du widget.
    * `WorkManager` : Pour planifier la mise à jour automatique en arrière-plan (ex: toutes les 30 ou 60 minutes) sans vider la batterie.
    * `HttpURLConnection` ou `OkHttp` : Pour effectuer les requêtes réseaux asynchrones vers les APIs.

---

## 3. Configuration des APIs et Endpoints

L'agent IA devra formater les requêtes HTTP GET en injectant dynamiquement les variables `${lat}` et `${lon}` récupérées par le GPS.

### Source A : Open-Meteo (Modèle Canadien HRDPS)
* **URL de base :** `https://api.open-meteo.com/v1/forecast`
* **Paramètres requis :** `latitude=${lat}&longitude=${lon}&hourly=precipitation_probability&models=gem_hrdps&forecast_days=1`
* **Extraction :** Récupérer le tableau `hourly.precipitation_probability` pour les index correspondants aux 3 prochaines heures.

### Source B : Tomorrow.io (Nowcasting & Radars)
* **URL de base :** `https://api.tomorrow.io/v4/weather/forecast`
* **Paramètres requis :** `location=${lat},${lon}&timesteps=1h&fields=precipitationProbability&apikey=VOTRE_CLE_API`
* **Extraction :** Parcourir la structure `timelines.hourly` et extraire `values.precipitationProbability`.

### Source C : OpenWeatherMap (Modèle Global / One Call)
* **URL de base :** `https://api.openweathermap.org/data/3.0/onecall`
* **Paramètres requis :** `lat=${lat}&lon=${lon}&exclude=current,minutely,daily,alerts&appid=VOTRE_CLE_API`
* **Extraction :** Extraire le champ `pop` (Probability of Precipitation) du tableau `hourly`. Note : multiplier par 100 car la valeur est retournée entre 0.0 et 1.0.

---

## 4. Logique de Traitement des Données (Algorithme)

Pour chaque heure cible (H+1, H+2, H+3), l'application doit :
1. Extraire le pourcentage obtenu auprès des 3 sources.
2. Calculer la moyenne (ou conserver les 3 valeurs pour affichage complet, ex: "Can: 40% | US: 60% | Eu: 45%").
3. Si une API échoue (problème de clé, réseau), l'algorithme doit ignorer la source manquante et faire la moyenne sur les deux autres restantes.

---

## 5. Interface Utilisateur du Widget (UI Design)

Le widget doit être compact (format 2x1 ou 3x1 sur la grille Android).