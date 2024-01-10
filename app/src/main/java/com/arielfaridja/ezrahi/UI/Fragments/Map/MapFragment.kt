package com.arielfaridja.ezrahi.UI.Fragments.Map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.arielfaridja.ezrahi.Consts
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.UI.IconTextAdapter
import com.arielfaridja.ezrahi.UI.Main.MainActivity
import com.arielfaridja.ezrahi.entities.ActUser
import com.arielfaridja.ezrahi.entities.Report
import com.arielfaridja.ezrahi.entities.User
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
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
    private val reportsMarker = mutableMapOf<String, Marker>()

    private fun addUserMarker(user: ActUser) {
        var marker = Marker(map).apply {
            when (user.role) { //TODO: set by type
                0 -> icon =
                    resources.getDrawable(R.drawable.hiker_sign, null)

                else -> icon =
                    resources.getDrawable(R.drawable.hiker_sign, null)
            }
            position = GeoPoint(user.location.latitude, user.location.latitude)
            title = user.firstName + " " + user.lastName
            //setTextIcon(user.firstName + " " + user.lastName)
            textLabelFontSize = 12
            setVisible(true)
        }
        usersMarkers.put(user.id, marker)
        map!!.overlayManager.add(usersMarkers[user.id])
        map!!.invalidate()
    }

    private fun addReportMarker(report: Report) {

    }

    private fun modifyUserMarker(user: ActUser, marker: Marker?) {
        if (user.id == model.currentUser.id)
            marker!!.remove(map)
        usersMarkers.get(user.id)!!.position =
            GeoPoint(user.location.latitude, user.location.longitude)


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
                if (!u.key.equals(model.currentUser!!.id))
                    if (usersMarkers.containsKey(u.key))
//                    if (u.value.location.latitude != usersMarkers[u.key]!!.position.latitude ||
//                        u.value.location.longitude != usersMarkers[u.key]!!.position.longitude
//                    )
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
        myLocationOverlay!!.setPersonIcon(
            BitmapFactory.decodeResource(
                resources,
                R.drawable.current_location
            )
        )
        myLocationOverlay!!.setPersonAnchor(.5f, .5f)
        myLocationOverlay!!.enableMyLocation()
        myLocationOverlay!!.enableFollowLocation()

        map!!.overlayManager.add(myLocationOverlay)
        (mapController as IMapController).setCenter(myLocationOverlay!!.myLocation)

        val rotationGestureOverlay = RotationGestureOverlay(map!!)
        rotationGestureOverlay.isEnabled = true
        map!!.overlayManager.add(rotationGestureOverlay)

        val overlay = object : Overlay() {
            override fun onLongPress(event: MotionEvent?, mapView: MapView?): Boolean {
                event?.let { ev ->
                    val x = ev.x
                    val y = ev.y
                    mapView?.let { mapView1 ->
                        val dialogView = layoutInflater.inflate(R.layout.dialog_add_marker, null)
                        val titleEditText =
                            dialogView.findViewById<TextInputEditText>(R.id.edit_text_title)
                        val snippetEditText =
                            dialogView.findViewById<TextInputEditText>(R.id.edit_text_snippet)
                        val iconSpinner = dialogView.findViewById<Spinner>(R.id.spinner_icon)
                        iconSpinner.adapter = IconTextAdapter(context, Consts.iconTextArray)

                        val dialogBuilder = AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.add_marker))
                            .setView(dialogView)
                            .setPositiveButton(getString(R.string.Add)) { _, _ ->
                                val title = titleEditText.text.toString()
                                val description = snippetEditText.text.toString()
                                val selectedIcon = iconSpinner.selectedItem.toString()
                                val marker = Marker(mapView)
                                var location = mapView.projection.fromPixels(
                                    event.x.toInt(),
                                    event.y.toInt()
                                ) as GeoPoint?
                                model.addReport(title, description, location) { response ->
                                    if (response.message.equals(Consts.REPORT_ADD_SUCCESS)) {
                                        //addReportMarker()
                                        marker.position = location
                                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                        marker.title = title
                                        marker.snippet =
                                            "${(iconSpinner.selectedItem as Pair<Int, String>).second}:\n$description"
                                        marker.icon = resources.getDrawable(
                                            (iconSpinner.selectedItem as Pair<Int, String>).first,
                                            null
                                        )

                                        mapView.overlays.add(marker)
                                        mapView.invalidate()
                                    } else
                                        Toast.makeText(
                                            requireContext(),
                                            response.exception.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                }


                            }
                            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
                        val dialog = dialogBuilder.create()
                        dialog.show()
//                        val geoPoint = mapView1.projection.fromPixels(x.toInt(), y.toInt())
//                        //////TODO:  replace with dialog
//                        /////show the marker just after it updated in db.
//                        val marker = Marker(mapView1)
//                        marker.position = geoPoint as GeoPoint?
//                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
//                        marker.title = "New marker"
//                        marker.snippet = "You have added a new marker"
//                        map!!.overlayManager.add(marker)
//                        /////
//                        mapView1.invalidate()
                    }
                }
                return super.onLongPress(event, mapView)
            }
        }
        map!!.overlayManager.add(overlay)
    }


    companion object {
        fun newInstance() = MapFragment()
    }
}