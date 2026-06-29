package com.sebed.vatumouiller.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sebed.vatumouiller.VaTuMouillerApplication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(context)
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        val size = LocalSize.current
        val width = size.width
        val height = size.height
        val isWide = width >= 250.dp
        val labelSize = if (isWide) 12.sp else 10.sp
        val valueSize = if (isWide) 34.sp else 16.sp
        val colSpacing = if (isWide) 24.dp else 12.dp
        val paddingVal = 4.dp
        val spacerHeight = if (isWide) (if (height >= 70.dp) 8.dp else 2.dp) else 2.dp

        val app = context.applicationContext as? VaTuMouillerApplication
        val container = app?.container
        val repo = container?.weatherRepository

        val opacity = repo?.getWidgetOpacity() ?: 0.90f
        val backgroundColor = ColorProvider(Color(0xFF121212).copy(alpha = opacity)) // Dark background with adjustable opacity
        val primaryTextColor = ColorProvider(Color(0xFFFFFFFF))

        // Exécuter la logique d'initialisation (non-composable) dans un bloc try-catch standard
        var cachedForecast: com.sebed.vatumouiller.data.repository.WeatherForecast? = null
        var errorOccurred: Throwable? = null

        try {
            if (repo == null) throw IllegalStateException("Repository non initialisé")
            cachedForecast = repo.getCachedForecast()
        } catch (e: Throwable) {
            errorOccurred = e
        }

        // Appeler les fonctions composables en fonction du résultat de l'initialisation
        if (errorOccurred != null) {
            // Afficher l'erreur en rouge directement dans le widget pour un diagnostic facile
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Erreur de chargement",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFF3B30)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = errorOccurred.localizedMessage ?: errorOccurred.toString(),
                        style = TextStyle(
                            color = primaryTextColor,
                            fontSize = 8.sp
                        )
                    )
                }
            }
        } else {
            val secondaryTextColor = ColorProvider(Color(0xFF8E8E93))
            val accentColor = ColorProvider(Color(0xFF0A84FF)) // Vibrant blue for precipitation probability

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(paddingVal)
            ) {
                if (cachedForecast != null) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HourForecastItem("H+1", cachedForecast.h1, primaryTextColor, accentColor, labelSize, valueSize)
                            Spacer(modifier = GlanceModifier.width(colSpacing))
                            HourForecastItem("H+2", cachedForecast.h2, primaryTextColor, accentColor, labelSize, valueSize)
                            Spacer(modifier = GlanceModifier.width(colSpacing))
                            HourForecastItem("H+3", cachedForecast.h3, primaryTextColor, accentColor, labelSize, valueSize)

                            if (width >= 220.dp) {
                                Spacer(modifier = GlanceModifier.width(colSpacing))
                                HourForecastItem("H+4", cachedForecast.h4, primaryTextColor, accentColor, labelSize, valueSize)
                            }
                        }

                        if (height >= 38.dp) {
                            Spacer(modifier = GlanceModifier.height(1.dp))
                            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val timeStr = sdf.format(Date(cachedForecast.timestamp))
                            val cacheAgeMs = System.currentTimeMillis() - cachedForecast.timestamp
                            val isStale = cacheAgeMs > 40 * 60 * 1000 // Plus de 40 minutes
                            val footerText = if (isStale) "Mis à jour à $timeStr (Cache)" else "Mis à jour à $timeStr"

                            Text(
                                text = footerText,
                                style = TextStyle(
                                    color = secondaryTextColor,
                                    fontSize = if (isWide) 8.sp else 7.sp
                                )
                            )
                        }
                    }
                } else {
                    // Message d'invite si aucune donnée n'est présente
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pas de données",
                            style = TextStyle(color = primaryTextColor, fontSize = if (isWide) 14.sp else 12.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "Ouvrez l'application",
                            style = TextStyle(color = secondaryTextColor, fontSize = if (isWide) 11.sp else 9.sp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun HourForecastItem(
        label: String,
        probability: Int,
        textColor: ColorProvider,
        accentColor: ColorProvider,
        labelSize: TextUnit,
        valueSize: TextUnit
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = TextStyle(color = textColor, fontSize = labelSize)
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "$probability%",
                style = TextStyle(
                    color = if (probability > 30) accentColor else textColor,
                    fontSize = valueSize
                )
            )
        }
    }
}
