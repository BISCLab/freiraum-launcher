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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Pictogram set for Elements (Addresses/Links/Action buttons) that need a pickable icon independent
 * of any app icon. Keys are stable identifiers stored in the database, so the [ImageVector] behind
 * a key can be swapped for another drawing without touching any stored data.
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
        "money" to Icons.Filled.AttachMoney,
        "label" to Icons.AutoMirrored.Filled.Label,
        "game" to Icons.Filled.SportsEsports,
        "music" to Icons.Filled.MusicNote,
        "video" to Icons.Filled.Movie,
        "photo" to Icons.Filled.PhotoCamera,
        "social" to Icons.Filled.Group,
        "news" to Icons.Filled.Newspaper,
        "map" to Icons.Filled.Map,
        "accessibility" to Icons.Filled.Accessibility,
        "book" to Icons.Filled.MenuBook,
        "fitness" to Icons.Filled.FitnessCenter,
        "pets" to Icons.Filled.Pets,
        "tools" to Icons.Filled.Build,
        "new" to Icons.Filled.NewReleases,
        "sleep" to Icons.Filled.Bedtime,
        "trending" to Icons.AutoMirrored.Filled.TrendingUp,
        "inbox" to Icons.Filled.Inbox
    )

    fun iconFor(key: String?): ImageVector? = key?.let { icons[it] }
}
