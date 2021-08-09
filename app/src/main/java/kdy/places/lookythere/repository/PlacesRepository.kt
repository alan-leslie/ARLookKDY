package kdy.places.lookythere.repository

import android.content.Context
import androidx.annotation.WorkerThread
import kdy.places.lookythere.database.place.PlaceDao
import kdy.places.lookythere.database.place.PlaceData
import kotlinx.coroutines.flow.Flow

/**
 * Abstracted Repository as promoted by the Architecture Guide.
 * https://developer.android.com/topic/libraries/architecture/guide.html
 */
class PlacesRepository(private val placeDao: PlaceDao) {
    private val _TAG = "PlaceRepository"

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    suspend fun dbPlaces(): List<PlaceData>
    {
        return placeDao.getPlaces()
    }

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(place: PlaceData) {
        placeDao.insert(place)
    }
}