package kdy.places.lookythere.database.place

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Provides access to read/write operations on the place table.
 * Used by the view models to format the query results for use in the UI.
 */
@Dao
interface PlaceDao {

    // The flow always holds/caches latest version of data. Notifies its observers when the
    // data has changed.
    @Query("SELECT * FROM place_table ORDER BY name ASC")
    suspend fun getPlaces(): List<PlaceData>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(place: PlaceData)

    @Query("DELETE FROM place_table")
    suspend fun deleteAll()
}
