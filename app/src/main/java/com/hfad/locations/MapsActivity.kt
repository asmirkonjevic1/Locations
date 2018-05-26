package com.hfad.locations

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var KEY : Int = 0
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var mCurrentLatLng: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        KEY = intent.getIntExtra("key", 0)
        if (KEY != -100){
            mCurrentLatLng = LatLng(intent.getStringExtra("latitude").toDouble(), intent.getStringExtra("longitude").toDouble())
        }

    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener{
            override fun onLocationChanged(location: Location?) {
                mMap.clear()
                val mCurrentLocation = LatLng(location!!.latitude, location.longitude)
                mMap.isMyLocationEnabled = true
                //mMap.addMarker(MarkerOptions().position(mCurrentLocation).title("Your Location"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLocation, 10f))
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }

            override fun onProviderEnabled(provider: String?) {
            }

            override fun onProviderDisabled(provider: String?) {
            }
        }

        if (KEY == -100){
            if (MainActivity.mLocationPermissionsGranted){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1000f, locationListener)
            }

        }else {
            mMap.isMyLocationEnabled = true
            mMap.addMarker(MarkerOptions().position(mCurrentLatLng).title(MainActivity.locationsStrings[KEY]))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLatLng, 10f))
        }
    }
}
