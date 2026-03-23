package com.expensetracker.presentation.ui.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps the string icon key stored in the database to a Material Icons ImageVector.
 * All default categories use keys from this map.
 * User-created categories fall back to [defaultIcon] if the key isn't found.
 */
object CategoryIcons {

    val defaultIcon: ImageVector = Icons.Default.Category

    private val map: Map<String, ImageVector> = mapOf(
        // ── Food & Drink ──────────────────────────────────────────────────────
        "restaurant"          to Icons.Default.Restaurant,
        "local_cafe"          to Icons.Default.LocalCafe,
        "local_bar"           to Icons.Default.LocalBar,
        "fastfood"            to Icons.Default.Fastfood,
        "cake"                to Icons.Default.Cake,
        "liquor"              to Icons.Default.Liquor,
        "lunch_dining"        to Icons.Default.LunchDining,
        "dinner_dining"       to Icons.Default.DinnerDining,
        "breakfast_dining"    to Icons.Default.BreakfastDining,
        "coffee"              to Icons.Default.Coffee,

        // ── Groceries & Shopping ─────────────────────────────────────────────
        "shopping_cart"       to Icons.Default.ShoppingCart,
        "shopping_bag"        to Icons.Default.ShoppingBag,
        "local_grocery_store" to Icons.Default.LocalGroceryStore,
        "storefront"          to Icons.Default.Storefront,
        "store"               to Icons.Default.Store,
        "redeem"              to Icons.Default.Redeem,
        "loyalty"             to Icons.Default.Loyalty,

        // ── Home & Utilities ─────────────────────────────────────────────────
        "home"                to Icons.Default.Home,
        "house"               to Icons.Default.House,
        "apartment"           to Icons.Default.Apartment,
        "bolt"                to Icons.Default.Bolt,
        "water_drop"          to Icons.Default.WaterDrop,
        "wifi"                to Icons.Default.Wifi,
        "phone"               to Icons.Default.Phone,
        "build"               to Icons.Default.Build,
        "cleaning_services"   to Icons.Default.CleaningServices,

        // ── Transport ────────────────────────────────────────────────────────
        "directions_car"      to Icons.Default.DirectionsCar,
        "directions_bus"      to Icons.Default.DirectionsBus,
        "train"               to Icons.Default.Train,
        "two_wheeler"         to Icons.Default.TwoWheeler,
        "local_taxi"          to Icons.Default.LocalTaxi,
        "electric_car"        to Icons.Default.ElectricCar,
        "local_gas_station"   to Icons.Default.LocalGasStation,
        "directions_walk"     to Icons.AutoMirrored.Filled.DirectionsWalk,

        // ── Travel ───────────────────────────────────────────────────────────
        "flight"              to Icons.Default.Flight,
        "hotel"               to Icons.Default.Hotel,
        "beach_access"        to Icons.Default.BeachAccess,
        "map"                 to Icons.Default.Map,
        "luggage"             to Icons.Default.Luggage,
        "card_travel"         to Icons.Default.CardTravel,

        // ── Health & Fitness ─────────────────────────────────────────────────
        "local_hospital"      to Icons.Default.LocalHospital,
        "medical_services"    to Icons.Default.MedicalServices,
        "medication"          to Icons.Default.Medication,
        "fitness_center"      to Icons.Default.FitnessCenter,
        "spa"                 to Icons.Default.Spa,
        "self_improvement"    to Icons.Default.SelfImprovement,
        "monitor_heart"       to Icons.Default.MonitorHeart,

        // ── Education ────────────────────────────────────────────────────────
        "school"              to Icons.Default.School,
        "menu_book"           to Icons.AutoMirrored.Filled.MenuBook,
        "auto_stories"        to Icons.Default.AutoStories,
        "science"             to Icons.Default.Science,
        "calculate"           to Icons.Default.Calculate,
        "computer"            to Icons.Default.Computer,

        // ── Entertainment ────────────────────────────────────────────────────
        "movie"               to Icons.Default.Movie,
        "music_note"          to Icons.Default.MusicNote,
        "sports_esports"      to Icons.Default.SportsEsports,
        "sports_cricket"      to Icons.Default.SportsCricket,
        "sports_soccer"       to Icons.Default.SportsSoccer,
        "theater_comedy"      to Icons.Default.TheaterComedy,
        "headphones"          to Icons.Default.Headphones,
        "tv"                  to Icons.Default.Tv,
        "live_tv"             to Icons.Default.LiveTv,

        // ── Personal Care & Lifestyle ─────────────────────────────────────────
        "face"                to Icons.Default.Face,
        "face_retouching_natural" to Icons.Default.FaceRetouchingNatural,
        "checkroom"           to Icons.Default.Checkroom,
        "dry_cleaning"        to Icons.Default.DryCleaning,
        "child_care"          to Icons.Default.ChildCare,
        "pets"                to Icons.Default.Pets,

        // ── Finance & Work ───────────────────────────────────────────────────
        "work"                to Icons.Default.Work,
        "business_center"     to Icons.Default.BusinessCenter,
        "account_balance"     to Icons.Default.AccountBalance,
        "trending_up"         to Icons.AutoMirrored.Filled.TrendingUp,
        "trending_down"       to Icons.AutoMirrored.Filled.TrendingDown,
        "savings"             to Icons.Default.Savings,
        "paid"                to Icons.Default.Paid,
        "receipt"             to Icons.Default.Receipt,
        "attach_money"        to Icons.Default.AttachMoney,
        "currency_rupee"      to Icons.Default.CurrencyRupee,
        "currency_bitcoin"    to Icons.Default.CurrencyBitcoin,
        "real_estate_agent"   to Icons.Default.RealEstateAgent,
        "laptop"              to Icons.Default.Laptop,
        "star"                to Icons.Default.Star,

        // ── Insurance & Protection ───────────────────────────────────────────
        "shield"              to Icons.Default.Shield,
        "security"            to Icons.Default.Security,
        "verified_user"       to Icons.Default.VerifiedUser,
        "health_and_safety"   to Icons.Default.HealthAndSafety,

        // ── Subscriptions ────────────────────────────────────────────────────
        "subscriptions"       to Icons.Default.Subscriptions,
        "stream"              to Icons.Default.Stream,
        "cloud"               to Icons.Default.Cloud,

        // ── Transfer & Misc ───────────────────────────────────────────────────
        "swap_horiz"          to Icons.Default.SwapHoriz,
        "sync_alt"            to Icons.Default.SyncAlt,
        "more_horiz"          to Icons.Default.MoreHoriz,
        "category"            to Icons.Default.Category,
        "label"               to Icons.AutoMirrored.Filled.Label,
        "tag"                 to Icons.Default.Tag,
    )

