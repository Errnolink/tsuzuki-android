package dev.errnolink.tsuzuki.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.errnolink.tsuzuki.designsystem.GroupedRow
import dev.errnolink.tsuzuki.designsystem.InsetGroupedList
import dev.errnolink.tsuzuki.ui.shell.ShellList
import eu.kanade.domain.extension.interactor.GetExtensionLanguages.Companion.getLanguageIconID
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.xmlpull.v1.XmlPullParser
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

class TsuzukiAppLanguageScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val langs = remember { getLangs(context) }
        var currentLanguage by remember {
            mutableStateOf(AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag() ?: "")
        }

        LaunchedEffect(currentLanguage) {
            val locale = if (currentLanguage.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(currentLanguage)
            }
            AppCompatDelegate.setApplicationLocales(locale)
        }

        ShellList(
            title = stringResource(MR.strings.pref_app_language),
            navigateUp = navigator::pop,
            itemSpacing = 4.dp,
        ) {
            items(langs, key = { it.langTag }) { language ->
                InsetGroupedList {
                    GroupedRow(
                        title = language.displayName,
                        subtitle = language.localizedDisplayName,
                        value = if (currentLanguage == language.langTag) "Selected" else null,
                        onClick = { currentLanguage = language.langTag },
                        divider = false,
                        leading = {
                            val icon = getLanguageIconID(language.langTag) ?: R.drawable.globe
                            Icon(painterResource(icon), language.langTag, Modifier.width(32.dp).height(24.dp), tint = Color.Unspecified)
                        },
                    )
                }
            }
        }
    }

    private fun getLangs(context: Context): ImmutableList<Language> {
        val langs = mutableListOf<Language>()
        val parser = context.resources.getXml(R.xml.locales_config)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                for (i in 0..<parser.attributeCount) {
                    if (parser.getAttributeName(i) == "name") {
                        val langTag = parser.getAttributeValue(i)
                        val displayName = LocaleHelper.getLocalizedDisplayName(langTag)
                        if (displayName.isNotEmpty()) {
                            langs.add(Language(langTag, displayName, LocaleHelper.getDisplayName(langTag)))
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        langs.sortBy { it.displayName }
        langs.add(0, Language("", context.stringResource(MR.strings.label_default), null))

        return langs.toImmutableList()
    }

    private data class Language(
        val langTag: String,
        val displayName: String,
        val localizedDisplayName: String?,
    )
}
