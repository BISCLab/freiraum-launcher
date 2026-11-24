package com.bisc.flauncher.util

import androidx.core.net.toUri
import android.content.Context
import android.net.Uri
import com.bisc.flauncher.data.model.SavedMapTarget

/** Opens a saved place in the chosen maps app, or the default one, through a geo: URI. */
fun launchMapTarget(context: Context, target: SavedMapTarget) {
    val uri = if (target.address.isNotBlank()) {
        "geo:0,0?q=${Uri.encode(target.address)}".toUri()
    } else {
        "geo:${target.lat},${target.lon}?q=${target.lat},${target.lon}(${Uri.encode(target.label)})".toUri()
    }
    startView(context, uri, target.navigationApp ?: ExternalApps.maps)
}

/** Free-text place/address search - the maps-search command result. */
fun launchMapsQuery(context: Context, query: String): Boolean =
    startView(context, "geo:0,0?q=${Uri.encode(query.trim())}".toUri(), ExternalApps.maps)
