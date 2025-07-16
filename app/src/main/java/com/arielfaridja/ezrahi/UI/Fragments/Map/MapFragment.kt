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
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
    private var map: MapView? = null
    private var mapController: IMapController? = null
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var user: User? = null
    private val usersMarkers = mutableMapOf<String, Marker>()
    private val reportsMarkers = mutableMapOf<String, Marker>()
    private lateinit var dialog: AlertDialog
    private lateinit var locationManager: LocationManager
    private lateinit var model: MapViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        model = ViewModelProvider(this)[MapViewModel::class.java]
        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        findViews(view)
        setupMyLocationButton()
        setupMap()
        observeViewModel()
        model.refresh()
        if (model.currentActivity.id.isNullOrBlank()) showNoActivityAssignedDialog()
        return view
    }

    // region UI Setup
    private fun findViews(view: View) {
        map = view.findViewById(R.id.map)
        myLocationButton = view.findViewById(R.id.myLocationBtn)
    }

    private fun setupMyLocationButton() {
        myLocationButton.setOnClickListener {
            if (!checkLocationPermission()) {
                showLocationPermissionRationale()
                return@setOnClickListener
            }
            myLocationButton.isEnabled = false
            myLocationButton.alpha = 0.5f
            myLocationButton.postDelayed({
                myLocationButton.isEnabled = true
                myLocationButton.alpha = 1f
            }, 1000)
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                myLocationOverlay?.enableFollowLocation()
                animateMapCenter(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER))
                mapController?.zoomTo(20, null)
            } else {
                showEnableGpsDialog()
            }
        }
    }

    private fun showLocationPermissionRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.enable_location_services))
            .setMessage(getString(R.string.location_services_required))
            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun animateMapCenter(location: Location?) {
        location?.let {
            mapController?.animateTo(GeoPoint(it))
        } ?: Toast.makeText(context, getString(R.string.cant_get_occurred_location_right_now), Toast.LENGTH_SHORT).show()
    }
    // endregion

    // region Map Setup
    private fun setupMap() {
        map?.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            setLastMapCenter()
            mapController = controller
            mapController?.setCenter(GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
            if (!isLocationServicesEnabled()) showEnableLocationServicesDialog() else setupLocationOverlay()
            addRotationGestureOverlay()
            addLongPressOverlay()
        }
    }

    private fun isLocationServicesEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showEnableLocationServicesDialog() {
        dialog = AlertDialog.Builder(requireActivity())
            .setTitle(getString(R.string.enable_location_services))
            .setMessage(getString(R.string.location_services_required))
            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun setupLocationOverlay() {
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map).apply {
            setPersonIcon(BitmapFactory.decodeResource(resources, R.drawable.current_location))
            setPersonAnchor(.5f, .5f)
            enableMyLocation()
            enableFollowLocation()
        }
        map?.overlayManager?.add(myLocationOverlay)
        myLocationOverlay?.myLocation?.let { mapController?.setCenter(it) }
    }

    private fun addRotationGestureOverlay() {
        map?.let {
            val rotationGestureOverlay = RotationGestureOverlay(it)
            rotationGestureOverlay.isEnabled = true
            it.overlayManager.add(rotationGestureOverlay)
        }
    }

    private fun addLongPressOverlay() {
        map?.let { mapView ->
            val overlay = object : Overlay() {
                override fun onLongPress(event: MotionEvent?, mapView: MapView?): Boolean {
                    event?.let { ev ->
                        mapView?.let {
                            showAddMarkerDialog(ev, it)
                        }
                    }
                    return super.onLongPress(event, mapView)
                }
            }
            mapView.overlayManager.add(overlay)
        }
    }
    // endregion

    // region Dialogs
    private fun showAddMarkerDialog(event: MotionEvent, mapView: MapView) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_marker, null)
        val titleEditText = dialogView.findViewById<TextInputEditText>(R.id.edit_text_title)
        val snippetEditText = dialogView.findViewById<TextInputEditText>(R.id.edit_text_snippet)
        val iconSpinner = dialogView.findViewById<Spinner>(R.id.spinner_icon)
        iconSpinner.adapter = IconTextAdapter(context, Consts.iconTextArray)
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_marker))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.Add)) { _, _ ->
                val title = titleEditText.text.toString()
                val description = snippetEditText.text.toString()
                val selectedIcon = (iconSpinner.selectedItem as Triple<*, *, *>).third as Int
                val location = mapView.projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint?
                model.addReport(
                    title,
                    description,
                    location,
                    selectedIcon.toEnum<ReportType>()!!,
                    object : Callback<String> {
                        override fun onResponse(response: Callback.Response<String>) {
                            if (response.message?.startsWith(requireContext().getString(R.string.report_add_success)) == true) {
                                // Marker will be added via observer
                            } else {
                                Toast.makeText(requireContext(),
                                    response.exception?.message
                                        ?: getString(R.string.unknown_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showNoActivityAssignedDialog() {
        if (::dialog.isInitialized && dialog.isShowing) dialog.dismiss()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.no_activity_assigned))
            .setMessage(getString(R.string.please_assign_activity))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener {
                (requireActivity() as MainActivity).navController.navigate(R.id.nav_settings)
            }
            .show()
    }
    // endregion

    // region ViewModel Observers
    private fun observeViewModel() {
        model.users.observe(viewLifecycleOwner, Observer { users ->
            users.forEach { (id, user) ->
                if (id != model.currentUser.id) {
                    if (usersMarkers.containsKey(id)) {
                        modifyUserMarker(user, usersMarkers[id])
                    } else {
                        addUserMarker(user)
                    }
                }
            }
        })
        model.reports.observe(viewLifecycleOwner, Observer { reports ->
            val deletedKeys = reportsMarkers.keys.subtract(reports.keys)
            reports.forEach { (id, report) ->
                if (reportsMarkers.containsKey(id)) {
                    modifyReportMarker(report, reportsMarkers[id])
                } else {
                    addReportMarker(report)
                }
            }
            deletedKeys.forEach { key ->
                reportsMarkers[key]?.remove(map)
                reportsMarkers.remove(key)
            }
        })
    }
    // endregion

    // region Marker Management
    private fun addUserMarker(user: ActUser) {
        val marker = Marker(map).apply {
            icon = getUserIcon(user.role)
            position = GeoPoint(user.location.latitude, user.location.longitude)
            title = "${user.firstName} ${user.lastName}"
            textLabelFontSize = 12
            setVisible(true)
        }
        usersMarkers[user.id] = marker
        map?.overlayManager?.add(marker)
        map?.invalidate()
    }

    private fun modifyUserMarker(user: ActUser, marker: Marker?) {
        marker?.let {
            it.position = GeoPoint(user.location.latitude, user.location.longitude)
        }
    }

    private fun addReportMarker(report: Report) {
        val reportType = report.reportType ?: ReportType.GENERAL
        val location = report.location ?: Latlng(0.0, 0.0)
        val id = report.id ?: ""
        val marker = Marker(map).apply {
            icon = reportTypeToIcon(reportType)
            position = GeoPoint(location.latitude, location.longitude)
            title = report.title ?: ""
            snippet = report.description ?: ""
            alpha = getReportAlpha(report.reportStatus)
            textLabelFontSize = 12
            setVisible(true)
        }
        reportsMarkers[id] = marker
        map?.overlayManager?.add(marker)
        map?.invalidate()
    }

    private fun modifyReportMarker(report: Report, marker: Marker?) {
        marker?.let {
            val reportType = report.reportType ?: ReportType.GENERAL
            val location = report.location ?: Latlng(0.0, 0.0)
            it.title = report.title ?: ""
            it.snippet = report.description ?: ""
            it.position = GeoPoint(location.latitude, location.longitude)
            it.icon = reportTypeToIcon(reportType)
            it.alpha = getReportAlpha(report.reportStatus)
        }
    }
    // endregion

    // region Helpers
    private fun getUserIcon(role: Int): Drawable? {
        // ניתן להרחיב לפי סוגי תפקידים
        return ResourcesCompat.getDrawable(resources, R.drawable.hiker_sign, null)
    }

    private fun reportTypeToIcon(reportType: ReportType?): Drawable? {
        return when (reportType ?: ReportType.GENERAL) {
            ReportType.GENERAL, ReportType.UNKNOWN -> ResourcesCompat.getDrawable(resources, R.drawable.report_canvas, null)
            ReportType.MEDICAL -> ResourcesCompat.getDrawable(resources, R.drawable.report_medical, null)
            else -> ResourcesCompat.getDrawable(resources, R.drawable.report_canvas, null)
        }
    }

    private fun getReportAlpha(status: ReportStatus?): Float {
        return when (status) {
            ReportStatus.REPORTED -> 1f
            ReportStatus.HANDLED -> 0.5f
            ReportStatus.UNKNOWN, null -> 0.0f
        }
    }

    private fun checkLocationPermission(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                0
            )
            return false
        }
        return true
    }

    private fun setMapCenter(location: Location?) {
        location?.let {
            mapController?.setCenter(GeoPoint(it))
        } ?: Toast.makeText(context, getString(R.string.cant_get_occurred_location_right_now), Toast.LENGTH_SHORT).show()
    }

    private fun setMapCenter(latitude: Double, longitude: Double) {
        mapController?.setCenter(GeoPoint(latitude, longitude))
    }
    // endregion

    // region Map State Persistence
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
        map?.let {
            val editor = requireActivity().getPreferences(Context.MODE_PRIVATE).edit()
            editor.putFloat(KEY_LAST_LATITUDE, it.mapCenter.latitude.toFloat())
            editor.putFloat(KEY_LAST_LONGITUDE, it.mapCenter.longitude.toFloat())
            editor.putInt(KEY_LAST_ZOOM_LEVEL, it.zoomLevelDouble.toInt())
            editor.apply()
        }
    }

    private fun setLastMapCenter() {
        val sharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val lastLatitude = sharedPreferences.getFloat(KEY_LAST_LATITUDE, DEFAULT_LATITUDE.toFloat()).toDouble()
        val lastLongitude = sharedPreferences.getFloat(KEY_LAST_LONGITUDE, DEFAULT_LONGITUDE.toFloat()).toDouble()
        val lastZoomLevel = sharedPreferences.getInt(KEY_LAST_ZOOM_LEVEL, DEFAULT_ZOOM_LEVEL).toDouble()
        mapController = map?.controller
        mapController?.setZoom(lastZoomLevel)
        setMapCenter(lastLatitude, lastLongitude)
    }
    // endregion

    private fun showEnableGpsDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.enable_gps))
            .setMessage(getString(R.string.gps_not_enabled))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    companion object {
        fun newInstance() = MapFragment()
        private const val DEFAULT_ZOOM_LEVEL = 18
        private const val DEFAULT_LATITUDE = 31.776551
        private const val DEFAULT_LONGITUDE = 35.233808
        private const val KEY_LAST_LATITUDE = "lastLatitude"
        private const val KEY_LAST_LONGITUDE = "lastLongitude"
        private const val KEY_LAST_ZOOM_LEVEL = "lastZoomLevel"
    }
}