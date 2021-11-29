package com.example.map_navigation_demo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMap.OnMapClickListener
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.Style.OnStyleLoaded
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnMapClickListener,
    PermissionsListener {
    var mapView: MapView? = null
    var mapboxMap: MapboxMap? = null
    var locationComponent: LocationComponent? = null
    var permissionsManager: PermissionsManager? = null
    var currentRoute: DirectionsRoute? = null
    var navigationMapRoute: NavigationMapRoute? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(
            this,
            "pk.eyJ1IjoiZm9ybWljcyIsImEiOiJja3dhZHN1NHc4NTljMnVtdGoycGNhaGU0In0.AdObMlYwx_adlVGF7lrREw"
        )
        setContentView(R.layout.activity_main)
        mapView = findViewById<View>(R.id.mapView) as MapView
        mapView!!.onCreate(savedInstanceState)
        mapView!!.getMapAsync(this)
    }
//
//    private fun initSearchFab() {
//        fab_location_search?.setOnClickListener { v: View? ->
//            val intent = PlaceAutocomplete.IntentBuilder()
//                .accessToken(
//                    (if (Mapbox.getAccessToken() != null) Mapbox.getAccessToken() else getString(
//                        R.string.mapbox_access_token
//                    ))!!
//                )
//                .placeOptions(
//                    PlaceOptions.builder()
//                        .backgroundColor(Color.parseColor("#EEEEEE"))
//                        .limit(10) //.addInjectedFeature(home)
//                        //.addInjectedFeature(work)
//                        .build(PlaceOptions.MODE_CARDS)
//                )
//                .build(this@MainActivity)
//            startActivityForResult(
//                intent, REQUEST_CODE_AUTOCOMPLETE
//            )
//        }
//    }

    fun startNavigationBtnClick(v: View?) {

        if (currentRoute == null) {
            return; // Route has not been set, so we ignore the button press
        }

        val simulateRoute = true
        val options = NavigationLauncherOptions.builder()
            .directionsRoute(currentRoute)
            .shouldSimulateRoute(simulateRoute)
            .build()
        NavigationLauncher.startNavigation(this@MainActivity, options)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {}
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap!!.style!!)
        } else {
            Toast.makeText(applicationContext, "Permission not granted", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        val destinationPoint = Point.fromLngLat(point.longitude, point.latitude)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED
        )


        {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        // this get current location
        val originPoint = Point.fromLngLat(
            locationComponent!!.lastKnownLocation!!.longitude,
            locationComponent!!.lastKnownLocation!!.latitude
        )
        val source = mapboxMap!!.style!!.getSourceAs<GeoJsonSource>("destination-source-id")
        source?.setGeoJson(Feature.fromGeometry(destinationPoint))
        getRoute(originPoint, destinationPoint)
        return true
    }

    private fun getRoute(originPoint: Point, destinationPoint: Point) {
        NavigationRoute.builder(this)
            .accessToken("pk.eyJ1IjoiZm9ybWljcyIsImEiOiJja3dhZHN1NHc4NTljMnVtdGoycGNhaGU0In0.AdObMlYwx_adlVGF7lrREw")
            .origin(originPoint)
            .destination(destinationPoint)
            .build()
            .getRoute(object : Callback<DirectionsResponse?> {
                override fun onResponse(
                    call: Call<DirectionsResponse?>,
                    response: Response<DirectionsResponse?>
                ) {
                    if (response.body() != null && response.body()!!.routes().size >= 1) {
                        currentRoute = response.body()!!.routes()[0]
                        if (navigationMapRoute != null) {
                            navigationMapRoute!!.removeRoute()
                        } else {
                            navigationMapRoute = mapView?.let {
                                mapboxMap?.let { it1 ->
                                    NavigationMapRoute(
                                        null,
                                        it,
                                        it1,
                                        R.style.NavigationMapRoute
                                    )
                                }
                            }
                        }
                        navigationMapRoute!!.addRoute(currentRoute)
                    }
                }

                override fun onFailure(call: Call<DirectionsResponse?>, t: Throwable) {
                    Toast.makeText(this@MainActivity,t.message,Toast.LENGTH_LONG).show()
                }
            })
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        this.mapboxMap!!.setMinZoomPreference(12.0)
        mapboxMap.setStyle(
            getString(R.string.navigation_guidance_day)
        ) { style ->
            enableLocationComponent(style)
            addDestinationIconLayer(style)
            mapboxMap.addOnMapClickListener(this@MainActivity)
        }
    }

    private fun addDestinationIconLayer(style: Style) {
        style.addImage(
            "destination-icon-id",
            BitmapFactory.decodeResource(this.resources, R.drawable.mapbox_marker_icon_default)
        )
        val geoJsonSource = GeoJsonSource("destination-source-id")
        style.addSource(geoJsonSource)
        val destinationSymbolLayer =
            SymbolLayer("destination-symbol-layer-id", "destination-source-id")
        destinationSymbolLayer.withProperties(
            PropertyFactory.iconImage("destination-icon-id"),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true)
        )
        style.addLayer(destinationSymbolLayer)
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationComponent = mapboxMap!!.locationComponent
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            locationComponent!!.activateLocationComponent(this, loadedMapStyle)
            locationComponent!!.isLocationComponentEnabled = true
            locationComponent!!.cameraMode = CameraMode.TRACKING
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView!!.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        mapView!!.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }
}