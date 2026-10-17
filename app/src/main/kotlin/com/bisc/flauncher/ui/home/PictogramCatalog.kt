package com.bisc.flauncher.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Placeholder pictogram set for Elements (Addresses/Links/Powerbuttons) that need a user-pickable
 * icon independent of any app icon. Keys are stable identifiers stored in the database — the
 * Material Icons behind them are a stand-in until the user supplies a real flat/transparent SVG set
 * (see masterplan Phase 6), at which point each key's [ImageVector] is swapped for a VectorDrawable
 * without touching any stored data.
 */
object PictogramCatalog {
    val icons: Map<String, ImageVector> = linkedMapOf(
        "home" to Icons.Filled.Home,
        "work" to Icons.Filled.Work,
        "favorite" to Icons.Filled.Favorite,
        "star" to Icons.Filled.Star,
        "place" to Icons.Filled.Place,
        "language" to Icons.Filled.Language,
        "search" to Icons.Filled.Search,
        "apps" to Icons.Filled.Apps,
        "folder" to Icons.Filled.Folder,
        "person" to Icons.Filled.Person,
        "phone" to Icons.Filled.Phone,
        "mail" to Icons.Filled.Mail,
        "shopping" to Icons.Filled.ShoppingCart,
        "hospital" to Icons.Filled.LocalHospital,
        "school" to Icons.Filled.School,
        "restaurant" to Icons.Filled.Restaurant,
        "car" to Icons.Filled.DirectionsCar,
        "flight" to Icons.Filled.Flight,
        "hotel" to Icons.Filled.Hotel,
        "money" to Icons.Filled.AttachMoney
    )

    fun iconFor(key: String?): ImageVector? = key?.let { icons[it] }
}
