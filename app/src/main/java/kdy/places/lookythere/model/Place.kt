//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package kdy.places.lookythere.model

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.ar.sceneform.math.Vector3
import com.google.maps.android.data.Geometry
import com.google.maps.android.ktx.utils.sphericalHeading
import com.google.maps.android.ktx.utils.sphericalPathLength
import kotlin.math.cos
import kotlin.math.sin

/**
 * A model describing details about a Place (location, name, type, etc.).
 */
data class Place(
    val place_id: String,
    val icon: String,
    val name: String,
    val geometry: kdy.places.lookythere.model.Geometry
) {
    val _TAG = "Place"

    override fun equals(other: Any?): Boolean {
        if (other !is Place) {
            return false
        }

        return this.place_id == other.place_id
    }

    override fun hashCode(): Int {
        return this.place_id.hashCode()
    }
}

fun Place.getPathLength(latLng: LatLng): Float {
    val placeLatLng = this.geometry.location.latLng
    val path: List<LatLng> = listOf(latLng, placeLatLng)
    return path.sphericalPathLength().toFloat()
}

fun Place.getPositionVector(azimuth: Float, latLng: LatLng): Vector3 {
    val placeLatLng = this.geometry.location.latLng
    val heading = latLng.sphericalHeading(placeLatLng)
    val heading2rad =  Math.toRadians(heading)
    val pathLength = getPathLength(latLng)
    val relativeAngleRad = (azimuth * -1.0).toDouble() + heading2rad
    val relativeAngleDeg = Math.toDegrees(relativeAngleRad)
    val radius = calcRadius(pathLength)
    val x = radius * sin((azimuth * -1.0).toDouble() + heading2rad).toFloat()
    val y = 0.1f
    val z = -radius * cos((azimuth * -1.0).toDouble() + heading2rad).toFloat()
    return Vector3(x, y, z)
}

private fun calcRadius(pathLength: Float): Float
{
    if(pathLength <= 100.0f)
    {
        return 4.0f
    }
    else
    {
        if(pathLength <= 500.0f)
        {
            val diff = pathLength  - 100.0f
            return 4.0f + diff / 200.0f
        }
        else
        {
            if (pathLength < 4500)
            {
                val diff = pathLength - 500.0f
                return 6.0f + diff/ 1000.0f
            }
            else
            {
                return 10.0f
            }
        }
    }

    return 10.0f
}

data class Geometry(
    val location: GeometryLocation
)

data class GeometryLocation(
    val lat: Double,
    val lng: Double
) {
    val latLng: LatLng
        get() = LatLng(lat, lng)
}
