package by.vsdev.cpt.core.database

/**
 * Constructing a [RoomDatabase.Builder] needs a platform [android.content.Context] on Android but
 * nothing extra elsewhere, which doesn't fit a single expect/actual function signature — so each
 * platform gets its own concrete implementation instead (wired per-platform via Koin), same
 * pattern as :core:secrets' SecretStore.
 */
interface DatabaseProvider {
    fun database(): AppDatabase
}
