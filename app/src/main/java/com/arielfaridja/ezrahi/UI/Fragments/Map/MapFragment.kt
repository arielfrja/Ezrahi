package com.arielfaridja.ezrahi.UI.Fragments.Map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.UI.Main.MainActivity
import com.arielfaridja.ezrahi.entities.ActUser
import com.arielfaridja.ezrahi.entities.User
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class MapFragment : Fragment() {
    private lateinit var myLocationButton: FloatingActionButton
    private var toolbar: Toolbar? = null
    var map: MapView? = null
    var mapController: IMapController? = null
    var myLocationOverlay: MyLocationNewOverlay? = null
    var user: User? = null
    private val usersMarkers = mutableMapOf<String, Marker>()

    private fun addUserMarker(user: ActUser) {
        var marker = Marker(map).apply {
            when (user.permission) { //TODO: set by type
                0 -> icon = resources.getDrawable(R.drawable.ic_round_man_24, null)
            }
            position.latitude = user.location.latitude
            position.longitude = user.location.longitude
        }
        usersMarkers.put(user.id, marker)
        map!!.overlays.add(usersMarkers[user.id])
    }


    private fun modifyUserMarker(user: ActUser, marker: Marker?) {
        usersMarkers.get(user.id)!!.position.latitude = user.location.latitude
        usersMarkers.get(user.id)!!.position.latitude = user.location.longitude
    }

    private lateinit var locationManager: LocationManager
    private lateinit var model: MapViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_map, container, false)
        model = ViewModelProvider(this).get(MapViewModel::class.java)
        locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (activity is MainActivity) {
            val mainActivity = activity as MainActivity
            user = mainActivity.user
            mainActivity.supportActionBar!!.title = null
            mainActivity.setToolbarFloating(true)


        }

        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        this.findViews(view)
        myLocationButton.setOnClickListener { view ->
            if (checkLocationPermission()) {
                myLocationOverlay!!.enableFollowLocation()
                setMapCenter(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER))
                mapController!!.zoomTo(20, null)
            }

        }

        this.mapDefinition()
        model.users.observe(viewLifecycleOwner, Observer { users ->
            for (u in users) {
                if (usersMarkers.containsKey(u.key))
                    modifyUserMarker(u.value, usersMarkers.get(u.key))
                else
                    addUserMarker(u.value)
            }
        })


        return view
    }

    private fun checkLocationPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), 0
            )
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return false
        } else
            return true
    }

    private fun setMapCenter(location: Location?) {
        mapController?.setCenter(GeoPoint(location))
    }


    private fun findViews(view: View) {
        map = view.findViewById(R.id.map)
        myLocationButton = view.findViewById(R.id.myLocationBtn)
        this.toolbar = activity?.findViewById<Toolbar>(R.id.toolbar)
    }


    private fun mapDefinition() {
        map!!.setTileSource(TileSourceFactory.MAPNIK)
        map!!.setMultiTouchControls(true)
        map!!.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        mapController = map!!.controller
        (mapController as IMapController).setZoom(18.0)
        (mapController as IMapController).setCenter(GeoPoint(31.776551, 35.233808))


        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) {
            // Location services are not enabled
            // Show a dialog to the user asking them to enable location services

            AlertDialog.Builder(requireActivity())
                .setTitle("Enable Location Services")
                .setMessage("Location services are required for this app. Please enable location services.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    // Open the settings page to enable location services
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Cancel") { _, _ ->
                    // Do nothing
                }
                .show()
        } else {
            // Location services are enabled
            // Start requesting location updates
            //startLocationUpdates()
        }

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
        //myLocationOverlay!!.setPersonIcon(BitmapFactory.decodeResource(resources,R.drawable.ic_my_location_dot_24, null))
        myLocationOverlay!!.enableMyLocation()
        myLocationOverlay!!.enableFollowLocation()
        map!!.overlays.add(myLocationOverlay)
        (mapController as IMapController).setCenter(myLocationOverlay!!.myLocation)
    }


    companion object {
        fun newInstance() = MapFragment()
    }
}