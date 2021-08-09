package kdy.places.lookythere.database.place

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

/**
 * Represents a single table in the database. Each row is a separate instance of the Schedule class.
 * Each property corresponds to a column. Additionally, an ID is needed as a unique identifier for
 * each row in the database.
 */

@Entity(tableName = "place_table")
data class PlaceData(
    @PrimaryKey @NonNull @ColumnInfo(name = "name") val name: String,
    @NonNull @ColumnInfo(name = "latitude") val latitude: Double,
    @NonNull @ColumnInfo(name = "longitude") val longitude: Double,
    @NonNull @ColumnInfo(name = "address") val address: String,
    @NonNull @ColumnInfo(name = "rating") val rating: Float
) {
    val latLng: LatLng
        get() = LatLng(latitude, longitude)
}
