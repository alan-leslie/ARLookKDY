package kdy.places.lookythere.api

import kdy.places.lookythere.model.Peak
import com.squareup.moshi.Json

/**
 * Data class encapsulating a response from the nearby search call to the Places API.
 */
data class NearbyPeaksResponse(
    @Json(name = "elements") val results: List<Peak>
)