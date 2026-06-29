# VaTuMouiller 🌧️

**VaTuMouiller** est une application Android dotée d'un widget d'écran d'accueil ultra-précis conçu pour répondre à une question essentielle : *"Vais-je me faire mouiller dans les prochaines heures ?"*

Pour fournir l'estimation la plus robuste possible, l'application effectue un consensus en temps réel (moyenne glissante) entre trois services météo de référence : **Open-Meteo**, **Tomorrow.io** et **OpenWeatherMap One Call**.

---

## La Logique Temporelle : Absolue vs Relative (Pondérée)

La plupart des applications météo affichent des prévisions pour des heures fixes de la journée (ex: 14h00, 15h00, 16h00). C'est ce qu'on appelle une approche **absolue**. 
Cependant, pour un widget de type *Nowcasting* (prévisions immédiates à court terme), cette approche présente une faille majeure.

### Le Problème de l'Approche Absolue
Imaginez qu'il soit **14h55** :
*   Une application classique affichera sous le libellé "H+1" la prévision de **15h00**.
*   Or, 15h00 est dans seulement **5 minutes** ! Ce n'est pas "dans une heure".
*   Si de la pluie torrentielle commence à **15h15** et s'arrête à **15h45**, la prévision de 15h00 affichera `0%` de pluie. Vous allez donc sortir en pensant être au sec, et vous faire mouiller 20 minutes plus tard.

### Notre Solution : Les Tranches Glissantes Pondérées (Relative)
Pour corriger cette faille, **VaTuMouiller** calcule les probabilités pour des blocs de temps glissants de 60 minutes par rapport à la minute exacte de consultation :
*   **Tranche 1 :** de maintenant à +60 min (ex: de 14h18 à 15h18)
*   **Tranche 2 :** de +61 min à +120 min (ex: de 15h18 à 16h18)
*   **Tranche 3 :** de +121 min à +180 min (ex: de 15h18 à 17h18)
*   **Tranche 4 :** de +181 min à +240 min (ex: de 17h18 à 18h18)

---

## Le Modèle Mathématique d'Interpolation

Puisque les API météo ne fournissent des données que pour les heures piles (ex: $P_{14h}$ à 14h, $P_{15h}$ à 15h), l'application effectue une **moyenne pondérée par minutes** des heures qui se chevauchent.

Soit $m$ la minute actuelle de l'heure courante (comprise entre $0$ et $59$).
Pour la **Tranche 1 (0 à 60 minutes)**, la tranche couvre :
1.  Les $60 - m$ minutes restantes de l'heure courante (pondération de l'heure $P_{\text{courante}}$).
2.  Les $m$ premières minutes de l'heure suivante (pondération de l'heure $P_{\text{suivante}}$).

La formule mathématique appliquée est :

$$\text{Probabilité Tranche 1} = \frac{(P_{\text{courante}} \times (60 - m)) + (P_{\text{suivante}} \times m)}{60}$$

### Exemples Concrets de Transition
Prenons un scénario où il y a **100% de pluie** prévue entre 14h00 et 15h00, et **0% de pluie** entre 15h00 et 16h00 ($P_{14h} = 100$, $P_{15h} = 0$).

*   **Consultation à 14h15 ($m = 15$) :**
    La première heure de sortie contiendra 45 minutes de l'heure pluvieuse et 15 minutes de l'heure sèche.
    $$\text{Tranche 1} = \frac{(100 \times (60 - 15)) + (0 \times 15)}{60} = \frac{4500}{60} = \mathbf{75\%}$$
    *(La probabilité décroît logiquement au fur et à mesure que la fin de l'épisode pluvieux approche).*

*   **Consultation à 14h55 ($m = 5$) :**
    Il ne reste plus que 5 minutes de la tranche pluvieuse dans l'heure qui vient.
    $$\text{Tranche 1} = \frac{(100 \times 5) + (0 \times 55)}{60} = \frac{500}{60} = \mathbf{8\%}$$

Cette approche garantit une transition **continue, fluide et extrêmement fidèle à la réalité temporelle** de l'utilisateur.

---

## Préservation de l'Intégrité des Données (Zéro Contamination)

Pour éviter toute pollution ou dérive des calculs météo :
1.  Les requêtes sont faites aux API sur leurs grilles d'heures piles absolues.
2.  Le consensus (calcul de la moyenne des API actives) est effectué **en premier** sur ces heures piles synchronisées.
3.  L'interpolation mathématique n'est appliquée **qu'à la toute fin** sur les consensus calculés.

Ainsi, la comparaison des différentes sources météo reste scientifiquement rigoureuse.

---

## Fonctionnalités Clés du Widget
*   **Affichage Réactif (Glance) :** S'adaptant à la taille du widget (affiche 3 colonnes en 2x1/3x1, et passe à 4 colonnes en format 4x1 en agrandissant les textes des pourcentages à **34sp** pour occuper tout l'espace).
*   **Opacité Ajustable :** Un curseur (`SeekBar`) dans l'interface de l'application permet de régler la transparence du fond du widget en temps réel.
*   **Résilience Hors Ligne :** Indicateur discret `(Cache)` dans le pied de page uniquement si la dernière mise à jour réussie date de plus de 40 minutes.

---

## Configuration des Clés d'API

Pour compiler et faire fonctionner les requêtes réseau de l'application, vous devez vous procurer deux clés d'API et les configurer localement.

### 1. Obtenir les Clés d'API

*   **Tomorrow.io** :
    1. Créez un compte gratuit sur le [Tableau de bord Tomorrow.io](https://app.tomorrow.io/signup).
    2. Copiez votre clé d'API secrète depuis l'onglet API de votre console.
*   **OpenWeatherMap** (One Call API 4.0) :
    1. Inscrivez-vous sur le site [OpenWeatherMap Sign Up](https://home.openweathermap.org/users/sign_up).
    2. Souscrivez à l'abonnement "One Call by Call 4.0" (les 1 000 premières requêtes quotidiennes sont gratuites).
    3. Configurez un seuil de blocage quotidien de "1000" appels dans votre tableau de bord financier pour garantir la gratuité absolue et bloquer tout dépassement.
    4. Récupérez votre clé d'API générée.
*   **Open-Meteo** :
    *   Aucune clé n'est requise. L'API est libre d'accès pour les forfaits personnels, elle est interrogée directement par l'application.

### 2. Configurer le projet

À la racine du projet Android, créez ou modifiez le fichier [local.properties](file:///Y:/VaTuMouiller/local.properties) (qui est déjà inscrit dans le `.gitignore` pour protéger vos secrets) et ajoutez-y vos clés :

```properties
TOMORROW_IO_KEY=VOTRE_CLE_TOMORROW_ICI
OPEN_WEATHER_KEY=VOTRE_CLE_OPENWEATHER_ICI
```

Lors de la compilation, le script Gradle charge ces variables et les injecte de manière sécurisée sous forme de constantes statiques dans la classe `BuildConfig` pour qu'elles soient consommées par notre repository météo.

---

*Conçu par Sébastien Bédard*
