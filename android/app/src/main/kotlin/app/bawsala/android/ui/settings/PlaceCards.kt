package app.bawsala.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.bawsala.android.R
import app.bawsala.android.domain.PlaceFix
import app.bawsala.android.location.rememberPlaceRequest
import app.bawsala.android.ui.design.ActionButton
import app.bawsala.android.ui.design.ActionStyle
import app.bawsala.android.ui.design.LucideIcon
import app.bawsala.android.ui.format.methodName
import app.bawsala.android.ui.i18n.LocalLang
import app.bawsala.android.ui.i18n.tr
import app.bawsala.android.ui.theme.Palette
import app.bawsala.android.ui.theme.TypeScale
import app.bawsala.android.viewmodel.AppViewModel
import com.batoulapps.adhan2.CalculationMethod

/** "Use my location": approximate location, read once, after asking for permission. */
@Composable
fun UseMyLocation(vm: AppViewModel, modifier: Modifier = Modifier) {
    val request = rememberPlaceRequest { fix ->
        if (fix != null) vm.settings.usePlace(fix) else vm.messages.show("locationUnavailable")
    }
    ActionButton(
        tr("useMyLocation"),
        request,
        modifier.fillMaxWidth(),
        ActionStyle.Secondary,
        R.drawable.ic_lucide_locate_fixed,
    )
}

/** After a location change: the usual method there, applied only if the user accepts. */
@Composable
fun MethodSuggestionCard(vm: AppViewModel, method: CalculationMethod) {
    PlaceCard(
        R.drawable.ic_lucide_circle_help,
        tr("methodSuggestion", methodName(method, LocalLang.current)),
        tr("useIt") to vm.settings::acceptMethod,
        tr("keepMine") to vm.settings::dismissMethod,
    )
}

/** Today, when the phone is far from the saved place: update prayer times there? */
@Composable
fun TravelCard(vm: AppViewModel, fix: PlaceFix) {
    PlaceCard(
        R.drawable.ic_lucide_map_pin,
        fix.label?.let { tr("travelPrompt", it) } ?: tr("travelPromptUnnamed"),
        tr("updatePrayerTimes") to { vm.settings.usePlace(fix) },
        tr("notNow") to vm.settings::dismissTravel,
    )
}

@Composable
private fun PlaceCard(
    icon: Int,
    text: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>,
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.amberTint)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LucideIcon(icon, tint = Palette.amber, size = 18.dp)
            Text(
                text,
                style = TypeScale.small.copy(color = Palette.ink),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(primary.first, primary.second, Modifier.weight(1f))
            ActionButton(secondary.first, secondary.second, Modifier.weight(1f), ActionStyle.Ghost)
        }
    }
}
