package com.bisc.flauncher.util

import android.content.Context
import android.net.Uri
import com.bisc.flauncher.data.model.SavedMapTarget

/** Opens a saved place in the chosen maps app, or the default one, through a geo: URI. */
fun launchMapTarget(context: Context, target: SavedMapTarget) {
    val uri = if (target.address.isNotBlank()) {
        Uri.parse("geo:0,0?q=${Uri.encode(target.address)}")
    } else {
        Uri.parse("geo:${target.lat},${target.lon}?q=${target.lat},${target.lon}(${Uri.encode(target.label)})")
    }
    startView(context, uri, ExternalApps.maps)
}

/** Free-text place/address search — the maps-search command result. */
fun launchMapsQuery(context: Context, query: String): Boolean =
    startView(context, Uri.parse("geo:0,0?q=${Uri.encode(query.trim())}"), ExternalApps.maps)
