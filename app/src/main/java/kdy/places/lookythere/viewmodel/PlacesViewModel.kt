package kdy.places.lookythere.viewmodel

import android.location.Location
import androidx.lifecycle.*
import kdy.places.lookythere.api.PlacesApi
import kdy.places.lookythere.model.Place
import kdy.places.lookythere.BuildConfig.GOOGLE_MAPS_API_KEY
import kdy.places.lookythere.database.place.PlaceData
import kdy.places.lookythere.model.Geometry
import kdy.places.lookythere.model.GeometryLocation
import kdy.places.lookythere.repository.PlacesRepository
import kotlinx.coroutines.launch

class PlacesViewModel (private val repository: PlacesRepository) : ViewModel() {
    // The internal MutableLiveData that stores the status of the most recent request
    private val _status = MutableLiveData<String>()

    // The external immutable LiveData for the request status
    val status: LiveData<String> = _status

    private val _places = MutableLiveData<List<Place>>()
    val places: LiveData<List<Place>> = _places

//    private lateinit var placeDao: PlaceDao
//
//    // Room executes all queries on a separate thread.
//    // Observed Flow will notify the observer when the data has changed.
//    val placesData: Flow<List<PlaceData>> = placeDao.getPlaces()


    /**
     * Gets places of specified type close to the location
     * [Place] [List] [LiveData].
     */
     fun getNearbyPlaces(location: Location) {
        val apiKey = GOOGLE_MAPS_API_KEY
        viewModelScope.launch {
            try {
                val results = PlacesApi.retrofitService.nearbyPlaces(
                    apiKey = apiKey,
                    location = "${location.latitude},${location.longitude}",
                    radiusInMeters = 500,
                    placeType = "bar").results
                _places.value = results
                _status.value = "   First Place : ${_places.value!![0].name}"
            } catch (e: Exception) {
                _status.value = "Failure: ${e.message}, ${e.stackTraceToString()}"
            }
        }
    }

    /**
     * Gets places at NESW from the location
     * [Place] [List] [LiveData].
     */
    fun getFixedPlaces(location: Location) {
        val northSouth = .045
        val eastWest = .055
        val north5 = Place(place_id="north5", icon="https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png", name="north5", geometry= Geometry(location= GeometryLocation(location.latitude + northSouth, location.longitude)))
        val south5 = Place(place_id="south5", icon="https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png", name="south5", geometry= Geometry(location= GeometryLocation(location.latitude - northSouth, location.longitude)))
        val east5 = Place(place_id="east5", icon="https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png", name="east5", geometry= Geometry(location= GeometryLocation(location.latitude, location.longitude + eastWest)))
        val west5 = Place(place_id="west5", icon="https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png", name="west5", geometry= Geometry(location= GeometryLocation(location.latitude, location.longitude - eastWest)))
        val ct = Place(place_id="chapel tavern", icon="https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png", name="chapel tavern", geometry= Geometry(location= GeometryLocation(56.13023958, -3.2023989)))
        val wc = Place(place_id="Woodland", icon="https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png", name="Woodland Creatures", geometry= Geometry(location= GeometryLocation(55.9661769, -3.1758515)))
        val fixedPlaces: List<Place> = listOf(wc, north5, south5, east5, west5)
        _places.value = fixedPlaces
        _status.value = "   First Place : ${_places.value!![0].name}"
    }

    /**
     * Gets places at NESW from the location
     * [Place] [List] [LiveData].
     */
    fun getCardinalPointPlaces(location: Location) {
        val northSouth = .045
        val eastWest = .055
        val north5 = Place(place_id="north5", icon="https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png", name="north5", geometry= Geometry(location= GeometryLocation(location.latitude + northSouth, location.longitude)))
        val south5 = Place(place_id="south5", icon="https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png", name="south5", geometry= Geometry(location= GeometryLocation(location.latitude - northSouth, location.longitude)))
        val east5 = Place(place_id="east5", icon="https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png", name="east5", geometry= Geometry(location= GeometryLocation(location.latitude, location.longitude + eastWest)))
        val west5 = Place(place_id="west5", icon="https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png", name="west5", geometry= Geometry(location= GeometryLocation(location.latitude, location.longitude - eastWest)))
//        val northeast5 = Place(place_id="northeast5", icon="https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png", name="northeast5", geometry= Geometry(location= GeometryLocation(lat=56.15861, lng=-3.1522225)))
//        val northwest5 = Place(place_id="northwest5", icon="https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png", name="northwest5", geometry= Geometry(location= GeometryLocation(lat=56.15861, lng=-3.266389)))
//        val home = Place(place_id="home", icon="https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/park-71.png", name="home", geometry= Geometry(location= GeometryLocation(location.latitude, location.longitude)))
//        val fixedPlaces: List<Place> = listOf(north5, south5, east5, west5, northeast5, northwest5, home)
        val fixedPlaces: List<Place> = listOf(north5, south5, east5, west5)
        _places.value = fixedPlaces
        _status.value = "   First Place : ${_places.value!![0].name}"
    }

    /**
     * Gets places at NESW from the location
     * [Place] [List] [LiveData].
     */
    fun getDBPlaces() {
        viewModelScope.launch {
            val dbPlaces: List<PlaceData> = repository.dbPlaces()
            val results: MutableList<Place> = mutableListOf<Place>()
            for (place in dbPlaces) {
                val geometry = GeometryLocation(place.latitude, place.longitude)
                val newPlace = Place(place.name, "icon", place.name, Geometry(geometry))

                results.add(newPlace)
            }

            _places.value = results
        }
    }
}

class PlacesViewModelFactory(private val repository: PlacesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlacesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlacesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}