    /** Returns the [ImageVector] for the given key, or [defaultIcon] if not found. */
    fun get(key: String): ImageVector = map[key] ?: defaultIcon

    /** All available entries for the icon picker, grouped by section label. */
    val sections: List<Pair<String, List<Pair<String, ImageVector>>>> = listOf(
        "Food & Drink" to listOf(
            "restaurant", "local_cafe", "local_bar", "fastfood",
            "lunch_dining", "dinner_dining", "breakfast_dining", "coffee", "cake", "liquor"
        ),
        "Groceries & Shopping" to listOf(
            "shopping_cart", "shopping_bag", "local_grocery_store",
            "storefront", "store", "redeem", "loyalty"
        ),
        "Home & Utilities" to listOf(
            "home", "house", "apartment", "bolt", "water_drop",
            "wifi", "phone", "build", "cleaning_services"
        ),
        "Transport" to listOf(
            "directions_car", "directions_bus", "train", "two_wheeler",
            "local_taxi", "electric_car", "local_gas_station", "directions_walk"
        ),
        "Travel" to listOf(
            "flight", "hotel", "beach_access", "map", "luggage", "card_travel"
        ),
        "Health & Fitness" to listOf(
            "local_hospital", "medical_services", "medication",
            "fitness_center", "spa", "self_improvement", "monitor_heart"
        ),
        "Education" to listOf(
            "school", "menu_book", "auto_stories", "science", "calculate", "computer"
        ),
        "Entertainment" to listOf(
            "movie", "music_note", "sports_esports", "sports_cricket",
            "sports_soccer", "theater_comedy", "headphones", "tv", "live_tv"
        ),
        "Personal Care" to listOf(
            "face", "face_retouching_natural", "checkroom", "dry_cleaning", "child_care", "pets"
        ),
        "Finance & Work" to listOf(
            "work", "business_center", "account_balance", "trending_up",
            "savings", "paid", "attach_money", "currency_rupee",
            "currency_bitcoin", "laptop", "star", "real_estate_agent"
        ),
        "Insurance" to listOf(
            "shield", "security", "verified_user", "health_and_safety"
        ),
        "Subscriptions" to listOf(
            "subscriptions", "stream", "cloud"
        ),
        "Other" to listOf(
            "swap_horiz", "sync_alt", "more_horiz", "category", "label", "tag"
        ),
    ).map { (label, keys) ->
        label to keys.map { key -> key to get(key) }
    }
}