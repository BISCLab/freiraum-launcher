package com.bisc.flauncher.util

import android.content.Context
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Parser for an icon pack's `xml/drawable.xml`: the full list of icons it ships for manual picking,
 * including ones `appfilter.xml` never maps to an app (e.g. themed folder icons).
 */
object IconPackDrawableXmlParser {
    fun parse(context: Context, iconPackPackage: String): List<String> {
        val resources = runCatching {
            context.packageManager.getResourcesForApplication(iconPackPackage)
        }.getOrNull() ?: return emptyList()

        val xmlResId = resources.getIdentifier("drawable", "xml", iconPackPackage)
        val parser: XmlPullParser = if (xmlResId != 0) {
            resources.getXml(xmlResId)
        } else {
            val assetStream = runCatching {
                context.createPackageContext(iconPackPackage, 0).assets.open("drawable.xml")
            }.getOrNull() ?: return emptyList()
            XmlPullParserFactory.newInstance().newPullParser().apply { setInput(assetStream, null) }
        }

        val results = mutableListOf<String>()
        runCatching {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    parser.getAttributeValue(null, "drawable")?.let { results += it }
                }
                eventType = parser.next()
            }
        }
        return results
    }
}
