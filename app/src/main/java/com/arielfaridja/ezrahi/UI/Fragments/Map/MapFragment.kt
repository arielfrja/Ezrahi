package com.arielfaridja.ezrahi.UI.Fragments.Map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
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
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.arielfaridja.ezrahi.Consts
import com.arielfaridja.ezrahi.ExtensionMethods.Companion.toEnum
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.UI.IconTextAdapter
import com.arielfaridja.ezrahi.UI.Main.MainActivity
import com.arielfaridja.ezrahi.entities.ActUser
import com.arielfaridja.ezrahi.entities.Callback
import com.arielfaridja.ezrahi.entities.Latlng
import com.arielfaridja.ezrahi.entities.Report
import com.arielfaridja.ezrahi.entities.ReportStatus
import com.arielfaridja.ezrahi.entities.ReportType
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
    private val usersMarkers = mutableMapOf<String, Marker>() // <user ID, Marker>
    private val reportsMarkers = mutableMapOf<String, Marker>()
    private lateinit var dialog: AlertDialog
    private fun addUserMarker(user: ActUser) {
        var marker = Marker(map).apply {
            when (user.role) { //TODO: set by type
                0 -> icon = resources.getDrawable(R.drawable.hiker_sign, null)

                else -> icon = resources.getDrawable(R.drawable.hiker_sign, null)
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
        val reportType = report.reportType ?: ReportType.GENERAL
        val location = report.location ?: Latlng(0.0, 0.0)
        val id = report.id ?: ""
        var marker = Marker(map).apply {
            icon = reportTypeToIcon(reportType)
            position = GeoPoint(location.latitude, location.longitude)
            title = report.title ?: ""
            snippet = report.description ?: ""
            alpha = when (report.reportStatus) {
                ReportStatus.REPORTED -> 1f
                ReportStatus.HANDLED -> .5f
                ReportStatus.UNKNOWN -> .0f
            }
            textLabelFontSize = 12
            setVisible(true)
        }
        reportsMarkers[id] = marker
        map!!.overlayManager.add(marker)
        map!!.invalidate()
    }

    private fun modifyUserMarker(user: ActUser, marker: Marker?) {
        if (user.id == model.currentUser.id) marker!!.remove(map)
        usersMarkers.get(user.id)!!.position =
            GeoPoint(user.location.latitude, user.location.longitude)


    }

    private lateinit var locationManager: LocationManager
    private lateinit var model: MapViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_map, container, false)
        model = ViewModelProvider(this)[MapViewModel::class.java]
        locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (activity is MainActivity) {
            val mainActivity = activity as MainActivity
            mainActivity.supportActionBar!!.title = null
            mainActivity.setToolbarFloating(true)


        }

        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(requireContext()))
        this.findViews(view)
        myLocationButton.setOnClickListener { _ ->
            if (checkLocationPermission()) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    myLocationOverlay!!.enableFollowLocation()

                    setMapCenter(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER))
                    mapController!!.zoomTo(20, null)
                } else {
                    val alertDialog = AlertDialog.Builder(requireContext())
                    alertDialog.setTitle("Enable GPS")
                    alertDialog.setMessage("GPS is not enabled. Do you want to enable it now?")
                    alertDialog.setPositiveButton("Yes") { _, _ ->
                        // You can open the device settings to enable GPS here
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                    }
                    alertDialog.setNegativeButton("No") { _, _ ->
                        // Do Nothing
                    }
                    alertDialog.show()
                }
            }


        }

        this.mapDefinition()
        model.refresh()
        model.users.observe(viewLifecycleOwner, Observer { users ->
            for (u in users) {
                if (!u.key.equals(model.currentUser.id)) if (usersMarkers.containsKey(u.key))
//                    if (u.value.location.latitude != usersMarkers[u.key]!!.position.latitude ||
//                        u.value.location.longitude != usersMarkers[u.key]!!.position.longitude
//                    )
                    modifyUserMarker(u.value, usersMarkers.get(u.key))
                else addUserMarker(u.value)
            }
        })
        model.reports.observe(viewLifecycleOwner, Observer { reports ->
            val deletedKeys = reportsMarkers.keys.subtract(reports.keys)
            for (r in reports) {
                if (reportsMarkers.containsKey(r.key)) modifyReportMarker(
                    r.value,
                    reportsMarkers.get(r.key)
                )
                else addReportMarker(r.value)
            }
            for (key in deletedKeys) {
                reportsMarkers[key]!!.remove(map!!)
                reportsMarkers.remove(key)
            }

        })


        if (model.currentActivity.id.isNullOrBlank())
            noActivityAssignedDialog()
        return view
    }


    private fun Fragment.noActivityAssignedDialog() {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())

        // Set the dialog title and message
        alertDialogBuilder.setTitle("No Activity Assigned")
        alertDialogBuilder.setMessage("Please assign an activity.")

        // Set the OK button
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
            // Navigate to the "settingsFragment" using the NavController
            // Dismiss the dialog
            dialog.dismiss()
        }

        // Set the dismiss action (if needed)
        alertDialogBuilder.setOnDismissListener {
            // This block will be executed when the dialog is dismissed
            // You can add additional actions here if needed
            (requireActivity() as MainActivity).navController.navigate(R.id.nav_settings)
        }

        if (::dialog.isInitialized && dialog.isShowing)
            dialog.dismiss()
        dialog = alertDialogBuilder.create()
        dialog.show()
    }

    private fun modifyReportMarker(report: Report, marker: Marker?) {
        if (marker != null) {
            val reportType = report.reportType ?: ReportType.GENERAL
            val location = report.location ?: Latlng(0.0, 0.0)
            marker.title = report.title ?: ""
            marker.snippet = report.description ?: ""
            marker.position = GeoPoint(location.latitude, location.longitude)
            marker.icon = reportTypeToIcon(reportType)
            marker.alpha = when (report.reportStatus) {
                ReportStatus.REPORTED -> 1f
                ReportStatus.HANDLED -> .5f
                ReportStatus.UNKNOWN -> .0f
            }
        }
    }

    private fun reportTypeToIcon(reportType: ReportType?): Drawable? {
        return when (reportType ?: ReportType.GENERAL) {
            ReportType.GENERAL -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.report_canvas,
                null
            )
            ReportType.MEDICAL -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.report_medical,
                null
            )
            ReportType.UNKNOWN -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.report_canvas,
                null
            )
            else -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.report_canvas,
                null
            )
        }
    }

    private fun checkLocationPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION
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
        } else return true
    }

    private fun setMapCenter(location: Location?) {
        if (location != null) mapController?.setCenter(GeoPoint(location))
        else Toast.makeText(
            context, getString(R.string.cant_get_occurred_location_right_now), Toast.LENGTH_SHORT
        ).show()
    }

    private fun setMapCenter(latitude: Double, longitude: Double) {
        mapController?.setCenter(GeoPoint(latitude, longitude))
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
        setLastMapCenter()

        (mapController as IMapController).setCenter(GeoPoint(31.776551, 35.233808))


        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        ) {
            // Location services are not enabled
            // Show a dialog to the user asking them to enable location services

            dialog = AlertDialog.Builder(requireActivity()).setTitle("Enable Location Services")
                .setMessage("Location services are required for this app. Please enable location services.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    // Open the settings page to enable location services
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }.setNegativeButton("Cancel") { _, _ ->
                    // Do nothing
                }.show()
        } else {
            // Location services are enabled
            // Start requesting location updates
            //startLocationUpdates()
        }

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
        myLocationOverlay!!.setPersonIcon(
            BitmapFactory.decodeResource(
                resources, R.drawable.current_location
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
                            .setTitle(getString(R.string.add_marker)).setView(dialogView)
                            .setPositiveButton(getString(R.string.Add)) { _, _ ->
                                val title = titleEditText.text.toString()
                                val description = snippetEditText.text.toString()
                                val selectedIcon =
                                    (iconSpinner.selectedItem as Triple<*, *, *>).third as Int
                                val marker = Marker(mapView)
                                var location = mapView.projection.fromPixels(
                                    event.x.toInt(), event.y.toInt()
                                ) as GeoPoint?
                                model.addReport(
                                    title,
                                    description,
                                    location,
                                    selectedIcon.toEnum<ReportType>()!!,
                                    object : Callback<String> {
                                        override fun onResponse(response: Callback.Response<String>) {
                                            if (response.message?.startsWith(requireContext().getString(R.string.report_add_success)) == true) {
                                                //addReportMarker()
                                                /*
                                                marker.position = location
                                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                                marker.title = title
                                                marker.snippet =
                                                    "${requireContext().getString((iconSpinner.selectedItem as Triple<*, Int, *>).second)}:\n$description"
                                                marker.icon = ResourcesCompat.getDrawable(
                                                    resources,
                                                    (iconSpinner.selectedItem as Triple<*, *, *>).first as Int,
                                                    null
                                                )

                                                mapView.overlays.add(marker)
                                                mapView.invalidate()
                                                 */
                                            } else Toast.makeText(
                                                requireContext(),
                                                response.exception?.message ?: "Unknown error",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )


                            }.setNegativeButton(getString(R.string.cancel)) { _, _ -> }
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

    override fun onPause() {
        super.onPause()
        saveMapCenter()
    }

    override fun onResume() {
        super.onResume()
        setLastMapCenter()
    }

    override fun onStop() {
        super.onStop()
        saveMapCenter()
    }

    override fun onStart() {
        super.onStart()
        setLastMapCenter()
    }


    private fun saveMapCenter() {
        val editor = requireActivity().getPreferences(Context.MODE_PRIVATE).edit()
        editor.putFloat("lastLatitude", map!!.mapCenter.latitude.toFloat())
        editor.putFloat("lastLongitude", map!!.mapCenter.longitude.toFloat())
        editor.putInt("lastZoomLevel", map!!.zoomLevelDouble.toInt())
        editor.apply()
    }

    private fun setLastMapCenter() {
        val sharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val lastLatitude =
            sharedPreferences.getFloat("lastLatitude", DEFAULT_LATITUDE.toFloat()).toDouble()
        val lastLongitude =
            sharedPreferences.getFloat("lastLongitude", DEFAULT_LONGITUDE.toFloat()).toDouble()
        val lastZoomLevel = sharedPreferences.getInt("lastZoomLevel", DEFAULT_ZOOM_LEVEL).toDouble()
        mapController = map!!.controller
        (mapController as IMapController).setZoom(lastZoomLevel)
        setMapCenter(lastLatitude, lastLongitude)
    }

    companion object {
        fun newInstance() = MapFragment()
        private const val DEFAULT_ZOOM_LEVEL = 18
        private const val DEFAULT_LATITUDE = 31.776551
        private const val DEFAULT_LONGITUDE = 35.233808
    }
}