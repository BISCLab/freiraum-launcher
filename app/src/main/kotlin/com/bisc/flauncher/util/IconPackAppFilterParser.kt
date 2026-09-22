package com.bisc.flauncher.util

import android.content.Context
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** Parser for the appfilter.xml format used by icon packs. */
object IconPackAppFilterParser {
    fun parse(context: Context, iconPackPackage: String): List<Pair<String, String>> {
        val resources = runCatching {
            context.packageManager.getResourcesForApplication(iconPackPackage)
        }.getOrNull() ?: return emptyList()

        val xmlResId = resources.getIdentifier("appfilter", "xml", iconPackPackage)
        val parser: XmlPullParser = if (xmlResId != 0) {
            resources.getXml(xmlResId)
        } else {
            val assetStream = runCatching {
                context.createPackageContext(iconPackPackage, 0).assets.open("appfilter.xml")
            }.getOrNull() ?: return emptyList()
            XmlPullParserFactory.newInstance().newPullParser().apply { setInput(assetStream, null) }
        }

        val results = mutableListOf<Pair<String, String>>()
        runCatching {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val rawComponent = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (rawComponent != null && drawable != null) {
                        val component = COMPONENT_INFO_REGEX.find(rawComponent)?.groupValues?.get(1)
                            ?: rawComponent
                        results += component to drawable
                    }
                }
                eventType = parser.next()
            }
        }
        return results
    }

    private val COMPONENT_INFO_REGEX = Regex("ComponentInfo\\{([^}]+)\\}")
}
