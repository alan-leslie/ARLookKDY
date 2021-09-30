package kdy.places.lookythere.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

private const val BASE_URL = "https://overpass-api.de/api"


/**
 * Build the Moshi object with Kotlin adapter factory that Retrofit will be using.
 */
private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

/**
 * The Retrofit object with the Moshi converter.
 */
private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

/**
 * Interface definition for a service that interacts with the Places API.
 *
 * @see [Place Search](https://developers.google.com/places/web-service/search)
 */
//https://overpass-api.de/api/interpreter?data=[out:json];node[natural=peak](56.126732,%20-3.3643,%2056.2455,%20-3.209272);out;
interface PeaksApiService {
    @GET("nearbysearch/json")
    suspend fun nearbyPlaces(
        @retrofit2.http.Query("key") apiKey: String,
        @retrofit2.http.Query("location") location: String,
        @retrofit2.http.Query("radius") radiusInMeters: Int,
        @retrofit2.http.Query("type") placeType: String
    ): NearbyPeaksResponse
}

object PeaksApi {
    val retrofitService: PeaksApiService by lazy {
        retrofit.create(PeaksApiService::class.java)
    }
}