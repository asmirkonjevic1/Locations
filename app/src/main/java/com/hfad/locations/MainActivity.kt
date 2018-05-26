package com.hfad.locations

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList
import android.location.LocationManager
import android.provider.Settings


class MainActivity : AppCompatActivity(), AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {

    companion object {
        private lateinit var mAdapter: ArrayAdapter<String>
        private lateinit var mLocationListener: LocationListener
        private lateinit var mLocationManager: LocationManager
        private lateinit var currentLocation: Location
        private lateinit var sharedPreferences: SharedPreferences

        private const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        private const val COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1234

        var locationsStrings: ArrayList<String> = ArrayList()
        var latitudes: ArrayList<String> = ArrayList()
        var longitudes: ArrayList<String> = ArrayList()
        var mLocationPermissionsGranted = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        mLocationListener = object : LocationListener {
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }

            override fun onProviderEnabled(provider: String?) {
            }

            override fun onProviderDisabled(provider: String?) {
            }

            override fun onLocationChanged(location: Location?) {
                currentLocation = location!!
            }

        }
    }

    override fun onResume() {
        super.onResume()
        this.isGpsAndNetworkEnabled()
    }

    private fun isGpsAndNetworkEnabled() {
        var off = 0

        try {
            off = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
        } catch (e : Exception) {
            e.printStackTrace()
        }
        if (off == 0) {
            val dialog = AlertDialog.Builder(this)
            dialog.setMessage(this.resources.getString(R.string.gps_network_not_enabled))
            dialog.setPositiveButton(this.resources.getString(R.string.open_location_settings), { _, _ ->
                val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(myIntent)
                //get gps
            })
            dialog.setNegativeButton(getString(R.string.Cancel), { _, _ ->

            })
            dialog.show()
        } else {
            getLocationPermission()
            init()
        }
    }

    private fun getLocationPermission() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (ContextCompat.checkSelfPermission(applicationContext, FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(applicationContext, COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    @SuppressLint("MissingPermission")
    private fun init() {
        sharedPreferences = this.getSharedPreferences("com.hfad.locationsStrings", Context.MODE_PRIVATE)
        try {
            locationsStrings.clear()
            latitudes.clear()
            longitudes.clear()
            locationsStrings = ObjectSerializer.deserialize(sharedPreferences.getString("locationsStrings", ObjectSerializer.serialize(object : ArrayList<String>() {}))) as ArrayList<String>
            latitudes = ObjectSerializer.deserialize(sharedPreferences.getString("latitudes", ObjectSerializer.serialize(object : ArrayList<String>() {}))) as ArrayList<String>
            longitudes = ObjectSerializer.deserialize(sharedPreferences.getString("longitudes", ObjectSerializer.serialize(object : ArrayList<String>() {}))) as ArrayList<String>
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (mLocationPermissionsGranted) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1000f, mLocationListener)
        }

        mAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, locationsStrings)
        listView.adapter = mAdapter
        listView.onItemLongClickListener = this
        listView.onItemClickListener = this
    }

    private fun getAddress(location: Location) {
        val geocoder = Geocoder(this, Locale.getDefault())
        var list: List<Address> = ArrayList()

        try {
            list = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (list.isNotEmpty()) {
            val address = list[0]
            Log.i("TAG", address.toString())
            val mAddress = address.getAddressLine(0).toString()

            if (locationsStrings.size > 0) {
                var i = 0
                var duplicate = 0
                while (i < locationsStrings.size) {
                    if (locationsStrings[i] == mAddress) {
                        duplicate++
                    }
                    i++
                }
                if (duplicate < 1) {
                    saveData(mAddress, location)
                }

            } else {
                saveData(mAddress, location)
            }
        }
    }

    fun saveCurrentLocation(view: View) {
        if (mLocationPermissionsGranted) {
            getAddress(currentLocation)
        }
    }

    fun showCurrentLocation(view: View) {
        val intent = Intent(this, MapsActivity::class.java)
        intent.putExtra("key", -100)
        startActivity(intent)

    }

    private fun saveData(address: String, location: Location) {
        locationsStrings.add(address)
        latitudes.add(location.latitude.toString())
        longitudes.add(location.longitude.toString())
        mAdapter.notifyDataSetChanged()
        sharedPreferences.edit().putString("locationsStrings", ObjectSerializer.serialize(locationsStrings)).apply()
        sharedPreferences.edit().putString("latitudes", ObjectSerializer.serialize(latitudes)).apply()
        sharedPreferences.edit().putString("longitudes", ObjectSerializer.serialize(longitudes)).apply()
        Toast.makeText(applicationContext, "Location Saved!", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mLocationPermissionsGranted = false

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    var i = 0
                    while (i < grantResults.size) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false
                            return
                        }
                        i++
                    }
                    mLocationPermissionsGranted = true
                }
            }
        }
    }

    override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
        AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_delete)
                .setTitle("Detele Location")
                .setMessage("Do you want to delete selected location?")
                .setPositiveButton("Yes") { _, _ ->
                    locationsStrings.removeAt(position)
                    latitudes.removeAt(position)
                    longitudes.removeAt(position)
                    sharedPreferences.edit().putString("locationsStrings", ObjectSerializer.serialize(locationsStrings)).apply()
                    sharedPreferences.edit().putString("latitudes", ObjectSerializer.serialize(latitudes)).apply()
                    sharedPreferences.edit().putString("longitudes", ObjectSerializer.serialize(longitudes)).apply()
                    mAdapter.notifyDataSetChanged()
                    Toast.makeText(applicationContext, "Location Deleted!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        return true
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val intent = Intent(this, MapsActivity::class.java)
        intent.putExtra("key", position)
        intent.putExtra("latitude", latitudes[position])
        intent.putExtra("longitude", longitudes[position])
        startActivity(intent)
    }
}
